package dev.aaow.launcher;

import dev.aaow.apk.AndroidManifest;
import dev.aaow.apk.ApkFile;
import dev.aaow.rt.HostWindow;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Công cụ phân tích độ tương thích tĩnh (Static Compatibility Scanner) cho file APK.
 * Quét toàn bộ bytecode đã dịch từ classes*.dex bằng ASM, đối chiếu với:
 *   1. Các class/method đã cài đặt thật trong module :framework của AAOW.
 *   2. Các class/method có sẵn trong android.jar của Android SDK (sẽ tự động De-Stub lúc chạy).
 *   3. JDK 21 chuẩn.
 * Từ đó tính tỉ lệ tương thích định lượng và liệt kê Top API thiếu ưu tiên cao nhất.
 */
public final class ApkCompatibilityScanner {

    public enum ApiStatus {
        IMPLEMENTED,
        MISSING_METHOD_ON_FRAMEWORK_CLASS,
        STUBBED_VIA_SDK,
        MISSING_COMPLETELY
    }

    public record MissingClassStat(String className, ApiStatus status, int references, int uniqueMethodsCalled) {
    }

    public record MissingMethodStat(String owner, String methodName, String descriptor,
                                    ApiStatus status, int callSites) {
        public String signature() {
            return owner + "." + methodName + descriptor;
        }
    }

