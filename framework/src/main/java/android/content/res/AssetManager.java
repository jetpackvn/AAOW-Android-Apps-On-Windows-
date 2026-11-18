package android.content.res;

import android.os.ParcelFileDescriptor;
import dev.aaow.apk.ApkFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AssetManager implements AutoCloseable {
    public static final int ACCESS_UNKNOWN = 0;
    public static final int ACCESS_RANDOM = 1;
    public static final int ACCESS_STREAMING = 2;
    public static final int ACCESS_BUFFER = 3;

    private final ApkFile apk;

    public AssetManager(ApkFile apk) {
        this.apk = apk;
    }

    public ApkFile getApk() {
        return apk;
    }

    public InputStream open(String fileName) throws IOException {
        if (apk == null) throw new FileNotFoundException(fileName);
        String entry = normalizeAssetPath(fileName);
        byte[] bytes = apk.read(entry);
        if (bytes == null) throw new FileNotFoundException(fileName);
        return new ByteArrayInputStream(bytes);
    }

    public InputStream open(String fileName, int accessMode) throws IOException {
        return open(fileName);
    }

    public AssetFileDescriptor openFd(String fileName) throws IOException {
        if (apk == null) throw new FileNotFoundException(fileName);
        String entry = normalizeAssetPath(fileName);
        byte[] bytes = apk.read(entry);
        if (bytes == null) throw new FileNotFoundException(fileName);
        return createTempAssetFd(entry, bytes);
    }

    public AssetFileDescriptor openNonAssetFd(String fileName) throws IOException {
        if (apk == null) throw new FileNotFoundException(fileName);
        byte[] bytes = apk.read(fileName);
        if (bytes == null) throw new FileNotFoundException(fileName);
        return createTempAssetFd(fileName, bytes);
    }

    public String[] list(String path) throws IOException {
        if (apk == null) return new String[0];
        String norm = path == null ? "" : path.replace('\\', '/');
        while (norm.startsWith("/")) norm = norm.substring(1);
        while (norm.endsWith("/")) norm = norm.substring(0, norm.length() - 1);
        String prefix = norm.isEmpty() ? "assets/" : "assets/" + norm + "/";
        Set<String> children = new LinkedHashSet<>();
        for (String entry : apk.listEntries(prefix)) {
            String rest = entry.substring(prefix.length());
            if (rest.isEmpty()) continue;
            int slash = rest.indexOf('/');
            children.add(slash >= 0 ? rest.substring(0, slash) : rest);
        }
        return children.toArray(new String[0]);
    }

    static AssetFileDescriptor createTempAssetFd(String entryName, byte[] bytes) throws IOException {
        Path cacheDir = Path.of(System.getProperty("java.io.tmpdir"), "aaow-raw-fd");
        Files.createDirectories(cacheDir);
        String safe = entryName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path out = cacheDir.resolve(Integer.toHexString(entryName.hashCode()) + "_" + safe);
        if (!Files.exists(out) || Files.size(out) != bytes.length) {
            Files.write(out, bytes);
        }
        File f = out.toFile();
        return new AssetFileDescriptor(ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY), 0L, bytes.length);
    }

    private static String normalizeAssetPath(String fileName) {
        String s = fileName == null ? "" : fileName.replace('\\', '/');
        while (s.startsWith("/")) s = s.substring(1);
        return s.startsWith("assets/") ? s : "assets/" + s;
    }

    @Override
    public void close() {
    }
}
