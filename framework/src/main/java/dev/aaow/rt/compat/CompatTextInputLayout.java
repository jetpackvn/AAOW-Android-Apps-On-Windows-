package dev.aaow.rt.compat;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import dev.aaow.apk.XmlElement;

/**
 * Container tương thích cho com.google.android.material.textfield.TextInputLayout:
 * hiển thị nhãn gợi ý (hint label), trường EditText bên trong và thông báo lỗi (error).
 */
public class CompatTextInputLayout extends LinearLayout {
    private final TextView hintLabel;
    private final TextView errorLabel;
    private EditText editText;
    private CharSequence hint;
    private CharSequence error;

    public CompatTextInputLayout(Context context) {
        this(context, null, 0);
    }

    public CompatTextInputLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CompatTextInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        hintLabel = new TextView(context);
        hintLabel.setTextSize(12f);
        hintLabel.setTextColor(0xFF5C6BC0);
        hintLabel.setVisibility(GONE);
        super.addView(hintLabel, -1, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        errorLabel = new TextView(context);
        errorLabel.setTextSize(11f);
        errorLabel.setTextColor(0xFFD32F2F);
        errorLabel.setVisibility(GONE);
        super.addView(errorLabel, -1, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        if (attrs instanceof AttributeSet.XmlAttributeSet xas && context != null) {
            Resources res = context.getResources();
            XmlElement.Attribute h = xas.element.attr("hint");
            if (h != null) {
                setHint(res.resolveString(h.value));
            }
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (child instanceof EditText et) {
            this.editText = et;
            if ((hint == null || hint.length() == 0) && et.getHint() != null && et.getHint().length() > 0) {
                setHint(et.getHint());
            } else if (hint != null && (et.getHint() == null || et.getHint().length() == 0)) {
                et.setHint(hint);
            }
            int insertPos = Math.max(1, getChildCount() - 1);
            super.addView(child, insertPos, params);
            return;
        }
        int insertPos = Math.max(1, getChildCount() - 1);
        super.addView(child, insertPos, params);
    }

    public EditText getEditText() {
        return editText;
    }

    public CharSequence getHint() {
        return hint;
    }

    public void setHint(CharSequence hint) {
        this.hint = hint;
        if (hint != null && hint.length() > 0) {
            hintLabel.setText(hint);
            hintLabel.setVisibility(VISIBLE);
            if (editText != null && (editText.getHint() == null || editText.getHint().length() == 0)) {
                editText.setHint(hint);
            }
        } else {
            hintLabel.setVisibility(GONE);
        }
        requestLayout();
        invalidate();
    }

    public CharSequence getError() {
        return error;
    }

    public void setError(CharSequence errorText) {
        this.error = errorText;
        if (errorText != null && errorText.length() > 0) {
            errorLabel.setText(errorText);
            errorLabel.setVisibility(VISIBLE);
        } else {
            errorLabel.setVisibility(GONE);
        }
        requestLayout();
        invalidate();
    }

    public void setErrorEnabled(boolean enabled) {
        if (!enabled) setError(null);
    }
}
