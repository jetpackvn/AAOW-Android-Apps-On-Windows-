package android.os;

/** Tín hiệu cho phép hủy các truy vấn SQLite hoặc thao tác I/O bất đồng bộ. */
public final class CancellationSignal {
    public interface OnCancelListener {
        void onCancel();
    }

    private boolean isCanceled;
    private OnCancelListener onCancelListener;
    private boolean cancelInProgress;

    public boolean isCanceled() {
        synchronized (this) {
            return isCanceled;
        }
    }

    public void throwIfCanceled() {
        if (isCanceled()) {
            throw new OperationCanceledException();
        }
    }

    public void cancel() {
        final OnCancelListener listener;
        synchronized (this) {
            if (isCanceled) {
                return;
            }
            isCanceled = true;
            cancelInProgress = true;
            listener = onCancelListener;
        }
        try {
            if (listener != null) {
                listener.onCancel();
            }
        } finally {
            synchronized (this) {
                cancelInProgress = false;
                notifyAll();
            }
        }
    }

    public void setOnCancelListener(OnCancelListener listener) {
        synchronized (this) {
            waitForCancelFinishedLocked();
            if (this.onCancelListener == listener) {
                return;
            }
            this.onCancelListener = listener;
            if (!isCanceled || listener == null) {
                return;
            }
        }
        listener.onCancel();
    }

    private void waitForCancelFinishedLocked() {
        while (cancelInProgress) {
            try {
                wait();
            } catch (InterruptedException ignored) {
            }
        }
    }
}
