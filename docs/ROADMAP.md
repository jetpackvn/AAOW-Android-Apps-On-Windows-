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

## Sprint 3: Chuẩn hóa Kỹ thuật, Công cụ Quét API & Đánh giá Kiến trúc (Đã hoàn thành)
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

---

# Giai đoạn 2: Thực chiến Chạy & Tương tác APK Thật (Offline → Non-GMS → GMS/Commercial)

Giai đoạn 1 (Sprint 1–8) đã dựng xong nền tảng kỹ thuật và bộ cài Windows độc lập. Giai đoạn 2 tập trung hoàn toàn vào việc **mở, chạy và sử dụng thực tế các file `.apk` ngoài đời** theo lộ trình bậc thang từ dễ đến khó.

## Sprint 9 (Phase A): Phá rào `VerifyError` Bytecode & Lấp lỗ hổng Core Runtime (Đã hoàn thành)
- **Mục tiêu:** Giải quyết triệt để lỗi JVM `VerifyError` khi dịch DEX của các APK hiện đại (Kotlin/R8/AndroidX `ComponentActivity` & `InitializationProvider`) và thay thế các class tiện ích lõi đang bị Stub rỗng.
- **Hạng mục chính:**
  1. **Sửa lỗi kiểu Bytecode trong `DexLoader` (ASM Bytecode Verifier Fixer):**
     - Phân tích kiểu cục bộ/stack bằng ASM để tự động chèn `CHECKCAST` hợp lệ khi `dex2jar` suy luận thanh ghi dùng chung thành `java/lang/Object` trước các lệnh `PUTFIELD`, `PUTSTATIC`, `GETFIELD`, `INVOKEVIRTUAL`/`INVOKESPECIAL`/`INVOKEINTERFACE`, `ARETURN`, `ARRAYLENGTH`, `AALOAD`/`AASTORE`...
     - Làm sạch triệt để lỗi `Expecting a stackmap frame` (`V1_6` + bỏ `StackMapTable` hỏng, hoặc `V1_5` an toàn cho class không có `invokedynamic`) để nạp 100% class AndroidX/Kotlin hiện đại mà không văng `VerifyError`.
  2. **Cài đặt thật các lớp Core Runtime đang bị rơi về Stub:**
     - `android.util.LruCache<K, V>` (bộ nhớ đệm thật dựa trên `LinkedHashMap`, hỗ trợ `sizeOf`, `create`, `entryRemoved`, `trimToSize`, `evictAll`, `snapshot`).
     - `android.view.Choreographer` (`getInstance()`, `postFrameCallback`, `postFrameCallbackDelayed`, `removeFrameCallback`, chạy nhịp 60fps thật trên Main Looper / Swing Timer).
     - `android.util.Base64InputStream`, `android.util.Base64OutputStream`, `android.util.LogPrinter`.
     - `android.os.SystemProperties` (hidden API của AOSP trả thông số thiết bị AAOW chuẩn), `android.os.Debug` (`isDebuggerConnected`, `waitingForDebugger`, `MemoryInfo`).
  3. **Tinh chỉnh Runtime Permissions (`Auto-Grant Async`):**
     - Bảo đảm `Activity.requestPermissions()` post bất đồng bộ `onRequestPermissionsResult()` về Main Looper (tránh đệ quy/phá vòng đời nếu gọi trong `onCreate`/`onResume`), bổ sung `shouldShowRequestPermissionRationale()`.
- **Tiêu chí nghiệm thu:** `com.jarsilio.android.scrambledeggsif_88.apk` vượt qua 100% lỗi `VerifyError` trên `InitializationProvider` và `ComponentActivity`; `LruCache`, `Choreographer`, `SystemProperties`, `Base64*Stream` chạy thật với unit test JUnit 5; toàn bộ test cũ không hồi quy.

---

## Sprint 10 (Phase B): Chạy & Tương tác Trọn vẹn Các Ứng dụng Offline (Offline Apps)
- **Mục tiêu:** Mở lên và **bấm dùng thật trọn vẹn luồng tính năng chính** của 5 ứng dụng Offline thực tế từ AAOW App Hub (không chỉ hiện khung màn hình tĩnh).
- **Hạng mục chính:**
  1. Hoàn thiện tương tác cho bộ 5 APK Offline:
     - `org.billthefarmer.editor` (tạo, gõ, lưu, mở file văn bản).
     - `org.billthefarmer.notes` (xem, bấm sửa, lưu Markdown và kết xuất lại trên WebView).
     - `org.woheller69.level` (đóng dialog cảnh báo, tương tác màn hình chính & Settings).
     - `com.jarsilio.android.scrambledeggsif` (mở UI chính, chọn ảnh qua Windows File Picker, cài `ExifInterface` xóa EXIF và lưu ảnh qua `Bitmap.compress()`).
     - `org.secuso.privacyfriendlynotes` (bấm Skip/Next qua `TutorialActivity` → `MainActivity` → mở Drawer / bấm FAB `+` → tạo Text/Checklist Note → lưu Room SQLite → hiện danh sách trên `RecyclerView` và mở xem lại).
  2. Hoàn thiện tương tác các thành phần AndroidX/Material: `CompatViewPager`, `CompatRecyclerView` (cuộn, click item, refresh adapter), `DrawerLayout` cơ bản, `TextInputLayout`.
  3. Viết bộ kiểm thử tương tác tự động (mô phỏng click/nhập liệu) và bổ sung cấu hình nạp tiện lợi trong `AppHubWindow`.
