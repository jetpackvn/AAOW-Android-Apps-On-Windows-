package dev.aaow.apk;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Một file APK đã mở: thực chất là file ZIP chứa manifest, resources, classes*.dex, lib/... */
public final class ApkFile implements Closeable {
    private final Path path;
    private final ZipFile zip;
    private final ResourceTable resources;
    private final AndroidManifest manifest;

    public ApkFile(Path path) throws IOException {
        this.path = path;
        this.zip = new ZipFile(path.toFile());
        byte[] arsc = read("resources.arsc");
        this.resources = arsc != null ? ResourceTable.parse(arsc) : ResourceTable.empty();
        byte[] mf = read("AndroidManifest.xml");
        if (mf == null) throw new IOException("APK không có AndroidManifest.xml");
        this.manifest = AndroidManifest.parse(mf, resources);
    }

    public Path getPath() {
        return path;
    }

    public ResourceTable getResources() {
        return resources;
    }

    public AndroidManifest getManifest() {
        return manifest;
    }

    public boolean has(String name) {
        return zip.getEntry(name) != null;
    }

    public byte[] read(String name) throws IOException {
        ZipEntry e = zip.getEntry(name);
        if (e == null) return null;
        try (InputStream in = zip.getInputStream(e)) {
            return in.readAllBytes();
        }
    }

    /** Liệt kê toàn bộ tên entry trong APK bắt đầu bằng tiền tố cho trước. */
    public List<String> listEntries(String prefix) {
        String p = prefix != null ? prefix : "";
        List<String> out = new ArrayList<>();
        zip.stream().forEach(e -> {
            if (!e.isDirectory() && e.getName().startsWith(p)) {
                out.add(e.getName());
            }
        });
        return out;
    }

    /** classes.dex, classes2.dex, classes3.dex... theo đúng thứ tự. */
    public List<byte[]> readDexFiles() throws IOException {
        List<byte[]> out = new ArrayList<>();
        byte[] first = read("classes.dex");
        if (first == null) return out;
        out.add(first);
        for (int i = 2; ; i++) {
            byte[] d = read("classes" + i + ".dex");
            if (d == null) break;
            out.add(d);
        }
        return out;
    }

    /** Danh sách ABI có thư viện native (lib/arm64-v8a, lib/x86_64...). */
    public List<String> nativeAbis() {
        List<String> abis = new ArrayList<>();
        zip.stream().forEach(e -> {
            String n = e.getName();
            if (n.startsWith("lib/") && n.endsWith(".so")) {
                int slash = n.indexOf('/', 4);
                if (slash > 4) {
                    String abi = n.substring(4, slash);
                    if (!abis.contains(abi)) abis.add(abi);
                }
            }
        });
        return abis;
    }

    /** Liệt kê tên file thư viện native (.so / .dll) trong một thư mục ABI hoặc toàn bộ lib/. */
    public List<String> listNativeLibraries(String abi) {
        String prefix = abi != null && !abi.isEmpty() ? "lib/" + abi + "/" : "lib/";
        List<String> out = new ArrayList<>();
        zip.stream().forEach(e -> {
            if (!e.isDirectory() && e.getName().startsWith(prefix)
                    && (e.getName().endsWith(".so") || e.getName().endsWith(".dll"))) {
                out.add(e.getName());
            }
        });
        return out;
    }

    @Override
    public void close() throws IOException {
        zip.close();
    }
}