    public record ScanReport(
            String apkFileName,
            String packageName,
            String label,
            String versionName,
            int versionCode,
            int minSdk,
            int targetSdk,
            int dexFileCount,
            int apkClassCount,
            int resourceCount,
            List<String> nativeAbis,
            int nativeMethodCount,
            int loadLibraryCallCount,
            String applicationClass,
            String launcherActivity,
            boolean launcherExtendsActivity,
            boolean androidJarAvailable,
            Map<String, Integer> bundledLibraries,
            int totalSystemClasses,
            int implementedSystemClasses,
            int stubbedSystemClasses,
            int missingSystemClasses,
            int totalUniqueSystemMethods,
            int implementedUniqueMethods,
            int missingOnFrameworkUniqueMethods,
            int stubbedUniqueMethods,
            int missingCompletelyUniqueMethods,
            int totalSystemCallSites,
            int implementedCallSites,
            int missingOnFrameworkCallSites,
            int stubbedCallSites,
            int missingCompletelyCallSites,
            List<MissingClassStat> topMissingClasses,
            List<MissingMethodStat> topFrameworkMissingMethods,
            List<MissingMethodStat> topAllMissingMethods
    ) {
        public double classCoveragePercent() {
            return totalSystemClasses == 0 ? 100.0 : (implementedSystemClasses * 100.0 / totalSystemClasses);
        }

        public double uniqueMethodCoveragePercent() {
            return totalUniqueSystemMethods == 0 ? 100.0 : (implementedUniqueMethods * 100.0 / totalUniqueSystemMethods);
        }

        public double callSiteCoveragePercent() {
            return totalSystemCallSites == 0 ? 100.0 : (implementedCallSites * 100.0 / totalSystemCallSites);
        }

        public String formatReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("====================================================================\n");
            sb.append(" AAOW APK COMPATIBILITY SCANNER (PHÂN TÍCH TƯƠNG THÍCH TĨNH)\n");
            sb.append("====================================================================\n");
            sb.append(String.format(" File APK      : %s%n", apkFileName));
            sb.append(String.format(" Package       : %s (%s v%s / %d)%n",
                    packageName, label != null ? label : "-", versionName, versionCode));
            sb.append(String.format(" SDK Target    : minSdk=%d, targetSdk=%d%n", minSdk, targetSdk));
            sb.append(String.format(" Cấu trúc DEX  : %d file DEX • %d class • %d resources%n",
                    dexFileCount, apkClassCount, resourceCount));
            sb.append(String.format(" Native (.so)  : ABI=%s • hàm native=%d • lệnh loadLibrary=%d%n",
                    nativeAbis.isEmpty() ? "Không (Thuần Java/Kotlin)" : nativeAbis,
                    nativeMethodCount, loadLibraryCallCount));
            sb.append(String.format(" Application   : %s%n", applicationClass != null ? applicationClass : "<mặc định>"));
            sb.append(String.format(" Launcher      : %s (%s)%n",
                    launcherActivity != null ? launcherActivity : "<không có>",
                    launcherExtendsActivity ? "Kế thừa android.app.Activity" : "KHÔNG nối trực tiếp android.app.Activity"));
            sb.append(String.format(" Android SDK   : %s%n",
                    androidJarAvailable ? "Đã kết nối android.jar (hỗ trợ đối chiếu & De-Stub)" : "Không tìm thấy android.jar"));

            if (!bundledLibraries.isEmpty()) {
                sb.append("\n--- THƯ VIỆN ĐÓNG GÓI BÊN TRONG APK ---\n");
                for (var entry : bundledLibraries.entrySet()) {
                    sb.append(String.format("  • %-28s : %d class%n", entry.getKey(), entry.getValue()));
                }
            }

            sb.append("\n--- ĐỘ PHỦ API HỆ THỐNG (android.* / dalvik.* / org.apache.http.*) ---\n");
            sb.append(String.format("  [1] Theo Class hệ thống (%d class được tham chiếu):%n", totalSystemClasses));
            sb.append(String.format("      • Đã cài đặt trong :framework : %4d (%5.1f%%)%n",
                    implementedSystemClasses, classCoveragePercent()));
            sb.append(String.format("      • Tự gỡ Stub từ android.jar   : %4d (%5.1f%%)%n",
                    stubbedSystemClasses, pct(stubbedSystemClasses, totalSystemClasses)));
            sb.append(String.format("      • Thiếu hoàn toàn             : %4d (%5.1f%%)%n",
                    missingSystemClasses, pct(missingSystemClasses, totalSystemClasses)));

            sb.append(String.format("  [2] Theo Method duy nhất (%d chữ ký hàm hệ thống):%n", totalUniqueSystemMethods));
            sb.append(String.format("      • Đã cài đặt thật (:framework): %4d (%5.1f%%)%n",
                    implementedUniqueMethods, uniqueMethodCoveragePercent()));
            sb.append(String.format("      • Thiếu trên class đã có (!): %4d (%5.1f%%)  [Nguy cơ NoSuchMethodError]%n",
                    missingOnFrameworkUniqueMethods, pct(missingOnFrameworkUniqueMethods, totalUniqueSystemMethods)));
            sb.append(String.format("      • Tự gỡ Stub từ android.jar   : %4d (%5.1f%%)%n",
                    stubbedUniqueMethods, pct(stubbedUniqueMethods, totalUniqueSystemMethods)));
            sb.append(String.format("      • Thiếu hoàn toàn             : %4d (%5.1f%%)%n",
                    missingCompletelyUniqueMethods, pct(missingCompletelyUniqueMethods, totalUniqueSystemMethods)));

            sb.append(String.format("  [3] Theo Tần suất Lời gọi (Call Sites = %d điểm gọi):%n", totalSystemCallSites));
            sb.append(String.format("      • Khớp :framework thật        : %4d (%5.1f%%)%n",
                    implementedCallSites, callSiteCoveragePercent()));
            sb.append(String.format("      • Thiếu trên class đã có (!)  : %4d (%5.1f%%)%n",
                    missingOnFrameworkCallSites, pct(missingOnFrameworkCallSites, totalSystemCallSites)));
            sb.append(String.format("      • Rơi vào De-Stub android.jar : %4d (%5.1f%%)%n",
                    stubbedCallSites, pct(stubbedCallSites, totalSystemCallSites)));
            sb.append(String.format("      • Thiếu hoàn toàn             : %4d (%5.1f%%)%n",
                    missingCompletelyCallSites, pct(missingCompletelyCallSites, totalSystemCallSites)));

            if (!topFrameworkMissingMethods.isEmpty()) {
                sb.append("\n--- TOP 20 METHOD THIẾU TRONG CLASS :framework ĐÃ CÓ (ƯU TIÊN SỐ 1) ---\n");
                int idx = 1;
                for (MissingMethodStat m : topFrameworkMissingMethods) {
                    sb.append(String.format("  %2d. [x%-4d] %s%n", idx++, m.callSites(), m.signature()));
                }
            }

            if (!topMissingClasses.isEmpty()) {
                sb.append("\n--- TOP 20 CLASS HỆ THỐNG CHƯA CÓ TRONG :framework ---\n");
                int idx = 1;
                for (MissingClassStat c : topMissingClasses) {
                    String tag = c.status() == ApiStatus.STUBBED_VIA_SDK ? "De-Stub" : "Thiếu hẳn";
                    sb.append(String.format("  %2d. [%-9s | refs=%-4d | methods=%-3d] %s%n",
                            idx++, tag, c.references(), c.uniqueMethodsCalled(), c.className()));
                }
            }

            if (!topAllMissingMethods.isEmpty()) {
                sb.append("\n--- TOP 20 METHOD HỆ THỐNG THIẾU NHIỀU NHẤT (THEO CALL SITES) ---\n");
                int idx = 1;
                for (MissingMethodStat m : topAllMissingMethods) {
                    String tag = switch (m.status()) {
                        case MISSING_METHOD_ON_FRAMEWORK_CLASS -> "NoSuchMethod!";
                        case STUBBED_VIA_SDK -> "De-Stub";
                        default -> "Thiếu hẳn";
                    };
                    sb.append(String.format("  %2d. [%-13s | x%-4d] %s%n",
                            idx++, tag, m.callSites(), m.signature()));
                }
            }

            sb.append("====================================================================\n");
            return sb.toString();
        }

