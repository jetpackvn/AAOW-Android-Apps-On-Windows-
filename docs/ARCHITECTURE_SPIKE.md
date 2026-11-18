# Thử nghiệm Kiến trúc (Architecture Spike): `android-all` (Robolectric) vs. Tự phát triển `:framework`

**Thời điểm thực hiện:** Sprint 3 (18/11/2026)  
**Mục tiêu (theo ROADMAP 3.5):** Đánh giá định lượng khả năng tái sử dụng mã nguồn Android Framework thật (thông qua `org.robolectric:android-all` trên Maven Central) so với chiến lược tiếp tục phát triển module `:framework` của AAOW trước khi bước vào mở rộng quy mô lớn ở Sprint 4–7.

---

## 1. Số liệu Đo đạc Thực nghiệm

| Chỉ số đo được | AAOW `:framework` + Runtime hiện tại | `org.robolectric:android-all` (`15-robolectric-12650502`) | Android SDK `android.jar` (`android-37.0`) |
|---|---|---|---|
| **Kích thước đóng gói (JAR)** | `framework.jar`: **353.7 KB** (`353,713` bytes)<br>Tổng toàn bộ `lib/*.jar` (gồm `dex2jar`, `ASM`, `apk`, `launcher`): **2.42 MB** (`2,533,056` bytes) | **176.31 MB** (`184,872,954` bytes) — lớn gấp **522×** riêng `framework.jar` | **41.05 MB** (chỉ chứa chữ ký Stub, không có mã thực thi) |
| **Số lượng class** | ~125 class lõi tự cài đặt + De-Stub động theo nhu cầu | Hàng chục nghìn class toàn bộ AOSP System + Framework | 6.440 class (trong đó 4.915 class `android.*`) |
| **Cơ chế đồ họa & cửa sổ** | Kết xuất trực tiếp qua **Java2D / Swing** trên Windows (`HostWindow`) | Gọi JNI native (`libandroid_runtime.so`, Skia, HWUI, SurfaceFlinger) -> **không chạy trực tiếp trên JVM Windows** nếu không có hàng trăm Shadow class | Không chứa implementation |
| **Vòng đời & IPC** | Tối giản, gắn thẳng `Activity`/`Context` vào JVM thread & Swing EDT | Phụ thuộc chặt `ActivityThread`, `Binder`, `ServiceManager`, `Looper`/`MessageQueue` native (epoll), hidden APIs (`dalvik.system`, `libcore`) | Stub |

---

## 2. Phân tích Ưu / Nhược điểm của Hai Hướng Tiếp cận

### Hướng A: Đưa `org.robolectric:android-all` làm nền tảng `:framework`
- **Ưu điểm:**
  - Có sẵn hàng nghìn class thuần Java của AOSP (như `Bundle`, `Intent`, `Uri`, `SpannableStringBuilder`, `SparseIntArray`, `Configuration`, các thuật toán layout `LinearLayout`/`RelativeLayout`...).
- **Nhược điểm & Rào cản chí mạng cho mục tiêu phần mềm Desktop Windows:**
  1. **Kích thước & Bộ nhớ:** Gắn thêm **176.3 MB** JAR vào bộ cài AAOW ngay lập tức, làm tăng thời gian khởi động và RAM tiêu thụ, trái ngược mục tiêu ứng dụng desktop nhẹ (< 1 giây mở máy) và bộ cài `.exe` gọn nhẹ ở Sprint 8.
  2. **Rào cản JNI / Native AOSP:** Mọi lớp đồ họa, văn bản, cơ sở dữ liệu và tài nguyên của AOSP (`Canvas`, `Paint`, `Path`, `Bitmap`, `Typeface`, `AssetManager`, `ResourcesImpl`, `SQLiteConnection`, `MessageQueue`) đều gọi `native*` tới thư viện C++ của Linux/Android (`libandroid_runtime.so`, `libskia.so`, `libbinder.so`). Để chạy được trên Windows JVM, AAOW sẽ phải tự xây lại toàn bộ tầng **Robolectric Shadow + Bytecode ClassInstrumentor** để chặn hàng trăm hàm `native` — độ phức tạp tương đương hoặc lớn hơn tự viết lớp tương thích sạch.
  3. **Xung đột JVM Desktop (Java 21):** `android-all` được biên dịch cho ART/libcore với nhiều phụ thuộc nội bộ (`sun.misc.Unsafe`, `dalvik.system.VMRuntime`, `android.os.SystemProperties`, hidden flags `--add-opens`) dễ gãy khi nâng cấp JDK.

