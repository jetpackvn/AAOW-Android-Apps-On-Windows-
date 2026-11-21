package dev.aaow.launcher;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import dev.aaow.apk.ApkFile;
import dev.aaow.rt.AndroidRuntimeContext;
import dev.aaow.rt.HostWindow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử cho hiệu ứng màn hình nạp APK, báo tiến độ dịch DEX
 * và cơ chế chặn không tự động hiện màn hình thiếu tài nguyên / chẩn đoán
 * trước khi ứng dụng thực sự boot vào giao diện chính.
 */
class ApkLoadingAndBootUiTest {

    public static class EmptySplashActivity extends Activity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Cố ý chưa gọi setContentView() để mô phỏng SplashActivity chờ bất đồng bộ
        }
    }

    public static class BootedHomeActivity extends Activity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            TextView tv = new TextView(this);
            tv.setText("Welcome Home");
            setContentView(tv);
        }
    }

    @Test
    void hostWindowDeferredLoadingScreenRendersCleanlyBeforeRuntimeAttached() throws Exception {
        HostWindow host = new HostWindow(
                "Demo App",
                "dev.aaow.demo",
                "1.2.3",
                IconAssetGenerator.renderAaowAppLogo(64));
        try {
            assertEquals(HostWindow.BootState.LOADING_APK, host.getBootState());
            assertFalse(host.isShowingDiagnosticFallback());

            host.updateLoadingProgress(0.45f, "Đang dịch DEX sang JVM bytecode...", "classes.dex");
            assertTrue(host.getLoadingProgress() >= 0.44f);
            assertEquals("Đang dịch DEX sang JVM bytecode...", host.getLoadingStageText());

            BufferedImage loadingSnap = host.renderLoadingSnapshot(420, 820);
            assertNotNull(loadingSnap);
            assertEquals(420, loadingSnap.getWidth());
            assertEquals(820, loadingSnap.getHeight());
            assertTrue(hasVisualVariance(loadingSnap), "Màn hình loading phải vẽ hiệu ứng trực quan");

            host.showBootFailure("Lỗi mô phỏng trước khi boot", new IllegalStateException("Test failure"));
            SwingUtilities.invokeAndWait(() -> {
            });
            assertEquals(HostWindow.BootState.FAILED, host.getBootState());
            assertFalse(host.isShowingDiagnosticFallback(),
                    "Ngay cả khi báo lỗi boot cũng không được tự ép sang màn hình chẩn đoán");

            BufferedImage failSnap = host.renderLoadingSnapshot(420, 820);
            assertNotNull(failSnap);
            assertTrue(hasVisualVariance(failSnap));
        } finally {
            host.forceStop();
        }
    }

    @Test
    void hostWindowDoesNotShowDiagnosticScreenBeforeAppActuallyBoots(@TempDir Path tmpDir) throws Exception {
        Path showcaseApk = resolveRepoFile("samples/aaow-showcase.apk");
        try (ApkFile apk = new ApkFile(showcaseApk)) {
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(
                    apk, ApkLoadingAndBootUiTest.class.getClassLoader(), tmpDir);
            Application app = new Application();
            app.preAttachBaseContext(ctx);
            ctx.setApplicationContext(app);
            app.attach(ctx);

            HostWindow host = new HostWindow(ctx, app);
            try {
                host.launchHeadless(EmptySplashActivity.class.getName(), 420, 820);

                assertEquals(HostWindow.BootState.WAITING_FOR_UI, host.getBootState());
                assertFalse(host.isShowingDiagnosticFallback(),
                        "Không được tự hiện màn hình thiếu tài nguyên khi Activity chưa setContentView");
                Activity splash = host.getCurrentActivity();
                assertNotNull(splash);
                assertInstanceOf(EmptySplashActivity.class, splash);
                assertEquals(0, ((ViewGroup) splash.getWindow().getDecorView()).getChildCount(),
                        "DecorView phải giữ rỗng chờ app boot, không chèn Báo cáo Tương thích");

                // Khi ứng dụng gọi setContentView bất đồng bộ hoặc mở Activity chính -> tự động chuyển sang BOOTED
                SwingUtilities.invokeAndWait(() -> {
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.setClassName(ctx.getPackageName(), BootedHomeActivity.class.getName());
                    host.startActivity(homeIntent);
                });
                host.pumpEventsAndLayout(100);

                assertEquals(HostWindow.BootState.BOOTED, host.getBootState());
                assertFalse(host.isShowingDiagnosticFallback());
                Activity home = host.getCurrentActivity();
                assertInstanceOf(BootedHomeActivity.class, home);
                assertEquals(1, ((ViewGroup) home.getWindow().getDecorView()).getChildCount());
            } finally {
                host.forceStop();
            }
        }
    }

    @Test
    void hostWindowFatalLaunchErrorDoesNotAutoShowDiagnosticUntilUserRequests(@TempDir Path tmpDir) throws Exception {
        Path showcaseApk = resolveRepoFile("samples/aaow-showcase.apk");
        try (ApkFile apk = new ApkFile(showcaseApk)) {
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(
                    apk, ApkLoadingAndBootUiTest.class.getClassLoader(), tmpDir);
            Application app = new Application();
            app.preAttachBaseContext(ctx);
            ctx.setApplicationContext(app);
            app.attach(ctx);

            HostWindow host = new HostWindow(ctx, app);
            try {
                host.launchHeadless("dev.aaow.nonexistent.BrokenSplashActivity", 420, 820);

                assertNotEquals(HostWindow.BootState.BOOTED, host.getBootState());
                assertFalse(host.isShowingDiagnosticFallback(),
                        "Không được tự động mở màn hình Báo cáo Tương thích khi lỗi khởi chạy Activity");
                assertEquals(0, ((ViewGroup) host.getCurrentActivity().getWindow().getDecorView()).getChildCount());

                // Chỉ mở màn hình Báo cáo Tương thích khi người dùng chủ động gọi openDiagnosticsManually()
                host.openDiagnosticsManually();
                SwingUtilities.invokeAndWait(() -> {
                });
                host.pumpEventsAndLayout(100);

                assertTrue(host.isShowingDiagnosticFallback());
                View decor = host.getCurrentActivity().getWindow().getDecorView();
                assertTrue(((ViewGroup) decor).getChildCount() > 0);
                assertTrue(containsText(decor, "Báo cáo Tương thích AAOW"));
            } finally {
                host.forceStop();
            }
        }
    }

    @Test
    void dexLoaderReportsProgressForColdAndCachedTranslation(@TempDir Path tmpDir) throws Exception {
        Path srcApk = resolveRepoFile("samples/aaow-2048.apk");
        Path copyApk = tmpDir.resolve("aaow-2048-progress.apk");
        Files.copy(srcApk, copyApk);
        Path cacheDir = tmpDir.resolve("dex-cache");

        List<Float> coldProgress = Collections.synchronizedList(new ArrayList<>());
        List<String> coldMessages = Collections.synchronizedList(new ArrayList<>());
        try (ApkFile apk = new ApkFile(copyApk)) {
            DexLoader.ensureDexJar(apk, cacheDir, (p, msg) -> {
                coldProgress.add(p);
                coldMessages.add(msg);
            });
        }
        assertTrue(coldProgress.size() >= 4, "Dịch lạnh phải báo nhiều mốc tiến độ, thực tế=" + coldProgress.size());
        assertNonDecreasingInUnitRange(coldProgress);
        assertTrue(coldMessages.stream().noneMatch(String::isBlank));

        List<Float> warmProgress = Collections.synchronizedList(new ArrayList<>());
        List<String> warmMessages = Collections.synchronizedList(new ArrayList<>());
        try (ApkFile apk = new ApkFile(copyApk)) {
            DexLoader.ensureDexJar(apk, cacheDir, (p, msg) -> {
                warmProgress.add(p);
                warmMessages.add(msg);
            });
        }
        assertFalse(warmProgress.isEmpty(), "Nạp từ cache cũng phải báo tiến độ");
        assertNonDecreasingInUnitRange(warmProgress);
        assertTrue(warmMessages.stream().anyMatch(m -> m.toLowerCase().contains("bộ đệm")));
    }

    private static void assertNonDecreasingInUnitRange(List<Float> values) {
        float prev = -1f;
        for (float v : values) {
            assertTrue(v >= 0f && v <= 1f, "Tiến độ phải nằm trong [0, 1], gặp: " + v);
            assertTrue(v >= prev, "Tiến độ phải tăng dần hoặc giữ nguyên: " + prev + " -> " + v);
            prev = v;
        }
    }

    private static boolean containsText(View root, String expected) {
        if (root instanceof TextView tv && tv.getText() != null && tv.getText().toString().contains(expected)) {
            return true;
        }
        if (root instanceof ViewGroup vg) {
            for (int i = 0; i < vg.getChildCount(); i++) {
                if (containsText(vg.getChildAt(i), expected)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasVisualVariance(BufferedImage img) {
        int base = img.getRGB(20, 20);
        int diff = 0;
        for (int y = 0; y < img.getHeight(); y += 8) {
            for (int x = 0; x < img.getWidth(); x += 8) {
                if (img.getRGB(x, y) != base && ++diff >= 30) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Path resolveRepoFile(String relativePath) {
        Path direct = Path.of(relativePath);
        if (Files.exists(direct)) return direct;
        return Path.of("..", relativePath).normalize();
    }
}
