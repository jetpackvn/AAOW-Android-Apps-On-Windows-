package dev.aaow.launcher;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AlarmManager;
import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobWorkItem;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Looper;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.Process;
import android.os.StatFs;
import android.os.StrictMode;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Base64;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.appset.AppSet;
import com.google.android.gms.appset.AppSetIdInfo;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.security.ProviderInstaller;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.provider.FirebaseInitProvider;
import com.jakewharton.disklrucache.DiskLruCache;
import dev.aaow.apk.ApkFile;
import dev.aaow.apk.ElfFile;
import dev.aaow.rt.AndroidRuntimeContext;
import dev.aaow.rt.Arm64JniInterpreter;
import dev.aaow.rt.NativeBridge;
import dev.aaow.rt.NativeLibraryManager;
import libcore.io.Memory;
import org.apache.commons.logging.LogFactory;
import org.conscrypt.Conscrypt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.awt.EventQueue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử đơn vị toàn diện cho Sprint 7:
 * - Phân tích định dạng nhị phân ELF (.so), giải nén nativeLibraryDir & trình thông dịch ARM64 JNI.
 * - Điều phối DexLoader ACC_NATIVE -> NativeBridge -> HLE (MMKV, WebP, zlib, MemFile) & fallback an toàn.
 * - Lớp tương thích Google Play Services / Firebase (microG-lite), AIDL Binder, child-first ClassLoader & plugin stub.
 * - Các Android Framework API mới (AlarmManager, JobScheduler, Process.myProcessName, Looper EDT/worker, v.v.).
 */
class Sprint7FrameworkTest {

    @BeforeEach
    void resetRuntimeState() {
        NativeBridge.clearDiagnostics();
        NativeLibraryManager.clearSession();
    }

    @AfterEach
    void cleanupRuntimeState() {
        NativeBridge.clearDiagnostics();
        NativeLibraryManager.clearSession();
    }

    @Test
    void elfFileAndArm64InterpreterExecuteMathAndJniNewStringUtf(@TempDir Path tempDir) throws Exception {
        byte[] elfBytes = buildSyntheticArm64Elf();
        assertTrue(ElfFile.isElf(elfBytes));

        ElfFile elf = ElfFile.parse(elfBytes);
        assertEquals(ElfFile.EM_AARCH64, elf.getMachine());
        assertEquals("arm64-v8a", elf.getMachineName());
        assertTrue(elf.is64Bit());
        assertEquals(List.of("libc.so", "liblog.so"), elf.getNeededLibraries());

        String sumSymName = ElfFile.toJniSymbolName("com.example.NativeMath", "sumSquares");
        String greetSymName = ElfFile.toJniSymbolName("com.example.NativeMath", "greetFromArm64");
        ElfFile.ElfSymbol sumSym = elf.findSymbol(sumSymName);
        ElfFile.ElfSymbol greetSym = elf.findSymbol(greetSymName);
        assertNotNull(sumSym);
        assertNotNull(greetSym);
        assertTrue(sumSym.isJniSymbol());
        assertTrue(greetSym.isJniSymbol());

        ElfFile.VirtualImage image = elf.buildVirtualMemoryImage(Arm64JniInterpreter.CODE_BASE_ADDR);
        assertTrue(image.size() > 0);

        // Chạy hàm tính tổng bình phương 1^2 + 2^2 + ... + 5^2 = 55 qua CPU ARM64 ảo hóa
        Arm64JniInterpreter.ExecutionResult mathRes = Arm64JniInterpreter.executeJni(
                image, sumSym, "(I)I", new Object[]{5});
        assertTrue(mathRes.handled(), () -> "ARM64 sumSquares bị bỏ qua: " + mathRes.reason());
        assertEquals(55, mathRes.returnValue());
        assertTrue(mathRes.instructionsExecuted() >= 15);

        // Chạy hàm ARM64 gọi JNIEnv->NewStringUTF qua vtable offset #1336
        Arm64JniInterpreter.ExecutionResult strRes = Arm64JniInterpreter.executeJni(
                image, greetSym, "()Ljava/lang/String;", new Object[0]);
        assertTrue(strRes.handled(), () -> "ARM64 greetFromArm64 bị bỏ qua: " + strRes.reason());
        assertEquals("AAOW-ARM64-OK", strRes.returnValue());

        // Kiểm tra hàm chưa định nghĩa trả unhandled an toàn
        Arm64JniInterpreter.ExecutionResult missingRes = Arm64JniInterpreter.executeJni(
                image, new ElfFile.ElfSymbol("missing", 0x999999L, 4, 2, 1), "()I", new Object[0]);
        assertFalse(missingRes.handled());

        // Nếu APK thương mại APKPure hiện có trên đĩa, kiểm tra phân tích ELF64 & trích xuất thật
        Path apkpureApk = resolveRepoFile("build/aaow-data/installed-apps/com.apkpure.aegon.apk");
        if (Files.isRegularFile(apkpureApk)) {
            try (ApkFile apk = new ApkFile(apkpureApk)) {
                List<String> armLibs = apk.listNativeLibraries("arm64-v8a");
                assertTrue(armLibs.contains("lib/arm64-v8a/libdeflater7z.so"));
                assertTrue(armLibs.contains("lib/arm64-v8a/libdaemon_acc_v2.1.5.so"));

                Path nativeDir = NativeLibraryManager.prepareForApk(apk, tempDir.resolve("apkpure-data"));
                assertNotNull(nativeDir);
                assertTrue(Files.isRegularFile(nativeDir.resolve("libdaemon_acc_v2.1.5.so")));
                assertFalse(NativeLibraryManager.getRegisteredLibraries().isEmpty());

                byte[] rawDaemon = apk.read("lib/arm64-v8a/libdaemon_acc_v2.1.5.so");
                ElfFile daemonElf = ElfFile.parse(rawDaemon);
                assertEquals("arm64-v8a", daemonElf.getMachineName());
                assertTrue(daemonElf.getJniSymbols().stream().anyMatch(s -> s.name().contains("MemFileJniApi")));
            }
        }
    }

