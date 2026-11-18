package dev.aaow.launcher;

import android.app.Application;
import android.util.Log;
import dev.aaow.apk.AndroidManifest;
import dev.aaow.apk.ApkFile;
import dev.aaow.rt.AndroidRuntimeContext;
import dev.aaow.rt.HostWindow;

import javax.swing.UIManager;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Điểm vào chính của AAOW (Android Apps On Windows).
 */
public final class Main {
    private static Path resolveAaowHome() {
        String env = System.getenv("AAOW_HOME");
        if (env != null && !env.isBlank()) {
            Path p = Path.of(env);
            if (Files.isDirectory(p)) return p;
        }
        return Path.of("").toAbsolutePath();
    }

    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        Path home = resolveAaowHome();
        Path dataRoot = home.resolve("build").resolve("aaow-data");
        Path samplesDir = home.resolve("samples");

        if (args.length >= 1 && ("--help".equals(args[0]) || "-h".equals(args[0]))) {
            printUsage();
            return;
        }

        // Mặc định hoặc --hub: Mở giao diện AAOW App Hub
        if (args.length == 0 || (args.length == 1 && "--hub".equals(args[0]))) {
            AppRepository repo = new AppRepository(dataRoot, samplesDir);
            new AppHubWindow(repo).show();
            return;
        }

        if (args.length >= 2 && "--screenshot-hub".equals(args[0])) {
            AppRepository repo = new AppRepository(dataRoot, samplesDir);
            File out = new File(args[1]);
            new AppHubWindow(repo).renderScreenshot(out, 1080, 700);
            Log.i("AAOW", "Đã xuất ảnh chụp giao diện AAOW App Hub ra: " + out.getAbsolutePath());
            return;
        }

        if (args.length >= 2 && "--scan".equals(args[0])) {
            Path apkPath = Path.of(args[1]);
            if (!Files.exists(apkPath)) {
                System.err.println("Không tìm thấy file APK: " + apkPath.toAbsolutePath());
                System.exit(1);
            }
            ApkCompatibilityScanner.ScanReport report = ApkCompatibilityScanner.scan(apkPath, dataRoot.resolve("dex-cache"));
            System.out.print(report.formatReport());
            return;
        }

        if (args.length >= 2 && "--info".equals(args[0])) {
            printApkInfo(Path.of(args[1]), args.length >= 3 && "--xml".equals(args[2]));
            return;
        }

        if (args.length >= 3 && "--screenshot".equals(args[0])) {
            Path apkPath = Path.of(args[1]);
            File outFile = new File(args[2]);
            String actOverride = args.length >= 4 ? args[3] : null;
            runApk(apkPath, dataRoot, actOverride, outFile);
            return;
        }

