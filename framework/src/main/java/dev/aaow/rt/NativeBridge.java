package dev.aaow.rt;

import android.util.Log;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Cầu nối chặn các lời gọi System.loadLibrary / System.load từ bytecode APK.
 * Trên máy Windows x86_64, các file .so biên dịch cho ARM64/ARMv7 không thể nạp trực tiếp
 * vào tiến trình JVM x86_64; NativeBridge ghi nhận lại tên thư viện thay vì làm sập ứng dụng.
 */
public final class NativeBridge {
    private static final Set<String> INTERCEPTED_LIBS = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final Set<String> INTERCEPTED_NATIVE_CALLS = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final Set<String> STUBBED_CLASSES = Collections.synchronizedSet(new LinkedHashSet<>());

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
        if (methodSig != null && INTERCEPTED_NATIVE_CALLS.size() < 200) {
            if (INTERCEPTED_NATIVE_CALLS.add(methodSig)) {
                Log.w("AAOW-JNI", "Đã giả lập lời gọi hàm JNI native: " + methodSig);
            }
        }
    }

    public static void recordStubbedClass(String className) {
        if (className != null && STUBBED_CLASSES.size() < 200) {
            STUBBED_CLASSES.add(className.replace('/', '.'));
        }
    }

    public static Set<String> getInterceptedLibraries() {
        synchronized (INTERCEPTED_LIBS) {
            return new LinkedHashSet<>(INTERCEPTED_LIBS);
        }
    }

    public static Set<String> getInterceptedNativeCalls() {
        synchronized (INTERCEPTED_NATIVE_CALLS) {
            return new LinkedHashSet<>(INTERCEPTED_NATIVE_CALLS);
        }
    }

    public static Set<String> getStubbedClasses() {
        synchronized (STUBBED_CLASSES) {
            return new LinkedHashSet<>(STUBBED_CLASSES);
        }
    }

    public static void clearDiagnostics() {
        INTERCEPTED_LIBS.clear();
        INTERCEPTED_NATIVE_CALLS.clear();
        STUBBED_CLASSES.clear();
    }
}