    @Test
    void dexLoaderNativeDispatchHleMmkvWebpZlibAndMemFileWork(@TempDir Path tempDir) throws Exception {
        // 1. Tạo lớp MMKV với phương thức native và viết lại qua DexLoader.rewriteApkClassForWindows
        byte[] rawMmkv = buildSyntheticMmkvNativeClass();
        byte[] rewrittenMmkv = DexLoader.rewriteApkClassForWindows(rawMmkv);

        Path jarPath = tempDir.resolve("mmkv-test.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new JarEntry("com/tencent/mmkv/MMKV.class"));
            jos.write(rewrittenMmkv);
            jos.closeEntry();
        }

        Path mmkvDir = tempDir.resolve("mmkv-store");
        try (ApkDexClassLoader loader = new ApkDexClassLoader(jarPath.toUri().toURL(),
                Sprint7FrameworkTest.class.getClassLoader(), Map.of())) {
            Class<?> mmkvCls = Class.forName("com.tencent.mmkv.MMKV", true, loader);
            Method jniInit = mmkvCls.getDeclaredMethod("jniInitialize", String.class, String.class, int.class, boolean.class);
            jniInit.invoke(null, mmkvDir.toString(), null, 0, false);

            long handle = (long) mmkvCls.getDeclaredMethod("getDefaultMMKV", int.class, String.class)
                    .invoke(null, 1, null);
            assertTrue(handle > 0);

            Object inst = mmkvCls.getDeclaredConstructor(long.class).newInstance(handle);
            assertEquals("1.3.9-aaow", mmkvCls.getDeclaredMethod("version").invoke(null));
            assertTrue((boolean) mmkvCls.getDeclaredMethod("encodeString", long.class, String.class, String.class)
                    .invoke(inst, handle, "token", "aaow-secret"));
            assertTrue((boolean) mmkvCls.getDeclaredMethod("encodeInt", long.class, String.class, int.class)
                    .invoke(inst, handle, "launchCount", 42));
            assertEquals("aaow-secret",
                    mmkvCls.getDeclaredMethod("decodeString", long.class, String.class, String.class)
                            .invoke(inst, handle, "token", ""));
            assertEquals(42,
                    mmkvCls.getDeclaredMethod("decodeInt", long.class, String.class, int.class)
                            .invoke(inst, handle, "launchCount", 0));

            String[] keys = (String[]) mmkvCls.getDeclaredMethod("allKeys", long.class, boolean.class)
                    .invoke(inst, handle, false);
            assertTrue(List.of(keys).containsAll(List.of("token", "launchCount")));
            mmkvCls.getDeclaredMethod("sync", long.class).invoke(inst, handle);
            assertTrue(Files.isRegularFile(mmkvDir.resolve("mmkv.default.aaow.mmkv")));
        }

        // Kiểm tra thống kê NativeBridge.getEmulatedNativeCallCounts() ghi nhận lời gọi HLE
        assertTrue(NativeBridge.getEmulatedNativeCallCounts().keySet().stream()
                .anyMatch(k -> k.startsWith("HLE:com.tencent.mmkv.MMKV.encodeString")));

        // 2. DeflateTask, MemFileJniApi, và WebP header qua BitmapFactory
        Object compressedLen = NativeBridge.invokeNative(
                "com.tencent.yybsdk.apkpatch.patch64.DeflateTask.deflateByCallback(J[B)I",
                null, new Object[]{1L, "AAOW Sprint 7 Zlib Test Payload".getBytes(StandardCharsets.UTF_8)});
        assertTrue(compressedLen instanceof Integer len && len > 0);

        Object memHandle = NativeBridge.invokeNative(
                "com.tencent.assistant.alive.db.jni.MemFileJniApi.init(Ljava/lang/String;)J",
                null, new Object[]{tempDir.resolve("mem").toString()});
        assertTrue(memHandle instanceof Long h && h > 0);
        assertEquals(Boolean.TRUE, NativeBridge.invokeNative(
                "com.tencent.assistant.alive.db.jni.MemFileJniApi.writeString(JLjava/lang/String;)Z",
                null, new Object[]{memHandle, "alive-state"}));
        assertEquals("alive-state", NativeBridge.invokeNative(
                "com.tencent.assistant.alive.db.jni.MemFileJniApi.readString(J)Ljava/lang/String;",
                null, new Object[]{memHandle}));

        // Giải mã kích thước thật từ header RIFF WebP (VP8L lossless 64x32 có alpha)
        byte[] vp8lHeader = buildSyntheticVp8lWebpHeader(64, 32, true);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        assertNull(BitmapFactory.decodeStream(new ByteArrayInputStream(vp8lHeader), null, opts));
        assertEquals(64, opts.outWidth);
        assertEquals(32, opts.outHeight);
        assertEquals("image/webp", opts.outMimeType);

        // 3. Lời gọi native chưa có HLE phải trả NativeBridge.UNHANDLED và ghi nhận intercepted
        Object unhandled = NativeBridge.invokeNative("com.unknown.Vendor.secretCheck()I", null, new Object[0]);
        assertEquals(NativeBridge.UNHANDLED, unhandled);
        assertTrue(NativeBridge.getInterceptedNativeCalls().contains("com.unknown.Vendor.secretCheck()I"));
    }

