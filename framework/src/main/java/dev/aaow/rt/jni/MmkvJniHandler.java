package dev.aaow.rt.jni;

import dev.aaow.rt.NativeBridge;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bộ giả lập hoàn chỉnh cho thư viện Tencent MMKV (libmmkv.so) trên JVM Windows.
 * Hỗ trợ lưu trữ bền vững ra file trong thư mục dữ liệu ứng dụng, đồng bộ đa kiểu dữ liệu
 * (boolean, int, long, float, double, String, byte[], String[]) và quản lý handle theo mmapID.
 */
public final class MmkvJniHandler implements NativeJniHandler {
    private static final String MMKV_CLASS = "com.tencent.mmkv.MMKV";

    private static final class Store {
        final long handle;
        final String mmapId;
        final Path filePath;
        String cryptKey;
        final Map<String, Object> entries = new ConcurrentHashMap<>();

        Store(long handle, String mmapId, Path filePath, String cryptKey) {
            this.handle = handle;
            this.mmapId = mmapId;
            this.filePath = filePath;
            this.cryptKey = cryptKey;
            load();
        }

        @SuppressWarnings("unchecked")
        synchronized void load() {
            if (filePath == null || !Files.isRegularFile(filePath)) return;
            try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(filePath))) {
                Object obj = ois.readObject();
                if (obj instanceof Map<?, ?> map) {
                    entries.clear();
                    for (var e : map.entrySet()) {
                        if (e.getKey() instanceof String k && e.getValue() != null) {
                            entries.put(k, e.getValue());
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        synchronized void flush() {
            if (filePath == null) return;
            try {
                Path parent = filePath.getParent();
                if (parent != null) Files.createDirectories(parent);
                Map<String, Serializable> snapshot = new LinkedHashMap<>();
                for (var e : entries.entrySet()) {
                    if (e.getValue() instanceof Serializable s) {
                        snapshot.put(e.getKey(), s);
                    }
                }
                try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(filePath))) {
                    oos.writeObject(snapshot);
                }
            } catch (IOException ignored) {
            }
        }
    }

    private final AtomicLong nextHandle = new AtomicLong(0x10_0000L);
    private final AtomicLong nextNativeBuffer = new AtomicLong(0x50_0000L);
    private final Map<Long, Store> storesByHandle = new ConcurrentHashMap<>();
    private final Map<String, Store> storesByKey = new ConcurrentHashMap<>();
    private final Map<Long, byte[]> nativeBuffers = new ConcurrentHashMap<>();
    private volatile Path rootDir = Path.of(System.getProperty("java.io.tmpdir"), "aaow-mmkv");

    @Override
    public boolean supports(String className) {
        return MMKV_CLASS.equals(className) || "com.tencent.mmkv.NativeBuffer".equals(className);
    }

    @Override
    public Object invoke(String methodSig, String className, String methodName, String descriptor, Object self, Object[] args) {
        return switch (methodName) {
            case "jniInitialize" -> {
                if (args != null && args.length >= 1 && args[0] instanceof String dir && !dir.isEmpty()) {
                    rootDir = Path.of(dir);
                    try {
                        Files.createDirectories(rootDir);
                    } catch (IOException ignored) {
                    }
                }
                yield null;
            }
            case "onExit" -> {
                storesByHandle.values().forEach(Store::flush);
                yield null;
            }
            case "setLogLevel", "setCallbackHandler", "setWantsContentChangeNotify",
                 "lock", "unlock", "clearMemoryCache", "trim", "checkContentChangedByOuterProcess" -> null;
            case "tryLock" -> true;
            case "pageSize" -> 4096;
            case "version" -> "1.3.9-aaow";
            case "ashmemFD", "ashmemMetaFD" -> -1;
            case "checkProcessMode" -> true;
            case "isFileValid" -> true;
            case "getDefaultMMKV" -> {
                String crypt = (args != null && args.length >= 2 && args[1] instanceof String s) ? s : null;
                yield getOrCreateStore("mmkv.default", null, crypt).handle;
            }
            case "getMMKVWithID", "getMMKVWithIDAndSize", "getMMKVWithAshmemFD" -> {
                String id = (args != null && args.length >= 1 && args[0] instanceof String s) ? s : "mmkv.default";
                String crypt = null;
                String path = null;
                if (args != null) {
                    for (int i = 1; i < args.length; i++) {
                        if (args[i] instanceof String str) {
                            if (i == args.length - 1 && (str.contains("/") || str.contains("\\"))) {
                                path = str;
                            } else if (crypt == null) {
                                crypt = str;
                            } else {
                                path = str;
                            }
                        }
                    }
                }
                yield getOrCreateStore(id, path, crypt).handle;
            }
            case "encodeBool" -> putValue(args, 0, 1, 2);
            case "encodeInt" -> putValue(args, 0, 1, 2);
            case "encodeLong" -> putValue(args, 0, 1, 2);
            case "encodeFloat" -> putValue(args, 0, 1, 2);
            case "encodeDouble" -> putValue(args, 0, 1, 2);
            case "encodeString" -> putValue(args, 0, 1, 2);
            case "encodeBytes" -> putValue(args, 0, 1, 2);
            case "encodeSet" -> putValue(args, 0, 1, 2);
            case "decodeBool" -> {
                Object val = getValue(args);
                boolean def = args != null && args.length >= 3 && args[2] instanceof Boolean b && b;
                yield val instanceof Boolean b ? b : def;
            }
            case "decodeInt" -> {
                Object val = getValue(args);
                int def = args != null && args.length >= 3 && args[2] instanceof Number n ? n.intValue() : 0;
                yield val instanceof Number n ? n.intValue() : def;
            }
            case "decodeLong" -> {
                Object val = getValue(args);
                long def = args != null && args.length >= 3 && args[2] instanceof Number n ? n.longValue() : 0L;
                yield val instanceof Number n ? n.longValue() : def;
            }
            case "decodeFloat" -> {
                Object val = getValue(args);
                float def = args != null && args.length >= 3 && args[2] instanceof Number n ? n.floatValue() : 0f;
                yield val instanceof Number n ? n.floatValue() : def;
            }
            case "decodeDouble" -> {
                Object val = getValue(args);
                double def = args != null && args.length >= 3 && args[2] instanceof Number n ? n.doubleValue() : 0d;
                yield val instanceof Number n ? n.doubleValue() : def;
            }
            case "decodeString" -> {
                Object val = getValue(args);
                String def = args != null && args.length >= 3 && args[2] instanceof String s ? s : null;
                yield val instanceof String s ? s : def;
            }
            case "decodeBytes" -> {
                Object val = getValue(args);
                yield val instanceof byte[] b ? b.clone() : null;
            }
            case "decodeStringSet" -> {
                Object val = getValue(args);
                yield val instanceof String[] arr ? arr.clone() : null;
            }
            case "containsKey" -> {
                Store st = resolveStore(self, args);
                String key = extractKey(args);
                yield st != null && key != null && st.entries.containsKey(key);
            }
            case "removeValueForKey" -> {
                Store st = resolveStore(self, args);
                String key = extractKey(args);
                if (st != null && key != null) {
                    st.entries.remove(key);
                    st.flush();
                }
                yield null;
            }
            case "removeValuesForKeys" -> {
                Store st = resolveStore(self, args);
                if (st != null && args != null && args.length >= 1 && args[0] instanceof String[] keys) {
                    for (String k : keys) {
                        if (k != null) st.entries.remove(k);
                    }
                    st.flush();
                }
                yield null;
            }
            case "allKeys" -> {
                Store st = resolveStore(self, args);
                yield st != null ? st.entries.keySet().toArray(new String[0]) : new String[0];
            }
            case "count" -> {
                Store st = resolveStore(self, args);
                yield st != null ? (long) st.entries.size() : 0L;
            }
            case "totalSize", "actualSize" -> {
                Store st = resolveStore(self, args);
                yield st != null ? Math.max(4096L, st.entries.size() * 64L) : 0L;
            }
            case "valueSize" -> {
                Store st = resolveStore(self, args);
                String key = extractKey(args);
                Object v = (st != null && key != null) ? st.entries.get(key) : null;
                yield estimateSize(v);
            }
            case "clearAll" -> {
                Store st = resolveStore(self, args);
                if (st != null) {
                    st.entries.clear();
                    st.flush();
                }
                yield null;
            }
            case "sync", "close" -> {
                Store st = resolveStore(self, args);
                if (st != null) st.flush();
                yield null;
            }
            case "mmapID" -> {
                Store st = resolveStore(self, args);
                yield st != null ? st.mmapId : "";
            }
            case "cryptKey" -> {
                Store st = resolveStore(self, args);
                yield st != null ? st.cryptKey : null;
            }
            case "reKey", "checkReSetCryptKey" -> {
                Store st = resolveStore(self, args);
                if (st != null && args != null && args.length >= 1) {
                    st.cryptKey = args[0] instanceof String s ? s : null;
                }
                yield "reKey".equals(methodName) ? Boolean.TRUE : null;
            }
            case "createNB" -> {
                int size = (args != null && args.length >= 1 && args[0] instanceof Number n) ? Math.max(1, n.intValue()) : 64;
                long h = nextNativeBuffer.getAndIncrement();
                nativeBuffers.put(h, new byte[size]);
                yield h;
            }
            case "destroyNB" -> {
                if (args != null && args.length >= 1 && args[0] instanceof Number n) {
                    nativeBuffers.remove(n.longValue());
                }
                yield null;
            }
            case "writeValueToNB" -> 0;
            case "backupOneToDirectory", "restoreOneMMKVFromDirectory" -> true;
            case "backupAllToDirectory", "restoreAllFromDirectory" -> (long) storesByHandle.size();
            default -> NativeBridge.UNHANDLED;
        };
    }

    private Store getOrCreateStore(String mmapId, String customDir, String cryptKey) {
        String id = (mmapId == null || mmapId.isEmpty()) ? "mmkv.default" : mmapId;
        Path dir = (customDir != null && !customDir.isEmpty()) ? Path.of(customDir) : rootDir;
        String storeKey = dir.toAbsolutePath() + "::" + id;
        return storesByKey.computeIfAbsent(storeKey, k -> {
            long h = nextHandle.getAndIncrement();
            String safeName = id.replaceAll("[^a-zA-Z0-9._-]", "_");
            Store st = new Store(h, id, dir.resolve(safeName + ".aaow.mmkv"), cryptKey);
            storesByHandle.put(h, st);
            return st;
        });
    }

    private boolean putValue(Object[] args, int handleIdx, int keyIdx, int valIdx) {
        if (args == null || args.length <= valIdx) return false;
        long handle = args[handleIdx] instanceof Number n ? n.longValue() : 0L;
        Store st = storesByHandle.get(handle);
        if (st == null || !(args[keyIdx] instanceof String key)) return false;
        Object val = args[valIdx];
        if (val == null) {
            st.entries.remove(key);
        } else if (val instanceof byte[] bytes) {
            st.entries.put(key, bytes.clone());
        } else if (val instanceof String[] arr) {
            st.entries.put(key, Arrays.copyOf(arr, arr.length));
        } else {
            st.entries.put(key, val);
        }
        st.flush();
        return true;
    }

    private Object getValue(Object[] args) {
        if (args == null || args.length < 2) return null;
        long handle = args[0] instanceof Number n ? n.longValue() : 0L;
        Store st = storesByHandle.get(handle);
        if (st == null || !(args[1] instanceof String key)) return null;
        return st.entries.get(key);
    }

    private Store resolveStore(Object self, Object[] args) {
        if (args != null && args.length >= 1 && args[0] instanceof Number n) {
            Store st = storesByHandle.get(n.longValue());
            if (st != null) return st;
        }
        if (self != null) {
            long handle = readNativeHandleField(self);
            if (handle != 0L) {
                return storesByHandle.get(handle);
            }
        }
        return null;
    }

    private static String extractKey(Object[] args) {
        if (args == null) return null;
        for (Object a : args) {
            if (a instanceof String s) return s;
        }
        return null;
    }

    private static long readNativeHandleField(Object self) {
        try {
            Field f = self.getClass().getDeclaredField("nativeHandle");
            f.setAccessible(true);
            Object v = f.get(self);
            return v instanceof Number n ? n.longValue() : 0L;
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private static int estimateSize(Object v) {
        if (v == null) return 0;
        if (v instanceof String s) return s.getBytes(StandardCharsets.UTF_8).length;
        if (v instanceof byte[] b) return b.length;
        if (v instanceof Long || v instanceof Double) return 8;
        if (v instanceof Integer || v instanceof Float) return 4;
        if (v instanceof Boolean) return 1;
        return 16;
    }
}
