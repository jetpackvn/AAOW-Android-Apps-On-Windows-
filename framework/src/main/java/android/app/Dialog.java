package android.app;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.ActionMode;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.window.OnBackInvokedDispatcher;

public class Dialog implements DialogInterface, Window.Callback {
    private static volatile java.util.function.BiConsumer<Dialog, Boolean> hostDialogLifecycleBridge;

    protected final Context context;
    protected final Window window;
    protected CharSequence title = "";
    protected boolean created;
    protected boolean showing;
    protected boolean cancelable = true;
    protected boolean canceledOnTouchOutside = true;
    protected Activity ownerActivity;
    protected int volumeControlStream = -1;
    protected OnCancelListener onCancelListener;
    protected OnDismissListener onDismissListener;
    protected OnShowListener onShowListener;
    protected OnKeyListener onKeyListener;

    public static void setHostDialogLifecycleBridge(java.util.function.BiConsumer<Dialog, Boolean> bridge) {
        hostDialogLifecycleBridge = bridge;
    }

    public boolean isCancelable() {
        return cancelable;
    }

    public boolean isCanceledOnTouchOutside() {
        return canceledOnTouchOutside;
    }

    public Dialog(Context context) {
        this(context, 0);
    }

    public Dialog(Context context, int themeResId) {
        Context effective = (themeResId != 0 && context != null)
                ? new ContextThemeWrapper(context, themeResId)
                : context;
        this.context = effective;
        this.window = new Window(effective);
        this.window.setCallback(this);
        if (context instanceof Activity act) {
            this.ownerActivity = act;
        }
    }

    protected Dialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        this(context, 0);
        this.cancelable = cancelable;
        this.onCancelListener = cancelListener;
    }

    public final Context getContext() {
        return context;
    }

    public Window getWindow() {
        return window;
    }

    public LayoutInflater getLayoutInflater() {
        return LayoutInflater.from(context);
    }

    public ActionBar getActionBar() {
        return null;
    }

    public final void setOwnerActivity(Activity activity) {
        this.ownerActivity = activity;
    }

    public final Activity getOwnerActivity() {
        return ownerActivity;
    }

    public OnBackInvokedDispatcher getOnBackInvokedDispatcher() {
        return window != null ? window.getOnBackInvokedDispatcher() : null;
    }

    public View getCurrentFocus() {
        return window != null ? window.getCurrentFocus() : null;
    }

    public void setTitle(CharSequence title) {
        this.title = title != null ? title : "";
        if (window != null) window.setTitle(this.title);
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

    public void setContentView(View view, ViewGroup.LayoutParams params) {
        window.setContentView(view, params);
    }

    public void addContentView(View view, ViewGroup.LayoutParams params) {
        window.addContentView(view, params);
    }

    public <T extends View> T findViewById(int id) {
        return window.findViewById(id);
    }

    public final <T extends View> T requireViewById(int id) {
        T view = findViewById(id);
        if (view == null) {
            throw new IllegalArgumentException("ID does not reference a View inside this Dialog");
        }
        return view;
    }

    public final boolean requestWindowFeature(int featureId) {
        return window.requestFeature(featureId);
    }

    public final void setFeatureDrawableResource(int featureId, int resId) {
    }

    public final void setFeatureDrawableUri(int featureId, Uri uri) {
    }

    public final void setFeatureDrawable(int featureId, Drawable drawable) {
    }

    public final void setFeatureDrawableAlpha(int featureId, int alpha) {
    }

    public final void setVolumeControlStream(int streamType) {
        this.volumeControlStream = streamType;
    }

    public final int getVolumeControlStream() {
        return volumeControlStream;
    }

    public void setCancelable(boolean flag) {
        this.cancelable = flag;
    }

    public void setCanceledOnTouchOutside(boolean cancel) {
        this.canceledOnTouchOutside = cancel;
        if (cancel && !cancelable) {
            cancelable = true;
        }
    }

    public boolean isShowing() {
        return showing;
    }

    public void create() {
        if (!created) {
            dispatchOnCreate(null);
        }
    }

    private void dispatchOnCreate(Bundle savedInstanceState) {
        if (!created) {
            onCreate(savedInstanceState);
            created = true;
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
    }

    protected void onStart() {
    }

    protected void onStop() {
    }

    public Bundle onSaveInstanceState() {
        return new Bundle();
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
    }

    public void show() {
        if (showing) return;
        if (!created) {
            dispatchOnCreate(null);
        }
        onStart();
        showing = true;
        if (hostDialogLifecycleBridge != null) {
            hostDialogLifecycleBridge.accept(this, Boolean.TRUE);
        }
        if (onShowListener != null) {
            onShowListener.onShow(this);
        }
    }

    public void hide() {
        if (!showing) return;
        showing = false;
        if (hostDialogLifecycleBridge != null) {
            hostDialogLifecycleBridge.accept(this, Boolean.FALSE);
        }
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
        if (hostDialogLifecycleBridge != null) {
            hostDialogLifecycleBridge.accept(this, Boolean.FALSE);
        }
        onStop();
        if (onDismissListener != null) onDismissListener.onDismiss(this);
    }

    public void onBackPressed() {
        if (cancelable) {
            cancel();
        }
    }

    public void setOnCancelListener(OnCancelListener listener) {
        this.onCancelListener = listener;
    }

    public void setOnDismissListener(OnDismissListener listener) {
        this.onDismissListener = listener;
    }

    public void setOnShowListener(OnShowListener listener) {
        this.onShowListener = listener;
    }

    public void setOnKeyListener(OnKeyListener onKeyListener) {
        this.onKeyListener = onKeyListener;
    }

    public void setCancelMessage(Message msg) {
    }

    public void setDismissMessage(Message msg) {
    }

    public void takeKeyEvents(boolean get) {
    }

    public void invalidateOptionsMenu() {
    }

    public void openOptionsMenu() {
    }

    public void closeOptionsMenu() {
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    public void onOptionsMenuClosed(Menu menu) {
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return true;
        }
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return false;
    }

    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    public boolean onTrackballEvent(MotionEvent event) {
        return false;
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (onKeyListener != null && event != null && onKeyListener.onKey(this, event.getKeyCode(), event)) {
            return true;
        }
        if (window != null && window.superDispatchKeyEvent(event)) {
            return true;
        }
        if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
            return onKeyDown(event.getKeyCode(), event);
        }
        if (event != null && event.getAction() == KeyEvent.ACTION_UP) {
            return onKeyUp(event.getKeyCode(), event);
        }
        return false;
    }

    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (window != null && window.superDispatchTouchEvent(ev)) {
            return true;
        }
        return onTouchEvent(ev);
    }

    public boolean dispatchTrackballEvent(MotionEvent ev) {
        return onTrackballEvent(ev);
    }

    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        return onGenericMotionEvent(ev);
    }

    @Override
    public void onWindowAttributesChanged(WindowManager.LayoutParams params) {
    }

    @Override
    public void onContentChanged() {
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    }

    @Override
    public void onAttachedToWindow() {
    }

    @Override
    public void onDetachedFromWindow() {
    }

    @Override
    public View onCreatePanelView(int featureId) {
        return null;
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        return false;
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        return false;
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        return false;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        return false;
    }

    @Override
    public void onPanelClosed(int featureId, Menu menu) {
    }

    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
        return null;
    }

    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) {
        return null;
    }

    public void onActionModeStarted(ActionMode mode) {
    }

    public void onActionModeFinished(ActionMode mode) {
    }
}
