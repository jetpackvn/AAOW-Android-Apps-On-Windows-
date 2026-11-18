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

## ✅ Biên bản Nghiệm thu Sprint 3 (Đã hoàn thành)
- **Thời gian hoàn thành:** 18/11/2026
- **Giải quyết toàn bộ điểm hạn chế Sprint 2 & Hạng mục Sprint 3 đã nghiệm thu:**
  1. **Chuẩn hóa Git & Khởi động nhanh (`installDist`):**
     - Cập nhật [.gitignore](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/.gitignore) bỏ qua `bin/` / `**/bin/` và gỡ file build cache `framework/bin/...` khỏi chỉ mục Git.
     - Nâng cấp [aaow.bat](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/aaow.bat) chạy trực tiếp bản phân phối đã biên dịch sẵn (`launcher/build/install/aaow/bin/aaow.bat`), hỗ trợ `--rebuild` và biến `AAOW_HOME` trong [Main.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/launcher/src/main/java/dev/aaow/launcher/Main.java). Thời gian mở CLI `--info` đo qua `cmd /c` còn ~1,47 giây (không còn tốn 7–10 giây chờ Gradle mỗi lần chạy).
  2. **Công cụ Phân tích Tương thích Tĩnh (`ApkCompatibilityScanner`):**
     - Xây dựng [ApkCompatibilityScanner.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/launcher/src/main/java/dev/aaow/launcher/ApkCompatibilityScanner.java), tích hợp cả CLI (`.\aaow.bat --scan <file.apk>`) lẫn nút **"Quét API"** trực tiếp trên mỗi thẻ trong [AppHubWindow.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/launcher/src/main/java/dev/aaow/launcher/AppHubWindow.java).
     - Phân giải kế thừa đa tầng (APK DEX -> `:framework` -> `android.jar` -> JDK), phân biệt rõ 4 trạng thái (`IMPLEMENTED`, `MISSING_METHOD_ON_FRAMEWORK_CLASS` [Nguy cơ `NoSuchMethodError`], `STUBBED_VIA_SDK`, `MISSING_COMPLETELY`), thống kê thư viện đóng gói và xuất Top 20 class/method thiếu.
  3. **Nâng cấp Smart De-Stub & Theo dõi lời gọi lúc chạy:**
     - Nâng cấp [AndroidSdkFallbackClassLoader.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/launcher/src/main/java/dev/aaow/launcher/AndroidSdkFallbackClassLoader.java): trả về giá trị rỗng an toàn (`""`, `Collections.emptyList/Set/Map/Iterator`, `Optional.empty()`, mảng độ dài 0 `NEWARRAY`/`ANEWARRAY`/`MULTIANEWARRAY`, fluent builder `this`, khởi tạo `enum` constants & `values()`), đồng thời chèn hook [NativeBridge.onStubMethodCall](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/framework/src/main/java/dev/aaow/rt/NativeBridge.java) hiển thị top hàm Stub được gọi trên [HostWindow.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/framework/src/main/java/dev/aaow/rt/HostWindow.java).
     - Nâng cấp [DexLoader.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/launcher/src/main/java/dev/aaow/launcher/DexLoader.java): dùng Smart Default Return cho hàm `native` và chuẩn hóa class/inner-class modifier flags (gỡ `ACC_STATIC` trên class header và gỡ `ACC_FINAL` khi đi kèm `ACC_ABSTRACT`) để chữa dứt điểm `ClassFormatError: 0x419` trên các APK dùng R8/AndroidX.
  4. **Chuẩn hóa Cây Kế thừa & Bổ sung Nhóm API Thiếu Hàng Đầu:**
     - Sửa toàn bộ lỗi lệch superclass so với `android.jar`: bổ sung `AbsSeekBar`, `AbsListView`, `InputEvent`, `PackageItemInfo`, `ComponentInfo`, `ServiceInfo`, `AndroidException`, `AbsoluteLayout`, `Checkable`, `SpinnerAdapter`, `Spanned`, `Spannable`, `GetChars`, `SpannableStringBuilder`, `Parcelable` (và `Parcel`).
     - Bổ sung nhóm class/method đứng đầu bảng thiếu từ quét thực tế: `org.json.*` (`JSONObject`, `JSONArray`, `JSONTokener`, `JSONException`), `PreferenceManager`, `HandlerThread`, `LocaleList`, `Configuration.setLocales`/`equals`, `Uri` (`fromFile`, `getQueryParameter`, `Builder`, `getPathSegments`), `ContentValues`, `ContentResolver`, `BroadcastReceiver`, `IntentFilter`, `Toolbar`, `HorizontalScrollView`, `ViewAnimator`, `ViewSwitcher`, `WebSettings`, `WebViewClient`, `ViewTreeObserver`, `ViewPropertyAnimator`, `SparseIntArray`, `Pair`, `Matrix`, `PointF`, `Point`, `Display`, `PorterDuff`, `ActionBar.setCustomView`, `Environment.isExternalStorageManager`, các hàm tag/callback/scroll của `View`/`Drawable`/`Activity`.
  5. **Thử nghiệm Kiến trúc (Architecture Spike) & Bộ Kiểm thử Tự động (JUnit 5):**
     - Lập tài liệu đánh giá kiến trúc [ARCHITECTURE_SPIKE.md](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/docs/ARCHITECTURE_SPIKE.md): đối chiếu thực nghiệm `android-all-15-robolectric-12650502.jar` (176,31 MB) vs AAOW (`framework.jar` 353,7 KB; tổng `lib/*.jar` 2,42 MB) -> chốt hướng **Selective Hybrid Compatibility Layer**.
     - Thiết lập JUnit 5 với 3 bộ test (6/6 tests PASSED): [AndroidSdkFallbackClassLoaderTest.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/launcher/src/test/java/dev/aaow/launcher/AndroidSdkFallbackClassLoaderTest.java), [FrameworkHierarchyTest.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/launcher/src/test/java/dev/aaow/launcher/FrameworkHierarchyTest.java) (khóa chặt 0 `SUPER_MISMATCH` với `android.jar`), và [ApkCompatibilityScannerTest.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/launcher/src/test/java/dev/aaow/launcher/ApkCompatibilityScannerTest.java).
