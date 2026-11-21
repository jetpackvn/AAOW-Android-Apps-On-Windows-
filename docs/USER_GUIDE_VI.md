# Hướng dẫn Sử dụng AAOW (Android Apps On Windows)

🌐 **Ngôn ngữ:** **Tiếng Việt** | [English](USER_GUIDE_EN.md)

**AAOW (Android Apps On Windows)** là trình chạy tệp `.apk` trực tiếp trên Windows theo kiến trúc **Lớp tương thích (Compatibility Layer)** trên Java 21 — **không dùng máy ảo (VM), không giả lập Android**, mở ứng dụng thành từng cửa sổ Windows độc lập, nhẹ RAM và khởi động nhanh.

---

## Mục lục
1. [Yêu cầu Hệ thống](#1-yêu-cầu-hệ-thống)
2. [Cài đặt & Khởi chạy](#2-cài-đặt--khởi-chạy)
3. [Sử dụng AAOW App Hub](#3-sử-dụng-aaow-app-hub)
4. [Điều khiển Cửa sổ Ứng dụng Android & Phím tắt](#4-điều-khiển-cửa-sổ-ứng-dụng-android--phím-tắt)
5. [Tham chiếu Dòng lệnh (CLI)](#5-tham-chiếu-dòng-lệnh-cli)
6. [Lưu trữ Dữ liệu, Bộ nhớ đệm DEX & Quyền riêng tư](#6-lưu-trữ-dữ-liệu-bộ-nhớ-đệm-dex--quyền-riêng-tư)
7. [Khả năng Tương thích, Giới hạn & Xử lý Sự cố](#7-khả-năng-tương-thích-giới-hạn--xử-lý-sự-cố)
8. [Dành cho Nhà phát triển](#8-dành-cho-nhà-phát-triển)

---

## 1. Yêu cầu Hệ thống

- **Hệ điều hành:** Windows 10 hoặc Windows 11 (64-bit).
- **Đối với người dùng cuối (dùng bản cài đặt `.exe` hoặc `.zip`):** **Không cần cài sẵn Java/JDK** — bộ cài đã đóng gói kèm Java 21 Runtime rút gọn (`jlink`).
- **Đối với người chạy trực tiếp từ mã nguồn:** Cần cài **JDK 21** (`java`/`javac`). Nếu muốn công cụ tự động gỡ Stub cho các API hiếm trong Android SDK (`AndroidSdkFallbackClassLoader`), khuyến nghị có Android SDK (`platforms/android-*/android.jar`).

---

## 2. Cài đặt & Khởi chạy

### Cách 1: Dùng Bộ cài đặt Windows (`AAOW-Setup-1.0.0.exe`) — Khuyên dùng
1. Chạy file `AAOW-Setup-1.0.0.exe` (nằm trong `build\release\` nếu tự đóng gói).
2. Chọn thư mục cài đặt (mặc định: `%LOCALAPPDATA%\AAOW`, không yêu cầu quyền Administrator).
3. Tích chọn các tùy chọn mong muốn:
   - **Liên kết đuôi file `.apk`:** Cho phép nhấp đúp trực tiếp bất kỳ tệp `.apk` nào trong Windows Explorer để mở bằng AAOW (hoặc chuột phải → **Cài vào AAOW App Hub**).
   - **Tạo lối tắt trên Desktop và Start Menu.**
   - **Mở AAOW App Hub ngay sau khi cài đặt.**
4. Nhấn **Cài đặt**. Để gỡ cài đặt, vào *Windows Settings → Apps → Installed apps* hoặc chạy `Uninstall-AAOW.exe` trong thư mục cài đặt.

**Cài đặt / Gỡ cài đặt im lặng (Silent CLI flags cho `AAOW-Setup-1.0.0.exe`):**
```powershell
# Cài đặt im lặng vào thư mục tùy chọn
.\AAOW-Setup-1.0.0.exe /S /D="D:\Apps\AAOW"

# Cài đặt im lặng không liên kết đuôi .apk và không tạo shortcut
.\AAOW-Setup-1.0.0.exe /S /NOASSOC /NOSHORTCUT

# Gỡ cài đặt im lặng
.\Uninstall-AAOW.exe /UNINSTALL /S
```

### Cách 2: Dùng Bản Di động (`AAOW-Portable-1.0.0.zip`)
Giải nén `AAOW-Portable-1.0.0.zip` vào bất kỳ thư mục nào và chạy **`AAOW.exe`** (giao diện đồ họa) hoặc **`aaow-cli.exe`** (dòng lệnh).

### Cách 3: Chạy trực tiếp từ Mã nguồn
Mở PowerShell hoặc Command Prompt tại thư mục gốc dự án:
```powershell
.\aaow.bat
```
*(Lần đầu tiên sẽ tự động biên dịch bản phân phối nhanh `installDist`; các lần sau khởi động chỉ ~1–2 giây.)*

---

## 3. Sử dụng AAOW App Hub

**AAOW App Hub** là giao diện trung tâm quản lý toàn bộ ứng dụng Android đã cài trên máy:

- **Cài đặt APK mới:**
  - **Kéo-thả** một hoặc nhiều tệp `.apk` từ Windows Explorer thả thẳng vào cửa sổ App Hub; hoặc
  - Bấm nút **`+ Cài đặt APK...`** ở góc trên bên phải để chọn tệp `.apk`; hoặc
  - Bấm **`Nạp bộ APK`** để nhập hàng loạt tất cả tệp `.apk` trong một thư mục (tự động phát hiện thư mục `build\benchmark-apks` hoặc `offline-apks` nếu có).
  - Trong quá trình cài đặt, thanh tiến trình động ở đầu danh sách sẽ hiển thị tiến độ giải nén và biên dịch sẵn DEX sang JVM bytecode mà **không làm đơ cửa sổ**.
- **Tìm kiếm & Lọc:** Nhập tên ứng dụng hoặc `packageName` vào ô **Tìm kiếm** để lọc tức thì.
- **Các thao tác trên từng thẻ ứng dụng:**
  - **`Chạy ứng dụng →`**: Mở ứng dụng trong một cửa sổ Windows độc lập (có thể mở nhiều ứng dụng song song cùng lúc). Trong lúc ứng dụng đang biên dịch/nạp, thẻ sẽ hiện trạng thái **`◌ ĐANG TẢI...`** (bấm **`Xem cửa sổ tải`** hoặc **`Hủy`**); khi đã vào ứng dụng, thẻ chuyển sang **`● ĐANG CHẠY`** (bấm **`Đưa lên trước`** hoặc **`Dừng`**).
  - **`Soi APK`**: Hiển thị chi tiết cấu trúc APK bên khung phải (Package, Version, `minSdk`/`targetSdk`, danh sách Activity, quyền `Permissions`, Native ABI và toàn bộ `AndroidManifest.xml` đã giải mã từ AXML).
  - **`Quét API`**: Chạy phân tích tĩnh mức độ tương thích của APK với AAOW (độ phủ Class / Method / Call-sites và Top API còn thiếu) ngay trong khung phải.
  - **`Xóa dữ liệu`**: Xóa `SharedPreferences`, cơ sở dữ liệu SQLite, cache và bộ nhớ riêng của riêng ứng dụng đó (tương đương *Clear Data* trên Android).
  - **`Gỡ cài đặt`**: Đóng ứng dụng (nếu đang chạy), xóa tệp `.apk` khỏi kho và dọn dữ liệu ứng dụng.
- **Nút `Liên kết .apk` / `✓ Đã liên kết .apk`:** Bật hoặc tắt nhanh việc mở tệp `.apk` bằng cách nhấp đúp trong Windows Explorer (lưu dưới `HKCU\Software\Classes`, không cần quyền Admin).

---

## 4. Điều khiển Cửa sổ Ứng dụng Android & Phím tắt

### Hiệu ứng Màn hình Tải APK (Loading Screen)
Khi khởi chạy ứng dụng, cửa sổ thiết bị xuất hiện ngay lập tức với hiệu ứng vòng xoay kép 60fps, thanh tiến trình và 4 giai đoạn: **`APK` → `DEX` → `Runtime` → `UI`**.
- **Không tự ngắt giữa chừng:** AAOW chỉ ẩn màn hình tải và hiện giao diện khi ứng dụng thực sự dựng xong nội dung màn hình chính hoặc hộp thoại đầu tiên (tránh tình trạng hiện màn hình "thiếu tài nguyên / báo cáo chẩn đoán" khi `SplashActivity` đang chờ chuyển trang).
- Nếu ứng dụng gặp lỗi nghiêm trọng khi khởi chạy hoặc chờ lâu quá 10 giây chưa hiện giao diện, người dùng có thể chủ động bấm nút **`Xem Báo cáo Tương thích`** / **`Xem chẩn đoán`** hoặc **`Đóng`**.

### Thanh công cụ Cửa sổ Desktop (Phía trên)
- **`◀`**: Nút Quay lại (tương đương phím Back của Android).
- **`↻ Ngang` / `↻ Dọc`**: Xoay hướng màn hình dọc ↔ ngang và phát `onConfigurationChanged` tới ứng dụng.
- **Tỉ lệ thu phóng (`Tự do (1:1)`, `75%`, `100%`, `125%`, `150%`)**: Ở chế độ `Tự do (1:1)`, có thể kéo viền cửa sổ Windows để thay đổi kích thước khung nhìn đáp ứng theo cửa sổ.
- **`Soi View`**: Mở/đóng bảng **AAOW Live Inspector** bên phải để xem cây `View` theo thời gian thực, kích thước từng widget và danh sách hàm Stub (nếu có).
- **Chỉ số góc phải:** Hiển thị tốc độ khung hình (`fps`), thời gian dựng khung hình (`ms`) và bộ nhớ heap JVM (`MB`).

### Thanh điều hướng Android (Phía dưới)
- **Tam giác trái (Back):** Đóng Dialog/Menu hiện tại hoặc quay lại Activity trước.
- **Hình tròn giữa (Home):** Đóng các màn hình phụ để trở về Activity gốc của ứng dụng.
- **Hình vuông phải (Overview/Inspector):** Bật/tắt bảng `Soi View`.

### Bảng Phím tắt
| Phím tắt | Tác dụng |
|---|---|
| `Esc` | Quay lại (Back) — đóng Dialog hoặc thoát màn hình hiện tại |
| `Ctrl + R` | Xoay màn hình Dọc ↔ Ngang |
| `Ctrl + =` / `Ctrl + +` | Phóng to khung nhìn (+25%, tối đa 200%) |
| `Ctrl + -` | Thu nhỏ khung nhìn (-25%, tối thiểu 50%) |
| `Ctrl + 0` | Khôi phục chế độ co giãn `Tự do (1:1)` |
| `F12` | Bật/tắt bảng **Soi View (Live Inspector)** |
| `Phím mũi tên` / `W A S D` / `Enter` | Điều khiển DPAD / di chuyển trong game (như AAOW 2048) khi không nhập văn bản |
| `Ctrl + C` / `Ctrl + X` / `Ctrl + V` | Sao chép, cắt, dán hai chiều giữa `EditText` và Clipboard của Windows |

### Tích hợp Sâu với Windows
- **Chọn / Lưu tệp:** Các nút chọn tệp trong ứng dụng (`ACTION_GET_CONTENT`, `ACTION_OPEN_DOCUMENT`, `ACTION_CREATE_DOCUMENT`) tự động mở hộp thoại chọn/lưu tệp chuẩn của Windows.
- **Chia sẻ tệp (`ACTION_SEND`):** Tự động xuất tệp kết quả sang thư mục `shared-out` trong thư mục dữ liệu AAOW.
- **Mở liên kết Web:** Bấm đường dẫn `http://`/`https://` sẽ mở bằng trình duyệt mặc định trên Windows.
- **Thông báo (`NotificationManager`):** Đẩy ra khay hệ thống Windows (`SystemTray`).

---

## 5. Tham chiếu Dòng lệnh (CLI)

Có thể dùng `.\aaow.bat` (khi chạy từ mã nguồn) hoặc `aaow-cli.exe` (trong bản cài đặt/portable):

| Lệnh | Mô tả |
|---|---|
| `aaow.bat` hoặc `aaow.bat --hub` | Mở giao diện **AAOW App Hub** |
| `aaow.bat <duong-dan.apk> [ten.Activity]` | Mở trực tiếp một tệp `.apk` (có thể chỉ định tên lớp Activity cụ thể) |
| `aaow.bat --install <file.apk\|thu-muc>` | Cài đặt 1 APK hoặc cả thư mục APK vào kho, biên dịch sẵn DEX và mở App Hub |
| `aaow.bat --install-silent <file.apk\|thu-muc>` | Cài đặt APK im lặng không mở giao diện (dùng cho script/CI) |
| `aaow.bat --scan <file.apk>` | Quét độ tương thích API tĩnh (Class, Method, Call Sites, Top API thiếu, thư viện đóng gói, ABI) |
| `aaow.bat --info <file.apk> [--xml]` | In thông tin APK; thêm `--xml` để xuất toàn bộ `AndroidManifest.xml` đã giải mã |
| `aaow.bat --associate-apk` | Đăng ký mở file `.apk` bằng cách nhấp đúp trong Windows Explorer (`HKCU`) |
| `aaow.bat --unassociate-apk` | Gỡ liên kết đuôi `.apk` khỏi Windows Explorer |
| `aaow.bat --check-associate-apk` | Kiểm tra trạng thái liên kết `.apk` (`REGISTERED` / `NOT_REGISTERED`) |
| `aaow.bat --screenshot <apk> <out.png> [Act]` | Chụp ảnh màn hình APK tự động ở chế độ không cửa sổ (`420x820`) |
| `aaow.bat --screenshot-hub <out.png>` | Chụp ảnh màn hình giao diện AAOW App Hub (`1080x700`) |
| `aaow.bat --data-dir <thu-muc>` | Chỉ định thư mục lưu dữ liệu & cache riêng cho phiên chạy này |
| `aaow.bat --rebuild [tham-so...]` | *(Chỉ `aaow.bat`)* Biên dịch lại bản chạy nhanh `installDist` rồi chạy tham số phía sau |
| `aaow.bat --help` (hoặc `-h`) | Hiển thị hướng dẫn câu lệnh |

---

## 6. Lưu trữ Dữ liệu, Bộ nhớ đệm DEX & Quyền riêng tư

Tùy theo cách bạn khởi chạy, AAOW lưu dữ liệu ứng dụng tại:
- **Bản cài đặt độc lập (`AAOW.exe` / `aaow-cli.exe`):** `%LOCALAPPDATA%\AAOW\data` (dự phòng `~/.aaow/data`).
- **Bản chạy từ mã nguồn (`aaow.bat`):** `build\aaow-data` trong thư mục dự án.

Cấu trúc bên trong thư mục dữ liệu:
- `installed-apps\`: Bản sao các tệp `.apk` đã cài vào App Hub.
- `dex-cache\`: Bản dịch bytecode JVM (`*-v12.jar`), chỉ mục tham chiếu (`.refs`) và cache icon (`icons\`) giúp lần mở thứ 2 trở đi nhanh hơn gấp nhiều lần.
- `<packageName>\`: Dữ liệu riêng của từng ứng dụng (`files\`, `cache\`, `shared_prefs\`, `databases\`, `lib\`).
- `sdcard\`: Bộ nhớ ngoài dùng chung cho các ứng dụng (`Documents\`, `Notes\`, `Pictures\`, `Download\`, ...).
- `shared-out\`: Thư mục nhận tệp xuất ra từ thao tác Chia sẻ (`ACTION_SEND`) của ứng dụng.

> **Mẹo dọn dẹp / làm mới cache:** Nếu bạn nâng cấp AAOW và muốn biên dịch lại toàn bộ cache DEX hoặc làm mới dữ liệu, có thể xóa thư mục `dex-cache\` (an toàn, sẽ tự tạo lại ở lần mở tiếp theo) hoặc bấm **`Xóa dữ liệu`** ngay trên thẻ ứng dụng trong App Hub.

---

## 7. Khả năng Tương thích, Giới hạn & Xử lý Sự cố

### Nhóm ứng dụng hoạt động tốt
- **Ứng dụng thuần Java / Kotlin / AndroidX / Material Components:** Trình soạn thảo văn bản, ghi chú, công cụ tiện ích, ứng dụng dùng SQLite / Room, `RecyclerView`, `ViewPager`, `DrawerLayout`, `Fragment`, `Dialog`.
- **Ứng dụng có kết nối mạng (REST/HTTPS) & WebView / Bản đồ / Biểu đồ:** Ứng dụng dùng `HttpURLConnection`, `OkHttp3`, `Volley`, tải ảnh `Glide`/`Picasso`/`Coil` (hỗ trợ cả WebP qua `imageio-webp`), JavaFX WebKit `WebView`, biểu đồ 2D.
- **Ứng dụng sử dụng một số dịch vụ Google cơ bản (`microG-lite`) & JNI phổ biến:** Hỗ trợ khởi tạo `FirebaseApp`, `GoogleApiAvailability`, `AdvertisingIdClient`, `MMKV`, `WebP`, `zlib`, `MemFile`, `QSea`/`QMSP`, và bộ thông dịch ARM64 thuần Java cho các hàm tính toán nhẹ.

### Giới hạn hiện tại (Cần biết trước khi chạy)
- **Game 3D / Engine đồ họa nặng:** Chưa hỗ trợ OpenGL ES / Vulkan đầy đủ (AAOW tập trung vào đồ họa UI/Canvas 2D trên Java2D).
- **Thư viện Native (`.so`) phức tạp:** Các thư viện C/C++ gọi sâu syscall Linux/Bionic, anti-debug, hoặc video codec phần cứng không nằm trong danh sách HLE có thể chưa chạy hết toàn bộ tính năng.
- **Dịch vụ Google yêu cầu xác thực thực tế:** Đăng nhập Google OAuth thật, Google Play Billing, Google Play Integrity / SafetyNet, hoặc bản đồ Google Maps SDK gốc yêu cầu chứng thực máy chủ Google sẽ không xác thực thật được qua `microG-lite`.

### Cách chẩn đoán khi một APK không lên giao diện
1. **Quét trước khi chạy:** Bấm **`Quét API`** trong App Hub hoặc chạy `.\aaow.bat --scan app.apk` để xem tỉ lệ Call-Sites khớp `:framework`, ABI `.so` cần thiết và Top 20 hàm còn thiếu.
2. **Xem Báo cáo Tương thích thủ công:** Nếu ứng dụng gặp lỗi khi mở hoặc chờ quá 10 giây ở màn hình loading, hãy bấm nút **`Xem Báo cáo Tương thích`** / **`Xem chẩn đoán`** trên màn hình tải, hoặc nhấn **`F12`** (`Soi View`) để kiểm tra chi tiết ngoại lệ và thử mở các Activity khác trong APK.

---

## 8. Dành cho Nhà phát triển

- **Chạy toàn bộ bộ kiểm thử tự động JUnit 5 (bao gồm kiểm tra phân cấp kế thừa `android.jar`):**
  ```powershell
  .\gradlew.bat test --rerun-tasks --console=plain
  ```
- **Biên dịch lại bản chạy nhanh CLI/Hub sau khi sửa mã nguồn:**
  ```powershell
  .\aaow.bat --rebuild
  ```
- **Đóng gói lại 2 APK mẫu (`samples\aaow-showcase.apk`, `samples\aaow-2048.apk`):**
  ```powershell
  powershell -ExecutionPolicy Bypass -File .\scripts\build-sample-apk.ps1
  ```
- **Đóng gói lại `AAOW.exe`, `AAOW-Portable-1.0.0.zip` và `AAOW-Setup-1.0.0.exe`:**
  ```powershell
  powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1
  ```
  *(Yêu cầu JDK 21 có `jlink.exe` & `jpackage.exe`, cùng `.NET Framework 4 csc.exe` có sẵn trên Windows 10/11.)*
