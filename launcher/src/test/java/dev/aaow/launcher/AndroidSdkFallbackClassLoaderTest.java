package dev.aaow.launcher;

import dev.aaow.rt.NativeBridge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AndroidSdkFallbackClassLoaderTest {

    @BeforeEach
    void clearDiag() {
        NativeBridge.clearDiagnostics();
    }

    @Test
    void smartDeStubReturnsSafeNonNullDefaultsAndTracksCalls() throws Exception {
        String internal = "dev/aaow/teststub/DummySdkWidget";
        byte[] rawStub = buildStubClassBytes(internal);
        AndroidSdkFallbackClassLoader loader = new AndroidSdkFallbackClassLoader(getClass().getClassLoader());
        byte[] transformed = loader.deStubAndroidClass(rawStub);

        Class<?> cls = defineInChildLoader(loader, internal.replace('/', '.'), transformed);
        Object instance = cls.getDeclaredConstructor().newInstance();

        assertEquals("", cls.getMethod("getLabel").invoke(instance));
        assertInstanceOf(List.class, cls.getMethod("getItems").invoke(instance));
        assertTrue(((List<?>) cls.getMethod("getItems").invoke(instance)).isEmpty());
        assertInstanceOf(Set.class, cls.getMethod("getTags").invoke(instance));
        assertInstanceOf(Map.class, cls.getMethod("getAttrs").invoke(instance));
        assertEquals(Optional.empty(), cls.getMethod("findOpt").invoke(instance));
        assertArrayEquals(new int[0], (int[]) cls.getMethod("getIntArr").invoke(instance));
        assertArrayEquals(new String[0], (String[]) cls.getMethod("getStrArr").invoke(instance));
        assertEquals(0, ((int[][]) cls.getMethod("getMatrix").invoke(instance)).length);
        assertSame(instance, cls.getMethod("setFlag", int.class).invoke(instance, 42));

        Map<String, Integer> calls = NativeBridge.getStubMethodCalls();
        assertTrue(calls.containsKey("dev.aaow.teststub.DummySdkWidget.getLabel()Ljava/lang/String;"));
        assertTrue(calls.containsKey("dev.aaow.teststub.DummySdkWidget.setFlag(I)Ldev/aaow/teststub/DummySdkWidget;"));
        assertEquals(2, calls.get("dev.aaow.teststub.DummySdkWidget.getItems()Ljava/util/List;"));
    }

    @Test
    void smartDeStubInitializesEnumConstantsAndValues() throws Exception {
        String internal = "dev/aaow/teststub/DummyEnum";
        byte[] rawEnum = buildStubEnumBytes(internal);
        AndroidSdkFallbackClassLoader loader = new AndroidSdkFallbackClassLoader(getClass().getClassLoader());
        byte[] transformed = loader.deStubAndroidClass(rawEnum);

        Class<?> enumCls = defineInChildLoader(loader, internal.replace('/', '.'), transformed);
        Object alpha = enumCls.getField("ALPHA").get(null);
        Object beta = enumCls.getField("BETA").get(null);
        assertNotNull(alpha);
        assertNotNull(beta);
        assertNotSame(alpha, beta);

        Object[] values = (Object[]) enumCls.getMethod("values").invoke(null);
        assertEquals(2, values.length);
        assertSame(alpha, values[0]);
        assertSame(beta, values[1]);
    }

    private static Class<?> defineInChildLoader(ClassLoader parent, String name, byte[] bytes) {
        return new ClassLoader(parent) {
            Class<?> define() {
                return defineClass(name, bytes, 0, bytes.length);
            }
        }.define();
    }

    private static byte[] buildStubClassBytes(String internal) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, internal, null, "java/lang/Object", null);

        emitStubThrowingMethod(cw, Opcodes.ACC_PUBLIC, "<init>", "()V");
        emitStubThrowingMethod(cw, Opcodes.ACC_PUBLIC, "getLabel", "()Ljava/lang/String;");
        emitStubThrowingMethod(cw, Opcodes.ACC_PUBLIC, "getItems", "()Ljava/util/List;");
        emitStubThrowingMethod(cw, Opcodes.ACC_PUBLIC, "getTags", "()Ljava/util/Set;");
        emitStubThrowingMethod(cw, Opcodes.ACC_PUBLIC, "getAttrs", "()Ljava/util/Map;");
        emitStubThrowingMethod(cw, Opcodes.ACC_PUBLIC, "findOpt", "()Ljava/util/Optional;");
        emitStubThrowingMethod(cw, Opcodes.ACC_PUBLIC, "getIntArr", "()[I");
        emitStubThrowingMethod(cw, Opcodes.ACC_PUBLIC, "getStrArr", "()[Ljava/lang/String;");
        emitStubThrowingMethod(cw, Opcodes.ACC_PUBLIC, "getMatrix", "()[[I");
        emitStubThrowingMethod(cw, Opcodes.ACC_PUBLIC, "setFlag", "(I)L" + internal + ";");

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] buildStubEnumBytes(String internal) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        String desc = "L" + internal + ";";
        String arrDesc = "[" + desc;
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER | Opcodes.ACC_ENUM,
                internal, null, "java/lang/Enum", null);

        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_ENUM,
                "ALPHA", desc, null, null).visitEnd();
        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_ENUM,
                "BETA", desc, null, null).visitEnd();
        cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC,
                "$VALUES", arrDesc, null, null).visitEnd();

        emitStubThrowingMethod(cw, Opcodes.ACC_PRIVATE, "<init>", "(Ljava/lang/String;I)V");
        emitStubThrowingMethod(cw, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "values", "()" + arrDesc);
        emitStubThrowingMethod(cw, Opcodes.ACC_STATIC, "<clinit>", "()V");
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static void emitStubThrowingMethod(ClassWriter cw, int access, String name, String desc) {
        MethodVisitor mv = cw.visitMethod(access, name, desc, null, null);
        mv.visitCode();
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/RuntimeException");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("Stub!");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.ATHROW);
        mv.visitMaxs(3, 4);
        mv.visitEnd();
    }
}