- **Kết quả đo lường trên Bộ APK Kiểm thử (2 APK mẫu + 5 APK F-Droid + APKPure):**
  - `aaow-showcase.apk` & `aaow-2048.apk`: **100%** tương thích class/method/call-sites, chạy hoàn chỉnh (0 hồi quy).
  - `org.billthefarmer.editor_198.apk` (F-Droid Editor): Tỉ lệ call-site khớp `:framework` tăng từ **55,2% → 83,0%** (class `51,0% → 65,6%`), **khởi chạy thành công lên giao diện thật trên cửa sổ Windows**.
  - `org.billthefarmer.notes_142.apk` (F-Droid Notes): Tỉ lệ call-site khớp `:framework` tăng từ **60,3% → 81,5%** (class `47,3% → 60,9%`), **khởi chạy thành công lên giao diện thật (có `ActionBar`, `WebView`, nút FAB xanh)**.
  - `org.woheller69.level_35.apk` (F-Droid Bubble Level, dùng AndroidX `AppCompatActivity`): Call-site đạt **71,2%**; đã vượt qua `VerifyError` (`AbsSeekBar`), `ClassFormatError` (`0x419`) và đi vào bên trong `AppCompatDelegateImpl.onCreate()`, hiện dừng tại `Activity.getOnBackInvokedDispatcher()`.
  - `com.jarsilio.android.scrambledeggsif_88.apk` (F-Droid Scrambled Exif): Call-site đạt **70,1%**.
  - `org.secuso.privacyfriendlynotes_105.apk` (F-Droid PF Notes): Call-site tăng từ **57,2% → 69,8%**.
  - `com.apkpure.aegon.apk`: Call-site khớp `:framework` tăng từ **62,1% → 80,1%** (59.360/74.095 điểm gọi); đã vượt qua `attachBaseContext()` để vào tới `SplashActivity.onCreate()` (hiện dừng ở `NoClassDefFoundError: tmsdk.fg.module.cleanV2.CleanManager` và thiếu `kotlinx-coroutines-android` Main dispatcher / `FirebaseApp`).
- **Đánh giá trung thực những điểm còn hạn chế sau Sprint 3:**
  - 2 APK F-Droid không dùng AndroidX (`Editor`, `Notes`) đã mở lên màn hình chính thật, nhưng các tính năng menu/hộp thoại nâng cao (Option Menu, File Picker, Markdown HTML trong `WebView`) vẫn cần Sprint 4 và Sprint 6 để tương tác trọn vẹn.
  - Các APK dựa trên AndroidX `AppCompatActivity` / `FragmentActivity` (`Level`, `PF Notes`, `Scrambled Exif`) vẫn còn thiếu các lớp/hàm API hiện đại (`OnBackInvokedDispatcher`, `FragmentManager` native bridge, `MenuInflater`, `RecyclerView`, `Animator`...).
  - `com.apkpure.aegon.apk` vẫn bị chặn ở các lớp thương mại/GMS (`FirebaseApp`, Tencent dynamic plugin `CleanManager`, `kotlinx-coroutines-android` Main dispatcher).

---

## 🎯 Chuẩn bị cho Sprint 4 (Mục tiêu cho phiên làm việc tiếp theo)

Khi bắt đầu triển khai **Sprint 4 (Mở rộng Android Framework Cốt lõi: UI, Fragment & Ánh xạ AndroidX)**, thực hiện lần lượt các bước sau:
1. **Bước 4.1 — Hỗ trợ chuỗi khởi chạy `AppCompatActivity` & `FragmentActivity`:**
   - Giải quyết các điểm nghẽn đứng đầu trên `org.woheller69.level` và các app AndroidX: `OnBackInvokedDispatcher` (API 33), `Window.Callback` / `ActionBar` / `MenuInflater` (đọc `res/menu/*.xml` thật và hiển thị nút menu trên `ActionBar`/`Toolbar`), `android.app.Fragment` / `FragmentManager` / `FragmentTransaction`.
2. **Bước 4.2 — Hệ thống Container & Adapter phổ biến:**
   - Cài đặt cơ bản/ánh xạ cho `RecyclerView` (hoặc nối Adapter sang `LinearLayout`/`ListView` ảo), `CardView`, `ViewPager`, `ConstraintLayout` cơ bản trong `LayoutInflater`, giúp các app dùng `androidx.*` hiển thị đầy đủ giao diện.
3. **Bước 4.3 — Hoạt ảnh & Đa chạm cơ bản:**
   - Cài đặt `ValueAnimator`, `ObjectAnimator`, `AnimatorSet`, `ScaleGestureDetector`, `GestureDetector` thật chạy trên Swing `Timer` 60fps.
4. **Bước 4.4 — Kiểm thử & Nghiệm thu Sprint 4:**
   - Kiểm thử tương tác thật trên bộ APK F-Droid (mở menu của `Editor`/`Notes`, mở được giao diện chính của `org.woheller69.level` và ít nhất 1 app AndroidX khác), chạy `.\gradlew.bat test` (giữ 100% PASS) và cập nhật biên bản vào file này.
