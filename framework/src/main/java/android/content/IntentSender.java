package android.content;

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AndroidException;

import java.util.Objects;

public class IntentSender implements Parcelable {
    public static class SendIntentException extends AndroidException {
        public SendIntentException() {
        }

        public SendIntentException(String name) {
            super(name);
        }

        public SendIntentException(Exception cause) {
            super(cause);
        }
    }

    public interface OnFinished {
        void onSendFinished(IntentSender intentSender, Intent intent, int resultCode,
                            String resultData, Bundle resultExtras);
    }

    private final PendingIntent target;

    public IntentSender(PendingIntent target) {
        this.target = target;
    }

    public void sendIntent(Context context, int code, Intent intent,
                           OnFinished onFinished, Handler handler) throws SendIntentException {
        sendIntent(context, code, intent, onFinished, handler, null);
    }

    public void sendIntent(Context context, int code, Intent intent,
                           OnFinished onFinished, Handler handler, String requiredPermission)
            throws SendIntentException {
        if (target == null) {
            throw new SendIntentException("Null IntentSender target");
        }
        try {
            target.send(context, code, intent,
                    onFinished != null ? (pi, it, rc, rd, re) -> onFinished.onSendFinished(this, it, rc, rd, re) : null,
                    handler, requiredPermission);
        } catch (PendingIntent.CanceledException e) {
            throw new SendIntentException(e);
        }
    }

    public String getCreatorPackage() {
        return target != null ? target.getCreatorPackage() : "";
    }

    public int getCreatorUid() {
        return target != null ? target.getCreatorUid() : 1000;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(target, flags);
    }

    public static void writeIntentSenderOrNullToParcel(IntentSender sender, Parcel out) {
        out.writeParcelable(sender, 0);
    }

    public static IntentSender readIntentSenderOrNullFromParcel(Parcel in) {
        return in.readParcelable(null);
    }

    public static final Creator<IntentSender> CREATOR = new Creator<>() {
        @Override
        public IntentSender createFromParcel(Parcel in) {
            PendingIntent pi = in.readParcelable(null);
            return new IntentSender(pi);
        }

        @Override
        public IntentSender[] newArray(int size) {
            return new IntentSender[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        return o instanceof IntentSender other && Objects.equals(target, other.target);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(target);
    }
}
