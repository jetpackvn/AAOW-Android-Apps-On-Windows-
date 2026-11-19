package dev.aaow.launcher;

import android.app.Application;
import android.content.res.Resources;
import android.graphics.Bitmap;
import dev.aaow.apk.AndroidManifest;
import dev.aaow.apk.ApkFile;
import dev.aaow.rt.AndroidRuntimeContext;
import dev.aaow.rt.HostWindow;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Quản lý kho ứng dụng Android (.apk) đã cài đặt trên Windows,
 * biên dịch sẵn DEX và theo dõi các cửa sổ ứng dụng đang chạy.
 */
public final class AppRepository {
    public record InstalledApp(
            String packageName,
            String label,
            String versionName,
            int versionCode,
            int minSdk,
            int targetSdk,
            String launcherActivity,
            List<String> activities,
            List<String> permissions,
            List<String> nativeAbis,
            int resourceCount,
            Path apkPath,
            long apkSizeBytes,
            String manifestXmlDump,
            BufferedImage icon
    ) {
    }

    private final Path dataRoot;
    private final Path installedDir;
    private final Path dexCacheDir;
    private final Map<String, InstalledApp> apps = new LinkedHashMap<>();
    private final Map<String, HostWindow> runningWindows = new ConcurrentHashMap<>();
    private Consumer<String> logListener = System.out::println;

    public AppRepository(Path dataRoot, Path samplesDir) {
        this.dataRoot = dataRoot;
        this.installedDir = dataRoot.resolve("installed-apps");
        this.dexCacheDir = dataRoot.resolve("dex-cache");
        try {
            Files.createDirectories(installedDir);
            Files.createDirectories(dexCacheDir);
        } catch (IOException ignored) {
        }
        seedSamplesIfNeeded(samplesDir);
        reloadInstalledApps();
    }

    public void setLogListener(Consumer<String> listener) {
        this.logListener = listener != null ? listener : System.out::println;
    }

    private void log(String msg) {
        System.out.println(msg);
        if (logListener != null) logListener.accept(msg);
    }