        Path apkPath = Path.of(args[0]);
        String activityOverride = args.length >= 2 ? args[1] : null;
        runApk(apkPath, dataRoot, activityOverride, null);
    }

    private static void printUsage() {
        System.out.println("""
                AAOW — Android Apps On Windows (Compatibility Layer)
                Cách dùng:
                  aaow.bat                                     Mở AAOW App Hub
                  aaow.bat <file.apk> [ten.Activity]           Chạy trực tiếp một file APK
                  aaow.bat --scan <file.apk>                   Quét độ tương thích API tĩnh của APK
                  aaow.bat --info <file.apk> [--xml]           Xem thông tin & giải mã AndroidManifest
                  aaow.bat --screenshot <apk> <out.png> [Act]  Chụp ảnh màn hình APK tự động
                  aaow.bat --screenshot-hub <out.png>          Chụp ảnh màn hình App Hub
                  aaow.bat --rebuild [tham-so-khac...]         Biên dịch lại bản chạy nhanh installDist
                """);
    }

    private static void runApk(Path apkPath, Path dataRoot, String activityOverride, File screenshotOut) throws Exception {
        if (!Files.exists(apkPath)) {
            System.err.println("Không tìm thấy file APK: " + apkPath.toAbsolutePath());
            System.exit(1);
        }

        ApkFile apk = new ApkFile(apkPath);
        AndroidManifest mf = apk.getManifest();
        List<String> abis = apk.nativeAbis();

        System.out.println("============================================================");
        System.out.println(" AAOW — Android Apps On Windows (Compatibility Layer)");
        System.out.println("============================================================");
        System.out.println(" APK         : " + apkPath.getFileName());
        System.out.println(" Package     : " + mf.packageName);
        System.out.println(" Label       : " + (mf.label != null ? mf.label : "<none>"));
        System.out.println(" Version     : " + mf.versionName + " (code " + mf.versionCode + ")");
        System.out.println(" SDK         : minSdk=" + mf.minSdk + ", targetSdk=" + mf.targetSdk);
        System.out.println(" Resources   : " + apk.getResources().size() + " mục");
        System.out.println(" Native ABIs : " + (abis.isEmpty() ? "Thuần Java/Kotlin (không cần .so)" : abis));
        System.out.println(" Activity    : " + (activityOverride != null ? activityOverride : mf.launcherActivity));
        System.out.println("============================================================");

        ClassLoader apkClassLoader = DexLoader.loadApkDex(apk, dataRoot.resolve("dex-cache"), Main.class.getClassLoader());
        Thread.currentThread().setContextClassLoader(apkClassLoader);

        AndroidRuntimeContext runtimeContext = new AndroidRuntimeContext(apk, apkClassLoader, dataRoot);
        Application app = createApplication(mf, apkClassLoader, runtimeContext);
        runtimeContext.setApplicationContext(app);
        try {
            app.onCreate();
        } catch (Throwable t) {
            Log.w("AAOW", "Application.onCreate() gặp ngoại lệ (đã bỏ qua để tiếp tục mở Activity): " + t);
        }

        String targetActivity = activityOverride != null ? activityOverride : mf.launcherActivity;
        if (targetActivity == null) {
            throw new IllegalStateException("APK không khai báo Launcher Activity trong AndroidManifest.xml");
        }

        HostWindow host = new HostWindow(runtimeContext, app);
        if (screenshotOut != null) {
            host.renderHeadlessScreenshot(targetActivity, screenshotOut, 420, 820);
            Log.i("AAOW", "Đã xuất ảnh chụp giao diện APK ra: " + screenshotOut.getAbsolutePath());
            apk.close();
            System.exit(0);
        } else {
            host.showWindow(targetActivity);
        }
    }

    private static Application createApplication(AndroidManifest mf, ClassLoader cl, AndroidRuntimeContext ctx) {
        if (mf.applicationClass != null && !mf.applicationClass.isEmpty()) {
            try {
                Class<?> cls = Class.forName(mf.applicationClass, true, cl);
                if (Application.class.isAssignableFrom(cls)) {
                    Application app = (Application) cls.getDeclaredConstructor().newInstance();
                    app.attach(ctx);
                    return app;
                }
            } catch (Throwable t) {
                Log.w("AAOW", "Không khởi tạo được Application tuỳ chỉnh (" + mf.applicationClass + "), dùng mặc định: " + t);
            }
        }
        Application app = new Application();
        app.attach(ctx);
        return app;
    }

    private static void printApkInfo(Path apkPath, boolean dumpXml) throws Exception {
        try (ApkFile apk = new ApkFile(apkPath)) {
            AndroidManifest mf = apk.getManifest();
            System.out.println("=== THÔNG TIN APK ===");
            System.out.println("File        : " + apkPath.toAbsolutePath());
            System.out.println("Package     : " + mf.packageName);
            System.out.println("Label       : " + mf.label);
            System.out.println("Version     : " + mf.versionName + " (" + mf.versionCode + ")");
            System.out.println("SDK         : min=" + mf.minSdk + ", target=" + mf.targetSdk);
            System.out.println("Application : " + mf.applicationClass);
            System.out.println("Launcher    : " + mf.launcherActivity);
            System.out.println("DEX files   : " + apk.readDexFiles().size());
            System.out.println("Resources   : " + apk.getResources().size());
            System.out.println("Native ABIs : " + apk.nativeAbis());
            System.out.println("Permissions : " + mf.permissions);
            System.out.println("Activities  :");
            for (AndroidManifest.Activity a : mf.activities) {
                System.out.println("  - " + a);
            }
            if (dumpXml && mf.xml != null) {
                System.out.println("\n=== ANDROIDMANIFEST.XML (ĐÃ GIẢI MÃ AXML) ===");
                System.out.print(mf.xml.dump());
            }
        }
    }
}
