package dev.aaow.rt.jni;

import dev.aaow.rt.NativeBridge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bộ giả lập JNI cho MemFileJniApi, KeepAliveJniApi và GMS NativeOnCompleteListener.
 */
public final class MemFileJniHandler implements NativeJniHandler {
    private static final class MemSlot {
        String strVal = "";
        boolean boolVal;
        long longVal;
    }

    private final AtomicLong nextHandle = new AtomicLong(1L);
    private final Map<Long, MemSlot> slots = new ConcurrentHashMap<>();

    @Override
    public boolean supports(String className) {
        return className.startsWith("com.tencent.assistant.alive.")
                || "com.google.android.gms.tasks.NativeOnCompleteListener".equals(className);
    }

    @Override
    public Object invoke(String methodSig, String className, String methodName, String descriptor, Object self, Object[] args) {
        if ("com.tencent.assistant.alive.db.jni.MemFileJniApi".equals(className)) {
            return switch (methodName) {
                case "init" -> {
                    long h = nextHandle.getAndIncrement();
                    slots.put(h, new MemSlot());
                    yield h;
                }
                case "writeString" -> {
                    MemSlot s = slot(args);
                    if (s != null && args.length >= 2 && args[1] instanceof String val) {
                        s.strVal = val;
                        yield true;
                    }
                    yield false;
                }
                case "readString" -> {
                    MemSlot s = slot(args);
                    yield s != null ? s.strVal : "";
                }
                case "writeBool" -> {
                    MemSlot s = slot(args);
                    if (s != null && args.length >= 2 && args[1] instanceof Boolean val) {
                        s.boolVal = val;
                        yield true;
                    }
                    yield false;
                }
                case "readBool" -> {
                    MemSlot s = slot(args);
                    yield s != null && s.boolVal;
                }
                case "writeLong" -> {
                    MemSlot s = slot(args);
                    if (s != null && args.length >= 2 && args[1] instanceof Number val) {
                        s.longVal = val.longValue();
                        yield true;
                    }
                    yield false;
                }
                case "readLong" -> {
                    MemSlot s = slot(args);
                    yield s != null ? s.longVal : 0L;
                }
                default -> NativeBridge.UNHANDLED;
            };
        }
        if ("com.tencent.assistant.alive.jni.KeepAliveJniApi".equals(className)) {
            return switch (methodName) {
                case "init" -> nextHandle.getAndIncrement();
                case "doWatch", "setServiceComponent" -> true;
                default -> NativeBridge.UNHANDLED;
            };
        }
        if ("com.google.android.gms.tasks.NativeOnCompleteListener".equals(className)
                && "nativeOnComplete".equals(methodName)) {
            return null;
        }
        return NativeBridge.UNHANDLED;
    }

    private MemSlot slot(Object[] args) {
        if (args == null || args.length < 1 || !(args[0] instanceof Number n)) return null;
        return slots.get(n.longValue());
    }
}
