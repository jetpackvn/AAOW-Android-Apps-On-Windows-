package android.app;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;

public class ActionBar {
    public static final int DISPLAY_USE_LOGO = 0x1;
    public static final int DISPLAY_SHOW_HOME = 0x2;
    public static final int DISPLAY_HOME_AS_UP = 0x4;
    public static final int DISPLAY_SHOW_TITLE = 0x8;
    public static final int DISPLAY_SHOW_CUSTOM = 0x10;

    private Context context;
    private CharSequence title = "";
    private CharSequence subtitle = "";
    private boolean showing = true;
    private int displayOptions = DISPLAY_SHOW_TITLE;
    private View customView;
    private final Runnable onInvalidate;

    public ActionBar(CharSequence initialTitle, Runnable onInvalidate) {
        this(null, initialTitle, onInvalidate);
    }

    public ActionBar(Context context, CharSequence initialTitle, Runnable onInvalidate) {
        this.context = context;
        this.title = initialTitle != null ? initialTitle : "";
        this.onInvalidate = onInvalidate;
    }

    public Context getThemedContext() {
        return context;
    }

    public void setTitle(CharSequence title) {
        this.title = title != null ? title : "";
        if (onInvalidate != null) onInvalidate.run();
    }

    public void setTitle(int resId) {
        if (context != null) {
            setTitle(context.getText(resId));
        }
    }

    public CharSequence getTitle() {
        return title;
    }

    public void setSubtitle(CharSequence subtitle) {
        this.subtitle = subtitle != null ? subtitle : "";
        if (onInvalidate != null) onInvalidate.run();
    }

    public void setSubtitle(int resId) {
        if (context != null) {
            setSubtitle(context.getText(resId));
        }
    }

    public CharSequence getSubtitle() {
        return subtitle;
    }

    public void setDisplayOptions(int options) {
        this.displayOptions = options;
        if (onInvalidate != null) onInvalidate.run();
    }

    public void setDisplayOptions(int options, int mask) {
        this.displayOptions = (this.displayOptions & ~mask) | (options & mask);
        if (onInvalidate != null) onInvalidate.run();
    }

    public int getDisplayOptions() {
        return displayOptions;
    }

    public void setDisplayHomeAsUpEnabled(boolean showHomeAsUp) {
        setDisplayOptions(showHomeAsUp ? DISPLAY_HOME_AS_UP : 0, DISPLAY_HOME_AS_UP);
    }

    public boolean isDisplayHomeAsUp() {
        return (displayOptions & DISPLAY_HOME_AS_UP) != 0;
    }

    public void setDisplayShowHomeEnabled(boolean showHome) {
        setDisplayOptions(showHome ? DISPLAY_SHOW_HOME : 0, DISPLAY_SHOW_HOME);
    }

    public void setDisplayShowTitleEnabled(boolean showTitle) {
        setDisplayOptions(showTitle ? DISPLAY_SHOW_TITLE : 0, DISPLAY_SHOW_TITLE);
    }

    public void setDisplayShowCustomEnabled(boolean showCustom) {
        setDisplayOptions(showCustom ? DISPLAY_SHOW_CUSTOM : 0, DISPLAY_SHOW_CUSTOM);
    }

    public void setCustomView(int resId) {
        if (context != null) {
            setCustomView(LayoutInflater.from(context).inflate(resId, null, false));
        }
    }

    public void setCustomView(View view) {
        this.customView = view;
        if (onInvalidate != null) onInvalidate.run();
    }

    public View getCustomView() {
        return customView;
    }

    public void setBackgroundDrawable(Drawable d) {
        if (onInvalidate != null) onInvalidate.run();
    }

    public void show() {
        this.showing = true;
        if (onInvalidate != null) onInvalidate.run();
    }

    public void hide() {
        this.showing = false;
        if (onInvalidate != null) onInvalidate.run();
    }

    public boolean isShowing() {
        return showing;
    }
}
