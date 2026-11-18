package android.app;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;

public class Dialog implements DialogInterface {
    protected final Context context;
    protected final Window window;
    protected CharSequence title = "";
    protected boolean showing;
    protected boolean cancelable = true;
    protected OnCancelListener onCancelListener;
    protected OnDismissListener onDismissListener;

    public Dialog(Context context) {
        this.context = context;
        this.window = new Window(context);
    }

    public final Context getContext() {
        return context;
    }

    public Window getWindow() {
        return window;
    }

    public void setTitle(CharSequence title) {
        this.title = title != null ? title : "";
    }

    public void setTitle(int titleId) {
        if (context != null) setTitle(context.getText(titleId));
    }

    public void setContentView(int layoutResID) {
        window.setContentView(layoutResID);
    }

    public void setContentView(View view) {
        window.setContentView(view);
    }

    public <T extends View> T findViewById(int id) {
        return window.findViewById(id);
    }

    public void setCancelable(boolean flag) {
        this.cancelable = flag;
    }

    public boolean isShowing() {
        return showing;
    }

    public void show() {
        showing = true;
    }

    @Override
    public void cancel() {
        if (onCancelListener != null) onCancelListener.onCancel(this);
        dismiss();
    }

    @Override
    public void dismiss() {
        if (!showing) return;
        showing = false;
        if (onDismissListener != null) onDismissListener.onDismiss(this);
    }

    public void setOnCancelListener(OnCancelListener listener) {
        this.onCancelListener = listener;
    }

    public void setOnDismissListener(OnDismissListener listener) {
        this.onDismissListener = listener;
    }
}
