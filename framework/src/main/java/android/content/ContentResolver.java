package android.content;

import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

public class ContentResolver {
    private final Context context;

    public ContentResolver(Context context) {
        this.context = context;
    }

    public InputStream openInputStream(Uri uri) throws FileNotFoundException {
        if (uri == null) throw new FileNotFoundException("Null URI");
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            try {
                File f = new File(URI.create(uri.toString()));
                return new FileInputStream(f);
            } catch (Exception e) {
                File f = new File(uri.getPath());
                return new FileInputStream(f);
            }
        }
        throw new FileNotFoundException("AAOW chưa hỗ trợ ContentProvider cho URI: " + uri);
    }

    public OutputStream openOutputStream(Uri uri) throws FileNotFoundException {
        if (uri == null) throw new FileNotFoundException("Null URI");
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            try {
                File f = new File(URI.create(uri.toString()));
                return new FileOutputStream(f);
            } catch (Exception e) {
                File f = new File(uri.getPath());
                return new FileOutputStream(f);
            }
        }
        throw new FileNotFoundException("AAOW chưa hỗ trợ ghi ContentProvider cho URI: " + uri);
    }

    public String getType(Uri url) {
        if (url == null) return null;
        String p = url.getPath();
        if (p == null) return null;
        if (p.endsWith(".txt") || p.endsWith(".md")) return "text/plain";
        if (p.endsWith(".html") || p.endsWith(".htm")) return "text/html";
        if (p.endsWith(".json")) return "application/json";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        return null;
    }
}
