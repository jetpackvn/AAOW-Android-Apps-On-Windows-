package dev.aaow.launcher;

import android.app.ActivityManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.Looper;
import android.os.MessageQueue;
import android.os.Parcel;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.accessibility.AccessibilityNodeInfo;
import android.webkit.WebSettings;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;
import dev.aaow.apk.ApkFile;
import dev.aaow.apk.ResourceTable;
import dev.aaow.rt.AndroidRuntimeContext;
import dev.aaow.rt.NativeBridge;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.crypto.Cipher;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị và E2E cho Sprint 13 (Phase D2 — Thực chiến Ứng dụng Thương mại
 * & Mở rộng Native JNI HLE):
 * 1. Các API hệ thống cốt lõi (Build, Intent/IntentFilter, ConnectivityManager.getActiveLinkProperties,
 *    PowerManager, MessageQueue/Looper.myQueue, Uri.fromParts, TextUtils, WebSettings, PendingIntent/IntentSender,
 *    ActivityManager.RunningServiceInfo, Parcel arrays/lists/maps).
 * 2. Các API UI/Drawable/Context mở rộng cho AppCompat, Material & Glide (Context.registerComponentCallbacks,
 *    GradientDrawable, ClipDrawable, Bitmap.prepareToDraw, Paint.setFilterBitmap, RelativeLayout.setGravity,
 *    ViewFlipper/ViewAnimator, TextView typography, View tags/visibility, ProgressBar, Window, AccessibilityNodeInfo).
 * 3. Trường reflection mNativePtr / native_instance trên các lớp đồ họa & Parcel.
 * 4. Cầu nối JNI HLE QseaAndQmspJniHandler (libqsealib.so RSA-2048 X.509 & AES-CBC, libqmp.so).
 * 5. Chuyển tiếp tham số constructor lên super(...) trong AndroidSdkFallbackClassLoader.
 * 6. Chống đụng độ khóa attr làm rối kiểu AndResGuard ('arg'), obtainTypedArray & giải mã WebP thật.
 * 7. E2E APKPure (com.apkpure.aegon.apk) vượt SplashActivity vào MainTabActivity và vẽ giao diện thật.
 */
class Sprint13FrameworkTest {

    @Test
    void coreSystemAndIntentFilterApis(@TempDir Path tmpDir) throws Exception {
        // 1. Build fields & methods
        assertFalse(Build.BOOTLOADER.isEmpty());
        assertFalse(Build.SERIAL.isEmpty());
        assertFalse(Build.RADIO.isEmpty());
        assertFalse(Build.HOST.isEmpty());
        assertFalse(Build.USER.isEmpty());
        assertFalse(Build.SOC_MANUFACTURER.isEmpty());
        assertFalse(Build.SKU.isEmpty());
        assertFalse(Build.VERSION.SECURITY_PATCH.isEmpty());
        assertEquals(Build.RADIO, Build.getRadioVersion());
        assertEquals(Build.SERIAL, Build.getSerial());
        assertTrue(Build.TIME > 0L);
        assertEquals(35, Build.VERSION_CODES.VANILLA_ICE_CREAM);

        // 2. Intent & IntentFilter.match + AuthorityEntry
        Uri sampleUri = Uri.parse("https://apkpure.com:8443/app/demo");
        Intent it = new Intent(Intent.ACTION_VIEW, sampleUri)
                .setType("application/vnd.android.package-archive")
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // setType clears data per Android contract, so re-set with setDataAndType
        it.setDataAndType(sampleUri, "application/vnd.android.package-archive");
        assertEquals("https", it.getScheme());
        assertEquals("application/vnd.android.package-archive", it.resolveTypeIfNeeded(null));
        assertEquals("application/vnd.android.package-archive", it.resolveType((android.content.ContentResolver) null));

        Intent cloned = it.cloneFilter();
        cloned.putExtra("transient", 123);
        assertTrue(it.filterEquals(cloned));
        assertEquals(it.filterHashCode(), cloned.filterHashCode());

        cloned.removeCategory(Intent.CATEGORY_BROWSABLE);
        assertFalse(cloned.hasCategory(Intent.CATEGORY_BROWSABLE));
        it.removeFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, it.getFlags());
        Bundle b = new Bundle();
        b.putString("k", "v");
        it.replaceExtras(b);
        assertEquals("v", it.getStringExtra("k"));

