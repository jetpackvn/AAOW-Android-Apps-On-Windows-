package android.content;

import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cài đặt android.content.ContentResolver hỗ trợ đầy đủ file://, content:// (ContentProvider/FileProvider),
 * android.resource:// và cơ chế ContentObserver notifyChange().
 */
public class ContentResolver {
    public static final String SCHEME_CONTENT = "content";
    public static final String SCHEME_ANDROID_RESOURCE = "android.resource";
    public static final String SCHEME_FILE = "file";

    public static final String QUERY_ARG_SQL_SELECTION = "android:query-arg-sql-selection";
    public static final String QUERY_ARG_SQL_SELECTION_ARGS = "android:query-arg-sql-selection-args";
    public static final String QUERY_ARG_SQL_SORT_ORDER = "android:query-arg-sql-sort-order";

    public static final int NOTIFY_SYNC_TO_NETWORK = 1 << 0;
    public static final int NOTIFY_UPDATE = 1 << 1;
    public static final int NOTIFY_INSERT = 1 << 2;
    public static final int NOTIFY_DELETE = 1 << 3;

    private record ObserverEntry(Uri uri, boolean notifyForDescendants, ContentObserver observer) {
    }

    private final Context context;
    private final Map<String, ContentProvider> providers = new ConcurrentHashMap<>();
    private final List<ObserverEntry> observers = new CopyOnWriteArrayList<>();

    public ContentResolver(Context context) {
        this.context = context;
    }

    public void registerProvider(String authority, ContentProvider provider) {
        if (authority == null || authority.isEmpty() || provider == null) return;
        ProviderInfo info = new ProviderInfo();
        info.authority = authority;
        info.name = provider.getClass().getName();
        info.packageName = context != null ? context.getPackageName() : "";
        info.exported = true;
        provider.attachInfo(context, info);
        providers.put(authority, provider);
    }

    public void registerInitializedProvider(String authority, ContentProvider provider) {
        if (authority == null || authority.isEmpty() || provider == null) return;
        providers.putIfAbsent(authority, provider);
    }

    public ContentProvider acquireProvider(Uri uri) {
        if (uri == null || !SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) return null;
        return acquireProvider(uri.getAuthority());
    }

    public ContentProvider acquireProvider(String authority) {
        if (authority == null || authority.isEmpty()) return null;
        ContentProvider existing = providers.get(authority);
        if (existing != null) return existing;
        if (context == null || context.getPackageManager() == null) return null;
        ProviderInfo pi = context.getPackageManager().resolveContentProvider(authority, PackageManager.GET_META_DATA);
        if (pi == null || pi.name == null) return null;
        return providers.computeIfAbsent(authority, auth -> instantiateProvider(pi));
    }

