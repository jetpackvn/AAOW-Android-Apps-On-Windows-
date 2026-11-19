package dev.aaow.launcher;

import android.app.ActivityThread;
import android.app.Application;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import dev.aaow.apk.AndroidManifest;
import dev.aaow.apk.ApkFile;
import dev.aaow.apk.ApkSignatureExtractor;
import dev.aaow.rt.AndroidRuntimeContext;
import dev.aaow.rt.LinuxProcFsBridge;
import dev.aaow.rt.microg.MicroGServiceRegistry;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị và E2E cho Sprint 12 (Phase D1 — Hạ tầng GMS Khởi động Sớm,
 * Chứng chỉ X.509 Thật & AIDL microG-lite):
 * 1. Khởi động sớm Process.myProcessName(), cầu nối /proc/<pid>/cmdline (LinuxProcFsBridge + ASM)
 *    và thứ tự khởi tạo FirebaseInitProvider trước Application.attachBaseContext().
 * 2. Trích xuất chứng chỉ X.509 DER thật từ APK (v1 META-INF/*.RSA, v2 APK Sig Block 42, fallback tự ký RFC 5280),
 *    PackageInfo.signingInfo (SigningInfo) & PackageManager.hasSigningCertificate().
 * 3. Căn chỉnh 4-byte của Parcel cho GMS SafeParcelWriter/SafeParcelReader & bắt tay AIDL microG-lite
 *    (IAdvertisingIdService, IMeasurementService, IAnalyticsService, IAppSetService, IGmsServiceBroker code=46).
 * 4. Kiểm thử thực chiến trên com.apkpure.aegon.apk & F-Droid APK thật.
 */
class Sprint12FrameworkTest {

    // ===== Các lớp kiểm thử thứ tự khởi tạo Early FirebaseInitProvider =====
    static final List<String> INIT_EVENTS = new CopyOnWriteArrayList<>();

    public static final class TestEarlyFirebaseInitProvider extends ContentProvider {
        @Override
        public boolean onCreate() {
            INIT_EVENTS.add("firebase_provider:" + Process.myProcessName()
                    + ":appCtx=" + (getContext().getApplicationContext() instanceof Application));
            return true;
        }
        @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
        @Override public String getType(Uri u) { return null; }
        @Override public Uri insert(Uri u, ContentValues v) { return null; }
        @Override public int delete(Uri u, String s, String[] a) { return 0; }
        @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
    }

    public static final class TestRegularProvider extends ContentProvider {
        @Override
        public boolean onCreate() {
            INIT_EVENTS.add("regular_provider:" + Process.myProcessName());
            return true;
        }
        @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
        @Override public String getType(Uri u) { return null; }
        @Override public Uri insert(Uri u, ContentValues v) { return null; }
        @Override public int delete(Uri u, String s, String[] a) { return 0; }
        @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
    }

    public static final class TestEarlyCheckApplication extends Application {
        @Override
        protected void attachBaseContext(Context base) {
            super.attachBaseContext(base);
            INIT_EVENTS.add("attachBaseContext:" + Process.myProcessName()
                    + ":firebaseReady=" + INIT_EVENTS.stream().anyMatch(e -> e.startsWith("firebase_provider:")));
        }

        @Override
        public void onCreate() {
            super.onCreate();
            INIT_EVENTS.add("onCreate:" + Process.myProcessName());
        }
    }

    @Test
    void earlyStartupProcessNameAndProcFsBridge(@TempDir Path tmpDir) throws Exception {
        String prevPkg = ActivityThread.currentPackageName();
        String prevProc = ActivityThread.currentProcessName();
        try {
            String testProc = "com.aaow.sprint12.earlyproc";
            ActivityThread.setPackageName(testProc);
            ActivityThread.setProcessName(testProc);
            LinuxProcFsBridge.configure(testProc, tmpDir);

            assertEquals(testProc, Process.myProcessName());
            assertEquals(testProc, ActivityThread.currentProcessName());
            assertEquals(testProc, Application.getProcessName());

            // Kiểm tra chuyển hướng trực tiếp đường dẫn /proc/... trên Windows
            String remappedPid = LinuxProcFsBridge.remapPath("/proc/" + Process.myPid() + "/cmdline");
            assertNotEquals("/proc/" + Process.myPid() + "/cmdline", remappedPid);
            assertTrue(Files.isRegularFile(Path.of(remappedPid)));
            assertEquals(testProc, Files.readString(Path.of(remappedPid), StandardCharsets.UTF_8).replace("\0", ""));

            String remappedSelf = LinuxProcFsBridge.remapPath("/proc/self/cmdline");
            assertEquals(remappedPid, remappedSelf);

            String remappedWinDrive = LinuxProcFsBridge.remapPath("D:\\proc\\9988\\cmdline");
            assertEquals(remappedPid, remappedWinDrive);

            File remappedCpu = LinuxProcFsBridge.remapFile(new File("/proc/cpuinfo"));
            assertTrue(remappedCpu.isFile());
            assertTrue(Files.readString(remappedCpu.toPath()).contains("AAOW Virtual SoC"));

            File remappedMem = LinuxProcFsBridge.remapFile(new File("/proc/meminfo"));
            assertTrue(remappedMem.isFile());
            assertTrue(Files.readString(remappedMem.toPath()).contains("MemTotal:"));

            // Đường dẫn thông thường không bị sửa đổi
            assertEquals("C:\\Windows\\System32\\notepad.exe",
                    LinuxProcFsBridge.remapPath("C:\\Windows\\System32\\notepad.exe"));

            // Kiểm tra ASM rewrite trong DexLoader: FileReader(String) và FileInputStream(File) tự đọc được /proc/<pid>/cmdline
            byte[] rawClass = generateProcCmdlineReaderClass("dev/aaow/test/ProcReaderHelper");
            byte[] rewritten = DexLoader.rewriteApkClassForWindows(rawClass);
            Class<?> helperCls = new ClassLoader(Sprint12FrameworkTest.class.getClassLoader()) {
                Class<?> loadGenerated() {
                    return defineClass("dev.aaow.test.ProcReaderHelper", rewritten, 0, rewritten.length);
                }
            }.loadGenerated();

            String viaFileReader = (String) helperCls.getMethod("readViaFileReader").invoke(null);
            String viaFileInputStream = (String) helperCls.getMethod("readViaFileInputStream").invoke(null);
            assertEquals(testProc, viaFileReader.replace("\0", "").trim());
            assertEquals(testProc, viaFileInputStream.replace("\0", "").trim());
        } finally {
            ActivityThread.setPackageName(prevPkg);
            ActivityThread.setProcessName(prevProc);
        }
    }

    @Test
    void earlyFirebaseInitProviderRunsBeforeApplicationAttachBaseContext(@TempDir Path tmpDir) throws Exception {
        Path showcaseApk = resolveRepoFile("samples/aaow-showcase.apk");
        Assumptions.assumeTrue(Files.isRegularFile(showcaseApk));

        INIT_EVENTS.clear();
        String prevPkg = ActivityThread.currentPackageName();
        String prevProc = ActivityThread.currentProcessName();
        try (ApkFile apk = new ApkFile(showcaseApk)) {
            AndroidManifest mf = apk.getManifest();
            String pkg = mf.packageName;
            mf.applicationClass = TestEarlyCheckApplication.class.getName();

            AndroidManifest.Provider firebaseDecl = new AndroidManifest.Provider();
            firebaseDecl.name = TestEarlyFirebaseInitProvider.class.getName();
            firebaseDecl.authorities = pkg + ".firebaseinitprovider";
            firebaseDecl.enabled = true;
            firebaseDecl.initOrder = 100;

            AndroidManifest.Provider regularDecl = new AndroidManifest.Provider();
            regularDecl.name = TestRegularProvider.class.getName();
            regularDecl.authorities = pkg + ".regularprovider";
            regularDecl.enabled = true;
            regularDecl.initOrder = 50;

            mf.providers.add(firebaseDecl);
            mf.providers.add(regularDecl);

            AndroidRuntimeContext ctx = new AndroidRuntimeContext(
                    apk, Sprint12FrameworkTest.class.getClassLoader(), tmpDir);
            Application app = AppRepository.createAndInitializeApplication(
                    mf, Sprint12FrameworkTest.class.getClassLoader(), ctx, msg -> {});

            assertInstanceOf(TestEarlyCheckApplication.class, app);
            assertSame(app, ctx.getApplicationContext());
            assertEquals(List.of(
                    "firebase_provider:" + pkg + ":appCtx=true",
                    "attachBaseContext:" + pkg + ":firebaseReady=true",
                    "regular_provider:" + pkg,
                    "onCreate:" + pkg
            ), INIT_EVENTS);
        } finally {
            ActivityThread.setPackageName(prevPkg);
            ActivityThread.setProcessName(prevProc);
        }
    }

    @Test
    void realX509CertificateExtractionAndSigningInfo(@TempDir Path tmpDir) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // 1. Chứng chỉ tự ký X.509 v3 DER chuẩn RFC 5280 cho microG-lite & fallback
        Signature gmsSig = MicroGServiceRegistry.getGmsSignature();
        assertNotNull(gmsSig);
        X509Certificate gmsCert = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(gmsSig.toByteArray()));
        assertTrue(gmsCert.getSubjectX500Principal().getName().contains("CN=AAOW MicroG Services"));
        assertNotNull(gmsCert.getPublicKey());

        // 2. Tạo file APK tổng hợp chứa APK Signature Scheme v2 Signing Block thật ("APK Sig Block 42")
        byte[] syntheticCertDer = ApkSignatureExtractor.buildSelfSignedX509Der("AAOW Test V2 Signer", "AAOW Test");
        Path v2ApkPath = tmpDir.resolve("synthetic-v2.apk");
        writeSyntheticV2SignedApk(v2ApkPath, syntheticCertDer);

        try (ZipFile zip = new ZipFile(v2ApkPath.toFile())) {
            ApkSignatureExtractor.SigningResult res = ApkSignatureExtractor.extractFromApk(v2ApkPath, zip);
            assertEquals(ApkSignatureExtractor.SCHEME_APK_V2, res.schemeVersion());
            assertEquals(1, res.certificates().size());
            assertArrayEquals(syntheticCertDer, res.certificates().get(0));

            X509Certificate parsed = (X509Certificate) cf.generateCertificate(
                    new ByteArrayInputStream(res.certificates().get(0)));
            assertTrue(parsed.getSubjectX500Principal().getName().contains("CN=AAOW Test V2 Signer"));
        }

        // 3. Kiểm thử PackageManager + SigningInfo + hasSigningCertificate với APK mẫu chưa ký
        Path showcaseApk = resolveRepoFile("samples/aaow-showcase.apk");
        Assumptions.assumeTrue(Files.isRegularFile(showcaseApk));
        try (ApkFile apk = new ApkFile(showcaseApk)) {
            AndroidManifest mf = apk.getManifest();
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(
                    apk, Sprint12FrameworkTest.class.getClassLoader(), tmpDir.resolve("showcase"));
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(mf.packageName,
                    PackageManager.GET_SIGNATURES | PackageManager.GET_SIGNING_CERTIFICATES);

            assertNotNull(pi.signatures);
            assertTrue(pi.signatures.length >= 1);
            assertNotNull(pi.signingInfo);
            assertFalse(pi.signingInfo.hasMultipleSigners());
            assertArrayEquals(pi.signatures[0].toByteArray(), pi.signingInfo.getApkContentsSigners()[0].toByteArray());

            // Chứng chỉ giải mã trơn tru bằng CertificateFactory (không CertificateException)
            X509Certificate appCert = (X509Certificate) cf.generateCertificate(
                    new ByteArrayInputStream(pi.signatures[0].toByteArray()));
            assertNotNull(appCert.getPublicKey());
            Collection<PublicKey> keys = pi.signingInfo.getPublicKeys();
            assertEquals(1, keys.size());

            // Kiểm tra hasSigningCertificate theo cả RAW_X509 và SHA256
            byte[] rawCert = pi.signatures[0].toByteArray();
            byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(rawCert);
            assertTrue(pm.hasSigningCertificate(mf.packageName, rawCert, PackageManager.CERT_INPUT_RAW_X509));
            assertTrue(pm.hasSigningCertificate(mf.packageName, sha256, PackageManager.CERT_INPUT_SHA256));
            assertFalse(pm.hasSigningCertificate(mf.packageName, new byte[]{1, 2, 3}, PackageManager.CERT_INPUT_SHA256));

            // Kiểm tra SigningInfo Parcelable roundtrip
            Parcel p = Parcel.obtain();
            try {
                pi.signingInfo.writeToParcel(p, 0);
                p.setDataPosition(0);
                SigningInfo restored = SigningInfo.CREATOR.createFromParcel(p);
                assertTrue(pi.signingInfo.signersMatchExactly(restored));
                assertEquals(pi.signingInfo.getSchemeVersion(), restored.getSchemeVersion());
            } finally {
                p.recycle();
            }

            // Gói ảo com.google.android.gms & com.android.vending trả chứng chỉ X.509 DER chuẩn
            PackageInfo gmsPi = pm.getPackageInfo(MicroGServiceRegistry.GMS_PACKAGE,
                    PackageManager.GET_SIGNATURES | PackageManager.GET_SIGNING_CERTIFICATES);
            assertNotNull(gmsPi.signingInfo);
            assertEquals(SigningInfo.VERSION_SIGNING_BLOCK_V2, gmsPi.signingInfo.getSchemeVersion());
            X509Certificate parsedGms = (X509Certificate) cf.generateCertificate(
                    new ByteArrayInputStream(gmsPi.signatures[0].toByteArray()));
            assertEquals(gmsCert.getSerialNumber(), parsedGms.getSerialNumber());

            // TelephonyManager cũng sẵn sàng trên AndroidRuntimeContext
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            assertNotNull(tm);
            assertEquals(TelephonyManager.PHONE_TYPE_NONE, tm.getPhoneType());
            assertNotNull(tm.getSimOperator());
        }
    }

    @Test
    void parcelFourByteAlignmentAndSafeParcelRoundtrip() {
        Parcel p = Parcel.obtain();
        try {
            IBinder tokenBinder = new Binder("com.example.Callbacks");
            p.writeInterfaceToken(MicroGServiceRegistry.DESC_GMS_BROKER); // slot 0 (pos 0 -> 4)
            p.writeStrongBinder(tokenBinder);                             // slot 1 (pos 4 -> 8)
            p.writeInt(1);                                                // slot 2 (pos 8 -> 12)
            assertEquals(12, p.dataPosition());

            // Mô phỏng SafeParcelWriter.beginObjectHeader -> write fields -> finishObjectHeader (setDataPosition(start - 4))
            p.writeInt(0xFFFF4F45);                                       // slot 3 (pos 12 -> 16)
            p.writeInt(0);                                                // slot 4 placeholder (pos 16 -> 20)
            int objectStart = p.dataPosition();
            assertEquals(20, objectStart);

            p.writeInt(42);                                               // +4 bytes -> 24
            p.writeLong(0x1122334455667788L);                             // +8 bytes -> 32
            p.writeDouble(3.14159);                                       // +8 bytes -> 40
            p.writeString("safe-parcel-ok");                              // +4 bytes -> 44
            int objectEnd = p.dataPosition();
            assertEquals(44, objectEnd);

            // Backpatch độ dài tại objectStart - 4 (slot 4) — KHÔNG ghi đè slot 1 (tokenBinder)!
            p.setDataPosition(objectStart - 4);
            p.writeInt(objectEnd - objectStart);
            p.setDataPosition(objectEnd);
            assertEquals(44, p.dataSize());

            // Đọc lại từ đầu: tokenBinder còn nguyên vẹn, độ dài SafeParcel khớp đúng 24 byte
            p.setDataPosition(0);
            assertEquals(44, p.dataAvail());
            p.enforceInterface(MicroGServiceRegistry.DESC_GMS_BROKER);
            assertSame(tokenBinder, p.readStrongBinder());
            assertEquals(1, p.readInt());
            assertEquals(0xFFFF4F45, p.readInt());
            assertEquals(24, p.readInt());
            assertEquals(42, p.readInt());
            assertEquals(0x1122334455667788L, p.readLong());
            assertEquals(3.14159, p.readDouble(), 1e-9);
            assertEquals("safe-parcel-ok", p.readString());
            assertEquals(0, p.dataAvail());

            // Kiểm tra marshall() / unmarshall() giữ nguyên các giá trị Serialized & căn chỉnh 8-byte
            Parcel p2 = Parcel.obtain();
            try {
                p2.writeInt(7);
                p2.writeLong(9876543210123L);
                p2.writeString("marshalled");
                byte[] bytes = p2.marshall();

                Parcel p3 = Parcel.obtain();
                try {
                    p3.unmarshall(bytes, 0, bytes.length);
                    assertEquals(16, p3.dataSize()); // 4 + 8 + 4
                    assertEquals(7, p3.readInt());
                    assertEquals(9876543210123L, p3.readLong());
                    assertEquals("marshalled", p3.readString());
                } finally {
                    p3.recycle();
                }
            } finally {
                p2.recycle();
            }
        } finally {
            p.recycle();
        }
    }

    @Test
    void microGLiteAidlBindersAndGmsServiceBrokerHandshake() throws Exception {
        // 1. IAdvertisingIdService (getId, isLimitAdTrackingEnabled, isAdIdFakeForDebugLogging)
        AtomicReference<IBinder> adBinderRef = new AtomicReference<>();
        Intent adIntent = new Intent(MicroGServiceRegistry.ACTION_ADS_IDENTIFIER)
                .setPackage(MicroGServiceRegistry.GMS_PACKAGE);
        assertTrue(MicroGServiceRegistry.bindVirtualService(adIntent, null, new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder service) { adBinderRef.set(service); }
            @Override public void onServiceDisconnected(ComponentName name) {}
        }));
        IBinder adBinder = adBinderRef.get();
        assertNotNull(adBinder);
        assertEquals(MicroGServiceRegistry.DESC_ADVERTISING_ID, adBinder.getInterfaceDescriptor());

        Parcel d1 = Parcel.obtain(), r1 = Parcel.obtain();
        try {
            d1.writeInterfaceToken(MicroGServiceRegistry.DESC_ADVERTISING_ID);
            assertTrue(adBinder.transact(1, d1, r1, 0));
            r1.readException();
            assertEquals(MicroGServiceRegistry.getAdvertisingId(), r1.readString());
        } finally {
            d1.recycle();
            r1.recycle();
        }

        Parcel d2 = Parcel.obtain(), r2 = Parcel.obtain();
        try {
            d2.writeInterfaceToken(MicroGServiceRegistry.DESC_ADVERTISING_ID);
            d2.writeInt(1);
            assertTrue(adBinder.transact(2, d2, r2, 0));
            r2.readException();
            assertEquals(0, r2.readInt());
        } finally {
            d2.recycle();
            r2.recycle();
        }

        // 2. IMeasurementService qua IGmsServiceBroker.getService (code = 46) với SafeParcel backpatch
        AtomicReference<IBinder> measBinderRef = new AtomicReference<>();
        Intent measIntent = new Intent(MicroGServiceRegistry.ACTION_MEASUREMENT)
                .setPackage(MicroGServiceRegistry.GMS_PACKAGE);
        assertTrue(MicroGServiceRegistry.bindVirtualService(measIntent, null, new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder service) { measBinderRef.set(service); }
            @Override public void onServiceDisconnected(ComponentName name) {}
        }));
        IBinder measBinder = measBinderRef.get();
        assertNotNull(measBinder);
        assertEquals(MicroGServiceRegistry.DESC_MEASUREMENT, measBinder.getInterfaceDescriptor());

        AtomicInteger callbackStatus = new AtomicInteger(-1);
        AtomicReference<IBinder> callbackServiceBinder = new AtomicReference<>();
        Binder fakeGmsCallbacks = new Binder(MicroGServiceRegistry.DESC_GMS_CALLBACKS) {
            @Override
            protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                if (code == MicroGServiceRegistry.CALLBACK_ON_POST_INIT_COMPLETE) {
                    data.enforceInterface(MicroGServiceRegistry.DESC_GMS_CALLBACKS);
                    callbackStatus.set(data.readInt());
                    callbackServiceBinder.set(data.readStrongBinder());
                    int hasBundle = data.readInt();
                    assertEquals(0, hasBundle);
                    assertEquals(0, data.dataAvail());
                    if (reply != null) reply.writeNoException();
                    return true;
                }
                return super.onTransact(code, data, reply, flags);
            }
        };

        Parcel brokerData = Parcel.obtain(), brokerReply = Parcel.obtain();
        try {
            brokerData.writeInterfaceToken(MicroGServiceRegistry.DESC_GMS_BROKER);
            brokerData.writeStrongBinder(fakeGmsCallbacks);
            brokerData.writeInt(1);
            // SafeParcel GetServiceRequest header + backpatch
            brokerData.writeInt(0xFFFF4F45);
            brokerData.writeInt(0);
            int start = brokerData.dataPosition();
            brokerData.writeInt(6); // version
            brokerData.writeString("com.aaow.test");
            int end = brokerData.dataPosition();
            brokerData.setDataPosition(start - 4);
            brokerData.writeInt(end - start);
            brokerData.setDataPosition(end);

            assertTrue(measBinder.transact(MicroGServiceRegistry.TRANSACTION_GET_SERVICE, brokerData, brokerReply, 0));
            brokerReply.readException();
        } finally {
            brokerData.recycle();
            brokerReply.recycle();
        }
        assertEquals(0, callbackStatus.get());
        assertNotNull(callbackServiceBinder.get());
        assertEquals(MicroGServiceRegistry.DESC_MEASUREMENT, callbackServiceBinder.get().getInterfaceDescriptor());

        // Gọi trực tiếp code = 11 (getAppInstanceId) trên IMeasurementService
        Parcel md = Parcel.obtain(), mr = Parcel.obtain();
        try {
            md.writeInterfaceToken(MicroGServiceRegistry.DESC_MEASUREMENT);
            assertTrue(callbackServiceBinder.get().transact(11, md, mr, 0));
            mr.readException();
            assertEquals(MicroGServiceRegistry.getAppInstanceId(), mr.readString());
        } finally {
            md.recycle();
            mr.recycle();
        }

        // 3. IAnalyticsService (code = 3 getVersion)
        IBinder analyticsBinder = MicroGServiceRegistry.createBinderForAction(MicroGServiceRegistry.ACTION_ANALYTICS);
        assertEquals(MicroGServiceRegistry.DESC_ANALYTICS, analyticsBinder.getInterfaceDescriptor());
        Parcel ad = Parcel.obtain(), ar = Parcel.obtain();
        try {
            ad.writeInterfaceToken(MicroGServiceRegistry.DESC_ANALYTICS);
            assertTrue(analyticsBinder.transact(3, ad, ar, 0));
            ar.readException();
            assertTrue(ar.readString().contains("aaow"));
        } finally {
            ad.recycle();
            ar.recycle();
        }

        // 4. GooglePlayServicesUtilLight.isGooglePlayServicesAvailable ASM bridge
        byte[] rawGmsUtil = generateStubGmsUtilLightClass();
        byte[] rewrittenGmsUtil = DexLoader.rewriteApkClassForWindows(rawGmsUtil);
        Class<?> gmsUtilCls = new ClassLoader(Sprint12FrameworkTest.class.getClassLoader()) {
            Class<?> loadGenerated() {
                return defineClass("com.google.android.gms.common.GooglePlayServicesUtilLight",
                        rewrittenGmsUtil, 0, rewrittenGmsUtil.length);
            }
        }.loadGenerated();
        Method availMethod = gmsUtilCls.getMethod("isGooglePlayServicesAvailable", Context.class, int.class);
        assertEquals(0, availMethod.invoke(null, null, 12451000));
        assertEquals(2, availMethod.invoke(null, null, Integer.MAX_VALUE));

        // 5. Messenger (Handler + Parcelable roundtrip)
        AtomicInteger receivedWhat = new AtomicInteger();
        CountDownLatch msgLatch = new CountDownLatch(1);
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                receivedWhat.set(msg.what);
                msgLatch.countDown();
            }
        };
        Messenger messenger = new Messenger(handler);
        Parcel mp = Parcel.obtain();
        try {
            messenger.writeToParcel(mp, 0);
            mp.setDataPosition(0);
            Messenger restored = Messenger.CREATOR.createFromParcel(mp);
            assertEquals(messenger, restored);
            restored.send(Message.obtain(null, 777));
        } finally {
            mp.recycle();
        }
        assertTrue(msgLatch.await(3, TimeUnit.SECONDS));
        assertEquals(777, receivedWhat.get());
    }

    @Test
    void fdroidSignedApkRealX509Extraction() throws Exception {
        List<String> candidates = List.of(
                "build/aaow-data/installed-apps/cz.martykan.forecastie.apk",
                "build/aaow-data/installed-apps/org.billthefarmer.currency.apk",
                "build/aaow-data/installed-apps/org.woheller69.omweather.apk"
        );
        List<Path> existing = candidates.stream()
                .map(Sprint12FrameworkTest::resolveRepoFile)
                .filter(Files::isRegularFile)
                .toList();
        Assumptions.assumeFalse(existing.isEmpty(), "Bỏ qua nếu chưa cài bộ APK F-Droid vào kho");

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        for (Path apkPath : existing) {
            try (ApkFile apk = new ApkFile(apkPath)) {
                ApkSignatureExtractor.SigningResult res = apk.getSigningResult();
                assertTrue(res.hasCertificates(), apkPath.getFileName() + " phải trích xuất được chứng chỉ X.509 thật");
                assertTrue(res.schemeVersion() >= ApkSignatureExtractor.SCHEME_JAR_V1);
                X509Certificate cert = (X509Certificate) cf.generateCertificate(
                        new ByteArrayInputStream(res.certificates().get(0)));
                assertNotNull(cert.getSubjectX500Principal());
                assertNotNull(cert.getPublicKey());
            }
        }
    }

    @Test
    void apkpureRealApkEarlyFirebaseRealX509AndMicroGHandshake() throws Exception {
        Path apkpureApk = resolveRepoFile("build/aaow-data/installed-apps/com.apkpure.aegon.apk");
        Assumptions.assumeTrue(Files.isRegularFile(apkpureApk), "Bỏ qua nếu chưa có com.apkpure.aegon.apk");

        Path runDataDir = resolveRepoFile("build").resolve("tmp/sprint12-apkpure-test-" + System.nanoTime());
        Files.createDirectories(runDataDir);

        String prevPkg = ActivityThread.currentPackageName();
        String prevProc = ActivityThread.currentProcessName();
        Application prevApp = ActivityThread.currentApplication();
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        try (ApkFile apk = new ApkFile(apkpureApk)) {
            // 1. Chứng chỉ X.509 thật từ META-INF/APKPURE.RSA
            ApkSignatureExtractor.SigningResult sigRes = apk.getSigningResult();
            assertTrue(sigRes.hasCertificates());
            assertEquals(ApkSignatureExtractor.SCHEME_JAR_V1, sigRes.schemeVersion());
            X509Certificate realCert = (X509Certificate) cf.generateCertificate(
                    new ByteArrayInputStream(sigRes.certificates().get(0)));
            assertTrue(realCert.getSubjectX500Principal().getName().toLowerCase().contains("apkpure"),
                    "Chứng chỉ thật phải thuộc APKPure: " + realCert.getSubjectX500Principal());

            ClassLoader appCl = DexLoader.loadApkDex(
                    apk, resolveRepoFile("build/aaow-data/dex-cache"), Sprint12FrameworkTest.class.getClassLoader());
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, appCl, runDataDir);

            // 2. PackageManager trả đúng chứng chỉ thật & SigningInfo
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo("com.apkpure.aegon",
                    PackageManager.GET_SIGNATURES | PackageManager.GET_SIGNING_CERTIFICATES);
            assertNotNull(pi.signatures);
            assertArrayEquals(sigRes.certificates().get(0), pi.signatures[0].toByteArray());
            assertNotNull(pi.signingInfo);
            assertEquals(SigningInfo.VERSION_JAR, pi.signingInfo.getSchemeVersion());
            byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(sigRes.certificates().get(0));
            assertTrue(pm.hasSigningCertificate("com.apkpure.aegon", sha256, PackageManager.CERT_INPUT_SHA256));

            // 3. Khởi động sớm FirebaseInitProvider (không gọi APKPure.onCreate để không rò luồng nền giữa các test)
            Application hostApp = new Application();
            hostApp.preAttachBaseContext(ctx);
            ctx.setApplicationContext(hostApp);
            ctx.installEarlyFirebaseProviders();

            assertEquals("com.apkpure.aegon", Process.myProcessName());
            Class<?> firebaseAppCls = appCl.loadClass("ge.qdae");
            Object defaultFirebaseApp = firebaseAppCls.getMethod("c").invoke(null);
            assertNotNull(defaultFirebaseApp, "FirebaseApp mặc định phải được khởi tạo sớm từ FirebaseInitProvider");
            assertEquals("[DEFAULT]", firebaseAppCls.getField("b").get(defaultFirebaseApp));

            // ProcessUtils.getMyProcessName() đọc /proc/<pid>/cmdline qua LinuxProcFsBridge
            Class<?> procUtilsCls = appCl.loadClass("com.google.android.gms.common.util.ProcessUtils");
            String gmsProcName = (String) procUtilsCls.getMethod("getMyProcessName").invoke(null);
            assertEquals("com.apkpure.aegon", gmsProcName);

            // GoogleApiAvailabilityLight.isGooglePlayServicesAvailable trả về 0 (SUCCESS)
            Class<?> gmsAvailCls = appCl.loadClass("com.google.android.gms.common.GoogleApiAvailabilityLight");
            Object gmsAvailInst = gmsAvailCls.getMethod("getInstance").invoke(null);
            int gmsStatus = (Integer) gmsAvailCls.getMethod("isGooglePlayServicesAvailable", Context.class)
                    .invoke(gmsAvailInst, ctx);
            assertEquals(0, gmsStatus);

            // AdvertisingIdClient.getAdvertisingIdInfo(ctx) hoạt động thật qua IAdvertisingIdService (không IOException!)
            ExecutorService pool = Executors.newSingleThreadExecutor();
            try {
                Future<String> adIdFuture = pool.submit(() -> {
                    Class<?> adClientCls = appCl.loadClass("com.google.android.gms.ads.identifier.AdvertisingIdClient");
                    Object info = adClientCls.getMethod("getAdvertisingIdInfo", Context.class).invoke(null, ctx);
                    assertNotNull(info);
                    Boolean limited = (Boolean) info.getClass().getMethod("isLimitAdTrackingEnabled").invoke(info);
                    assertFalse(limited);
                    return (String) info.getClass().getMethod("getId").invoke(info);
                });
                assertEquals(MicroGServiceRegistry.getAdvertisingId(), adIdFuture.get(10, TimeUnit.SECONDS));
            } finally {
                pool.shutdownNow();
            }
        } finally {
            ActivityThread.setPackageName(prevPkg);
            ActivityThread.setProcessName(prevProc);
            ActivityThread.setCurrentApplication(prevApp);
        }
    }

    // ===== Helpers tạo bytecode & synthetic APK v2 cho unit test =====

    private static byte[] generateProcCmdlineReaderClass(String internalName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);

        // readViaFileReader(): new BufferedReader(new FileReader("/proc/1234/cmdline")).readLine()
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "readViaFileReader", "()Ljava/lang/String;", null, new String[]{"java/lang/Exception"});
        mv.visitCode();
        mv.visitTypeInsn(Opcodes.NEW, "java/io/BufferedReader");
        mv.visitInsn(Opcodes.DUP);
        mv.visitTypeInsn(Opcodes.NEW, "java/io/FileReader");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("/proc/1234/cmdline");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/io/FileReader", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/io/BufferedReader", "<init>", "(Ljava/io/Reader;)V", false);
        mv.visitVarInsn(Opcodes.ASTORE, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/BufferedReader", "readLine", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/BufferedReader", "close", "()V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // readViaFileInputStream(): new String(new FileInputStream(new File("/proc/self/cmdline")).readAllBytes())
        MethodVisitor mv2 = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "readViaFileInputStream", "()Ljava/lang/String;", null, new String[]{"java/lang/Exception"});
        mv2.visitCode();
        mv2.visitTypeInsn(Opcodes.NEW, "java/io/FileInputStream");
        mv2.visitInsn(Opcodes.DUP);
        mv2.visitTypeInsn(Opcodes.NEW, "java/io/File");
        mv2.visitInsn(Opcodes.DUP);
        mv2.visitLdcInsn("/proc/self/cmdline");
        mv2.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V", false);
        mv2.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/io/FileInputStream", "<init>", "(Ljava/io/File;)V", false);
        mv2.visitVarInsn(Opcodes.ASTORE, 0);
        mv2.visitVarInsn(Opcodes.ALOAD, 0);
        mv2.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/FileInputStream", "readAllBytes", "()[B", false);
        mv2.visitVarInsn(Opcodes.ASTORE, 1);
        mv2.visitVarInsn(Opcodes.ALOAD, 0);
        mv2.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/FileInputStream", "close", "()V", false);
        mv2.visitTypeInsn(Opcodes.NEW, "java/lang/String");
        mv2.visitInsn(Opcodes.DUP);
        mv2.visitVarInsn(Opcodes.ALOAD, 1);
        mv2.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false);
        mv2.visitInsn(Opcodes.ARETURN);
        mv2.visitMaxs(0, 0);
        mv2.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] generateStubGmsUtilLightClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC,
                "com/google/android/gms/common/GooglePlayServicesUtilLight", null, "java/lang/Object", null);
        // Gốc trả 9 (SERVICE_INVALID), nhưng DexLoader phải chuyển hướng về MicroGServiceRegistry
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "isGooglePlayServicesAvailable", "(Landroid/content/Context;I)I", null, null);
        mv.visitCode();
        mv.visitIntInsn(Opcodes.BIPUSH, 9);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** Tạo file ZIP hợp lệ có chèn APK Signing Block v2 (ID 0x7109871a) ngay trước Central Directory. */
    private static void writeSyntheticV2SignedApk(Path outFile, byte[] certDer) throws Exception {
        ByteArrayOutputStream zipBuf = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipBuf)) {
            zos.putNextEntry(new ZipEntry("assets/hello.txt"));
            zos.write("aaow".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        byte[] rawZip = zipBuf.toByteArray();

        // Tìm EOCD và offset của Central Directory
        int eocdPos = -1;
        for (int i = rawZip.length - 22; i >= 0; i--) {
            if ((rawZip[i] & 0xFF) == 0x50 && (rawZip[i + 1] & 0xFF) == 0x4b
                    && (rawZip[i + 2] & 0xFF) == 0x05 && (rawZip[i + 3] & 0xFF) == 0x06) {
                eocdPos = i;
                break;
            }
        }
        assertTrue(eocdPos > 0);
        int cdOffset = ByteBuffer.wrap(rawZip, eocdPos, 22).slice().order(ByteOrder.LITTLE_ENDIAN).getInt(16);

        // Xây dựng APK Sig Scheme v2 payload: signers -> signer -> signedData -> digests(empty) + certificates(certDer)
        byte[] certEntry = lengthPrefixedLe(certDer);
        byte[] certsList = lengthPrefixedLe(certEntry);
        byte[] emptyDigests = lengthPrefixedLe(new byte[0]);
        byte[] signedDataBody = concat(emptyDigests, certsList);
        byte[] signerBody = lengthPrefixedLe(signedDataBody);
        byte[] signersBlock = lengthPrefixedLe(lengthPrefixedLe(signerBody));

        // Pair: uint64(4 + value.length) | uint32(0x7109871a) | value
        ByteBuffer pairBb = ByteBuffer.allocate(8 + 4 + signersBlock.length).order(ByteOrder.LITTLE_ENDIAN);
        pairBb.putLong(4L + signersBlock.length);
        pairBb.putInt(0x7109871a);
        pairBb.put(signersBlock);
        byte[] pairBytes = pairBb.array();

        long sizeOfBlock = pairBytes.length + 8L + 16L; // pairs + size2 + magic
        ByteBuffer sigBlock = ByteBuffer.allocate(8 + (int) sizeOfBlock).order(ByteOrder.LITTLE_ENDIAN);
        sigBlock.putLong(sizeOfBlock);
        sigBlock.put(pairBytes);
        sigBlock.putLong(sizeOfBlock);
        sigBlock.put("APK Sig Block 42".getBytes(StandardCharsets.US_ASCII));
        byte[] sigBlockBytes = sigBlock.array();

        // Cập nhật cdOffset trong EOCD mới
        byte[] newTail = new byte[rawZip.length - cdOffset];
        System.arraycopy(rawZip, cdOffset, newTail, 0, newTail.length);
        int newEocdPos = eocdPos - cdOffset;
        ByteBuffer.wrap(newTail, newEocdPos, 22).slice().order(ByteOrder.LITTLE_ENDIAN)
                .putInt(16, cdOffset + sigBlockBytes.length);

        try (DataOutputStream dos = new DataOutputStream(Files.newOutputStream(outFile))) {
            dos.write(rawZip, 0, cdOffset);
            dos.write(sigBlockBytes);
            dos.write(newTail);
        }
    }

    private static Path resolveRepoFile(String relativePath) {
        Path direct = Path.of(relativePath);
        if (Files.exists(direct)) return direct;
        Path parent = Path.of("..", relativePath);
        if (Files.exists(parent)) return parent;
        return direct;
    }

    private static byte[] lengthPrefixedLe(byte[] data) {
        ByteBuffer bb = ByteBuffer.allocate(4 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(data.length);
        bb.put(data);
        return bb.array();
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
