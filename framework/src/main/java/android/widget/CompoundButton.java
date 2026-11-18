package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import dev.aaow.apk.ResValue;
import dev.aaow.apk.XmlElement;

public abstract class CompoundButton extends Button implements Checkable {
    public interface OnCheckedChangeListener {
        void onCheckedChanged(CompoundButton buttonView, boolean isChecked);
    }

    private boolean checked;
    private OnCheckedChangeListener onCheckedChangeListener;

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

    @Override
    public boolean performClick() {
        toggle();
        return super.performClick() || true;
    }
}
