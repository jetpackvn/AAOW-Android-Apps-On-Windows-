package dev.aaow.rt;

import android.util.Log;
import dev.aaow.rt.jni.MemFileJniHandler;
import dev.aaow.rt.jni.MmkvJniHandler;
import dev.aaow.rt.jni.NativeJniHandler;
import dev.aaow.rt.jni.SqliteJniHandler;
import dev.aaow.rt.jni.WebpJniHandler;
import dev.aaow.rt.jni.ZlibAndPatchJniHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cầu nối trung tâm điều phối System.loadLibrary / System.load và lời gọi hàm JNI native từ bytecode APK:
 * 1. Nạp thư viện native qua NativeLibraryManager (Windows DLL thật hoặc phân tích ELF .so).
 * 2. Điều phối hàm JNI sang các bộ giả lập HLE (MMKV, WebP, zlib, MemFile, SQLite).
 * 3. Nếu chưa có HLE nhưng APK có hàm Java_* trong .so arm64-v8a, chạy qua Arm64JniInterpreter.
 * 4. Theo dõi thống kê cả lời gọi giả lập thành công lẫn lời gọi dự phòng để chẩn đoán.
 */
public final class NativeBridge {
    private static final int MAX_TRACKED_ENTRIES = 1000;

    /** Giá trị đánh dấu chưa có bộ xử lý native nào nhận hàm này -> quay về giá trị mặc định an toàn. */
    public static final Object UNHANDLED = new Object();

    private static final List<NativeJniHandler> JNI_HANDLERS = List.of(
            new MmkvJniHandler(),
            new WebpJniHandler(),
            new ZlibAndPatchJniHandler(),
            new MemFileJniHandler(),
            new SqliteJniHandler()
    );

    private static final Set<String> INTERCEPTED_LIBS = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final Map<String, Integer> INTERCEPTED_NATIVE_CALLS = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<String, Integer> EMULATED_NATIVE_CALLS = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Set<String> STUBBED_CLASSES = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final Set<String> STUBBED_PLUGIN_CLASSES = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final Map<String, Integer> STUB_METHOD_CALLS = Collections.synchronizedMap(new LinkedHashMap<>());

    private NativeBridge() {
    }

    public static void loadLibrary(String libName) {
        if (libName == null) return;
        boolean handled = NativeLibraryManager.loadLibrary(libName);
        if (INTERCEPTED_LIBS.add(libName) && !handled) {
            Log.w("AAOW-JNI", "Đã chặn yêu cầu nạp thư viện native (.so): lib" + libName + ".so");
        }
    }

    public static void load(String filename) {
        if (filename == null) return;
        boolean handled = NativeLibraryManager.loadFile(filename);
        if (INTERCEPTED_LIBS.add(filename) && !handled) {
            Log.w("AAOW-JNI", "Đã chặn yêu cầu nạp file native: " + filename);
        }
    }

    /**
     * Điều phối một lời gọi phương thức native từ bytecode APK kèm đối tượng `this` và danh sách tham số.
     * Trả về {@link #UNHANDLED} nếu chưa có bộ giả lập/thông dịch nào xử lý.
     */
    public static Object invokeNative(String methodSig, Object self, Object[] args) {
        if (methodSig == null) return UNHANDLED;
        int paren = methodSig.indexOf('(');
        if (paren > 0) {
            String prefix = methodSig.substring(0, paren);
            int dot = prefix.lastIndexOf('.');
            if (dot > 0) {
                String className = prefix.substring(0, dot);
                String methodName = prefix.substring(dot + 1);
                String descriptor = methodSig.substring(paren);

                for (NativeJniHandler handler : JNI_HANDLERS) {
                    if (handler.supports(className)) {
                        Object res = handler.invoke(methodSig, className, methodName, descriptor, self, args);
                        if (res != UNHANDLED) {
                            recordEmulatedNativeCall("HLE:" + methodSig);
                            return res;
                        }
                    }
                }

                Object elfRes = NativeLibraryManager.tryInvokeArm64Jni(className, methodName, descriptor, args);
                if (elfRes != UNHANDLED) {
                    recordEmulatedNativeCall("ARM64:" + methodSig);
                    return elfRes;
                }
            }
        }
        onNativeCall(methodSig);
        return UNHANDLED;
    }

