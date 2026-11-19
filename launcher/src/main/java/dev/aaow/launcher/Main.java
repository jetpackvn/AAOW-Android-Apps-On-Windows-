package dev.aaow.launcher;

import android.app.Application;
import android.os.Environment;
import android.util.Log;
import dev.aaow.apk.AndroidManifest;
import dev.aaow.apk.ApkFile;
import dev.aaow.rt.AndroidRuntimeContext;
import dev.aaow.rt.HostWindow;

import javax.swing.UIManager;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Điểm vào chính của AAOW (Android Apps On Windows).
 */
public final class Main {
    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        Path cliDataDir = null;
        List<String> filtered = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if ("--data-dir".equals(args[i]) && i + 1 < args.length) {
                cliDataDir = Path.of(args[++i]);
            } else {
                filtered.add(args[i]);
            }
        }
        String[] effectiveArgs = filtered.toArray(String[]::new);

        Path home = WindowsIntegration.resolveAaowHome();
        Path dataRoot = WindowsIntegration.resolveDataRoot(home, cliDataDir);
        Path samplesDir = WindowsIntegration.resolveSamplesDir(home);
        Environment.setDataRoot(dataRoot);

        if (effectiveArgs.length >= 1 && ("--help".equals(effectiveArgs[0]) || "-h".equals(effectiveArgs[0]))) {
            printUsage();
            return;
        }

        if (effectiveArgs.length >= 1 && "--associate-apk".equals(effectiveArgs[0])) {
            Path launcher = WindowsIntegration.resolveSelfLauncher(home);
            Path icon = WindowsIntegration.resolveApkIconPath(home);
            boolean ok = WindowsIntegration.registerApkAssociation(launcher, icon);
            System.out.println(ok
                    ? "Đã liên kết đuôi .apk -> " + launcher.toAbsolutePath()
                    : "Chức năng liên kết đuôi .apk chỉ áp dụng trên Windows.");
            return;
        }

        if (effectiveArgs.length >= 1 && "--unassociate-apk".equals(effectiveArgs[0])) {
            boolean ok = WindowsIntegration.unregisterApkAssociation();
            System.out.println(ok
                    ? "Đã gỡ liên kết đuôi .apk khỏi Windows Explorer."
                    : "Chức năng liên kết đuôi .apk chỉ áp dụng trên Windows.");
            return;
        }

        if (effectiveArgs.length >= 1 && "--check-associate-apk".equals(effectiveArgs[0])) {
            System.out.println(WindowsIntegration.isApkAssociationRegistered() ? "REGISTERED" : "NOT_REGISTERED");
            return;
        }

        if (effectiveArgs.length >= 2 && ("--install".equals(effectiveArgs[0]) || "--install-silent".equals(effectiveArgs[0]))) {
            boolean silent = "--install-silent".equals(effectiveArgs[0]);
            Path apkPath = Path.of(effectiveArgs[1]);
            if (!Files.exists(apkPath)) {
                System.err.println("Không tìm thấy file/thư mục APK: " + apkPath.toAbsolutePath());
                System.exit(1);
            }
            AppRepository repo = new AppRepository(dataRoot, samplesDir);
            AppRepository.InstalledApp selected = null;
            if (Files.isDirectory(apkPath)) {
                List<AppRepository.InstalledApp> list = repo.installApksFromDirectory(apkPath);
                System.out.println("Đã cài đặt " + list.size() + " APK từ thư mục: " + apkPath.toAbsolutePath());
                if (!list.isEmpty()) selected = list.get(0);
            } else {
                selected = repo.installApk(apkPath);
                System.out.println("Đã cài đặt vào AAOW: " + selected.label() + " (" + selected.packageName() + ")");
            }
            if (!silent) {
                AppHubWindow hub = new AppHubWindow(repo);
                if (selected != null) hub.selectApp(selected);
                hub.show();
            }
            return;
        }

        // Mặc định hoặc --hub: Mở giao diện AAOW App Hub
        if (effectiveArgs.length == 0 || (effectiveArgs.length == 1 && "--hub".equals(effectiveArgs[0]))) {
            AppRepository repo = new AppRepository(dataRoot, samplesDir);
            new AppHubWindow(repo).show();
            return;
        }

        if (effectiveArgs.length >= 2 && "--screenshot-hub".equals(effectiveArgs[0])) {
            try {
                AppRepository repo = new AppRepository(dataRoot, samplesDir);
                File out = new File(effectiveArgs[1]);
                new AppHubWindow(repo).renderScreenshot(out, 1080, 700);
                Log.i("AAOW", "Đã xuất ảnh chụp giao diện AAOW App Hub ra: " + out.getAbsolutePath());
                System.exit(0);
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
            return;
        }

        if (effectiveArgs.length >= 2 && "--scan".equals(effectiveArgs[0])) {
            Path apkPath = Path.of(effectiveArgs[1]);
            if (!Files.exists(apkPath)) {
                System.err.println("Không tìm thấy file APK: " + apkPath.toAbsolutePath());
                System.exit(1);
            }
            ApkCompatibilityScanner.ScanReport report = ApkCompatibilityScanner.scan(apkPath, dataRoot.resolve("dex-cache"));
            System.out.print(report.formatReport());
            return;
        }

        if (effectiveArgs.length >= 2 && "--info".equals(effectiveArgs[0])) {
            printApkInfo(Path.of(effectiveArgs[1]), effectiveArgs.length >= 3 && "--xml".equals(effectiveArgs[2]));
            return;
        }

        if (effectiveArgs.length >= 3 && "--screenshot".equals(effectiveArgs[0])) {
            Path apkPath = Path.of(effectiveArgs[1]);
            File outFile = new File(effectiveArgs[2]);
            String actOverride = effectiveArgs.length >= 4 ? effectiveArgs[3] : null;
            runApk(apkPath, dataRoot, actOverride, outFile);
            return;
        }

        Path apkPath = Path.of(effectiveArgs[0]);
        String activityOverride = effectiveArgs.length >= 2 ? effectiveArgs[1] : null;
        runApk(apkPath, dataRoot, activityOverride, null);
    }

    private static void printUsage() {
        System.out.println("""
                AAOW — Android Apps On Windows (Compatibility Layer)
                Cách dùng:
                  aaow.bat                                     Mở AAOW App Hub
                  aaow.bat <file.apk> [ten.Activity]           Chạy trực tiếp một file APK
                  aaow.bat --install <file.apk>                Cài đặt APK vào kho và mở App Hub
                  aaow.bat --scan <file.apk>                   Quét độ tương thích API tĩnh của APK
                  aaow.bat --info <file.apk> [--xml]           Xem thông tin & giải mã AndroidManifest
                  aaow.bat --associate-apk / --unassociate-apk Đăng ký / gỡ liên kết nhấp đúp file .apk
                  aaow.bat --screenshot <apk> <out.png> [Act]  Chụp ảnh màn hình APK tự động
                  aaow.bat --screenshot-hub <out.png>          Chụp ảnh màn hình App Hub
                  aaow.bat --data-dir <thu-muc>                Chỉ định thư mục dữ liệu tùy chọn
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
        Application app = AppRepository.createAndInitializeApplication(
                mf, apkClassLoader, runtimeContext,
                msg -> Log.w("AAOW", msg));

        String targetActivity = activityOverride != null ? activityOverride : mf.launcherActivity;
        if (targetActivity == null) {
            throw new IllegalStateException("APK không khai báo Launcher Activity trong AndroidManifest.xml");
        }

        HostWindow host = new HostWindow(runtimeContext, app);
        if (screenshotOut != null) {
            try {
                host.renderHeadlessScreenshot(targetActivity, screenshotOut, 420, 820);
                Log.i("AAOW", "Đã xuất ảnh chụp giao diện APK ra: " + screenshotOut.getAbsolutePath());
                apk.close();
                System.exit(0);
            } catch (Throwable t) {
                t.printStackTrace();
                try {
                    apk.close();
                } catch (Exception ignored) {
                }
                System.exit(1);
            }
        } else {
            host.showWindow(targetActivity);
        }
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