        IntentFilter filter = new IntentFilter(Intent.ACTION_VIEW, "application/*");
        filter.addCategory(Intent.CATEGORY_BROWSABLE);
        filter.addDataScheme("https");
        filter.addDataAuthority("apkpure.com", "8443");
        int matched = filter.match(Intent.ACTION_VIEW, "application/vnd.android.package-archive",
                "https", sampleUri, Set.of(Intent.CATEGORY_BROWSABLE), "Sprint13Test");
        assertTrue(matched > 0, "IntentFilter phải khớp URL & MIME hợp lệ, code=" + matched);
        assertEquals(IntentFilter.NO_MATCH_ACTION,
                filter.match(Intent.ACTION_SEND, "application/octet-stream", "https", sampleUri, Set.of(), "Sprint13Test"));
        assertEquals(IntentFilter.NO_MATCH_CATEGORY,
                filter.match(Intent.ACTION_VIEW, "application/octet-stream", "https", sampleUri,
                        Set.of("android.intent.category.UNKNOWN"), "Sprint13Test"));

        // 3. ConnectivityManager.getActiveLinkProperties() & PowerManager.isDeviceIdleMode()
        ConnectivityManager cm = new ConnectivityManager();
        LinkProperties activeLp = cm.getActiveLinkProperties();
        assertNotNull(activeLp);
        assertNotNull(activeLp.getDnsServers());
        PowerManager pm = new PowerManager();
        assertFalse(pm.isDeviceIdleMode());
        assertFalse(pm.isLightDeviceIdleMode());

        // 4. Looper.getMainLooper().getQueue() & MessageQueue.IdleHandler
        MessageQueue mq = Looper.getMainLooper().getQueue();
        assertNotNull(mq);
        assertSame(mq, Looper.myQueue());
        CountDownLatch idleLatch = new CountDownLatch(1);
        MessageQueue.IdleHandler idleHandler = () -> {
            idleLatch.countDown();
            return false;
        };
        mq.addIdleHandler(idleHandler);
        assertTrue(idleLatch.await(3, TimeUnit.SECONDS), "MessageQueue.IdleHandler phải được kích hoạt trên Main Looper");
        mq.removeIdleHandler(idleHandler);

        // 5. Uri.fromParts & query helpers
        Uri pkgUri = Uri.fromParts("package", "com.apkpure.aegon", "frag");
        assertTrue(pkgUri.isOpaque());
        assertFalse(pkgUri.isHierarchical());
        assertTrue(pkgUri.isAbsolute());
        assertEquals("com.apkpure.aegon", pkgUri.getSchemeSpecificPart());
        Uri queryUri = Uri.parse("https://example.org/check?enabled=true&zero=0");
        assertTrue(queryUri.getBooleanQueryParameter("enabled", false));
        assertFalse(queryUri.getBooleanQueryParameter("zero", true));

        // 6. TextUtils & WebSettings
        Parcel tp = Parcel.obtain();
        try {
            TextUtils.writeToParcel("AAOW Sprint 13", tp, 0);
            tp.setDataPosition(0);
            CharSequence restoredCs = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(tp);
            assertEquals("AAOW Sprint 13", restoredCs.toString());
        } finally {
            tp.recycle();
        }
        assertEquals(View.LAYOUT_DIRECTION_LTR, TextUtils.getLayoutDirectionFromLocale(Locale.ENGLISH));
        assertEquals(View.LAYOUT_DIRECTION_RTL, TextUtils.getLayoutDirectionFromLocale(Locale.forLanguageTag("ar")));
        assertTrue(TextUtils.isDigitsOnly("202613"));
        assertFalse(TextUtils.isDigitsOnly("2026a"));

        String ua = WebSettings.getDefaultUserAgent(null);
        assertNotNull(ua);
        assertTrue(ua.contains("Android") && ua.contains("AAOW"));

        // 7. PendingIntent.getIntentSender(), RunningServiceInfo & Parcel collections
        Path showcaseApk = resolveRepoFile("samples/aaow-showcase.apk");
        Assumptions.assumeTrue(Files.isRegularFile(showcaseApk));
        try (ApkFile apk = new ApkFile(showcaseApk)) {
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, getClass().getClassLoader(), tmpDir);
            PendingIntent pi = PendingIntent.getActivity(ctx, 7, new Intent(Intent.ACTION_VIEW), PendingIntent.FLAG_IMMUTABLE);
            IntentSender sender = pi.getIntentSender();
            assertNotNull(sender);
            assertEquals(ctx.getPackageName(), sender.getCreatorPackage());

