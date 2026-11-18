package android.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AndroidException;

import java.util.Arrays;
import java.util.Objects;

public final class PendingIntent implements Parcelable {
    public static class CanceledException extends AndroidException {
        public CanceledException() {}
        public CanceledException(String name) { super(name); }
        public CanceledException(Exception cause) { super(cause); }
    }

    public interface OnFinished {
        void onSendFinished(PendingIntent pendingIntent, Intent intent, int resultCode,
                            String resultData, Bundle resultExtras);
    }

    public static final int FLAG_ONE_SHOT = 1 << 30;
    public static final int FLAG_NO_CREATE = 1 << 29;
    public static final int FLAG_CANCEL_CURRENT = 1 << 28;
    public static final int FLAG_UPDATE_CURRENT = 1 << 27;
    public static final int FLAG_IMMUTABLE = 1 << 26;
    public static final int FLAG_MUTABLE = 1 << 25;
    public static final int FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT = 1 << 24;

    private static final int KIND_ACTIVITY = 1;
    private static final int KIND_BROADCAST = 2;
    private static final int KIND_SERVICE = 3;
    private static final int KIND_FOREGROUND_SERVICE = 4;

    private Context context;
    private int kind;
    private int requestCode;
    private Intent[] intents;
    private int flags;
    private boolean canceled;
    private boolean sentOnce;

    PendingIntent() {
    }

    private PendingIntent(Context context, int kind, int requestCode, Intent[] intents, int flags) {
        this.context = context;
        this.kind = kind;
        this.requestCode = requestCode;
        this.intents = intents;
        this.flags = flags;
    }

    public static PendingIntent getActivity(Context context, int requestCode, Intent intent, int flags) {
        return getActivity(context, requestCode, intent, flags, null);
    }

    public static PendingIntent getActivity(Context context, int requestCode, Intent intent, int flags, Bundle options) {
        return getActivities(context, requestCode, new Intent[]{intent}, flags, options);
    }

    public static PendingIntent getActivities(Context context, int requestCode, Intent[] intents, int flags) {
        return getActivities(context, requestCode, intents, flags, null);
    }

    public static PendingIntent getActivities(Context context, int requestCode, Intent[] intents, int flags, Bundle options) {
        if ((flags & FLAG_NO_CREATE) != 0) return null;
        return new PendingIntent(context, KIND_ACTIVITY, requestCode, intents, flags);
    }

    public static PendingIntent getBroadcast(Context context, int requestCode, Intent intent, int flags) {
        if ((flags & FLAG_NO_CREATE) != 0) return null;
        return new PendingIntent(context, KIND_BROADCAST, requestCode, new Intent[]{intent}, flags);
    }

    public static PendingIntent getService(Context context, int requestCode, Intent intent, int flags) {
        if ((flags & FLAG_NO_CREATE) != 0) return null;
        return new PendingIntent(context, KIND_SERVICE, requestCode, new Intent[]{intent}, flags);
    }

    public static PendingIntent getForegroundService(Context context, int requestCode, Intent intent, int flags) {
        if ((flags & FLAG_NO_CREATE) != 0) return null;
        return new PendingIntent(context, KIND_FOREGROUND_SERVICE, requestCode, new Intent[]{intent}, flags);
    }

    public Intent getIntent() {
        return intents != null && intents.length > 0 ? intents[intents.length - 1] : null;
    }

    public String getCreatorPackage() {
        return context != null ? context.getPackageName() : "";
    }

    public String getTargetPackage() {
        return getCreatorPackage();
    }

    public int getCreatorUid() { return 1000; }
    public boolean isActivity() { return kind == KIND_ACTIVITY; }
    public boolean isBroadcast() { return kind == KIND_BROADCAST; }
    public boolean isService() { return kind == KIND_SERVICE || kind == KIND_FOREGROUND_SERVICE; }
    public boolean isForegroundService() { return kind == KIND_FOREGROUND_SERVICE; }
    public boolean isImmutable() { return (flags & FLAG_IMMUTABLE) != 0; }

    public void cancel() {
        this.canceled = true;
    }

    public void send() throws CanceledException {
        send(context, 0, null, null, null, null, null);
    }

    public void send(int code) throws CanceledException {
        send(context, code, null, null, null, null, null);
    }

    public void send(Context context, int code, Intent intent) throws CanceledException {
        send(context, code, intent, null, null, null, null);
    }

    public void send(Bundle options) throws CanceledException {
        send(context, 0, null, null, null, null, options);
    }

    public void send(int code, OnFinished onFinished, Handler handler) throws CanceledException {
        send(context, code, null, onFinished, handler, null, null);
    }

    public void send(Context context, int code, Intent intent, OnFinished onFinished, Handler handler)
            throws CanceledException {
        send(context, code, intent, onFinished, handler, null, null);
    }

    public void send(Context context, int code, Intent intent, OnFinished onFinished, Handler handler,
                     String requiredPermission) throws CanceledException {
        send(context, code, intent, onFinished, handler, requiredPermission, null);
    }

    public void send(Context ctx, int code, Intent fillIn, OnFinished onFinished, Handler handler,
                     String requiredPermission, Bundle options) throws CanceledException {
        if (canceled || ((flags & FLAG_ONE_SHOT) != 0 && sentOnce)) {
            throw new CanceledException("PendingIntent canceled");
        }
        sentOnce = true;
        Context targetCtx = ctx != null ? ctx : this.context;
        Intent base = getIntent();
        Intent dispatch = base != null ? new Intent(base) : new Intent();
        if (fillIn != null && (flags & FLAG_IMMUTABLE) == 0) {
            if (fillIn.getAction() != null && dispatch.getAction() == null) dispatch.setAction(fillIn.getAction());
            if (fillIn.getData() != null && dispatch.getData() == null) dispatch.setData(fillIn.getData());
            dispatch.putExtras(fillIn);
        }
        if (targetCtx != null) {
            switch (kind) {
                case KIND_ACTIVITY -> targetCtx.startActivity(dispatch, options);
                case KIND_BROADCAST -> targetCtx.sendBroadcast(dispatch, requiredPermission);
                case KIND_SERVICE -> targetCtx.startService(dispatch);
                case KIND_FOREGROUND_SERVICE -> targetCtx.startForegroundService(dispatch);
            }
        }
        if (onFinished != null) {
            Runnable done = () -> onFinished.onSendFinished(this, dispatch, code, null, null);
            if (handler != null) handler.post(done);
            else done.run();
        }
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(kind);
        dest.writeInt(requestCode);
        dest.writeInt(this.flags);
        dest.writeParcelable(getIntent(), flags);
    }

    public static void writePendingIntentOrNullToParcel(PendingIntent sender, Parcel out) {
        out.writeParcelable(sender, 0);
    }

    public static PendingIntent readPendingIntentOrNullFromParcel(Parcel in) {
        return in.readParcelable(null);
    }

    public static final Creator<PendingIntent> CREATOR = new Creator<>() {
        @Override
        public PendingIntent createFromParcel(Parcel in) {
            int kind = in.readInt();
            int req = in.readInt();
            int flg = in.readInt();
            Intent it = in.readParcelable(null);
            return new PendingIntent(null, kind, req, new Intent[]{it}, flg);
        }

        @Override
        public PendingIntent[] newArray(int size) { return new PendingIntent[size]; }
    };

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PendingIntent other)) return false;
        return kind == other.kind && requestCode == other.requestCode
                && Objects.equals(getCreatorPackage(), other.getCreatorPackage())
                && Arrays.equals(intents, other.intents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, requestCode, getCreatorPackage());
    }
}