    @Test
    void microGLiteFirebaseLooperAndBinderWorkEndToEnd(@TempDir Path tempDir) throws Exception {
        FirebaseApp.clearInstancesForTest();
        Path sampleApk = resolveRepoFile("samples/aaow-showcase.apk");
        try (ApkFile apk = new ApkFile(sampleApk)) {
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(
                    apk, Sprint7FrameworkTest.class.getClassLoader(), tempDir.resolve("app-data"));

            // ApplicationInfo & PackageManager kiểm tra gói GMS ảo & installer
            assertNotNull(ctx.getApplicationInfo().nativeLibraryDir);
            PackageManager pm = ctx.getPackageManager();
            PackageInfo gmsInfo = pm.getPackageInfo("com.google.android.gms", PackageManager.GET_SIGNATURES);
            assertEquals("com.google.android.gms", gmsInfo.packageName);
            assertNotNull(gmsInfo.signatures);
            assertTrue(gmsInfo.signatures.length > 0);
            assertEquals("com.android.vending", pm.getInstallerPackageName(apk.getManifest().packageName));
            assertFalse(pm.isInstantApp());

            // Kiểm tra phân biệt Main Looper trên Swing EDT với Worker Thread chưa prepare
            AtomicBoolean edtCheckFailedAsExpected = new AtomicBoolean(false);
            EventQueue.invokeAndWait(() -> {
                assertNotNull(Looper.myLooper());
                assertEquals(Looper.getMainLooper(), Looper.myLooper());
                try {
                    AdvertisingIdClient.getAdvertisingIdInfo(ctx);
                } catch (IllegalStateException expected) {
                    edtCheckFailedAsExpected.set(true);
                } catch (IOException unexpected) {
                    throw new AssertionError(unexpected);
                }
            });
            assertTrue(edtCheckFailedAsExpected.get(), "Gọi AdvertisingIdClient trên Main EDT phải báo lỗi giống Android");

            // Worker thread có Looper.myLooper() == null và kết nối qua AIDL Binder thành công
            ExecutorService pool = Executors.newSingleThreadExecutor();
            try {
                AdvertisingIdClient.Info adInfo = pool.submit(() -> {
                    assertNull(Looper.myLooper());
                    return AdvertisingIdClient.getAdvertisingIdInfo(ctx);
                }).get(5, TimeUnit.SECONDS);
                assertNotNull(adInfo);
                assertNotNull(adInfo.getId());
                assertFalse(adInfo.getId().isEmpty());
                assertFalse(adInfo.isLimitAdTrackingEnabled());
            } finally {
                pool.shutdownNow();
            }

            // GoogleApiAvailability, AppSet, ProviderInstaller, Tasks
            assertEquals(ConnectionResult.SUCCESS, GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ctx));
            AppSetIdInfo appSetInfo = Tasks.await(AppSet.getClient(ctx).getAppSetIdInfo());
            assertNotNull(appSetInfo.getId());
            assertEquals(AppSetIdInfo.SCOPE_APP, appSetInfo.getScope());
            ProviderInstaller.installIfNeeded(ctx);

            // GServices ContentProvider qua ContentResolver
            try (Cursor cursor = ctx.getContentResolver().query(
                    Uri.parse("content://com.google.android.gsf.gservices"),
                    null, null, new String[]{"android_id"}, null)) {
                assertNotNull(cursor);
                assertTrue(cursor.moveToFirst());
                assertEquals("android_id", cursor.getString(0));
            }

            // FirebaseInitProvider + FirebaseOptions + các dịch vụ Firebase cơ bản
            FirebaseOptions opts = new FirebaseOptions.Builder()
                    .setApplicationId("1:123456:android:abc")
                    .setApiKey("AIzaTestKey")
                    .setProjectId("aaow-test")
                    .build();
            FirebaseApp app = FirebaseApp.initializeApp(ctx, opts);
            assertEquals("aaow-test", app.getOptions().getProjectId());

            FirebaseInitProvider initProvider = new FirebaseInitProvider();
            initProvider.attachInfo(ctx, null);
            assertNotNull(FirebaseApp.getInstance());
            assertEquals("aaow-fid-installation-id", Tasks.await(FirebaseInstallations.getInstance().getId()));
            assertNotNull(Tasks.await(FirebaseMessaging.getInstance().getToken()));
            FirebaseAnalytics.getInstance(ctx).logEvent("aaow_test_event", null);
            FirebaseCrashlytics.getInstance().setCustomKey("sprint", 7);

            // Đảm bảo tên SharedPreferences chứa ':' không gây InvalidPathException trên Windows
            SharedPreferences prefs = ctx.getSharedPreferences("com.google.firebase.common.prefs:MTIzNDU=", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("firebase_data_collection_default_enabled", true).commit();
            assertTrue(prefs.getBoolean("firebase_data_collection_default_enabled", false));
        } finally {
            FirebaseApp.clearInstancesForTest();
        }
    }

