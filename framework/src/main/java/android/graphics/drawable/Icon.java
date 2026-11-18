package android.graphics.drawable;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;

public final class Icon implements Parcelable {
    public interface OnDrawableLoadedListener {
        void onDrawableLoaded(Drawable d);
    }

    public static final int TYPE_BITMAP = 1;
    public static final int TYPE_RESOURCE = 2;
    public static final int TYPE_DATA = 3;
    public static final int TYPE_URI = 4;
    public static final int TYPE_ADAPTIVE_BITMAP = 5;
    public static final int TYPE_URI_ADAPTIVE_BITMAP = 6;

    private int type;
    private Bitmap bitmap;
    private String resPackage;
    private int resId;
    private byte[] data;
    private Uri uri;
    private int tintColor;
    private boolean hasTint;

    Icon() {
    }

    public static Icon createWithResource(Context context, int resId) {
        Icon ic = new Icon();
        ic.type = TYPE_RESOURCE;
        ic.resPackage = context != null ? context.getPackageName() : "";
        ic.resId = resId;
        return ic;
    }

    public static Icon createWithResource(String resPackage, int resId) {
        Icon ic = new Icon();
        ic.type = TYPE_RESOURCE;
        ic.resPackage = resPackage;
        ic.resId = resId;
        return ic;
    }

    public static Icon createWithBitmap(Bitmap bits) {
        Icon ic = new Icon();
        ic.type = TYPE_BITMAP;
        ic.bitmap = bits;
        return ic;
    }

    public static Icon createWithAdaptiveBitmap(Bitmap bits) {
        Icon ic = createWithBitmap(bits);
        ic.type = TYPE_ADAPTIVE_BITMAP;
        return ic;
    }

    public static Icon createWithData(byte[] data, int offset, int length) {
        Icon ic = new Icon();
        ic.type = TYPE_DATA;
        if (data != null && length > 0) {
            ic.data = java.util.Arrays.copyOfRange(data, offset, offset + length);
        }
        return ic;
    }

    public static Icon createWithContentUri(Uri uri) {
        Icon ic = new Icon();
        ic.type = TYPE_URI;
        ic.uri = uri;
        return ic;
    }

    public static Icon createWithContentUri(String uri) {
        return createWithContentUri(uri != null ? Uri.parse(uri) : null);
    }

    public static Icon createWithAdaptiveBitmapContentUri(Uri uri) {
        Icon ic = createWithContentUri(uri);
        ic.type = TYPE_URI_ADAPTIVE_BITMAP;
        return ic;
    }

    public static Icon createWithAdaptiveBitmapContentUri(String uri) {
        return createWithAdaptiveBitmapContentUri(uri != null ? Uri.parse(uri) : null);
    }

    public static Icon createWithFilePath(String path) {
        return createWithContentUri(path != null ? Uri.fromFile(new java.io.File(path)) : null);
    }

    public int getType() { return type; }
    public int getResId() { return resId; }
    public String getResPackage() { return resPackage; }
    public Uri getUri() { return uri; }
    public Bitmap getBitmap() { return bitmap; }

    public Icon setTint(int tint) {
        this.tintColor = tint;
        this.hasTint = true;
        return this;
    }

    public Icon setTintList(ColorStateList tintList) {
        if (tintList != null) setTint(tintList.getDefaultColor());
        return this;
    }

    public Icon setTintMode(PorterDuff.Mode mode) {
        return this;
    }

    public Drawable loadDrawable(Context context) {
        Drawable d = null;
        if (type == TYPE_BITMAP || type == TYPE_ADAPTIVE_BITMAP) {
            if (bitmap != null) d = new BitmapDrawable(context != null ? context.getResources() : null, bitmap);
        } else if (type == TYPE_RESOURCE && context != null && context.getResources() != null) {
            d = context.getResources().getDrawable(resId);
        } else if (type == TYPE_DATA && data != null) {
            Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (b != null) d = new BitmapDrawable(context != null ? context.getResources() : null, b);
        } else if ((type == TYPE_URI || type == TYPE_URI_ADAPTIVE_BITMAP) && uri != null && context != null) {
            try (var in = context.getContentResolver().openInputStream(uri)) {
                if (in != null) {
                    Bitmap b = BitmapFactory.decodeStream(in);
                    if (b != null) d = new BitmapDrawable(context.getResources(), b);
                }
            } catch (Exception ignored) {
            }
        }
        if (d != null && hasTint) {
            d.setTint(tintColor);
        }
        return d;
    }

    public void loadDrawableAsync(Context context, OnDrawableLoadedListener listener, Handler handler) {
        Drawable d = loadDrawable(context);
        if (listener != null) {
            if (handler != null) handler.post(() -> listener.onDrawableLoaded(d));
            else listener.onDrawableLoaded(d);
        }
    }

    public void loadDrawableAsync(Context context, Message andThen) {
        Drawable d = loadDrawable(context);
        if (andThen != null) {
            andThen.obj = d;
            andThen.sendToTarget();
        }
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeString(resPackage);
        dest.writeInt(resId);
        dest.writeParcelable(uri, flags);
        dest.writeParcelable(bitmap, flags);
    }

    public static final Creator<Icon> CREATOR = new Creator<>() {
        @Override
        public Icon createFromParcel(Parcel in) {
            Icon ic = new Icon();
            ic.type = in.readInt();
            ic.resPackage = in.readString();
            ic.resId = in.readInt();
            ic.uri = in.readParcelable(null);
            ic.bitmap = in.readParcelable(null);
            return ic;
        }

        @Override
        public Icon[] newArray(int size) { return new Icon[size]; }
    };
}
