package dev.aaow.rt;

import android.util.Log;
import dev.aaow.apk.ApkFile;
import dev.aaow.apk.ElfFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý giải nén, phân tích định dạng ELF (.so) và nạp thư viện native (Windows .dll / Android .so).
 * Hỗ trợ ưu tiên kiến trúc x86_64 / arm64-v8a và cung cấp bảng ánh xạ hàm JNI sang bộ thông dịch ARM64.
 */
public final class NativeLibraryManager {
    private static final String TAG = "AAOW-NativeLib";
    private static final List<String> ABI_PRIORITY = List.of(
            "windows-x86_64", "x86_64", "arm64-v8a", "armeabi-v7a", "armeabi", "x86"
    );

    public record NativeLibraryInfo(
            String libName,
            String abi,
            Path extractedPath,
            ElfFile elfFile,
            ElfFile.VirtualImage arm64Image,
            boolean loadedAsWindowsDll
    ) {
        public String describe() {
            if (loadedAsWindowsDll) {
                return libName + " [Windows DLL]";
            }
            if (elfFile != null) {
                return libName + " [" + abi + ", " + elfFile.getMachineName()
                        + ", JNI=" + elfFile.getJniSymbols().size() + "]";
            }
            return libName + " [" + abi + "]";
        }
    }

    private static final Map<String, NativeLibraryInfo> REGISTERED_LIBS = new ConcurrentHashMap<>();
    private static final Map<String, NativeLibraryInfo> LOADED_LIBS = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<String, NativeLibraryInfo> JNI_SYMBOL_INDEX = new ConcurrentHashMap<>();
    private static volatile Path activeNativeLibDir;

    private NativeLibraryManager() {
    }

    /**
     * Giải nén thư viện native (.so / .dll) từ APK ra thư mục nativeLibraryDir trên Windows
     * và lập chỉ mục toàn bộ ký hiệu hàm JNI (Java_*) từ header ELF.
     */
    public static Path prepareForApk(ApkFile apk, Path dataDir) {
        if (apk == null || dataDir == null) return null;
        Path libRoot = dataDir.resolve("lib");
        try {
            Files.createDirectories(libRoot);
        } catch (IOException ignored) {
        }

        List<String> entries = apk.listNativeLibraries(null);
        if (entries.isEmpty()) {
            activeNativeLibDir = libRoot;
            return libRoot;
        }

        // Nhóm theo tên thư viện để chọn ABI tốt nhất theo thứ tự ưu tiên
        Map<String, List<String>> byFileName = new LinkedHashMap<>();
        for (String entry : entries) {
            int lastSlash = entry.lastIndexOf('/');
            String fileName = lastSlash >= 0 ? entry.substring(lastSlash + 1) : entry;
            byFileName.computeIfAbsent(fileName, k -> new ArrayList<>()).add(entry);
        }

        String primaryAbi = pickPrimaryAbi(apk.nativeAbis());
        Path primaryDir = libRoot.resolve(primaryAbi);
        try {
            Files.createDirectories(primaryDir);
        } catch (IOException ignored) {
        }
        activeNativeLibDir = primaryDir;

        for (var e : byFileName.entrySet()) {
            String fileName = e.getKey();
            String chosenEntry = pickBestAbiEntry(e.getValue());
            if (chosenEntry == null) continue;
            String abi = extractAbi(chosenEntry);
            Path outDir = abi.equals(primaryAbi) ? primaryDir : libRoot.resolve(abi);
            try {
                Files.createDirectories(outDir);
                byte[] bytes = apk.read(chosenEntry);
                if (bytes == null) continue;

                Path outPath = outDir.resolve(fileName);
                if (!Files.exists(outPath) || Files.size(outPath) != bytes.length) {
                    Files.write(outPath, bytes);
                }

                // Đặt thêm bản sao ở primaryDir để ứng dụng tìm bằng applicationInfo.nativeLibraryDir luôn thấy
                Path primaryPath = primaryDir.resolve(fileName);
                if (!primaryPath.equals(outPath) && (!Files.exists(primaryPath) || Files.size(primaryPath) != bytes.length)) {
                    Files.write(primaryPath, bytes);
                }

                registerParsedBinary(fileName, abi, outPath, bytes);
            } catch (Exception ex) {
                Log.w(TAG, "Không thể giải nén " + chosenEntry + ": " + ex.getMessage());
            }
        }
        return primaryDir;
    }

