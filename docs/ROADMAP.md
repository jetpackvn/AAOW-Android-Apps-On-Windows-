# Lộ trình Tổng thể Dự án AAOW (Android Apps On Windows)

Tài liệu này định nghĩa lộ trình từ đầu đến cuối của dự án **AAOW** theo từng Sprint. Mỗi Sprint có mục tiêu rõ ràng, hạng mục bàn giao và tiêu chí nghiệm thu cụ thể.

---

## Sprint 1: Nền tảng APK, DEX & Core UI MVP (Đã hoàn thành)
- **Mục tiêu:** Chứng minh tính khả thi của kiến trúc Lớp tương thích (Compatibility Layer) bằng cách đọc một file `.apk` thật và chạy nó trực tiếp trên cửa sổ Windows không qua máy ảo.
- **Hạng mục chính:**
  1. Bộ giải mã ZIP/APK (`ApkFile`), XML nhị phân (`AxmlParser`) và bảng tài nguyên `resources.arsc` (`ResourceTable`).
  2. Bộ dịch bytecode Dalvik/ART (`classes*.dex`) sang JVM bytecode (`DexLoader` dùng `dex2jar` + `ASM`).
  3. Khung Android Framework tối thiểu: `Context`, `Resources`, `Activity`, `Window`, `View`, `ViewGroup`, `LayoutInflater`, `Canvas`/`Paint` (Java2D), các bố cục (`LinearLayout`, `FrameLayout`, `RelativeLayout`, `ScrollView`) và widget cơ bản (`TextView`, `Button`, `EditText`, `Switch`, `CheckBox`, `SeekBar`, `ImageView`, `AlertDialog`, `Toast`).
  4. Lưu trữ `SharedPreferences` xuống ổ cứng Windows.
  5. Đóng gói và chạy thành công APK mẫu đầu tiên (`aaow-showcase.apk`).
- **Tiêu chí nghiệm thu:** Chạy được `aaow-showcase.apk` với đầy đủ bố cục XML, icon `<vector>`, chuyển trang `Intent` giữa 2 `Activity`, vẽ `Canvas` 2D và lưu `SharedPreferences`.

---

## Sprint 2: AAOW App Hub, Đa cửa sổ & Cầu nối SDK/JNI (Đã hoàn thành)
- **Mục tiêu:** Xây dựng trình quản lý ứng dụng trên desktop (giống Google Play Games on PC) và cơ chế bảo vệ/chẩn đoán khi nạp các APK thương mại bên thứ 3.
- **Hạng mục chính:**
  1. Kho quản lý ứng dụng đã cài đặt (`AppRepository`) và giao diện trung tâm `AppHubWindow` (hỗ trợ kéo-thả file `.apk` từ Windows Explorer, tìm kiếm, soi Manifest, xóa dữ liệu, gỡ cài đặt).
  2. Hỗ trợ mở nhiều ứng dụng Android đồng thời trên nhiều cửa sổ Windows độc lập.
  3. Đóng gói ứng dụng mẫu thứ hai: game **AAOW 2048 Puzzle** (`aaow-2048.apk`), kết nối phím mũi tên / WASD của Windows sang `KeyEvent` của Android.
  4. `AndroidSdkFallbackClassLoader`: Tự động lấy các class còn thiếu từ `android.jar` của Android SDK và dùng ASM gỡ bỏ `"Stub!"` ngay khi nạp.
  5. `NativeBridge`: Chặn `System.loadLibrary` và bọc các hàm `native` (JNI) để không sập vì `UnsatisfiedLinkError`.
  6. Màn hình **Báo cáo Tương thích AAOW** & bộ chọn `Activity` trực tiếp trong `HostWindow` khi mở các APK phức tạp (đã kiểm thử với `com.apkpure.aegon.apk`).
- **Tiêu chí nghiệm thu:** Quản lý và chạy song song các APK từ App Hub; mở `com.apkpure.aegon.apk` lên cửa sổ chẩn đoán mà không bị sập ngầm.

---

