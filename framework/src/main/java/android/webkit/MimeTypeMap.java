package android.webkit;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MimeTypeMap {
    private static final MimeTypeMap SINGLETON = new MimeTypeMap();
    private static final Map<String, String> EXT_TO_MIME = new HashMap<>();
    private static final Map<String, String> MIME_TO_EXT = new HashMap<>();

    static {
        add("html", "text/html");
        add("htm", "text/html");
        add("css", "text/css");
        add("js", "application/javascript");
        add("json", "application/json");
        add("xml", "text/xml");
        add("txt", "text/plain");
        add("md", "text/markdown");
        add("markdown", "text/markdown");
        add("csv", "text/csv");
        add("pdf", "application/pdf");
        add("zip", "application/zip");
        add("gz", "application/gzip");
        add("apk", "application/vnd.android.package-archive");
        add("png", "image/png");
        add("jpg", "image/jpeg");
        add("jpeg", "image/jpeg");
        add("gif", "image/gif");
        add("webp", "image/webp");
        add("svg", "image/svg+xml");
        add("ico", "image/x-icon");
        add("mp3", "audio/mpeg");
        add("wav", "audio/wav");
        add("ogg", "audio/ogg");
        add("m4a", "audio/mp4");
        add("aac", "audio/aac");
        add("flac", "audio/flac");
        add("mp4", "video/mp4");
        add("webm", "video/webm");
        add("mkv", "video/x-matroska");
        add("woff", "font/woff");
        add("woff2", "font/woff2");
        add("ttf", "font/ttf");
    }

    private static void add(String ext, String mime) {
        EXT_TO_MIME.put(ext, mime);
        MIME_TO_EXT.putIfAbsent(mime, ext);
    }

    MimeTypeMap() {
    }

    public static MimeTypeMap getSingleton() {
        return SINGLETON;
    }

    public static String getFileExtensionFromUrl(String url) {
        if (url == null || url.isEmpty()) return "";
        int fragment = url.lastIndexOf('#');
        if (fragment > 0) url = url.substring(0, fragment);
        int query = url.lastIndexOf('?');
        if (query > 0) url = url.substring(0, query);
        int filenamePos = url.lastIndexOf('/');
        String filename = filenamePos >= 0 ? url.substring(filenamePos + 1) : url;
        int dotPos = filename.lastIndexOf('.');
        if (dotPos >= 0 && dotPos < filename.length() - 1) {
            return filename.substring(dotPos + 1);
        }
        return "";
    }

    public boolean hasMimeType(String mimeType) {
        return mimeType != null && MIME_TO_EXT.containsKey(mimeType.toLowerCase(Locale.ROOT));
    }

    public String getMimeTypeFromExtension(String extension) {
        if (extension == null || extension.isEmpty()) return null;
        return EXT_TO_MIME.get(extension.toLowerCase(Locale.ROOT));
    }

    public boolean hasExtension(String extension) {
        return extension != null && EXT_TO_MIME.containsKey(extension.toLowerCase(Locale.ROOT));
    }

    public String getExtensionFromMimeType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) return null;
        return MIME_TO_EXT.get(mimeType.toLowerCase(Locale.ROOT));
    }
}
