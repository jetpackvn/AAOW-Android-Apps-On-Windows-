# AAOW (Android Apps On Windows) — Project Memory & Sprint Rules

> **TỰ ĐỘNG NẠP ĐẦU MỖI CHAT MỚI:** File này được hệ thống tự động đọc ở mọi phiên hội thoại mới trong workspace `d:\Project\AAOW (Android Apps On Windows)`.

## 1. Mục tiêu & Kiến trúc Dự án
- **Mục tiêu:** Xây dựng trình chạy ứng dụng Android (`.apk`) trực tiếp trên Windows theo hướng **Lớp tương thích (Compatibility Layer)** (tương tự Wine / Android Translation Layer), **không dùng máy ảo (VM) hay trình giả lập (Emulator)**.
- **Ngôn ngữ & Nền tảng:** Java 21 + Gradle Multi-project (`:apk`, `:framework`, `:launcher`), kết xuất đồ họa trực tiếp qua Java2D / Swing trên Windows, dịch DEX sang JVM Bytecode qua `dex2jar` + `ASM`.
- **Cấu trúc 3 module:**
  - `apk/`: Giải mã định dạng nhị phân của Android (`ApkFile`, `AxmlParser`, `ResourceTable`, `StringPool`, `AndroidManifest`).
  - `framework/`: Lớp tương thích các gói `android.*` chạy trên Windows (`Activity`, `Application`, `View`, `ViewGroup`, `LayoutInflater`, `Canvas`, `Paint`, `Resources`, `HostWindow`, `NativeBridge`...).
  - `launcher/`: Dịch `classes*.dex` → JVM `.jar` (`DexLoader`), tự động gỡ `"Stub!"` từ `android.jar` (`AndroidSdkFallbackClassLoader`), quản lý gói (`AppRepository`) và giao diện trung tâm (`AppHubWindow`).

---

## 2. Quy trình Làm việc Theo Sprint (Bắt buộc cho AI)
Vì dự án lớn không thể làm hết trong một cửa sổ ngữ cảnh (context window), AI phải tuân thủ quy trình sau:
1. **Đầu mỗi cuộc trò chuyện mới:**
   - Đọc file này (`GEMINI.md`) cùng với [docs/ROADMAP.md](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/docs/ROADMAP.md) và [docs/SPRINT_LOG.md](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/docs/SPRINT_LOG.md) để nắm chính xác dự án đang ở Sprint nào, đã làm xong gì và bước tiếp theo là gì.
2. **Trong mỗi Sprint:**
   - Chỉ tập trung giải quyết mục tiêu của Sprint hiện tại.
   - Kiểm thử thực tế bằng lệnh chạy/chụp màn hình (`.\aaow.bat` hoặc `gradlew`) trước khi báo cáo.
3. **Cuối mỗi Sprint (Nghiệm thu & Bàn giao):**
   - Chạy kiểm thử nghiệm thu thực tế, đánh giá trung thực những gì **chạy thật** và những gì **đang giả lập/stub**.
   - Cập nhật kết quả nghiệm thu và mục tiêu chuẩn bị cho Sprint kế tiếp vào [docs/SPRINT_LOG.md](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/docs/SPRINT_LOG.md).
   - Cập nhật bảng trạng thái Sprint bên dưới trong `GEMINI.md`.

---

## 3. Tổng quan Lộ trình 8 Sprint (Từ đầu đến cuối)

Chi tiết từng Sprint xem tại [docs/ROADMAP.md](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/docs/ROADMAP.md).

