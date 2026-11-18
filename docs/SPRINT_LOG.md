# Nhật ký Nghiệm thu & Bàn giao Sprint (Sprint Log)

Tài liệu này lưu lại biên bản nghiệm thu trung thực sau mỗi Sprint và danh sách công việc chuẩn bị cho Sprint kế tiếp. **AI ở mỗi phiên hội thoại mới cần đọc file này trước khi bắt đầu làm việc.**

---

## ✅ Biên bản Nghiệm thu Sprint 1 (Đã hoàn thành)
- **Thời gian hoàn thành:** 17/11/2026
- **Những gì đã xây dựng & kiểm chứng chạy thật:**
  1. **Module `:apk`:**
     - [ApkFile.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/apk/src/main/java/dev/aaow/apk/ApkFile.java), [AxmlParser.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/apk/src/main/java/dev/aaow/apk/AxmlParser.java), [ResourceTable.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/apk/src/main/java/dev/aaow/apk/ResourceTable.java), [StringPool.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/apk/src/main/java/dev/aaow/apk/StringPool.java), [ResValue.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/apk/src/main/java/dev/aaow/apk/ResValue.java), [AndroidManifest.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/apk/src/main/java/dev/aaow/apk/AndroidManifest.java).
     - Giải mã chính xác `AndroidManifest.xml`, `resources.arsc`, `res/layout/*.xml`, `res/drawable/*.xml`.
  2. **Module `:framework`:**
     - Hệ thống đồ họa Java2D: `Canvas`, `Paint`, `Path`, `Bitmap`, `BitmapFactory`, `Typeface`, `ColorDrawable`, `BitmapDrawable`, `GradientDrawable`, `VectorDrawable` (vẽ SVG `pathData`).
     - Hệ thống View & Widget: `View`, `ViewGroup`, `LayoutInflater`, `LinearLayout`, `FrameLayout`, `RelativeLayout`, `ScrollView`, `ListView`, `ArrayAdapter`, `TextView`, `Button`, `EditText`, `Switch`, `CheckBox`, `ProgressBar`, `SeekBar`, `ImageView`, `AlertDialog`, `Toast`.
     - Hệ thống Vòng đời & Lưu trữ: `Application`, `Activity`, `Intent`, `Bundle`, `Handler`, `Looper`, `Resources`, `DiskSharedPreferences`.
  3. **Module `:launcher` & APK Mẫu 1 (`aaow-showcase.apk`):**
     - Dịch `classes.dex` sang JVM bytecode bằng `dex2jar` + chuẩn hóa classfile về `V1_6` qua ASM để chạy trên Java 21.
     - Đóng gói và chạy hoàn chỉnh [aaow-showcase.apk](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/samples/aaow-showcase.apk) (2 màn hình `MainActivity` và `CanvasLabActivity`).

---

## ✅ Biên bản Nghiệm thu Sprint 2 (Đã hoàn thành)
- **Thời gian hoàn thành:** 17/11/2026
- **Những gì đã xây dựng & kiểm chứng chạy thật:**
  1. **Giao diện quản lý AAOW App Hub & Kho ứng dụng:**
     - [AppRepository.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/launcher/src/main/java/dev/aaow/launcher/AppRepository.java) và [AppHubWindow.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/launcher/src/main/java/dev/aaow/launcher/AppHubWindow.java).
     - Hỗ trợ kéo-thả file `.apk` từ Windows Explorer vào cửa sổ để cài đặt, trích xuất icon, soi `AndroidManifest.xml`, xóa dữ liệu `SharedPreferences`, gỡ cài đặt, và mở nhiều ứng dụng Android trên nhiều cửa sổ cùng lúc.
  2. **APK Mẫu 2 — Game 2048 (`aaow-2048.apk`):**
     - Mã nguồn tại [Game2048Activity.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/samples/2048-src/src/dev/aaow/game2048/Game2048Activity.java), đóng gói ra [aaow-2048.apk](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/samples/aaow-2048.apk). Chơi mượt bằng phím mũi tên / WASD của Windows, vuốt chuột hoặc bấm nút.
  3. **Cầu nối SDK/JNI & Màn hình Chẩn đoán cho APK bên thứ 3:**
     - [AndroidSdkFallbackClassLoader.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/launcher/src/main/java/dev/aaow/launcher/AndroidSdkFallbackClassLoader.java): Tự động đọc class còn thiếu từ `android.jar` của Android SDK và dùng ASM gỡ lệnh `throw new RuntimeException("Stub!")`.
     - [NativeBridge.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/framework/src/main/java/dev/aaow/rt/NativeBridge.java) + [DexLoader.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/launcher/src/main/java/dev/aaow/launcher/DexLoader.java): Chặn `System.loadLibrary` và bọc các hàm `native` trong DEX để tránh `UnsatisfiedLinkError`.
     - Màn hình **Báo cáo Tương thích AAOW** trong [HostWindow.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/framework/src/main/java/dev/aaow/rt/HostWindow.java): Khi mở `com.apkpure.aegon.apk` (10,5 MB, 12.386 resources, 2 file DEX), cửa sổ mở lên không bị sập ngầm, hiển thị rõ điểm dừng và danh sách nút bấm để thử 107 `Activity` trong APK.