    private static void registerParsedBinary(String fileName, String abi, Path extractedPath, byte[] bytes) {
        String shortName = normalizeLibName(fileName);
        ElfFile elf = null;
        ElfFile.VirtualImage arm64Image = null;
        if (ElfFile.isElf(bytes)) {
            try {
                elf = ElfFile.parse(bytes);
                if (elf.getMachine() == ElfFile.EM_AARCH64) {
                    arm64Image = elf.buildVirtualMemoryImage(Arm64JniInterpreter.CODE_BASE_ADDR);
                }
            } catch (Exception ex) {
                Log.w(TAG, "Lỗi phân tích ELF " + fileName + ": " + ex.getMessage());
            }
        }
        NativeLibraryInfo info = new NativeLibraryInfo(shortName, abi, extractedPath, elf, arm64Image, false);
        REGISTERED_LIBS.put(shortName, info);
        REGISTERED_LIBS.put(fileName, info);
        if (elf != null) {
            for (ElfFile.ElfSymbol sym : elf.getJniSymbols()) {
                JNI_SYMBOL_INDEX.putIfAbsent(sym.name(), info);
            }
        }
    }

    public static boolean loadLibrary(String libName) {
        if (libName == null || libName.isEmpty()) return false;
        String key = normalizeLibName(libName);

        // 1. Kiểm tra xem có bản Windows DLL đi kèm không (hỗ trợ native thật trên Windows)
        if (tryLoadWindowsDll(key, libName)) {
            return true;
        }

        // 2. Kiểm tra thư viện .so đã giải nén từ APK
        NativeLibraryInfo info = REGISTERED_LIBS.get(key);
        if (info == null) {
            info = REGISTERED_LIBS.get("lib" + key + ".so");
        }
        if (info != null) {
            LOADED_LIBS.put(key, info);
            String msg = "Đã nạp siêu dữ liệu ELF " + info.describe();
            if (info.elfFile() != null && !info.elfFile().getNeededLibraries().isEmpty()) {
                msg += " | phụ thuộc=" + info.elfFile().getNeededLibraries();
            }
            Log.i(TAG, msg);
            return true;
        }
        return false;
    }

