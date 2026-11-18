package dev.aaow.rt;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cầu nối chặn các lời gọi System.loadLibrary / System.load từ bytecode APK
 * và theo dõi các hàm native / hàm SDK de-stubbed được gọi trong lúc chạy.
 */
public final class NativeBridge {
    private static final int MAX_TRACKED_ENTRIES = 1000;

    private static final Set<String> INTERCEPTED_LIBS = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final Map<String, Integer> INTERCEPTED_NATIVE_CALLS = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Set<String> STUBBED_CLASSES = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final Map<String, Integer> STUB_METHOD_CALLS = Collections.synchronizedMap(new LinkedHashMap<>());

    private NativeBridge() {
    }

    public static void loadLibrary(String libName) {
        if (libName == null) return;
        if (INTERCEPTED_LIBS.add(libName)) {
            Log.w("AAOW-JNI", "Đã chặn yêu cầu nạp thư viện native (.so): lib" + libName + ".so (APK ARM trên Windows x86_64)");
        }
    }

    public static void load(String filename) {
        if (filename == null) return;
        if (INTERCEPTED_LIBS.add(filename)) {
            Log.w("AAOW-JNI", "Đã chặn yêu cầu nạp file native: " + filename);
        }
    }

    public static void onNativeCall(String methodSig) {
        if (methodSig == null) return;
        boolean firstCall = false;
        synchronized (INTERCEPTED_NATIVE_CALLS) {
            Integer prev = INTERCEPTED_NATIVE_CALLS.get(methodSig);
            if (prev != null) {
                INTERCEPTED_NATIVE_CALLS.put(methodSig, prev + 1);
            } else if (INTERCEPTED_NATIVE_CALLS.size() < MAX_TRACKED_ENTRIES) {
                INTERCEPTED_NATIVE_CALLS.put(methodSig, 1);
                firstCall = true;
            }
        }
        if (firstCall) {
            Log.w("AAOW-JNI", "Đã giả lập lời gọi hàm JNI native: " + methodSig);
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

    public static void onStubMethodCall(String methodSig) {
        if (methodSig == null) return;
        boolean firstCall = false;
        synchronized (STUB_METHOD_CALLS) {
            Integer prev = STUB_METHOD_CALLS.get(methodSig);
            if (prev != null) {
                STUB_METHOD_CALLS.put(methodSig, prev + 1);
            } else if (STUB_METHOD_CALLS.size() < MAX_TRACKED_ENTRIES) {
                STUB_METHOD_CALLS.put(methodSig, 1);
                firstCall = true;
            }
        }
        if (firstCall) {
            Log.w("AAOW-STUB", "Gọi hàm Stub từ android.jar (trả giá trị rỗng an toàn): " + methodSig);
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

    public static Set<String> getStubbedClasses() {
        synchronized (STUBBED_CLASSES) {
            return new LinkedHashSet<>(STUBBED_CLASSES);
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
        STUBBED_CLASSES.clear();
        STUB_METHOD_CALLS.clear();
    }
}
