package android.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;

public class Window {
    public static final int FEATURE_NO_TITLE = 1;
    public static final int FEATURE_ACTION_BAR = 8;
    public static final int ID_ANDROID_CONTENT = 0x01020002; // android.R.id.content

    private final Context context;
    private final FrameLayout decorView;
    private int statusBarColor = 0xFF1A237E;
    private int navigationBarColor = 0xFF121212;
    private boolean noTitle;
    private Runnable onContentChanged;

    public Window(Context context) {
        this.context = context;
        this.decorView = new FrameLayout(context);
        this.decorView.setId(ID_ANDROID_CONTENT);
    }

    public Context getContext() {
        return context;
    }

    public View getDecorView() {
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
    }

    public void addFlags(int flags) {
    }

    public void clearFlags(int flags) {
    }

    public void setOnContentChanged(Runnable r) {
        this.onContentChanged = r;
    }
}
