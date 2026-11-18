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
 * - Nếu APK gọi tới một class hệ thống Android chưa được viết tay, ClassLoader này đọc class đó
 *   từ android.jar của Android SDK và dùng ASM gỡ bỏ lệnh throw new RuntimeException("Stub!").
 * - Nâng cấp Sprint 3 (Smart De-Stub):
 *   1. Ghi nhận chính xác từng hàm Stub được gọi lúc chạy qua NativeBridge.onStubMethodCall().
 *   2. Trả về các giá trị rỗng an toàn (String "", Collections/List/Set/Map rỗng, mảng rỗng độ dài 0,
 *      Optional.empty(), Fluent Builder `this`, khởi tạo Enum constants) để giảm NullPointerException dây chuyền.
 */
public final class AndroidSdkFallbackClassLoader extends ClassLoader {
    private final ZipFile androidJar;

    public AndroidSdkFallbackClassLoader(ClassLoader parent) {
        super(parent);
        this.androidJar = openSystemAndroidJar();
    }

    public static Path findSystemAndroidJarPath() {
        List<Path> candidates = new ArrayList<>();
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            collectPlatformJars(Path.of(localAppData, "Android", "Sdk", "platforms"), candidates);
        }
        String androidHome = System.getenv("ANDROID_HOME");
        if (androidHome != null) {
            collectPlatformJars(Path.of(androidHome, "platforms"), candidates);
        }
        String androidSdkRoot = System.getenv("ANDROID_SDK_ROOT");
        if (androidSdkRoot != null) {
            collectPlatformJars(Path.of(androidSdkRoot, "platforms"), candidates);
        }
        candidates.sort(Comparator.comparing(Path::toString).reversed());
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static ZipFile openSystemAndroidJar() {
        Path p = findSystemAndroidJarPath();
        if (p != null) {
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

    public boolean hasAndroidJar() {
        return androidJar != null;
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
     * bằng ghi nhận log gọi hàm + trả về giá trị mặc định an toàn.
     */
    public byte[] deStubAndroidClass(byte[] rawBytes) {
        ClassReader cr = new ClassReader(rawBytes);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            private String selfInternalName = "java/lang/Object";
            private String superInternalName = "java/lang/Object";
            private boolean isEnum;
            private boolean isAbstract;
            private boolean isInterface;
            private boolean clinitVisited;
            private String valuesFieldName;
            private String valuesFieldDesc;
            private final List<String> enumConstantNames = new ArrayList<>();
            private final List<String> enumCtorDescriptors = new ArrayList<>();

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.selfInternalName = name != null ? name : "java/lang/Object";
                this.superInternalName = superName != null ? superName : "java/lang/Object";
                this.isEnum = (access & Opcodes.ACC_ENUM) != 0 && "java/lang/Enum".equals(this.superInternalName);
                this.isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0;
                this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;

                // Bỏ cờ final trên class thường để không chặn kế thừa; interface giữ V1_8 để hỗ trợ static/default method
                int cleanAccess = isEnum ? access : (access & ~Opcodes.ACC_FINAL);
                int targetVersion = isInterface ? Opcodes.V1_8 : Opcodes.V1_6;
                super.visit(targetVersion, cleanAccess, name, signature, superName, interfaces);
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                if (isEnum && (access & Opcodes.ACC_ENUM) != 0
                        && ("L" + selfInternalName + ";").equals(descriptor)) {
                    enumConstantNames.add(name);
                } else if (isEnum && (access & Opcodes.ACC_STATIC) != 0
                        && ("[L" + selfInternalName + ";").equals(descriptor)
                        && ("$VALUES".equals(name) || (access & Opcodes.ACC_SYNTHETIC) != 0)) {
                    valuesFieldName = name;
                    valuesFieldDesc = descriptor;
                }
                return super.visitField(access, name, descriptor, signature, value);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                int cleanAccess = access & ~(Opcodes.ACC_FINAL | Opcodes.ACC_NATIVE);
                if ((cleanAccess & Opcodes.ACC_ABSTRACT) != 0) {
                    return super.visitMethod(cleanAccess, name, descriptor, signature, exceptions);
                }

                if ("<clinit>".equals(name)) {
                    clinitVisited = true;
                    if (isEnum && !isAbstract && !enumConstantNames.isEmpty()) {
                        // Trì hoãn sinh <clinit> tới visitEnd() sau khi đã biết toàn bộ field & ctor của enum
                        return null;
                    }
                    MethodVisitor mv = super.visitMethod(cleanAccess, name, descriptor, signature, exceptions);
                    mv.visitCode();
                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                    return null;
                }

                if ("<init>".equals(name) && isEnum) {
                    enumCtorDescriptors.add(descriptor);
                }

                MethodVisitor mv = super.visitMethod(cleanAccess, name, descriptor, signature, exceptions);
                mv.visitCode();
                if ("<init>".equals(name)) {
                    emitSuperConstructorCall(mv, superInternalName, descriptor);
                    emitStubCallTrace(mv, selfInternalName, name, descriptor);
                    mv.visitInsn(Opcodes.RETURN);
                } else if (isEnum && !isAbstract && "values".equals(name)
                        && ("()[L" + selfInternalName + ";").equals(descriptor)
                        && valuesFieldName != null && !enumConstantNames.isEmpty()) {
                    mv.visitFieldInsn(Opcodes.GETSTATIC, selfInternalName, valuesFieldName, valuesFieldDesc);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, valuesFieldDesc, "clone", "()Ljava/lang/Object;", false);
                    mv.visitTypeInsn(Opcodes.CHECKCAST, valuesFieldDesc);
                    mv.visitInsn(Opcodes.ARETURN);
                } else {
                    emitStubCallTrace(mv, selfInternalName, name, descriptor);
                    boolean isStatic = (cleanAccess & Opcodes.ACC_STATIC) != 0;
                    emitSmartDefaultReturn(mv, Type.getReturnType(descriptor), selfInternalName, isStatic);
                }
                mv.visitMaxs(0, 0);
                mv.visitEnd();
                return null;
            }

            @Override
            public void visitEnd() {
                if (isEnum && !isAbstract && !enumConstantNames.isEmpty() && (clinitVisited || !enumCtorDescriptors.isEmpty())) {
                    emitEnumClinit(cw, selfInternalName, enumConstantNames, enumCtorDescriptors, valuesFieldName, valuesFieldDesc);
                }
                super.visitEnd();
            }
        };
        cr.accept(cv, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        return cw.toByteArray();
    }

    private static void emitEnumClinit(ClassWriter cw, String selfInternalName,
                                       List<String> enumConstants, List<String> enumCtors,
                                       String valuesFieldName, String valuesFieldDesc) {
        String ctorDesc = pickSimplestEnumCtor(enumCtors);
        if (ctorDesc == null) {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            return;
        }

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        Type[] ctorArgs = Type.getArgumentTypes(ctorDesc);
        String selfFieldDesc = "L" + selfInternalName + ";";

        for (int i = 0; i < enumConstants.size(); i++) {
            String constName = enumConstants.get(i);
            mv.visitTypeInsn(Opcodes.NEW, selfInternalName);
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(constName);
            pushIntConstant(mv, i);
            for (int a = 2; a < ctorArgs.length; a++) {
                pushDefaultValue(mv, ctorArgs[a]);
            }
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, selfInternalName, "<init>", ctorDesc, false);
            mv.visitFieldInsn(Opcodes.PUTSTATIC, selfInternalName, constName, selfFieldDesc);
        }

        if (valuesFieldName != null && valuesFieldDesc != null) {
            pushIntConstant(mv, enumConstants.size());
            mv.visitTypeInsn(Opcodes.ANEWARRAY, selfInternalName);
            for (int i = 0; i < enumConstants.size(); i++) {
                mv.visitInsn(Opcodes.DUP);
                pushIntConstant(mv, i);
                mv.visitFieldInsn(Opcodes.GETSTATIC, selfInternalName, enumConstants.get(i), selfFieldDesc);
                mv.visitInsn(Opcodes.AASTORE);
            }
            mv.visitFieldInsn(Opcodes.PUTSTATIC, selfInternalName, valuesFieldName, valuesFieldDesc);
        }

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static String pickSimplestEnumCtor(List<String> ctors) {
        String best = null;
        int bestLen = Integer.MAX_VALUE;
        for (String desc : ctors) {
            Type[] args = Type.getArgumentTypes(desc);
            if (args.length >= 2
                    && "java/lang/String".equals(args[0].getInternalName())
                    && args[1].getSort() == Type.INT
                    && args.length < bestLen) {
                bestLen = args.length;
                best = desc;
            }
        }
        return best;
    }

    private static void emitStubCallTrace(MethodVisitor mv, String ownerInternalName, String methodName, String descriptor) {
        String sig = ownerInternalName.replace('/', '.') + "." + methodName + descriptor;
        mv.visitLdcInsn(sig);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "dev/aaow/rt/NativeBridge",
                "onStubMethodCall", "(Ljava/lang/String;)V", false);
    }