### Hướng B: Tiếp tục `:framework` tự chủ kết hợp tái sử dụng chọn lọc (Hybrid Architecture)
- **Ưu điểm:**
  - **Nhẹ và khởi động tức thì:** Toàn bộ phân phối `installDist` hiện tại chỉ nặng **2.42 MB**, mở CLI hoặc App Hub dưới ~1–1.5 giây.
  - **Ánh xạ thẳng Windows:** `Canvas`/`Paint`/`Bitmap`/`VectorDrawable` vẽ trực tiếp bằng phần cứng Java2D; `SharedPreferences` lưu file thật; sự kiện chuột/bàn phím/clipboard nối thẳng Swing/AWT.
  - **Kiểm soát chặt qua đo lường:** Nhờ `ApkCompatibilityScanner` (`--scan`) và `FrameworkHierarchyTest` (đối chiếu tự động với `android.jar`), ta biết chính xác class/method nào cần viết và đảm bảo **0% lệch cây kế thừa** (`VerifyError` bằng 0).
- **Nhược điểm:**
  - Cần đầu tư công sức cài đặt các nhóm API còn thiếu (`Fragment`, `RecyclerView`, `ConstraintLayout`, `SQLiteDatabase`, `Notification`...).

---

## 3. Quyết định Kiến trúc Chính thức (Architectural Decision Record)

**Chọn Hướng B nâng cấp — Kiến trúc Lai Chọn lọc (Selective Hybrid Compatibility Layer):**
1. **Giữ lõi `:framework` tự chủ, nhẹ (Java 21 + Java2D/Swing)** cho toàn bộ tầng: Windowing (`HostWindow`), Đồ họa 2D (`Canvas`, `Paint`, `Drawable`, `Bitmap`), Vòng đời (`Application`, `Activity`, `Context`), Tài nguyên (`Resources`, `AssetManager`), và Cầu nối Hệ thống Windows.
2. **Không nhúng nguyên khối `org.robolectric:android-all` (176.3 MB)** để bảo toàn tốc độ khởi động, độ gọn của bộ cài `.exe` (Sprint 8) và tránh phụ thuộc Shadow Engine.
3. **Tái sử dụng có chọn lọc các thư viện chuẩn JVM / Android thuần Java ở các Sprint 4–6:**
   - **Sprint 4:** Tái sử dụng/bổ sung trực tiếp các gói UI thuần Java (hoặc port các lớp thuần Java từ AOSP/AndroidX như `FragmentManager`, `RecyclerView`, `ConstraintLayout`, `ViewPager`).
   - **Sprint 5:** Dùng **SQLite JDBC** (thư viện Java chuẩn có sẵn driver Windows) để hiện thực `SQLiteDatabase`/`Cursor`, và Java `HttpURLConnection`/`java.net.http.HttpClient` cho lớp mạng.
   - **Sprint 6:** Dùng JavaFX `WebView` / JCEF cho `android.webkit.WebView` thật và `javax.sound` / Windows Tray API cho âm thanh & thông báo.
4. **Duy trì `AndroidSdkFallbackClassLoader` (Smart De-Stub) + `FrameworkHierarchyTest`:** Tự động lấp đầy hàng nghìn class SDK phụ trợ ít chạm tới mà không cần viết tay 100% AOSP, đồng thời dùng unit test tự động khóa chặt tính đúng đắn của cây kế thừa với `android.jar`.
