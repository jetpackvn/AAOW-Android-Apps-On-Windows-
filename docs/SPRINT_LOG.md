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

---

## Sprint 4: Mở rộng Android Framework Cốt lõi (UI, Fragment, Animation & Tương thích AndroidX) — ✅ Đã nghiệm thu

### Các hạng mục đã hoàn thành
1. **Fragment, Menu, ActionBar/Toolbar & Vòng đời Hiện đại:**
   - Hoàn thiện `android.app.Fragment`, `FragmentManager`, `FragmentTransaction` (hỗ trợ `add`, `replace` kèm `expandOps`/`popBackStackImmediate`, `hide`/`show`, `attach`/`detach`, `FragmentLifecycleCallbacks`, đồng bộ View trong container), `PreferenceFragment`/`PreferenceScreen`/`PreferenceManager` đọc XML cài đặt.
   - Bổ sung `Activity.registerActivityLifecycleCallbacks()` + `default onActivityPre*/Post*` trên `Application.ActivityLifecycleCallbacks` (cho Jetpack Lifecycle `ReportFragment`), `OnBackInvokedDispatcher`/`OnBackInvokedCallback` ưu tiên theo mức độ ưu tiên trước khi fall back sang `FragmentManager.popBackStackImmediate()`, `Window.Callback`, `WindowInsets` + `WindowInsets.Builder` + `WindowInsetsController`.
   - Hoàn thiện `SimpleMenu`, `MenuInflater` đọc trực tiếp `res/menu/*.xml` (mục chính, `SubMenu`, icon vector, cờ `showAsAction`), thanh `Toolbar` / `ActionBar` & overlay dropdown `HostWindow` (kèm tự động ẩn ActionBar tổng hợp khi Activity đã nạp `ActionBarOverlayLayout`/`Toolbar` của AndroidX).
2. **Resource XML Parser, Theme Stack & AndroidX/Material Containers:**
   - Thêm `XmlPullParser` / `XmlPullParserException` / `XmlResourceParser` / `AxmlXmlResourceParser` / `android.util.Xml` và cải tiến `Resources.getXml`/`TypedArray` + stack nhiều tầng style trong `Resources.Theme.applyStyle(resId, force)` (tránh trường hợp `Theme.AppCompat.Empty` ghi đè làm mất thuộc tính cha của `LevelTheme`).
   - Sửa `LayoutInflater` bảo toàn instance theo từng `Activity`, hỗ trợ `Factory`/`Factory2` + private `Activity` factory (cho Jetpack `FragmentContainerView`), thẻ `<view class="..."/>`, ưu tiên class AndroidX/Material thật trước rồi fallback thông minh sang `CompatConstraintLayout`, `CompatCardView`, `CompatRecyclerView`, `CompatViewPager`.
   - Bổ sung loạt lớp UI & đồ họa nền tảng giúp thành phần AndroidX/Material hoạt động trực tiếp: `ViewStub`, `SurfaceView`/`SurfaceHolder`/`Surface` (double-buffer Bitmap), `CheckedTextView`, `AutoCompleteTextView` (`setOnItemClickListener`/`getDropDownAnchor`), `ViewOutlineProvider`, `VectorDrawable` (gói chuẩn `android.graphics.drawable`), `Path.op`/`transform`/`rLineTo`, `Matrix.setRectToRect`/`ScaleToFit`, `Xfermode`/`PorterDuffXfermode`, `Handler.createAsync` (cho Kotlin Coroutines `Dispatchers.Main`), `View.Property` (`ALPHA`, `TRANSLATION_Z`, v.v.), `Drawable.getConstantState()`, `Html.fromHtml()`, `Observable`/`DataSetObservable`, `AccessibilityManager`/`AccessibilityDelegate`, `AudioAttributes`/`SoundPool` và `SensorManager` cảm biến ảo.
3. **Hệ thống Animation 60fps & Cử chỉ:**
   - Triển khai `ValueAnimator`, `ObjectAnimator`, `AnimatorSet`, `PropertyValuesHolder`, `TimeInterpolator`, `TypeEvaluator`/`ArgbEvaluator`, `PathInterpolator`, `ViewPropertyAnimator`, họ `View` `Animation` (`AlphaAnimation`, `TranslateAnimation`, `ScaleAnimation`, `RotateAnimation`, `AnimationSet`), `GestureDetector` & `ScaleGestureDetector` đa điểm chạm.
4. **Ổn định CLI Headless Screenshot:**
   - Cập nhật `--screenshot` & `--screenshot-hub` trong `Main.java` luôn thoát sạch JVM sau khi chụp/lỗi (tránh treo bởi non-daemon Swing EDT) và chạy 1 nhịp EDT flush trước khi chụp `SurfaceView`/`Handler.post`.

### Kết quả Nghiệm thu Thực tế Sprint 4
- **Unit test JUnit 5 (`.\gradlew.bat test --rerun-tasks --console=plain`):** **12/12 PASSED** (0 failures, 0 skipped):
  - `AndroidSdkFallbackClassLoaderTest` (2), `ApkCompatibilityScannerTest` (2), `FrameworkHierarchyTest` (2 — **0 sai lệch superclass** đối chiếu `android.jar` cho toàn bộ lớp `android.*` mới), `Sprint4FrameworkTest` (6 — kiểm thử `MenuInflater`/`Toolbar` trên APK thật `editor_198` & `notes_142`, `FragmentManager` back stack & `OnBackInvokedDispatcher`, `Resources.Theme` stack, `ViewStub`, `Compat*`, `Animator`/`GestureDetector`, `SurfaceView`/`SensorManager`).
- **Độ phủ API tĩnh (`.\aaow.bat --scan`) sau Sprint 4 (theo Call-Sites):**
  - `org.billthefarmer.editor_198.apk`: **94.4%** call-sites (`805/853` khớp `:framework` thật).
  - `org.billthefarmer.notes_142.apk`: **93.3%** call-sites (`856/917` khớp `:framework` thật).
  - `org.woheller69.level_35.apk`: tăng từ `71.2%` -> **87.1%** call-sites (`8,542/9,804`).
  - `org.secuso.privacyfriendlynotes_105.apk`: tăng từ `69.8%` -> **83.2%** call-sites (`29,555/35,539`).
- **Kiểm thử chạy APK & xuất ảnh thực tế (`build/sprint4-final/*.png`):**
  - **Chạy thật trọn vẹn (0 lỗi):** `aaow-showcase.apk`, `aaow-2048.apk`, `org.billthefarmer.editor_198.apk` (hiển thị ActionBar `Editor.txt` + `Edit` / `New file` / `Open file...` / `⋮`), `org.woheller69.level_35.apk` (vào `Level`, hiển thị Toolbar `Bubble` và hộp thoại `FreeDroidWarn` ngắt dòng sạch đẹp; 0 stub warning), `org.secuso.privacyfriendlynotes_105.apk` (`SplashActivity -> TutorialActivity` với nút `Skip`, chấm `• • •`, `Next`; 0 stub warning), và `AAOW App Hub` (`hub.png`).
  - **Chạy có phạm vi Sprint kế tiếp / giữ chỗ nhẹ:**
    - `org.billthefarmer.notes_142.apk`: ActionBar và nút tác vụ hiển thị đầy đủ không lỗi, vùng nội dung hiển thị `WebView: about:blank` vì WebView thật thuộc **Sprint 6**.
    - `org.secuso.privacyfriendlynotes_105.apk` (`MainActivity`): UI AndroidX/Material (`DrawerLayout`, `AppBarLayout`, `MaterialToolbar`, `SearchView`, `RecyclerView`, `CoordinatorLayout`, `FragmentContainerView` + `MainFABFragment` nút `+`) đã khởi tạo và dàn trang không còn lỗi UI hay fallback; luồng nền `Room`/`FrameworkSQLiteOpenHelper` dừng tại `Context.getDatabasePath` + `SQLiteOpenHelper` thuộc **Sprint 5**.
    - `com.jarsilio.android.scrambledeggsif_88.apk` vẫn giữ giới hạn JVM bytecode verifier từ R8 (`kotlin.UNINITIALIZED_VALUE`), không nằm trong mục tiêu Sprint 4.
    - Một số thành phần dùng giữ chỗ nhẹ đúng phạm vi Sprint 4: `SoundPool` chưa phát âm thanh thật (thuộc Sprint 6), `SensorManager` cung cấp dữ liệu cảm biến tĩnh ảo, và `MaterialToolbar` chưa tô nền `MaterialShapeDrawable` theo tint từ theme.

### Chuẩn bị cho Sprint tiếp theo (Sprint 5 — Dữ liệu, Lưu trữ, Mạng & Đa luồng Hệ thống)
- Cài đặt `SQLiteDatabase`, `SQLiteOpenHelper`, `Cursor`/`MatrixCursor`, `Context.getDatabasePath()` (kết nối JDBC SQLite) để ứng dụng dùng `Room` (như `PFNotes MainActivity`) đọc/ghi dữ liệu thật.
- Hoàn thiện `ContentResolver`/`ContentProvider` (mở/đọc URI tệp trên Windows), `BroadcastReceiver`, `Service`, `HttpURLConnection`, `ConnectivityManager` và đa luồng nền (`AsyncTask`/`HandlerThread`).

---

## Sprint 5: Dữ liệu, Lưu trữ, Mạng & Đa luồng Hệ thống — ✅ Đã nghiệm thu

### Các hạng mục đã hoàn thành
1. **Cơ sở dữ liệu SQLite thật (`org.xerial:sqlite-jdbc:3.54.1.0`) & Hệ thống `Cursor`:**
   - Thêm phụ thuộc `org.xerial:sqlite-jdbc:3.54.1.0` vào `:framework` (`framework/build.gradle.kts`) để chạy SQLite thật trực tiếp trên ổ cứng Windows.
   - Cài đặt đầy đủ `android.database.*` & `android.database.sqlite.*`: `SQLiteDatabase` (`openDatabase`, `openOrCreateDatabase`, `createInMemory`, `deleteDatabase`, giao dịch `beginTransaction`/`setTransactionSuccessful`/`endTransaction`, `SQLiteTransactionListener`, truy vấn & cập nhật tham số hóa), `SQLiteOpenHelper` (`onConfigure`/`onCreate`/`onUpgrade`/`onDowngrade`/`onOpen`), `SQLiteProgram`, `SQLiteStatement`, `SQLiteQuery`, `SQLiteCursor`, `SQLiteQueryBuilder`, `Cursor`, `AbstractCursor`, `AbstractWindowedCursor`, `CursorWindow`, `MatrixCursor`, `CursorWrapper`, `CrossProcessCursor`/`CrossProcessCursorWrapper`, `ContentObserver`, `DatabaseUtils`, `DefaultDatabaseErrorHandler` và bộ ngoại lệ `SQLite*Exception`.
   - Giải quyết vấn đề giữ khóa file `.db` trên Windows với AndroidX `Room` (`SharedSQLiteStatement` được cache không đóng ngay khi `RoomDatabase.close()`) bằng cách không giữ reference `SQLiteDatabase` suốt vòng đời `SQLiteProgram`, giúp `RoomDatabase.close()` giải phóng kết nối JDBC ngay lập tức.
   - Bổ sung quản lý đường dẫn dữ liệu ứng dụng trên Windows (`Context.getDatabasePath()`, `openOrCreateDatabase()`, `deleteDatabase()`, `databaseList()`).
