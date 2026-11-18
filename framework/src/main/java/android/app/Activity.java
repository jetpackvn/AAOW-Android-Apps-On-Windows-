package android.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import javax.swing.SwingUtilities;

/**
 * Cài đặt android.app.Activity với đầy đủ vòng đời và kết nối tới cửa sổ Windows.
 */
public class Activity extends ContextThemeWrapper {
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
        this.actionBar = new ActionBar(this.title, () -> {
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
        return null;
    }

    public LayoutInflater getLayoutInflater() {
        return LayoutInflater.from(this);
    }

    public ActionBar getActionBar() {
        return window != null && window.isNoTitle() ? null : actionBar;
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

    public void onContentChanged() {
    }

    public void onConfigurationChanged(Configuration newConfig) {
    }

    public void onBackPressed() {
        finish();
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
