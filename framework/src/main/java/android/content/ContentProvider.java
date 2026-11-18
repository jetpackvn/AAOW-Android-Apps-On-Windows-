package android.content;

import android.content.pm.PathPermission;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/** Lớp cơ sở cho các ContentProvider (bao gồm FileProvider và provider tùy chỉnh của ứng dụng). */
public abstract class ContentProvider implements ComponentCallbacks2 {
    public interface PipeDataWriter<T> {
        void writeDataToPipe(ParcelFileDescriptor output, Uri uri, String mimeType, Bundle opts, T args);
    }

    private Context mContext;
    private String mReadPermission;
    private String mWritePermission;
    private PathPermission[] mPathPermissions;
    private boolean mExported;
    private boolean mAttached;

    public ContentProvider() {
    }

    public abstract boolean onCreate();

    public abstract Cursor query(Uri uri, String[] projection, String selection,
                                 String[] selectionArgs, String sortOrder);

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder, CancellationSignal cancellationSignal) {
        if (cancellationSignal != null) cancellationSignal.throwIfCanceled();
        return query(uri, projection, selection, selectionArgs, sortOrder);
    }

    public Cursor query(Uri uri, String[] projection, Bundle queryArgs, CancellationSignal cancellationSignal) {
        if (cancellationSignal != null) cancellationSignal.throwIfCanceled();
        String selection = queryArgs != null ? queryArgs.getString(ContentResolver.QUERY_ARG_SQL_SELECTION) : null;
        String[] selectionArgs = queryArgs != null ? queryArgs.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS) : null;
        String sortOrder = queryArgs != null ? queryArgs.getString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER) : null;
        return query(uri, projection, selection, selectionArgs, sortOrder, cancellationSignal);
    }

    public abstract String getType(Uri uri);

    public String getTypeAnonymous(Uri uri) {
        return getType(uri);
    }

    public Uri canonicalize(Uri url) {
        return null;
    }

    public Uri uncanonicalize(Uri url) {
        return url;
    }

    public boolean refresh(Uri uri, Bundle extras, CancellationSignal cancellationSignal) {
        return false;
    }

    public abstract Uri insert(Uri uri, ContentValues values);

    public Uri insert(Uri uri, ContentValues values, Bundle extras) {
        return insert(uri, values);
    }

    public int bulkInsert(Uri uri, ContentValues[] values) {
        if (values == null) return 0;
        for (ContentValues cv : values) {
            insert(uri, cv);
        }
        return values.length;
    }

    public abstract int delete(Uri uri, String selection, String[] selectionArgs);

    public int delete(Uri uri, Bundle extras) {
        String selection = extras != null ? extras.getString(ContentResolver.QUERY_ARG_SQL_SELECTION) : null;
        String[] selectionArgs = extras != null ? extras.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS) : null;
        return delete(uri, selection, selectionArgs);
    }

    public abstract int update(Uri uri, ContentValues values, String selection, String[] selectionArgs);

    public int update(Uri uri, ContentValues values, Bundle extras) {
        String selection = extras != null ? extras.getString(ContentResolver.QUERY_ARG_SQL_SELECTION) : null;
        String[] selectionArgs = extras != null ? extras.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS) : null;
        return update(uri, values, selection, selectionArgs);
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        throw new FileNotFoundException("No files supported by provider at " + uri);
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
        if (signal != null) signal.throwIfCanceled();
        return openFile(uri, mode);
    }

    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        ParcelFileDescriptor fd = openFile(uri, mode);
        return fd != null ? new AssetFileDescriptor(fd, 0, -1) : null;
    }

    public AssetFileDescriptor openAssetFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
        if (signal != null) signal.throwIfCanceled();
        return openAssetFile(uri, mode);
    }

    protected final ParcelFileDescriptor openFileHelper(Uri uri, String mode) throws FileNotFoundException {
        Cursor c = query(uri, new String[]{"_data"}, null, null, null);
        int count = (c != null) ? c.getCount() : 0;
        if (count != 1) {
            if (c != null) c.close();
            throw new FileNotFoundException(count == 0 ? "No entry for " + uri : "Multiple items at " + uri);
        }
        c.moveToFirst();
        int i = c.getColumnIndex("_data");
        String path = (i >= 0 ? c.getString(i) : null);
        c.close();
        if (path == null) {
            throw new FileNotFoundException("Column _data not found.");
        }
        int modeBits = ParcelFileDescriptor.parseMode(mode);
        return ParcelFileDescriptor.open(new File(path), modeBits);
    }

    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        return null;
    }

    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts) throws FileNotFoundException {
        if ("*/*".equals(mimeTypeFilter)) {
            return openAssetFile(uri, "r");
        }
        String baseType = getType(uri);
        if (baseType != null && (mimeTypeFilter == null || baseType.equalsIgnoreCase(mimeTypeFilter))) {
            return openAssetFile(uri, "r");
        }
        throw new FileNotFoundException("Can't open " + uri + " as type " + mimeTypeFilter);
    }

    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts,
                                                 CancellationSignal signal) throws FileNotFoundException {
        if (signal != null) signal.throwIfCanceled();
        return openTypedAssetFile(uri, mimeTypeFilter, opts);
    }

    public Bundle call(String method, String arg, Bundle extras) {
        return null;
    }

    public Bundle call(String authority, String method, String arg, Bundle extras) {
        return call(method, arg, extras);
    }

    public final Context getContext() {
        return mContext;
    }

    public final Context requireContext() {
        if (mContext == null) {
            throw new IllegalStateException("Cannot find context from the provider.");
        }
        return mContext;
    }

    public final String getCallingPackage() {
        return mContext != null ? mContext.getPackageName() : null;
    }

    public final String getCallingPackageUnchecked() {
        return getCallingPackage();
    }

    protected final void setReadPermission(String permission) {
        mReadPermission = permission;
    }

    public final String getReadPermission() {
        return mReadPermission;
    }

    protected final void setWritePermission(String permission) {
        mWritePermission = permission;
    }

    public final String getWritePermission() {
        return mWritePermission;
    }

    protected final void setPathPermissions(PathPermission[] permissions) {
        mPathPermissions = permissions;
    }

    public final PathPermission[] getPathPermissions() {
        return mPathPermissions;
    }

    protected boolean isTemporary() {
        return false;
    }

    public void attachInfo(Context context, ProviderInfo info) {
        if (!mAttached) {
            mAttached = true;
            mContext = context;
            if (info != null) {
                mReadPermission = info.readPermission;
                mWritePermission = info.writePermission;
                mPathPermissions = info.pathPermissions;
                mExported = info.exported;
            }
            onCreate();
        }
    }

    public void shutdown() {
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public void onTrimMemory(int level) {
    }
}
