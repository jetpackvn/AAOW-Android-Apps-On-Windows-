package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import dev.aaow.apk.ResValue;
import dev.aaow.apk.XmlElement;

public abstract class CompoundButton extends Button implements Checkable {
    public interface OnCheckedChangeListener {
        void onCheckedChanged(CompoundButton buttonView, boolean isChecked);
    }

    private static final int[] CHECKED_STATE_SET = { 0x010100a0 }; // android.R.attr.state_checked

    private boolean checked;
    private OnCheckedChangeListener onCheckedChangeListener;
    private android.graphics.drawable.Drawable buttonDrawable;
    private android.content.res.ColorStateList buttonTintList;
    private android.graphics.PorterDuff.Mode buttonTintMode;

    public CompoundButton(Context context) {
        this(context, null, 0);
    }

    public CompoundButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CompoundButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.textColor = 0xFF1C1B1F;
        if (attrs instanceof AttributeSet.XmlAttributeSet xas && context != null) {
            XmlElement.Attribute c = xas.element.attr("checked");
            if (c != null) {
                ResValue rv = context.getResources().getTable().resolve(c.value);
                if (rv != null) this.checked = rv.data() != 0;
            }
        }
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        if (this.checked != checked) {
            this.checked = checked;
            if (onCheckedChangeListener != null) {
                onCheckedChangeListener.onCheckedChanged(this, this.checked);
            }
            invalidate();
        }
    }

    public void toggle() {
        setChecked(!checked);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.onCheckedChangeListener = listener;
    }

    public void setButtonDrawable(int resId) {
        if (resId != 0 && getResources() != null) {
            setButtonDrawable(getResources().getDrawable(resId, getContext() != null ? getContext().getTheme() : null));
        } else {
            setButtonDrawable((android.graphics.drawable.Drawable) null);
        }
    }

    public void setButtonDrawable(android.graphics.drawable.Drawable drawable) {
        this.buttonDrawable = drawable;
        if (buttonDrawable != null && buttonTintList != null) {
            buttonDrawable.setTintList(buttonTintList);
        }
        requestLayout();
        invalidate();
    }

    public android.graphics.drawable.Drawable getButtonDrawable() {
        return buttonDrawable;
    }

    public void setButtonTintList(android.content.res.ColorStateList tint) {
        this.buttonTintList = tint;
        if (buttonDrawable != null) {
            buttonDrawable.setTintList(tint);
        }
        invalidate();
    }

    public android.content.res.ColorStateList getButtonTintList() {
        return buttonTintList;
    }

    public void setButtonTintMode(android.graphics.PorterDuff.Mode tintMode) {
        this.buttonTintMode = tintMode;
        if (buttonDrawable != null && tintMode != null) {
            buttonDrawable.setTintMode(tintMode);
        }
        invalidate();
    }

    public android.graphics.PorterDuff.Mode getButtonTintMode() {
        return buttonTintMode;
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    public boolean performClick() {
        toggle();
        return super.performClick() || true;
    }
}