            Parcel p = Parcel.obtain();
            try {
                sender.writeToParcel(p, 0);
                ActivityManager.RunningServiceInfo rsi = new ActivityManager.RunningServiceInfo();
                rsi.pid = 4321;
                rsi.process = "com.aaow.svc";
                rsi.foreground = true;
                rsi.started = true;
                rsi.writeToParcel(p, 0);

                int[] intArr = {10, 20, 30};
                p.writeIntArray(intArr);
                p.writeList(List.of("alpha", "beta"));
                p.writeMap(Map.of("env", "windows", "sprint", 13));

                p.setDataPosition(0);
                IntentSender restoredSender = IntentSender.CREATOR.createFromParcel(p);
                assertEquals(sender, restoredSender);

                ActivityManager.RunningServiceInfo restoredRsi = ActivityManager.RunningServiceInfo.CREATOR.createFromParcel(p);
                assertEquals(4321, restoredRsi.pid);
                assertEquals("com.aaow.svc", restoredRsi.process);
                assertTrue(restoredRsi.foreground);
                assertTrue(restoredRsi.started);

                int[] outArr = new int[3];
                p.readIntArray(outArr);
                assertArrayEquals(intArr, outArr);

                List<Object> outList = new ArrayList<>();
                p.readList(outList, getClass().getClassLoader());
                assertEquals(List.of("alpha", "beta"), outList);

                Map<Object, Object> outMap = new HashMap<>();
                p.readMap(outMap, getClass().getClassLoader());
                assertEquals("windows", outMap.get("env"));
                assertEquals(13, outMap.get("sprint"));
            } finally {
                p.recycle();
            }

            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            assertNotNull(am.getRunningServices(10));
            PackageItemInfo pii = new PackageItemInfo();
            pii.packageName = ctx.getPackageName();
            assertNotNull(pii.loadIcon(ctx.getPackageManager()));

