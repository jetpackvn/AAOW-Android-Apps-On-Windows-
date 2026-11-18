package android.view.inputmethod;

import android.os.IBinder;
import android.os.ResultReceiver;
import android.view.View;

public final class InputMethodManager {
    public static final int SHOW_IMPLICIT = 0x0001;
    public static final int SHOW_FORCED = 0x0002;
    public static final int HIDE_IMPLICIT_ONLY = 0x0001;
    public static final int HIDE_NOT_ALWAYS = 0x0002;
    public static final int RESULT_UNCHANGED_SHOWN = 0;
    public static final int RESULT_UNCHANGED_HIDDEN = 1;
    public static final int RESULT_SHOWN = 2;
    public static final int RESULT_HIDDEN = 3;

    private static final InputMethodManager INSTANCE = new InputMethodManager();

    InputMethodManager() {
    }

    public static InputMethodManager getInstance() {
        return INSTANCE;
    }

    public boolean showSoftInput(View view, int flags) {
        return view != null && view.requestFocus();
    }

    public boolean showSoftInput(View view, int flags, ResultReceiver resultReceiver) {
        boolean ok = showSoftInput(view, flags);
        if (resultReceiver != null) {
            resultReceiver.send(ok ? RESULT_SHOWN : RESULT_UNCHANGED_HIDDEN, null);
        }
        return ok;
    }

    public boolean hideSoftInputFromWindow(IBinder windowToken, int flags) {
        return true;
    }

    public boolean hideSoftInputFromWindow(IBinder windowToken, int flags, ResultReceiver resultReceiver) {
        if (resultReceiver != null) resultReceiver.send(RESULT_HIDDEN, null);
        return true;
    }

    public void toggleSoftInput(int showFlags, int hideFlags) {
    }

    public boolean isActive() {
        return true;
    }

    public boolean isActive(View view) {
        return view != null && view.isFocused();
    }

    public boolean isAcceptingText() {
        return true;
    }

    public void restartInput(View view) {
    }
}
