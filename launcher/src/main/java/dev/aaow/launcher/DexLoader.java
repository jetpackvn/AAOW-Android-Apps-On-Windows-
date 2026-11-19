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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
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
        Map<String, ApkDexClassLoader.RefTypeMeta> externalRefs = ApkDexClassLoader.loadOrScanExternalReferences(jarFile);
        return new ApkDexClassLoader(jarFile.toUri().toURL(), getSdkFallbackParent(parent), externalRefs);
    }

    private record RawClassEntry(String internalName, byte[] rawBytes) {
    }

    private record ProcessedClassEntry(
            String internalName,
            byte[] transformedBytes,
            Map<String, ApkDexClassLoader.RefTypeMeta> externalRefs
    ) {
    }

    /**
     * Dịch toàn bộ classes*.dex trong APK ra file JAR JVM đã chuẩn hóa (có lưu đệm + chỉ mục `.refs`).
     * Dùng dịch trực tiếp trong bộ nhớ + biến đổi ASM song song đa nhân để tăng tốc tối đa.
     */
    public static Path ensureDexJar(ApkFile apk, Path cacheDir) throws IOException {
        Files.createDirectories(cacheDir);
        Path apkPath = apk.getPath();
        long size = Files.size(apkPath);
        long mtime = Files.getLastModifiedTime(apkPath).toMillis();
        String cacheKey = String.format("%s-%d-%d-v12.jar",
                apkPath.getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_"), size, mtime);
        Path jarFile = cacheDir.resolve(cacheKey);
        Path refsFile = cacheDir.resolve(cacheKey + ".refs");

        if (!Files.exists(jarFile)) {
            List<byte[]> dexFiles = apk.readDexFiles();
            if (dexFiles.isEmpty()) {
                throw new IOException("APK không chứa classes.dex");
            }
            byte[] combinedInput = dexFiles.size() == 1 ? dexFiles.get(0) : packMultiDexZip(dexFiles);
            long t0 = System.currentTimeMillis();

            DexR8Sanitizer.SanitizedReader sanitized = DexR8Sanitizer.sanitize(MultiDexFileReader.open(combinedInput));
            DexR8Sanitizer.SanitizeStats r8Stats = sanitized.stats();
            combinedInput = null; // Giải phóng mảng DEX gốc trước khi dịch
            int dexCount = dexFiles.size();
            dexFiles = null;

            ByteArrayOutputStream rawStream = new ByteArrayOutputStream(65536);
            Dex2jar.from(sanitized.reader())
                    .reUseReg(false)
                    .topoLogicalSort()
                    .skipDebug(false)
                    .optimizeSynchronized(false)
                    .doTranslate(rawStream);
            sanitized = null; // Giải phóng DexFileNode hoàn toàn trước bước biến đổi ASM song song

            byte[] translatedBytes = rawStream.toByteArray();
            rawStream = null;
            List<RawClassEntry> rawEntries = parseTranslatedStream(translatedBytes);
            translatedBytes = null;

            List<ProcessedClassEntry> processed = rawEntries.parallelStream()
                    .map(entry -> {
                        Map<String, ApkDexClassLoader.RefTypeMeta> localRefs = new HashMap<>();
                        byte[] rewritten = entry.rawBytes().length > 8
                                ? rewriteAndScanClass(entry.rawBytes(), localRefs)
                                : entry.rawBytes();
                        return new ProcessedClassEntry(entry.internalName(), rewritten, localRefs);
                    })
                    .toList();
            rawEntries = null;

            Set<String> definedInJar = new LinkedHashSet<>(processed.size() * 2);
            Map<String, ApkDexClassLoader.RefTypeMeta> mergedRefs = new HashMap<>();
            Path tmpJar = cacheDir.resolve(cacheKey + ".tmp");
            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(tmpJar), 65536))) {
                zos.setLevel(Deflater.BEST_SPEED);
                for (ProcessedClassEntry entry : processed) {
                    definedInJar.add(entry.internalName());
                    zos.putNextEntry(new ZipEntry(entry.internalName() + ".class"));
                    zos.write(entry.transformedBytes());
                    zos.closeEntry();
                    for (var refEntry : entry.externalRefs().entrySet()) {
                        mergedRefs.computeIfAbsent(refEntry.getKey(), ApkDexClassLoader.RefTypeMeta::new)
                                .mergeFrom(refEntry.getValue());
                    }
                }
            }
            Files.move(tmpJar, jarFile, StandardCopyOption.REPLACE_EXISTING);

            mergedRefs.keySet().removeAll(definedInJar);
            ApkDexClassLoader.writeRefsIndex(refsFile, mergedRefs);

            long elapsed = System.currentTimeMillis() - t0;
            System.out.printf("[AAOW-DEX] Đã dịch %d file DEX (%d classes, R8 ctor fix=%d calls/%d ctors) -> %s trong %d ms%n",
                    dexCount, processed.size(),
                    r8Stats.rewrittenCallSites(), r8Stats.synthesizedConstructors(),
                    jarFile.getFileName(), elapsed);
        } else {
            System.out.printf("[AAOW-DEX] Dùng bản dịch DEX đã lưu đệm: %s%n", jarFile.getFileName());
        }
        return jarFile;
    }

    private static List<RawClassEntry> parseTranslatedStream(byte[] data) throws IOException {
        List<RawClassEntry> entries = new ArrayList<>();
        Set<String> seenNames = new LinkedHashSet<>();
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            while (dis.available() > 0) {
                int nameLen;
                try {
                    nameLen = dis.readInt();
                } catch (EOFException eof) {
                    break;
                }
                if (nameLen <= 0 || nameLen > 8192) {
                    throw new IOException("Invalid class name length in Dex2jar stream: " + nameLen);
                }
                byte[] nameBytes = dis.readNBytes(nameLen);
                if (nameBytes.length != nameLen) break;
                int bodyLen = dis.readInt();
                if (bodyLen < 0) {
                    throw new IOException("Invalid class body length in Dex2jar stream: " + bodyLen);
                }
                byte[] bodyBytes = dis.readNBytes(bodyLen);
                if (bodyBytes.length != bodyLen) break;
                String internalName = new String(nameBytes, StandardCharsets.UTF_8);
                if (internalName.endsWith(".class")) {
                    internalName = internalName.substring(0, internalName.length() - 6);
                }
                if (!internalName.isEmpty() && seenNames.add(internalName)) {
                    entries.add(new RawClassEntry(internalName, bodyBytes));
                }
            }
        }
        return entries;
    }

    /**
     * Biến đổi class bytecode của APK (và cũng dùng trực tiếp cho unit test native dispatch):
     * - Chuẩn hóa modifier flags & version V1_6 cho JVM HotSpot.
     * - Chặn System.loadLibrary / System.load sang NativeBridge.
     * - Bọc mọi phương thức native qua NativeBridge.invokeNative(...) với fallback Smart Default Return.
     */
    public static byte[] rewriteApkClassForWindows(byte[] classBytes) {
        return rewriteAndScanClass(classBytes, null);
    }

    private static byte[] rewriteAndScanClass(
            byte[] classBytes,
            Map<String, ApkDexClassLoader.RefTypeMeta> externalRefs) {
        ClassReader cr = new ClassReader(classBytes);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            private String currentInternal = "";
            private String currentClass = "";

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.currentInternal = name != null ? name : "";
                this.currentClass = this.currentInternal.replace('/', '.');
                if (externalRefs != null) {
                    if (superName != null && !ApkDexClassLoader.isBuiltInInternal(superName)) {
                        externalRefs.computeIfAbsent(superName, ApkDexClassLoader.RefTypeMeta::new).mustBeClass = true;
                    }
                    if (interfaces != null) {
                        for (String itf : interfaces) {
                            if (itf != null && !ApkDexClassLoader.isBuiltInInternal(itf)) {
                                externalRefs.computeIfAbsent(itf, ApkDexClassLoader.RefTypeMeta::new).mustBeInterface = true;
                            }
                        }
                    }
                }
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

                // Định tuyến kiểm tra sẵn sàng GMS của play-services-basement sang AAOW microG-lite
                if ("com/google/android/gms/common/GooglePlayServicesUtilLight".equals(currentInternal)
                        && "isGooglePlayServicesAvailable".equals(name)
                        && "(Landroid/content/Context;I)I".equals(descriptor)
                        && (access & Opcodes.ACC_STATIC) != 0) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    mv.visitCode();
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitVarInsn(Opcodes.ILOAD, 1);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                            "dev/aaow/rt/microg/MicroGServiceRegistry",
                            "isGooglePlayServicesAvailable",
                            "(Landroid/content/Context;I)I", false);
                    mv.visitInsn(Opcodes.IRETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                    return null;
                }

                MethodVisitor baseMv = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, baseMv) {
                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        if (externalRefs != null) {
                            String clean = ApkDexClassLoader.unwrap(type);
                            if (clean != null && !ApkDexClassLoader.isBuiltInInternal(clean)) {
                                var meta = externalRefs.computeIfAbsent(clean, ApkDexClassLoader.RefTypeMeta::new);
                                if (opcode == Opcodes.NEW) meta.mustBeClass = true;
                            }
                        }
                        super.visitTypeInsn(opcode, type);
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String fName, String fDesc) {
                        if (externalRefs != null) {
                            String clean = ApkDexClassLoader.unwrap(owner);
                            if (clean != null && !ApkDexClassLoader.isBuiltInInternal(clean)) {
                                var meta = externalRefs.computeIfAbsent(clean, ApkDexClassLoader.RefTypeMeta::new);
                                meta.fields.putIfAbsent(fName, fDesc);
                                if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {
                                    meta.staticFields.add(fName);
                                }
                            }
                        }
                        super.visitFieldInsn(opcode, owner, fName, fDesc);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String mName, String mDesc, boolean isInterface) {
                        if (externalRefs != null) {
                            String clean = ApkDexClassLoader.unwrap(owner);
                            if (clean != null && !ApkDexClassLoader.isBuiltInInternal(clean)) {
                                var meta = externalRefs.computeIfAbsent(clean, ApkDexClassLoader.RefTypeMeta::new);
                                if (isInterface || opcode == Opcodes.INVOKEINTERFACE) meta.mustBeInterface = true;
                                else if (opcode == Opcodes.INVOKESPECIAL || opcode == Opcodes.INVOKEVIRTUAL) {
                                    meta.mustBeClass = true;
                                }
                                String key = mName + mDesc;
                                meta.methods.putIfAbsent(key, mName);
                                if (opcode == Opcodes.INVOKESTATIC) {
                                    meta.staticMethods.add(key);
                                }
                            }
                        }
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
                        if (opcode == Opcodes.INVOKESPECIAL
                                && "<init>".equals(mName)
                                && ("java/io/FileReader".equals(owner) || "java/io/FileInputStream".equals(owner))) {
                            if ("(Ljava/lang/String;)V".equals(mDesc)) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "dev/aaow/rt/LinuxProcFsBridge",
                                        "remapPath", "(Ljava/lang/String;)Ljava/lang/String;", false);
                            } else if ("(Ljava/io/File;)V".equals(mDesc)) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "dev/aaow/rt/LinuxProcFsBridge",
                                        "remapFile", "(Ljava/io/File;)Ljava/io/File;", false);
                            }
                        }
                        if ((currentInternal.endsWith("/FileProvider") || currentInternal.contains("/FileProvider$"))
                                && opcode == Opcodes.INVOKEVIRTUAL
                                && "java/io/File".equals(owner)
                                && "()Ljava/lang/String;".equals(mDesc)
                                && ("getCanonicalPath".equals(mName) || "getPath".equals(mName))) {
                            super.visitMethodInsn(opcode, owner, mName, mDesc, isInterface);
                            super.visitIntInsn(Opcodes.BIPUSH, '\\');
                            super.visitIntInsn(Opcodes.BIPUSH, '/');
                            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "replace",
                                    "(CC)Ljava/lang/String;", false);
                            return;
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
