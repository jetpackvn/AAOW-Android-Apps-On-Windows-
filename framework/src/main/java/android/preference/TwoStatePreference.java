package android.preference;

import android.content.Context;
import android.util.AttributeSet;

public abstract class TwoStatePreference extends Preference {
    protected boolean checked;

    public TwoStatePreference(Context context) {
        this(context, null, 0, 0);
    }

    public TwoStatePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public TwoStatePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TwoStatePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        if (this.checked != checked) {
            this.checked = checked;
            persistBoolean(checked);
            notifyChanged();
        }
    }

    @Override
    protected void onClick() {
        boolean newValue = !isChecked();
        if (callChangeListener(newValue)) {
            setChecked(newValue);
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        boolean def = defaultValue instanceof Boolean b && b;
        setChecked(restorePersistedValue ? getPersistedBoolean(def) : def);
    }
}