    private void seedSamplesIfNeeded(Path samplesDir) {
        if (samplesDir == null || !Files.isDirectory(samplesDir)) return;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(samplesDir, "*.apk")) {
            for (Path sampleApk : ds) {
                try (ApkFile apk = new ApkFile(sampleApk)) {
                    String pkg = apk.getManifest().packageName;
                    if (pkg != null && !pkg.isEmpty()) {
                        Path target = installedDir.resolve(pkg + ".apk");
                        if (!Files.exists(target) || Files.size(target) != Files.size(sampleApk)) {
                            Files.copy(sampleApk, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (IOException ignored) {
        }
    }

    public synchronized void reloadInstalledApps() {
        apps.clear();
        if (!Files.isDirectory(installedDir)) return;
        List<InstalledApp> loaded = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(installedDir, "*.apk")) {
            for (Path p : ds) {
                try {
                    loaded.add(inspectApk(p));
                } catch (Exception e) {
                    log("[AAOW-PM] Bỏ qua file APK lỗi " + p.getFileName() + ": " + e.getMessage());
                }
            }
        } catch (IOException ignored) {
        }
        loaded.sort(Comparator.comparing(InstalledApp::label, String.CASE_INSENSITIVE_ORDER));
        for (InstalledApp app : loaded) {
            apps.put(app.packageName(), app);
        }
    }

    public Path getDexCacheDir() {
        return dexCacheDir;
    }

    public synchronized List<InstalledApp> listApps() {
        return new ArrayList<>(apps.values());
    }

    public synchronized InstalledApp getApp(String packageName) {
        return apps.get(packageName);
    }

    public boolean isRunning(String packageName) {
        return runningWindows.containsKey(packageName);
    }

    public int getRunningCount() {
        return runningWindows.size();
    }

    public synchronized InstalledApp installApk(Path sourceApk) throws IOException {
        InstalledApp inspected = inspectApk(sourceApk);
        Path target = installedDir.resolve(inspected.packageName() + ".apk");
        if (!sourceApk.toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize())) {
            Files.copy(sourceApk, target, StandardCopyOption.REPLACE_EXISTING);
        }
        InstalledApp finalApp = inspectApk(target);
        // Biên dịch sẵn DEX sang JVM bytecode ngay khi cài đặt
        try (ApkFile apk = new ApkFile(target)) {
            DexLoader.loadApkDex(apk, dexCacheDir, AppRepository.class.getClassLoader());
        }
        apps.put(finalApp.packageName(), finalApp);
        log("[AAOW-PM] Đã cài đặt: " + finalApp.label() + " (" + finalApp.packageName() + " v" + finalApp.versionName() + ")");
        return finalApp;
    }

    /**
     * Cài đặt hàng loạt tất cả file {@code *.apk} trong một thư mục (ví dụ thư mục Offline Apps).
     */
    public synchronized List<InstalledApp> installApksFromDirectory(Path dir) throws IOException {
        List<InstalledApp> installed = new ArrayList<>();
        if (dir == null || !Files.isDirectory(dir)) {
            return installed;
        }
        List<Path> apkFiles = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.apk")) {
            for (Path p : ds) {
                if (Files.isRegularFile(p)) {
                    apkFiles.add(p);
                }
            }
        }
        apkFiles.sort(Comparator.comparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
        for (Path apkPath : apkFiles) {
            try {
                installed.add(installApk(apkPath));
            } catch (Exception e) {
                log("[AAOW-PM] Bỏ qua " + apkPath.getFileName() + ": " + e.getMessage());
            }
        }
        log("[AAOW-PM] Đã nạp " + installed.size() + "/" + apkFiles.size() + " APK từ " + dir.toAbsolutePath());
        return installed;
    }

    /**
     * Tự động tìm thư mục chứa bộ APK Offline có sẵn trong workspace hoặc thư mục cài đặt AAOW.
     */
    public static Path findDefaultOfflineApkDir() {
        List<Path> candidates = new ArrayList<>();
        candidates.add(Path.of("build", "benchmark-apks"));
        candidates.add(Path.of("offline-apks"));
        try {
            Path home = WindowsIntegration.resolveAaowHome();
            if (home != null) {
                candidates.add(home.resolve("build").resolve("benchmark-apks"));
                candidates.add(home.resolve("offline-apks"));
            }
        } catch (Exception ignored) {
        }
        for (Path dir : candidates) {
            if (Files.isDirectory(dir)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.apk")) {
                    if (ds.iterator().hasNext()) {
                        return dir.toAbsolutePath().normalize();
                    }
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    public synchronized boolean uninstallApp(String packageName) {
        stopApp(packageName);
        InstalledApp removed = apps.remove(packageName);
        if (removed == null) return false;
        try {
            Files.deleteIfExists(removed.apkPath());
        } catch (IOException ignored) {
        }
        clearAppData(packageName);
        log("[AAOW-PM] Đã gỡ cài đặt: " + removed.label() + " (" + packageName + ")");
        return true;
    }

    public void clearAppData(String packageName) {
        Path pkgData = dataRoot.resolve(packageName);
        deleteRecursive(pkgData);
        log("[AAOW-PM] Đã xóa dữ liệu người dùng của: " + packageName);
    }

    private static void deleteRecursive(Path path) {
        if (path == null || !Files.exists(path)) return;
        try {
            if (Files.isDirectory(path)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
                    for (Path child : ds) deleteRecursive(child);
                }
            }
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    public void stopApp(String packageName) {
        HostWindow win = runningWindows.remove(packageName);
        if (win != null) {
            win.forceStop();
            log("[AAOW-ActivityManager] Đã dừng ứng dụng: " + packageName);
        }
    }

    public HostWindow launchApp(InstalledApp app, Runnable onStateChanged) {
        HostWindow existing = runningWindows.get(app.packageName());
        if (existing != null) {
            existing.bringToFront();
            log("[AAOW-ActivityManager] Đưa cửa sổ đang chạy lên trước: " + app.label());
            return existing;
        }
        try {
            dev.aaow.rt.NativeBridge.clearDiagnostics();
            ApkFile apk = new ApkFile(app.apkPath());
            AndroidManifest mf = apk.getManifest();
            ClassLoader cl = DexLoader.loadApkDex(apk, dexCacheDir, AppRepository.class.getClassLoader());

            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, cl, dataRoot);
            Application androidApp = createAndInitializeApplication(mf, cl, ctx, this::log);

            HostWindow host = new HostWindow(ctx, androidApp);
            host.setExitProcessOnClose(false);
            runningWindows.put(app.packageName(), host);
            if (onStateChanged != null) onStateChanged.run();

            host.setOnWindowClosedCallback(() -> {
                runningWindows.remove(app.packageName());
                try {
                    apk.close();
                } catch (IOException ignored) {
                }
                log("[AAOW-ActivityManager] Đã đóng cửa sổ ứng dụng: " + app.label());
                if (onStateChanged != null) onStateChanged.run();
            });

            log("[AAOW-ActivityManager] Khởi chạy " + app.label() + " -> " + app.launcherActivity());
            host.showWindow(app.launcherActivity());
            return host;
        } catch (Throwable e) {
            log("[AAOW-ERROR] Lỗi mở " + app.label() + ": " + e);
            e.printStackTrace();
            return null;
        }
    }

    public static Application createAndInitializeApplication(
            AndroidManifest mf, ClassLoader cl, AndroidRuntimeContext ctx, java.util.function.Consumer<String> warnLog) {
        java.util.concurrent.atomic.AtomicReference<Application> ref = new java.util.concurrent.atomic.AtomicReference<>();
        Runnable initTask = () -> {
            ClassLoader prevCl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(cl);
            try {
                // 1. Khởi tạo sớm Process.myProcessName() / ActivityThread trước khi nạp lớp Application
                if (mf.packageName != null && !mf.packageName.isEmpty()) {
                    android.app.ActivityThread.setPackageName(mf.packageName);
                    android.app.ActivityThread.setProcessName(mf.packageName);
                }

                // 2. Tạo thực thể Application và pre-attach base context để FirebaseInitProvider sớm
                //    nhìn thấy getApplicationContext() là một android.app.Application hợp lệ
                Application app = null;
                if (mf.applicationClass != null && !mf.applicationClass.isEmpty()) {
                    try {
                        Class<?> cls = Class.forName(mf.applicationClass, true, cl);
                        if (Application.class.isAssignableFrom(cls)) {
                            app = (Application) cls.getDeclaredConstructor().newInstance();
                        }
                    } catch (Throwable t) {
                        if (warnLog != null) {
                            warnLog.accept("Không khởi tạo được Application tuỳ chỉnh (" + mf.applicationClass + "), dùng mặc định: " + t);
                        }
                    }
                }
                if (app == null) {
                    app = new Application();
                }
                app.preAttachBaseContext(ctx);
                ctx.setApplicationContext(app);

                // 3. Khởi tạo sớm FirebaseInitProvider / FirebaseApp TRƯỚC Application.attachBaseContext()
                ctx.installEarlyFirebaseProviders();

                // 4. Chạy vòng đời attachBaseContext() của Application (Tinker/CrabShell/MultiDex)
                try {
                    app.attach(ctx);
                } catch (Throwable t) {
                    if (warnLog != null) {
                        warnLog.accept("Lỗi trong Application.attachBaseContext (" + app.getClass().getName() + "): " + t);
                    }
                }
                ctx.setApplicationContext(app);
                ctx.installManifestContentProviders();
                try {
                    app.onCreate();
                } catch (Throwable t) {
                    if (warnLog != null) {
                        warnLog.accept("[AAOW-Warn] Application.onCreate() gặp ngoại lệ (đã bỏ qua để tiếp tục mở Activity): " + t);
                    }
                }
                ref.set(app);
            } finally {
                Thread.currentThread().setContextClassLoader(prevCl);
            }
        };

        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            initTask.run();
        } else {
            try {
                javax.swing.SwingUtilities.invokeAndWait(initTask);
            } catch (Exception e) {
                initTask.run();
            }
        }
        return ref.get();
    }

    private InstalledApp inspectApk(Path apkPath) throws IOException {
        try (ApkFile apk = new ApkFile(apkPath)) {
            AndroidManifest mf = apk.getManifest();
            Resources res = new Resources(apk);
            String pkg = mf.packageName != null && !mf.packageName.isEmpty()
                    ? mf.packageName
                    : apkPath.getFileName().toString().replace(".apk", "");
            String label = mf.label != null && !mf.label.isEmpty() ? mf.label : pkg;
            String verName = mf.versionName != null && !mf.versionName.isEmpty() ? mf.versionName : "1.0";

            List<String> acts = new ArrayList<>();
            for (AndroidManifest.Activity a : mf.activities) {
                acts.add(a.toString());
            }

            BufferedImage iconImg = loadOrRenderAppIcon(apkPath, res, mf.iconRes, label, pkg);
            String xmlDump = mf.xml != null ? mf.xml.dump() : "";
            long fileSize = Files.size(apkPath);

            return new InstalledApp(
                    pkg,
                    label,
                    verName,
                    mf.versionCode,
                    mf.minSdk,
                    mf.targetSdk,
                    mf.launcherActivity,
                    acts,
                    new ArrayList<>(mf.permissions),
                    apk.nativeAbis(),
                    apk.getResources().size(),
                    apkPath,
                    fileSize,
                    xmlDump,
                    iconImg
            );
        }
    }

    private BufferedImage loadOrRenderAppIcon(Path apkPath, Resources res, int iconRes, String label, String pkg) {
        try {
            long size = Files.size(apkPath);
            long mtime = Files.getLastModifiedTime(apkPath).toMillis();
            Path iconsDir = dexCacheDir.resolve("icons");
            Path cacheFile = iconsDir.resolve(pkg.replaceAll("[^a-zA-Z0-9._-]", "_") + "-" + size + "-" + mtime + ".png");
            if (Files.isRegularFile(cacheFile)) {
                BufferedImage cached = ImageIO.read(cacheFile.toFile());
                if (cached != null) return cached;
            }
            BufferedImage rendered = renderAppIcon(res, iconRes, label, pkg);
            Files.createDirectories(iconsDir);
            ImageIO.write(rendered, "png", cacheFile.toFile());
            return rendered;
        } catch (Exception ignored) {
            return renderAppIcon(res, iconRes, label, pkg);
        }
    }

    private static BufferedImage renderAppIcon(Resources res, int iconRes, String label, String pkg) {
        int size = 64;
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int hash = Math.abs(pkg.hashCode());
        Color c1 = new Color(Color.HSBtoRGB((hash % 360) / 360f, 0.65f, 0.78f));
        Color c2 = new Color(Color.HSBtoRGB(((hash + 40) % 360) / 360f, 0.75f, 0.55f));
        g.setPaint(new GradientPaint(0, 0, c1, size, size, c2));
        g.fill(new RoundRectangle2D.Float(0, 0, size, size, 18, 18));

        boolean drewIcon = false;
        if (iconRes != 0) {
            Bitmap bmp = res.loadBitmap(iconRes);
            if (bmp != null) {
                g.drawImage(bmp.getAwtImage(), 10, 10, size - 20, size - 20, null);
                drewIcon = true;
            }
        }
        if (!drewIcon) {
            String initials = label.isEmpty() ? "A" : label.substring(0, 1).toUpperCase();
            g.setColor(Color.WHITE);
            g.setFont(new Font("Segoe UI", Font.BOLD, 28));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(initials, (size - fm.stringWidth(initials)) / 2, (size + fm.getAscent() - fm.getDescent()) / 2);
        }
        g.dispose();
        return out;
    }
}
