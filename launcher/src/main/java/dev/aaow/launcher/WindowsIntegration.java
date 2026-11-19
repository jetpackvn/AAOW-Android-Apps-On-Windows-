package dev.aaow.launcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tích hợp hệ điều hành Windows cho AAOW:
 * - Tự động xác định thư mục chạy (dev workspace vs bản cài đặt jpackage AAOW.exe) và thư mục dữ liệu ghi được.
 * - Đăng ký / gỡ liên kết đuôi file `.apk` dưới HKCU\Software\Classes (không cần quyền Admin)
 *   để người dùng nhấp đúp file `.apk` trong Windows Explorer là mở hoặc cài trực tiếp bằng AAOW.
 */
public final class WindowsIntegration {
    public static final String PROG_ID = "AAOW.ApkPackage";
    public static final String HKCU_CLASSES_PREFIX = "HKCU\\Software\\Classes";
    public static final String MIME_TYPE_APK = "application/vnd.android.package-archive";

    private WindowsIntegration() {
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public static Path resolveAaowHome() {
        String env = System.getenv("AAOW_HOME");
        if (env != null && !env.isBlank()) {
            Path p = Path.of(env);
            if (Files.isDirectory(p)) return p;
        }
        String jpkgAppPath = System.getProperty("jpackage.app-path");
        if (jpkgAppPath != null && !jpkgAppPath.isBlank()) {
            Path exeDir = Path.of(jpkgAppPath).toAbsolutePath().getParent();
            if (exeDir != null && Files.isDirectory(exeDir)) return exeDir;
        }
        try {
            var cs = WindowsIntegration.class.getProtectionDomain().getCodeSource();
            if (cs != null && cs.getLocation() != null) {
                Path loc = Path.of(cs.getLocation().toURI()).toAbsolutePath();
                Path dir = Files.isDirectory(loc) ? loc : loc.getParent();
                // Nếu đang chạy từ launcher/build/install/aaow/lib -> đi lên gốc nếu có settings.gradle.kts
                Path cur = dir;
                for (int i = 0; i < 6 && cur != null; i++) {
                    if (Files.exists(cur.resolve("settings.gradle.kts"))) {
                        return cur;
                    }
                    if (Files.exists(cur.resolve("AAOW.exe"))) {
                        return cur;
                    }
                    cur = cur.getParent();
                }
            }
        } catch (Exception ignored) {
        }
        return Path.of("").toAbsolutePath();
    }

    public static Path resolveDataRoot(Path home, Path cliOverride) {
        if (cliOverride != null) {
            return cliOverride.toAbsolutePath();
        }
        String prop = System.getProperty("aaow.data.dir");
        if (prop != null && !prop.isBlank()) {
            return Path.of(prop).toAbsolutePath();
        }
        String envHome = System.getenv("AAOW_HOME");
        boolean inDevWorkspace = (envHome != null && !envHome.isBlank())
                || (home != null && Files.exists(home.resolve("settings.gradle.kts")));
        if (inDevWorkspace && home != null) {
            return home.resolve("build").resolve("aaow-data");
        }
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "AAOW", "data");
        }
        return Path.of(System.getProperty("user.home", "."), ".aaow", "data");
    }

    public static Path resolveSamplesDir(Path home) {
        if (home != null) {
            Path direct = home.resolve("samples");
            if (Files.isDirectory(direct)) return direct;
            Path inApp = home.resolve("app").resolve("samples");
            if (Files.isDirectory(inApp)) return inApp;
        }
        Path cwdSamples = Path.of("samples").toAbsolutePath();
        if (Files.isDirectory(cwdSamples)) return cwdSamples;
        return home != null ? home.resolve("samples") : cwdSamples;
    }

    public static Path resolveSelfLauncher(Path home) {
        String jpkgAppPath = System.getProperty("jpackage.app-path");
        if (jpkgAppPath != null && !jpkgAppPath.isBlank()) {
            Path p = Path.of(jpkgAppPath).toAbsolutePath();
            if (Files.exists(p)) return p;
        }
        if (home != null) {
            Path exe = home.resolve("AAOW.exe");
            if (Files.exists(exe)) return exe;
            Path bat = home.resolve("aaow.bat");
            if (Files.exists(bat)) return bat;
        }
        return Path.of("aaow.bat").toAbsolutePath();
    }

    public static Path resolveApkIconPath(Path home) {
        if (home != null) {
            Path ico1 = home.resolve("apk-file.ico");
            if (Files.exists(ico1)) return ico1;
            Path ico2 = home.resolve("app").resolve("apk-file.ico");
            if (Files.exists(ico2)) return ico2;
            Path ico3 = home.resolve("packaging").resolve("apk-file.ico");
            if (Files.exists(ico3)) return ico3;
        }
        return null;
    }

    /**
     * Tạo danh sách lệnh `reg.exe add` để liên kết đuôi `.apk` vào `regPrefix` (mặc định HKCU\Software\Classes).
     */
    public static List<List<String>> buildAssociationCommands(String regPrefix, Path launcherPath, Path iconPath) {
        String prefix = normalizePrefix(regPrefix);
        String launcher = launcherPath.toAbsolutePath().toString();
        String openCmd = "\"" + launcher + "\" \"%1\"";
        String installCmd = "\"" + launcher + "\" --install \"%1\"";
        String iconVal = iconPath != null
                ? iconPath.toAbsolutePath().toString()
                : (launcher.toLowerCase(Locale.ROOT).endsWith(".exe") ? launcher + ",0" : "");

        List<List<String>> cmds = new ArrayList<>();
        cmds.add(List.of("reg.exe", "add", prefix + "\\.apk", "/ve", "/d", PROG_ID, "/f"));
        cmds.add(List.of("reg.exe", "add", prefix + "\\.apk", "/v", "Content Type", "/d", MIME_TYPE_APK, "/f"));
        cmds.add(List.of("reg.exe", "add", prefix + "\\" + PROG_ID, "/ve", "/d", "Android Application Package (AAOW)", "/f"));
        if (!iconVal.isEmpty()) {
            cmds.add(List.of("reg.exe", "add", prefix + "\\" + PROG_ID + "\\DefaultIcon", "/ve", "/d", iconVal, "/f"));
        }
        cmds.add(List.of("reg.exe", "add", prefix + "\\" + PROG_ID + "\\shell\\open", "/ve", "/d", "Mở bằng AAOW", "/f"));
        cmds.add(List.of("reg.exe", "add", prefix + "\\" + PROG_ID + "\\shell\\open\\command", "/ve", "/d", openCmd, "/f"));
        cmds.add(List.of("reg.exe", "add", prefix + "\\" + PROG_ID + "\\shell\\install", "/ve", "/d", "Cài vào AAOW App Hub", "/f"));
        cmds.add(List.of("reg.exe", "add", prefix + "\\" + PROG_ID + "\\shell\\install\\command", "/ve", "/d", installCmd, "/f"));
        return cmds;
    }

    public static String generateRegFileContent(Path launcherPath, Path iconPath) {
        return generateRegFileContent(HKCU_CLASSES_PREFIX, launcherPath, iconPath);
    }

    public static String generateRegFileContent(String regPrefix, Path launcherPath, Path iconPath) {
        String root = toRegRootName(normalizePrefix(regPrefix));
        String launcher = escapeReg(launcherPath.toAbsolutePath().toString());
        String iconVal = iconPath != null
                ? escapeReg(iconPath.toAbsolutePath().toString())
                : (launcherPath.toString().toLowerCase(Locale.ROOT).endsWith(".exe") ? launcher + ",0" : "");
        StringBuilder sb = new StringBuilder();
        sb.append("Windows Registry Editor Version 5.00\r\n\r\n");
        sb.append("[").append(root).append("\\.apk]\r\n");
        sb.append("@=\"").append(PROG_ID).append("\"\r\n");
        sb.append("\"Content Type\"=\"").append(MIME_TYPE_APK).append("\"\r\n\r\n");
        sb.append("[").append(root).append("\\").append(PROG_ID).append("]\r\n");
        sb.append("@=\"Android Application Package (AAOW)\"\r\n\r\n");
        if (!iconVal.isEmpty()) {
            sb.append("[").append(root).append("\\").append(PROG_ID).append("\\DefaultIcon]\r\n");
            sb.append("@=\"").append(iconVal).append("\"\r\n\r\n");
        }
        sb.append("[").append(root).append("\\").append(PROG_ID).append("\\shell\\open]\r\n");
        sb.append("@=\"Mở bằng AAOW\"\r\n\r\n");
        sb.append("[").append(root).append("\\").append(PROG_ID).append("\\shell\\open\\command]\r\n");
        sb.append("@=\"\\\"").append(launcher).append("\\\" \\\"%1\\\"\"\r\n\r\n");
        sb.append("[").append(root).append("\\").append(PROG_ID).append("\\shell\\install]\r\n");
        sb.append("@=\"Cài vào AAOW App Hub\"\r\n\r\n");
        sb.append("[").append(root).append("\\").append(PROG_ID).append("\\shell\\install\\command]\r\n");
        sb.append("@=\"\\\"").append(launcher).append("\\\" --install \\\"%1\\\"\"\r\n");
        return sb.toString();
    }

    public static boolean registerApkAssociation(Path launcherPath, Path iconPath) throws IOException {
        return registerApkAssociation(HKCU_CLASSES_PREFIX, launcherPath, iconPath);
    }

    public static boolean registerApkAssociation(String regPrefix, Path launcherPath, Path iconPath) throws IOException {
        if (!isWindows()) return false;
        String regBody = generateRegFileContent(regPrefix, launcherPath, iconPath);
        Path tmpReg = Files.createTempFile("aaow-assoc-", ".reg");
        try {
            byte[] utf16 = regBody.getBytes(StandardCharsets.UTF_16LE);
            byte[] withBom = new byte[2 + utf16.length];
            withBom[0] = (byte) 0xFF;
            withBom[1] = (byte) 0xFE;
            System.arraycopy(utf16, 0, withBom, 2, utf16.length);
            Files.write(tmpReg, withBom);
            ExecResult res = runProcess(List.of("reg.exe", "import", tmpReg.toAbsolutePath().toString()));
            if (res.exitCode() != 0) {
                throw new IOException("Lỗi nhập Registry (.reg): " + res.output());
            }
        } finally {
            Files.deleteIfExists(tmpReg);
        }
        if (HKCU_CLASSES_PREFIX.equalsIgnoreCase(normalizePrefix(regPrefix))) {
            notifyShellAssociationChanged();
        }
        return true;
    }

    public static boolean unregisterApkAssociation() throws IOException {
        return unregisterApkAssociation(HKCU_CLASSES_PREFIX);
    }

    public static boolean unregisterApkAssociation(String regPrefix) throws IOException {
        if (!isWindows()) return false;
        String prefix = normalizePrefix(regPrefix);
        ExecResult q = runProcess(List.of("reg.exe", "query", prefix + "\\.apk", "/ve"));
        if (q.exitCode() == 0 && q.output().contains(PROG_ID)) {
            runProcess(List.of("reg.exe", "delete", prefix + "\\.apk", "/f"));
        }
        runProcess(List.of("reg.exe", "delete", prefix + "\\" + PROG_ID, "/f"));
        if (HKCU_CLASSES_PREFIX.equalsIgnoreCase(prefix)) {
            notifyShellAssociationChanged();
        }
        return true;
    }

    public static boolean isApkAssociationRegistered() {
        return isApkAssociationRegistered(HKCU_CLASSES_PREFIX);
    }

    public static boolean isApkAssociationRegistered(String regPrefix) {
        if (!isWindows()) return false;
        try {
            String prefix = normalizePrefix(regPrefix);
            ExecResult extQuery = runProcess(List.of("reg.exe", "query", prefix + "\\.apk", "/ve"));
            if (extQuery.exitCode() != 0 || !extQuery.output().contains(PROG_ID)) {
                return false;
            }
            ExecResult cmdQuery = runProcess(List.of("reg.exe", "query", prefix + "\\" + PROG_ID + "\\shell\\open\\command", "/ve"));
            return cmdQuery.exitCode() == 0 && cmdQuery.output().contains("%1");
        } catch (IOException e) {
            return false;
        }
    }

    private static void notifyShellAssociationChanged() {
        try {
            runProcess(List.of("ie4uinit.exe", "-show"));
        } catch (Exception ignored) {
        }
    }

    private static String normalizePrefix(String p) {
        if (p == null || p.isBlank()) return HKCU_CLASSES_PREFIX;
        return p.endsWith("\\") ? p.substring(0, p.length() - 1) : p;
    }

    private static String toRegRootName(String prefix) {
        if (prefix.startsWith("HKCU\\")) {
            return "HKEY_CURRENT_USER\\" + prefix.substring(5);
        }
        if ("HKCU".equals(prefix)) {
            return "HKEY_CURRENT_USER";
        }
        return prefix;
    }

    private static String escapeReg(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record ExecResult(int exitCode, String output) {
    }

    private static ExecResult runProcess(List<String> command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        byte[] bytes = p.getInputStream().readAllBytes();
        try {
            int code = p.waitFor();
            return new ExecResult(code, new String(bytes, StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted running " + command.get(0), e);
        }
    }
}
