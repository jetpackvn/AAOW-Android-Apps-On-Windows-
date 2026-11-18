package android.print;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

public abstract class PrintDocumentAdapter {
    public static final String EXTRA_PRINT_PREVIEW = "EXTRA_PRINT_PREVIEW";

    public void onStart() {
    }

    public abstract void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                                  CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras);

    public abstract void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                                 CancellationSignal cancellationSignal, WriteResultCallback callback);

    public void onFinish() {
    }

    public static abstract class LayoutResultCallback {
        public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
        }

        public void onLayoutFailed(CharSequence error) {
        }

        public void onLayoutCancelled() {
        }
    }

    public static abstract class WriteResultCallback {
        public void onWriteFinished(PageRange[] pages) {
        }

        public void onWriteFailed(CharSequence error) {
        }

        public void onWriteCancelled() {
        }
    }
}
