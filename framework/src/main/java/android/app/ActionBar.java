package android.app;

import android.graphics.drawable.Drawable;

public class ActionBar {
    private CharSequence title = "";
    private CharSequence subtitle = "";
    private boolean showing = true;
    private boolean displayHomeAsUp;
    private final Runnable onInvalidate;

    public ActionBar(CharSequence initialTitle, Runnable onInvalidate) {
        this.title = initialTitle != null ? initialTitle : "";
        this.onInvalidate = onInvalidate;
    }

    public void setTitle(CharSequence title) {
        this.title = title != null ? title : "";
        if (onInvalidate != null) onInvalidate.run();
    }

    public void setTitle(int resId) {
    }

    public CharSequence getTitle() {
        return title;
    }

    public void setSubtitle(CharSequence subtitle) {
        this.subtitle = subtitle != null ? subtitle : "";
        if (onInvalidate != null) onInvalidate.run();
    }

    public CharSequence getSubtitle() {
        return subtitle;
    }

    public void setDisplayHomeAsUpEnabled(boolean showHomeAsUp) {
        this.displayHomeAsUp = showHomeAsUp;
        if (onInvalidate != null) onInvalidate.run();
    }

    public boolean isDisplayHomeAsUp() {
        return displayHomeAsUp;
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