| Sprint | Tên Sprint | Trọng tâm chính | Trạng thái |
|---|---|---|---|
| **Sprint 1** | **Nền tảng APK, DEX & Core UI MVP** | Đọc APK, giải mã AXML & `resources.arsc`, dịch DEX → JVM, dựng khung `Activity`/`View`/`Canvas`/`Widget` cơ bản, chạy APK mẫu đầu tiên trên cửa sổ Windows. | ✅ **Đã hoàn thành & Nghiệm thu** |
| **Sprint 2** | **AAOW App Hub, Đa cửa sổ & Cầu nối SDK/JNI** | Giao diện quản lý App Hub (kéo-thả cài APK, soi Manifest, chạy đa cửa sổ), thêm game 2048 mẫu, `AndroidSdkFallbackClassLoader` gỡ Stub từ `android.jar`, `NativeBridge` + Màn hình chẩn đoán tương thích. | ✅ **Đã hoàn thành & Nghiệm thu** |
| **Sprint 3** | **Chuẩn hóa Kỹ thuật, Công cụ Quét API & Đánh giá Kiến trúc** | Khởi tạo Git, tạo bản build chạy nhanh không qua Gradle daemon, viết công cụ `ApkCompatibilityScanner` đo tỉ lệ API thiếu trước khi chạy, lập bộ APK kiểm thử chuẩn (F-Droid) & thử nghiệm tích hợp AOSP/Robolectric. | ✅ **Đã hoàn thành & Nghiệm thu** |
| **Sprint 4** | **Mở rộng Android Framework Cốt lõi (UI & Lifecycle nâng cao)** | Hoàn thiện `Fragment`, `ActionBar`/`Toolbar`, `Menu`, `RecyclerView`/`ListView` nâng cao, `ConstraintLayout`/`CardView` ánh xạ `androidx.*`, `Animation`/`ValueAnimator`, giúp các app tiện ích mở nguồn mở hiển thị đầy đủ giao diện. | ✅ **Đã hoàn thành & Nghiệm thu** |
| **Sprint 5** | **Dữ liệu, Lưu trữ, Mạng & Đa luồng Hệ thống** | Cài đặt thật `SQLiteDatabase` (qua JDBC SQLite), `ContentResolver`/`ContentProvider`, `AsyncTask`/`HandlerThread`, `HttpURLConnection`/OkHttp, `ConnectivityManager`, `BroadcastReceiver`, `Service`. | ✅ **Đã hoàn thành & Nghiệm thu** |
| **Sprint 6** | **Đa phương tiện, WebView & Tích hợp Sâu Windows** | Tích hợp `WebView` thật (JavaFX WebView / JCEF), âm thanh `MediaPlayer`/`SoundPool`/`AudioTrack` qua Windows Audio, `NotificationManager` đẩy thông báo ra Windows System Tray, đồng bộ Clipboard & File Picker. | ✅ **Đã hoàn thành & Nghiệm thu** |
| **Sprint 7** | **Tầng Thư viện Native (`.so` / JNI) & Dịch vụ thay thế GMS** | Hỗ trợ nạp thư viện native `x86_64` (hoặc cầu nối JNI/FEX/Unicorn cho các hàm `.so` phổ biến) và lớp giả lập nhẹ các API Google Play Services / Firebase cơ bản (giống hướng microG) để app thương mại không bị chặn lúc khởi động. | ✅ **Đã hoàn thành & Nghiệm thu** |
| **Sprint 8** | **Tối ưu Hiệu năng, Đóng gói Bộ cài Windows (`.exe`/`.msi`) & Phát hành** | Tối ưu tốc độ khung hình (60fps), giảm RAM, liên kết đuôi file `.apk` trên Windows (nhấp đúp file `.apk` mở thẳng bằng AAOW), đóng gói bằng `jpackage` thành bộ cài `.exe` độc lập không cần cài JDK. | ⏳ **SPRINT TIẾP THEO (Đang chờ triển khai)** |

---

## 4. Lệnh Chạy Nhanh Dành Cho AI & Người Dùng
- Mở **AAOW App Hub**: `.\aaow.bat`
- Chạy thẳng 1 file APK: `.\aaow.bat <duong-dan.apk>`
- Quét độ tương thích API tĩnh của APK: `.\aaow.bat --scan <duong-dan.apk>`
- Biên dịch lại bản chạy nhanh (`installDist`) sau khi sửa mã nguồn: `.\aaow.bat --rebuild`
- Chạy toàn bộ bộ kiểm thử tự động JUnit 5: `.\gradlew.bat test`
- Xem thông tin & giải mã Manifest của APK: `.\aaow.bat --info <duong-dan.apk> --xml`
- Xuất ảnh chụp màn hình tự động để kiểm thử:
  - App lẻ: `.\aaow.bat --screenshot <duong-dan.apk> build/out.png [ten.Activity]`
  - App Hub: `.\aaow.bat --screenshot-hub build/hub.png`
- Đóng gói lại 2 APK mẫu (`aaow-showcase.apk`, `aaow-2048.apk`):
  `powershell -ExecutionPolicy Bypass -File .\scripts\build-sample-apk.ps1`