    @Test
    void sprint7SystemServicesAndroidApisAndThirdPartyCompatWork(@TempDir Path tempDir) throws Exception {
        Path sampleApk = resolveRepoFile("samples/aaow-showcase.apk");
        try (ApkFile apk = new ApkFile(sampleApk)) {
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(
                    apk, Sprint7FrameworkTest.class.getClassLoader(), tempDir.resolve("sys-data"));

            Application app = AppRepository.createAndInitializeApplication(
                    apk.getManifest(), Sprint7FrameworkTest.class.getClassLoader(), ctx, null);
            assertNotNull(app);
            assertEquals(apk.getManifest().packageName, Process.myProcessName());
            assertEquals(app, ActivityThread.currentApplication());
            assertEquals(apk.getManifest().packageName, ActivityThread.currentProcessName());

            // AlarmManager hẹn giờ & hủy qua Executor
            AlarmManager am = ctx.getSystemService(AlarmManager.class);
            assertNotNull(am);
            CountDownLatch alarmLatch = new CountDownLatch(1);
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 20, "tag",
                    Runnable::run, alarmLatch::countDown);
            assertTrue(alarmLatch.await(3, TimeUnit.SECONDS));

            // JobScheduler & namespace
            JobScheduler js = ctx.getSystemService(JobScheduler.class);
            assertNotNull(js);
            JobInfo job = new JobInfo.Builder(701, new ComponentName(apk.getManifest().packageName, "dev.aaow.SyncJob"))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setRequiresCharging(true)
                    .build();
            assertEquals(JobScheduler.RESULT_SUCCESS, js.enqueue(job, new JobWorkItem(new Intent("SYNC"))));
            JobScheduler workNs = js.forNamespace("androidx.work");
            workNs.schedule(new JobInfo.Builder(702, new ComponentName(apk.getManifest().packageName, "dev.aaow.Worker")).build());
            assertEquals(1, js.getAllPendingJobs().size());
            assertEquals(1, workNs.getAllPendingJobs().size());
            js.cancel(701);
            assertNull(js.getPendingJob(701));

            // PowerManager, ActivityManager, UserManager, Settings
            PowerManager pm = ctx.getSystemService(PowerManager.class);
            assertNotNull(pm);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "aaow:lock");
            wl.acquire();
            assertTrue(wl.isHeld());
            wl.release();
            assertFalse(wl.isHeld());

            ActivityManager actMgr = ctx.getSystemService(ActivityManager.class);
            assertNotNull(actMgr);
            assertFalse(actMgr.getRunningAppProcesses().isEmpty());
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            actMgr.getMemoryInfo(mi);
            assertTrue(mi.totalMem > 0);