- **Tiêu chí nghiệm thu:** Mở và thao tác đầu-cuối thành công trên cả 5 APK Offline thực tế.

---

## Sprint 11 (Phase C): Ứng dụng Không dùng Google Play Services (Non-GMS Online & Media Apps)
- **Mục tiêu:** Chạy được các ứng dụng thực tế có kết nối mạng (REST API, tải ảnh, đọc tin/RSS/thời tiết/từ điển) nhưng không phụ thuộc Google Play Services.
- **Hạng mục chính:**
  1. Tuyển chọn 2–3 APK F-Droid thực tế có mạng làm bộ kiểm thử Non-GMS Online.
  2. Hoàn thiện tầng mạng & callback: `ConnectivityManager.registerDefaultNetworkCallback`/`registerNetworkCallback` (chủ động bắn `onAvailable`/`onCapabilitiesChanged`), `WifiManager`, `TrafficStats`, `NetworkSecurityPolicy`, chuẩn hóa TLS/SSL cho `OkHttp`/`Retrofit`.
  3. Hoàn thiện tải & giải mã media: `ImageDecoder`, mở rộng `BitmapFactory` cho `Glide`/`Picasso`/`Coil`, `MediaStore` ánh xạ sang thư mục dữ liệu Windows, nối `Intent.ACTION_VIEW` (http/https) ra trình duyệt Windows.
- **Tiêu chí nghiệm thu:** Tối thiểu 2–3 ứng dụng Non-GMS có mạng tải dữ liệu thật từ Internet, hiển thị ảnh và cho phép lướt/tương tác bình thường.

---

## Sprint 12 (Phase D1): Hạ tầng GMS Khởi động Sớm, Chứng chỉ X.509 Thật & AIDL `microG-lite`
- **Mục tiêu:** Loại bỏ các lỗi khởi tạo sớm và kiểm tra toàn vẹn chữ ký/GMS trên các APK thương mại lớn.
- **Hạng mục chính:**
  1. Chuẩn hóa khởi động sớm: gán `Process.myProcessName()` ngay từ đầu, khởi tạo sớm `FirebaseApp` trước `Application.attachBaseContext()` cho các app dùng Tinker/MultiDex proxy.
  2. Trích xuất chứng chỉ X.509 DER thật từ `META-INF/*.RSA`/`*.DSA`/`*.EC` trong chính file APK cho `PackageManager.getPackageInfo(GET_SIGNATURES | GET_SIGNING_CERTIFICATES)`.
  3. Hoàn thiện AIDL Binder descriptor của `microG-lite` (`com.google.android.gms.measurement.START`, `analytics.service.START`, `ads.identifier.service.START`) và callback `onServiceConnected`.
- **Tiêu chí nghiệm thu:** Loại bỏ hoàn toàn `Default FirebaseApp is not initialized`, `CertificateException: Incomplete BER/DER data`, `Got binder with a wrong descriptor` và `AdvertisingIdClient IOException` khi nạp `com.apkpure.aegon.apk`.

---

## Sprint 13 (Phase D2): Thực chiến Ứng dụng Thương mại & Mở rộng Native JNI HLE
- **Mục tiêu:** Gỡ các lỗi dây chuyền (`NoSuchMethodError`, `NoSuchFieldError`, reflection `mNativePtr`, Material/Custom View) để đưa các APK thương mại mục tiêu (như `com.apkpure.aegon.apk`) vượt qua màn hình Splash vào màn hình chính.
- **Hạng mục chính:**
  1. Truy vết và vá triệt để các `NoSuchMethodError`/`NoSuchFieldError` trên luồng UI & luồng nền của APK thương mại mục tiêu.
  2. Tự động hỗ trợ các trường reflection JNI phổ biến (`mNativePtr`) và mở rộng JNI HLE cho các thư viện `.so` thương mại thường gặp.
  3. Đóng gói lại bộ cài Windows (`AAOW-Setup`) sau khi nghiệm thu khả năng chạy APK thật.
- **Tiêu chí nghiệm thu:** `com.apkpure.aegon.apk` (và/hoặc APK thương mại mục tiêu) vượt qua `SplashActivity` vào được giao diện chính trên cửa sổ Windows.
