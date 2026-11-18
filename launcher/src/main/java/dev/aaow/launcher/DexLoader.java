package dev.aaow.launcher;

import com.googlecode.d2j.dex.Dex2jar;
import com.googlecode.d2j.reader.MultiDexFileReader;
import dev.aaow.apk.ApkFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Dịch các file classes*.dex (Dalvik/ART bytecode) trong APK sang bytecode JVM,
 * chuẩn hóa classfile về V1_6, chặn lệnh nạp .so ARM (System.loadLibrary) và
 * bọc các phương thức JNI native để không gây UnsatisfiedLinkError trên Windows.
 */
public final class DexLoader {
    private static AndroidSdkFallbackClassLoader sharedSdkFallback;

    private DexLoader() {
    }

    private static synchronized ClassLoader getSdkFallbackParent(ClassLoader baseParent) {
        if (sharedSdkFallback == null) {
            sharedSdkFallback = new AndroidSdkFallbackClassLoader(baseParent);
        }
        return sharedSdkFallback;
    }

    public static ClassLoader loadApkDex(ApkFile apk, Path cacheDir, ClassLoader parent) throws IOException {
        Path jarFile = ensureDexJar(apk, cacheDir);
        return new URLClassLoader(new URL[]{jarFile.toUri().toURL()}, getSdkFallbackParent(parent));
    }

    /**
     * Dịch toàn bộ classes*.dex trong APK ra file JAR JVM đã chuẩn hóa (có lưu đệm).
     * Dùng chung cho cả việc chạy APK và công cụ quét tương thích tĩnh ApkCompatibilityScanner.
     */
    public static Path ensureDexJar(ApkFile apk, Path cacheDir) throws IOException {
        Files.createDirectories(cacheDir);
        Path apkPath = apk.getPath();
        long size = Files.size(apkPath);
        long mtime = Files.getLastModifiedTime(apkPath).toMillis();
        String cacheKey = String.format("%s-%d-%d-v6.jar",
                apkPath.getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_"), size, mtime);
        Path jarFile = cacheDir.resolve(cacheKey);

        if (!Files.exists(jarFile)) {
            List<byte[]> dexFiles = apk.readDexFiles();
            if (dexFiles.isEmpty()) {
                throw new IOException("APK không chứa classes.dex");
            }
            byte[] combinedInput = dexFiles.size() == 1 ? dexFiles.get(0) : packMultiDexZip(dexFiles);
            long t0 = System.currentTimeMillis();
            Path rawJar = cacheDir.resolve(cacheKey + ".raw.jar");
            Dex2jar.from(MultiDexFileReader.open(combinedInput))
                    .reUseReg(false)
                    .topoLogicalSort()
                    .skipDebug(false)
                    .optimizeSynchronized(false)
                    .to(rawJar);
            normalizeJarForJvmVerifier(rawJar, jarFile);
            Files.deleteIfExists(rawJar);
            long elapsed = System.currentTimeMillis() - t0;
            System.out.printf("[AAOW-DEX] Đã dịch %d file DEX -> JVM bytecode (%s) trong %d ms%n",
                    dexFiles.size(), jarFile.getFileName(), elapsed);
        } else {
            System.out.printf("[AAOW-DEX] Dùng bản dịch DEX đã lưu đệm: %s%n", jarFile.getFileName());
        }
        return jarFile;
    }

    private static void normalizeJarForJvmVerifier(Path rawJar, Path outJar) throws IOException {
        try (ZipFile zf = new ZipFile(rawJar.toFile());
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(outJar))) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                byte[] bytes;
                try (InputStream in = zf.getInputStream(e)) {
                    bytes = in.readAllBytes();
                }
                if (e.getName().endsWith(".class") && bytes.length > 8) {
                    bytes = rewriteApkClassForWindows(bytes);
                }
                zos.putNextEntry(new ZipEntry(e.getName()));
                zos.write(bytes);
                zos.closeEntry();
            }
        }
    }

    private static byte[] rewriteApkClassForWindows(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            private String currentInternal = "";
            private String currentClass = "";

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.currentInternal = name != null ? name : "";
                this.currentClass = this.currentInternal.replace('/', '.');
                // JVM ClassFile không cho phép ACC_STATIC/ACC_PRIVATE/ACC_PROTECTED trên class header hay ACC_ABSTRACT | ACC_FINAL
                int cleanAccess = access & ~(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED);
                if ((cleanAccess & Opcodes.ACC_ABSTRACT) != 0) {
                    cleanAccess &= ~Opcodes.ACC_FINAL;
                }
                super.visit(Opcodes.V1_6, cleanAccess, name, signature, superName, interfaces);
            }

            @Override
            public void visitInnerClass(String name, String outerName, String innerName, int access) {
                int cleanAccess = access;
                if ((cleanAccess & Opcodes.ACC_ABSTRACT) != 0) {
                    cleanAccess &= ~Opcodes.ACC_FINAL;
                }
                super.visitInnerClass(name, outerName, innerName, cleanAccess);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if ((access & Opcodes.ACC_NATIVE) != 0) {
                    // Biến phương thức JNI native thành phương thức Java an toàn ghi nhận log
                    int cleanAccess = access & ~Opcodes.ACC_NATIVE;
                    MethodVisitor mv = super.visitMethod(cleanAccess, name, descriptor, signature, exceptions);
                    mv.visitCode();
                    mv.visitLdcInsn(currentClass + "." + name + descriptor);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "dev/aaow/rt/NativeBridge",
                            "onNativeCall", "(Ljava/lang/String;)V", false);
                    boolean isStatic = (cleanAccess & Opcodes.ACC_STATIC) != 0;
                    AndroidSdkFallbackClassLoader.emitSmartDefaultReturn(
                            mv, Type.getReturnType(descriptor), currentInternal, isStatic);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                    return null;
                }

                MethodVisitor baseMv = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, baseMv) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String mName, String mDesc, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC && "java/lang/System".equals(owner)) {
                            if ("loadLibrary".equals(mName) && "(Ljava/lang/String;)V".equals(mDesc)) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "dev/aaow/rt/NativeBridge",
                                        "loadLibrary", "(Ljava/lang/String;)V", false);
                                return;
                            } else if ("load".equals(mName) && "(Ljava/lang/String;)V".equals(mDesc)) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "dev/aaow/rt/NativeBridge",
                                        "load", "(Ljava/lang/String;)V", false);
                                return;
                            }
                        }
                        super.visitMethodInsn(opcode, owner, mName, mDesc, isInterface);
                    }
                };
            }
        };
        cr.accept(cv, ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    private static byte[] packMultiDexZip(List<byte[]> dexFiles) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (int i = 0; i < dexFiles.size(); i++) {
                String name = i == 0 ? "classes.dex" : "classes" + (i + 1) + ".dex";
                zos.putNextEntry(new ZipEntry(name));
                zos.write(dexFiles.get(i));
                zos.closeEntry();
            }
        }
        return bos.toByteArray();
    }
}
