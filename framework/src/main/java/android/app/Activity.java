package android.app;

import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.window.OnBackInvokedDispatcher;
import dev.aaow.rt.AndroidRuntimeContext;
import dev.aaow.rt.SimpleMenu;
import dev.aaow.rt.WindowsFilePickerBridge;

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
    private FragmentManager fragmentManager;
    private SimpleMenu optionsMenu;
    private boolean optionsMenuCreated;
    private Runnable openOptionsMenuBridge;
    private LayoutInflater cachedLayoutInflater;
    private final java.util.List<Application.ActivityLifecycleCallbacks> lifecycleCallbacks =
            new java.util.concurrent.CopyOnWriteArrayList<>();

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
        this.fragmentManager = new FragmentManager.Impl(this, null);
    }

    public void setOpenOptionsMenuBridge(Runnable bridge) {
        this.openOptionsMenuBridge = bridge;
    }

    public final Application getApplication() {
        return application;
    }

    public Intent getIntent() {
        return intent;
    }

    public ComponentName getComponentName() {
        if (intent != null && intent.getComponent() != null) {
            return intent.getComponent();
        }
        return new ComponentName(getPackageName() != null ? getPackageName() : "", getClass().getName());
    }

    public String getLocalClassName() {
        String pkg = getPackageName();
        String cls = getClass().getName();
        if (pkg != null && !pkg.isEmpty() && cls.startsWith(pkg + ".")) {
            return cls.substring(pkg.length() + 1);
        }
        return cls;
    }

    public ComponentName getCallingActivity() {
        return null;
    }

    public String getCallingPackage() {
        return null;
    }

    public final Activity getParent() {
        return null;
    }

    public final boolean isChild() {
        return false;
    }

    public int getTaskId() {
        return 1;
    }

    public boolean isTaskRoot() {
        return true;
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

    @Override
    public Object getSystemService(String name) {
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            if (cachedLayoutInflater == null) {
                cachedLayoutInflater = new LayoutInflater(this);
            }
            return cachedLayoutInflater;
        }
        return super.getSystemService(name);
    }

    public void invalidateOptionsMenu() {
        optionsMenuCreated = false;
        rebuildOptionsMenu();
        if (onHostRepaint != null) onHostRepaint.run();
    }

    public MenuInflater getMenuInflater() {
        return new MenuInflater(this);
    }

    public SimpleMenu getOptionsMenu() {
        if (!optionsMenuCreated) {
            rebuildOptionsMenu();
        }
        return optionsMenu;
    }

    public void rebuildOptionsMenu() {
        if (optionsMenu == null) {
            optionsMenu = new SimpleMenu(this);
        } else {
            optionsMenu.clear();
        }
        try {
            boolean show = onCreateOptionsMenu(optionsMenu);
            if (fragmentManager != null) {
                show |= fragmentManager.dispatchCreateOptionsMenu(optionsMenu, getMenuInflater());
            }
            if (show) {
                onPrepareOptionsMenu(optionsMenu);
                if (fragmentManager != null) {
                    fragmentManager.dispatchPrepareOptionsMenu(optionsMenu);
                }
            }
        } catch (Throwable ignored) {
        }
        optionsMenuCreated = true;
    }

    public void openOptionsMenu() {
        rebuildOptionsMenu();
        if (openOptionsMenuBridge != null) {
            openOptionsMenuBridge.run();
        }
    }

    public void closeOptionsMenu() {
    }

    public boolean dispatchOptionsItemSelected(MenuItem item) {
        if (item == null) return false;
        if (item instanceof SimpleMenu.SimpleMenuItem smi && smi.invokeClick()) return true;
        if (onOptionsItemSelected(item)) return true;
        return fragmentManager != null && fragmentManager.dispatchOptionsItemSelected(item);
    }

    public FragmentManager getFragmentManager() {
        if (fragmentManager == null) {
            fragmentManager = new FragmentManager.Impl(this, null);
        }
        return fragmentManager;
    }

    public void onAttachFragment(Fragment fragment) {
    }

    public OnBackInvokedDispatcher getOnBackInvokedDispatcher() {
        return window != null ? window.getOnBackInvokedDispatcher() : null;
    }

    public ActionMode startActionMode(ActionMode.Callback callback) {
        return null;
    }

    public void onActionModeStarted(ActionMode mode) {
    }

    public void onActionModeFinished(ActionMode mode) {
    }

    public void requestPermissions(String[] permissions, int requestCode) {
        if (permissions == null) return;
        final String[] copy = permissions.clone();
        final int[] grants = new int[copy.length];
        for (int i = 0; i < grants.length; i++) {
            grants[i] = PackageManager.PERMISSION_GRANTED;
        }
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            if (!isDestroyed() && !isFinishing()) {
                onRequestPermissionsResult(requestCode, copy, grants);
            }
        });
    }

    public boolean shouldShowRequestPermissionRationale(String permission) {
        return false;
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
        startActivityForResult(intent, requestCode, null);
    }

    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        if (intent == null) return;
        if (dev.aaow.rt.WindowsBrowserBridge.isWebBrowserIntent(intent)) {
            if (dev.aaow.rt.WindowsBrowserBridge.handleBrowserIntent(this, intent)) {
                return;
            }
        }
        if (WindowsFilePickerBridge.isFilePickerIntent(intent)) {
            Intent picked = WindowsFilePickerBridge.pickFiles(this, intent);
            int code = picked != null ? RESULT_OK : RESULT_CANCELED;
            onActivityResult(requestCode, code, picked);
            if (onHostRepaint != null) onHostRepaint.run();
            return;
        }
        Context c = getBaseContext();
        while (c instanceof ContextWrapper cw) {
            if (c instanceof AndroidRuntimeContext) break;
            c = cw.getBaseContext();
        }
        if (c instanceof AndroidRuntimeContext arc) {
            arc.dispatchStartActivityForResult(this, intent, requestCode);
        } else {
            startActivity(intent, options);
        }
    }

    public final void dispatchActivityResult(int requestCode, int resultCode, Intent data) {
        onActivityResult(requestCode, resultCode, data);
        if (onHostRepaint != null) onHostRepaint.run();
    }

    public void overridePendingTransition(int enterAnim, int exitAnim) {
    }

    public void setRequestedOrientation(int requestedOrientation) {
    }

    public int getRequestedOrientation() {
        return -1;
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
        if (window != null && window.dispatchBackInvoked()) {
            return;
        }
        if (fragmentManager != null && fragmentManager.popBackStackImmediate()) {
            return;
        }
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

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            boolean show = onCreateOptionsMenu(menu);
            if (fragmentManager != null) {
                show |= fragmentManager.dispatchCreateOptionsMenu(menu, getMenuInflater());
            }
            return show;
        }
        return false;
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL && menu != null) {
            boolean goforit = onPrepareOptionsMenu(menu);
            if (fragmentManager != null) {
                goforit |= fragmentManager.dispatchPrepareOptionsMenu(menu);
            }
            return goforit;
        }
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            return dispatchOptionsItemSelected(item);
        }
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

    public void registerActivityLifecycleCallbacks(Application.ActivityLifecycleCallbacks callback) {
        if (callback != null && !lifecycleCallbacks.contains(callback)) {
            lifecycleCallbacks.add(callback);
        }
    }

    public void unregisterActivityLifecycleCallbacks(Application.ActivityLifecycleCallbacks callback) {
        lifecycleCallbacks.remove(callback);
    }

    private void notifyLifecycle(java.util.function.Consumer<Application.ActivityLifecycleCallbacks> action) {
        for (Application.ActivityLifecycleCallbacks cb : lifecycleCallbacks) {
            try {
                action.accept(cb);
            } catch (Throwable ignored) {
            }
        }
    }

    // --------------------------------------------------------- Gọi nội bộ bởi AAOW Runtime

    public final void performCreate(Bundle savedInstanceState) {
        notifyLifecycle(cb -> cb.onActivityPreCreated(this, savedInstanceState));
        onCreate(savedInstanceState);
        notifyLifecycle(cb -> cb.onActivityCreated(this, savedInstanceState));
        notifyLifecycle(cb -> cb.onActivityPostCreated(this, savedInstanceState));
        if (application != null) application.dispatchActivityCreated(this, savedInstanceState);
        if (fragmentManager != null) fragmentManager.dispatchCreate();
        onPostCreate(savedInstanceState);
        rebuildOptionsMenu();
    }

    public final void performStart() {
        notifyLifecycle(cb -> cb.onActivityPreStarted(this));
        onStart();
        notifyLifecycle(cb -> cb.onActivityStarted(this));
        notifyLifecycle(cb -> cb.onActivityPostStarted(this));
        if (application != null) application.dispatchActivityStarted(this);
        if (fragmentManager != null) fragmentManager.dispatchStart();
    }

    public final void performResume() {
        notifyLifecycle(cb -> cb.onActivityPreResumed(this));
        onResume();
        notifyLifecycle(cb -> cb.onActivityResumed(this));
        notifyLifecycle(cb -> cb.onActivityPostResumed(this));
        if (application != null) application.dispatchActivityResumed(this);
        if (fragmentManager != null) fragmentManager.dispatchResume();
        onPostResume();
        invalidateOptionsMenu();
    }

    public final void performPause() {
        notifyLifecycle(cb -> cb.onActivityPrePaused(this));
        notifyLifecycle(cb -> cb.onActivityPaused(this));
        if (application != null) application.dispatchActivityPaused(this);
        if (fragmentManager != null) fragmentManager.dispatchPause();
        onPause();
        notifyLifecycle(cb -> cb.onActivityPostPaused(this));
    }

    public final void performStop() {
        notifyLifecycle(cb -> cb.onActivityPreStopped(this));
        notifyLifecycle(cb -> cb.onActivityStopped(this));
        if (application != null) application.dispatchActivityStopped(this);
        if (fragmentManager != null) fragmentManager.dispatchStop();
        onStop();
        notifyLifecycle(cb -> cb.onActivityPostStopped(this));
    }

    public final void performDestroy() {
        notifyLifecycle(cb -> cb.onActivityPreDestroyed(this));
        if (fragmentManager != null) fragmentManager.dispatchDestroy();
        onDestroy();
        notifyLifecycle(cb -> cb.onActivityDestroyed(this));
        notifyLifecycle(cb -> cb.onActivityPostDestroyed(this));
        if (application != null) application.dispatchActivityDestroyed(this);
    }
}