        private static double pct(int part, int total) {
            return total == 0 ? 0.0 : (part * 100.0 / total);
        }
    }

    private record ClassMeta(
            String internalName,
            String superName,
            List<String> interfaces,
            Set<String> methods // "name+descriptor"
    ) {
    }

    private ApkCompatibilityScanner() {
    }

    public static ScanReport scan(Path apkPath, Path cacheDir) throws IOException {
        try (ApkFile apk = new ApkFile(apkPath)) {
            Path jarPath = DexLoader.ensureDexJar(apk, cacheDir);
            return analyzeJar(apk, jarPath);
        }
    }

    private static ScanReport analyzeJar(ApkFile apk, Path jarPath) throws IOException {
        Map<String, ClassMeta> frameworkClasses = loadFrameworkClasses();
        Path sdkJarPath = AndroidSdkFallbackClassLoader.findSystemAndroidJarPath();
        try (ZipFile sdkZip = sdkJarPath != null && Files.exists(sdkJarPath) ? new ZipFile(sdkJarPath.toFile()) : null;
             ZipFile apkZip = new ZipFile(jarPath.toFile())) {

            Map<String, ClassMeta> sdkCache = new HashMap<>();
            Map<String, ClassMeta> apkClasses = loadApkClasses(apkZip);

            Map<String, Integer> bundledLibs = countBundledLibraries(apkClasses.keySet());

            // Thu thập thống kê tham chiếu
            Map<String, Integer> systemClassRefs = new HashMap<>();
            Map<String, Set<String>> classUniqueMethods = new HashMap<>();
            Map<String, Integer> methodCallCounts = new HashMap<>();
            Map<String, ApiStatus> methodStatuses = new HashMap<>();
            Map<String, String[]> methodParts = new HashMap<>();
            int[] nativeMethodCount = {0};
            int[] loadLibCount = {0};

            Enumeration<? extends ZipEntry> entries = apkZip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (!e.getName().endsWith(".class")) continue;
                try (InputStream in = apkZip.getInputStream(e)) {
                    ClassReader cr = new ClassReader(in);
                    cr.accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                            recordClassTypeRef(superName, apkClasses, systemClassRefs);
                            if (interfaces != null) {
                                for (String itf : interfaces) {
                                    recordClassTypeRef(itf, apkClasses, systemClassRefs);
                                }
                            }
                        }

                        @Override
                        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                            recordDescriptorTypes(descriptor, apkClasses, systemClassRefs);
                            return null;
                        }

                        @Override
                        public MethodVisitor visitMethod(int access, String mName, String mDesc, String signature, String[] exceptions) {
                            recordDescriptorTypes(mDesc, apkClasses, systemClassRefs);
                            return new MethodVisitor(Opcodes.ASM9) {
                                @Override
                                public void visitTypeInsn(int opcode, String type) {
                                    recordInternalOrArrayType(type, apkClasses, systemClassRefs);
                                }

                                @Override
                                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                                    recordInternalOrArrayType(owner, apkClasses, systemClassRefs);
                                    recordDescriptorTypes(descriptor, apkClasses, systemClassRefs);
                                }

                                @Override
                                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                    if ("dev/aaow/rt/NativeBridge".equals(owner)) {
                                        if ("invokeNative".equals(name) || "onNativeCall".equals(name)) {
                                            nativeMethodCount[0]++;
                                        } else if ("loadLibrary".equals(name) || "load".equals(name)) {
                                            loadLibCount[0]++;
                                        }
                                        return;
                                    }

                                    String cleanOwner = unwrapArrayType(owner);
                                    if (cleanOwner == null) return;

                                    if (isSystemNamespace(cleanOwner) && !apkClasses.containsKey(cleanOwner)) {
                                        systemClassRefs.merge(cleanOwner, 1, Integer::sum);
                                    }

                                    // Phân giải lời gọi method tới class gốc (trong APK, :framework, JDK hoặc android.jar)
                                    ResolvedCall resolved = resolveMethodCall(
                                            cleanOwner, name, descriptor, opcode == Opcodes.INVOKESPECIAL,
                                            apkClasses, frameworkClasses, sdkZip, sdkCache);
                                    if (resolved == null) return; // App-internal hoặc JDK

                                    String targetOwnerDot = resolved.targetOwner.replace('/', '.');
                                    String key = targetOwnerDot + "." + name + descriptor;
                                    methodCallCounts.merge(key, 1, Integer::sum);
                                    methodStatuses.putIfAbsent(key, resolved.status);
                                    methodParts.putIfAbsent(key, new String[]{targetOwnerDot, name, descriptor});
                                    if (isSystemNamespace(resolved.targetOwner) && !apkClasses.containsKey(resolved.targetOwner)) {
                                        systemClassRefs.putIfAbsent(resolved.targetOwner, 0);
                                        classUniqueMethods.computeIfAbsent(resolved.targetOwner, k -> new HashSet<>())
                                                .add(name + descriptor);
                                    }
                                }

                                @Override
                                public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
                                    recordDescriptorTypes(descriptor, apkClasses, systemClassRefs);
                                }
                            };
                        }
                    }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }

            // Tổng hợp Class stats
            int totalSysClasses = systemClassRefs.size();
            int implClasses = 0;
            int stubClasses = 0;
            int missClasses = 0;
            List<MissingClassStat> missingClassList = new ArrayList<>();

            for (var entry : systemClassRefs.entrySet()) {
                String clsInternal = entry.getKey();
                int refs = entry.getValue();
                if (frameworkClasses.containsKey(clsInternal) || isJdkClass(clsInternal)) {
                    implClasses++;
                } else if (loadSdkClass(clsInternal, sdkZip, sdkCache) != null) {
                    stubClasses++;
                    int uniqM = classUniqueMethods.getOrDefault(clsInternal, Set.of()).size();
                    missingClassList.add(new MissingClassStat(
                            clsInternal.replace('/', '.'), ApiStatus.STUBBED_VIA_SDK, refs, uniqM));
                } else {
                    missClasses++;
                    int uniqM = classUniqueMethods.getOrDefault(clsInternal, Set.of()).size();
                    missingClassList.add(new MissingClassStat(
                            clsInternal.replace('/', '.'), ApiStatus.MISSING_COMPLETELY, refs, uniqM));
                }
            }
            missingClassList.sort(
                    Comparator.comparingInt(MissingClassStat::references).reversed()
                            .thenComparing(Comparator.comparingInt(MissingClassStat::uniqueMethodsCalled).reversed())
                            .thenComparing(MissingClassStat::className));

            // Tổng hợp Method stats
            int totalUniqM = methodCallCounts.size();
            int implUniqM = 0, missFwUniqM = 0, stubUniqM = 0, missAllUniqM = 0;
            int totalCalls = 0, implCalls = 0, missFwCalls = 0, stubCalls = 0, missAllCalls = 0;

            List<MissingMethodStat> fwMissingList = new ArrayList<>();
            List<MissingMethodStat> allMissingList = new ArrayList<>();

            for (var entry : methodCallCounts.entrySet()) {
                String key = entry.getKey();
                int count = entry.getValue();
                ApiStatus status = methodStatuses.get(key);
                totalCalls += count;
                switch (status) {
                    case IMPLEMENTED -> {
                        implUniqM++;
                        implCalls += count;
                    }
                    case MISSING_METHOD_ON_FRAMEWORK_CLASS -> {
                        missFwUniqM++;
                        missFwCalls += count;
                        String[] p = methodParts.get(key);
                        MissingMethodStat st = new MissingMethodStat(p[0], p[1], p[2], status, count);
                        fwMissingList.add(st);
                        allMissingList.add(st);
                    }
                    case STUBBED_VIA_SDK -> {
                        stubUniqM++;
                        stubCalls += count;
                        String[] p = methodParts.get(key);
                        allMissingList.add(new MissingMethodStat(p[0], p[1], p[2], status, count));
                    }
                    case MISSING_COMPLETELY -> {
                        missAllUniqM++;
                        missAllCalls += count;
                        String[] p = methodParts.get(key);
                        allMissingList.add(new MissingMethodStat(p[0], p[1], p[2], status, count));
                    }
                }
            }

            Comparator<MissingMethodStat> mCmp = Comparator.comparingInt(MissingMethodStat::callSites).reversed()
                    .thenComparing(MissingMethodStat::signature);
            fwMissingList.sort(mCmp);
            allMissingList.sort(mCmp);

            AndroidManifest mf = apk.getManifest();
            boolean launcherIsActivity = extendsAndroidActivity(mf.launcherActivity, apkClasses, frameworkClasses, sdkZip, sdkCache);

            return new ScanReport(
                    apk.getPath().getFileName().toString(),
                    mf.packageName,
                    mf.label,
                    mf.versionName,
                    mf.versionCode,
                    mf.minSdk,
                    mf.targetSdk,
                    apk.readDexFiles().size(),
                    apkClasses.size(),
                    apk.getResources().size(),
                    apk.nativeAbis(),
                    nativeMethodCount[0],
                    loadLibCount[0],
                    mf.applicationClass,
                    mf.launcherActivity,
                    launcherIsActivity,
                    sdkZip != null,
                    bundledLibs,
                    totalSysClasses,
                    implClasses,
                    stubClasses,
                    missClasses,
                    totalUniqM,
                    implUniqM,
                    missFwUniqM,
                    stubUniqM,
                    missAllUniqM,
                    totalCalls,
                    implCalls,
                    missFwCalls,
                    stubCalls,
                    missAllCalls,
                    limitList(missingClassList, 20),
                    limitList(fwMissingList, 20),
                    limitList(allMissingList, 20)
            );
        }
    }

    private record ResolvedCall(String targetOwner, ApiStatus status) {
    }

    private static ResolvedCall resolveMethodCall(
            String owner, String name, String descriptor, boolean isSpecial,
            Map<String, ClassMeta> apkClasses,
            Map<String, ClassMeta> frameworkClasses,
            ZipFile sdkZip, Map<String, ClassMeta> sdkCache) {

        String methodKey = name + descriptor;

        // Trường hợp 1: owner là class hệ thống trực tiếp (không có trong APK)
        if (!apkClasses.containsKey(owner)) {
            if (isJdkClass(owner)) return null;
            if (!isSystemNamespace(owner)) return null;
            return resolveInSystemHierarchy(owner, name, descriptor, isSpecial, frameworkClasses, sdkZip, sdkCache);
        }

        // Trường hợp 2: owner nằm trong DEX của APK -> kiểm tra xem chính APK có định nghĩa method này không
        ClassMeta currentApk = apkClasses.get(owner);
        if (currentApk != null && currentApk.methods.contains(methodKey)) {
            return null; // Lời gọi nội bộ app / thư viện nhúng
        }
        if ("<init>".equals(name) || "<clinit>".equals(name)) {
            return null;
        }

        // Leo lên chuỗi kế thừa trong APK trước, thu thập tất cả các root hệ thống mà class này chạm tới
        Set<String> visited = new HashSet<>();
        List<String> queue = new ArrayList<>();
        enqueueParents(currentApk, queue, visited);

        List<String> systemCandidates = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) {
            String candidate = queue.get(i);
            ClassMeta apkParent = apkClasses.get(candidate);
            if (apkParent != null) {
                if (apkParent.methods.contains(methodKey)) {
                    return null; // Định nghĩa ở lớp cha hoặc interface trong APK
                }
                enqueueParents(apkParent, queue, visited);
            } else if (isJdkClass(candidate)) {
                if (hasJdkMethod(candidate, name, descriptor)) {
                    return null;
                }
            } else if (isSystemNamespace(candidate)) {
                systemCandidates.add(candidate);
            }
        }

        // Chỉ khi không có trong APK/JDK mới phân giải trên các class/interface hệ thống tìm được
        ResolvedCall best = null;
        for (String sysRoot : systemCandidates) {
            ResolvedCall res = resolveInSystemHierarchy(sysRoot, name, descriptor, false,
                    frameworkClasses, sdkZip, sdkCache);
            if (res == null) {
                return null; // Thuộc JDK thông qua cây hệ thống (ví dụ Object.toString)
            }
            if (res.status == ApiStatus.IMPLEMENTED) {
                return res;
            }
            if (best == null || res.status.ordinal() < best.status.ordinal()) {
                best = res;
            }
        }
        return best;
    }

    private static ResolvedCall resolveInSystemHierarchy(
            String systemOwner, String name, String descriptor, boolean isInitOnly,
            Map<String, ClassMeta> frameworkClasses,
            ZipFile sdkZip, Map<String, ClassMeta> sdkCache) {

        String methodKey = name + descriptor;
        Set<String> visited = new HashSet<>();
        List<String> queue = new ArrayList<>();
        queue.add(systemOwner);
        visited.add(systemOwner);

        boolean ownerExistsInFramework = frameworkClasses.containsKey(systemOwner);
        String firstSdkDeclaringOwner = null;

        for (int i = 0; i < queue.size(); i++) {
            String cur = queue.get(i);
            if (isJdkClass(cur)) {
                if (hasJdkMethod(cur, name, descriptor)) {
                    // Các hàm chuẩn của JDK (như Object.toString/hashCode/equals, Closeable.close)
                    // có sẵn trên JVM, không tính vào mẫu số Android API để tránh làm lệch tỉ lệ %.
                    return null;
                }
                continue;
            }

            ClassMeta fw = frameworkClasses.get(cur);
            if (fw != null) {
                if (fw.methods.contains(methodKey)) {
                    return new ResolvedCall(cur, ApiStatus.IMPLEMENTED);
                }
                if (!"<init>".equals(name)) {
                    enqueueParents(fw, queue, visited);
                }
            }

            ClassMeta sdk = loadSdkClass(cur, sdkZip, sdkCache);
            if (sdk != null) {
                if (sdk.methods.contains(methodKey) && firstSdkDeclaringOwner == null) {
                    firstSdkDeclaringOwner = cur;
                }
                if (!"<init>".equals(name)) {
                    enqueueParents(sdk, queue, visited);
                }
            }
        }

        // Nếu chưa có trong :framework nhưng tìm thấy trong android.jar:
        if (firstSdkDeclaringOwner != null) {
            boolean declaringClassInFramework = frameworkClasses.containsKey(firstSdkDeclaringOwner);
            if (declaringClassInFramework || ownerExistsInFramework) {
                return new ResolvedCall(firstSdkDeclaringOwner, ApiStatus.MISSING_METHOD_ON_FRAMEWORK_CLASS);
            }
            return new ResolvedCall(firstSdkDeclaringOwner, ApiStatus.STUBBED_VIA_SDK);
        }

        // Không thấy ở cả :framework lẫn android.jar
        return new ResolvedCall(systemOwner, ApiStatus.MISSING_COMPLETELY);
    }

    private static void enqueueParents(ClassMeta meta, List<String> queue, Set<String> visited) {
        if (meta == null) return;
        if (meta.superName != null && visited.add(meta.superName)) {
            queue.add(meta.superName);
        }
        for (String itf : meta.interfaces) {
            if (itf != null && visited.add(itf)) {
                queue.add(itf);
            }
        }
    }

    private static boolean extendsAndroidActivity(
            String launcherActivity,
            Map<String, ClassMeta> apkClasses,
            Map<String, ClassMeta> frameworkClasses,
            ZipFile sdkZip,
            Map<String, ClassMeta> sdkCache) {
        if (launcherActivity == null || launcherActivity.isEmpty()) return false;
        String cur = launcherActivity.replace('.', '/');
        Set<String> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            if ("android/app/Activity".equals(cur)) return true;
            ClassMeta meta = apkClasses.get(cur);
            if (meta == null) meta = frameworkClasses.get(cur);
            if (meta == null) meta = loadSdkClass(cur, sdkZip, sdkCache);
            cur = meta != null ? meta.superName : null;
        }
        return false;
    }

    private static void recordClassTypeRef(String internalOrArray, Map<String, ClassMeta> apkClasses, Map<String, Integer> refs) {
        String clean = unwrapArrayType(internalOrArray);
        if (clean != null && isSystemNamespace(clean) && !apkClasses.containsKey(clean)) {
            refs.merge(clean, 1, Integer::sum);
        }
    }

    private static void recordInternalOrArrayType(String internalOrDescriptor, Map<String, ClassMeta> apkClasses, Map<String, Integer> refs) {
        if (internalOrDescriptor == null) return;
        if (internalOrDescriptor.startsWith("[")) {
            recordDescriptorTypes(internalOrDescriptor, apkClasses, refs);
        } else {
            recordClassTypeRef(internalOrDescriptor, apkClasses, refs);
        }
    }

    private static void recordDescriptorTypes(String desc, Map<String, ClassMeta> apkClasses, Map<String, Integer> refs) {
        if (desc == null || desc.indexOf('L') < 0) return;
        Type t = Type.getType(desc);
        if (t.getSort() == Type.METHOD) {
            for (Type arg : t.getArgumentTypes()) {
                recordAsmType(arg, apkClasses, refs);
            }
            recordAsmType(t.getReturnType(), apkClasses, refs);
        } else {
            recordAsmType(t, apkClasses, refs);
        }
    }

    private static void recordAsmType(Type t, Map<String, ClassMeta> apkClasses, Map<String, Integer> refs) {
        while (t.getSort() == Type.ARRAY) {
            t = t.getElementType();
        }
        if (t.getSort() == Type.OBJECT) {
            String internal = t.getInternalName();
            if (isSystemNamespace(internal) && !apkClasses.containsKey(internal)) {
                refs.merge(internal, 1, Integer::sum);
            }
        }
    }

    private static String unwrapArrayType(String typeName) {
        if (typeName == null || typeName.isEmpty()) return null;
        if (typeName.startsWith("[")) {
            Type t = Type.getType(typeName);
            while (t.getSort() == Type.ARRAY) {
                t = t.getElementType();
            }
            return t.getSort() == Type.OBJECT ? t.getInternalName() : null;
        }
        return typeName;
    }

    private static boolean isSystemNamespace(String internalName) {
        return internalName.startsWith("android/")
                || internalName.startsWith("androidx/")
                || internalName.startsWith("dalvik/")
                || internalName.startsWith("com/android/")
                || internalName.startsWith("com/google/android/")
                || internalName.startsWith("org/apache/http/")
                || internalName.startsWith("org/json/")
                || internalName.startsWith("org/xmlpull/");
    }

    private static boolean isJdkClass(String internalName) {
        return internalName.startsWith("java/")
                || internalName.startsWith("javax/")
                || internalName.startsWith("sun/")
                || internalName.startsWith("jdk/")
                || internalName.startsWith("org/w3c/")
                || internalName.startsWith("org/xml/");
    }

    private static boolean hasJdkMethod(String internalName, String name, String descriptor) {
        try {
            Class<?> cls = Class.forName(internalName.replace('/', '.'), false, ApkCompatibilityScanner.class.getClassLoader());
            if ("<init>".equals(name)) {
                for (Constructor<?> c : cls.getDeclaredConstructors()) {
                    if (Type.getConstructorDescriptor(c).equals(descriptor)) return true;
                }
                return false;
            }
            for (Method m : cls.getMethods()) {
                if (m.getName().equals(name) && Type.getMethodDescriptor(m).equals(descriptor)) {
                    return true;
                }
            }
            for (Method m : cls.getDeclaredMethods()) {
                if (m.getName().equals(name) && Type.getMethodDescriptor(m).equals(descriptor)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static ClassMeta loadSdkClass(String internalName, ZipFile sdkZip, Map<String, ClassMeta> cache) {
        if (sdkZip == null || internalName == null) return null;
        if (cache.containsKey(internalName)) {
            return cache.get(internalName);
        }
        ZipEntry entry = sdkZip.getEntry(internalName + ".class");
        if (entry == null) {
            cache.put(internalName, null);
            return null;
        }
        try (InputStream in = sdkZip.getInputStream(entry)) {
            ClassMeta meta = readClassMeta(in.readAllBytes());
            cache.put(internalName, meta);
            return meta;
        } catch (IOException e) {
            cache.put(internalName, null);
            return null;
        }
    }

    private static Map<String, ClassMeta> loadApkClasses(ZipFile apkZip) throws IOException {
        Map<String, ClassMeta> map = new HashMap<>();
        Enumeration<? extends ZipEntry> entries = apkZip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement();
            if (!e.getName().endsWith(".class")) continue;
            try (InputStream in = apkZip.getInputStream(e)) {
                byte[] bytes = in.readAllBytes();
                if (bytes.length > 8) {
                    ClassMeta meta = readClassMeta(bytes);
                    if (meta.internalName != null) {
                        map.put(meta.internalName, meta);
                    }
                }
            }
        }
        return map;
    }

    private static Map<String, ClassMeta> loadFrameworkClasses() throws IOException {
        Map<String, ClassMeta> map = new HashMap<>();
        URL loc = HostWindow.class.getProtectionDomain().getCodeSource().getLocation();
        if (loc == null) return map;

        Path codePath;
        try {
            codePath = Path.of(loc.toURI());
        } catch (Exception e) {
            return map;
        }

        if (Files.isDirectory(codePath)) {
            try (Stream<Path> walk = Files.walk(codePath)) {
                for (Path p : walk.filter(f -> f.toString().endsWith(".class")).toList()) {
                    byte[] bytes = Files.readAllBytes(p);
                    ClassMeta meta = readClassMeta(bytes);
                    if (meta.internalName != null && !meta.internalName.startsWith("dev/aaow/")) {
                        map.put(meta.internalName, meta);
                    }
                }
            }
        } else if (Files.isRegularFile(codePath)) {
            try (ZipFile zf = new ZipFile(codePath.toFile())) {
                Enumeration<? extends ZipEntry> entries = zf.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    if (!e.getName().endsWith(".class")) continue;
                    try (InputStream in = zf.getInputStream(e)) {
                        ClassMeta meta = readClassMeta(in.readAllBytes());
                        if (meta.internalName != null && !meta.internalName.startsWith("dev/aaow/")) {
                            map.put(meta.internalName, meta);
                        }
                    }
                }
            }
        }
        return map;
    }

    private static ClassMeta readClassMeta(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        final String[] self = {null};
        final String[] sup = {null};
        final List<String> itfs = new ArrayList<>();
        final Set<String> methods = new HashSet<>();

        cr.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                self[0] = name;
                sup[0] = superName;
                if (interfaces != null) {
                    for (String i : interfaces) itfs.add(i);
                }
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                methods.add(name + descriptor);
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        return new ClassMeta(self[0], sup[0], itfs, methods);
    }

    private static Map<String, Integer> countBundledLibraries(Set<String> apkClassInternalNames) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        String[][] knownPrefixes = {
                {"androidx/", "AndroidX (androidx.*)"},
                {"android/support/", "Android Support Legacy"},
                {"kotlin/", "Kotlin Stdlib"},
                {"kotlinx/", "Kotlinx (Coroutines/...)"},
                {"com/google/android/gms/", "Google Play Services (GMS)"},
                {"com/google/firebase/", "Firebase SDK"},
                {"com/google/android/material/", "Material Components"},
                {"okhttp3/", "OkHttp3"},
                {"retrofit2/", "Retrofit2"},
                {"com/bumptech/glide/", "Glide Image Loader"},
                {"io/reactivex/", "RxJava"},
                {"com/tencent/", "Tencent SDK (Bugly/MMKV/...)"}
        };

        for (String cls : apkClassInternalNames) {
            for (String[] pair : knownPrefixes) {
                if (cls.startsWith(pair[0])) {
                    counts.merge(pair[1], 1, Integer::sum);
                    break;
                }
            }
        }
        return counts;
    }

    private static <T> List<T> limitList(List<T> src, int max) {
        return src.size() <= max ? src : new ArrayList<>(src.subList(0, max));
    }
}
