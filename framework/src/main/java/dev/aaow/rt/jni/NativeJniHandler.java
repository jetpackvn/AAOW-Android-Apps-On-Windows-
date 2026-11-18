package dev.aaow.rt.jni;

/**
 * Giao diện cho các bộ giả lập hàm JNI cấp cao (High-Level Emulation) trong Java.
 */
public interface NativeJniHandler {
    boolean supports(String className);

    /**
     * Trả về kết quả tính toán hoặc {@link dev.aaow.rt.NativeBridge#UNHANDLED} nếu không xử lý hàm này.
     */
    Object invoke(String methodSig, String className, String methodName, String descriptor, Object self, Object[] args);
}
