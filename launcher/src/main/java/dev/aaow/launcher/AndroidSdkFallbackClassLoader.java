package dev.aaow.launcher;

import dev.aaow.rt.NativeBridge;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ClassLoader dự phòng thông minh:
 * - Ưu tiên toàn bộ class đã cài đặt thật trong module :framework của AAOW.
 * - Nếu APK gọi tới một class hệ thống Android chưa được viết tay (trong hơn 4.000 class của AOSP),
 *   ClassLoader này sẽ đọc class đó từ android.jar của Android SDK và dùng ASM viết lại thân hàm
 *   ngay trên bộ nhớ (xóa lệnh throw new RuntimeException("Stub!") và trả về giá trị mặc định an toàn).
 */
public final class AndroidSdkFallbackClassLoader extends ClassLoader {
    private final ZipFile androidJar;

    public AndroidSdkFallbackClassLoader(ClassLoader parent) {
        super(parent);
        this.androidJar = openSystemAndroidJar();
    }

    private static ZipFile openSystemAndroidJar() {
        List<Path> candidates = new ArrayList<>();
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            collectPlatformJars(Path.of(localAppData, "Android", "Sdk", "platforms"), candidates);
        }
        String androidHome = System.getenv("ANDROID_HOME");
        if (androidHome != null) {
            collectPlatformJars(Path.of(androidHome, "platforms"), candidates);
        }
        candidates.sort(Comparator.comparing(Path::toString).reversed());
        for (Path p : candidates) {
            try {
                return new ZipFile(p.toFile());
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private static void collectPlatformJars(Path platformsDir, List<Path> out) {
        if (!Files.isDirectory(platformsDir)) return;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(platformsDir, "android-*")) {
            for (Path dir : ds) {
                Path jar = dir.resolve("android.jar");
                if (Files.exists(jar)) out.add(jar);
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (androidJar != null) {
            String entryName = name.replace('.', '/') + ".class";
            ZipEntry entry = androidJar.getEntry(entryName);
            if (entry != null) {
                try (InputStream in = androidJar.getInputStream(entry)) {
                    byte[] rawBytes = in.readAllBytes();
                    byte[] transformed = deStubAndroidClass(rawBytes);
                    NativeBridge.recordStubbedClass(name);
                    return defineClass(name, transformed, 0, transformed.length);
                } catch (Throwable t) {
                    throw new ClassNotFoundException(name, t);
                }
            }
        }
        throw new ClassNotFoundException(name);
    }

    /**
     * Dùng ASM biến đổi một class trong android.jar: thay toàn bộ thân hàm "Stub!"
     * bằng lệnh trả về mặc định an toàn.
     */
    private byte[] deStubAndroidClass(byte[] rawBytes) {
        ClassReader cr = new ClassReader(rawBytes);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            private String superInternalName = "java/lang/Object";

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.superInternalName = superName != null ? superName : "java/lang/Object";
                // Bỏ cờ final trên class/method để các class con hoặc kế thừa không bị lỗi
                int cleanAccess = access & ~Opcodes.ACC_FINAL;
                super.visit(Opcodes.V1_6, cleanAccess, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                int cleanAccess = access & ~(Opcodes.ACC_FINAL | Opcodes.ACC_NATIVE);
                MethodVisitor mv = super.visitMethod(cleanAccess, name, descriptor, signature, exceptions);
                if ((cleanAccess & Opcodes.ACC_ABSTRACT) != 0) {
                    return mv;
                }
                // Tự sinh thân hàm an toàn thay vì đọc lệnh throw new RuntimeException("Stub!")
                mv.visitCode();
                if ("<init>".equals(name)) {
                    emitSuperConstructorCall(mv, superInternalName, descriptor);
                    mv.visitInsn(Opcodes.RETURN);
                } else {
                    emitDefaultReturn(mv, Type.getReturnType(descriptor));
                }
                mv.visitMaxs(0, 0);
                mv.visitEnd();
                return null;
            }
        };
        cr.accept(cv, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        return cw.toByteArray();
    }

    private void emitSuperConstructorCall(MethodVisitor mv, String superInternalName, String selfDesc) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        if ("java/lang/Enum".equals(superInternalName)) {
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Enum", "<init>", "(Ljava/lang/String;I)V", false);
            return;
        }
        String superCtorDesc = findSimplestSuperCtorDescriptor(superInternalName);
        Type[] argTypes = Type.getArgumentTypes(superCtorDesc);
        for (Type t : argTypes) {
            pushDefaultValue(mv, t);
        }
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superInternalName, "<init>", superCtorDesc, false);
    }

    private String findSimplestSuperCtorDescriptor(String superInternalName) {
        if ("java/lang/Object".equals(superInternalName)) return "()V";
        // Kiểm tra nếu class cha đã nạp trên JVM (:framework hoặc JDK)
        try {
            Class<?> cls = Class.forName(superInternalName.replace('/', '.'), false, this);
            Constructor<?> best = null;
            for (Constructor<?> c : cls.getDeclaredConstructors()) {
                if (c.getParameterCount() == 0) return "()V";
                if (best == null || c.getParameterCount() < best.getParameterCount()) {
                    best = c;
                }
            }
            if (best != null) return Type.getConstructorDescriptor(best);
        } catch (Throwable ignored) {
        }
        // Hoặc đọc trực tiếp từ android.jar
        if (androidJar != null) {
            ZipEntry entry = androidJar.getEntry(superInternalName + ".class");
            if (entry != null) {
                try (InputStream in = androidJar.getInputStream(entry)) {
                    ClassReader cr = new ClassReader(in);
                    final String[] bestDesc = {null};
                    final int[] bestParams = {Integer.MAX_VALUE};
                    cr.accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if ("<init>".equals(name) && (access & Opcodes.ACC_PRIVATE) == 0) {
                                int n = Type.getArgumentTypes(descriptor).length;
                                if (n < bestParams[0]) {
                                    bestParams[0] = n;
                                    bestDesc[0] = descriptor;
                                }
                            }
                            return null;
                        }
                    }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    if (bestDesc[0] != null) return bestDesc[0];
                } catch (Throwable ignored) {
                }
            }
        }
        return "()V";
    }

    static void pushDefaultValue(MethodVisitor mv, Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> mv.visitInsn(Opcodes.ICONST_0);
            case Type.LONG -> mv.visitInsn(Opcodes.LCONST_0);
            case Type.FLOAT -> mv.visitInsn(Opcodes.FCONST_0);
            case Type.DOUBLE -> mv.visitInsn(Opcodes.DCONST_0);
            default -> mv.visitInsn(Opcodes.ACONST_NULL);
        }
    }

    static void emitDefaultReturn(MethodVisitor mv, Type returnType) {
        switch (returnType.getSort()) {
            case Type.VOID -> mv.visitInsn(Opcodes.RETURN);
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> {
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitInsn(Opcodes.IRETURN);
            }
            case Type.LONG -> {
                mv.visitInsn(Opcodes.LCONST_0);
                mv.visitInsn(Opcodes.LRETURN);
            }
            case Type.FLOAT -> {
                mv.visitInsn(Opcodes.FCONST_0);
                mv.visitInsn(Opcodes.FRETURN);
            }
            case Type.DOUBLE -> {
                mv.visitInsn(Opcodes.DCONST_0);
                mv.visitInsn(Opcodes.DRETURN);
            }
            default -> {
                if ("java/lang/String".equals(returnType.getInternalName())
                        || "java/lang/CharSequence".equals(returnType.getInternalName())) {
                    mv.visitLdcInsn("");
                } else {
                    mv.visitInsn(Opcodes.ACONST_NULL);
                }
                mv.visitInsn(Opcodes.ARETURN);
            }
        }
    }
}