## Sprint 3: Chuẩn hóa Kỹ thuật, Công cụ Quét API & Đánh giá Kiến trúc (Sprint tiếp theo)
- **Mục tiêu:** Đặt nền móng kỹ thuật vững chắc (Git, kiểm thử tự động, đo lường định lượng) trước khi mở rộng quy mô lớn cho `framework`.
- **Hạng mục chính:**
  1. **Quản lý phiên bản & Khởi động nhanh:** Khởi tạo Git repository (`.gitignore`, commit mốc Sprint 1–2) và cấu hình `installDist` để `aaow.bat` mở ứng dụng tức thì (< 1 giây) không cần chờ Gradle daemon.
  2. **Công cụ phân tích độ tương thích tĩnh (`ApkCompatibilityScanner`):** Quét toàn bộ `classes*.dex` của bất kỳ APK nào để thống kê chính xác: bao nhiêu % class/method `android.*` đã được AAOW cài đặt thật, bao nhiêu % đang phải dùng bản gỡ Stub, và danh sách top các hàm cần viết tiếp.
  3. **Kiểm soát hành vi De-Stub:** Nâng cấp `AndroidSdkFallbackClassLoader` để ghi log chi tiết từng phương thức Stub được gọi lúc chạy (giúp phát hiện ngay hàm Stub nào trả về `null` làm hỏng luồng của APK).
  4. **Bộ APK kiểm thử thực tế (F-Droid Benchmark Suite):** Tuyển chọn 3–5 APK mã nguồn mở thuần Java thực tế từ bên ngoài để làm thước đo chuẩn.
  5. **Thử nghiệm kiến trúc (Spike):** Đánh giá khả năng tái sử dụng một phần mã nguồn Android Framework thật (`android-all` / hướng Robolectric) so với tự viết tiếp module `framework`.
- **Tiêu chí nghiệm thu:** Có Git repo sạch; chạy `aaow.bat --scan <file.apk>` in ra bảng điểm tương thích chi tiết; có quyết định kiến trúc rõ ràng dựa trên số liệu thực nghiệm.

---

## Sprint 4: Mở rộng Android Framework Cốt lõi (UI, Fragment & Ánh xạ AndroidX)
- **Mục tiêu:** Chạy được giao diện của các ứng dụng Android thực tế sử dụng `Fragment`, `Toolbar`/`ActionBar`, `Menu` và các thư viện giao diện phổ biến.
- **Hạng mục chính:**
  1. Cài đặt vòng đời `android.app.Fragment` / `FragmentManager` / `FragmentTransaction` và cầu nối cho `androidx.fragment.app.FragmentActivity` / `AppCompatActivity`.
  2. Hệ thống `Menu`, `MenuInflater` (đọc `res/menu/*.xml`), `Toolbar`, `ActionBar` tương tác thật trên thanh tiêu đề cửa sổ.
  3. Mở rộng `LayoutInflater` (`Factory` / `Factory2`) và bổ sung các container phổ biến: `RecyclerView` (hoặc ánh xạ Adapter), `ViewPager`, `CardView`, `ConstraintLayout` cơ bản.
  4. Hệ thống hoạt ảnh (`ValueAnimator`, `ObjectAnimator`, `Animation`) chạy mượt ở 60fps trên luồng giao diện Windows.
- **Tiêu chí nghiệm thu:** Mở và tương tác được tối thiểu 2–3 ứng dụng thực tế bên thứ 3 (từ bộ APK kiểm thử F-Droid) hiển thị đúng giao diện chính.

---

## Sprint 5: Dữ liệu, Lưu trữ, Mạng & Dịch vụ Nền
- **Mục tiêu:** Đảm bảo các ứng dụng cần cơ sở dữ liệu SQLite, gọi API mạng hoặc chạy dịch vụ nền hoạt động đúng dữ liệu.
- **Hạng mục chính:**
  1. Cài đặt thật `android.database.sqlite.SQLiteDatabase`, `SQLiteOpenHelper`, `Cursor` dựa trên thư viện SQLite JDBC chạy trực tiếp trên ổ cứng Windows.
  2. Cài đặt `ContentResolver`, `ContentProvider`, `ContentValues` và `UriMatcher`.
  3. Hoàn thiện `AsyncTask`, `HandlerThread`, `IntentService`, `Service` (bind/start service nội bộ) và `BroadcastReceiver` (`registerReceiver` / `sendBroadcast`).
  4. Cài đặt `ConnectivityManager`, `NetworkInfo`, `NetworkCapabilities` (báo trạng thái mạng thật của Windows để các app có mạng không báo lỗi "Offline").
