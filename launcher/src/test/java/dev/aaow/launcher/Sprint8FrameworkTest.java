package dev.aaow.launcher;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import dev.aaow.apk.ApkFile;
import dev.aaow.rt.AndroidRuntimeContext;
import dev.aaow.rt.HostWindow;
import dev.aaow.rt.ResourceCache;
import dev.aaow.rt.jni.WebpHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử đơn vị toàn diện cho Sprint 8:
 * - Bộ nhớ đệm tài nguyên LRU giới hạn byte (Bitmap/XML/Drawable ConstantState), Bitmap.recycle, inSampleSize.
 * - Tối ưu vẽ View (alpha/rotation/scale transform, viewport clip culling, ValueAnimator, HostWindow dirty-layout skip).
 * - Đáp ứng thay đổi kích thước cửa sổ, phóng to/thu nhỏ (Zoom) & xoay ngang/dọc (Configuration + Display.getRotation).
 * - Dịch DEX trên bộ nhớ song song đa nhân & chỉ mục `.refs` khởi động nóng.
 * - Tích hợp Windows: icon đa phân giải (.ico), phân giải đường dẫn bản cài đặt độc lập, liên kết đuôi file `.apk`.
 */
class Sprint8FrameworkTest {

    @Test
    void resourceCacheBoundsConstantStateRecycleAndInSampleSizeWork(@TempDir Path tempDir) throws Exception {
        // 1. Kiểm tra LRU giới hạn dung lượng byte cho Bitmap & Bitmap.recycle()
        ResourceCache smallCache = new ResourceCache(10_000L, 16, 16);
        Bitmap b1 = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888); // 6,400 bytes
        Bitmap b2 = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888); // 6,400 bytes -> vượt 10,000 -> đẩy b1
        assertEquals(6400, b1.getByteCount());
        assertEquals(6400, b1.getAllocationByteCount());
        assertTrue(b1.isMutable());

        smallCache.putBitmap(101, b1);
        assertNotNull(smallCache.getBitmap(101));
        smallCache.putBitmap(102, b2);
        assertNull(smallCache.getBitmap(101)); // bị đẩy khỏi LRU
        assertNotNull(smallCache.getBitmap(102));
        assertTrue(smallCache.getStats().evictions() >= 1);
        assertTrue(smallCache.getStats().currentBitmapBytes() <= 10_000L);

        b2.recycle();
        assertTrue(b2.isRecycled());
        assertNull(smallCache.getBitmap(102)); // tự loại bỏ bitmap đã recycle

        // 2. Kiểm tra cache Drawable/XML trên APK thật & ConstantState trả về instance độc lập
        Path showcaseApk = resolveRepoFile("samples/aaow-showcase.apk");
        try (ApkFile apk = new ApkFile(showcaseApk)) {
            Resources res = new Resources(apk);
            int iconId = apk.getManifest().iconRes;
            assertTrue(iconId != 0);

            Drawable d1 = res.getDrawable(iconId);
            Drawable d2 = res.getDrawable(iconId);
            assertNotNull(d1);
            assertNotNull(d2);
            assertNotSame(d1, d2);
            d1.setBounds(0, 0, 24, 24);
            d2.setBounds(10, 10, 58, 58);
            assertEquals(24, d1.getBounds().width());
            assertEquals(48, d2.getBounds().width());
            assertTrue(res.getCacheStats().drawableHits() >= 1 || res.getCacheStats().xmlHits() >= 1);

            res.trimMemory(60);
            assertEquals(0L, res.getCacheStats().currentBitmapBytes());
        }

        // 3. Kiểm tra WebpHelper inJustDecodeBounds & inSampleSize giảm RAM
        BufferedImage srcImg = new BufferedImage(120, 80, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
        ImageIO.write(srcImg, "png", pngOut);
        byte[] pngBytes = pngOut.toByteArray();

        BitmapFactory.Options boundsOnly = new BitmapFactory.Options();
        boundsOnly.inJustDecodeBounds = true;
        assertNull(WebpHelper.decodeWithOptions(pngBytes, 0, pngBytes.length, boundsOnly));
        assertEquals(120, boundsOnly.outWidth);
        assertEquals(80, boundsOnly.outHeight);

        BitmapFactory.Options sampled = new BitmapFactory.Options();
        sampled.inSampleSize = 2;
        Bitmap half = WebpHelper.decodeWithOptions(pngBytes, 0, pngBytes.length, sampled);
        assertNotNull(half);
        assertEquals(60, half.getWidth());
        assertEquals(40, half.getHeight());
    }

    @Test
    void viewTransformAlphaViewportCullingAndValueAnimatorWork(@TempDir Path tempDir) {
        Path showcaseApk = resolveRepoFile("samples/aaow-showcase.apk");
        try (ApkFile apk = new ApkFile(showcaseApk)) {
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, getClass().getClassLoader(), tempDir);
            FrameLayout container = new FrameLayout(ctx);
            container.setClipChildren(true);

            AtomicInteger offscreenDraws = new AtomicInteger();
            AtomicInteger onscreenDraws = new AtomicInteger();

            View offscreenChild = new View(ctx) {
                @Override
                protected void onDraw(Canvas canvas) {
                    offscreenDraws.incrementAndGet();
                }
            };
            View onscreenChild = new View(ctx) {
                @Override
                protected void onDraw(Canvas canvas) {
                    onscreenDraws.incrementAndGet();
                    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                    p.setColor(Color.RED);
                    canvas.drawRect(0, 0, getWidth(), getHeight(), p);
                }
            };
            onscreenChild.setAlpha(0.5f);
            onscreenChild.setRotation(15f);
            onscreenChild.setScaleX(1.2f);

            container.addView(offscreenChild, 100, 50);
            container.addView(onscreenChild, 100, 50);

            container.measure(
                    View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY));
            container.layout(0, 0, 200, 200);
            offscreenChild.layout(0, 0, 100, 50);
            onscreenChild.layout(0, 420, 100, 470);
            container.scrollTo(0, 400);

            Bitmap target = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
            target.eraseColor(Color.WHITE);
            Canvas canvas = new Canvas(target);
            container.draw(canvas);

            assertEquals(0, offscreenDraws.get(), "Child nằm hoàn toàn ngoài viewport phải được bỏ qua khi vẽ");
            assertEquals(1, onscreenDraws.get(), "Child nằm trong vùng nhìn thấy phải được vẽ");
            assertEquals(50f, onscreenChild.getPivotX());
            assertEquals(25f, onscreenChild.getPivotY());

            // Kiểm tra ValueAnimator cập nhật giá trị đúng
            ValueAnimator anim = ValueAnimator.ofFloat(10f, 50f);
            AtomicBoolean updated = new AtomicBoolean(false);
            anim.addUpdateListener(a -> updated.set(true));
            anim.setCurrentFraction(0.5f);
            assertTrue(updated.get());
            assertEquals(30f, (Float) anim.getAnimatedValue(), 0.5f);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void hostWindowDirtyLayoutSkipRotateOrientationAndZoomWork(@TempDir Path tempDir) throws Exception {
        Path showcaseApk = resolveRepoFile("samples/aaow-showcase.apk");
        Path outImg = tempDir.resolve("showcase-s8.png");
        ClassLoader cl = null;
        try (ApkFile apk = new ApkFile(showcaseApk)) {
            cl = DexLoader.loadApkDex(apk, tempDir.resolve("dex-cache"), getClass().getClassLoader());
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, cl, tempDir.resolve("data"));
            Application app = AppRepository.createAndInitializeApplication(apk.getManifest(), cl, ctx, msg -> {});
            HostWindow host = new HostWindow(ctx, app);

            File outFile = outImg.toFile();
            host.renderHeadlessScreenshot(apk.getManifest().launcherActivity, outFile, 420, 820);
            assertTrue(Files.isRegularFile(outImg));

            HostWindow.FrameStats afterFirst = host.getFrameStats();
            assertTrue(afterFirst.totalFrames() >= 1);
            assertTrue(afterFirst.layoutPasses() >= 1);
            assertEquals(420, afterFirst.logicalWidth());
            assertEquals(820, afterFirst.logicalHeight());
            assertFalse(afterFirst.landscape());

            // Vẽ lại khung hình thứ 2 mà không gọi requestLayout -> phải skip measure/layout
            Path outImg2 = tempDir.resolve("showcase-s8-second.png");
            host.renderCurrentViewportSnapshot(outImg2.toFile());
            HostWindow.FrameStats afterSecond = host.getFrameStats();
            assertEquals(afterFirst.layoutPasses(), afterSecond.layoutPasses(), "Không thay đổi layout thì không đo/dàn trang lại");
            assertTrue(afterSecond.layoutSkips() >= 1, "Phải ghi nhận lượt skip layout thừa");

            // Đổi hướng xoay ngang & phóng to 125%
            SwingUtilities.invokeAndWait(() -> {
                host.setOrientationLandscape(true);
            });
            host.renderCurrentViewportSnapshot(tempDir.resolve("showcase-s8-land.png").toFile());

            HostWindow.FrameStats rotated = host.getFrameStats();
            assertTrue(rotated.landscape());
            assertEquals(820, rotated.logicalWidth());
            assertEquals(420, rotated.logicalHeight());
            assertEquals(Configuration.ORIENTATION_LANDSCAPE, ctx.getResources().getConfiguration().orientation);
            assertEquals(Surface.ROTATION_90, ctx.getDisplay().getRotation());
            assertTrue(rotated.layoutPasses() > afterSecond.layoutPasses());

            SwingUtilities.invokeAndWait(() -> {
                host.setViewportZoom(1.25f);
                assertEquals(1.25f, host.getViewportZoom());
            });
            assertEquals(1.25f, host.getFrameStats().zoomScale());
        } finally {
            if (cl instanceof Closeable closeable) {
                closeable.close();
            }
        }
    }

    @Test
    void dexLoaderInMemoryTranslationAndRefsIndexRoundTripWork(@TempDir Path tempDir) throws Exception {
        Path srcApk = resolveRepoFile("samples/aaow-2048.apk");
        Path copyApk = tempDir.resolve("aaow-2048-copy.apk");
        Files.copy(srcApk, copyApk);

        Path dexCache = tempDir.resolve("dex-cache");
        ClassLoader cl = null;
        try (ApkFile apk = new ApkFile(copyApk)) {
            Path jarPath = DexLoader.ensureDexJar(apk, dexCache);
            assertTrue(Files.isRegularFile(jarPath));
            assertTrue(jarPath.getFileName().toString().endsWith("-v9.jar"));

            Path refsPath = Path.of(jarPath.toString() + ".refs");
            assertTrue(Files.isRegularFile(refsPath), "DexLoader phải sinh file chỉ mục .refs trong cùng lần biên dịch");
            assertFalse(Files.exists(Path.of(jarPath.toString() + ".raw.jar")), "Không được để sót raw.jar trung gian");

            // Nạp lại từ cache và đọc .refs
            Map<String, ApkDexClassLoader.RefTypeMeta> loadedRefs = ApkDexClassLoader.loadOrScanExternalReferences(jarPath);
            assertNotNull(loadedRefs);

            cl = DexLoader.loadApkDex(apk, dexCache, getClass().getClassLoader());
            Class<?> actCls = Class.forName(apk.getManifest().launcherActivity, true, cl);
            assertTrue(Activity.class.isAssignableFrom(actCls));
        } finally {
            if (cl instanceof Closeable closeable) {
                closeable.close();
            }
        }
    }

    @Test
    void windowsIntegrationIconsPathsAndApkAssociationWork(@TempDir Path tempDir) throws Exception {
        // 1. Tạo icon đa phân giải Windows (.ico) và kiểm tra header ICO
        Path appIco = tempDir.resolve("aaow.ico");
        Path apkIco = tempDir.resolve("apk-file.ico");
        IconAssetGenerator.writeAppIcon(appIco);
        IconAssetGenerator.writeApkFileIcon(apkIco);
        assertTrue(Files.size(appIco) > 10_000);
        assertTrue(Files.size(apkIco) > 5_000);

        byte[] icoBytes = Files.readAllBytes(appIco);
        ByteBuffer bb = ByteBuffer.wrap(icoBytes).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0, bb.getShort()); // reserved
        assertEquals(1, bb.getShort()); // type ICO
        assertEquals(5, bb.getShort()); // 16, 32, 48, 64, 256

        // 2. Phân giải thư mục dữ liệu (standalone vs workspace vs override) & Environment.setDataRoot
        Path fakeInstall = Files.createDirectories(tempDir.resolve("program-files-aaow"));
        Path resolvedStandalone = WindowsIntegration.resolveDataRoot(fakeInstall, null);
        if (System.getenv("AAOW_HOME") == null || System.getenv("AAOW_HOME").isBlank()) {
            assertFalse(resolvedStandalone.startsWith(fakeInstall),
                    "Bản cài đặt độc lập không được ghi dữ liệu vào thư mục cài đặt");
        }
        Path customData = tempDir.resolve("custom-data");
        assertEquals(customData.toAbsolutePath(), WindowsIntegration.resolveDataRoot(fakeInstall, customData));

        Environment.setDataRoot(customData);
        assertEquals(customData.resolve("sdcard").toFile(), Environment.getExternalStorageDirectory());

        // 3. Kiểm tra sinh lệnh Registry & .reg + test đăng ký/gỡ thực trên khóa tạm cô lập
        Path fakeExe = fakeInstall.resolve("AAOW.exe");
        List<List<String>> cmds = WindowsIntegration.buildAssociationCommands(
                WindowsIntegration.HKCU_CLASSES_PREFIX, fakeExe, apkIco);
        assertTrue(cmds.size() >= 7);
        assertTrue(cmds.stream().anyMatch(c -> String.join(" ", c).contains("AAOW.ApkPackage\\shell\\open\\command")));

        String regContent = WindowsIntegration.generateRegFileContent(fakeExe, apkIco);
        assertTrue(regContent.contains("Windows Registry Editor Version 5.00"));
        assertTrue(regContent.contains(WindowsIntegration.PROG_ID));
        assertTrue(regContent.contains("--install"));

        if (WindowsIntegration.isWindows()) {
            String testPrefix = "HKCU\\Software\\AAOW_Test_" + UUID.randomUUID().toString().replace("-", "");
            try {
                assertFalse(WindowsIntegration.isApkAssociationRegistered(testPrefix));
                assertTrue(WindowsIntegration.registerApkAssociation(testPrefix, fakeExe, apkIco));
                assertTrue(WindowsIntegration.isApkAssociationRegistered(testPrefix));
                assertTrue(WindowsIntegration.unregisterApkAssociation(testPrefix));
                assertFalse(WindowsIntegration.isApkAssociationRegistered(testPrefix));
            } finally {
                try {
                    new ProcessBuilder("reg.exe", "delete", testPrefix, "/f").start().waitFor();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static Path resolveRepoFile(String relativePath) {
        Path direct = Path.of(relativePath);
        if (Files.exists(direct)) return direct;
        Path parent = Path.of("..", relativePath);
        if (Files.exists(parent)) return parent;
        return direct;
    }
}