    private void emitSuperConstructorCall(MethodVisitor mv, String superInternalName, String selfDesc) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        if ("java/lang/Enum".equals(superInternalName)) {
            Type[] selfArgs = Type.getArgumentTypes(selfDesc);
            if (selfArgs.length >= 2
                    && "java/lang/String".equals(selfArgs[0].getInternalName())
                    && selfArgs[1].getSort() == Type.INT) {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitVarInsn(Opcodes.ILOAD, 2);
            } else {
                mv.visitLdcInsn("ENUM_STUB");
                mv.visitInsn(Opcodes.ICONST_0);
            }
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

    static void pushIntConstant(MethodVisitor mv, int value) {
        if (value >= -1 && value <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + value);
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, value);
        } else {
            mv.visitLdcInsn(value);
        }
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
        emitSmartDefaultReturn(mv, returnType, null, true);
    }

    static void emitSmartDefaultReturn(MethodVisitor mv, Type returnType, String selfInternalName, boolean isStatic) {
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
            case Type.ARRAY -> {
                int dims = returnType.getDimensions();
                if (dims == 1) {
                    Type elem = returnType.getElementType();
                    mv.visitInsn(Opcodes.ICONST_0);
                    if (elem.getSort() >= Type.BOOLEAN && elem.getSort() <= Type.DOUBLE) {
                        mv.visitIntInsn(Opcodes.NEWARRAY, toNewArrayOperand(elem));
                    } else {
                        mv.visitTypeInsn(Opcodes.ANEWARRAY, elem.getInternalName());
                    }
                } else {
                    for (int i = 0; i < dims; i++) {
                        mv.visitInsn(Opcodes.ICONST_0);
                    }
                    mv.visitMultiANewArrayInsn(returnType.getDescriptor(), dims);
                }
                mv.visitInsn(Opcodes.ARETURN);
            }
            default -> {
                String internal = returnType.getInternalName();
                if (!isStatic && selfInternalName != null && selfInternalName.equals(internal)) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                } else {
                    emitSafeObjectValue(mv, internal);
                }
                mv.visitInsn(Opcodes.ARETURN);
            }
        }
    }

    private static void emitSafeObjectValue(MethodVisitor mv, String internalName) {
        switch (internalName) {
            case "java/lang/String", "java/lang/CharSequence" -> mv.visitLdcInsn("");
            case "java/lang/StringBuilder", "java/lang/StringBuffer",
                 "java/util/ArrayList", "java/util/HashSet", "java/util/HashMap" -> {
                mv.visitTypeInsn(Opcodes.NEW, internalName);
                mv.visitInsn(Opcodes.DUP);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, internalName, "<init>", "()V", false);
            }
            case "java/util/List", "java/util/Collection", "java/lang/Iterable" ->
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "emptyList", "()Ljava/util/List;", false);
            case "java/util/Set" ->
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "emptySet", "()Ljava/util/Set;", false);
            case "java/util/Map" ->
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "emptyMap", "()Ljava/util/Map;", false);
            case "java/util/Iterator" ->
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "emptyIterator", "()Ljava/util/Iterator;", false);
            case "java/util/ListIterator" ->
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "emptyListIterator", "()Ljava/util/ListIterator;", false);
            case "java/util/Enumeration" ->
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "emptyEnumeration", "()Ljava/util/Enumeration;", false);
            case "java/util/Optional" ->
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Optional", "empty", "()Ljava/util/Optional;", false);
            case "java/util/stream/Stream" ->
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/stream/Stream", "empty", "()Ljava/util/stream/Stream;", true);
            case "java/lang/Boolean" ->
                    mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Boolean", "FALSE", "Ljava/lang/Boolean;");
            case "java/lang/Integer" -> {
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            }
            case "java/lang/Long" -> {
                mv.visitInsn(Opcodes.LCONST_0);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            }
            case "java/lang/Float" -> {
                mv.visitInsn(Opcodes.FCONST_0);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            }
            case "java/lang/Double" -> {
                mv.visitInsn(Opcodes.DCONST_0);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            }
            default -> mv.visitInsn(Opcodes.ACONST_NULL);
        }
    }

    private static int toNewArrayOperand(Type elem) {
        return switch (elem.getSort()) {
            case Type.BOOLEAN -> Opcodes.T_BOOLEAN;
            case Type.CHAR -> Opcodes.T_CHAR;
            case Type.FLOAT -> Opcodes.T_FLOAT;
            case Type.DOUBLE -> Opcodes.T_DOUBLE;
            case Type.BYTE -> Opcodes.T_BYTE;
            case Type.SHORT -> Opcodes.T_SHORT;
            case Type.INT -> Opcodes.T_INT;
            case Type.LONG -> Opcodes.T_LONG;
            default -> throw new IllegalArgumentException("Không phải kiểu nguyên thủy: " + elem);
        };
    }
}
