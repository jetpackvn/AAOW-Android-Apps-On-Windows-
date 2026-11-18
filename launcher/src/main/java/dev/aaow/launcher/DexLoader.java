package dev.aaow.launcher;

import com.googlecode.d2j.dex.Dex2jar;
import com.googlecode.d2j.reader.MultiDexFileReader;
import dev.aaow.apk.ApkFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Dịch các file classes*.dex (Dalvik/ART bytecode) trong APK sang bytecode JVM,
 * chuẩn hóa classfile về V1_6, chặn/nạp thư viện native qua NativeBridge,
 * điều phối các phương thức native qua NativeBridge.invokeNative(...) (HLE / ARM64 / Smart Default),
 * và nạp qua ApkDexClassLoader hỗ trợ microG-lite + tổng hợp lớp plugin tùy chọn.
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
        Map<String, ApkDexClassLoader.RefTypeMeta> externalRefs = ApkDexClassLoader.scanExternalReferences(jarFile);
        return new ApkDexClassLoader(jarFile.toUri().toURL(), getSdkFallbackParent(parent), externalRefs);
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
        String cacheKey = String.format("%s-%d-%d-v7.jar",
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

    /**
     * Biến đổi class bytecode của APK (và cũng dùng trực tiếp cho unit test native dispatch):
     * - Chuẩn hóa modifier flags & version V1_6 cho JVM HotSpot.
     * - Chặn System.loadLibrary / System.load sang NativeBridge.
     * - Bọc mọi phương thức native qua NativeBridge.invokeNative(...) với fallback Smart Default Return.
     */
    public static byte[] rewriteApkClassForWindows(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            private String currentInternal = "";
            private String currentClass = "";

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.currentInternal = name != null ? name : "";
                this.currentClass = this.currentInternal.replace('/', '.');
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
                    int cleanAccess = access & ~Opcodes.ACC_NATIVE;
                    MethodVisitor mv = super.visitMethod(cleanAccess, name, descriptor, signature, exceptions);
                    emitNativeBridgeMethod(mv, currentInternal, currentClass, cleanAccess, name, descriptor);
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

    private static void emitNativeBridgeMethod(MethodVisitor mv, String currentInternal, String currentClass,
                                               int cleanAccess, String methodName, String descriptor) {
        boolean isStatic = (cleanAccess & Opcodes.ACC_STATIC) != 0;
        Type[] argTypes = Type.getArgumentTypes(descriptor);
        Type retType = Type.getReturnType(descriptor);

        mv.visitCode();
        mv.visitLdcInsn(currentClass + "." + methodName + descriptor);
        if (isStatic) {
            mv.visitInsn(Opcodes.ACONST_NULL);
        } else {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
        }

        AndroidSdkFallbackClassLoader.pushIntConstant(mv, argTypes.length);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        int localIdx = isStatic ? 0 : 1;
        for (int i = 0; i < argTypes.length; i++) {
            Type t = argTypes[i];
            mv.visitInsn(Opcodes.DUP);
            AndroidSdkFallbackClassLoader.pushIntConstant(mv, i);
            mv.visitVarInsn(t.getOpcode(Opcodes.ILOAD), localIdx);
            emitBoxIfPrimitive(mv, t);
            mv.visitInsn(Opcodes.AASTORE);
            localIdx += t.getSize();
        }

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "dev/aaow/rt/NativeBridge",
                "invokeNative", "(Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        int resLocal = localIdx;
        mv.visitVarInsn(Opcodes.ASTORE, resLocal);

        Label unhandledLabel = new Label();
        mv.visitVarInsn(Opcodes.ALOAD, resLocal);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "dev/aaow/rt/NativeBridge", "UNHANDLED", "Ljava/lang/Object;");
        mv.visitJumpInsn(Opcodes.IF_ACMPEQ, unhandledLabel);

        emitUnboxAndReturn(mv, retType, resLocal);

        mv.visitLabel(unhandledLabel);
        AndroidSdkFallbackClassLoader.emitSmartDefaultReturn(mv, retType, currentInternal, isStatic);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void emitBoxIfPrimitive(MethodVisitor mv, Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            case Type.BYTE -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
            case Type.CHAR -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
            case Type.SHORT -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            case Type.INT -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            case Type.LONG -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            case Type.FLOAT -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            case Type.DOUBLE -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            default -> {
            }
        }
    }

    private static void emitUnboxAndReturn(MethodVisitor mv, Type retType, int resLocal) {
        int sort = retType.getSort();
        if (sort == Type.VOID) {
            mv.visitInsn(Opcodes.RETURN);
            return;
        }
        if (sort >= Type.BOOLEAN && sort <= Type.DOUBLE) {
            Label nonNull = new Label();
            mv.visitVarInsn(Opcodes.ALOAD, resLocal);
            mv.visitJumpInsn(Opcodes.IFNONNULL, nonNull);
            AndroidSdkFallbackClassLoader.pushDefaultValue(mv, retType);
            mv.visitInsn(retType.getOpcode(Opcodes.IRETURN));

            mv.visitLabel(nonNull);
            mv.visitVarInsn(Opcodes.ALOAD, resLocal);
            switch (sort) {
                case Type.BOOLEAN -> {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                }
                case Type.CHAR -> {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
                }
                case Type.BYTE -> {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Number");
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "byteValue", "()B", false);
                }
                case Type.SHORT -> {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Number");
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "shortValue", "()S", false);
                }
                case Type.INT -> {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Number");
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false);
                }
                case Type.LONG -> {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Number");
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J", false);
                }
                case Type.FLOAT -> {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Number");
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F", false);
                }
                case Type.DOUBLE -> {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Number");
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
                }
            }
            mv.visitInsn(retType.getOpcode(Opcodes.IRETURN));
            return;
        }

        mv.visitVarInsn(Opcodes.ALOAD, resLocal);
        String target = retType.getInternalName();
        if (!"java/lang/Object".equals(target)) {
            mv.visitTypeInsn(Opcodes.CHECKCAST, target);
        }
        mv.visitInsn(Opcodes.ARETURN);
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
