package android.content;

import android.os.Bundle;
import android.os.IBinder;

/** Lớp cơ sở nhận Intent broadcast từ Context.sendBroadcast(). */
public abstract class BroadcastReceiver {
    private PendingResult pendingResult;

    public static class PendingResult {
        private int resultCode;
        private String resultData;
        private Bundle resultExtras;
        private boolean abortBroadcast;
        private boolean finished;

        PendingResult(int resultCode, String resultData, Bundle resultExtras) {
            this.resultCode = resultCode;
            this.resultData = resultData;
            this.resultExtras = resultExtras;
        }

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

        public final void setResult(int code, String data, Bundle extras) {
            this.resultCode = code;
            this.resultData = data;
            this.resultExtras = extras;
        }

        public final boolean getAbortBroadcast() {
            return abortBroadcast;
        }

        public final void abortBroadcast() {
            this.abortBroadcast = true;
        }

        public final void clearAbortBroadcast() {
            this.abortBroadcast = false;
        }

        public final void finish() {
            this.finished = true;
        }

        public final boolean isFinished() {
            return finished;
        }
    }

    public abstract void onReceive(Context context, Intent intent);

    public final PendingResult goAsync() {
        if (pendingResult == null) {
            pendingResult = new PendingResult(0, null, null);
        }
        return pendingResult;
    }

    public final void setPendingResult(PendingResult result) {
        this.pendingResult = result;
    }

    public final PendingResult getPendingResult() {
        return pendingResult;
    }

    public IBinder peekService(Context myContext, Intent service) {
        return null;
    }

    public final void setResultCode(int code) {
        goAsync().setResultCode(code);
    }

    public final int getResultCode() {
        return pendingResult != null ? pendingResult.getResultCode() : 0;
    }

    public final void setResultData(String data) {
        goAsync().setResultData(data);
    }

    public final String getResultData() {
        return pendingResult != null ? pendingResult.getResultData() : null;
    }

    public final void setResultExtras(Bundle extras) {
        goAsync().setResultExtras(extras);
    }

    public final Bundle getResultExtras(boolean makeMap) {
        return goAsync().getResultExtras(makeMap);
    }

    public final void setResult(int code, String data, Bundle extras) {
        goAsync().setResult(code, data, extras);
    }

    public final boolean getAbortBroadcast() {
        return pendingResult != null && pendingResult.getAbortBroadcast();
    }

    public final void abortBroadcast() {
        goAsync().abortBroadcast();
    }

    public final void clearAbortBroadcast() {
        if (pendingResult != null) pendingResult.clearAbortBroadcast();
    }

    public final boolean isOrderedBroadcast() {
        return false;
    }

    public final boolean isInitialStickyBroadcast() {
        return false;
    }
}
