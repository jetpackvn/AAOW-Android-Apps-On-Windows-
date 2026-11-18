package android.preference;

import android.content.Context;
import android.util.AttributeSet;

public class CheckBoxPreference extends TwoStatePreference {
    public CheckBoxPreference(Context context) {
        this(context, null, 0, 0);
    }

    public CheckBoxPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public CheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}
