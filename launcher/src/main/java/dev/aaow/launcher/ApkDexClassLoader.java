package dev.aaow.launcher;

import dev.aaow.rt.NativeBridge;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ClassLoader dành riêng cho một APK đã dịch DEX -> JVM:
 * 1. Các gói hệ thống (java.*, android.*, dalvik.*, dev.aaow.*, libcore.*, javafx.*, org.json.*, org.sqlite.*)
 *    luôn ủy quyền lên parent (:framework + AndroidSdkFallbackClassLoader + JDK).
 * 2. Các lớp còn lại (code của app, androidx.*, kotlin.*, com.google.*) ưu tiên tìm trong DEX JAR của APK trước.
 * 3. Nếu lớp thiếu thuộc gói microG-lite (com.google.firebase.*, com.google.android.gms.*, org.conscrypt.*,
 *    org.apache.commons.logging.*, com.jakewharton.disklrucache.*), tự nạp từ dev/aaow/microg-lite.jar
 *    và định nghĩa NGAY TRONG ApkDexClassLoader này để tránh LinkageError với lớp đi kèm của APK.
 * 4. Nếu lớp thiếu là plugin tùy chọn được bytecode APK tham chiếu tĩnh (không phải probe Class.forName),
 *    tổng hợp stub an toàn qua ASM để JVM verifier không làm sập toàn bộ Activity/Application.
 */
public final class ApkDexClassLoader extends URLClassLoader {
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    static final class RefTypeMeta {
        final String internalName;
        boolean mustBeInterface;
        boolean mustBeClass;
        final Map<String, String> fields = new LinkedHashMap<>();       // name -> desc
        final Set<String> staticFields = new LinkedHashSet<>();
        final Map<String, String> methods = new LinkedHashMap<>();      // name+desc -> name
        final Set<String> staticMethods = new LinkedHashSet<>();

        RefTypeMeta(String internalName) {
            this.internalName = internalName;
        }
    }

    private final Map<String, byte[]> microgBytecode;
    private final Map<String, RefTypeMeta> externalTypeRefs;

    public ApkDexClassLoader(URL dexJarUrl, ClassLoader parent, Map<String, RefTypeMeta> externalTypeRefs) {
        super(new URL[]{dexJarUrl}, parent);
        this.externalTypeRefs = externalTypeRefs != null ? externalTypeRefs : Map.of();
        this.microgBytecode = loadMicrogArchive(parent);
    }

