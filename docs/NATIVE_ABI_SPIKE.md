# Nghiên cứu Kiến trúc Native ABI (`.so` / JNI): Unicorn Engine / Dynarmic vs. Hybrid JNI HLE + Pure-Java ARM64

**Thời điểm thực hiện:** Sprint 7 (18/11/2026)  
**Mục tiêu (theo ROADMAP 7.1–7.2):** Đánh giá các phương án chạy hoặc tương thích hóa thư viện C/C++ (`.so`) của APK Android trên Windows x86_64 JVM, đo đạc trên APK thương mại thực tế (`com.apkpure.aegon.apk`), và chốt kiến trúc cho tầng Native Bridge của AAOW.

---

## 1. Hiện trạng Thư viện Native trong APK Thực tế

Khảo sát trực tiếp trên `com.apkpure.aegon.apk` (APK thương mại 10,57 MB, 19.029 class, `minSdk=19`, `targetSdk=28`) cho thấy:
- **ABI đóng gói trong APK:** `[arm64-v8a, armeabi-v7a]` (không có `x86_64` hay `x86`, đây là thực tế phổ biến của >85% APK phát hành cho điện thoại ARM).
- **Tổng số hàm `native` khai báo trong DEX:** **95 hàm** trên 12 class, và **14 điểm gọi** `System.loadLibrary` / `System.load`.
- **5 file `.so` thực tế trong `lib/arm64-v8a/`:**
  1. `libglide-webp.so` (321.880 bytes) — giải mã WebP (`JNI_OnLoad`).
  2. `libqmp.so` (170.888 bytes) — mô-đun bảo mật/kiểm tra môi trường Tencent (`JNI_OnLoad`).
  3. `libqsealib.so` (94.520 bytes) — mã hóa thiết bị QSEA (`JNI_OnLoad`).
  4. `libdeflater7z.so` (23.864 bytes) — nén Deflate (`Java_com_tencent_yybsdk_apkpatch_patch64_DeflateTask_deflateByCallback`).
  5. `libdaemon_acc_v2.1.5.so` (18.096 bytes) — đọc/ghi vùng nhớ dùng chung (`MemFileJniApi`, `KeepAliveJniApi`, 10 ký hiệu `Java_*`).
- **Thư viện phổ biến tải động hoặc khai báo JNI trong DEX:** `com.tencent.mmkv.MMKV` (`libmmkv.so`, ~47 phương thức `native` lưu trữ key-value hiệu năng cao).

**Vấn đề cốt lõi ngay cả khi APK có `lib/x86_64/*.so`:**  
File `lib/x86_64/*.so` của Android là **Linux ELF64** liên kết động tới Bionic libc của Android (`libc.so`, `libm.so`, `liblog.so`, `libandroid.so`, `libz.so`, `libc++_shared.so`), **không phải Windows PE DLL**. Vì vậy `System.load()` của JVM trên Windows chỉ nạp trực tiếp được `.dll` (PE), không thể `dlopen` trực tiếp ELF `.so` nếu thiếu một tầng biên dịch syscall Linux/Bionic (như WSL/Wine ngược).

---

## 2. Đánh giá 3 Phương án Kiến trúc Native

