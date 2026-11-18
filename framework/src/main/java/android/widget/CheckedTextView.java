package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class CheckedTextView extends TextView implements Checkable {
    private boolean checked;
    private Drawable checkMarkDrawable;
    private ColorStateList checkMarkTintList;
    private PorterDuff.Mode checkMarkTintMode;

    public CheckedTextView(Context context) {
        this(context, null, 0);
    }

    public CheckedTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CheckedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CheckedTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void toggle() {
        setChecked(!checked);
    }

    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (this.checked != checked) {
            this.checked = checked;
            refreshDrawableState();
            invalidate();
        }
    }

    public void setCheckMarkDrawable(int resid) {
        setCheckMarkDrawable(resid != 0 && getContext() != null ? getContext().getDrawable(resid) : null);
    }

    public void setCheckMarkDrawable(Drawable d) {
        this.checkMarkDrawable = d;
        invalidate();
    }

    public Drawable getCheckMarkDrawable() {
        return checkMarkDrawable;
    }

    public void setCheckMarkTintList(ColorStateList tint) {
        this.checkMarkTintList = tint;
        if (checkMarkDrawable != null) checkMarkDrawable.setTintList(tint);
    }

    public ColorStateList getCheckMarkTintList() {
        return checkMarkTintList;
    }

    public void setCheckMarkTintMode(PorterDuff.Mode tintMode) {
        this.checkMarkTintMode = tintMode;
        if (checkMarkDrawable != null) checkMarkDrawable.setTintMode(tintMode);
    }

    public PorterDuff.Mode getCheckMarkTintMode() {
        return checkMarkTintMode;
    }
}
