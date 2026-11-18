package android.preference;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

public class EditTextPreference extends DialogPreference {
    private EditText editText;
    private String text;

    public EditTextPreference(Context context) {
        this(context, null, 0, 0);
    }

    public EditTextPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public EditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public EditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.editText = new EditText(context, attrs);
    }

    public EditText getEditText() {
        return editText;
    }

    public void setText(String text) {
        this.text = text;
        persistString(text);
    }

    public String getText() {
        return text;
    }

    public boolean shouldDisableDependents() {
        return TextUtils.isEmpty(text);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        if (editText != null) editText.setText(getText());
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult && editText != null) {
            String value = editText.getText().toString();
            if (callChangeListener(value)) {
                setText(value);
            }
        }
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setText(restoreValue ? getPersistedString(text) : (defaultValue != null ? defaultValue.toString() : null));
    }
}