| Tiêu chí | Phương án 1: Nhúng Unicorn Engine / Dynarmic (Native DLL qua JNA/FFM) | Phương án 2: Chỉ chặn Stub tĩnh (`return 0/null`) | Phương án 3 (Đã chọn): Hybrid JNI HLE + ELF Parser + Pure-Java ARM64 Interpreter |
|---|---|---|---|
| **Độ phụ thuộc ngoài** | Cần đóng gói thêm `unicorn.dll` + viết Bionic libc + `JNIEnv` ~230 hàm + allocator/syscall bridge | Không | **0 thư viện C ngoài** — thuần Java 21 |
| **Kích thước đóng gói** | Lớn (thêm nhiều MB binary C/C++ theo nền tảng) | 0 KB | **~35 KB bytecode Java** |
| **Xử lý thư viện phổ biến (`MMKV`, `libz`, `WebP`, `SQLite`, `MemFile`)** | Chậm hơn do phải giả lập từng lệnh CPU + syscall `mmap`/`futex` của `MMKV` | **Thất bại logic** (MMKV mất toàn bộ cấu hình đọc/ghi, trả `0L`/`false`) | **Chạy thật ở tốc độ JVM gốc** (`MmkvJniHandler` lưu đĩa thật, `ZlibAndPatchJniHandler` dùng `java.util.zip`, `WebpHelper` đọc kích thước RIFF WebP) |
| **Hàm tính toán C thuần trong `.so` ARM64 (`Java_*`)** | Chạy được nếu hàm không chạm syscall phức tạp | Trả `0` | **Chạy được trực tiếp** qua `ElfFile` (`PT_LOAD`) + `Arm64JniInterpreter` (AArch64 user-mode + `JNIEnv*` tối giản) |
| **Nạp thư viện Windows `.dll` tùy chọn** | Có | Không | **Có** (`NativeLibraryManager.loadLibrary` / `loadFile` nạp thẳng `.dll` qua `System.load` nếu có) |

---

## 3. Lý do Chọn Phương án 3 & Thiết kế Triển khai

1. **Nâng cấp `DexLoader` (Bytecode Rewriting):** Mọi phương thức `ACC_NATIVE` được bọc gọi `NativeBridge.invokeNative(methodSig, self, args)` có truyền đầy đủ `this` và tham số đã đóng hộp, kiểm tra kết quả khác `NativeBridge.UNHANDLED` rồi unbox đúng kiểu trả về; chỉ fallback về `emitSmartDefaultReturn` khi chưa có handler/ELF nào xử lý.
2. **Tầng `NativeLibraryManager` + `ElfFile`:** Giải nén `.so` từ APK vào `applicationInfo.nativeLibraryDir`, phân tích chuẩn ELF32/ELF64 (`PT_LOAD`, `DT_NEEDED`, `SHT_DYNSYM` / `SHT_SYMTAB`), lập chỉ mục toàn bộ ký hiệu `JNI_OnLoad` & `Java_*`, đồng thời hỗ trợ `System.load()` trực tiếp nếu phát hiện thư viện Windows `.dll`.
3. **Tầng JNI HLE (`dev.aaow.rt.jni.*`):** Cài đặt các bộ giả lập chức năng thật trên JVM Windows (`MmkvJniHandler`, `WebpJniHandler`, `ZlibAndPatchJniHandler`, `MemFileJniHandler`, `SqliteJniHandler`) — giải quyết triệt để các thư viện JNI chiếm tỷ lệ lỗi cao nhất mà không vướng syscall Bionic (`mmap`/`futex`/`pthread`).
4. **Trình thông dịch ARM64 thuần Java (`Arm64JniInterpreter`):** Chứng minh khả năng thực thi trực tiếp mã máy `arm64-v8a` trong vùng nhớ `PT_LOAD` cho các hàm tính toán JNI (`Java_*`) với giới hạn bước an toàn (`200.000` lệnh) và bảng `JNIEnv*` ảo (`NewStringUTF`, `GetStringUTFChars`, `GetIntArrayElements`, `SetIntArrayRegion`...). Các hàm bảo mật/chống debug gọi syscall hệ điều hành hoặc PLT bên ngoài tự động thoái lui an toàn mà không làm sập tiến trình.
5. **Hướng mở rộng tương lai (nếu cần game 3D / Unity ở giai đoạn sau Sprint 8):** Có thể gắn Unicorn Engine hoặc Dynarmic qua Java 22 Foreign Function & Memory (FFM) API vào đúng interface `NativeLibraryManager` hiện tại mà không cần sửa `DexLoader` hay `:framework`.