    private static Map<String, byte[]> loadMicrogArchive(ClassLoader parent) {
        Map<String, byte[]> map = new HashMap<>();
        try (InputStream in = parent != null
                ? parent.getResourceAsStream("dev/aaow/microg-lite.jar")
                : ApkDexClassLoader.class.getResourceAsStream("/dev/aaow/microg-lite.jar")) {
            if (in == null) return map;
            try (ZipInputStream zis = new ZipInputStream(in)) {
                ZipEntry e;
                while ((e = zis.getNextEntry()) != null) {
                    if (!e.isDirectory() && e.getName().endsWith(".class")) {
                        map.put(e.getName(), zis.readAllBytes());
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return map;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded != null) {
                if (resolve) resolveClass(loaded);
                return loaded;
            }

            if (isParentFirstSystemPackage(name)) {
                try {
                    Class<?> sys = getParent().loadClass(name);
                    if (resolve) resolveClass(sys);
                    return sys;
                } catch (ClassNotFoundException ignored) {
                }
            }

            // Ưu tiên nạp class từ chính DEX JAR của APK trước
            try {
                Class<?> fromDex = super.findClass(name);
                if (resolve) resolveClass(fromDex);
                return fromDex;
            } catch (ClassNotFoundException ignored) {
            }

            // Kế tiếp: kiểm tra kho fallback microG-lite (chỉ nạp khi APK không đóng gói lớp này)
            String entryName = name.replace('.', '/') + ".class";
            byte[] microgBytes = microgBytecode.get(entryName);
            if (microgBytes != null) {
                Class<?> defined = defineClass(name, microgBytes, 0, microgBytes.length);
                if (resolve) resolveClass(defined);
                return defined;
            }

            // Ủy quyền lên parent (:framework / AndroidSdkFallbackClassLoader / JDK)
            try {
                Class<?> fromParent = getParent().loadClass(name);
                if (resolve) resolveClass(fromParent);
                return fromParent;
            } catch (ClassNotFoundException e) {
                // Nếu bytecode APK có tham chiếu trực tiếp (không phải gọi Class.forName thăm dò), tổng hợp stub an toàn
                String internal = name.replace('.', '/');
                RefTypeMeta meta = externalTypeRefs.get(internal);
                if (meta != null && !isDirectClassForNameProbe()) {
                    byte[] stubBytes = synthesizePluginStub(meta);
                    NativeBridge.recordStubbedPluginClass(name);
                    Class<?> stubbed = defineClass(name, stubBytes, 0, stubBytes.length);
                    if (resolve) resolveClass(stubbed);
                    return stubbed;
                }
                throw e;
            }
        }
    }

    private static boolean isParentFirstSystemPackage(String name) {
        return name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("sun.")
                || name.startsWith("jdk.")
                || name.startsWith("android.")
                || name.startsWith("dalvik.")
                || name.startsWith("dev.aaow.")
                || name.startsWith("libcore.")
                || name.startsWith("javafx.")
                || name.startsWith("org.json.")
                || name.startsWith("org.xml.")
                || name.startsWith("org.w3c.")
                || name.startsWith("org.xmlpull.")
                || name.startsWith("org.apache.http.")
                || name.startsWith("org.sqlite.");
    }

    /**
     * Phân biệt lệnh thăm dò Class.forName("com.missing.Lib") trực tiếp
     * với quá trình JVM verifier/linker nạp lớp tham chiếu tĩnh trong bytecode APK.
     */
    private static boolean isDirectClassForNameProbe() {
        return STACK_WALKER.walk(stream -> stream
                .skip(1)
                .filter(f -> {
                    Class<?> cls = f.getDeclaringClass();
                    return cls != ApkDexClassLoader.class
                            && cls != ClassLoader.class
                            && cls != URLClassLoader.class
                            && !"java.security.SecureClassLoader".equals(cls.getName());
                })
                .findFirst()
                .map(f -> f.getDeclaringClass() == Class.class && f.getMethodName().startsWith("forName"))
                .orElse(false));
    }

    private static byte[] synthesizePluginStub(RefTypeMeta meta) {
        boolean isInterface = meta.mustBeInterface && !meta.mustBeClass;
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        int access = Opcodes.ACC_PUBLIC | (isInterface ? (Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT) : 0);
        cw.visit(Opcodes.V1_6, access, meta.internalName, null, "java/lang/Object", null);

        if (!isInterface) {
            MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            init.visitCode();
            init.visitVarInsn(Opcodes.ALOAD, 0);
            init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            init.visitInsn(Opcodes.RETURN);
            init.visitMaxs(0, 0);
            init.visitEnd();
        }

        for (var f : meta.fields.entrySet()) {
            String fName = f.getKey();
            String fDesc = f.getValue();
            int fAcc = Opcodes.ACC_PUBLIC | (meta.staticFields.contains(fName) ? Opcodes.ACC_STATIC : 0);
            if (isInterface) fAcc |= Opcodes.ACC_STATIC | Opcodes.ACC_FINAL;
            FieldVisitor fv = cw.visitField(fAcc, fName, fDesc, null, null);
            if (fv != null) fv.visitEnd();
        }

        for (var m : meta.methods.entrySet()) {
            String key = m.getKey();
            String mName = m.getValue();
            if ("<init>()V".equals(key)) continue;
            if ("<init>".equals(mName) && isInterface) continue;
            if ("<clinit>".equals(mName)) continue;
            String mDesc = key.substring(mName.length());
            boolean isStatic = meta.staticMethods.contains(key);
            int mAcc = Opcodes.ACC_PUBLIC | (isStatic ? Opcodes.ACC_STATIC : 0);
            if (isInterface) {
                mAcc |= Opcodes.ACC_ABSTRACT;
                MethodVisitor mv = cw.visitMethod(mAcc, mName, mDesc, null, null);
                if (mv != null) mv.visitEnd();
            } else {
                MethodVisitor mv = cw.visitMethod(mAcc, mName, mDesc, null, null);
                mv.visitCode();
                if ("<init>".equals(mName)) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                    mv.visitInsn(Opcodes.RETURN);
                } else {
                    AndroidSdkFallbackClassLoader.emitSmartDefaultReturn(
                            mv, Type.getReturnType(mDesc), meta.internalName, isStatic);
                }
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** Quét trước các lớp ngoại lai mà DEX JAR tham chiếu tĩnh để phục vụ tổng hợp stub an toàn lúc verify. */
    static Map<String, RefTypeMeta> scanExternalReferences(java.nio.file.Path jarPath) {
        Map<String, RefTypeMeta> refs = new HashMap<>();
        Set<String> definedInJar = new LinkedHashSet<>();
        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(jarPath.toFile())) {
            var en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.getName().endsWith(".class")) {
                    definedInJar.add(e.getName().substring(0, e.getName().length() - 6));
                }
            }
            en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (!e.getName().endsWith(".class")) continue;
                try (InputStream in = zf.getInputStream(e)) {
                    new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public void visit(int v, int acc, String name, String sig, String superName, String[] itfs) {
                            if (superName != null && !isBuiltInInternal(superName)) {
                                refs.computeIfAbsent(superName, RefTypeMeta::new).mustBeClass = true;
                            }
                            if (itfs != null) {
                                for (String itf : itfs) {
                                    if (itf != null && !isBuiltInInternal(itf)) {
                                        refs.computeIfAbsent(itf, RefTypeMeta::new).mustBeInterface = true;
                                    }
                                }
                            }
                        }

                        @Override
                        public MethodVisitor visitMethod(int acc, String mName, String mDesc, String sig, String[] ex) {
                            return new MethodVisitor(Opcodes.ASM9) {
                                @Override
                                public void visitTypeInsn(int opcode, String type) {
                                    String clean = unwrap(type);
                                    if (clean != null && !isBuiltInInternal(clean)) {
                                        RefTypeMeta meta = refs.computeIfAbsent(clean, RefTypeMeta::new);
                                        if (opcode == Opcodes.NEW) meta.mustBeClass = true;
                                    }
                                }

                                @Override
                                public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                                    String clean = unwrap(owner);
                                    if (clean != null && !isBuiltInInternal(clean)) {
                                        RefTypeMeta meta = refs.computeIfAbsent(clean, RefTypeMeta::new);
                                        meta.fields.putIfAbsent(name, desc);
                                        if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {
                                            meta.staticFields.add(name);
                                        }
                                    }
                                }

                                @Override
                                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isItf) {
                                    String clean = unwrap(owner);
                                    if (clean != null && !isBuiltInInternal(clean)) {
                                        RefTypeMeta meta = refs.computeIfAbsent(clean, RefTypeMeta::new);
                                        if (isItf || opcode == Opcodes.INVOKEINTERFACE) meta.mustBeInterface = true;
                                        else if (opcode == Opcodes.INVOKESPECIAL || opcode == Opcodes.INVOKEVIRTUAL) {
                                            meta.mustBeClass = true;
                                        }
                                        String key = name + desc;
                                        meta.methods.putIfAbsent(key, name);
                                        if (opcode == Opcodes.INVOKESTATIC) {
                                            meta.staticMethods.add(key);
                                        }
                                    }
                                }
                            };
                        }
                    }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }
        } catch (IOException ignored) {
        }
        refs.keySet().removeAll(definedInJar);
        return refs;
    }

    private static String unwrap(String t) {
        if (t == null || t.isEmpty()) return null;
        if (t.startsWith("[")) {
            Type parsed = Type.getType(t);
            if (parsed.getElementType().getSort() == Type.OBJECT) {
                return parsed.getElementType().getInternalName();
            }
            return null;
        }
        return t;
    }

    private static boolean isBuiltInInternal(String internal) {
        return internal.startsWith("java/")
                || internal.startsWith("javax/")
                || internal.startsWith("sun/")
                || internal.startsWith("jdk/")
                || internal.startsWith("android/")
                || internal.startsWith("dalvik/")
                || internal.startsWith("dev/aaow/")
                || internal.startsWith("libcore/")
                || internal.startsWith("javafx/")
                || internal.startsWith("org/json/")
                || internal.startsWith("org/xml")
                || internal.startsWith("org/w3c/")
                || internal.startsWith("org/apache/http/")
                || internal.startsWith("org/sqlite/");
    }
}