    public static boolean loadFile(String filename) {
        if (filename == null || filename.isEmpty()) return false;
        Path p = Path.of(filename);
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".dll") && Files.isRegularFile(p)) {
            try {
                System.load(p.toAbsolutePath().toString());
                String key = normalizeLibName(p.getFileName().toString());
                NativeLibraryInfo info = new NativeLibraryInfo(key, "windows-x86_64", p, null, null, true);
                LOADED_LIBS.put(key, info);
                Log.i(TAG, "Đã nạp thư viện Windows DLL thật: " + p);
                return true;
            } catch (Throwable t) {
                Log.w(TAG, "Lỗi System.load(" + filename + "): " + t.getMessage());
            }
        }

        String fileName = p.getFileName() != null ? p.getFileName().toString() : filename;
        String key = normalizeLibName(fileName);
        if (REGISTERED_LIBS.containsKey(key)) {
            return loadLibrary(key);
        }
        if (Files.isRegularFile(p)) {
            try {
                byte[] bytes = Files.readAllBytes(p);
                if (ElfFile.isElf(bytes)) {
                    registerParsedBinary(fileName, "external", p, bytes);
                    return loadLibrary(key);
                }
            } catch (IOException ignored) {
            }
        }
        return false;
    }

    private static boolean tryLoadWindowsDll(String key, String rawName) {
        Path dir = activeNativeLibDir;
        if (dir == null) return false;
        Path[] candidates = {
                dir.resolve(rawName + ".dll"),
                dir.resolve("lib" + key + ".dll"),
                dir.getParent() != null ? dir.getParent().resolve("windows-x86_64").resolve(rawName + ".dll") : null,
                dir.getParent() != null ? dir.getParent().resolve("windows-x86_64").resolve(key + ".dll") : null
        };
        for (Path cand : candidates) {
            if (cand != null && Files.isRegularFile(cand)) {
                try {
                    System.load(cand.toAbsolutePath().toString());
                    NativeLibraryInfo info = new NativeLibraryInfo(key, "windows-x86_64", cand, null, null, true);
                    LOADED_LIBS.put(key, info);
                    Log.i(TAG, "Đã nạp thư viện Windows DLL thật: " + cand);
                    return true;
                } catch (Throwable t) {
                    Log.w(TAG, "Nạp Windows DLL thất bại (" + cand + "): " + t.getMessage());
                }
            }
        }
        return false;
    }

    /** Thực thi hàm JNI ARM64 từ các .so đã đăng ký nếu khớp tên Java_* và chỉ là hàm tính toán thuần. */
    public static Object tryInvokeArm64Jni(String className, String methodName, String descriptor, Object[] args) {
        String jniName = ElfFile.toJniSymbolName(className, methodName);
        NativeLibraryInfo info = JNI_SYMBOL_INDEX.get(jniName);
        if (info == null) {
            // Tìm cả phiên bản mangled đầy đủ (nếu overload)
            for (var entry : JNI_SYMBOL_INDEX.entrySet()) {
                if (entry.getKey().startsWith(jniName + "__")) {
                    jniName = entry.getKey();
                    info = entry.getValue();
                    break;
                }
            }
        }
        if (info == null || info.elfFile() == null || info.arm64Image() == null) {
            return NativeBridge.UNHANDLED;
        }
        ElfFile.ElfSymbol sym = info.elfFile().findSymbol(jniName);
        if (sym == null || !sym.isDefined()) {
            return NativeBridge.UNHANDLED;
        }

        Arm64JniInterpreter.ExecutionResult res = Arm64JniInterpreter.executeJni(
                info.arm64Image(), sym, descriptor, args);
        if (res.handled()) {
            return res.returnValue();
        }
        return NativeBridge.UNHANDLED;
    }

    public static Map<String, NativeLibraryInfo> getRegisteredLibraries() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(REGISTERED_LIBS));
    }

    public static List<NativeLibraryInfo> getLoadedLibraries() {
        synchronized (LOADED_LIBS) {
            return new ArrayList<>(LOADED_LIBS.values());
        }
    }

    public static Path getActiveNativeLibDir() {
        return activeNativeLibDir;
    }

    public static void clearSession() {
        REGISTERED_LIBS.clear();
        LOADED_LIBS.clear();
        JNI_SYMBOL_INDEX.clear();
        activeNativeLibDir = null;
    }

    public static String normalizeLibName(String name) {
        String s = name == null ? "" : name.trim();
        if (s.endsWith(".so")) s = s.substring(0, s.length() - 3);
        else if (s.endsWith(".dll")) s = s.substring(0, s.length() - 4);
        if (s.startsWith("lib") && s.length() > 3) s = s.substring(3);
        return s;
    }

    private static String pickPrimaryAbi(List<String> abis) {
        for (String pref : ABI_PRIORITY) {
            if (abis.contains(pref)) return pref;
        }
        return abis.isEmpty() ? "x86_64" : abis.get(0);
    }

    private static String pickBestAbiEntry(List<String> entries) {
        for (String pref : ABI_PRIORITY) {
            String prefix = "lib/" + pref + "/";
            for (String e : entries) {
                if (e.startsWith(prefix)) return e;
            }
        }
        return entries.isEmpty() ? null : entries.get(0);
    }

    private static String extractAbi(String entry) {
        if (!entry.startsWith("lib/")) return "unknown";
        int slash = entry.indexOf('/', 4);
        return slash > 4 ? entry.substring(4, slash) : "unknown";
    }
}
