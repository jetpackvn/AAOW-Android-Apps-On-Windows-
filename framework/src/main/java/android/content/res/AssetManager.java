package android.content.res;

import dev.aaow.apk.ApkFile;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public final class AssetManager implements AutoCloseable {
    private final ApkFile apk;

    public AssetManager(ApkFile apk) {
        this.apk = apk;
    }

    public InputStream open(String fileName) throws IOException {
        if (apk == null) throw new FileNotFoundException(fileName);
        String entry = fileName.startsWith("assets/") ? fileName : "assets/" + fileName;
        byte[] bytes = apk.read(entry);
        if (bytes == null) throw new FileNotFoundException(fileName);
        return new ByteArrayInputStream(bytes);
    }

    public InputStream open(String fileName, int accessMode) throws IOException {
        return open(fileName);
    }

    @Override
    public void close() {
    }
}