    public static void onNativeCall(String methodSig) {
        if (methodSig == null) return;
        boolean firstCall = trackCall(INTERCEPTED_NATIVE_CALLS, methodSig);
        if (firstCall) {
            Log.w("AAOW-JNI", "Đã giả lập mặc định lời gọi hàm JNI native: " + methodSig);
        }
    }

    private static void recordEmulatedNativeCall(String tagAndSig) {
        boolean first = trackCall(EMULATED_NATIVE_CALLS, tagAndSig);
        if (first) {
            Log.i("AAOW-JNI", "Đã thực thi/giả lập JNI thật: " + tagAndSig);
        }
    }

    public static void recordStubbedClass(String className) {
        if (className == null) return;
        synchronized (STUBBED_CLASSES) {
            if (STUBBED_CLASSES.size() < MAX_TRACKED_ENTRIES) {
                STUBBED_CLASSES.add(className.replace('/', '.'));
            }
        }
    }

    public static void recordStubbedPluginClass(String className) {
        if (className == null) return;
        String clean = className.replace('/', '.');
        boolean added;
        synchronized (STUBBED_PLUGIN_CLASSES) {
            added = STUBBED_PLUGIN_CLASSES.size() < MAX_TRACKED_ENTRIES && STUBBED_PLUGIN_CLASSES.add(clean);
        }
        if (added) {
            Log.i("AAOW-PLUGIN", "Đã tổng hợp lớp plugin tùy chọn để vượt bước verify JVM: " + clean);
        }
    }

    public static void onStubMethodCall(String methodSig) {
        if (methodSig == null) return;
        boolean firstCall = trackCall(STUB_METHOD_CALLS, methodSig);
        if (firstCall) {
            Log.w("AAOW-STUB", "Gọi hàm Stub từ android.jar (trả giá trị rỗng an toàn): " + methodSig);
        }
    }

    private static boolean trackCall(Map<String, Integer> map, String key) {
        synchronized (map) {
            Integer prev = map.get(key);
            if (prev != null) {
                map.put(key, prev + 1);
                return false;
            }
            if (map.size() < MAX_TRACKED_ENTRIES) {
                map.put(key, 1);
                return true;
            }
            return false;
        }
    }

    public static Set<String> getInterceptedLibraries() {
        synchronized (INTERCEPTED_LIBS) {
            return new LinkedHashSet<>(INTERCEPTED_LIBS);
        }
    }

    public static Set<String> getInterceptedNativeCalls() {
        synchronized (INTERCEPTED_NATIVE_CALLS) {
            return new LinkedHashSet<>(INTERCEPTED_NATIVE_CALLS.keySet());
        }
    }

    public static Map<String, Integer> getInterceptedNativeCallCounts() {
        synchronized (INTERCEPTED_NATIVE_CALLS) {
            return new LinkedHashMap<>(INTERCEPTED_NATIVE_CALLS);
        }
    }

    public static Map<String, Integer> getEmulatedNativeCallCounts() {
        synchronized (EMULATED_NATIVE_CALLS) {
            return new LinkedHashMap<>(EMULATED_NATIVE_CALLS);
        }
    }

    public static Set<String> getStubbedClasses() {
        synchronized (STUBBED_CLASSES) {
            return new LinkedHashSet<>(STUBBED_CLASSES);
        }
    }

    public static Set<String> getStubbedPluginClasses() {
        synchronized (STUBBED_PLUGIN_CLASSES) {
            return new LinkedHashSet<>(STUBBED_PLUGIN_CLASSES);
        }
    }

    public static Map<String, Integer> getStubMethodCalls() {
        synchronized (STUB_METHOD_CALLS) {
            return new LinkedHashMap<>(STUB_METHOD_CALLS);
        }
    }

    public static List<Map.Entry<String, Integer>> getTopStubMethodCalls(int limit) {
        List<Map.Entry<String, Integer>> list;
        synchronized (STUB_METHOD_CALLS) {
            list = new ArrayList<>(STUB_METHOD_CALLS.entrySet());
        }
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        if (limit > 0 && list.size() > limit) {
            return new ArrayList<>(list.subList(0, limit));
        }
        return list;
    }

    public static void clearDiagnostics() {
        INTERCEPTED_LIBS.clear();
        INTERCEPTED_NATIVE_CALLS.clear();
        EMULATED_NATIVE_CALLS.clear();
        STUBBED_CLASSES.clear();
        STUBBED_PLUGIN_CLASSES.clear();
        STUB_METHOD_CALLS.clear();
    }
}
