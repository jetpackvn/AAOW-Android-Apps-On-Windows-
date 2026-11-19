package dev.aaow.rt;

import android.app.ActivityThread;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Cầu nối giả lập hệ thống tệp ảo Linux {@code /proc/...} trên Windows cho các APK và SDK Android
 * (như {@code ProcessUtils.getMyProcessName()}, {@code QDDownloader}, {@code AegonApplication})
 * khi đọc {@code /proc/<pid>/cmdline}, {@code /proc/self/cmdline}, {@code /proc/cpuinfo}, {@code /proc/meminfo}.
 */
public final class LinuxProcFsBridge {
    private static volatile Path procDir;
    private static volatile Path cmdlineFile;
    private static volatile Path cpuInfoFile;
    private static volatile Path memInfoFile;
    private static volatile String configuredProcessName;

    private LinuxProcFsBridge() {
    }

    /** Khởi tạo tệp ảo {@code proc_cmdline} (và procfs cơ bản) ngay khi tạo phiên chạy APK. */
    public static synchronized void configure(String processName, Path dataDir) {
        String proc = (processName != null && !processName.isEmpty())
                ? processName
                : ActivityThread.currentProcessName();
        configuredProcessName = proc;
        Path dir = dataDir != null
                ? dataDir.resolve("aaow-proc")
                : Path.of(System.getProperty("java.io.tmpdir"), "aaow-proc");
        procDir = dir;
        try {
            Files.createDirectories(dir);
            cmdlineFile = dir.resolve("cmdline");
            Files.writeString(cmdlineFile, proc + "\0", StandardCharsets.UTF_8);

            cpuInfoFile = dir.resolve("cpuinfo");
            if (!Files.exists(cpuInfoFile)) {
                int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < cores; i++) {
                    sb.append("processor\t: ").append(i).append('\n');
                    sb.append("BogoMIPS\t: 100.00\n");
                    sb.append("Features\t: fp asimd evtstrm aes pmull sha1 sha2 crc32\n");
                    sb.append("CPU implementer\t: 0x41\n");
                    sb.append("CPU architecture: 8\n\n");
                }
                sb.append("Hardware\t: AAOW Virtual SoC\n");
                Files.writeString(cpuInfoFile, sb.toString(), StandardCharsets.UTF_8);
            }

            memInfoFile = dir.resolve("meminfo");
            if (!Files.exists(memInfoFile)) {
                long totalKb = Math.max(2048L * 1024L, Runtime.getRuntime().maxMemory() / 1024L);
                long freeKb = Math.max(512L * 1024L, Runtime.getRuntime().freeMemory() / 1024L);
                String mem = "MemTotal:       " + totalKb + " kB\n"
                        + "MemFree:        " + freeKb + " kB\n"
                        + "MemAvailable:   " + freeKb + " kB\n";
                Files.writeString(memInfoFile, mem, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }
    }

    /** Chuyển hướng đường dẫn {@code /proc/...} sang tệp thật trên Windows; giữ nguyên mọi đường dẫn khác. */
    public static String remapPath(String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) {
            return rawPath;
        }
        String norm = normalizeProcPath(rawPath);
        if (norm == null) {
            return rawPath;
        }
        Path target = resolveVirtualProcFile(norm);
        return target != null ? target.toAbsolutePath().toString() : rawPath;
    }

    /** Chuyển hướng đối tượng {@link File} trỏ tới {@code /proc/...} sang tệp thật trên Windows. */
    public static File remapFile(File rawFile) {
        if (rawFile == null) {
            return null;
        }
        String remapped = remapPath(rawFile.getPath());
        if (remapped != null && !remapped.equals(rawFile.getPath())) {
            return new File(remapped);
        }
        return rawFile;
    }

    private static String normalizeProcPath(String path) {
        // Nhanh: bỏ qua ngay nếu không liên quan đến "proc"
        if (!path.contains("proc") && !path.contains("PROC")) {
            return null;
        }
        String s = path.replace('\\', '/');
        // Xử lý cả trường hợp Windows resolve "/proc/123/cmdline" thành "D:/proc/123/cmdline"
        if (s.length() >= 8 && Character.isLetter(s.charAt(0)) && s.charAt(1) == ':' && s.charAt(2) == '/') {
            s = s.substring(2);
        }
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.startsWith("/proc/")) {
            return lower;
        }
        return null;
    }

    private static Path resolveVirtualProcFile(String lowerProcPath) {
        if (isCmdlinePath(lowerProcPath)) {
            return ensureCmdlineFile();
        }
        if ("/proc/cpuinfo".equals(lowerProcPath)) {
            ensureConfigured();
            return cpuInfoFile;
        }
        if ("/proc/meminfo".equals(lowerProcPath)) {
            ensureConfigured();
            return memInfoFile;
        }
        return null;
    }

    private static boolean isCmdlinePath(String lowerProcPath) {
        if ("/proc/self/cmdline".equals(lowerProcPath)) {
            return true;
        }
        if (!lowerProcPath.startsWith("/proc/") || !lowerProcPath.endsWith("/cmdline")) {
            return false;
        }
        String pidPart = lowerProcPath.substring(6, lowerProcPath.length() - 8);
        if (pidPart.isEmpty()) return false;
        for (int i = 0; i < pidPart.length(); i++) {
            if (!Character.isDigit(pidPart.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static void ensureConfigured() {
        if (cmdlineFile == null || !Files.isRegularFile(cmdlineFile)) {
            configure(ActivityThread.currentProcessName(), procDir != null ? procDir.getParent() : null);
        }
    }

    private static Path ensureCmdlineFile() {
        String activeProc = ActivityThread.currentProcessName();
        if (cmdlineFile == null || !Files.isRegularFile(cmdlineFile)
                || (activeProc != null && !activeProc.equals(configuredProcessName))) {
            configure(activeProc, procDir != null ? procDir.getParent() : null);
        }
        return cmdlineFile;
    }
}
