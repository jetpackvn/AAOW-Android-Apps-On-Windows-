package android.os;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class Environment {
    public static final String MEDIA_MOUNTED = "mounted";
    public static final String DIRECTORY_DOWNLOADS = "Download";
    public static final String DIRECTORY_PICTURES = "Pictures";
    public static final String DIRECTORY_DCIM = "DCIM";
    public static final String DIRECTORY_MUSIC = "Music";
    public static final String DIRECTORY_MOVIES = "Movies";
    public static final String DIRECTORY_DOCUMENTS = "Documents";

    private static File ensureDir(String sub) {
        Path p = Path.of("build", "aaow-data", "sdcard", sub);
        try {
            Files.createDirectories(p);
        } catch (Exception ignored) {
        }
        return p.toFile();
    }

    public static File getExternalStorageDirectory() {
        return ensureDir("");
    }

    public static File getExternalStoragePublicDirectory(String type) {
        return ensureDir(type != null ? type : "");
    }

    public static File getDataDirectory() {
        return ensureDir("data");
    }

    public static File getDownloadCacheDirectory() {
        return ensureDir("cache");
    }

    public static File getRootDirectory() {
        return ensureDir("system");
    }

    public static String getExternalStorageState() {
        return MEDIA_MOUNTED;
    }

    public static String getExternalStorageState(File path) {
        return MEDIA_MOUNTED;
    }

    public static boolean isExternalStorageEmulated() {
        return true;
    }

    public static boolean isExternalStorageRemovable() {
        return false;
    }
}