2. **`ContentResolver`, `ContentProvider`, Manifest Metadata & IPC Binder:**
   - Mở rộng [AndroidManifest.java](file:///d:/Project/AAOW%20%28Android%20Apps%20On%20Windows%29/apk/src/main/java/dev/aaow/apk/AndroidManifest.java) phân tích `<service>`, `<receiver>`, `<provider>`, `<meta-data>`, `<intent-filter>`; bổ sung `PackageManager.resolveContentProvider`, `PackageItemInfo.loadXmlMetaData`, `PackageManager.getServiceInfo`/`getResourcesForApplication`/`getXml`/`hasSystemFeature`, `ProviderInfo`, `PathPermission`, `PatternMatcher`.
   - Cài đặt `ContentResolver`, `ContentProvider`, `ContentUris`, `UriMatcher`, `ParcelFileDescriptor` (`AutoCloseInputStream`/`AutoCloseOutputStream`), `AssetFileDescriptor`, `OperationApplicationException` hỗ trợ đọc/ghi `file://` và định tuyến `content://` (kể cả nạp XML đường dẫn của `androidx.core.content.FileProvider` / `android.support.v4.content.FileProvider`).
   - Hoàn thiện `IBinder`, `Binder`, `IInterface`, `RemoteException`, `CancellationSignal`/`OperationCanceledException` và mở rộng `Parcel` (`writeStrongBinder`, `readStrongBinder`, `writeStrongInterface`, `writeNoException`, `readException`), `BaseBundle`/`Bundle`, `IntentFilter`, `Uri`.
3. **Đa luồng Hệ thống (`Looper`/`HandlerThread`/`AsyncTask`), `Service` & `BroadcastReceiver`:**
   - Viết lại `Looper`, `Message`, `Handler`, `HandlerThread` với hàng đợi tin nhắn có độ trễ cho luồng nền và đồng bộ `Looper.getMainLooper().getThread()` đúng tới Swing EDT (`AWT-EventQueue-0`), khắc phục `LiveData.assertMainThread()` và đảm bảo `HostWindow.renderHeadlessScreenshot()` chạy vòng đời `Activity` trên đúng luồng giao diện chính.
   - Cài đặt `AsyncTask` (thực thi trên background thread và trả kết quả `onProgressUpdate`/`onPostExecute` về Main Looper), `Service`, `IntentService`, `ServiceConnection` (`startService`/`stopService`/`bindService`/`unbindService` nội bộ trong `AndroidRuntimeContext` và tự động `stopAllServices()` khi đóng cửa sổ `HostWindow`), và `BroadcastReceiver` (`registerReceiver`, `unregisterReceiver`, `sendBroadcast`, `sendOrderedBroadcast`, `sendStickyBroadcast`, `goAsync()`/`PendingResult`).
4. **Kết nối Mạng (`ConnectivityManager`) & API hỗ trợ phát sinh:**
   - Cài đặt `ConnectivityManager`, `Network`, `NetworkInfo`, `NetworkCapabilities`, `NetworkRequest` thăm dò giao diện mạng Windows thật qua `java.net.NetworkInterface` (báo trạng thái kết nối online cho các ứng dụng/thư viện truy vấn `ConnectivityManager`, kết hợp `java.net.HttpURLConnection` chuẩn của Java 21).
   - Bổ sung một số API hỗ trợ phát sinh khi kiểm thử APK thật: `StateSet`, `TextUtils.split`, `Paint.setShadowLayer`/`clearShadowLayer`, `SparseArray` mở rộng, `Configuration.fontWeightAdjustment`, `TextView` (`setMinWidth`/`getMinWidth`/`setMaxWidth`/`getMaxWidth`, `getLetterSpacing`/`setLetterSpacing`, `setShowSoftInputOnFocus`/`getShowSoftInputOnFocus`, `getTextColors`), `AutoCompleteTextView.setText(CharSequence, boolean)`/`setOnDismissListener`, `Adapter`/`BaseAdapter`/`ViewGroup.setAddStatesFromChildren`.

### Kết quả Nghiệm thu Thực tế Sprint 5
- **Unit test JUnit 5 (`.\gradlew.bat test --rerun-tasks --console=plain`):** **17/17 PASSED** (0 failures, 0 errors, 0 skipped):
  - `AndroidSdkFallbackClassLoaderTest` (2), `ApkCompatibilityScannerTest` (2), `FrameworkHierarchyTest` (2 — **0 sai lệch superclass** khi đối chiếu `android.jar`, đã kiểm tra thêm hệ phân cấp SQLite, `Service`/`IntentService`, `ProviderInfo`, `RemoteException` và giao diện `Parcelable` cho các lớp mới), `Sprint4FrameworkTest` (6), `Sprint5FrameworkTest` (5 — kiểm thử `SQLiteOpenHelper` CRUD/upgrade/rollback/listener + `MatrixCursor`/`CursorWrapper`, `ContentResolver`/`UriMatcher`/`FileProvider` trên `editor_198`, `HandlerThread`/`Looper`/`AsyncTask`, `Service`/`IntentService`/`Binder`/`BroadcastReceiver`, `ConnectivityManager` và **khởi tạo/insert/query thật bảng `categories` trên AndroidX `Room` `NoteDatabase` (`allthenotes`) từ APK `org.secuso.privacyfriendlynotes_105.apk`**).
- **Độ phủ API tĩnh (`.\aaow.bat --scan`) sau Sprint 5 (theo Call-Sites):**
  - `org.billthefarmer.editor_198.apk`: **97.5%** (`842/864` call-sites; class `87.4%`, method `94.0%`).
  - `org.billthefarmer.notes_142.apk`: **96.2%** (`893/928` call-sites; class `88.2%`, method `91.7%`).
  - `org.woheller69.level_35.apk`: **88.5%** (`8,687/9,820` call-sites; class `69.3%`, method `67.3%`).
  - `org.secuso.privacyfriendlynotes_105.apk`: **88.3%** (`31,564/35,732` call-sites; class `48.1%`, method `50.8%`).
  - Tham chiếu baseline thương mại `com.apkpure.aegon.apk`: **92.6%** (`69,130/74,640` call-sites; vẫn chờ Sprint 6–7 cho WebView/MediaPlayer/GMS/Native `.so`).
- **Kiểm thử chạy APK & xuất ảnh thực tế (`build/sprint5-final/*.png`):**
  - **Chạy thật trọn vẹn (0 lỗi, 0 stub warning):** `aaow-showcase.apk`, `aaow-2048.apk`, `org.billthefarmer.editor_198.apk`, `org.woheller69.level_35.apk`, `org.secuso.privacyfriendlynotes_105.apk` (`SplashActivity -> TutorialActivity`, `MainActivity` kèm `MainFABFragment`, `ManageCategoriesActivity`), và `AAOW App Hub` (`hub.png`).
  - **Đánh giá trung thực phần chưa hoàn chỉnh / ngoài phạm vi Sprint 5:**
    - `org.secuso.privacyfriendlynotes_105.apk`: `MainActivity` mở sạch không lỗi và `Room` (`NoteDatabase`) đọc/ghi SQLite thật trên Windows, nhưng thanh `MaterialToolbar` vẫn giữ nền trắng (chưa tô màu từ `MaterialShapeDrawable`), và màn hình phụ `TextNoteActivity` dừng khi inflate `Material TextInputLayout` tại `EditText.getTextCursorDrawable()` (API 29 widget Material, ngoài phạm vi Sprint 5).
    - `org.billthefarmer.notes_142.apk`: giao diện ActionBar chạy sạch, vùng nội dung vẫn hiển thị khung `WebView: about:blank` chờ **Sprint 6**.
    - `Service` và `BroadcastReceiver` hoạt động chuẩn trong cùng tiến trình ứng dụng AAOW; chưa có lớp IPC liên tiến trình giữa nhiều APK tách biệt.

### Chuẩn bị cho Sprint tiếp theo (Sprint 6 — Đa phương tiện, WebView & Tích hợp Sâu Hệ điều hành Windows)
- Nâng cấp `android.webkit.WebView` từ khung giữ chỗ sang trình duyệt nhúng thật (JavaFX WebView hoặc JCEF) để hiển thị nội dung HTML/Web trong app (`billthefarmer.notes`, trang đăng nhập/tin tức).
- Cài đặt phát âm thanh thật (`MediaPlayer`, `SoundPool`, `AudioTrack`) qua hệ thống âm thanh Windows.
- Kết nối `NotificationManager` sang Windows System Tray / Toast notification, đồng bộ `ClipboardManager` hai chiều và mở hộp thoại chọn file của Windows cho `Intent.ACTION_GET_CONTENT` / `ACTION_OPEN_DOCUMENT`.

---

## Sprint 6: Đa phương tiện, WebView & Tích hợp Sâu Hệ điều hành Windows — ✅ Đã hoàn thành & Nghiệm thu

### Mục tiêu đã đề ra
Nâng cấp lớp tương thích AAOW từ chạy UI/lưu trữ cơ bản lên hỗ trợ trình duyệt web nhúng thật (`WebView`), âm thanh đa phương tiện thật qua Windows Audio, và tích hợp sâu các dịch vụ hệ điều hành Windows (`NotificationManager` → `SystemTray`, đồng bộ `ClipboardManager`, hộp thoại chọn/tạo file Windows và in ấn).

### Các hạng mục đã triển khai
1. **Tích hợp JavaFX 21 WebKit & `android.webkit.*` hoàn chỉnh**
   - Bổ sung thư viện `org.openjfx:javafx-{base,graphics,controls,media,web,swing}:21.0.5:win` vào `framework/build.gradle.kts` chạy trực tiếp trên classpath Java 21.
   - Triển khai `dev.aaow.rt.WebViewEngineBridge` quản lý `javafx.scene.web.WebView`/`WebEngine` trong offscreen `Stage` (có lớp dự phòng Swing `JEditorPane` nếu môi trường thiếu JavaFX), trích xuất thư mục `assets/` của APK ra `<cacheDir>/aaow-webview-assets/` để phục vụ `file:///android_asset/` và `<base href=...>`.
   - Hỗ trợ `addJavascriptInterface` (ràng buộc theo vòng đời `SCHEDULED`/`RUNNING`/`progressProperty`/`documentProperty`/`SUCCEEDED`), `evaluateJavascript`, `WebViewClient`, `WebChromeClient`, `WebBackForwardList`, `findAll`/`findNext`, `zoomIn`/`Out`/`By`, chuyển tiếp chuột/cuộn/phím và chụp snapshot sang Java2D (`awaitIdle`/`awaitAllIdle`).
   - Bổ sung & hoàn thiện hệ sinh thái `android.webkit.*`: `WebView` (kế thừa `AbsoluteLayout` khớp đúng chữ ký `android.jar`), `URLUtil` (giải quyết gốc rễ lỗi `MarkdownView.loadMarkdownUrl` bị rơi về `about:blank` trong `org.billthefarmer.notes`), `MimeTypeMap`, `WebSettings`, `WebViewClient`, `WebChromeClient`, `ValueCallback`, `WebResourceRequest`, `WebResourceResponse`, `WebResourceError`, `WebHistoryItem`, `WebBackForwardList`, `ConsoleMessage`, `JsResult`, `JsPromptResult`, `DownloadListener`, `JavascriptInterface`, `CookieManager`.
   - Cập nhật `ApkFile.listEntries`, `AssetManager` (`openFd`, `openNonAssetFd`, `list`), `ParcelFileDescriptor.getFile()`, và `Resources` (`getSystem`, `openRawResourceFd`, `getTextArray`, `obtainAttributes`).

2. **Âm thanh Windows thật (`android.media.*`)**
   - Tạo `WindowsAudioHelper`, `MediaPlayer`, `SoundPool`, `AudioTrack`, `AudioManager`, `AudioFormat`, `AudioFocusRequest`, `MediaDataSource`, `AudioRouting`, `VolumeAutomation`.
   - Phát âm thanh thật qua `javax.sound.sampled` (`Clip`, `SourceDataLine`, PCM WAV tổng hợp/file/URI/RawResource/AssetFd) kết hợp fallback qua JavaFX Media.

3. **Tích hợp Sâu Hệ điều hành Windows (`SystemTray`, `Clipboard`, `File Picker`, `Print`, `IME`)**
   - **Thông báo (`NotificationManager`):** Tạo `WindowsTrayBridge` đẩy thông báo thật ra `java.awt.SystemTray` của Windows kèm `NotificationChannel`, `NotificationChannelGroup`, `Notification`, `PendingIntent`, `Icon`, `RemoteViews`, `PersistableBundle`, `ResultReceiver`.
   - **Clipboard hai chiều:** Triển khai `android.text.ClipboardManager`, `android.content.ClipboardManager`, `ClipData`, `ClipDescription` đồng bộ trực tiếp với `Toolkit.getDefaultToolkit().getSystemClipboard()`, tích hợp vào `EditText` (`Ctrl+C`, `Ctrl+X`, `Ctrl+V`).
   - **Chọn / Lưu tệp Windows:** Tạo `WindowsFilePickerBridge`, `android.provider.DocumentsContract`, mở rộng `Intent` (`ACTION_CREATE_DOCUMENT`, `ACTION_OPEN_DOCUMENT_TREE`, `EXTRA_ALLOW_MULTIPLE`, `EXTRA_MIME_TYPES`, `ClipData`) và chặn `Activity.startActivityForResult` để bật hộp thoại chọn/tạo/thư mục của Windows (`ACTION_GET_CONTENT`, `ACTION_OPEN_DOCUMENT`, `ACTION_CREATE_DOCUMENT`, `ACTION_OPEN_DOCUMENT_TREE`), trả `Uri` kết quả về `HostWindow` → `onActivityResult`.
   - **In ấn & UI/Layout phụ trợ:** Tạo gói `android.print.*` (`PrintManager`, `PrintDocumentAdapter`, `PrintAttributes`, `PrintDocumentInfo`, `PageRange`, `PrintJob`, `PrintJobInfo`, `PrintJobId`), `InputMethodManager`, `InputFilter`, `StaticLayout.Builder`, `TextDirectionHeuristic`/`Heuristics`, `Paint.FontMetricsInt`, `Canvas.concat(Matrix)`, `Html.toHtml`, `LayoutInflater.Filter` và các API `View`/`TextView`/`HorizontalScrollView`/`Toast` để mở khóa thêm các màn hình phức tạp (`TextNoteActivity`, `AudioNoteActivity`).

### Kết quả kiểm thử & nghiệm thu Sprint 6
1. **Kiểm thử tự động JUnit 5 (`.\gradlew.bat test --rerun-tasks --console=plain`):**
   - `AndroidSdkFallbackClassLoaderTest`: 2/2 PASS
   - `ApkCompatibilityScannerTest`: 2/2 PASS
   - `FrameworkHierarchyTest`: 2/2 PASS (**0 `SUPER_MISMATCH`** khi đối chiếu toàn bộ cây kế thừa lớp framework với `android.jar`)
   - `Sprint4FrameworkTest`: 6/6 PASS
   - `Sprint5FrameworkTest`: 5/5 PASS
   - `Sprint6FrameworkTest`: 4/4 PASS
   - **Tổng cộng:** `21 tests, 0 failures, 0 errors, 0 skipped`.
2. **Đóng gói chạy nhanh & Quét API tĩnh (`.\aaow.bat --rebuild`, `.\aaow.bat --scan`):**
   - `org.billthefarmer.notes_142.apk`: Class `104/110 (94.5%)`, Unique Method `323/327 (98.8%)`, Call Sites `924/928 (99.6%)`
   - `org.billthefarmer.editor_198.apk`: Class `89/95 (93.7%)`, Unique Method `263/267 (98.5%)`, Call Sites `858/864 (99.3%)`
   - `org.woheller69.level_35.apk`: Class `298/407 (73.2%)`, Unique Method `1658/2359 (70.3%)`, Call Sites `8820/9820 (89.8%)`
   - `org.secuso.privacyfriendlynotes_105.apk`: Class `406/751 (54.1%)`, Unique Method `2773/4863 (57.0%)`, Call Sites `32242/35732 (90.2%)`
   - `com.apkpure.aegon.apk`: Class `431/760 (56.7%)`, Unique Method `3040/4676 (65.0%)`, Call Sites `70652/74640 (94.7%)`
3. **Kiểm thử chạy thật & Chụp màn hình (`build/sprint6-final/*.png`):**
   - `org.billthefarmer.notes_142.apk` (`notes.png`): Mở thẳng giao diện `Notes.md`, nạp ghi chú mẫu UTF-8 tại `build/aaow-data/sdcard/Notes/Notes.md` và kết xuất trọn vẹn HTML/CSS/danh sách/code qua `MarkdownView` (JavaFX WebKit) với `file:///android_asset/styles.css` — không còn màn hình trắng `about:blank`.
   - `samples/aaow-showcase.apk` (`showcase.png`), `samples/aaow-2048.apk` (`game2048.png`), `org.billthefarmer.editor_198.apk` (`editor.png`), `org.woheller69.level_35.apk` (`level.png`), và AAOW App Hub (`hub.png`): Tất cả dựng hình ổn định, không có hồi quy.
   - `org.secuso.privacyfriendlynotes_105.apk`: Cả `MainActivity` (`pfnotes-main.png`), `TextNoteActivity` (`pfnotes-text.png`) và `AudioNoteActivity` (`pfnotes-audio.png`) đều mở và hiển thị sạch sẽ (spinner `Default`, ô nhập tiêu đề, nút FAB, `SeekBar` âm thanh).

### Đánh giá trung thực (Chạy thật vs Giới hạn hiện tại)
- **Chạy thật:**
  - `WebView` dựng bằng động cơ trình duyệt WebKit thật (JavaFX 21 WebView), giải mã asset nội bộ APK (`file:///android_asset/`), chạy JS/CSS và tương tác hai chiều với mã Java trong APK (`addJavascriptInterface`, `evaluateJavascript`).
  - Âm thanh (`MediaPlayer`, `SoundPool`, `AudioTrack`) phát qua thiết bị âm thanh Windows thật bằng `javax.sound.sampled` (kèm JavaFX Media fallback); thông báo `NotificationManager` đẩy thật qua `SystemTray` của Windows; `ClipboardManager` đồng bộ hai chiều với clipboard Windows; `startActivityForResult` mở hộp thoại chọn/lưu file Windows.
- **Giới hạn hiện tại & Ngoài phạm vi Sprint 6:**
  - Chạy `org.billthefarmer.notes` cần có sẵn tệp ghi chú (đã đặt sẵn tệp mẫu kiểm thử `build/aaow-data/sdcard/Notes/Notes.md`) nếu muốn thấy nội dung ngay khi vừa mở app mới cài.
  - Ở `org.secuso.privacyfriendlynotes_105.apk`, thanh `MaterialToolbar` vẫn giữ nền trắng (hạn chế theme AppCompat/Material đã ghi nhận từ Sprint 5) và `TextInputLayout` phát ra 1 cảnh báo `LayoutInflater` fallback (`The current box background mode 0 is not supported by the end icon mode 3`) trước khi hiển thị an toàn.
  - Định dạng âm thanh nén đặc thù ngoài khả năng giải mã mặc định của Java Sound / JavaFX Media (ví dụ một số codec AAC/OGG hiếm) phụ thuộc codec hỗ trợ trên JVM/Windows; hộp thoại File Picker & SystemTray ở chế độ `--screenshot` headless sử dụng đường dẫn an toàn không chặn luồng UI.
  - Chưa hỗ trợ nạp thư viện native C/C++ (`.so` ARM/x86_64) và chưa có lớp thay thế Google Play Services / Firebase (đây là mục tiêu của Sprint 7).

### Chuẩn bị cho Sprint tiếp theo (Sprint 7 — Tầng Thư viện Native (`.so` / JNI) & Dịch vụ thay thế GMS)
- Nghiên cứu và xây dựng cơ chế nạp/ánh xạ thư viện native `.so` (`System.loadLibrary`, cầu nối JNI hoặc giả lập/chuyển dịch nhẹ cho các hàm native phổ biến).
- Xây dựng lớp tương thích nhẹ cho các API Google Play Services / Firebase cơ bản (theo hướng microG) để các ứng dụng thương mại (`com.apkpure.aegon` v.v.) không bị chặn ngay từ bước khởi tạo dịch vụ.


---

## Sprint 7: Tầng Thư viện Native (`.so` / JNI) & Dịch vụ thay thế GMS (`microG-lite`)
**Trạng thái:** ✅ Đã hoàn thành & Nghiệm thu (18/11/2026)

### 1. Mục tiêu đã hoàn thành
1. **Nghiên cứu kiến trúc Native ABI ([NATIVE_ABI_SPIKE.md](NATIVE_ABI_SPIKE.md)):**
   - Đo đạc trực tiếp APK thương mại `com.apkpure.aegon.apk` (5 thư viện ARM `.so`: `libglide-webp.so`, `libqmp.so`, `libqsealib.so`, `libdeflater7z.so`, `libdaemon_acc_v2.1.5.so`; 95 hàm `native` trên 12 class, trong đó `MMKV` chiếm 47 hàm).
   - Chốt kiến trúc **Hybrid JNI HLE + ELF Parser + Pure-Java ARM64 Interpreter + Windows DLL Loader** (0 thư viện C ngoài, thuần Java 21).
2. **Tầng phân tích ELF, giải nén & cầu nối Native (`.so` / `.dll` / JNI):**
   - Bổ sung [ElfFile.java](../apk/src/main/java/dev/aaow/apk/ElfFile.java) và mở rộng [ApkFile.java](../apk/src/main/java/dev/aaow/apk/ApkFile.java) giải mã header ELF32/ELF64, phân đoạn `PT_LOAD` (`VirtualImage`), phụ thuộc `DT_NEEDED`, bảng ký hiệu `SHT_DYNSYM`/`SHT_SYMTAB` (`JNI_OnLoad`, `Java_*`).
   - Xây dựng [NativeLibraryManager.java](../framework/src/main/java/dev/aaow/rt/NativeLibraryManager.java) giải nén `.so` từ APK vào `applicationInfo.nativeLibraryDir`, hỗ trợ nạp `.dll` Windows thật qua `System.load` khi có, và lập chỉ mục hàm JNI `arm64-v8a`.
   - Nâng cấp [DexLoader.java](../launcher/src/main/java/dev/aaow/launcher/DexLoader.java) (`-v7.jar`) viết lại mọi phương thức `ACC_NATIVE` gọi qua [NativeBridge.invokeNative](../framework/src/main/java/dev/aaow/rt/NativeBridge.java) có truyền `this` và tham số trước khi thoái lui an toàn về giá trị mặc định.
   - Xây dựng 5 bộ giả lập JNI HLE trong [dev/aaow/rt/jni](../framework/src/main/java/dev/aaow/rt/jni):
     - `MmkvJniHandler`: lưu trữ key-value bền vững ra file `.aaow.mmkv` (bool, int, long, float, double, String, byte[], String[], `allKeys`, `sync`, NativeBuffer).
     - `WebpJniHandler` & `WebpHelper` (tích hợp vào `BitmapFactory`): đọc kích thước thực từ header RIFF WebP (`VP8` / `VP8L` / `VP8X`) và giải mã ảnh tiêu chuẩn.
     - `ZlibAndPatchJniHandler`: hỗ trợ `DeflateTask.deflateByCallback`, `CRC32.updateBytes`.
     - `MemFileJniHandler`: giả lập handle bộ nhớ dùng chung cho `MemFileJniApi`.
     - `SqliteJniHandler`: cầu nối JNI SQLite bổ trợ.
   - Xây dựng [Arm64JniInterpreter.java](../framework/src/main/java/dev/aaow/rt/Arm64JniInterpreter.java) (trình thông dịch CPU AArch64 thuần Java ở mức người dùng, giới hạn 200.000 bước, hỗ trợ bảng ảo `JNIEnv*` như `NewStringUTF`, `GetStringUTFChars`, mảng nguyên thủy).
3. **Lớp tương thích Google Play Services & Firebase (`microG-lite`) & Mở khóa khởi động APK thương mại:**
   - Tổ chức source set thứ cấp `framework/src/microg/java/` đóng gói thành `dev/aaow/microg-lite.jar` vào resource của `:framework`.
   - Xây dựng [ApkDexClassLoader.java](../launcher/src/main/java/dev/aaow/launcher/ApkDexClassLoader.java) theo thứ tự **system parent-first -> APK DEX child-first -> `microg-lite.jar` fallback (cùng ClassLoader để tránh `LinkageError`)** -> tổng hợp stub cho plugin tùy chọn bằng ASM (`DynamicPluginStubber` có kiểm tra `StackWalker` để không nuốt `Class.forName` thăm dò).
   - Cài đặt đầy đủ gói `microg-lite`: `FirebaseApp`, `FirebaseOptions`, `FirebaseInitProvider`, `FirebaseAnalytics`, `FirebaseCrashlytics`, `FirebaseMessaging`, `FirebaseInstallations`, `GoogleApiAvailability`, `GoogleApiAvailabilityLight`, `GooglePlayServicesUtil`, `AdvertisingIdClient` (kết nối AIDL `Binder` qua [MicroGServiceRegistry.java](../framework/src/main/java/dev/aaow/rt/microg/MicroGServiceRegistry.java)), `AppSet`, `Tasks`/`TaskCompletionSource`, `ProviderInstaller`, `Conscrypt`, `DiskLruCache`, `commons-logging`.
   - Chuẩn hóa vòng đời khởi động `Application.attach` -> `installManifestContentProviders()` (sắp xếp theo `initOrder`) trong [AppRepository.java](../launcher/src/main/java/dev/aaow/launcher/AppRepository.java) & [AndroidRuntimeContext.java](../framework/src/main/java/dev/aaow/rt/AndroidRuntimeContext.java) (kèm làm sạch ký tự `:` trong tên `SharedPreferences` trên Windows).
   - Sửa `Looper.myLooper()` chỉ trả `MAIN_LOOPER` trên luồng EDT / sau `prepareMainLooper()` (`null` trên worker thread để vượt qua kiểm tra `Preconditions.checkNotMainThread`) và bổ sung loạt API Android cốt lõi: `AlarmManager`, `JobScheduler`/`JobInfo`/`JobWorkItem`, `ActivityThread`, `ActivityManager`, `PowerManager`, `UserManager`, `Settings` (`Secure`/`System`/`Global`), `StatFs`, `StrictMode`, `Base64`, `JsonReader`/`JsonWriter`, `Process.myProcessName()`, `PackageManager` (`getPackageArchiveInfo`, `getInstalledPackages`, `getReceiverInfo`, `getProviderInfo`, `queryBroadcastReceivers`, `getInstallerPackageName`, `isInstantApp`, gói GMS/Play ảo), `libcore.io.Memory`, cùng nâng cấp Diagnostic Card 2 trên `HostWindow`.

### 2. Kết quả Nghiệm thu Thực tế (Đã chạy sau chỉnh sửa cuối cùng)
- **Kiểm thử tự động (`.\gradlew.bat test --rerun-tasks --console=plain`):**
  - **26/26 tests PASS (0 failed, 0 errors, 0 skipped)** trong `:launcher:test`:
    - `AndroidSdkFallbackClassLoaderTest` (2), `ApkCompatibilityScannerTest` (2), `FrameworkHierarchyTest` (2 — giữ vững **0 `SUPER_MISMATCH`** trước `android.jar` API 37), `Sprint4FrameworkTest` (6), `Sprint5FrameworkTest` (5), `Sprint6FrameworkTest` (4), `Sprint7FrameworkTest` (5).
- **Quét tương thích tĩnh (`.\aaow.bat --scan`):**
  - `samples/aaow-showcase.apk`: **100.0%** class (38/38), **100.0%** method (74/74), **100.0%** call sites (154/154).
  - `samples/aaow-2048.apk`: **100.0%** class (22/22), **100.0%** method (44/44), **100.0%** call sites (84/84).
  - `org.billthefarmer.editor_198.apk`: **93.7%** class (89/95), **98.5%** method (263/267), **99.3%** call sites (858/864).
  - `org.billthefarmer.notes_142.apk`: **94.5%** class (104/110), **98.8%** method (323/327), **99.6%** call sites (924/928).
  - `org.woheller69.level_35.apk`: **74.4%** class (303/407), **71.0%** method (1674/2359), **90.0%** call sites (8842/9820).
  - `org.secuso.privacyfriendlynotes_105.apk`: **57.1%** class (429/751), **59.3%** method (2886/4865), **91.1%** call sites (32565/35734).
  - `com.jarsilio.android.scrambledeggsif_88.apk`: **65.1%** class (329/505), **64.3%** method (2079/3234), **90.1%** call sites (18105/20092).
  - `com.apkpure.aegon.apk`: **60.2%** class (458/761), **68.4%** method (3202/4679), **96.0%** call sites (71673/74651); chỉ còn 4 system class thiếu hoàn toàn (0.5%).
- **Kiểm thử chạy thật & chụp màn hình (`build/sprint7-final/*.png`):**
  - Toàn bộ ứng dụng mẫu (`showcase`, `2048`), các app F-Droid (`Editor`, `Notes` WebView, `Bubble` Level, `Privacy Friendly Notes` onboarding) và `AAOW App Hub` hiển thị sạch, không thoái lui giao diện.
  - `com.apkpure.aegon.apk` đã vượt qua khởi động `Application`, chạy HLE `MemFileJniApi.init`/`readString`, khởi tạo `FirebaseInitProvider` + `FA` (`App measurement initialized`), tổng hợp stub plugin `tmsdk.fg.module.cleanV2.CleanManager`, vượt qua các điểm nghẽn `AlarmManager` / `JobScheduler` / `Looper` / `PackageManager`, và chạy hết `SplashActivity.onCreate()` mà không còn crash Activity (hiển thị Diagnostic Card với trạng thái chờ callback native/mạng trước `setContentView`).
  - **Vá hậu nghiệm thu (22:20):** Bổ sung `View.hasTransientState()` / `setHasTransientState(boolean)` và `ViewParent`/`ViewGroup.childHasTransientStateChanged()` (khắc phục `NoSuchMethodError` trên `org.secuso.privacyfriendlynotes.ui.main.MainActivity` khi `RecyclerView` có dữ liệu `Room`); thêm `SQLiteDatabase.resolveDatabasePath()` định tuyến các tên DB tương đối về thư mục `databases/` của ứng dụng thay vì rò ra thư mục làm việc hiện tại.

### 3. Đánh giá Trung thực (Chạy thật vs. Giả lập / Giới hạn hiện tại)
- **Chạy thật trên JVM Windows:** giải mã ELF32/ELF64, nạp `.dll` Windows thật qua `System.load` nếu có, lưu/đọc bền vững `MMKV` (`.aaow.mmkv`), nén `zlib` (`DeflateTask`, `CRC32`), đọc kích thước RIFF WebP (`VP8`/`VP8L`/`VP8X`), bộ nhớ chia sẻ `MemFileJniApi`, thông dịch CPU ARM64 thuần Java cho hàm tính toán & `JNIEnv` cơ bản, luồng `FirebaseInitProvider` + AIDL `Binder` ảo cho `AdvertisingIdClient` / `AppSet`, `AlarmManager`, `JobScheduler`.
- **Giả lập / Giới hạn:** các thư viện `.so` Linux phức tạp gọi syscall Bionic/anti-debug/crypto (`libqmp.so`, `libqsealib.so`, full native WebP pixel pipeline của `libglide-webp.so`) hiện chưa có tầng dịch nhị phân đa luồng kiểu Unicorn/FEX nên tự thoái lui an toàn. Trên APKPure thương mại, một số module phụ trợ chạy nền vẫn gặp giới hạn (thiếu trường nội bộ như `mNativePtr`, thiếu `android.os.SystemProperties`, chứng chỉ X.509 giả lập chưa ở chuẩn DER cho verifier của GMS `FA`, và `SplashActivity` chưa tự mở UI chính trước callback mạng/native).

### 4. Chuẩn bị cho Sprint 8
- Đóng gói AAOW thành bộ cài Windows độc lập (`.exe`/`.msi`) qua `jpackage` không cần cài JDK sẵn.
- Tích hợp liên kết đuôi file `.apk` trên Windows (nhấp đúp mở thẳng bằng AAOW) và tối ưu tốc độ khung hình render / dung lượng RAM.

---

## Sprint 8: Tối ưu Hiệu năng, Đóng gói Bộ cài Windows (`.exe`) & Phát hành
**Trạng thái:** ✅ Đã hoàn thành & Nghiệm thu (18/11/2026)

### 1. Các hạng mục đã hoàn thành
1. **Tối ưu Bộ nhớ đệm, Đồ họa 60fps, Giảm RAM & Cửa sổ Đáp ứng (`:framework`):**
   - Xây dựng bộ nhớ đệm LRU giới hạn theo dung lượng byte [ResourceCache.java](../framework/src/main/java/dev/aaow/rt/ResourceCache.java) (mặc định tối đa 24 MB cho `Bitmap` đã giải mã, 512 cây XML nhị phân và 512 `Drawable.ConstantState`), tích hợp vào [Resources.java](../framework/src/main/java/android/content/res/Resources.java) kèm `trimMemory()` / `clearCaches()` / `getCacheStats()`.
   - Nâng cấp [Bitmap.java](../framework/src/main/java/android/graphics/Bitmap.java) (`getByteCount()`, `getAllocationByteCount()`, `isMutable()`, `recycle()` giải phóng bề mặt native Java2D + `isRecycled()`), [WebpHelper.java](../framework/src/main/java/dev/aaow/rt/jni/WebpHelper.java) (`inJustDecodeBounds` đọc header không nạp pixel vào RAM và hỗ trợ `inSampleSize` giảm mẫu ảnh), [Canvas.java](../framework/src/main/java/android/graphics/Canvas.java), [BitmapDrawable.java](../framework/src/main/java/android/graphics/drawable/BitmapDrawable.java), [ColorDrawable.java](../framework/src/main/java/android/graphics/drawable/ColorDrawable.java), [GradientDrawable.java](../framework/src/main/java/android/graphics/drawable/GradientDrawable.java) và [VectorDrawable.java](../framework/src/main/java/android/graphics/drawable/VectorDrawable.java) (chia sẻ `PathSpec` qua `ConstantState`, tái sử dụng `BasicStroke`/`Color`).
   - Tối ưu vòng vẽ 60fps: [View.java](../framework/src/main/java/android/view/View.java) áp dụng thực `alpha`, `rotation`, `scaleX`/`scaleY` quanh tâm pivot; [ViewGroup.java](../framework/src/main/java/android/view/ViewGroup.java) cắt bỏ (cull) các View con nằm ngoài vùng nhìn thấy khi cuộn; [ValueAnimator.java](../framework/src/main/java/android/animation/ValueAnimator.java) dùng `CopyOnWriteArrayList` tránh cấp phát bản sao mỗi khung hình.
   - Nâng cấp [HostWindow.java](../framework/src/main/java/dev/aaow/rt/HostWindow.java): bỏ qua đo/dàn trang (`measure`/`layout`) dư thừa khi cây View không bẩn (`layoutSkips`), đệm sẵn `Font`/`Color`/`BasicStroke`/icon, tiết lưu cập nhật Inspector (250 ms), bổ sung thống kê `FrameStats` (FPS, ms/khung hình, heap MB, cache hit%), thanh điều khiển desktop có nút **Xoay Dọc ↔ Ngang** (`Ctrl+R`), **Phóng to / Thu nhỏ / Tự do 1:1** (`75%`, `100%`, `125%`, `150%`, `Ctrl+±/0`), cập nhật [Configuration.java](../framework/src/main/java/android/content/res/Configuration.java) & [Display.java](../framework/src/main/java/android/view/Display.java) (`getRotation()`) và gọi `onConfigurationChanged()`.
2. **Tối ưu Tốc độ Dịch DEX & Khởi động Nóng (`:launcher`):**
   - Nâng cấp [DexLoader.java](../launcher/src/main/java/dev/aaow/launcher/DexLoader.java) (`-v8.jar`) dịch trực tiếp từ DEX sang luồng bộ nhớ qua `Dex2jar.doTranslate(ByteArrayOutputStream)` (loại bỏ tệp `.raw.jar` trung gian trên đĩa), biến đổi bytecode ASM song song đa nhân (`.parallelStream()`) đồng thời thu thập tham chiếu ngoại lai trong 1 lượt duyệt duy nhất, nén nhanh `Deflater.BEST_SPEED` và xuất chỉ mục nhị phân `.refs`.
   - Nâng cấp [ApkDexClassLoader.java](../launcher/src/main/java/dev/aaow/launcher/ApkDexClassLoader.java) nạp tức thì tệp chỉ mục `.refs` khi khởi động nóng và dùng chung cache `microg-lite.jar` giữa các cửa sổ ứng dụng; nâng cấp [AppRepository.java](../launcher/src/main/java/dev/aaow/launcher/AppRepository.java) lưu đệm icon 64×64 xuống `dex-cache/icons/`.
3. **Liên kết đuôi `.apk`, Chế độ Cài đặt Độc lập & Đóng gói Bộ cài Windows (`jpackage` + `jlink`):**
   - Tạo [IconAssetGenerator.java](../launcher/src/main/java/dev/aaow/launcher/IconAssetGenerator.java) sinh bộ icon Windows đa độ phân giải (`16/32/48/64/256px`: `aaow.ico`, `apk-file.ico`) và [WindowsIntegration.java](../launcher/src/main/java/dev/aaow/launcher/WindowsIntegration.java) phân giải thư mục dữ liệu chuẩn (`%LOCALAPPDATA%\AAOW\data` khi chạy bản cài đặt độc lập, kết hợp [Environment.java](../framework/src/main/java/android/os/Environment.java) `setDataRoot`), đồng thời đăng ký/gỡ liên kết đuôi `.apk` qua `reg.exe import` dưới `HKCU\Software\Classes` (không cần quyền Admin).
   - Cập nhật [Main.java](../launcher/src/main/java/dev/aaow/launcher/Main.java) và [AppHubWindow.java](../launcher/src/main/java/dev/aaow/launcher/AppHubWindow.java) thêm nút **"Liên kết .apk"**, lệnh `--install` / `--install-silent`, `--associate-apk` / `--unassociate-apk`, `--data-dir`.
   - Xây dựng quy trình đóng gói [scripts/package-windows.ps1](../scripts/package-windows.ps1), bộ cài & gỡ cài đặt native Windows [scripts/AaowSetup.cs](../scripts/AaowSetup.cs) và task Gradle `:launcher:packageWindows` trong [launcher/build.gradle.kts](../launcher/build.gradle.kts).

### 2. Kết quả Nghiệm thu Thực tế (Đã chạy sau chỉnh sửa cuối cùng)
- **Kiểm thử tự động JUnit 5 (`.\gradlew.bat test --rerun-tasks --console=plain`):**
  - **31/31 tests PASS (0 failed, 0 errors, 0 skipped)** trong `:launcher:test`:
    - `AndroidSdkFallbackClassLoaderTest` (2), `ApkCompatibilityScannerTest` (2), `FrameworkHierarchyTest` (2 — **0 `SUPER_MISMATCH`** đối chiếu `android.jar`), `Sprint4FrameworkTest` (6), `Sprint5FrameworkTest` (5), `Sprint6FrameworkTest` (4), `Sprint7FrameworkTest` (5), `Sprint8FrameworkTest` (5).
- **Đóng gói Bộ cài Windows độc lập (`powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1`):**
  - JRE Java 21 rút gọn bởi `jlink` (`build/packaging/runtime`): **59,8 MB** (gồm đủ `java.desktop`, `java.sql`, `java.net.http`, `java.scripting`, `jdk.jsobject`, `jdk.unsupported`, `jdk.xml.dom`, `jdk.crypto.ec`…).
  - Bản thực thi `jpackage --type app-image` (`build/jpackage/AAOW`): **117,0 MB**, tạo ra **`AAOW.exe`** (GUI), **`aaow-cli.exe`** (Console), `runtime/`, `app/`, `samples/`, `apk-file.ico`.
  - Bản di động ZIP (`build/release/AAOW-Portable-1.0.0.zip`): **99,2 MB**.
  - Bộ cài Windows (`build/release/AAOW-Setup-1.0.0.exe`): **99,3 MB** (biên dịch qua `csc.exe`, tích hợp GUI chọn thư mục, liên kết `.apk`, shortcut Start Menu/Desktop, cài/gỡ im lặng `/S` & `/UNINSTALL` và đăng ký Windows Apps & Features).
- **Kiểm thử cài đặt & chạy độc lập không cần JDK (`JAVA_HOME=""`, `AAOW_HOME=""`):**
  - Cài thử `AAOW-Setup-1.0.0.exe /S /NOASSOC /NOSHORTCUT` vào thư mục tạm cô lập → chạy thành công cả `AAOW.exe` lẫn `aaow-cli.exe` để cài APK, mở App Hub (`build/sprint8-final/hub-standalone.png`), kết xuất JavaFX WebView (`notes-webview.png`), Room SQLite (`pfnotes-main.png`), `showcase.png`, `game2048.png`, `editor.png`, `level.png`, `apkpure.png` → gỡ sạch bằng `Uninstall-AAOW.exe /UNINSTALL /S` (xác nhận xóa hết tệp cài đặt và khóa Registry Uninstall).
  - Đo hiệu năng chạy thực tế từ tiến trình độc lập `aaow-cli.exe --screenshot`:
    - Dịch DEX lạnh: `aaow-2048.apk` (13 class) `202 ms`; `editor_198` (319 class) `606–651 ms`; `notes_142` (449 class) `716–799 ms`; `level_35` (1.988 class) `2.077 ms`.
    - Thời gian khởi động lạnh → nóng (trọn vòng đời JVM + nạp APK + render): `org.secuso.privacyfriendlynotes_105` giảm từ **15.269 ms → 1.999 ms** (~7,6× nhanh hơn); `com.apkpure.aegon.apk` giảm từ **60.637 ms → 7.712 ms** (~7,9× nhanh hơn).

### 3. Đánh giá Trung thực (Chạy thật vs. Giới hạn hiện tại)
- **Chạy thật:** toàn bộ lớp tối ưu bộ nhớ đệm `ResourceCache`, `Bitmap.recycle`/`inSampleSize`, culling `ViewGroup`, bỏ qua layout thừa `HostWindow`, xoay ngang/dọc + Zoom + Resize đáp ứng, dịch DEX trên bộ nhớ song song + `.refs`, liên kết đuôi `.apk` trên `HKCU\Software\Classes`, bộ thực thi `AAOW.exe` / `aaow-cli.exe` từ `jpackage` + `jlink`, và bộ cài/gỡ cài đặt `AAOW-Setup-1.0.0.exe` đều chạy thật trên Windows không cần cài JDK.
- **Giới hạn hiện tại:** máy đóng gói hiện tại không cài sẵn WiX Toolset (`wix.exe` / `candle.exe` + `light.exe`), nên `package-windows.ps1` bỏ qua bước `jpackage --type msi` và xuất bộ cài `.exe` qua `AAOW-Setup-1.0.0.exe` (logic gọi `jpackage --type msi` sẽ tự kích hoạt nếu máy có WiX). Ngoài ra các giới hạn tương thích tầng Framework/Native `.so` phức tạp của APK thương mại (đã ghi nhận từ Sprint 7) giữ nguyên ngoài phạm vi Sprint 8.

---

## Sprint 9 (Phase A): Phá rào `VerifyError` Bytecode & Lấp lỗ hổng Core Runtime
**Trạng thái:** ✅ Đã hoàn thành & Nghiệm thu (19/11/2026)

### 1. Các hạng mục đã hoàn thành
1. **Xử lý tận gốc `VerifyError` do R8 Constructor Inlining & Dead Constructor Removal (`:launcher`):**
   - Chẩn đoán sâu bytecode của `com.jarsilio.android.scrambledeggsif_88.apk` (targetSdk 37, Kotlin + R8 + AndroidX) bằng `javap`: phát hiện nguyên nhân gốc rễ là R8 xóa constructor `<init>` trên các lớp con như `androidx.activity.contextaware.ContextAwareHelper`, `androidx.startup.StartupException` rồi gọi thẳng `invoke-direct Super.<init>()` trong DEX — khiến `dex2jar` dịch sai thành `new java/lang/Object` (`VerifyError: Bad type on operand stack in putfield`) hoặc `invokespecial Super.<init>` trên `uninitialized(Sub)` (`VerifyError: Expecting a stackmap frame`).
   - Xây dựng [DexR8Sanitizer.java](../launcher/src/main/java/dev/aaow/launcher/DexR8Sanitizer.java) + [R8CtorMarker.java](../framework/src/main/java/dev/aaow/rt/R8CtorMarker.java) chạy trực tiếp trên AST DEX (`DexFileNode`) **trước** `Dex2jar`: dùng phân tích dataflow theo dõi mọi đối tượng `uninitialized` (từ `NEW_INSTANCE` lẫn `this` trong `<init>`), tự động tổng hợp chuỗi bridge constructor hợp lệ trên từng lớp trung gian (dùng `R8CtorMarker` nếu trùng chữ ký constructor sẵn có) và viết lại lệnh `invoke-direct` về đúng lớp đích.
   - Tích hợp vào [DexLoader.java](../launcher/src/main/java/dev/aaow/launcher/DexLoader.java) (`-v9.jar`), đồng thời tối ưu bộ nhớ đỉnh bằng cách giải phóng từng `DexClassNode` ngay trong quá trình `dex2jar` tiêu thụ và giải phóng sớm buffer trung gian.
   - Trên `com.jarsilio.android.scrambledeggsif_88.apk`, `DexR8Sanitizer` tự động sửa **1.281 điểm gọi constructor R8** và tổng hợp **645 bridge constructor**, xóa sạch 100% lỗi `VerifyError`.
2. **Đưa Khởi tạo `Application` & `ContentProvider` lên đúng Main Thread (Swing EDT):**
   - Cập nhật [AppRepository.java](../launcher/src/main/java/dev/aaow/launcher/AppRepository.java) chạy `app.attach`, `installManifestContentProviders()` và `app.onCreate()` trên Swing EDT (`Looper.getMainLooper().getThread()`), khắc phục triệt để `IllegalStateException: Method handleLifecycleEvent must be called on the main thread` trong `androidx.startup.InitializationProvider` / `ProcessLifecycleInitializer`.
3. **Cài đặt thật các lớp Core Runtime & API 33 hiện đại (`:framework`):**
   - [LruCache.java](../framework/src/main/java/android/util/LruCache.java) (`LinkedHashMap` access-order, `sizeOf`, `create`, `entryRemoved`, `trimToSize`, `evictAll`, `resize`, `snapshot`).
   - [Choreographer.java](../framework/src/main/java/android/view/Choreographer.java) (nhịp 60fps thật qua `Handler`/Main Looper, `FrameCallback`, `VsyncCallback`, `FrameData`, `FrameTimeline`).
   - [Base64InputStream.java](../framework/src/main/java/android/util/Base64InputStream.java), [Base64OutputStream.java](../framework/src/main/java/android/util/Base64OutputStream.java) (hỗ trợ đầy đủ cờ `NO_CLOSE`, `URL_SAFE`, `NO_WRAP`), [LogPrinter.java](../framework/src/main/java/android/util/LogPrinter.java).
   - [SystemProperties.java](../framework/src/main/java/android/os/SystemProperties.java) (API ẩn của AOSP cho AndroidX/Firebase/SDK), [Debug.java](../framework/src/main/java/android/os/Debug.java) (kèm `Debug.MemoryInfo` implements `Parcelable`).
   - Bổ sung overload API 33 type-safe trên [Bundle.java](../framework/src/main/java/android/os/Bundle.java), [Intent.java](../framework/src/main/java/android/content/Intent.java), [Parcel.java](../framework/src/main/java/android/os/Parcel.java) (`getParcelable(String, Class)`, `getSerializable(String, Class)`...) và 4 lớp `*Flags` API 33 trên [PackageManager.java](../framework/src/main/java/android/content/pm/PackageManager.java).
   - Cài đặt thật [TableLayout.java](../framework/src/main/java/android/widget/TableLayout.java), [TableRow.java](../framework/src/main/java/android/widget/TableRow.java), hoàn thiện vòng đời [Dialog.java](../framework/src/main/java/android/app/Dialog.java) (`Dialog(Context, int)`, `Window.Callback`, `OnShowListener`/`OnKeyListener`, `getOnBackInvokedDispatcher()`) và chuẩn hóa bất đồng bộ `Activity.requestPermissions()` + `shouldShowRequestPermissionRationale()` trong [Activity.java](../framework/src/main/java/android/app/Activity.java).

### 2. Kết quả Nghiệm thu Thực tế (Đã chạy sau chỉnh sửa cuối cùng)
- **Kiểm thử tự động JUnit 5 (`.\gradlew.bat test --rerun-tasks --console=plain`):**
  - **36/36 tests PASS (0 failures, 0 errors, 0 skipped)** trong `:launcher:test`:
    - `AndroidSdkFallbackClassLoaderTest` (2), `ApkCompatibilityScannerTest` (2), `FrameworkHierarchyTest` (2 — giữ vững **0 `SUPER_MISMATCH`** đối chiếu `android.jar` API 37 cho toàn bộ lớp mới), `Sprint4FrameworkTest` (6), `Sprint5FrameworkTest` (5), `Sprint6FrameworkTest` (4), `Sprint7FrameworkTest` (5), `Sprint8FrameworkTest` (5), `Sprint9FrameworkTest` (5).
- **Quét tương thích tĩnh (`.\aaow.bat --scan`) sau Sprint 9:**
  - `com.jarsilio.android.scrambledeggsif_88.apk`: **66,5%** class (335/504), **65,3%** method (2.113/3.235), **90,4%** call sites (18.150/20.067).
  - `org.secuso.privacyfriendlynotes_105.apk`: **58,0%** class (435/750), **60,3%** method (2.937/4.870), **91,4%** call sites (32.648/35.719).
  - `org.woheller69.level_35.apk`: **75,1%** class (305/406), **71,7%** method (1.693/2.361), **90,3%** call sites (8.864/9.816).
  - `com.apkpure.aegon.apk`: **61,2%** class (465/760), **69,4%** method (3.250/4.683), **96,1%** call sites (71.763/74.675).
- **Kiểm thử chạy thật & xuất ảnh (`build/sprint9-final/*.png`):**
  - **`com.jarsilio.android.scrambledeggsif_88.apk` (Mở khóa thành công lần đầu!):** Vượt qua hoàn toàn `App` → `InitializationProvider` → `MainActivity.onCreate()` → `onStart()` → `onResume()` với **0 lỗi, 0 exception, 0 stub warning**, hiển thị trọn vẹn logo vector quả trứng và hướng dẫn *"Instructions"* trên cửa sổ Windows!
  - Toàn bộ 6 APK khác (`aaow-showcase`, `aaow-2048`, `Editor`, `Notes` WebView, `Bubble` Level, `Privacy Friendly Notes MainActivity`) tiếp tục chạy ổn định, không hồi quy.

### 3. Đánh giá Trung thực (Chạy thật vs. Hạn chế dành cho Sprint 10)
- **Chạy thật:** `DexR8Sanitizer` xử lý triệt để R8 Constructor Inlining (diệt `VerifyError` trên `Scrambled Exif` và các APK AndroidX/Kotlin R8 hiện đại); `LruCache`, `Choreographer`, `Base64*Stream`, `LogPrinter`, `SystemProperties`, `Debug`, `TableLayout`/`TableRow`, API 33 `Bundle`/`Intent`/`PackageManager`, `Dialog` lifecycle và async `requestPermissions` đều chạy thật trên JVM Windows.
- **Hạn chế chuyển sang Sprint 10 (Phase B):**
  - `androidx.appcompat.app.AlertDialog` / `MaterialAlertDialogBuilder` (kế thừa trực tiếp `android.app.Dialog`, không qua `android.app.AlertDialog`) đã chạy sạch không crash nhưng `HostWindow` mới gắn cầu nối vẽ overlay cho `android.app.AlertDialog` — cần nối thêm vẽ lớp phủ `Dialog` chung trên `HostWindow` ở Sprint 10 để hiển thị trực tiếp các hộp thoại AndroidX/Material.
  - Luồng tương tác đầu-cuối (bấm nút tạo/lưu/xem lại ghi chú, vuốt `ViewPager`, mở `DrawerLayout`, xóa EXIF ảnh thật qua `ExifInterface`, và tô màu nền `MaterialToolbar`) sẽ được hoàn thiện và kiểm thử tương tác thực tế trong **Sprint 10 (Phase B)**.

### 4. Chuẩn bị cho Sprint 10 (Phase B — Chạy & Tương tác Trọn vẹn Offline Apps) — Bắt đầu ở chat mới
Khi mở phiên hội thoại mới cho **Sprint 10**, triển khai theo thứ tự sau trên 5 APK Offline tại `build/benchmark-apks/`:
1. **Bước 10.1 — Vẽ lớp phủ `android.app.Dialog` & Thanh tiêu đề Material/AppCompat trên `HostWindow`:**
   - Nối `Dialog.show()` / `dismiss()` (bao gồm `androidx.appcompat.app.AlertDialog`, `ComponentDialog`, `MaterialAlertDialogBuilder`) vào `HostWindow` để hiển thị và nhận click trực tiếp trên nội dung `Window` của Dialog.
   - Xử lý màu nền/chữ trên `androidx.appcompat.widget.Toolbar` / `MaterialToolbar` để tiêu đề & nút điều hướng hiển thị rõ nét trên `Scrambled Exif` và `Privacy Friendly Notes`.
2. **Bước 10.2 — Hoàn thiện tương tác đầu-cuối cho 5 APK Offline thật:**
   - `org.billthefarmer.editor_198.apk`: tạo file, gõ văn bản, mở Options Menu, lưu & mở lại file trong `sdcard/`.
   - `org.billthefarmer.notes_142.apk`: chuyển chế độ sửa/xem, lưu ghi chú Markdown và kết xuất lại trên JavaFX `WebView`.
   - `org.woheller69.level_35.apk`: đóng dialog `FreeDroidWarn`, tương tác giao diện chính thước thủy và màn hình Settings.
   - `com.jarsilio.android.scrambledeggsif_88.apk`: đóng dialog Material nếu có, cài đặt `android.media.ExifInterface` + `Bitmap.compress()` để xóa metadata EXIF ảnh JPEG/PNG qua Windows File Picker / `ACTION_SEND`.
   - `org.secuso.privacyfriendlynotes_105.apk`: mở từ `SplashActivity` → bấm Skip/Next trên `TutorialActivity` (`CompatViewPager`) → `MainActivity` (`DrawerLayout`, `FAB +`, `CompatRecyclerView`) → mở `TextNoteActivity` / `ChecklistNoteActivity`, nhập chữ, bấm Lưu vào Room SQLite → quay lại `MainActivity` hiển thị danh sách ghi chú và bấm mở lại được.
3. **Bước 10.3 — Đưa Offline Apps vào `AppHubWindow` & Viết `Sprint10FrameworkTest`:**
   - Viết bộ test tương tác tự động (mô phỏng sự kiện chuột/bàn phím trên `HostWindow` cho từng luồng chính của 5 APK Offline) và hỗ trợ cài/mở nhanh các APK kiểm thử trong `AppHubWindow`.
   - Nghiệm thu cuối Sprint 10: giữ 100% test JUnit 5 PASS (0 `SUPER_MISMATCH`), cập nhật `SPRINT_LOG.md`, `ROADMAP.md`, `GEMINI.md` và commit Git trước khi sang chat Sprint 11.

---

## Sprint 10 (Phase B): Chạy & Tương tác Trọn vẹn Các Ứng dụng Offline (Offline Apps)
**Trạng thái:** ✅ Đã hoàn thành & Nghiệm thu (19/11/2026)

### 1. Các hạng mục đã hoàn thành
1. **Lớp phủ `Dialog` Tổng quát (`android.app.Dialog`, `androidx.appcompat.app.AlertDialog`, `MaterialAlertDialog`) & Theming Material/AppCompat (`:framework`):**
   - Nâng cấp [HostWindow.java](../framework/src/main/java/dev/aaow/ui/HostWindow.java), [Dialog.java](../framework/src/main/java/android/app/Dialog.java), [AlertDialog.java](../framework/src/main/java/android/app/AlertDialog.java) và [Window.java](../framework/src/main/java/android/view/Window.java) để vẽ và định tuyến sự kiện chuột/bàn phím trực tiếp trên cây `View` thực của mọi `Dialog` (cả `android.app.AlertDialog` lẫn `androidx.appcompat.app.AlertDialog` / `MaterialAlertDialogBuilder`).
   - Hỗ trợ đo đạc `MeasureSpec.AT_MOST`, căn giữa màn hình với lớp nền mờ (scrim), đóng dialog khi bấm nút (`BUTTON1`/`BUTTON2`/`BUTTON3`) hoặc chạm ngoài vùng `cancelable`, và hoàn thiện bố cục `AlertDialog` hệ thống (`android.R.id.title_template`, `customPanel`) cho các hộp thoại tùy biến.
   - Bổ sung giải mã màu & thuộc tính giao diện Material/AppCompat trong [Resources.java](../framework/src/main/java/android/content/res/Resources.java), [TypedArray.java](../framework/src/main/java/android/content/res/TypedArray.java), [ColorStateList.java](../framework/src/main/java/android/content/res/ColorStateList.java), [XmlDrawableLoader.java](../framework/src/main/java/dev/aaow/ui/XmlDrawableLoader.java), [VectorDrawable.java](../framework/src/main/java/android/graphics/drawable/VectorDrawable.java) và sửa [Button.java](../framework/src/main/java/android/widget/Button.java) để các nút `MaterialButton` / borderless dialog button không bị ép chữ trắng trên nền sáng.
   - Bổ sung cắt vùng clip ngược `Canvas.clipOutRect` / `clipOutPath` bằng `java.awt.geom.Area.subtract` trong [Canvas.java](../framework/src/main/java/android/graphics/Canvas.java), giúp khung viền `TextInputLayout` (`CutoutDrawable` của Material Components) khoét nhãn nổi *"Name"* / *"Category"* chuẩn xác.
2. **Cài đặt Thật `android.media.ExifInterface`, Chuẩn hóa Đường dẫn Windows cho `FileProvider` & Cầu nối Chia sẻ (`:framework`, `:launcher`):**
   - Xây dựng mới hoàn toàn [ExifInterface.java](../framework/src/main/java/android/media/ExifInterface.java) bằng thuần Java: phân tích và ghi binary phân đoạn JPEG (`SOI`, `APP1` EXIF TIFF header Little/Big Endian, `IFD0`, `ExifIFD`, `GPSIFD`, `SOFn`) và PNG (`IHDR`, `eXIf`), hỗ trợ đọc/ghi/xóa tag (`TAG_MAKE`, `TAG_MODEL`, `TAG_ORIENTATION`, `TAG_GPS_*`, `TAG_IMAGE_WIDTH`, `TAG_IMAGE_LENGTH`...) và `saveAttributes()` bảo toàn dữ liệu ảnh.
   - Nâng cấp [Bitmap.java](../framework/src/main/java/android/graphics/Bitmap.java) (`Bitmap.compress(CompressFormat.JPEG/PNG/WEBP, quality, OutputStream)`) sử dụng `ImageIO` kết hợp `JPEGImageWriteParam` theo đúng mức `quality`.
   - Sửa lỗi tương thích đường dẫn Windows (`\` vs `/`) trong AndroidX `FileProvider` bằng cách tự động chuẩn hóa kết quả `File.getCanonicalPath()` và `File.getPath()` sang `/` trong [DexLoader.java](../launcher/src/main/java/dev/aaow/launcher/DexLoader.java) (`-v10.jar`), đồng thời sửa [Uri.java](../framework/src/main/java/android/net/Uri.java) (`Uri.Builder.build()`) luôn thêm dấu `/` ngăn cách giữa `authority` và `encodedPath`.
   - Tạo mới [WindowsShareBridge.java](../framework/src/main/java/dev/aaow/ui/WindowsShareBridge.java) và tích hợp vào [Activity.java](../framework/src/main/java/android/app/Activity.java) để bắt `Intent.ACTION_CHOOSER` / `Intent.ACTION_SEND`, tự động sao lưu file chia sẻ (`content://...` hoặc `file://...`) ra thư mục `shared-out/` trên Windows và hỗ trợ chế độ headless cho kiểm thử tự động.
3. **Tương tác Đầu-Cuối (E2E) Trọn vẹn trên 5 APK Offline Thực tế (`build/benchmark-apks/`):**
   - **`org.billthefarmer.editor_198.apk` (Text Editor):** Hỗ trợ `ScrollView`/`HorizontalScrollView` `fillViewport`, nhập văn bản nhiều dòng vào `EditText`, mở Options Menu → bấm `Save` ghi file thật xuống `sdcard/Documents/Editor.txt`, mở dialog nội bộ `openDialog` và nạp lại file văn bản qua `Intent.ACTION_VIEW`.
   - **`org.billthefarmer.notes_142.apk` (Markdown Notes):** Hiển thị bản xem trước Markdown trên JavaFX `WebView`, bấm FAB `@id/edit` chuyển sang chế độ soạn thảo `EditText`, nhập nội dung Markdown mới, bấm FAB `@id/accept` kết xuất lại HTML trực tiếp trên `WebView` và lưu xuống `sdcard/Notes/Notes.md`.
   - **`org.woheller69.level_35.apk` (Bubble Level):** Hiển thị và bấm đóng dialog 3 nút `FreeDroidWarn`, chạy vòng lặp `SurfaceView` 60fps của thước thủy `LevelView` phản hồi trực tiếp với trọng lực ảo `SensorManager.setVirtualGravity(gx, gy, gz)` + phông chữ 7 đoạn LCD (`Typeface.createFromAsset`), và mở màn hình `SettingsActivity` (`PreferenceFragmentCompat` + `RecyclerView` + `SwitchCompat` + `SeekBar`).
   - **`com.jarsilio.android.scrambledeggsif_88.apk` (Scrambled Exif):** Hiển thị và bấm đóng `MaterialAlertDialog` (`androidx.appcompat.app.AlertDialog`), gửi ảnh JPEG có gắn thẻ EXIF (`Make`, `Model`, `GPSLatitude`) vào `HandleImageActivity` qua `Intent.ACTION_SEND`, xóa sạch metadata EXIF bằng `ExifInterface`, xuất URI chia sẻ `content://com.jarsilio.android.scrambledeggsif.fileprovider/shared_images/...jpg` qua `FileProvider` và trích xuất ra file JPEG sạch trên Windows qua `WindowsShareBridge`.
   - **`org.secuso.privacyfriendlynotes_105.apk` (Privacy Friendly Notes):** Chạy trọn luồng `SplashActivity` → `TutorialActivity` (bấm điều hướng `ViewPager` qua các trang hướng dẫn & `Skip`) → `MainActivity` → mở & đóng menu ngăn kéo `DrawerLayout` (`NavigationView` có icon vector) → bấm FAB `+` mở bảng chọn loại ghi chú → bấm `New text` mở `TextNoteActivity` (hiển thị ô nhập Material Outlined `TextInputLayout` có viền khoét nhãn *"Name"* & *"Category"*) → nhập tiêu đề & nội dung → bấm Back lưu vào cơ sở dữ liệu **Room SQLite** (`[Toast] Note saved`) → `MainActivity` hiển thị thẻ `CardView` của ghi chú trong `RecyclerView` → bấm vào thẻ ghi chú trong `RecyclerView` mở lại `TextNoteActivity` nạp đúng dữ liệu từ Room SQLite!
4. **Hoàn thiện Các Lớp Tương thích `Compat*`, Widget Cốt lõi & Tích hợp `AppHubWindow` (`:framework`, `:launcher`):**
   - Nâng cấp [CompatViewPager.java](../framework/src/main/java/dev/aaow/compat/CompatViewPager.java), [CompatRecyclerView.java](../framework/src/main/java/dev/aaow/compat/CompatRecyclerView.java) (tự động đồng bộ adapter khi dữ liệu thay đổi trong `onMeasure`), và thêm mới [CompatDrawerLayout.java](../framework/src/main/java/dev/aaow/compat/CompatDrawerLayout.java), [CompatTextInputLayout.java](../framework/src/main/java/dev/aaow/compat/CompatTextInputLayout.java), [LayoutTransition.java](../framework/src/main/java/android/animation/LayoutTransition.java), [FloatProperty.java](../framework/src/main/java/android/util/FloatProperty.java).
   - Bổ sung hàng loạt phương thức AndroidX/Material cần thiết trên `View`, `ViewGroup`, `ViewParent`, `TextView`, `CompoundButton`, `ImageView`, `AutoCompleteTextView`, `Drawable`, `Path`, `Bundle`, `Intent`, và cập nhật `SQLiteDatabase.releaseMemory()` giải phóng kết nối JDBC để không giữ khóa file `.db-shm`/`.db-wal` trên Windows.
   - Bổ sung `AppRepository.installApksFromDirectory(Path)` в [AppRepository.java](../launcher/src/main/java/dev/aaow/launcher/AppRepository.java), tham số CLI `--install-silent <file|dir>` trong [Main.java](../launcher/src/main/java/dev/aaow/launcher/Main.java), và nút **"Nạp APK Offline"** trên thanh công cụ của [AppHubWindow.java](../launcher/src/main/java/dev/aaow/launcher/AppHubWindow.java) để nạp nhanh toàn bộ 5 APK Offline từ `build/benchmark-apks/` vào App Hub chỉ với 1 cú nhấp chuột.

### 2. Kết quả Nghiệm thu Thực tế (Đã chạy sau chỉnh sửa cuối cùng)
- **Kiểm thử tự động JUnit 5 (`.\gradlew.bat test --rerun-tasks --console=plain`):**
  - **41/41 tests PASS (0 failures, 0 errors, 0 skipped)** trong `1m 07s`:
    - `AndroidSdkFallbackClassLoaderTest` (2)
    - `ApkCompatibilityScannerTest` (2)
    - `FrameworkHierarchyTest` (2 — giữ vững **0 `SUPER_MISMATCH`** đối chiếu `android.jar` API 37 cho toàn bộ 281 lớp `android.*`, bao gồm `ExifInterface`, `LayoutTransition`, `FloatProperty`)
    - `Sprint4FrameworkTest` (6)
    - `Sprint5FrameworkTest` (5)
    - `Sprint6FrameworkTest` (4)
    - `Sprint7FrameworkTest` (5)
    - `Sprint8FrameworkTest` (5)
    - `Sprint9FrameworkTest` (5)
    - `Sprint10FrameworkTest` (5 — **MỚI** tại [Sprint10FrameworkTest.java](../launcher/src/test/java/dev/aaow/launcher/Sprint10FrameworkTest.java), kiểm thử tự động đầu-cuối `ExifInterface`, lớp phủ `Dialog`, cảm biến `Level` + `SettingsActivity`, luồng xóa EXIF ảnh thật qua `Scrambled Exif` `HandleImageActivity`, và luồng tạo/lưu/mở lại ghi chú Room SQLite trên `Privacy Friendly Notes` + nạp batch 5 APK Offline vào `AppRepository`).
- **Bộ 19 ảnh chụp màn hình nghiệm thu thực tế (`build/sprint10-final/*.png`):**
  - `0-app-hub-offline.png`: Giao diện **AAOW App Hub** hiển thị đầy đủ 7 ứng dụng đã cài (2 APK mẫu + 5 APK Offline thật) kèm nút **"Nạp APK Offline"**.
  - `1-editor-typed.png`: **Editor** nhập văn bản nhiều dòng và lưu xuống `sdcard/Documents/Editor.txt`.
  - `2a-notes-view.png`, `2b-notes-editing.png`, `2c-notes-updated.png`: **Notes** chuyển từ xem Markdown trên JavaFX `WebView` → sửa trong `EditText` → bấm lưu và kết xuất lại HTML mới trên `WebView`.
  - `3a-level-dialog.png`, `3b-level-main.png`, `3c-level-settings.png`: **Bubble Level** hiển thị dialog `FreeDroidWarn`, đóng dialog vào thước thủy xanh lá với chữ số LCD 7 đoạn nghiêng theo trọng lực ảo, và mở màn hình `SettingsActivity`.
  - `4a-scrambled-dialog.png`, `4b-scrambled-main.png`: **Scrambled Exif** hiển thị `MaterialAlertDialog` với đủ 3 nút bấm rõ nét và đóng dialog vào màn hình chính.
  - `5a-pfnotes-tutorial-0.png` → `5i-pfnotes-reopened.png` (9 ảnh): **Privacy Friendly Notes** đi qua trọn vẹn các trang `TutorialActivity` (`ViewPager`), `MainActivity`, menu ngăn kéo `DrawerLayout`, menu `FAB +`, màn hình `TextNoteActivity` với khung viền Material `TextInputLayout`, nhập và lưu ghi chú vào Room SQLite, hiển thị thẻ ghi chú trên `RecyclerView` của `MainActivity`, và bấm mở lại thành công ghi chú từ Room SQLite!

### 3. Đánh giá Trung thực (Chạy thật vs. Hạn chế dành cho Sprint 11+)
- **Chạy thật 100%:** Toàn bộ 5 APK Offline thực tế (`Editor`, `Notes`, `Bubble Level`, `Scrambled Exif`, `Privacy Friendly Notes`) đều khởi chạy, hiển thị giao diện Material/AppCompat, nhận tương tác chuột/bàn phím/menu/dialog đầu-cuối, đọc/ghi file thật trên đĩa, xóa metadata EXIF JPEG/PNG thật qua `ExifInterface` + `FileProvider`, và lưu/truy vấn dữ liệu bền vững qua Room SQLite trên Windows.
- **Hạn chế chuyển sang Sprint 11–13:**
  - Các ứng dụng cần kết nối mạng thời gian thực và tải ảnh/dữ liệu trực tuyến (`ConnectivityManager.registerDefaultNetworkCallback`, `NetworkCapabilities`, `WifiManager`, `TrafficStats`, `NetworkSecurityPolicy`, `ImageDecoder`, `Glide`/`Coil`/`OkHttp` nâng cao, mở link ngoài qua trình duyệt Windows) sẽ được giải quyết trong **Sprint 11 (Phase C: Non-GMS Online & Media Apps)**.
  - Các ứng dụng thương mại phụ thuộc sâu vào Google Play Services / Firebase khởi tạo sớm, chữ ký chứng chỉ `META-INF/*.RSA` thật, AIDL `microG-lite` và thư viện native `.so` JNI HLE sẽ được giải quyết trong **Sprint 12 (Phase D1)** và **Sprint 13 (Phase D2)**.

### 4. Chuẩn bị cho Sprint 11 (Phase C — Non-GMS Online & Media Apps) — Bắt đầu ở chat mới
Khi mở phiên hội thoại mới cho **Sprint 11**, triển khai theo thứ tự sau:
1. **Bước 11.1 — Hoàn thiện Hạ tầng Mạng Hiện đại (`android.net.*` & `android.net.wifi.*`):**
   - Nâng cấp `ConnectivityManager` hỗ trợ đầy đủ `registerDefaultNetworkCallback`, `registerNetworkCallback(NetworkRequest, NetworkCallback)`, `unregisterNetworkCallback`, và tự động bắn callback `onAvailable(Network)` / `onCapabilitiesChanged(Network, NetworkCapabilities)` với cờ `NET_CAPABILITY_INTERNET`, `NET_CAPABILITY_VALIDATED`, `TRANSPORT_WIFI`/`TRANSPORT_ETHERNET`.
   - Cài đặt/mở rộng `Network`, `NetworkCapabilities`, `NetworkRequest`, `LinkProperties`, `TrafficStats` (no-op an toàn cho OkHttp/Retrofit/ExoPlayer), `NetworkSecurityPolicy` (`isCleartextTrafficPermitted`), và `WifiManager` / `WifiInfo`.
2. **Bước 11.2 — Giải mã Ảnh Hiện đại & Media (`ImageDecoder`, `MediaStore`, `ContentResolver`):**
   - Cài đặt `android.graphics.ImageDecoder` (`createSource`, `decodeBitmap`, `decodeDrawable`, `OnHeaderDecodedListener`) cho API 28+ (Glide/Coil).
   - Bổ sung hỗ trợ `ExifInterface(InputStream)` / `ExifInterface(FileDescriptor)` và mở rộng `ContentResolver.openFileDescriptor` / `openAssetFileDescriptor` cho các thư viện tải ảnh.
   - Nối `Intent.ACTION_VIEW` với URI `http://` hoặc `https://` trong `Activity.startActivity()` ra trình duyệt mặc định của Windows (`Desktop.getDesktop().browse(...)` kèm chế độ kiểm thử không mở cửa sổ ngoài khi chạy test tự động).
3. **Bước 11.3 — Kiểm thử Thực tế với APK Online Non-GMS & Viết `Sprint11FrameworkTest`:**
   - Tuyển chọn/nạp APK kiểm thử có kết nối mạng không phụ thuộc GMS (hoặc ứng dụng đọc tin/tải dữ liệu trực tuyến từ F-Droid), chạy kiểm thử tải dữ liệu thật qua HTTP/HTTPS (OkHttp / `HttpURLConnection` / `WebView`) và hiển thị lên giao diện Windows.
   - Viết `Sprint11FrameworkTest` kiểm chứng toàn bộ luồng `NetworkCallback`, `ImageDecoder`, `TrafficStats`, `NetworkSecurityPolicy`, `WifiManager` và HTTP client; nghiệm thu cuối Sprint 11, cập nhật tài liệu và commit Git sạch sẽ.

---

## Sprint 11 (Phase C): Ứng dụng Không dùng Google Play Services (Non-GMS Online & Media Apps)
**Trạng thái:** ✅ Đã hoàn thành & Nghiệm thu (19/11/2026)

### 1. Các hạng mục đã hoàn thành
1. **Hạ tầng Mạng Hiện đại (`android.net.*`, `android.net.wifi.*`, `android.security.*` & `httpcore` cho Volley):**
   - Nâng cấp [ConnectivityManager.java](../framework/src/main/java/android/net/ConnectivityManager.java) và [NetworkCapabilities.java](../framework/src/main/java/android/net/NetworkCapabilities.java): hỗ trợ đầy đủ `registerDefaultNetworkCallback`, `registerNetworkCallback`, `unregisterNetworkCallback`, `getLinkProperties`, tự động bắn `onAvailable` / `onCapabilitiesChanged` / `onLinkPropertiesChanged` khi đăng ký và `onLost` khi mất mạng (`setSimulatedOffline`), gắn cờ `NET_CAPABILITY_INTERNET` / `NET_CAPABILITY_VALIDATED` / `TRANSPORT_WIFI` + `TRANSPORT_ETHERNET` và `WifiInfo` transport info.
   - Xây dựng mới bộ mô hình mạng chuẩn Android SDK: [TransportInfo.java](../framework/src/main/java/android/net/TransportInfo.java), [IpPrefix.java](../framework/src/main/java/android/net/IpPrefix.java), [LinkAddress.java](../framework/src/main/java/android/net/LinkAddress.java), [RouteInfo.java](../framework/src/main/java/android/net/RouteInfo.java), [ProxyInfo.java](../framework/src/main/java/android/net/ProxyInfo.java), [LinkProperties.java](../framework/src/main/java/android/net/LinkProperties.java), [DhcpInfo.java](../framework/src/main/java/android/net/DhcpInfo.java), [TrafficStats.java](../framework/src/main/java/android/net/TrafficStats.java) và [NetworkSecurityPolicy.java](../framework/src/main/java/android/security/NetworkSecurityPolicy.java).
   - Xây dựng mới tầng Wi-Fi: [WifiManager.java](../framework/src/main/java/android/net/wifi/WifiManager.java) (kèm `WifiLock`, `MulticastLock`, phân giải địa chỉ IPv4 thật trên Windows, `getConnectionInfo()`, `getDhcpInfo()`, `calculateSignalLevel`), [WifiInfo.java](../framework/src/main/java/android/net/wifi/WifiInfo.java) và [SupplicantState.java](../framework/src/main/java/android/net/wifi/SupplicantState.java); đăng ký `WIFI_SERVICE` trong [Context.java](../framework/src/main/java/android/content/Context.java) & [AndroidRuntimeContext.java](../framework/src/main/java/dev/aaow/rt/AndroidRuntimeContext.java).
   - Bổ sung phụ thuộc thuần Java `org.apache.httpcomponents:httpcore:4.4.16` vào [framework/build.gradle.kts](../framework/build.gradle.kts) để đáp ứng chữ ký `org.apache.http.legacy` (`StatusLine`/`HttpResponse`/`Header`) cho các ứng dụng Android dùng **Volley**.
2. **Giải mã Ảnh Hiện đại, `MediaStore` & Cầu nối Trình duyệt Windows (`:framework`):**
   - Xây dựng mới [ImageDecoder.java](../framework/src/main/java/android/graphics/ImageDecoder.java) (`createSource` từ `byte[]`/`ByteBuffer`/`File`/`ContentResolver+Uri`/`Callable<AssetFileDescriptor>`, `decodeBitmap`/`decodeDrawable`, `OnHeaderDecodedListener` với `setTargetSize`/`setCrop`/`setPostProcessor`), [ColorSpace.java](../framework/src/main/java/android/graphics/ColorSpace.java), [PostProcessor.java](../framework/src/main/java/android/graphics/PostProcessor.java), [Size.java](../framework/src/main/java/android/util/Size.java) và [SizeF.java](../framework/src/main/java/android/util/SizeF.java).
   - Mở rộng [BitmapFactory.java](../framework/src/main/java/android/graphics/BitmapFactory.java), [Bitmap.java](../framework/src/main/java/android/graphics/Bitmap.java) và [WebpHelper.java](../framework/src/main/java/dev/aaow/rt/jni/WebpHelper.java) bổ sung đầy đủ trường `Options` cho Glide/Picasso/Coil (`inBitmap`, `inTempStorage`, `inPreferredColorSpace`, `outConfig`, `outColorSpace`, `mCancel`, `requestCancelDecode()`, `decodeResourceStream`).
   - Xây dựng mới [BaseColumns.java](../framework/src/main/java/android/provider/BaseColumns.java), [MediaStore.java](../framework/src/main/java/android/provider/MediaStore.java) và [MediaStoreProvider.java](../framework/src/main/java/dev/aaow/rt/MediaStoreProvider.java) ánh xạ `content://media/...` sang `sdcard/{Pictures,Movies,Music,Download,Documents}`; mở rộng [ContentResolver.java](../framework/src/main/java/android/content/ContentResolver.java) hỗ trợ `openFileDescriptor`/`openAssetFileDescriptor` cho `android.resource://`.
   - Xây dựng mới [WindowsBrowserBridge.java](../framework/src/main/java/dev/aaow/rt/WindowsBrowserBridge.java) và nối vào [AndroidRuntimeContext.java](../framework/src/main/java/dev/aaow/rt/AndroidRuntimeContext.java) & [Activity.java](../framework/src/main/java/android/app/Activity.java) để chuyển `Intent.ACTION_VIEW` (`http://`/`https://`) ra trình duyệt mặc định của Windows (`Desktop.getDesktop().browse(...)`, có cơ chế chặn bật cửa sổ ngoài khi chạy headless/test).
3. **Vá lỗi & Mở rộng Đồ họa, UI, Vòng đời & Chuyển cảnh phát hiện qua APK Online thực tế (`:framework`, `:launcher`):**
   - Cài đặt mới [ProgressDialog.java](../framework/src/main/java/android/app/ProgressDialog.java), [ShapeDrawable.java](../framework/src/main/java/android/graphics/drawable/ShapeDrawable.java) + `shapes/{Shape,RectShape,OvalShape,RoundRectShape}`, [PathEffect.java](../framework/src/main/java/android/graphics/PathEffect.java), [DashPathEffect.java](../framework/src/main/java/android/graphics/DashPathEffect.java), [PathMeasure.java](../framework/src/main/java/android/graphics/PathMeasure.java), [AppWidgetManager.java](../framework/src/main/java/android/appwidget/AppWidgetManager.java) / `AppWidgetProvider` / `AppWidgetProviderInfo`, [DateFormat.java](../framework/src/main/java/android/text/format/DateFormat.java), [DateUtils.java](../framework/src/main/java/android/text/format/DateUtils.java), [PointerIcon.java](../framework/src/main/java/android/view/PointerIcon.java), [WindowMetrics.java](../framework/src/main/java/android/view/WindowMetrics.java), [WindowId.java](../framework/src/main/java/android/view/WindowId.java), [ViewOverlay.java](../framework/src/main/java/android/view/ViewOverlay.java) và [ViewGroupOverlay.java](../framework/src/main/java/android/view/ViewGroupOverlay.java).
   - Khắc phục hàng loạt lỗi lõi quan trọng: chuẩn hóa `View.isAttachedToWindow()` + `ViewGroup.dispatchAttachedToWindow()` + bắn `ViewTreeObserver` (`onGlobalLayout`/`onPreDraw`/`onDraw`); tự động làm mới `mainEdtThread` trong [Looper.java](../framework/src/main/java/android/os/Looper.java) khi AWT EDT khởi tạo lại giữa các phiên; giữ nguyên `?query`/`#fragment` cho `file:///android_asset/...` trong [WebViewEngineBridge.java](../framework/src/main/java/dev/aaow/rt/WebViewEngineBridge.java); sửa giải mã màu hệ thống `@android:color/white` & `?attr/actionBarSize` trong [Resources.java](../framework/src/main/java/android/content/res/Resources.java) / [TypedArray.java](../framework/src/main/java/android/content/res/TypedArray.java); hoàn thiện `Rect`/`RectF`, `Path.op(Path, Path, Op)`, `Canvas.saveLayer`/`saveLayerAlpha`, `RelativeLayout` 2-pass measure + `ALIGN_*`, `Switch.setTextOn/Off`, `Environment.getStorageState`, `MotionEvent.getPressure`/`getHistorical*`, `AccessibilityManager` listeners và đổi nút nạp hàng loạt trong [AppHubWindow.java](../launcher/src/main/java/dev/aaow/launcher/AppHubWindow.java) thành **"Nạp bộ APK"**.
4. **Bộ 3 APK Non-GMS Online Thực tế (F-Droid) chạy thật qua Internet (`build/benchmark-apks/`):**
   - **`org.billthefarmer.currency_158.apk` (Currency v1.58, targetSdk 35):** Tải tỷ giá ngoại tệ trực tiếp qua HTTPS ECB XML, hiển thị cờ quốc gia vector (EUR, USD, GBP, CAD, AUD), hỗ trợ bấm làm mới & chọn dòng chuyển đổi ngoại tệ.
   - **`cz.martykan.forecastie_75.apk` (Forecastie v1.23, targetSdk 36 — OkHttp3 321 class + WilliamChart + Leaflet WebView):** Tải trực tiếp JSON từ `https://api.openweathermap.org/data/2.5/{weather,forecast,uvi}` qua HTTPS (0 lỗi), hiển thị màn hình chính (`London, GB`, nhiệt độ `10.2 °C`, `TabLayout` `TODAY | TOMORROW | LATER`, `ViewPager` + `RecyclerView`), vẽ thật biểu đồ đường/cột WilliamChart có lưới nét đứt (`DashPathEffect` + `PathMeasure`) trên `GraphActivity`, và kết xuất bản đồ thời tiết Leaflet OpenStreetMap trực tiếp trên JavaFX `WebView` (`MapActivity`).
   - **`org.woheller69.omweather_50.apk` (Cirrus / OMWeather v5.0, targetSdk 35 — AndroidX WorkManager + Room + Volley + OSMDroid + Open-Meteo):** Chạy trọn luồng `SplashActivity` → `TutorialActivity` → `SettingsActivity` (`PreferenceFragmentCompat` + `Switch`) → `ForecastCityActivity` → `ManageLocationsActivity` (bấm FAB `+` mở `AddLocationDialogOmGeocodingAPI`) → `RainViewerActivity` (Volley tải thật `https://api.librewxr.net/public/weather-maps.json` HTTP 200 trong 890 ms + khởi tạo `OSMDroid MapView` tile cache).

### 2. Kết quả Nghiệm thu Thực tế (Đã chạy sau chỉnh sửa cuối cùng)
- **Kiểm thử tự động JUnit 5 (`.\gradlew.bat test --rerun-tasks --console=plain`):**
  - **46/46 tests PASS (0 failures, 0 errors, 0 skipped)** trong `1m 06s`:
    - `AndroidSdkFallbackClassLoaderTest` (2), `ApkCompatibilityScannerTest` (2)
    - `FrameworkHierarchyTest` (2 — giữ vững **0 `SUPER_MISMATCH`** đối chiếu `android.jar` API 37 trên toàn bộ **432 lớp `android.*`** trong `:framework`, bao gồm toàn bộ 35+ lớp mới của Sprint 11)
    - `Sprint4FrameworkTest` (6), `Sprint5FrameworkTest` (5), `Sprint6FrameworkTest` (4), `Sprint7FrameworkTest` (5), `Sprint8FrameworkTest` (5), `Sprint9FrameworkTest` (5), `Sprint10FrameworkTest` (5 — **0 hồi quy**)
    - `Sprint11FrameworkTest` (5 — **MỚI** tại [Sprint11FrameworkTest.java](../launcher/src/test/java/dev/aaow/launcher/Sprint11FrameworkTest.java), kiểm thử `ConnectivityManager` callbacks + `WifiManager` + `TrafficStats` + `NetworkSecurityPolicy`, `ImageDecoder` + `BitmapFactory.Options` + `MediaStoreProvider` + `WindowsBrowserBridge`, tải JSON & PNG qua `HttpServer` cục bộ, và E2E thực tế trên cả 3 APK Online: `Currency`, `Forecastie`, `OMWeather/Cirrus`).
- **Biên dịch phân phối (`.\aaow.bat --rebuild`) & Quét tương thích tĩnh (`.\aaow.bat --scan`):**
  - `org.billthefarmer.currency_158.apk`: **99,2%** class (125/126), **99,7%** method (383/384, **0 thiếu trên class đã có**), **99,9%** call sites (1.985/1.986).
  - `cz.martykan.forecastie_75.apk`: **68,4%** class (386/564), **69,5%** method (2.595/3.733), **93,4%** call sites (21.947/23.501).
  - `org.woheller69.omweather_50.apk`: **57,9%** class (458/791), **62,2%** method (3.155/5.069), **92,1%** call sites (34.292/37.222).
- **Bộ 10 ảnh chụp nghiệm thu thực tế (`build/sprint11-final/*.png`):**
  - `currency-main.png`: Danh sách tỷ giá ECB kèm cờ vector sắc nét.
  - `forecastie-main.png`, `forecastie-graph.png`, `forecastie-map.png`: Thời tiết trực tuyến London từ OpenWeatherMap, 3 biểu đồ WilliamChart (Temperature, Rain, Pressure) với lưới nét đứt, và bản đồ thời tiết OpenStreetMap/Leaflet trên JavaFX `WebView`.
  - `omweather-main.png`, `omweather-settings.png`, `omweather-forecast.png`, `omweather-manage.png`, `omweather-radar.png`: Trọn luồng Cirrus (`TutorialActivity`, `SettingsActivity` với `SwitchPreference`, `ForecastCityActivity`, `ManageLocationsActivity` với FAB `+`, `RainViewerActivity`).
  - `hub-sprint11.png`: AAOW App Hub hiển thị bộ ứng dụng đã cài (gồm `Cirrus`, `Currency`, `Forecastie`, `APKPure`, 2 APK mẫu) kèm nút **"Nạp bộ APK"**.

### 3. Đánh giá Trung thực (Chạy thật vs. Hạn chế dành cho Sprint 12–13)
- **Chạy thật 100%:** Kết nối HTTP/HTTPS thời gian thực (`OkHttp3`, `Volley` + `httpcore`, `HttpURLConnection`), `ConnectivityManager` callbacks + `LinkProperties` + `WifiManager` (IPv4 thật của Windows), `ImageDecoder`, `BitmapFactory.Options`, `MediaStoreProvider` đọc/ghi thật xuống `sdcard/Pictures`, `WindowsBrowserBridge`, biểu đồ `WilliamChart` (`DashPathEffect` + `PathMeasure`), bản đồ `Leaflet` WebView và `OSMDroid MapView` tile cache đều chạy trực tiếp trên JVM Windows không qua máy ảo.
- **Giới hạn hiện tại & Chuyển sang Sprint 12–13:**
  - `WifiInfo` báo trạng thái kết nối Wi-Fi tổng hợp từ card mạng đang hoạt động của Windows (không truy vấn trực tiếp Windows Native Wi-Fi WLAN API cho SSID/BSSID phần cứng); `TrafficStats` theo dõi tag luồng chuẩn xác cho OkHttp/Volley/ExoPlayer còn bộ đếm byte toàn hệ thống trả mức cơ sở an toàn.
  - Trong `OMWeather`, `WorkManager` ghi cảnh báo nhẹ `W/SupportSQLiteLock` (do ghép đường dẫn tuyệt đối Windows `D:\...` vào tên file `.lck` trong `cacheDir`) nhưng tự bắt lỗi an toàn và Room DB vẫn vận hành bình thường.
  - Các ứng dụng thương mại phụ thuộc Google Play Services / Firebase khởi tạo sớm, kiểm tra chữ ký chứng chỉ X.509 thật từ `META-INF/*.RSA` của APK, và AIDL Binder của `microG-lite` là trọng tâm của **Sprint 12 (Phase D1)** và **Sprint 13 (Phase D2)**.

### 4. Chuẩn bị cho Sprint 12 (Phase D1 — Hạ tầng GMS Khởi động Sớm, X.509 Cert & AIDL `microG-lite`) — Bắt đầu ở chat mới
Khi mở phiên hội thoại mới cho **Sprint 12**, triển khai theo thứ tự sau (tập trung làm sạch các lỗi khởi động GMS/chứng chỉ trên `com.apkpure.aegon.apk`):
1. **Bước 12.1 — Chuẩn hóa Khởi động Sớm (`Process.myProcessName()` & `FirebaseApp`):**
   - Kiểm tra [Process.java](../framework/src/main/java/android/os/Process.java), `ActivityThread` và [AppRepository.java](../launcher/src/main/java/dev/aaow/launcher/AppRepository.java): đảm bảo `ActivityThread.currentProcessName()` / `Process.myProcessName()` được gán đúng `packageName` **trước** khi khởi tạo lớp `Application` proxy (Tinker/MultiDex) và chạy `ContentProvider` khởi động.
   - Đảm bảo [FirebaseApp.java](../framework/src/microg/java/com/google/firebase/FirebaseApp.java) / `FirebaseInitProvider` tự động nạp cấu hình từ `resources.arsc` (`google_app_id`, `gcm_defaultSenderId`, `google_api_key`, `project_id`) và khởi tạo sớm trước `Application.attachBaseContext()` để xóa triệt để lỗi `IllegalStateException: Default FirebaseApp is not initialized`.
2. **Bước 12.2 — Trích xuất Chứng chỉ X.509 DER Thật từ APK (`META-INF/*.RSA` / `*.DSA` / `*.EC`):**
   - Nâng cấp [ApkFile.java](../apk/src/main/java/dev/aaow/apk/ApkFile.java) và [PackageManager.java](../framework/src/main/java/android/content/pm/PackageManager.java): dùng `JarFile` / `PKCS7` hoặc đọc khối chữ ký X.509 DER thật từ `META-INF/*.RSA`/`*.DSA`/`*.EC` (hỗ trợ cả APK Signature Scheme v2/v3 nếu cần fallback sang X.509 tự sinh hợp lệ theo chuẩn ASN.1 DER — thay thế chuỗi hex giả lập trong [MicroGServiceRegistry.java](../framework/src/main/java/dev/aaow/rt/microg/MicroGServiceRegistry.java)) cho `PackageInfo.signatures` và `PackageInfo.signingInfo` (`GET_SIGNATURES | GET_SIGNING_CERTIFICATES`), xóa triệt để `CertificateException: Incomplete BER/DER data`.
3. **Bước 12.3 — Hoàn thiện AIDL Binder Descriptor & `onServiceConnected` của `microG-lite`:**
   - Mở rộng [MicroGServiceRegistry.java](../framework/src/main/java/dev/aaow/rt/microg/MicroGServiceRegistry.java) cho các dịch vụ GMS phổ biến (`com.google.android.gms.measurement.START`, `com.google.android.gms.analytics.service.START`, `com.google.android.gms.ads.identifier.service.START`, `com.google.android.gms.common.service.START` / `IGmsServiceBroker` trả về `IGmsCallbacks.onPostInitComplete` đúng chuẩn descriptor), xóa triệt để `Got binder with a wrong descriptor` và `AdvertisingIdClient IOException`.
   - Viết `Sprint12FrameworkTest` kiểm chứng khởi tạo sớm `Process.myProcessName()`/`FirebaseApp`, giải mã `CertificateFactory.getInstance("X.509")` từ `PackageManager.getPackageInfo(GET_SIGNATURES)` trên APK thật, và giao dịch Binder AIDL của `microG-lite`; nghiệm thu cuối Sprint 12, giữ 100% test PASS (0 `SUPER_MISMATCH`), cập nhật tài liệu và commit Git sạch sẽ.

---

## Sprint 12 (Phase D1): Hạ tầng GMS Khởi động Sớm, Chứng chỉ X.509 Thật & AIDL `microG-lite`
**Trạng thái:** ✅ Đã hoàn thành & Nghiệm thu (19/11/2026)

### 1. Các hạng mục đã hoàn thành
1. **Chuẩn hóa Khởi động Sớm (`Process.myProcessName()`, `/proc/<pid>/cmdline` & `FirebaseInitProvider` trước `attachBaseContext()`):**
   - Tách riêng `currentProcessName` / `setProcessName()` khỏi `currentPackageName` trong [ActivityThread.java](../framework/src/main/java/android/app/ActivityThread.java) để `Process.myProcessName()`, `ActivityThread.currentProcessName()` và `Application.getProcessName()` luôn sẵn sàng ngay khi tạo [AndroidRuntimeContext.java](../framework/src/main/java/dev/aaow/rt/AndroidRuntimeContext.java) (trước mọi proxy Tinker/CrabShell/MultiDex).
   - Xây dựng mới [LinuxProcFsBridge.java](../framework/src/main/java/dev/aaow/rt/LinuxProcFsBridge.java) tạo các tệp ảo `<dataDir>/aaow-proc/{cmdline,cpuinfo,meminfo}` chứa `<processName>\0` và chèn bytecode ASM trong [DexLoader.java](../launcher/src/main/java/dev/aaow/launcher/DexLoader.java) (cache key `-v12.jar`) tự động chuyển hướng `FileReader(String|File)` và `FileInputStream(String|File)` cho `/proc/<pid>/cmdline`, `/proc/self/cmdline`, `/proc/cpuinfo`, `/proc/meminfo` trên Windows — giúp `GMS ProcessUtils.getMyProcessName()`, `QDDownloader` và `AegonApplication` đọc đúng tên tiến trình mà không văng `FileNotFoundException`.
   - Bổ sung `Application.preAttachBaseContext(Context)` trong [Application.java](../framework/src/main/java/android/app/Application.java), `installEarlyFirebaseProviders()` (chống trùng lặp với `installManifestContentProviders()` và khớp cả `FirebaseInitProvider` lẫn authority `.firebaseinitprovider`) trong [AndroidRuntimeContext.java](../framework/src/main/java/dev/aaow/rt/AndroidRuntimeContext.java), và chuẩn hóa chuỗi khởi tạo trong [AppRepository.java](../launcher/src/main/java/dev/aaow/launcher/AppRepository.java) (`preAttachBaseContext` → `setApplicationContext(app)` → `installEarlyFirebaseProviders` → `app.attach` → `installManifestContentProviders` → `app.onCreate`). Nhờ vậy `FirebaseInitProvider` khởi tạo xong `FirebaseApp` (`ge.qdae`) *trước* `RealApplicationLike.attachBaseContext()` và `getApplicationContext()` đã là `Application` (không văng `ClassCastException` trong `com.google.android.gms.internal.measurement.zzee`).
   - Bổ sung [TelephonyManager.java](../framework/src/main/java/android/telephony/TelephonyManager.java) (`Context.TELEPHONY_SERVICE = "phone"`) dập sạch 16 lỗi NPE `getSimOperator()` từ Google DataTransport, [Messenger.java](../framework/src/main/java/android/os/Messenger.java) + trường `Message.replyTo`, và đọc thêm `ga_trackingId` trong [FirebaseOptions.java](../framework/src/microg/java/com/google/firebase/FirebaseOptions.java).
2. **Trích xuất Chứng chỉ X.509 DER Thật từ APK (`META-INF/*.RSA` / `*.DSA` / `*.EC` & APK Signature Scheme v2/v3):**
   - Xây dựng mới [ApkSignatureExtractor.java](../apk/src/main/java/dev/aaow/apk/ApkSignatureExtractor.java) hỗ trợ đầy đủ **APK Signature Scheme v3.1 / v3 / v2** (`APK Sig Block 42` trước Central Directory), **JAR Signing Scheme v1** (`META-INF/*.RSA`, `*.DSA`, `*.EC` định dạng PKCS#7/CMS), và bộ sinh chứng chỉ tự ký X.509 v3 DER chuẩn RFC 5280 thuần Java (`RSA-2048` + `SHA256withRSA`, lưu đệm tĩnh) cho gói ảo `microG-lite` và APK dev chưa ký.
   - Tích hợp `getSigningResult()` / `readSigningCertificates()` vào [ApkFile.java](../apk/src/main/java/dev/aaow/apk/ApkFile.java), tạo mới [SigningInfo.java](../framework/src/main/java/android/content/pm/SigningInfo.java) (khớp chữ ký API 37 `android.jar`, hỗ trợ `Parcelable`, `getApkContentsSigners`, `getSigningCertificateHistory`, `getPublicKeys`, `getSchemeVersion`, `signersMatchExactly`), thêm trường `PackageInfo.signingInfo` và nâng cấp [PackageManager.java](../framework/src/main/java/android/content/pm/PackageManager.java) (`GET_SIGNATURES | GET_SIGNING_CERTIFICATES`, `getPackageArchiveInfo`, `hasSigningCertificate` hỗ trợ `CERT_INPUT_RAW_X509` và `CERT_INPUT_SHA256`).
   - Thay thế hoàn toàn các chuỗi chữ ký hex giả bị cắt cụt trong [MicroGServiceRegistry.java](../framework/src/main/java/dev/aaow/rt/microg/MicroGServiceRegistry.java) bằng chứng chỉ X.509 v3 DER hợp lệ chuẩn RFC 5280.
3. **Hoàn thiện AIDL Binder Descriptor, `IGmsServiceBroker` (`code = 46`) & Căn chỉnh 4-byte `Parcel` cho GMS `SafeParcel`:**
   - Mở rộng [MicroGServiceRegistry.java](../framework/src/main/java/dev/aaow/rt/microg/MicroGServiceRegistry.java) với các AIDL Binder cụ thể đúng chuẩn interface descriptor:
     - `AdvertisingIdBinder` (`com.google.android.gms.ads.identifier.internal.IAdvertisingIdService`: `code = 1` `getId()`, `code = 2` `isLimitAdTrackingEnabled`, `code = 6` `isAdIdFakeForDebugLogging`, giao ngay không deadlock với `BlockingServiceConnection`).
     - `MeasurementServiceBinder` (`com.google.android.gms.measurement.internal.IMeasurementService`: hỗ trợ gắn kết trực tiếp lẫn bắt tay qua `IGmsServiceBroker` `code = 46` → `IGmsCallbacks.onPostInitComplete` `code = 1`, `getAppInstanceId`, `getAllUserProperties`, `upload`, `getSessionId`...).
     - `AnalyticsServiceBinder` (`com.google.android.gms.analytics.internal.IAnalyticsService`: `sendHit`, `clearHits`, `getVersion`), `AppSetServiceBinder` (`IAppSetService`) và `GmsServiceBrokerBinder` tổng quát (tự đọc `BaseGmsClient.getServiceDescriptor()` khi gặp dịch vụ GMS mở rộng).
   - Bổ sung cầu nối ASM trong [DexLoader.java](../launcher/src/main/java/dev/aaow/launcher/DexLoader.java) điều phối `GooglePlayServicesUtilLight.isGooglePlayServicesAvailable(Context, int)` sang `MicroGServiceRegistry.isGooglePlayServicesAvailable(...)` (trả `ConnectionResult.SUCCESS` = `0` cho dịch vụ microG-lite nội bộ, tránh yêu cầu khóa riêng của Google).
   - Nâng cấp [Parcel.java](../framework/src/main/java/android/os/Parcel.java) chuẩn hóa `dataPosition()` / `setDataPosition()` / `dataSize()` / `dataAvail()` / `appendFrom()` theo đơn vị căn chỉnh **4-byte** (và 8-byte cho `writeLong`/`writeDouble`), cùng đầy đủ `write*Array`/`create*Array`/`writeBinder*`/`SparseBooleanArray`. Nhờ vậy `SafeParcelWriter.finishObjectHeader` (`setDataPosition(start - 4)`) không ghi đè `callbacksBinder` trong giao dịch `IGmsServiceBroker.getService(code = 46)` và Firebase Analytics đọc/ghi trọn vẹn sự kiện `SafeParcel` qua `google_app_measurement_local.db`.

### 2. Kết quả Nghiệm thu Thực tế (Đã chạy sau chỉnh sửa cuối cùng)
- **Kiểm thử tự động JUnit 5 (`.\gradlew.bat test --rerun-tasks --console=plain`):**
  - **53/53 tests PASS (0 failures, 0 errors, 0 skipped)** trong `1m 06s`:
    - `AndroidSdkFallbackClassLoaderTest` (2), `ApkCompatibilityScannerTest` (2)
    - `FrameworkHierarchyTest` (2 — giữ vững **0 `SUPER_MISMATCH`** đối chiếu `android.jar` API 37 trên toàn bộ **435 lớp `android.*`** trong `:framework`, bao gồm `SigningInfo`, `Messenger`, `TelephonyManager`)
    - `Sprint4FrameworkTest` (6), `Sprint5FrameworkTest` (5), `Sprint6FrameworkTest` (4), `Sprint7FrameworkTest` (5), `Sprint8FrameworkTest` (5), `Sprint9FrameworkTest` (5), `Sprint10FrameworkTest` (5), `Sprint11FrameworkTest` (5 — **0 hồi quy**)
    - `Sprint12FrameworkTest` (7 — **MỚI** tại [Sprint12FrameworkTest.java](../launcher/src/test/java/dev/aaow/launcher/Sprint12FrameworkTest.java), kiểm thử `Process.myProcessName()` + `LinuxProcFsBridge` + ASM rewrite `/proc/<pid>/cmdline`, thứ tự `FirebaseInitProvider` trước `attachBaseContext()`, trích xuất X.509 v1/v2/fallback + `SigningInfo` + `hasSigningCertificate`, căn chỉnh 4-byte `Parcel`/SafeParcel, toàn bộ AIDL `microG-lite` + `IGmsServiceBroker` `code = 46`, trích xuất X.509 thật trên 3 APK F-Droid đã cài, và kiểm thử trực tiếp trên `com.apkpure.aegon.apk`).
- **Đối chiếu định lượng thực tế trên `com.apkpure.aegon.apk` (trước vs. sau Sprint 12, log tại `build/sprint12-final/apkpure-run.log`):**
  | Chỉ số / Lỗi GMS & Khởi động | Trước Sprint 12 | Sau Sprint 12 | Kết quả |
  |---|---:|---:|---|
  | `IllegalStateException: Default FirebaseApp is not initialized` | 5 | **0** | ✅ Xóa triệt để |
  | `CertificateException: Incomplete BER/DER data` | 18 | **0** | ✅ Xóa triệt để (trích xuất `APKPURE.RSA` 487B thật) |
  | `E/FA` & `E/GAv4: Got binder with a wrong descriptor` | 2 | **0** | ✅ Xóa triệt để |
  | `W/FA: Service invalid` | 1 | **0** | ✅ Xóa triệt để (`V/FA: Service available`) |
  | `AdvertisingIdClient IOException: Google Play services not available` | 2 | **0** | ✅ Xóa triệt để (4/4 lần bind `IAdvertisingIdService` OK) |
  | `W/GAv4: never got onServiceConnected callback` | 1 | **0** | ✅ Xóa triệt để (`Bound to IAnalyticsService interface`) |
  | `FileNotFoundException: \proc\<pid>\cmdline` | 1 | **0** | ✅ Xóa triệt để (`ProcessUtils.getMyProcessName() == "com.apkpure.aegon"`) |
  | `NullPointerException: TelephonyManager.getSimOperator()` | 16 | **0** | ✅ Xóa triệt để |
  | `IGmsServiceBroker.getService(measurement.START)` → `D/FA: Connected to remote service` | 0 | **1** | ✅ Bắt tay & xử lý 6 tác vụ Firebase Analytics |
- **Biên dịch phân phối (`.\aaow.bat --rebuild`), Quét tĩnh (`--scan`) & Ảnh chụp không hồi quy (`build/sprint12-final/*.png`):**
  - `com.apkpure.aegon.apk` (`--scan`): **66,1%** class hệ thống trong `:framework` (502/760), **74,9%** method (3.507/4.684), **97,0%** call sites (72.440/74.655).
  - Bộ ảnh nghiệm thu không hồi quy: `showcase.png`, `game2048.png`, `currency.png`, `forecastie.png` (tải thật `London, GB 9.4 °C` từ OpenWeatherMap), `hub.png`, `apkpure.png`.

### 3. Đánh giá Trung thực (Chạy thật vs. Giả lập / Hạn chế dành cho Sprint 13)
- **Chạy thật 100%:**
  - Trích xuất chứng chỉ X.509 DER thật từ APK ký v1 (`META-INF/APKPURE.RSA`, F-Droid `*.RSA`) và v2/v3 (`APK Sig Block 42`), xác thực qua JDK `CertificateFactory.getInstance("X.509")` & `PackageManager.hasSigningCertificate` (`RAW_X509`/`SHA256`).
  - Khởi động sớm `Process.myProcessName()`, đọc `/proc/<pid>/cmdline` trên Windows qua `LinuxProcFsBridge`, chạy thật `com.google.firebase.provider.FirebaseInitProvider` của chính APKPure để khởi tạo `FirebaseApp` + `FirebaseAnalytics` (`FA` v73000) + `GoogleAnalytics` (`GAv4` 12.4.51).
  - Bắt tay IPC Binder thật qua `Parcel` căn chỉnh 4-byte (`SafeParcelWriter`/`SafeParcelReader` + `IGmsServiceBroker` `code = 46` → `IGmsCallbacks.onPostInitComplete` `code = 1`) với `IAdvertisingIdService`, `IMeasurementService`, `IAnalyticsService`.
- **Giả lập nhẹ (`microG-lite`) & Hạn chế cố ý chuyển sang Sprint 13 (Phase D2):**
  - Gói hệ thống ảo `com.google.android.gms` / `com.android.vending` sử dụng chứng chỉ tự ký RFC 5280 riêng của AAOW (`CN=AAOW MicroG Services`) và định tuyến `GooglePlayServicesUtilLight.isGooglePlayServicesAvailable` về `MicroGServiceRegistry` (không giả mạo khóa ký Google); các hàm AIDL của `microG-lite` trả mã định danh AAOW cục bộ và phản hồi không lỗi chứ không kết nối máy chủ Google Play thật.
  - Đúng phạm vi Sprint 12 (Phase D1), `com.apkpure.aegon.apk` đã sạch hoàn toàn nhóm lỗi GMS/Firebase/X.509 và vào tới `SplashActivity.onCreate()`, nơi các lỗi tầng UI/API/Native của **Sprint 13 (Phase D2)** bắt đầu xuất hiện:
    1. `NoSuchMethodError: android.content.Intent.resolveTypeIfNeeded(ContentResolver)` trong `SplashActivity.onCreate` (`s1.qdaa.c` / `LocalBroadcastManager`).
    2. `NoSuchFieldError` trong `com.apkpure.aegon.utils.qdcg.uncaughtException:411` (`Timer-2` / `AWT-EventQueue-0`) và `NoSuchMethodError` tại `AppProtoBufUpdateService:g:49` / `qdae:invoke:69`.
    3. Reflection dò `ConnectivityManager.getActiveLinkProperties()` (`ks.qdad.j`).
    4. Các hàm JNI native trong `libqsealib.so` (`com.ola.qsea.encrypt.U.*`) và `libqmp.so` (`com.tencent.qmsp.sdk.u.U.a`) hiện rơi vào giá trị mặc định rỗng gây `InvalidKeySpecException` khi khởi tạo mã hóa khóa công khai.

### 4. Chuẩn bị cho Sprint 13 (Phase D2 — Thực chiến Ứng dụng Thương mại & Mở rộng Native JNI HLE) — Bắt đầu ở chat mới
Khi mở phiên hội thoại mới cho **Sprint 13**, triển khai theo thứ tự sau để đưa `com.apkpure.aegon.apk` vượt qua `SplashActivity` vào màn hình chính và phát hành bản cài Windows cập nhật:
1. **Bước 13.1 — Vá dây chuyền `NoSuchMethodError` / `NoSuchFieldError` trên luồng `SplashActivity` → `MainTabActivity` của APKPure:**
   - Bắt đầu từ các lỗi đã lộ rõ ngay ở cuối log Sprint 12 (`build/sprint12-final/apkpure-run.log`) và top method thiếu trong `build/sprint12-final/apkpure-scan.txt`:
     - `Intent.resolveTypeIfNeeded(ContentResolver)` (gây crash ngay trong `SplashActivity.onCreate`).
     - Soi bytecode `com.apkpure.aegon.utils.qdcg` (dòng 411 — xác định chính xác trường `android.*` thiếu gây `NoSuchFieldError`), `AppProtoBufUpdateService.g` và `qdae.invoke` bằng `javap`.
     - Bổ sung `ConnectivityManager.getActiveLinkProperties()` (hidden API bị reflection dò) và các method UI/View/Parcel/WebSettings thuộc Top 20 của `--scan` (`ProgressBar.setProgressDrawable`, `View.onVisibilityChanged`, `View.setDrawingCacheEnabled`, `View.findViewWithTag`, `View.getWindowVisibleDisplayFrame`, `TextView.setError`, `Parcel.readIntArray`/`readList`/`readMap`, `WebSettings.getDefaultUserAgent`, `Activity.isChangingConfigurations`, `PackageItemInfo.loadIcon`, `Uri.fromParts`, `TextUtils.writeToParcel`...).
   - Chạy lặp `.\aaow.bat --screenshot build\aaow-data\installed-apps\com.apkpure.aegon.apk ...`, truy vết và vá cuốn chiếu cho tới khi `SplashActivity` chuyển sang màn hình chính (và kết xuất nội dung UI thật trên cửa sổ Windows).
2. **Bước 13.2 — Reflection `mNativePtr` & Mở rộng JNI HLE cho Thư viện Thương mại:**
   - Kiểm tra/hỗ trợ các trường reflection JNI phổ biến (`mNativePtr`, `mNativeInstance` trên `Canvas`/`Paint`/`Path`/`Bitmap`/`Typeface`/`SQLiteConnection` nếu APK thương mại truy cập).
   - Bổ sung HLE an toàn trong `NativeBridge` cho các nhóm hàm native `com.ola.qsea.encrypt.U.*` (trả khóa công khai X.509 RSA/chuỗi hợp lệ tránh `InvalidKeySpecException`) và các thư viện `.so` gặp trong quá trình đưa APKPure vào màn hình chính.
3. **Bước 13.3 — Kiểm thử Tự động, Đóng gói Bộ cài Windows & Nghiệm thu Hoàn tất Giai đoạn 2:**
   - Viết `Sprint13FrameworkTest` (giữ **0 `SUPER_MISMATCH`** trên `FrameworkHierarchyTest` và 100% JUnit 5 PASS không hồi quy).
   - Chạy đóng gói lại `AAOW.exe` + bộ cài Windows (`powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1`), nghiệm thu ảnh chụp màn hình APKPure & các APK mẫu/F-Droid, cập nhật tài liệu tổng kết và commit mốc Git cuối Sprint 13.

---

## Sprint 13 (Phase D2): Thực chiến Ứng dụng Thương mại & Mở rộng Native JNI HLE
**Trạng thái:** ✅ Đã hoàn thành & Nghiệm thu (21/11/2026)

### 1. Các hạng mục đã hoàn thành
1. **Vá dây chuyền `NoSuchMethodError` / `NoSuchFieldError` & AndroidX/Material/Glide trên luồng `SplashActivity` → `MainTabActivity`:**
   - **Hệ thống, Vòng đời & Đa luồng:**
     - [Intent.java](../framework/src/main/java/android/content/Intent.java) & [IntentFilter.java](../framework/src/main/java/android/content/IntentFilter.java): Cài đặt `resolveTypeIfNeeded(ContentResolver)`, `resolveType`, `getScheme`, `removeCategory`, `removeFlags`, `replaceExtras`, `filterEquals`, `filterHashCode`, `cloneFilter`, hằng số `ACTION_*`/`CATEGORY_*`/`FLAG_*` và đầy đủ `IntentFilter.match(...)` + `AuthorityEntry` (gỡ chặn `SplashActivity.onCreate` / `LocalBroadcastManager`).
     - [Build.java](../framework/src/main/java/android/os/Build.java), [ConnectivityManager.java](../framework/src/main/java/android/net/ConnectivityManager.java), [PowerManager.java](../framework/src/main/java/android/os/PowerManager.java), [LocaleList.java](../framework/src/main/java/android/os/LocaleList.java), [Uri.java](../framework/src/main/java/android/net/Uri.java): Bổ sung `Build` (`BOOTLOADER`, `SERIAL`, `RADIO`, `HOST`, `USER`, `TIME`, `SOC_*`, `SKU`, `getRadioVersion()`, `getSerial()`, `VERSION.SECURITY_PATCH`, đầy đủ `VERSION_CODES`), `getActiveLinkProperties()`, `isDeviceIdleMode()`/`isLightDeviceIdleMode()`, `LocaleList.getAdjustedDefault()`, `Uri.fromParts`/`getSchemeSpecificPart`/`isOpaque`.
     - [MessageQueue.java](../framework/src/main/java/android/os/MessageQueue.java) (**MỚI**), [Looper.java](../framework/src/main/java/android/os/Looper.java) & [Handler.java](../framework/src/main/java/android/os/Handler.java): Cài đặt `MessageQueue` (`IdleHandler` chạy thật trên Main Looper / Swing EDT), `Looper.myQueue()`/`getQueue()` và ghi nhật ký chẩn đoán `LinkageError` trong `Handler`.
     - [Activity.java](../framework/src/main/java/android/app/Activity.java), [ActivityManager.java](../framework/src/main/java/android/app/ActivityManager.java), [PendingIntent.java](../framework/src/main/java/android/app/PendingIntent.java), [IntentSender.java](../framework/src/main/java/android/content/IntentSender.java) (**MỚI**): Bổ sung `isChangingConfigurations`, `getChangingConfigurations`, `moveTaskToBack`, `finishAffinity`, `finishAndRemoveTask`, `RunningServiceInfo` (`Parcelable`) + `getRunningServices`, `PendingIntent.getIntentSender()`.
     - [DiskSharedPreferences.java](../framework/src/main/java/dev/aaow/rt/DiskSharedPreferences.java): Đồng bộ hóa `Editor` (`put*`/`remove`/`clear`/`commit` với snapshot `staged`) chống `ConcurrentModificationException` khi các SDK nền (như `com.san.ads`) ghi cấu hình đồng thời với Swing EDT.
     - [Parcel.java](../framework/src/main/java/android/os/Parcel.java), [TextUtils.java](../framework/src/main/java/android/text/TextUtils.java), [WebSettings.java](../framework/src/main/java/android/webkit/WebSettings.java), [PackageItemInfo.java](../framework/src/main/java/android/content/pm/PackageItemInfo.java), [PackageManager.java](../framework/src/main/java/android/content/pm/PackageManager.java), [AccessibilityNodeInfo.java](../framework/src/main/java/android/view/accessibility/AccessibilityNodeInfo.java): Bổ sung `readIntArray`/`readList`/`readMap`, `TextUtils.writeToParcel`/`CHAR_SEQUENCE_CREATOR`/`getLayoutDirectionFromLocale`/`isDigitsOnly`, `WebSettings.getDefaultUserAgent`, `loadIcon`/`getApplicationIcon`, `CollectionInfo`/`CollectionItemInfo`.
   - **UI, Material, Glide & Chuyển tiếp Constructor De-Stub:**
     - [Context.java](../framework/src/main/java/android/content/Context.java) & [ContextWrapper.java](../framework/src/main/java/android/content/ContextWrapper.java): Bổ sung `CONTEXT_*` flags, `createPackageContext` và `registerComponentCallbacks`/`unregisterComponentCallbacks` (mở khóa khởi tạo `Glide`).
     - [Window.java](../framework/src/main/java/android/view/Window.java), [View.java](../framework/src/main/java/android/view/View.java), [ViewGroup.java](../framework/src/main/java/android/view/ViewGroup.java): Bổ sung `getWindowManager`/`setWindowManager`/`getLayoutInflater`, `onVisibilityChanged`, `unscheduleDrawable(Drawable)` (khớp API 37 `android.jar` cho Material `ForegroundLinearLayout`), drawing cache, `findViewWithTag`/`findViewWithTagTraversal`, `getWindowVisibleDisplayFrame`.
     - [RelativeLayout.java](../framework/src/main/java/android/widget/RelativeLayout.java), [ProgressBar.java](../framework/src/main/java/android/widget/ProgressBar.java), [TextView.java](../framework/src/main/java/android/widget/TextView.java), [ViewAnimator.java](../framework/src/main/java/android/widget/ViewAnimator.java), [ViewFlipper.java](../framework/src/main/java/android/widget/ViewFlipper.java) (**MỚI**): Cài đặt `RelativeLayout.setGravity`/`getGravity`, `ProgressBar` drawable/tint/secondary/increment, đầy đủ API AppCompat trên `TextView` (`setError`, `setFontVariationSettings`, `setTextLocales`, `setLineHeight`, `setCompoundDrawableTint*`...), `ViewAnimator.setInAnimation`/`setOutAnimation`/`setAnimateFirstView` và `ViewFlipper` kế thừa `ViewAnimator` cho `MarqueeView`.
     - [GradientDrawable.java](../framework/src/main/java/android/graphics/drawable/GradientDrawable.java), [ClipDrawable.java](../framework/src/main/java/android/graphics/drawable/ClipDrawable.java) (**MỚI**), [Bitmap.java](../framework/src/main/java/android/graphics/Bitmap.java), [Paint.java](../framework/src/main/java/android/graphics/Paint.java): Hoàn thiện API `GradientDrawable` (`setGradientType`, `setCornerRadii`, `setColor(ColorStateList)`, `setColors`, `setStroke` overloads), `ClipDrawable` kế thừa `DrawableWrapper`, `Bitmap.prepareToDraw`/`reconfigure`/`sameAs`, `Paint.setFilterBitmap`/`setDither`.
     - [AndroidSdkFallbackClassLoader.java](../launcher/src/main/java/dev/aaow/launcher/AndroidSdkFallbackClassLoader.java): Nâng cấp `emitSuperConstructorCall` chuyển tiếp tham số constructor khớp tiền tố (như `(Context, AttributeSet)`) lên `super(...)` thay vì truyền `null`.
     - [HostWindow.java](../framework/src/main/java/dev/aaow/rt/HostWindow.java): Bổ sung vòng lặp chờ `SplashActivity` chuyển sang `MainActivity` (tối đa 5 giây + 1,2 giây ổn định) trong `renderHeadlessScreenshot` và lượt `measure`/`layout` thứ 2 trong `DeviceViewport.paintComponent` khi `decor.isLayoutRequested()` vẫn bật sau lượt đầu (khắc phục thanh `AHBottomNavigation` tạo tab trong `onSizeChanged`).
2. **Giải mã tài nguyên làm rối AndResGuard (`'arg'`) & Giải mã WebP thật thuần Java:**
   - [ResourceTable.java](../apk/src/main/java/dev/aaow/apk/ResourceTable.java), [Resources.java](../framework/src/main/java/android/content/res/Resources.java), [TypedArray.java](../framework/src/main/java/android/content/res/TypedArray.java):
     - Phát hiện APKPure (AAPT2/AndResGuard) nén 1.280 khóa `attr` trùng tên `'arg'`: thêm `ResourceTable.isUniqueAttrKey(String)` ngăn ghép nhầm khác ID giữa các `attr` `0x7f`; tách tra cứu 3 tầng (`resolveStyleAttrExact` → `resolveStyleAttrCrossNamespace` tránh tự tham chiếu `TYPE_ATTRIBUTE` vòng lặp → `resolveSystemAttrDefault` cho `colorBackgroundFloating`, `textColorPrimary`...) giúp cả Material `SCardView`, `AppCompatTextHelper` lẫn `Scrambled Exif` `MaterialAlertDialogBuilder` giải mã chuẩn xác.
     - Cài đặt thật `Resources.obtainTypedArray(int)` đọc bag `array` từ `resources.arsc` và lọc `ResValue.TYPE_ATTRIBUTE` chưa giải được trong `TypedArray.obtain(...)`.
   - [framework/build.gradle.kts](../framework/build.gradle.kts): Tích hợp bộ giải mã WebP thuần Java `com.twelvemonkeys.imageio:imageio-webp:3.12.0` vào `javax.imageio.ImageIO`, giúp `BitmapFactory` / `WebpHelper` / `Glide` giải mã trực tiếp icon và ảnh WebP thật trên Windows không cần thư viện native.
3. **Trường reflection `mNativePtr` / `native_instance` & Mở rộng JNI HLE (`QseaAndQmspJniHandler`):**
   - [Canvas.java](../framework/src/main/java/android/graphics/Canvas.java), [Paint.java](../framework/src/main/java/android/graphics/Paint.java), [Path.java](../framework/src/main/java/android/graphics/Path.java), [Bitmap.java](../framework/src/main/java/android/graphics/Bitmap.java), [Typeface.java](../framework/src/main/java/android/graphics/Typeface.java), [Matrix.java](../framework/src/main/java/android/graphics/Matrix.java), [Parcel.java](../framework/src/main/java/android/os/Parcel.java): Bổ sung định danh con trỏ native tổng hợp duy nhất khác 0 (`mNativePtr`, `mNativeCanvasWrapper`, `mNativePaint`, `mNativePath`, `native_instance`) cho các SDK dò reflection.
   - [QseaAndQmspJniHandler.java](../framework/src/main/java/dev/aaow/rt/jni/QseaAndQmspJniHandler.java) (**MỚI**) đăng ký qua [NativeBridge.java](../framework/src/main/java/dev/aaow/rt/NativeBridge.java): Giả lập HLE cho `libqsealib.so` (`com.ola.qsea.encrypt.U`: sinh khóa công khai RSA-2048 X.509 hợp lệ cho `U.p()`, mã hóa/giải mã AES-CBC thật cho `U.a`, trả `"{}"` cho `U.d` và thông số thiết bị AAOW nhất quán) và `libqmp.so` (`com.tencent.qmsp.sdk.u.U`).
4. **Đóng gói lại Bộ cài Windows Độc lập Sprint 13:**
   - Biên dịch phân phối mới (`.\aaow.bat --rebuild`) và đóng gói qua [package-windows.ps1](../scripts/package-windows.ps1) kèm `imageio-webp-3.12.0.jar` vào bản độc lập `AAOW.exe` / `aaow-cli.exe` / `AAOW-Portable-1.0.0.zip` / `AAOW-Setup-1.0.0.exe`.

### 2. Kết quả Nghiệm thu Thực tế (Chạy sau chỉnh sửa mã nguồn cuối cùng)
- **Kiểm thử tự động JUnit 5 (`.\gradlew.bat test --rerun-tasks --console=plain`):**
  - **60/60 tests PASS (0 failures, 0 errors, 0 skipped)** trong `1m 53s`:
    - `AndroidSdkFallbackClassLoaderTest` (2), `ApkCompatibilityScannerTest` (2)
    - `FrameworkHierarchyTest` (2 — giữ vững **0 `SUPER_MISMATCH`** đối chiếu `android.jar` API 37 trên toàn bộ **439 lớp `android.*`** cấp cao nhất trong `:framework`, bao gồm `MessageQueue`, `ClipDrawable`, `ViewFlipper`, `IntentSender`, `ActivityManager.RunningServiceInfo`)
    - `Sprint4FrameworkTest` (6), `Sprint5FrameworkTest` (5), `Sprint6FrameworkTest` (4), `Sprint7FrameworkTest` (5), `Sprint8FrameworkTest` (5), `Sprint9FrameworkTest` (5), `Sprint10FrameworkTest` (5), `Sprint11FrameworkTest` (5), `Sprint12FrameworkTest` (7 — **0 hồi quy**)
    - `Sprint13FrameworkTest` (7 — **MỚI** tại [Sprint13FrameworkTest.java](../launcher/src/test/java/dev/aaow/launcher/Sprint13FrameworkTest.java), kiểm thử `Build`/`Intent`/`IntentFilter.match`/`ConnectivityManager.getActiveLinkProperties`/`PowerManager`/`MessageQueue`/`Uri.fromParts`/`TextUtils`/`WebSettings`/`PendingIntent.getIntentSender`/`RunningServiceInfo`/`Parcel` + đa luồng `DiskSharedPreferences.Editor`; `GradientDrawable`/`ClipDrawable`/`Context.registerComponentCallbacks`/`ViewFlipper`/`RelativeLayout`/`TextView`/`ProgressBar`/`Window`/`AccessibilityNodeInfo`; reflection `mNativePtr`/`native_instance`; `QseaAndQmspJniHandler` RSA-2048 X.509 & AES-CBC; chuyển tiếp constructor `AndroidSdkFallbackClassLoader`; chống trùng `'arg'` + `obtainTypedArray` + giải mã WebP thật trên `com.apkpure.aegon.apk`; và E2E JVM tách biệt chứng minh `SplashActivity` → `MainTabActivity` trên APKPure).
- **Đối chiếu định lượng thực tế trên `com.apkpure.aegon.apk` (Sprint 12 vs. Sprint 13, `build/sprint13-final/`):**
  | Chỉ số / Kết quả trên `com.apkpure.aegon.apk` | Trước (Sprint 12) | Sau (Sprint 13) | Kết quả |
  |---|---:|---:|---|
  | `--scan`: Độ phủ Class (`:framework` / 760) | 66,1% (502/760) | **67,2% (511/760)** | ✅ +9 lớp hệ thống |
  | `--scan`: Độ phủ Method (khớp `:framework`) | 74,9% (3.507/4.684) | **76,9% (3.603/4.683)** | ✅ +96 chữ ký hàm |
  | `--scan`: Điểm gọi Call Sites (trên 74.655 điểm gọi) | 97,0% (72.440) | **97,4% (72.732)** | ✅ +292 điểm gọi |
  | Chuyển màn hình `SplashActivity` → `MainTabActivity` | ❌ Dừng ở Splash | **✅ Chuyển & vẽ UI chính** | ✅ Vượt Splash thành công |
  | Lỗi dây chuyền luồng UI (`VerifyError`, `NoSuchFieldError`, `InvalidKeySpecException`, `GlideException`, `ConcurrentModificationException`) | Nhiều lỗi chặn UI | **0** | ✅ Sạch trên luồng UI chính |
  | Số chữ ký JNI HLE thực thi (`I/AAOW-JNI`) / Rơi dự phòng (`W/AAOW-JNI`) | 7 HLE / fallback ở `qsea`/`qmp` | **18 HLE / 0 fallback** | ✅ Thêm 11 hàm `qsea`/`qmp` |
  | Kết xuất WebP + HTTPS CDN + Navigation Bar | Trắng icon / mất tab | **✅ Ảnh WebP + 3 tab đáy** | ✅ Tải & hiển thị thật |
- **Đóng gói Bộ cài Windows (`scripts/package-windows.ps1`) & Smoke-test Bản Độc lập:**
  - `JRE jlink`: **59,8 MB**; `app-image` `build\jpackage\AAOW\AAOW.exe` (485.888 B) + `aaow-cli.exe` (482.816 B) tổng **118,2 MB** (tích hợp `imageio-webp-3.12.0.jar` 79.713 B).
  - `build\release\AAOW-Portable-1.0.0.zip`: **100,3 MB** (`105.157.681 B`).
  - `build\release\AAOW-Setup-1.0.0.exe`: **100,4 MB** (`105.306.624 B`).
  - Chạy trực tiếp `aaow-cli.exe --scan samples\aaow-showcase.apk` đạt **100,0%** (38/38 class, 74/74 method, 154/154 call sites) và `aaow-cli.exe --screenshot com.apkpure.aegon.apk build\sprint13-final\apkpure-packaged-cli.png` (**104.549 B**) kết xuất thành công `MainTabActivity` hoàn toàn độc lập không cần Gradle/JDK.
- **Bộ ảnh nghiệm thu thực tế (`build/sprint13-final/`):**
  - `apkpure.png`, `apkpure-packaged-cli.png`, `apkpure-maintab-loaded.png`: `MainTabActivity` của APKPure với thanh tìm kiếm động (`gta 5`, `gta 6`, `buffbuff...`), 4 tab Material (`Featured | Ranking | Categories | Pre-register`), banner PUBG MOBILE, danh sách **Discover Apps** / **Recommended For You** tải ảnh WebP thật qua HTTPS (`Arena Breakout`, `JustPlay`, `Duck Survival`, `Screw Out Factory Puzzle`, `Craft Treasure`, `MBS Mobile`), popup quảng bá `DOWNLOAD`, và thanh điều hướng đáy `HOME | STORE | Hot`.
  - Không hồi quy các ứng dụng Sprint 1–12: `showcase.png`, `game2048.png`, `currency.png` (ECB 2026), `forecastie.png` (London 4,6 °C trực tuyến), `hub.png` (6 ứng dụng đã cài).

### 3. Đánh giá Trung thực (Chạy thật vs. Giả lập / Hạn chế còn tồn tại)
- **Chạy thật 100% trên JVM Windows:**
  - Chuỗi khởi chạy `SplashActivity` → `MainTabActivity` của APKPure (từ bytecode DEX gốc dịch sang JVM), luồng HTTP/HTTPS lấy gợi ý tìm kiếm, feed trang chủ, tải và giải mã ảnh WebP qua `imageio-webp` + Glide, render Material `TabLayout` / `ViewFlipper` / `RecyclerView` / `AHBottomNavigation` và `ClipDrawable` / `GradientDrawable`.
  - Xử lý bất đối xứng RSA-2048 X.509 & AES-CBC trong `QseaAndQmspJniHandler` và đọc bag tài nguyên/chống đụng độ `'arg'` của AndResGuard chạy trực tiếp trên cấu trúc nhị phân `resources.arsc`.
  - Bộ cài `AAOW-Setup-1.0.0.exe` và `aaow-cli.exe` chạy độc lập hoàn chỉnh trên Windows.
- **Giả lập HLE & Hạn chế còn tồn tại (báo cáo trung thực):**
  - `QseaAndQmspJniHandler` là lớp giả lập cấp cao (HLE) cho `libqsealib.so` và `libqmp.so` (trả khóa RSA/định danh cục bộ của AAOW thay vì thực thi mã máy ARM64 gốc).
  - Các trường reflection `mNativePtr` / `native_instance` là định danh số nguyên tổng hợp an toàn trên JVM (không trỏ tới bộ nhớ C/C++ của Skia/Android gốc).
  - Một số tác vụ nền không làm sập UI trên APKPure vẫn còn tồn tại:
    - Trình báo cáo lỗi nền `com.apkpure.aegon.utils.qdbe.c` cố chạy `Runtime.exec("cat /proc/...")` trên Windows làm phát sinh 21 log `IOException` được bắt lại; GAv4 `ExceptionReporter` ghi nhận 6 ngoại lệ nền đã bắt (NPE ở `Firebase Background Thread #0` `@qddf:run:93`, NPE ở `@qdcg:uncaughtException:1126` và 1 `NoSuchMethodError` ở coroutine nền `@qdad:a:72`), cùng 1 `NoSuchFieldException: mGlobal` bị bắt trong `popups.dynamic.qdbf`.
    - Một vài hàm hệ thống ít chạm tới vẫn gọi về Stub an toàn `W/AAOW-STUB` (9 lần: `AppOpsManager.permissionToOp`, `Criteria`, `FileObserver`, `android.text.format.Time`, `Transition`/`ChangeBounds`, `Formatter.formatFileSize`).
    - Tab thứ 4 `ME` ở thanh đáy đôi khi đo `0x0` ở khung hình đầu tiên, các ảnh banner hero cỡ lớn có thể chưa kịp vẽ xong trong cửa sổ chụp headless, và các SDK quảng cáo bên thứ 3 (`com.san.ads` / TopOn) tự bỏ qua khi hết hạn kết nối mạng.

### 4. Tổng kết Hoàn tất Giai đoạn 2 (Sprint 9–13)
Hoàn thành trọn vẹn lộ trình Sprint 9–13: từ việc giải quyết triệt để `VerifyError` DEX/R8/Kotlin (Sprint 9), tương tác đầu-cuối 5 ứng dụng Offline (Sprint 10), ứng dụng Non-GMS có mạng & WebView/Map/Chart (Sprint 11), hạ tầng GMS sớm + chứng chỉ X.509 thật + AIDL `microG-lite` (Sprint 12), cho tới đưa APK thương mại lớn `com.apkpure.aegon.apk` vượt qua màn hình Splash để kết xuất giao diện cửa hàng thực tế kèm bộ cài Windows độc lập cập nhật tại Sprint 13.

---

## Mốc Phát hành `v1.0.0` (Hậu Sprint 13 — UX Load APK, Boot Gating, HDSD Song ngữ & GitHub Release)
**Thời gian hoàn thành:** 21/11/2026  
**Trạng thái:** ✅ **Đã nghiệm thu, đóng gói & phát hành công khai trên GitHub**

### 1. Các hạng mục đã hoàn thiện
1. **Hiệu ứng Load APK 60fps & Nạp bất đồng bộ (Không đơ UI):**
   - [HostWindow.java](../framework/src/main/java/dev/aaow/rt/HostWindow.java): Thêm `BootState` (`LOADING_APK`, `WAITING_FOR_UI`, `BOOTED`, `FAILED`), constructor mở cửa sổ tức thì `HostWindow(String, String, String, Image)`, `showLoadingWindow()`, `updateLoadingProgress(...)`, `attachRuntimeAndLaunch(...)`, `showBootFailure(...)` và lớp phủ đồ họa `drawApkLoadingOverlay(...)` (vòng xoay kép, thanh tiến trình gradient có hiệu ứng shimmer, stepper 4 giai đoạn `APK → DEX → Runtime → UI`, fade-out 240ms).
   - [DexLoader.java](../launcher/src/main/java/dev/aaow/launcher/DexLoader.java): Thêm overload `loadApkDex(..., progressCallback)` và `ensureDexJar(..., progressCallback)` kèm `makeMonotonic(...)` bảo đảm tiến độ tăng đơn điệu trên `parallelStream()`.
   - [AppRepository.java](../launcher/src/main/java/dev/aaow/launcher/AppRepository.java) & [Main.java](../launcher/src/main/java/dev/aaow/launcher/Main.java): Chuyển khởi chạy GUI (`launchApp` và `runApk` khi không truyền `--screenshot`) sang luồng nền daemon, giữ nguyên luồng đồng bộ cho CLI `--screenshot`.
   - [AppHubWindow.java](../launcher/src/main/java/dev/aaow/launcher/AppHubWindow.java): Thêm thanh tiến trình cài đặt `InstallLoadingBanner` (60fps + shimmer) cho kéo-thả / `+ Cài đặt APK...` / `Nạp bộ APK` trên `SwingWorker`, trạng thái thẻ `◌ ĐANG TẢI...` (`Xem cửa sổ tải` / `Hủy`), và sửa `renderScreenshot(...)` chạy an toàn trên EDT kèm tự động tạo thư mục cha.
2. **Chặn Màn hình Thiếu Tài nguyên Trước Khi Thực sự Boot Vào App:**
   - Gỡ bỏ cơ chế tự động chèn `buildDiagnosticFallbackView(...)` trong `HostWindow.startActivityForResult(...)`. Khi `Activity` khởi đầu chưa có nội dung (`hasRealAppContent(top) == false`, như `SplashActivity` chờ bất đồng bộ), giữ `BootState.WAITING_FOR_UI` trên màn hình tải thay vì hiện "Báo cáo Tương thích AAOW".
   - Chỉ khi `FAILED` hoặc chờ quá 10 giây, người dùng mới thấy tùy chọn chủ động bấm `Xem Báo cáo Tương thích` / `Xem chẩn đoán` (`openDiagnosticsManually()`).
3. **Tài liệu Hướng dẫn Sử dụng Song ngữ (EN / VI):**
   - Tạo mới [README.md](../README.md), [USER_GUIDE_EN.md](USER_GUIDE_EN.md) và [USER_GUIDE_VI.md](USER_GUIDE_VI.md).
   - Cập nhật [.gitignore](../.gitignore) bỏ qua `.vscode/`.
4. **Kiểm thử, Đóng gói & Phát hành GitHub `v1.0.0`:**
   - Viết mới [ApkLoadingAndBootUiTest.java](../launcher/src/test/java/dev/aaow/launcher/ApkLoadingAndBootUiTest.java) (4 bài test).
   - `.\gradlew.bat test --rerun-tasks --console=plain`: **64/64 tests PASS** trên 14 bộ kiểm thử (`0 failures`, `0 errors`, `0 skipped`, giữ **0 `SUPER_MISMATCH`** trên `FrameworkHierarchyTest`).
   - Đẩy mã nguồn & tag `v1.0.0` lên `https://github.com/jetpackvn/AAOW-Android-Apps-On-Windows-` (commit `ded2758`), đóng gói lại bằng `scripts/package-windows.ps1` và phát hành công khai tại **GitHub Release `v1.0.0`** (`https://github.com/jetpackvn/AAOW-Android-Apps-On-Windows-/releases/tag/v1.0.0`):
     - `AAOW-Setup-1.0.0.exe`: `105.333.760` bytes (`SHA-256: 19ca012dca563fb46da2b996574d309aa6b3712d74101eb406488356201df149`)
     - `AAOW-Portable-1.0.0.zip`: `105.185.036` bytes (`SHA-256: ea3b4713d6b467d883f9b624720a0c9f88615f61030d11c6fef503dec91a9bea`)

---

## 🎯 Chuẩn bị cho Sprint 14 (Giai đoạn 3 — Mở ở chat mới)
Khi bắt đầu phiên hội thoại mới cho **Sprint 14 (Cô lập Tiến trình Đa cửa sổ, Hoàn thiện UI/UX App Hub & Đa ngôn ngữ VI / EN)**, triển khai theo thứ tự sau:
1. **Bước 14.1 — Cô lập tiến trình cho mỗi ứng dụng mở từ `AppRepository` / `AppHubWindow`:**
   - Thay vì khởi chạy mọi APK trong cùng 1 JVM (gây đụng độ `Toast`/`Dialog`/`PopupWindow` static bridge, `ActivityThread`, `LinuxProcFsBridge`, `NativeBridge`), cho `AppRepository.launchApp(...)` khởi động tiến trình con độc lập (`ProcessBuilder` gọi launcher hiện tại với `--data-dir` và package/APK tương ứng), theo dõi trạng thái sống/dừng của tiến trình con để cập nhật thẻ trên `AppHubWindow` (và hỗ trợ nút Dừng / `destroyForcibly` sạch sẽ).
2. **Bước 14.2 — Đánh bóng giao diện App Hub & PowerShell script:**
   - Thay ký tự `✓` bị lỗi font ô vuông trên nút `Liên kết .apk` của `AppHubWindow` bằng icon vẽ vector hoặc text chuẩn; tùy biến `BasicScrollBarUI` tối đồng bộ nền `#0F111A`; bổ sung nút dọn `dex-cache/` và thống kê dung lượng dữ liệu app.
   - Lưu `scripts/package-windows.ps1` với UTF-8 BOM và sửa log cuối thành phiên bản động thay cho chữ `SPRINT 8`.
3. **Bước 14.3 — Hỗ trợ chuyển ngôn ngữ UI (`VI / EN`):**
   - Tạo lớp tài nguyên chuỗi song ngữ dùng chung cho `AppHubWindow`, `HostWindow` (toolbar/menu/màn hình loading/báo cáo), CLI `--help` và `scripts/AaowSetup.cs`.
4. **Bước 14.4 — Kiểm thử & Nghiệm thu Sprint 14:**
   - Viết `Sprint14FrameworkTest` kiểm chứng cô lập tiến trình con + chuyển đổi ngôn ngữ + không hồi quy bộ 64 test cũ, cập nhật `SPRINT_LOG.md`, `ROADMAP.md`, `GEMINI.md` và commit Git sạch sẽ.