- **Đánh giá trung thực những điểm còn hạn chế sau Sprint 2:**
  - `aaow-showcase.apk` và `aaow-2048.apk` chạy hoàn chỉnh 100%, nhưng `com.apkpure.aegon.apk` mới chỉ mở lên được màn hình chẩn đoán của AAOW chứ chưa hiển thị được giao diện thật của APKPure.
  - Cơ chế tự động gỡ Stub trong `AndroidSdkFallbackClassLoader` giúp không bị `NoClassDefFoundError`, nhưng việc trả về `null`/`0` cho các hàm chưa cài đặt có thể gây `NullPointerException` dây chuyền ở các APK lớn.
  - Dự án chưa khởi tạo Git repository và mỗi lần chạy `aaow.bat` qua Gradle vẫn mất vài giây khởi động.

---

## 🎯 Chuẩn bị cho Sprint 3 (Mục tiêu cho phiên làm việc tiếp theo)

Khi bắt đầu triển khai **Sprint 3**, thực hiện lần lượt các bước sau:
1. **Bước 3.1 — Khởi tạo Git & Tạo bản chạy nhanh (`installDist`):**
   - Tạo `.gitignore` (bỏ qua `.gradle/`, `build/`, giữ lại `samples/*.apk`), chạy `git init` và commit mốc hoàn thành Sprint 1–2.
   - Cập nhật `aaow.bat` dùng bản phân phối đã biên dịch sẵn (`launcher/build/install/aaow/bin/aaow.bat`) để khởi động dưới 1 giây.
2. **Bước 3.2 — Xây dựng công cụ `ApkCompatibilityScanner` (`--scan <file.apk>`):**
   - Dùng ASM quét toàn bộ các class trong DEX của một APK, liệt kê tất cả các class và phương thức thuộc `android.*` / `androidx.*` mà APK đó gọi.
   - Đối chiếu với module `:framework` hiện tại để in ra báo cáo định lượng:
     - Tỉ lệ % class/method đã có cài đặt thật trong `:framework`.
     - Danh sách Top 20 class/method `android.*` bị thiếu nhiều nhất trong APK đó (giúp ưu tiên viết đúng hàm cần thiết).
3. **Bước 3.3 — Cải tiến `AndroidSdkFallbackClassLoader`:**
   - Thêm chế độ theo dõi lời gọi hàm Stub (`recordStubMethodCall`) và trả về các đối tượng rỗng an toàn (Collections rỗng, mảng rỗng, hoặc đối tượng dummy thay vì `null` khi kiểu trả về cho phép) để giảm lỗi `NullPointerException` dây chuyền.
4. **Bước 3.4 — Lập bộ APK kiểm thử chuẩn & Nghiệm thu Sprint 3:**
   - Chạy `ApkCompatibilityScanner` trên các APK đang có (bao gồm `com.apkpure.aegon.apk` và các APK thuần Java nhỏ gọn), khắc phục nhóm phương thức thiếu đứng đầu bảng thống kê, chạy nghiệm thu và cập nhật vào file này để chuyển sang Sprint 4.
