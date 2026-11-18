package android.app;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.ContextMenu;
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

import javax.swing.SwingUtilities;

/**
 * Cài đặt android.app.Activity với đầy đủ vòng đời và kết nối tới cửa sổ Windows.
 */
public class Activity extends ContextThemeWrapper
        implements ComponentCallbacks2, KeyEvent.Callback, LayoutInflater.Factory2,
        View.OnCreateContextMenuListener, Window.Callback {
    public static final int RESULT_CANCELED = 0;
    public static final int RESULT_OK = -1;
    public static final int RESULT_FIRST_USER = 1;

    private Application application;
    private Intent intent;
    private Window window;
    private ActionBar actionBar;
    private CharSequence title = "";
    private int resultCode = RESULT_CANCELED;
    private Intent resultData;
    private boolean finishing;
    private boolean destroyed;
    private Runnable onFinishCallback;
    private Runnable onHostRepaint;
    private android.widget.Toolbar systemActionBarToolbar;

    public Activity() {
    }

    public final void attach(Context base, Application app, Intent intent, CharSequence initialTitle,
                             Runnable onFinishCallback, Runnable onHostRepaint) {
        attachBaseContext(base);
        this.application = app;
        this.intent = intent != null ? intent : new Intent();
        this.title = initialTitle != null ? initialTitle : "";
        this.onFinishCallback = onFinishCallback;
        this.onHostRepaint = onHostRepaint;
        this.window = new Window(this);
        this.window.setCallback(this);
        this.actionBar = new ActionBar(this, this.title, () -> {
            if (this.onHostRepaint != null) this.onHostRepaint.run();
        });
        this.window.setOnContentChanged(() -> {
            onContentChanged();
            if (this.onHostRepaint != null) this.onHostRepaint.run();
        });
    }

    public final Application getApplication() {
        return application;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(Intent newIntent) {
        this.intent = newIntent;
    }

    public Window getWindow() {
        return window;
    }

    public WindowManager getWindowManager() {
        Object svc = getSystemService(WINDOW_SERVICE);
        return svc instanceof WindowManager wm ? wm : null;
    }

    public void invalidateOptionsMenu() {
        if (onHostRepaint != null) onHostRepaint.run();
    }

    public void requestPermissions(String[] permissions, int requestCode) {
        if (permissions == null) return;
        int[] grants = new int[permissions.length];
        for (int i = 0; i < grants.length; i++) {
            grants[i] = PackageManager.PERMISSION_GRANTED;
        }
        onRequestPermissionsResult(requestCode, permissions, grants);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    }

    public LayoutInflater getLayoutInflater() {
        return LayoutInflater.from(this);
    }

    public ActionBar getActionBar() {
        return window != null && window.isNoTitle() ? null : actionBar;
    }

    public void setActionBar(android.widget.Toolbar toolbar) {
        if (toolbar != null && actionBar != null) {
            if (toolbar.getTitle() != null && toolbar.getTitle().length() > 0) {
                actionBar.setTitle(toolbar.getTitle());
            }
        }
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

    @SuppressWarnings("unchecked")
    public <T extends View> T findViewById(int id) {
        T found = window.findViewById(id);
        if (found == null && id == dev.aaow.rt.AndroidSystemResources.ID_ACTION_BAR) {
            if (systemActionBarToolbar == null) {
                systemActionBarToolbar = new android.widget.Toolbar(this);
                systemActionBarToolbar.setId(id);
                if (title != null) systemActionBarToolbar.setTitle(title);
            }
            return (T) systemActionBarToolbar;
        }
        return found;
    }

    public final boolean requestWindowFeature(int featureId) {
        return window.requestFeature(featureId);
    }

    public void setTitle(CharSequence title) {
        this.title = title != null ? title : "";
        if (actionBar != null) actionBar.setTitle(this.title);
        if (onHostRepaint != null) onHostRepaint.run();
    }

    public void setTitle(int titleId) {
        setTitle(getText(titleId));
    }

    public final CharSequence getTitle() {
        return title;
    }

    public SharedPreferences getPreferences(int mode) {
        return getSharedPreferences(getClass().getSimpleName(), mode);
    }

    public final void runOnUiThread(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    public final void setResult(int resultCode) {
        this.resultCode = resultCode;
    }

    public final void setResult(int resultCode, Intent data) {
        this.resultCode = resultCode;
        this.resultData = data;
    }

    public int getResultCode() {
        return resultCode;
    }

    public Intent getResultData() {
        return resultData;
    }

    public void startActivityForResult(Intent intent, int requestCode) {
        startActivity(intent);
    }

    public void finish() {
        if (!finishing) {
            finishing = true;
            if (onFinishCallback != null) onFinishCallback.run();
        }
    }

    public boolean isFinishing() {
        return finishing;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void recreate() {
    }

    public Object getLastNonConfigurationInstance() {
        return null;
    }

    public Object onRetainNonConfigurationInstance() {
        return null;
    }

    public final void setVolumeControlStream(int streamType) {
    }

    // --------------------------------------------------------- Lifecycle hooks

    protected void onCreate(Bundle savedInstanceState) {
    }

    protected void onPostCreate(Bundle savedInstanceState) {
    }

    protected void onStart() {
    }

    protected void onRestart() {
    }

    protected void onResume() {
    }

    protected void onPostResume() {
    }

    protected void onPause() {
    }

    protected void onStop() {
    }

    protected void onDestroy() {
        destroyed = true;
    }

    protected void onSaveInstanceState(Bundle outState) {
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    }

    protected void onNewIntent(Intent intent) {
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    @Override
    public void onContentChanged() {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public void onTrimMemory(int level) {
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return null;
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return null;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    }

    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
            return onKeyDown(event.getKeyCode(), event);
        }
        if (event != null && event.getAction() == KeyEvent.ACTION_UP) {
            return onKeyUp(event.getKeyCode(), event);
        }
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev == null) return false;
        return window != null && window.getDecorView().dispatchTouchEvent(ev);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
        return false;
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

    // --------------------------------------------------------- Gọi nội bộ bởi AAOW Runtime

    public final void performCreate(Bundle savedInstanceState) {
        onCreate(savedInstanceState);
        onPostCreate(savedInstanceState);
    }

    public final void performStart() {
        onStart();
    }

    public final void performResume() {
        onResume();
        onPostResume();
    }

    public final void performPause() {
        onPause();
    }

    public final void performStop() {
        onStop();
    }

    public final void performDestroy() {
        onDestroy();
    }
}
