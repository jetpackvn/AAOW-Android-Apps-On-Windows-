package dev.aaow.rt;

/**
 * Kiểu đánh dấu nội bộ dùng cho các bridge constructor được tổng hợp tự động bởi {@code DexR8Sanitizer}
 * khi cần tránh trùng chữ ký với một constructor sẵn có trên cùng lớp trong APK.
 */
public final class R8CtorMarker {
    private R8CtorMarker() {
    }
}