    private ContentProvider instantiateProvider(ProviderInfo pi) {
        try {
            ClassLoader cl = context.getClassLoader();
            Class<?> cls = Class.forName(pi.name, true, cl != null ? cl : getClass().getClassLoader());
            if (ContentProvider.class.isAssignableFrom(cls)) {
                ContentProvider cp = (ContentProvider) cls.getDeclaredConstructor().newInstance();
                cp.attachInfo(context, pi);
                return cp;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    // --------------------------------------------------------- Query / CRUD

    public final Cursor query(Uri uri, String[] projection, String selection,
                              String[] selectionArgs, String sortOrder) {
        return query(uri, projection, selection, selectionArgs, sortOrder, null);
    }

    public final Cursor query(Uri uri, String[] projection, String selection,
                              String[] selectionArgs, String sortOrder,
                              CancellationSignal cancellationSignal) {
        if (cancellationSignal != null) cancellationSignal.throwIfCanceled();
        if (uri == null) return null;
        ContentProvider provider = acquireProvider(uri);
        if (provider != null) {
            return provider.query(uri, projection, selection, selectionArgs, sortOrder, cancellationSignal);
        }
        if (SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
            File f = resolveFile(uri);
            String[] cols = projection != null ? projection : new String[]{"_display_name", "_size"};
            MatrixCursor mc = new MatrixCursor(cols, 1);
            if (f != null && f.exists()) {
                Object[] row = new Object[cols.length];
                for (int i = 0; i < cols.length; i++) {
                    if ("_display_name".equals(cols[i])) row[i] = f.getName();
                    else if ("_size".equals(cols[i])) row[i] = f.length();
                }
                mc.addRow(row);
            }
            return mc;
        }
        return null;
    }

    public final Cursor query(Uri uri, String[] projection, Bundle queryArgs,
                              CancellationSignal cancellationSignal) {
        if (cancellationSignal != null) cancellationSignal.throwIfCanceled();
        if (uri == null) return null;
        ContentProvider provider = acquireProvider(uri);
        if (provider != null) {
            return provider.query(uri, projection, queryArgs, cancellationSignal);
        }
        String selection = queryArgs != null ? queryArgs.getString(QUERY_ARG_SQL_SELECTION) : null;
        String[] selectionArgs = queryArgs != null ? queryArgs.getStringArray(QUERY_ARG_SQL_SELECTION_ARGS) : null;
        String sortOrder = queryArgs != null ? queryArgs.getString(QUERY_ARG_SQL_SORT_ORDER) : null;
        return query(uri, projection, selection, selectionArgs, sortOrder, cancellationSignal);
    }

    public final Uri insert(Uri url, ContentValues values) {
        return insert(url, values, null);
    }

    public final Uri insert(Uri url, ContentValues values, Bundle extras) {
        ContentProvider provider = acquireProvider(url);
        if (provider == null) return null;
        return extras != null ? provider.insert(url, values, extras) : provider.insert(url, values);
    }

    public final int bulkInsert(Uri url, ContentValues[] values) {
        ContentProvider provider = acquireProvider(url);
        if (provider == null) return 0;
        return provider.bulkInsert(url, values);
    }

    public final int delete(Uri url, String where, String[] selectionArgs) {
        ContentProvider provider = acquireProvider(url);
        if (provider == null) return 0;
        return provider.delete(url, where, selectionArgs);
    }

    public final int delete(Uri url, Bundle extras) {
        ContentProvider provider = acquireProvider(url);
        if (provider == null) return 0;
        return provider.delete(url, extras);
    }

    public final int update(Uri uri, ContentValues values, String where, String[] selectionArgs) {
        ContentProvider provider = acquireProvider(uri);
        if (provider == null) return 0;
        return provider.update(uri, values, where, selectionArgs);
    }

    public final int update(Uri uri, ContentValues values, Bundle extras) {
        ContentProvider provider = acquireProvider(uri);
        if (provider == null) return 0;
        return provider.update(uri, values, extras);
    }

    public final Bundle call(Uri uri, String method, String arg, Bundle extras) {
        if (uri == null) return null;
        ContentProvider provider = acquireProvider(uri);
        return provider != null ? provider.call(uri.getAuthority(), method, arg, extras) : null;
    }

    public final Bundle call(String authority, String method, String arg, Bundle extras) {
        ContentProvider provider = acquireProvider(authority);
        return provider != null ? provider.call(authority, method, arg, extras) : null;
    }

    // --------------------------------------------------------- Streams & FileDescriptors

    private static File resolveFile(Uri uri) {
        try {
            return new File(URI.create(uri.toString()));
        } catch (Exception e) {
            String path = uri.getPath();
            return path != null ? new File(path) : null;
        }
    }

    public final ParcelFileDescriptor openFileDescriptor(Uri uri, String mode) throws FileNotFoundException {
        return openFileDescriptor(uri, mode, null);
    }

    public final ParcelFileDescriptor openFileDescriptor(Uri uri, String mode,
                                                         CancellationSignal cancellationSignal) throws FileNotFoundException {
        if (cancellationSignal != null) cancellationSignal.throwIfCanceled();
        if (uri == null) throw new FileNotFoundException("Null URI");
        if (SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
            File f = resolveFile(uri);
            if (f == null) throw new FileNotFoundException("Invalid file URI: " + uri);
            return ParcelFileDescriptor.open(f, ParcelFileDescriptor.parseMode(mode));
        }
        ContentProvider provider = acquireProvider(uri);
        if (provider != null) {
            return provider.openFile(uri, mode, cancellationSignal);
        }
        throw new FileNotFoundException("No content provider for URI: " + uri);
    }

    public final AssetFileDescriptor openAssetFileDescriptor(Uri uri, String mode) throws FileNotFoundException {
        return openAssetFileDescriptor(uri, mode, null);
    }

    public final AssetFileDescriptor openAssetFileDescriptor(Uri uri, String mode,
                                                             CancellationSignal cancellationSignal) throws FileNotFoundException {
        if (cancellationSignal != null) cancellationSignal.throwIfCanceled();
        if (uri == null) throw new FileNotFoundException("Null URI");
        if (SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
            ParcelFileDescriptor pfd = openFileDescriptor(uri, mode, cancellationSignal);
            return new AssetFileDescriptor(pfd, 0, -1);
        }
        ContentProvider provider = acquireProvider(uri);
        if (provider != null) {
            return provider.openAssetFile(uri, mode, cancellationSignal);
        }
        throw new FileNotFoundException("No content provider for URI: " + uri);
    }

    public final InputStream openInputStream(Uri uri) throws FileNotFoundException {
        if (uri == null) throw new FileNotFoundException("Null URI");
        String scheme = uri.getScheme();
        if (SCHEME_FILE.equalsIgnoreCase(scheme)) {
            File f = resolveFile(uri);
            if (f == null) throw new FileNotFoundException("Invalid file URI: " + uri);
            return new FileInputStream(f);
        }
        if (SCHEME_ANDROID_RESOURCE.equalsIgnoreCase(scheme) && context != null) {
            Resources res = context.getResources();
            List<String> segs = uri.getPathSegments();
            if (res != null && !segs.isEmpty()) {
                int resId = 0;
                if (segs.size() == 1) {
                    try {
                        resId = Integer.parseInt(segs.get(0));
                    } catch (NumberFormatException ignored) {
                    }
                } else if (segs.size() == 2) {
                    resId = res.getIdentifier(segs.get(1), segs.get(0), uri.getAuthority());
                }
                if (resId != 0) {
                    try {
                        return res.openRawResource(resId);
                    } catch (Exception e) {
                        var rv = res.getTable().getValue(resId);
                        if (rv != null && rv.isString() && rv.string() != null && res.getApk() != null) {
                            try {
                                byte[] data = res.getApk().read(rv.string());
                                if (data != null) return new ByteArrayInputStream(data);
                            } catch (java.io.IOException ignored) {
                            }
                        }
                    }
                }
            }
            throw new FileNotFoundException("Resource not found: " + uri);
        }
        ContentProvider provider = acquireProvider(uri);
        if (provider != null) {
            ParcelFileDescriptor pfd = provider.openFile(uri, "r");
            if (pfd != null) {
                return new ParcelFileDescriptor.AutoCloseInputStream(pfd);
            }
        }
        throw new FileNotFoundException("AAOW chưa hỗ trợ mở InputStream cho URI: " + uri);
    }

    public final OutputStream openOutputStream(Uri uri) throws FileNotFoundException {
        return openOutputStream(uri, "w");
    }

    public final OutputStream openOutputStream(Uri uri, String mode) throws FileNotFoundException {
        if (uri == null) throw new FileNotFoundException("Null URI");
        String safeMode = mode != null ? mode : "w";
        if (SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
            File f = resolveFile(uri);
            if (f == null) throw new FileNotFoundException("Invalid file URI: " + uri);
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            boolean append = safeMode.contains("a");
            return new FileOutputStream(f, append);
        }
        ContentProvider provider = acquireProvider(uri);
        if (provider != null) {
            ParcelFileDescriptor pfd = provider.openFile(uri, safeMode);
            if (pfd != null) {
                return new ParcelFileDescriptor.AutoCloseOutputStream(pfd);
            }
        }
        throw new FileNotFoundException("AAOW chưa hỗ trợ ghi ContentProvider cho URI: " + uri);
    }

    // --------------------------------------------------------- Observers & MIME

    public final void registerContentObserver(Uri uri, boolean notifyForDescendants, ContentObserver observer) {
        if (uri != null && observer != null) {
            observers.add(new ObserverEntry(uri, notifyForDescendants, observer));
        }
    }

    public final void unregisterContentObserver(ContentObserver observer) {
        if (observer != null) {
            observers.removeIf(e -> e.observer == observer);
        }
    }

    public void notifyChange(Uri uri, ContentObserver observer) {
        notifyChange(uri, observer, 0);
    }

    public void notifyChange(Uri uri, ContentObserver observer, boolean syncToNetwork) {
        notifyChange(uri, observer, syncToNetwork ? NOTIFY_SYNC_TO_NETWORK : 0);
    }

    public void notifyChange(Uri uri, ContentObserver observer, int flags) {
        if (uri == null) return;
        notifyChange(Collections.singletonList(uri), observer, flags);
    }

    public void notifyChange(Collection<Uri> uris, ContentObserver observer, int flags) {
        if (uris == null || uris.isEmpty()) return;
        for (ObserverEntry entry : observers) {
            for (Uri changed : uris) {
                if (matchesObserver(entry, changed)) {
                    boolean selfChange = (entry.observer == observer);
                    if (!selfChange || entry.observer.deliverSelfNotifications()) {
                        entry.observer.dispatchChange(selfChange, changed, flags);
                    }
                    break;
                }
            }
        }
    }

    private static boolean matchesObserver(ObserverEntry entry, Uri changed) {
        if (entry.uri.equals(changed)) return true;
        if (!entry.notifyForDescendants) return false;
        String base = entry.uri.toString();
        String target = changed.toString();
        if (!base.endsWith("/")) base = base + "/";
        return target.startsWith(base);
    }

    public final String getType(Uri url) {
        if (url == null) return null;
        ContentProvider provider = acquireProvider(url);
        if (provider != null) {
            String t = provider.getType(url);
            if (t != null) return t;
        }
        String p = url.getPath();
        if (p == null) return null;
        String lower = p.toLowerCase();
        if (lower.endsWith(".txt") || lower.endsWith(".md")) return "text/plain";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".xml")) return "application/xml";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return null;
    }

    public static ContentResolver wrap(ContentProvider wrapped) {
        ContentResolver cr = new ContentResolver(wrapped != null ? wrapped.getContext() : null);
        if (wrapped != null) {
            cr.providers.put("", wrapped);
        }
        return cr;
    }
}
