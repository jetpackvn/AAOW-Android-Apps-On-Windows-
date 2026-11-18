package android.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public final class DocumentsContract {
    private DocumentsContract() {
    }

    public static boolean isDocumentUri(Context context, Uri uri) {
        if (uri == null) return false;
        String scheme = uri.getScheme();
        if (!"content".equalsIgnoreCase(scheme)) return false;
        List<String> segs = uri.getPathSegments();
        return segs.size() >= 2 && "document".equals(segs.get(0));
    }

    public static boolean isTreeUri(Uri uri) {
        if (uri == null) return false;
        List<String> segs = uri.getPathSegments();
        return !segs.isEmpty() && "tree".equals(segs.get(0));
    }

    public static String getDocumentId(Uri documentUri) {
        if (documentUri == null) return null;
        List<String> segs = documentUri.getPathSegments();
        if (segs.size() >= 2 && "document".equals(segs.get(0))) {
            return segs.get(1);
        }
        if (segs.size() >= 4 && "tree".equals(segs.get(0)) && "document".equals(segs.get(2))) {
            return segs.get(3);
        }
        String path = documentUri.getPath();
        return path != null ? path : documentUri.toString();
    }

    public static String getTreeDocumentId(Uri documentUri) {
        if (documentUri == null) return null;
        List<String> segs = documentUri.getPathSegments();
        if (segs.size() >= 2 && "tree".equals(segs.get(0))) {
            return segs.get(1);
        }
        return getDocumentId(documentUri);
    }

    public static Uri buildDocumentUri(String authority, String documentId) {
        return new Uri.Builder()
                .scheme("content")
                .authority(authority)
                .appendPath("document")
                .appendPath(documentId)
                .build();
    }

    public static Uri buildTreeDocumentUri(String authority, String treeDocumentId) {
        return new Uri.Builder()
                .scheme("content")
                .authority(authority)
                .appendPath("tree")
                .appendPath(treeDocumentId)
                .build();
    }

    public static boolean deleteDocument(ContentResolver content, Uri documentUri) throws FileNotFoundException {
        if (documentUri != null && "file".equalsIgnoreCase(documentUri.getScheme())) {
            File f = new File(documentUri.getPath());
            return f.delete();
        }
        return false;
    }

    public static Uri renameDocument(ContentResolver content, Uri documentUri, String displayName) throws FileNotFoundException {
        if (documentUri != null && "file".equalsIgnoreCase(documentUri.getScheme()) && displayName != null) {
            File src = new File(documentUri.getPath());
            File dst = new File(src.getParentFile(), displayName);
            if (src.renameTo(dst)) {
                return Uri.fromFile(dst);
            }
        }
        return null;
    }

    public static Uri createDocument(ContentResolver content, Uri parentDocumentUri, String mimeType, String displayName)
            throws FileNotFoundException {
        if (parentDocumentUri != null && "file".equalsIgnoreCase(parentDocumentUri.getScheme()) && displayName != null) {
            File dir = new File(parentDocumentUri.getPath());
            File target = new File(dir, displayName);
            try {
                if (target.createNewFile() || target.exists()) {
                    return Uri.fromFile(target);
                }
            } catch (Exception e) {
                throw new FileNotFoundException(e.getMessage());
            }
        }
        return null;
    }
}
