package android.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Window {
    public static final int FEATURE_OPTIONS_PANEL = 0;
    public static final int FEATURE_NO_TITLE = 1;
    public static final int FEATURE_PROGRESS = 2;
    public static final int FEATURE_LEFT_ICON = 3;
    public static final int FEATURE_RIGHT_ICON = 4;
    public static final int FEATURE_INDETERMINATE_PROGRESS = 5;
    public static final int FEATURE_CONTEXT_MENU = 6;
    public static final int FEATURE_CUSTOM_TITLE = 7;
    public static final int FEATURE_ACTION_BAR = 8;
    public static final int FEATURE_ACTION_BAR_OVERLAY = 9;
    public static final int FEATURE_ACTION_MODE_OVERLAY = 10;
    public static final int FEATURE_CONTENT_TRANSITIONS = 12;
    public static final int FEATURE_ACTIVITY_TRANSITIONS = 13;
    public static final int ID_ANDROID_CONTENT = 0x01020002; // android.R.id.content

    public interface Callback {
        default boolean dispatchKeyEvent(KeyEvent event) {
            return false;
        }

        default boolean dispatchTouchEvent(MotionEvent event) {
            return false;
        }

        default View onCreatePanelView(int featureId) {
            return null;
        }

        default boolean onCreatePanelMenu(int featureId, Menu menu) {
            return false;
        }

        default boolean onPreparePanel(int featureId, View view, Menu menu) {
            return false;
        }

        default boolean onMenuOpened(int featureId, Menu menu) {
            return false;
        }

        default boolean onMenuItemSelected(int featureId, MenuItem item) {
            return false;
        }

        default void onWindowAttributesChanged(WindowManager.LayoutParams attrs) {
        }

        default void onContentChanged() {
        }

        default void onWindowFocusChanged(boolean hasFocus) {
        }

        default void onAttachedToWindow() {
        }

        default void onDetachedFromWindow() {
        }

        default void onPanelClosed(int featureId, Menu menu) {
        }
    }

    private final Context context;
    private final FrameLayout decorView;
    private final android.os.Binder windowToken = new android.os.Binder();
    private final WindowManager.LayoutParams attributes = new WindowManager.LayoutParams();
    private Callback callback;
    private CharSequence title = "";
    private int statusBarColor = 0xFF1A237E;
    private int navigationBarColor = 0xFF121212;
    private boolean noTitle;
    private float dimAmount = 0.5f;
    private Runnable onContentChanged;
    private final List<BackCallbackEntry> backCallbacks = new ArrayList<>();
    private final WindowInsetsController insetsController = new WindowInsetsController() {
        private int appearance;
        private int behavior = BEHAVIOR_DEFAULT;

        @Override
        public void show(int types) {
        }

        @Override
        public void hide(int types) {
        }

        @Override
        public void setSystemBarsAppearance(int appearance, int mask) {
            this.appearance = (this.appearance & ~mask) | (appearance & mask);
        }

        @Override
        public int getSystemBarsAppearance() {
            return appearance;
        }

        @Override
        public void setSystemBarsBehavior(int behavior) {
            this.behavior = behavior;
        }

        @Override
        public int getSystemBarsBehavior() {
            return behavior;
        }
    };
    private final OnBackInvokedDispatcher backDispatcher = new OnBackInvokedDispatcher() {
        @Override
        public void registerOnBackInvokedCallback(int priority, OnBackInvokedCallback callback) {
            if (callback == null) return;
            unregisterOnBackInvokedCallback(callback);
            backCallbacks.add(new BackCallbackEntry(priority, callback));
            backCallbacks.sort(Comparator.comparingInt(BackCallbackEntry::priority).reversed());
        }

        @Override
        public void unregisterOnBackInvokedCallback(OnBackInvokedCallback callback) {
            backCallbacks.removeIf(e -> e.callback == callback);
        }
    };

    private record BackCallbackEntry(int priority, OnBackInvokedCallback callback) {
    }

    public Window(Context context) {
        this.context = context;
        this.decorView = new FrameLayout(context);
        this.decorView.setId(ID_ANDROID_CONTENT);
        this.decorView.setWindowTokenInternal(windowToken);
    }

    public void setTitle(CharSequence title) {
        this.title = title != null ? title : "";
    }

    public CharSequence getTitle() {
        return title;
    }

    public void setLayout(int width, int height) {
        attributes.width = width;
        attributes.height = height;
        if (callback != null) callback.onWindowAttributesChanged(attributes);
    }

    public void setGravity(int gravity) {
        attributes.gravity = gravity;
        if (callback != null) callback.onWindowAttributesChanged(attributes);
    }

    public void setDimAmount(float amount) {
        this.dimAmount = Math.max(0f, Math.min(1f, amount));
    }

    public float getDimAmount() {
        return dimAmount;
    }

    public void setWindowAnimations(int resId) {
    }

    public void setElevation(float elevation) {
        decorView.setElevation(elevation);
    }

    public void setClipToOutline(boolean clipToOutline) {
        decorView.setClipToOutline(clipToOutline);
    }

    public View getCurrentFocus() {
        return decorView.findFocus();
    }

    public boolean superDispatchTouchEvent(MotionEvent event) {
        return decorView.dispatchTouchEvent(event);
    }

    public boolean superDispatchKeyEvent(KeyEvent event) {
        if (event == null) return false;
        View focus = getCurrentFocus();
        if (focus != null) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && focus.onKeyDown(event.getKeyCode(), event)) return true;
            if (event.getAction() == KeyEvent.ACTION_UP && focus.onKeyUp(event.getKeyCode(), event)) return true;
        }
        return false;
    }

    public void invalidatePanelMenu(int featureId) {
    }

    public void openPanel(int featureId, KeyEvent event) {
    }

    public void closePanel(int featureId) {
    }

    public void closeAllPanels() {
    }

    public boolean isFloating() {
        return false;
    }

    public Context getContext() {
        return context;
    }

    private WindowManager windowManager;

    public WindowManager getWindowManager() {
        if (windowManager != null) return windowManager;
        if (context != null) {
            Object svc = context.getSystemService(Context.WINDOW_SERVICE);
            if (svc instanceof WindowManager wm) return wm;
        }
        return null;
    }

    public void setWindowManager(WindowManager wm, android.os.IBinder appToken, String appName) {
        this.windowManager = wm;
    }

    public void setWindowManager(WindowManager wm, android.os.IBinder appToken, String appName, boolean hardwareAccelerated) {
        this.windowManager = wm;
    }

    public LayoutInflater getLayoutInflater() {
        return LayoutInflater.from(context);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public final Callback getCallback() {
        return callback;
    }

    public final WindowManager.LayoutParams getAttributes() {
        return attributes;
    }

    public void setAttributes(WindowManager.LayoutParams a) {
        if (a != null) {
            attributes.width = a.width;
            attributes.height = a.height;
            attributes.x = a.x;
            attributes.y = a.y;
            attributes.flags = a.flags;
            attributes.alpha = a.alpha;
            if (callback != null) callback.onWindowAttributesChanged(attributes);
        }
    }

    public View getDecorView() {
        return decorView;
    }

    public View peekDecorView() {
        return decorView;
    }

    public void setContentView(int layoutResID) {
        decorView.removeAllViews();
        LayoutInflater.from(context).inflate(layoutResID, decorView, true);
        if (onContentChanged != null) onContentChanged.run();
    }

    public void setContentView(View view) {
        decorView.removeAllViews();
        if (view != null) {
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp == null) {
                lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
            }
            decorView.addView(view, lp);
        }
        if (onContentChanged != null) onContentChanged.run();
    }

    public void setContentView(View view, ViewGroup.LayoutParams params) {
        decorView.removeAllViews();
        if (view != null) decorView.addView(view, params);
        if (onContentChanged != null) onContentChanged.run();
    }

    public void addContentView(View view, ViewGroup.LayoutParams params) {
        if (view != null) decorView.addView(view, params);
        if (onContentChanged != null) onContentChanged.run();
    }

    public <T extends View> T findViewById(int id) {
        return decorView.findViewById(id);
    }

    public boolean requestFeature(int featureId) {
        if (featureId == FEATURE_NO_TITLE) noTitle = true;
        return true;
    }

    public boolean isNoTitle() {
        return noTitle;
    }

    public void setBackgroundDrawable(Drawable drawable) {
        decorView.setBackground(drawable);
    }

    public void setBackgroundDrawableResource(int resId) {
        decorView.setBackgroundResource(resId);
    }

    public int getStatusBarColor() {
        return statusBarColor;
    }

    public void setStatusBarColor(int color) {
        this.statusBarColor = color;
        decorView.invalidate();
    }

    public int getNavigationBarColor() {
        return navigationBarColor;
    }

    public void setNavigationBarColor(int color) {
        this.navigationBarColor = color;
        decorView.invalidate();
    }

    public void setFlags(int flags, int mask) {
        attributes.flags = (attributes.flags & ~mask) | (flags & mask);
    }

    public void addFlags(int flags) {
        attributes.flags |= flags;
    }

    public void clearFlags(int flags) {
        attributes.flags &= ~flags;
    }

    public void setSoftInputMode(int mode) {
        attributes.softInputMode = mode;
    }

    public void setDecorFitsSystemWindows(boolean decorFitsSystemWindows) {
    }

    public void setNavigationBarContrastEnforced(boolean enforceContrast) {
    }

    public boolean isNavigationBarContrastEnforced() {
        return false;
    }

    public void setStatusBarContrastEnforced(boolean ensureContrast) {
    }

    public boolean isStatusBarContrastEnforced() {
        return false;
    }

    public void setNavigationBarDividerColor(int dividerColor) {
    }

    public int getNavigationBarDividerColor() {
        return 0;
    }

    public void setOnContentChanged(Runnable r) {
        this.onContentChanged = r;
    }

    public WindowInsetsController getInsetsController() {
        return insetsController;
    }

    public OnBackInvokedDispatcher getOnBackInvokedDispatcher() {
        return backDispatcher;
    }

    public boolean dispatchBackInvoked() {
        if (backCallbacks.isEmpty()) return false;
        BackCallbackEntry top = backCallbacks.get(0);
        top.callback().onBackInvoked();
        return true;
    }
}