            UserManager um = ctx.getSystemService(UserManager.class);
            assertNotNull(um);
            assertTrue(um.isUserUnlocked());

            assertNotNull(Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID));
            assertTrue(Settings.System.putString(ctx.getContentResolver(), "screen_brightness", "180"));
            assertEquals(180, Settings.System.getInt(ctx.getContentResolver(), "screen_brightness", 0));
            assertEquals(1, Settings.Global.getInt(ctx.getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 0));

            // Base64, StatFs, JsonReader/JsonWriter, Parcel, Uri, StrictMode, Memory, DiskLruCache, Conscrypt, LogFactory
            byte[] sampleBytes = "Xin chào AAOW Sprint 7!".getBytes(StandardCharsets.UTF_8);
            String encoded = Base64.encodeToString(sampleBytes, Base64.URL_SAFE | Base64.NO_WRAP);
            assertArrayEquals(sampleBytes, Base64.decode(encoded, Base64.URL_SAFE | Base64.NO_WRAP));

            StatFs statFs = new StatFs(tempDir.toString());
            assertTrue(statFs.getTotalBytes() > 0);
            assertTrue(statFs.getAvailableBytes() > 0);

            StringWriter sw = new StringWriter();
            try (JsonWriter jw = new JsonWriter(sw)) {
                jw.beginObject().name("sprint").value(7).name("ok").value(true).endObject();
            }
            try (JsonReader jr = new JsonReader(new StringReader(sw.toString()))) {
                jr.beginObject();
                assertEquals("sprint", jr.nextName());
                assertEquals(7, jr.nextInt());
                assertEquals("ok", jr.nextName());
                assertTrue(jr.nextBoolean());
                assertEquals(JsonToken.END_OBJECT, jr.peek());
                jr.endObject();
            }

            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());

            Parcel p = Parcel.obtain();
            p.writeString("parcel-data");
            p.writeInt(777);
            byte[] marshalled = p.marshall();
            p.recycle();

            Parcel p2 = Parcel.obtain();
            p2.unmarshall(marshalled, 0, marshalled.length);
            p2.setDataPosition(0);
            assertEquals("parcel-data", p2.readString());
            assertEquals(777, p2.readInt());
            p2.recycle();

            Uri parsedUri = new Uri.Builder()
                    .scheme("https")
                    .encodedAuthority("example.org:8443")
                    .path("/v1/apps")
                    .encodedQuery("q=aaow")
                    .encodedFragment("top")
                    .build();
            assertEquals("example.org:8443", parsedUri.getAuthority());
            assertEquals("q=aaow", parsedUri.getEncodedQuery());
            assertEquals("top", parsedUri.getFragment());
            assertNull(parsedUri.buildUpon().clearQuery().build().getQuery());

            byte[] memBuf = new byte[8];
            Memory.pokeInt(memBuf, 0, 0x12345678, ByteOrder.BIG_ENDIAN);
            assertEquals(0x12345678, Memory.peekInt(memBuf, 0, ByteOrder.BIG_ENDIAN));

            try (DiskLruCache dlc = DiskLruCache.open(tempDir.resolve("dlc").toFile(), 1, 1, 1024 * 1024)) {
                DiskLruCache.Editor editor = dlc.edit("k1");
                editor.set(0, "cached-v7");
                editor.commit();
                try (DiskLruCache.Snapshot snap = dlc.get("k1")) {
                    assertNotNull(snap);
                    assertEquals("cached-v7", snap.getString(0));
                }
            }

            assertTrue(Conscrypt.isAvailable());
            assertNotNull(Conscrypt.newProviderBuilder().provideTrustManager().build());
            assertNotNull(LogFactory.getLog(Sprint7FrameworkTest.class));

            // View/ViewGroup hasTransientState lan truyền cho RecyclerView
            FrameLayout group = new FrameLayout(ctx);
            TextView child = new TextView(ctx);
            group.addView(child);
            assertFalse(group.hasTransientState());
            child.setHasTransientState(true);
            assertTrue(child.hasTransientState());
            assertTrue(group.hasTransientState());
            child.setHasTransientState(false);
            assertFalse(group.hasTransientState());

            // SQLiteDatabase tên tương đối phải đi vào databases/ của app thay vì rò ra CWD
            Path leakedInCwd = Path.of("relative_leak_guard.db").toAbsolutePath();
            Files.deleteIfExists(leakedInCwd);
            try (SQLiteDatabase relDb = SQLiteDatabase.openOrCreateDatabase("relative_leak_guard.db", null)) {
                assertNotNull(relDb);
                assertFalse(Files.exists(leakedInCwd), "Không được tạo DB tương đối ngoài thư mục làm việc");
                assertTrue(Files.isRegularFile(ctx.getDatabasePath("relative_leak_guard.db").toPath()));
            }
            assertTrue(SQLiteDatabase.deleteDatabase(new java.io.File("relative_leak_guard.db")));
            assertFalse(Files.exists(ctx.getDatabasePath("relative_leak_guard.db").toPath()));
        } finally {
            ActivityThread.setCurrentApplication(null);
        }
    }

    @Test
    void apkDexClassLoaderPrioritizesApkBundledClassesAndSynthesizesOptionalPluginStubs(@TempDir Path tempDir)
            throws Exception {
        // 1. APK tự đóng gói com.google.firebase.FirebaseApp riêng -> KHÔNG bị microg-lite.jar che mất
        byte[] customFirebaseBytes = buildNamedBeanClass("com/google/firebase/FirebaseApp", "marker", "from-apk-dex");
        // 2. Lớp Caller tham chiếu trực tiếp lớp plugin ngoài không tồn tại: tmsdk.fg.module.cleanV2.CleanManager
        byte[] callerBytes = buildPluginCallerClass();

        Path jarPath = tempDir.resolve("child-first-test.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new JarEntry("com/google/firebase/FirebaseApp.class"));
            jos.write(customFirebaseBytes);
            jos.closeEntry();
            jos.putNextEntry(new JarEntry("com/example/PluginCaller.class"));
            jos.write(callerBytes);
            jos.closeEntry();
        }

        ApkDexClassLoader.RefTypeMeta pluginMeta = new ApkDexClassLoader.RefTypeMeta("tmsdk/fg/module/cleanV2/CleanManager");
        pluginMeta.mustBeClass = true;
        pluginMeta.methods.put("scanJunk()I", "scanJunk");

        Map<String, ApkDexClassLoader.RefTypeMeta> externalRefs = new LinkedHashMap<>();
        externalRefs.put(pluginMeta.internalName, pluginMeta);

        try (ApkDexClassLoader loader = new ApkDexClassLoader(
                jarPath.toUri().toURL(),
                Sprint7FrameworkTest.class.getClassLoader(),
                externalRefs)) {
            Class<?> apkFirebase = Class.forName("com.google.firebase.FirebaseApp", true, loader);
            assertEquals("from-apk-dex", apkFirebase.getMethod("marker").invoke(null));
            assertNotEquals(FirebaseApp.class, apkFirebase);

            // Gọi PluginCaller.run() kích hoạt JVM verifier nạp CleanManager stub động
            Class<?> callerCls = Class.forName("com.example.PluginCaller", true, loader);
            int res = (int) callerCls.getMethod("run").invoke(null);
            assertEquals(0, res);
            assertTrue(NativeBridge.getStubbedPluginClasses().contains("tmsdk.fg.module.cleanV2.CleanManager"));

            // Thăm dò Class.forName trực tiếp cho lớp không tồn tại vẫn phải ném ClassNotFoundException
            assertThrows(ClassNotFoundException.class,
                    () -> Class.forName("tmsdk.fg.module.cleanV2.NonExistentProbe", true, loader));
        }
    }

    // ------------------------------------------------------------------ Helpers

    private static Path resolveRepoFile(String relative) {
        Path p = Path.of(relative);
        if (Files.exists(p)) return p;
        return Path.of("..").resolve(relative);
    }

    private static byte[] buildSyntheticArm64Elf() {
        // Chức năng 1 (offset 0x00): sumSquares(JNIEnv* x0, jclass x1, jint n w2)
        int[] func1 = {
                0xD2800003, // MOVZ X3, #0       (sum = 0)
                0xD2800024, // MOVZ X4, #1       (i = 1)
                0xEB02009F, // SUBS XZR, X4, X2  (CMP i, n)
                0x5400008C, // B.GT +4 insns     (nhảy sang ADD X0, X3, #0)
                0x9B040C83, // MADD X3, X4, X4, X3 (sum += i*i)
                0x91000484, // ADD X4, X4, #1    (i++)
                0x17FFFFFC, // B -4 insns        (quay lại CMP)
                0x91000060, // ADD X0, X3, #0    (trả sum)
                0xD65F03C0  // RET
        };
        // Chức năng 2 (offset 0x40): greetFromArm64(JNIEnv* x0, jclass x1) -> NewStringUTF("AAOW-ARM64-OK")
        int[] func2 = {
                0x910003D3, // ADD X19, X30, #0  (lưu LR)
                0x100000A1, // ADR X1, #20       (trỏ chuỗi ngay sau lệnh cuối tại +24)
                0xF9400008, // LDR X8, [X0, #0]  (*env -> vtable)
                0xF9429D08, // LDR X8, [X8, #1336] (NewStringUTF)
                0xD63F0100, // BLR X8
                0xD61F0260  // BR X19
        };
        byte[] strData = "AAOW-ARM64-OK\0".getBytes(StandardCharsets.US_ASCII);

        byte[] codeSeg = new byte[0x80];
        ByteBuffer cb = ByteBuffer.wrap(codeSeg).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < func1.length; i++) cb.putInt(i * 4, func1[i]);
        int f2Off = 0x40;
        for (int i = 0; i < func2.length; i++) cb.putInt(f2Off + i * 4, func2[i]);
        System.arraycopy(strData, 0, codeSeg, f2Off + func2.length * 4, strData.length);

        String sumSym = ElfFile.toJniSymbolName("com.example.NativeMath", "sumSquares");
        String greetSym = ElfFile.toJniSymbolName("com.example.NativeMath", "greetFromArm64");

        // .dynstr: \0 + sumSym + \0 + greetSym + \0 + JNI_OnLoad + \0 + libc.so + \0 + liblog.so + \0
        ByteArrayOutputStream strTab = new ByteArrayOutputStream();
        strTab.write(0);
        int offSum = writeCString(strTab, sumSym);
        int offGreet = writeCString(strTab, greetSym);
        int offOnLoad = writeCString(strTab, "JNI_OnLoad");
        int offLibc = writeCString(strTab, "libc.so");
        int offLiblog = writeCString(strTab, "liblog.so");
        byte[] dynstr = strTab.toByteArray();

        // .dynsym: 4 entries * 24 bytes (index 0 = NULL)
        byte[] dynsym = new byte[4 * 24];
        writeElf64Sym(dynsym, 24, offSum, 2, 1, 0x1000L, func1.length * 4L);
        writeElf64Sym(dynsym, 48, offGreet, 2, 1, 0x1000L + f2Off, func2.length * 4L);
        writeElf64Sym(dynsym, 72, offOnLoad, 2, 1, 0x1000L, 4L);

        // .dynamic: 3 entries * 16 bytes (DT_NEEDED libc.so, DT_NEEDED liblog.so, DT_NULL)
        byte[] dynamic = new byte[3 * 16];
        ByteBuffer db = ByteBuffer.wrap(dynamic).order(ByteOrder.LITTLE_ENDIAN);
        db.putLong(0, 1L); db.putLong(8, offLibc);
        db.putLong(16, 1L); db.putLong(24, offLiblog);
        db.putLong(32, 0L); db.putLong(40, 0L);

        int ehSize = 64;
        int phSize = 56;
        int codeOff = ehSize + phSize; // 120
        int dynstrOff = codeOff + codeSeg.length;
        int dynsymOff = dynstrOff + dynstr.length;
        int dynamicOff = dynsymOff + dynsym.length;
        int shOff = dynamicOff + dynamic.length;
        int totalSize = shOff + 4 * 64;

        byte[] out = new byte[totalSize];
        ByteBuffer buf = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN);

        // ELF64 Header
        out[0] = 0x7F; out[1] = 'E'; out[2] = 'L'; out[3] = 'F';
        out[4] = ElfFile.ELFCLASS64;
        out[5] = ElfFile.ELFDATA2LSB;
        out[6] = 1;
        buf.putShort(16, (short) 3); // ET_DYN
        buf.putShort(18, (short) ElfFile.EM_AARCH64);
        buf.putInt(20, 1);
        buf.putLong(24, 0x1000L);
        buf.putLong(32, ehSize);
        buf.putLong(40, shOff);
        buf.putShort(52, (short) ehSize);
        buf.putShort(54, (short) phSize);
        buf.putShort(56, (short) 1);
        buf.putShort(58, (short) 64);
        buf.putShort(60, (short) 4);

        // Phdr[0]: PT_LOAD
        buf.putInt(ehSize, 1); // PT_LOAD
        buf.putInt(ehSize + 4, 5); // PF_R | PF_X
        buf.putLong(ehSize + 8, codeOff);
        buf.putLong(ehSize + 16, 0x1000L);
        buf.putLong(ehSize + 24, 0x1000L);
        buf.putLong(ehSize + 32, codeSeg.length);
        buf.putLong(ehSize + 40, codeSeg.length);
        buf.putLong(ehSize + 48, 0x1000L);

        System.arraycopy(codeSeg, 0, out, codeOff, codeSeg.length);
        System.arraycopy(dynstr, 0, out, dynstrOff, dynstr.length);
        System.arraycopy(dynsym, 0, out, dynsymOff, dynsym.length);
        System.arraycopy(dynamic, 0, out, dynamicOff, dynamic.length);

        // Shdr[1] = .dynstr (SHT_STRTAB = 3)
        writeElf64Shdr(buf, shOff + 64, 3, dynstrOff, dynstr.length, 0, 0);
        // Shdr[2] = .dynsym (SHT_DYNSYM = 11, link=1, entsize=24)
        writeElf64Shdr(buf, shOff + 128, 11, dynsymOff, dynsym.length, 1, 24);
        // Shdr[3] = .dynamic (SHT_DYNAMIC = 6, link=1, entsize=16)
        writeElf64Shdr(buf, shOff + 192, 6, dynamicOff, dynamic.length, 1, 16);

        return out;
    }

    private static int writeCString(ByteArrayOutputStream out, String s) {
        int start = out.size();
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        out.write(b, 0, b.length);
        out.write(0);
        return start;
    }

    private static void writeElf64Sym(byte[] dst, int off, int stName, int stType, int shndx, long value, long size) {
        ByteBuffer b = ByteBuffer.wrap(dst).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(off, stName);
        dst[off + 4] = (byte) (stType & 0x0F);
        b.putShort(off + 6, (short) shndx);
        b.putLong(off + 8, value);
        b.putLong(off + 16, size);
    }

    private static void writeElf64Shdr(ByteBuffer buf, int off, int type, long fileOff, long size, int link, long entsize) {
        buf.putInt(off + 4, type);
        buf.putLong(off + 24, fileOff);
        buf.putLong(off + 32, size);
        buf.putInt(off + 40, link);
        buf.putLong(off + 56, entsize);
    }

    private static byte[] buildSyntheticMmkvNativeClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/tencent/mmkv/MMKV", null, "java/lang/Object", null);
        cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "nativeHandle", "J", null, null).visitEnd();

        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(J)V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitVarInsn(Opcodes.LLOAD, 1);
        init.visitFieldInsn(Opcodes.PUTFIELD, "com/tencent/mmkv/MMKV", "nativeHandle", "J");
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        int pubNative = Opcodes.ACC_PUBLIC | Opcodes.ACC_NATIVE;
        int pubStatNative = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE;
        cw.visitMethod(pubStatNative, "jniInitialize", "(Ljava/lang/String;Ljava/lang/String;IZ)V", null, null).visitEnd();
        cw.visitMethod(pubStatNative, "getDefaultMMKV", "(ILjava/lang/String;)J", null, null).visitEnd();
        cw.visitMethod(pubStatNative, "version", "()Ljava/lang/String;", null, null).visitEnd();
        cw.visitMethod(pubNative, "encodeString", "(JLjava/lang/String;Ljava/lang/String;)Z", null, null).visitEnd();
        cw.visitMethod(pubNative, "decodeString", "(JLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;", null, null).visitEnd();
        cw.visitMethod(pubNative, "encodeInt", "(JLjava/lang/String;I)Z", null, null).visitEnd();
        cw.visitMethod(pubNative, "decodeInt", "(JLjava/lang/String;I)I", null, null).visitEnd();
        cw.visitMethod(pubNative, "allKeys", "(JZ)[Ljava/lang/String;", null, null).visitEnd();
        cw.visitMethod(pubNative, "sync", "(Z)V", null, null).visitEnd(); // also add 1-arg variant? keep native signature matching handler:
        cw.visitMethod(pubNative, "sync", "(J)V", null, null).visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] buildSyntheticVp8lWebpHeader(int width, int height, boolean hasAlpha) {
        byte[] bytes = new byte[30];
        ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        bytes[0] = 'R'; bytes[1] = 'I'; bytes[2] = 'F'; bytes[3] = 'F';
        b.putInt(4, 22);
        bytes[8] = 'W'; bytes[9] = 'E'; bytes[10] = 'B'; bytes[11] = 'P';
        bytes[12] = 'V'; bytes[13] = 'P'; bytes[14] = '8'; bytes[15] = 'L';
        b.putInt(16, 5);
        bytes[20] = 0x2F;
        int packed = ((width - 1) & 0x3FFF) | (((height - 1) & 0x3FFF) << 14) | ((hasAlpha ? 1 : 0) << 28);
        b.putInt(21, packed);
        return bytes;
    }

    private static byte[] buildNamedBeanClass(String internalName, String methodName, String returnStr) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                methodName, "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitLdcInsn(returnStr);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] buildPluginCallerClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/example/PluginCaller", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", "()I", null, null);
        mv.visitCode();
        mv.visitTypeInsn(Opcodes.NEW, "tmsdk/fg/module/cleanV2/CleanManager");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "tmsdk/fg/module/cleanV2/CleanManager", "<init>", "()V", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "tmsdk/fg/module/cleanV2/CleanManager", "scanJunk", "()I", false);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }
}
