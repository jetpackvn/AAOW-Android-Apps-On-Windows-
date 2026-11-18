package android.content;

import android.os.Bundle;

public abstract class BroadcastReceiver {
    private int resultCode;
    private String resultData;
    private Bundle resultExtras;

    public abstract void onReceive(Context context, Intent intent);

    public final void setResultCode(int code) {
        this.resultCode = code;
    }

    public final int getResultCode() {
        return resultCode;
    }

    public final void setResultData(String data) {
        this.resultData = data;
    }

    public final String getResultData() {
        return resultData;
    }

    public final void setResultExtras(Bundle extras) {
        this.resultExtras = extras;
    }

    public final Bundle getResultExtras(boolean makeMap) {
        if (resultExtras == null && makeMap) resultExtras = new Bundle();
        return resultExtras;
    }

    public final void abortBroadcast() {
    }
}
