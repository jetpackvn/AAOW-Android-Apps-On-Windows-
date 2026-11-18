package com.jakewharton.disklrucache;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bộ nhớ đệm đĩa gọn nhẹ tương thích API com.jakewharton.disklrucache.DiskLruCache. */
public final class DiskLruCache implements Closeable {
    private final File directory;
    private final int appVersion;
    private final int valueCount;
    private final long maxSize;
    private final Map<String, Long> entries = new LinkedHashMap<>(0, 0.75f, true);
    private long currentSize;
    private boolean closed;

    public final class Snapshot implements Closeable {
        private final File[] files;

        Snapshot(File[] files) {
            this.files = files;
        }

        public InputStream getInputStream(int index) throws IOException {
            if (index < 0 || index >= files.length || !files[index].exists()) {
                throw new IOException("Snapshot entry missing at index " + index);
            }
            return new FileInputStream(files[index]);
        }

        public String getString(int index) throws IOException {
            try (InputStream in = getInputStream(index)) {
                return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }

        public long getLength(int index) {
            return (index >= 0 && index < files.length && files[index].exists()) ? files[index].length() : 0L;
        }

        @Override
        public void close() {
        }
    }

    public final class Editor {
        private final String key;
        private final File[] tempFiles;
        private final File[] finalFiles;
        private boolean committed;

        Editor(String key) {
            this.key = key;
            this.tempFiles = new File[valueCount];
            this.finalFiles = new File[valueCount];
            for (int i = 0; i < valueCount; i++) {
                this.finalFiles[i] = entryFile(key, i);
                this.tempFiles[i] = new File(directory, sanitize(key) + "." + i + ".tmp");
            }
        }

        public OutputStream newOutputStream(int index) throws IOException {
            directory.mkdirs();
            return new FileOutputStream(tempFiles[index]);
        }

        public void set(int index, String value) throws IOException {
            try (OutputStream out = newOutputStream(index)) {
                out.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        }

        public void commit() throws IOException {
            long totalLen = 0;
            for (int i = 0; i < valueCount; i++) {
                if (tempFiles[i].exists()) {
                    Files.move(tempFiles[i].toPath(), finalFiles[i].toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                if (finalFiles[i].exists()) {
                    totalLen += finalFiles[i].length();
                }
            }
            synchronized (DiskLruCache.this) {
                Long prev = entries.put(key, totalLen);
                if (prev != null) currentSize -= prev;
                currentSize += totalLen;
                trimToSize();
            }
            committed = true;
        }

        public void abort() throws IOException {
            for (File tf : tempFiles) {
                if (tf.exists()) tf.delete();
            }
        }

        public void abortUnlessCommitted() {
            if (!committed) {
                try {
                    abort();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private DiskLruCache(File directory, int appVersion, int valueCount, long maxSize) {
        this.directory = directory;
        this.appVersion = appVersion;
        this.valueCount = Math.max(1, valueCount);
        this.maxSize = Math.max(1024L, maxSize);
    }

    public static DiskLruCache open(File directory, int appVersion, int valueCount, long maxSize) throws IOException {
        if (directory == null) throw new IllegalArgumentException("directory == null");
        Files.createDirectories(directory.toPath());
        return new DiskLruCache(directory, appVersion, valueCount, maxSize);
    }

    public synchronized Snapshot get(String key) throws IOException {
        if (closed || key == null) return null;
        File[] files = new File[valueCount];
        for (int i = 0; i < valueCount; i++) {
            files[i] = entryFile(key, i);
            if (!files[i].exists()) return null;
        }
        return new Snapshot(files);
    }

    public synchronized Editor edit(String key) throws IOException {
        if (closed || key == null) return null;
        return new Editor(key);
    }

    public synchronized boolean remove(String key) throws IOException {
        if (key == null) return false;
        boolean removed = false;
        for (int i = 0; i < valueCount; i++) {
            File f = entryFile(key, i);
            if (f.exists() && f.delete()) removed = true;
        }
        Long sz = entries.remove(key);
        if (sz != null) currentSize -= sz;
        return removed;
    }

    public synchronized void flush() throws IOException {
    }

    public synchronized long size() {
        return currentSize;
    }

    public File getDirectory() {
        return directory;
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    @Override
    public synchronized void close() throws IOException {
        closed = true;
    }

    private void trimToSize() {
        while (currentSize > maxSize && !entries.isEmpty()) {
            var it = entries.entrySet().iterator();
            var oldest = it.next();
            it.remove();
            currentSize -= oldest.getValue();
            for (int i = 0; i < valueCount; i++) {
                File f = entryFile(oldest.getKey(), i);
                if (f.exists()) f.delete();
            }
        }
    }

    private File entryFile(String key, int index) {
        return new File(directory, sanitize(key) + "." + index);
    }

    private static String sanitize(String key) {
        return Integer.toHexString(key.hashCode()) + "_" + key.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