            // 8. DiskSharedPreferences.Editor thread safety khi nhiều luồng dùng chung 1 Editor
            android.content.SharedPreferences prefs = ctx.getSharedPreferences("race_test", Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor sharedEditor = prefs.edit();
            ExecutorService pool = Executors.newFixedThreadPool(4);
            try {
                List<Future<?>> futures = new ArrayList<>();
                for (int t = 0; t < 4; t++) {
                    final int tid = t;
                    futures.add(pool.submit(() -> {
                        for (int iter = 0; iter < 50; iter++) {
                            sharedEditor.putString("k_" + tid + "_" + iter, "v" + iter);
                            sharedEditor.putInt("n_" + tid, iter);
                            sharedEditor.apply();
                        }
                    }));
                }
                for (Future<?> f : futures) {
                    f.get(10, TimeUnit.SECONDS);
                }
                assertEquals(49, prefs.getInt("n_0", -1));
                assertEquals("v49", prefs.getString("k_3_49", null));
            } finally {
                pool.shutdownNow();
            }
        }
    }

    @Test
    void uiViewDrawableAndContextApis(@TempDir Path tmpDir) throws Exception {
        Path showcaseApk = resolveRepoFile("samples/aaow-showcase.apk");
        Assumptions.assumeTrue(Files.isRegularFile(showcaseApk));

        try (ApkFile apk = new ApkFile(showcaseApk)) {
            AndroidRuntimeContext baseCtx = new AndroidRuntimeContext(apk, getClass().getClassLoader(), tmpDir);
            Application app = new Application();
            app.preAttachBaseContext(baseCtx);
            baseCtx.setApplicationContext(app);

            // 1. Context.registerComponentCallbacks & createPackageContext (Glide dependency)
            ContextWrapper wrapper = new ContextWrapper(baseCtx);
            AtomicInteger lowMemCount = new AtomicInteger();
            ComponentCallbacks cb = new ComponentCallbacks() {
                @Override public void onConfigurationChanged(Configuration newConfig) {}
                @Override public void onLowMemory() { lowMemCount.incrementAndGet(); }
            };
            wrapper.registerComponentCallbacks(cb);
            app.onLowMemory();
            assertEquals(1, lowMemCount.get());
            wrapper.unregisterComponentCallbacks(cb);
            app.onLowMemory();
            assertEquals(1, lowMemCount.get());
            assertNotNull(wrapper.createPackageContext(baseCtx.getPackageName(), Context.CONTEXT_IGNORE_SECURITY));
            assertThrows(PackageManager.NameNotFoundException.class,
                    () -> wrapper.createPackageContext("com.nonexistent.pkg", 0));

            // 2. GradientDrawable (full Material/button gradient API)
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.RECTANGLE);
            gd.setGradientType(GradientDrawable.RADIAL_GRADIENT);
            assertEquals(GradientDrawable.RADIAL_GRADIENT, gd.getGradientType());
            float[] radii = {8f, 8f, 8f, 8f, 4f, 4f, 4f, 4f};
            gd.setCornerRadii(radii);
            assertArrayEquals(radii, gd.getCornerRadii());
            ColorStateList greenCsl = ColorStateList.valueOf(0xFF00AA55);
            gd.setColor(greenCsl);
            assertEquals(0xFF00AA55, gd.getColor().getDefaultColor());
            gd.setColors(new int[]{0xFF112233, 0xFF445566}, new float[]{0f, 1f});
            assertArrayEquals(new int[]{0xFF112233, 0xFF445566}, gd.getColors());
            gd.setStroke(2, ColorStateList.valueOf(0xFFFFFFFF));
            gd.setGradientCenter(0.3f, 0.7f);
            gd.setGradientRadius(24f);
            gd.setSize(64, 64);
            gd.setPadding(4, 2, 4, 2);
            Rect pad = new Rect();
            assertTrue(gd.getPadding(pad));
            assertEquals(4, pad.left);
            gd.setBounds(0, 0, 64, 64);
            Bitmap bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
            gd.draw(new Canvas(bmp));

            // 3. ClipDrawable clips strictly according to level (5000 = 50% width)
            ClipDrawable clip = new ClipDrawable(new ColorDrawable(0xFF00FF00), Gravity.LEFT, ClipDrawable.HORIZONTAL);
            clip.setBounds(0, 0, 100, 20);
            clip.setLevel(5000);
            Bitmap clipBmp = Bitmap.createBitmap(100, 20, Bitmap.Config.ARGB_8888);
            clip.draw(new Canvas(clipBmp));
            assertEquals(0xFF00FF00, clipBmp.getPixel(10, 10));
            assertEquals(0, clipBmp.getPixel(80, 10));

            // 4. Bitmap & Paint Glide helpers
            bmp.prepareToDraw();
            bmp.reconfigure(32, 32, Bitmap.Config.ARGB_8888);
            assertTrue(bmp.sameAs(bmp));
            Paint paint = new Paint();
            paint.setFilterBitmap(true);
            assertTrue(paint.isFilterBitmap());
            paint.setDither(true);
            assertTrue(paint.isDither());

            // 5. RelativeLayout, ViewFlipper/ViewAnimator, TextView, ProgressBar, View, Window, AccessibilityNodeInfo
            RelativeLayout rl = new RelativeLayout(baseCtx);
            rl.setGravity(Gravity.CENTER);
            assertEquals(Gravity.CENTER, rl.getGravity());
            rl.setTag("rl-root");

            ViewFlipper flipper = new ViewFlipper(baseCtx);
            flipper.setFlipInterval(1200);
            assertEquals(1200, flipper.getFlipInterval());
            flipper.setAutoStart(true);
            assertTrue(flipper.isAutoStart());
            flipper.setInAnimation(baseCtx, 0);
            flipper.setOutAnimation(baseCtx, 0);
            flipper.setAnimateFirstView(false);
            assertFalse(flipper.getAnimateFirstView());
            flipper.setTag("flip-tag");

            List<Integer> visibilityHistory = new ArrayList<>();
            View visView = new View(baseCtx) {
                @Override
                protected void onVisibilityChanged(View changedView, int visibility) {
                    super.onVisibilityChanged(changedView, visibility);
                    visibilityHistory.add(visibility);
                }
            };
            visView.setTag("vis-child");
            visView.setVisibility(View.INVISIBLE);
            assertEquals(List.of(View.INVISIBLE), visibilityHistory);

            FrameLayout container = new FrameLayout(baseCtx);
            container.addView(rl);
            rl.addView(flipper);
            flipper.addView(visView);
            assertSame(visView, container.findViewWithTag("vis-child"));

            Rect visRect = new Rect();
            visView.getWindowVisibleDisplayFrame(visRect);
            assertTrue(visRect.width() > 0 && visRect.height() > 0);

            TextView tv = new TextView(baseCtx);
            tv.setError("Invalid input");
            assertEquals("Invalid input", tv.getError());
            assertTrue(tv.setFontVariationSettings("'wght' 700"));
            assertEquals("'wght' 700", tv.getFontVariationSettings());
            tv.setTextLocales(LocaleList.getAdjustedDefault());
            assertFalse(tv.getTextLocales().isEmpty());
            tv.setCompoundDrawableTintList(ColorStateList.valueOf(0xFF123456));
            tv.setCompoundDrawableTintMode(PorterDuff.Mode.SRC_IN);
            tv.setLineHeight(28);
            tv.setFirstBaselineToTopHeight(12);
            tv.setLastBaselineToBottomHeight(10);
            assertEquals(12, tv.getFirstBaselineToTopHeight());
            assertEquals(10, tv.getLastBaselineToBottomHeight());

            ProgressBar pb = new ProgressBar(baseCtx);
            pb.setProgressDrawable(new ColorDrawable(0xFF00AAFF));
            pb.setProgressTintList(ColorStateList.valueOf(0xFF00AAFF));
            pb.setSecondaryProgress(40);
            pb.incrementSecondaryProgressBy(15);
            assertEquals(55, pb.getSecondaryProgress());

            Window win = new Window(baseCtx);
            win.setWindowManager(baseCtx.getSystemService(android.view.WindowManager.class), null, "test");
            assertNotNull(win.getWindowManager());
            assertNotNull(win.getLayoutInflater());

            AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain();
            node.setCollectionInfo(AccessibilityNodeInfo.CollectionInfo.obtain(5, 2, false));
            node.setCollectionItemInfo(AccessibilityNodeInfo.CollectionItemInfo.obtain(1, 1, 0, 1, false, true));
            assertEquals(5, node.getCollectionInfo().getRowCount());
            assertTrue(node.getCollectionItemInfo().isSelected());
        }
    }

    @Test
    void nativePtrReflectionFieldsAreNonZeroAndUnique() throws Exception {
        Canvas c1 = new Canvas();
        Canvas c2 = new Canvas();
        assertDistinctNonZeroPtr(c1, c2, "mNativeCanvasWrapper");
        assertDistinctNonZeroPtr(c1, c2, "mNativePtr");

        Paint p1 = new Paint();
        Paint p2 = new Paint();
        assertDistinctNonZeroPtr(p1, p2, "mNativePaint");
        assertDistinctNonZeroPtr(p1, p2, "mNativePtr");

        android.graphics.Path path1 = new android.graphics.Path();
        android.graphics.Path path2 = new android.graphics.Path();
        assertDistinctNonZeroPtr(path1, path2, "mNativePath");
        assertDistinctNonZeroPtr(path1, path2, "mNativePtr");

        Bitmap b1 = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
        Bitmap b2 = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
        assertDistinctNonZeroPtr(b1, b2, "mNativePtr");

        assertDistinctNonZeroPtr(Typeface.DEFAULT, Typeface.DEFAULT_BOLD, "native_instance");
        assertDistinctNonZeroPtr(Typeface.DEFAULT, Typeface.DEFAULT_BOLD, "mNativePtr");

        Matrix m1 = new Matrix();
        Matrix m2 = new Matrix();
        assertDistinctNonZeroPtr(m1, m2, "native_instance");
        assertDistinctNonZeroPtr(m1, m2, "mNativePtr");

        Parcel pr1 = Parcel.obtain();
        Parcel pr2 = Parcel.obtain();
        try {
            assertDistinctNonZeroPtr(pr1, pr2, "mNativePtr");
        } finally {
            pr1.recycle();
            pr2.recycle();
        }
    }

    @Test
    void qseaAndQmspJniHleHandlerWorksForReal(@TempDir Path tmpDir) throws Exception {
        NativeBridge.clearDiagnostics();

        // 1. RSA-2048 X.509 public key từ libqsealib.so U.p()
        Object pubObj = NativeBridge.invokeNative("com.ola.qsea.encrypt.U.p()Ljava/lang/String;", null, new Object[0]);
        assertInstanceOf(String.class, pubObj);
        byte[] x509Der = Base64.getDecoder().decode((String) pubObj);
        RSAPublicKey rsaKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(x509Der));
        assertEquals(2048, rsaKey.getModulus().bitLength());

        // 2. AES-CBC encrypt/decrypt round-trip thật qua U.a(int, byte[], byte[], byte[])
        byte[] key = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
        byte[] iv = "fedcba9876543210".getBytes(StandardCharsets.US_ASCII);
        byte[] plain = "aaow-sprint13-qsea-payload".getBytes(StandardCharsets.UTF_8);
        Object cipherObj = NativeBridge.invokeNative("com.ola.qsea.encrypt.U.a(I[B[B[B)[B",
                null, new Object[]{Cipher.ENCRYPT_MODE, key, iv, plain});
        assertInstanceOf(byte[].class, cipherObj);
        byte[] cipherBytes = (byte[]) cipherObj;
        assertNotEquals(new String(plain, StandardCharsets.UTF_8),
                Base64.getEncoder().encodeToString(cipherBytes));

        Object decObj = NativeBridge.invokeNative("com.ola.qsea.encrypt.U.a(I[B[B[B)[B",
                null, new Object[]{Cipher.DECRYPT_MODE, key, iv, cipherBytes});
        assertArrayEquals(plain, (byte[]) decObj);

        // 3. Các metadata & trait khác từ libqsealib.so và libqmp.so
        assertEquals("{}", new String((byte[]) NativeBridge.invokeNative(
                "com.ola.qsea.encrypt.U.d(J)[B", null, new Object[]{1L}), StandardCharsets.UTF_8));
        String[] traits = (String[]) NativeBridge.invokeNative(
                "com.ola.qsea.encrypt.U.c()[Ljava/lang/String;", null, new Object[0]);
        assertEquals(4, traits.length);
        for (String t : traits) {
            assertNotNull(t);
            assertFalse(t.isEmpty());
        }
        assertFalse(((String) NativeBridge.invokeNative(
                "com.ola.qsea.encrypt.U.o()Ljava/lang/String;", null, new Object[0])).isEmpty());
        assertFalse(((String) NativeBridge.invokeNative(
                "com.ola.qsea.encrypt.U.r()Ljava/lang/String;", null, new Object[0])).isEmpty());
        assertFalse(((String) NativeBridge.invokeNative(
                "com.ola.qsea.encrypt.U.s()Ljava/lang/String;", null, new Object[0])).isEmpty());
        assertFalse(((String) NativeBridge.invokeNative(
                "com.ola.qsea.encrypt.U.u()Ljava/lang/String;", null, new Object[0])).isEmpty());

        Path showcaseApk = resolveRepoFile("samples/aaow-showcase.apk");
        Assumptions.assumeTrue(Files.isRegularFile(showcaseApk));
        try (ApkFile apk = new ApkFile(showcaseApk)) {
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, getClass().getClassLoader(), tmpDir);
            String zVal = (String) NativeBridge.invokeNative(
                    "com.ola.qsea.encrypt.U.z(Landroid/content/Context;)Ljava/lang/String;",
                    null, new Object[]{ctx});
            assertNotNull(zVal);
            assertEquals(32, zVal.length());
        }

        assertEquals((byte) 0, NativeBridge.invokeNative("com.ola.qsea.encrypt.U.e()B", null, new Object[0]));

        String qmspJson = (String) NativeBridge.invokeNative(
                "com.tencent.qmsp.sdk.u.U.a(I)Ljava/lang/String;", null, new Object[]{1});
        assertTrue(qmspJson.contains("\"ret\":0"));
        assertEquals(0, NativeBridge.invokeNative("com.tencent.qmsp.sdk.u.U.b()I", null, new Object[0]));

        Map<String, Integer> hleCounts = NativeBridge.getEmulatedNativeCallCounts();
        assertTrue(hleCounts.containsKey("HLE:com.ola.qsea.encrypt.U.p()Ljava/lang/String;"));
        assertTrue(hleCounts.containsKey("HLE:com.ola.qsea.encrypt.U.a(I[B[B[B)[B"));
        assertTrue(hleCounts.containsKey("HLE:com.tencent.qmsp.sdk.u.U.a(I)Ljava/lang/String;"));
    }

    @Test
    void fallbackClassLoaderForwardsSuperConstructorArgs() throws Exception {
        String internal = "dev/aaow/teststub/StubCustomView";
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internal, null, "android/view/View", null);

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                "(Landroid/content/Context;Landroid/util/AttributeSet;)V", null, null);
        mv.visitCode();
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/RuntimeException");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("Stub!");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.ATHROW);
        mv.visitMaxs(3, 4);
        mv.visitEnd();
        cw.visitEnd();

        AndroidSdkFallbackClassLoader loader = new AndroidSdkFallbackClassLoader(getClass().getClassLoader());
        byte[] transformed = loader.deStubAndroidClass(cw.toByteArray());
        Class<?> viewCls = new ClassLoader(loader) {
            Class<?> define() {
                return defineClass(internal.replace('/', '.'), transformed, 0, transformed.length);
            }
        }.define();

        Context ctx = new ContextWrapper(null);
        View instance = (View) viewCls.getDeclaredConstructor(Context.class, AttributeSet.class)
                .newInstance(ctx, null);
        assertSame(ctx, instance.getContext(),
                "Constructor stub kế thừa android.view.View phải chuyển tiếp Context thay vì truyền null");
    }

    @Test
    void andResGuardObfuscatedAttrProtectionObtainTypedArrayAndRealWebpDecoding() throws Exception {
        assertTrue(ImageIO.getImageReadersByFormatName("webp").hasNext(),
                "ImageIO WebP reader SPI (twelvemonkeys imageio-webp) phải được kích hoạt");
        assertTrue(ImageIO.getImageReadersByMIMEType("image/webp").hasNext());

        Path apkpureApk = resolveRepoFile("build/aaow-data/installed-apps/com.apkpure.aegon.apk");
        Assumptions.assumeTrue(Files.isRegularFile(apkpureApk), "Bỏ qua nếu chưa có com.apkpure.aegon.apk");

        try (ApkFile apk = new ApkFile(apkpureApk)) {
            ResourceTable table = apk.getResources();
            assertFalse(table.isUniqueAttrKey("arg"),
                    "Tên khóa attr làm rối trùng lặp 'arg' trong APKPure phải bị đánh dấu không duy nhất");

            Resources res = new Resources(apk);
            // Hệ thống theme attr mặc định AOSP (colorBackgroundFloating = 0x010104e2) trả màu hợp lệ,
            // còn attr không tồn tại giữ unresolved không trả giá trị rác (TypedArray.hasValue == false)
            TypedArray ta = res.newTheme().obtainStyledAttributes(new int[]{0x010104e2, 0x7f04fffe});
            try {
                assertTrue(ta.hasValue(0));
                assertEquals(0xFFFFFFFF, ta.getColor(0, 0));
                assertFalse(ta.hasValue(1), "Attr không tồn tại phải trả hasValue == false");
            } finally {
                ta.recycle();
            }

            // Tìm 1 resource 'array' thật trong APKPure và kiểm tra Resources.obtainTypedArray(id)
            int arrayId = findFirstResourceIdOfType(table, "array");
            assertNotEquals(0, arrayId, "Phải tìm thấy resource 'array' trong APKPure");
            TypedArray arrTa = res.obtainTypedArray(arrayId);
            assertNotNull(arrTa);
            assertTrue(arrTa.length() > 0, "obtainTypedArray phải đọc được các phần tử thật của bag array");
            arrTa.recycle();

            // Trích xuất một ảnh WebP thật trong APKPure và giải mã pixel qua BitmapFactory
            byte[] webpBytes = apk.read("r/z/a2d.webp");
            assertNotNull(webpBytes, "Phải đọc được tệp WebP thật từ APK");
            BitmapFactory.Options opts = new BitmapFactory.Options();
            Bitmap bmp = BitmapFactory.decodeByteArray(webpBytes, 0, webpBytes.length, opts);
            assertNotNull(bmp);
            assertTrue(bmp.getWidth() > 0 && bmp.getHeight() > 0);
            assertNotNull(opts.outMimeType);
            assertTrue(opts.outMimeType.contains("webp"), "outMimeType=" + opts.outMimeType);

            boolean hasVisiblePixel = false;
            outer:
            for (int y = 0; y < bmp.getHeight(); y++) {
                for (int x = 0; x < bmp.getWidth(); x++) {
                    if ((bmp.getPixel(x, y) >>> 24) != 0) {
                        hasVisiblePixel = true;
                        break outer;
                    }
                }
            }
            assertTrue(hasVisiblePixel, "Ảnh WebP giải mã bởi imageio-webp phải có pixel thật khác trong suốt");
        }
    }

    @Test
    void apkpureSplashTransitionsToMainTabActivityEndToEnd(@TempDir Path tmpDir) throws Exception {
        Path apkpureApk = resolveRepoFile("build/aaow-data/installed-apps/com.apkpure.aegon.apk");
        Assumptions.assumeTrue(Files.isRegularFile(apkpureApk), "Bỏ qua nếu chưa có com.apkpure.aegon.apk");

        Path outPng = tmpDir.resolve("apkpure-e2e.png");
        Path logFile = tmpDir.resolve("apkpure-e2e.log");
        Path dataDir = tmpDir.resolve("aaow-data");
        Path repoRoot = resolveRepoRoot();

        Path javaExe = Path.of(System.getProperty("java.home"), "bin", "java.exe");
        String javaCmd = Files.isRegularFile(javaExe) ? javaExe.toAbsolutePath().toString() : "java";

        ProcessBuilder pb = new ProcessBuilder(
                javaCmd,
                "-Xmx1024m",
                "-Dfile.encoding=UTF-8",
                "-Daaow.dumpTree=true",
                "-cp", System.getProperty("java.class.path"),
                Main.class.getName(),
                "--data-dir", dataDir.toAbsolutePath().toString(),
                "--screenshot", apkpureApk.toAbsolutePath().toString(), outPng.toAbsolutePath().toString()
        );
        pb.directory(repoRoot.toFile());
        pb.redirectErrorStream(true);
        pb.redirectOutput(logFile.toFile());

        Process proc = pb.start();
        boolean finished = proc.waitFor(120, TimeUnit.SECONDS);
        if (!finished) {
            proc.destroyForcibly();
            fail("APKPure E2E child JVM quá hạn 120s");
        }

        String log = Files.isRegularFile(logFile)
                ? new String(Files.readAllBytes(logFile), StandardCharsets.UTF_8) : "";
        assertEquals(0, proc.exitValue(), "APKPure E2E phải thoát mã 0:\n" + tail(log, 2500));
        assertTrue(Files.isRegularFile(outPng), "Phải xuất được ảnh PNG kết xuất");
        assertTrue(Files.size(outPng) > 15_000, "Ảnh PNG kết xuất phải chứa giao diện phong phú");

        BufferedImage img = ImageIO.read(outPng.toFile());
        assertNotNull(img);
        assertEquals(420, img.getWidth());
        assertEquals(820, img.getHeight());
        assertTrue(hasVisualVariance(img), "Ảnh chụp APKPure không được đơn sắc/trắng xóa");

        assertTrue(log.contains("Starting Activity: com.apkpure.aegon.main.activity.SplashActivity"),
                "Phải khởi chạy từ SplashActivity:\n" + tail(log, 2000));
        assertTrue(log.contains("Starting Activity: com.apkpure.aegon.main.activity.MainTabActivity"),
                "SplashActivity phải tự chuyển tiếp vào MainTabActivity:\n" + tail(log, 2500));
        assertFalse(log.contains("VerifyError"), "Không được còn VerifyError");
        assertFalse(log.contains("AAOW Diagnostic"), "Không được rơi về màn hình chẩn đoán AAOW Diagnostic");
        assertTrue(log.contains("TabLayout") || log.contains("AHBottomNavigation")
                        || log.contains("MarqueeView") || log.contains("RecyclerView"),
                "Cây View kết xuất phải chứa thành phần giao diện MainTabActivity thật");
    }

    private static void assertDistinctNonZeroPtr(Object first, Object second, String fieldName) throws Exception {
        Field f = first.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        long v1 = f.getLong(first);
        long v2 = f.getLong(second);
        assertNotEquals(0L, v1, first.getClass().getSimpleName() + "." + fieldName + " phải khác 0");
        assertNotEquals(0L, v2, second.getClass().getSimpleName() + "." + fieldName + " phải khác 0");
        assertNotEquals(v1, v2, first.getClass().getSimpleName() + "." + fieldName + " giữa 2 instance phải khác nhau");
    }

    private static int findFirstResourceIdOfType(ResourceTable table, String targetType) {
        for (int t = 1; t <= 32; t++) {
            for (int e = 0; e < 25; e++) {
                int id = (0x7f << 24) | (t << 16) | e;
                ResourceTable.Entry entry = table.getEntry(id);
                if (entry != null && targetType.equals(entry.type) && entry.isBag()) {
                    return id;
                }
            }
        }
        return 0;
    }

    private static boolean hasVisualVariance(BufferedImage img) {
        int base = img.getRGB(20, 120);
        int diff = 0;
        for (int y = 60; y < img.getHeight() - 60; y += 8) {
            for (int x = 16; x < img.getWidth() - 16; x += 8) {
                if (img.getRGB(x, y) != base && ++diff >= 25) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String tail(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) return text;
        return text.substring(text.length() - maxChars);
    }

    private static Path resolveRepoFile(String relativePath) {
        Path direct = Path.of(relativePath);
        if (Files.exists(direct)) return direct;
        Path parent = Path.of("..", relativePath);
        if (Files.exists(parent)) return parent;
        return direct;
    }

    private static Path resolveRepoRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        if (Files.isRegularFile(cwd.resolve("settings.gradle.kts"))) {
            return cwd;
        }
        if (cwd.getParent() != null && Files.isRegularFile(cwd.getParent().resolve("settings.gradle.kts"))) {
            return cwd.getParent();
        }
        return cwd;
    }
}