- **Tiêu chí nghiệm thu:** Các ứng dụng ghi chú/quản lý dùng SQLite và các ứng dụng tải dữ liệu qua HTTP/HTTPS chạy và lưu trữ dữ liệu chính xác.

---

## Sprint 6: Đa phương tiện, WebView & Tích hợp Sâu Hệ điều hành Windows
- **Mục tiêu:** Kết nối các tính năng đa phương tiện của Android với phần cứng và giao diện của Windows.
- **Hạng mục chính:**
  1. Nâng cấp `android.webkit.WebView` từ khung giữ chỗ sang trình duyệt nhúng thật (JavaFX WebView hoặc JCEF) để hiển thị trang đăng nhập, tin tức, HTML5.
  2. Âm thanh: Cài đặt `MediaPlayer`, `SoundPool`, `AudioTrack` phát âm thanh thật qua hệ thống âm thanh Windows.
  3. Thông báo hệ thống: Nối `NotificationManager` của Android sang thông báo góc màn hình Windows (`SystemTray` / Windows Toast).
  4. Đồng bộ `ClipboardManager` hai chiều và nối `Intent.ACTION_GET_CONTENT` / `ACTION_OPEN_DOCUMENT` với hộp thoại chọn file của Windows.
- **Tiêu chí nghiệm thu:** App phát được nhạc/hiệu ứng âm thanh, hiển thị trang web trong `WebView` và bắn thông báo ra khay hệ thống Windows.

---

## Sprint 7: Tầng Thư viện Native (`.so` / JNI) & Lớp Tương thích Dịch vụ Google (microG-lite)
- **Mục tiêu:** Giải quyết 2 rào cản lớn của các ứng dụng thương mại: thư viện C/C++ `.so` và sự phụ thuộc vào Google Play Services / Firebase.
- **Hạng mục chính:**
  1. Hỗ trợ nạp thư viện native khi APK có sẵn bản `lib/x86_64/*.so` (hoặc viết tầng dịch JNI cho các thư viện phổ biến như `libmmkv`, `libsqlite`, `libz`).
  2. Nghiên cứu tích hợp bộ giả lập CPU người dùng (như Unicorn Engine / Dynarmic) cho các hàm tính toán `.so` ARM64 nhỏ gọn.
  3. Xây dựng lớp tương thích nhẹ cho `FirebaseApp`, `GoogleApiAvailability`, `AdvertisingIdClient` để các APK thương mại (như APKPure) không bị văng `IllegalStateException: Default FirebaseApp is not initialized` lúc khởi động.
- **Tiêu chí nghiệm thu:** Các ứng dụng có khởi tạo Firebase / GMS cơ bản hoặc thư viện native phổ biến vượt qua màn hình Splash và vào được màn hình chính.

---

## Sprint 8: Tối ưu Hiệu năng, Đóng gói Bộ cài Windows (`.exe`) & Phát hành
- **Mục tiêu:** Biến dự án thành sản phẩm phần mềm Windows hoàn chỉnh, dễ cài đặt cho người dùng cuối.
- **Hạng mục chính:**
  1. Tối ưu bộ nhớ đệm hình ảnh, tốc độ dịch DEX và độ mượt giao diện (60fps, hỗ trợ phóng to/thu nhỏ cửa sổ và xoay ngang/dọc màn hình).
  2. Đóng gói bằng công cụ `jpackage` của JDK 21 thành file thực thi `AAOW.exe` và bộ cài đặt Windows (`.msi` / `.exe`) tự kèm runtime Java rút gọn (`jlink`).
  3. Đăng ký File Association trên Windows: người dùng nhấp đúp vào bất kỳ file `.apk` nào trong File Explorer sẽ mở hoặc cài đặt trực tiếp bằng AAOW.
- **Tiêu chí nghiệm thu:** Cài đặt và chạy `AAOW.exe` như một phần mềm Windows độc lập, nhấp đúp file `.apk` chạy ngay.
