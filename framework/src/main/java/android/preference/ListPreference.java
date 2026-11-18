package android.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;

public class ListPreference extends DialogPreference {
    private CharSequence[] entries = new CharSequence[0];
    private CharSequence[] entryValues = new CharSequence[0];
    private String value;
    private int clickedDialogEntryIndex;

    public ListPreference(Context context) {
        this(context, null, 0, 0);
    }

    public ListPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public ListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setEntries(CharSequence[] entries) {
        this.entries = entries != null ? entries : new CharSequence[0];
    }

    public void setEntries(int entriesResId) {
        if (getContext() != null) setEntries(getContext().getResources().getStringArray(entriesResId));
    }

    public CharSequence[] getEntries() {
        return entries;
    }

    public void setEntryValues(CharSequence[] entryValues) {
        this.entryValues = entryValues != null ? entryValues : new CharSequence[0];
    }

    public void setEntryValues(int entryValuesResId) {
        if (getContext() != null) setEntryValues(getContext().getResources().getStringArray(entryValuesResId));
    }

    public CharSequence[] getEntryValues() {
        return entryValues;
    }

    public void setValue(String value) {
        this.value = value;
        persistString(value);
    }

    public String getValue() {
        return value;
    }

    public CharSequence getEntry() {
        int index = findIndexOfValue(value);
        return index >= 0 && index < entries.length ? entries[index] : null;
    }

    public int findIndexOfValue(String value) {
        if (value != null && entryValues != null) {
            for (int i = entryValues.length - 1; i >= 0; i--) {
                if (value.equals(entryValues[i].toString())) return i;
            }
        }
        return -1;
    }

    public void setValueIndex(int index) {
        if (entryValues != null && index >= 0 && index < entryValues.length) {
            setValue(entryValues[index].toString());
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        clickedDialogEntryIndex = findIndexOfValue(value);
        builder.setSingleChoiceItems(entries, clickedDialogEntryIndex, (dialog, which) -> {
            clickedDialogEntryIndex = which;
            ListPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
            dialog.dismiss();
        });
        builder.setPositiveButton(null, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && clickedDialogEntryIndex >= 0 && clickedDialogEntryIndex < entryValues.length) {
            String val = entryValues[clickedDialogEntryIndex].toString();
            if (callChangeListener(val)) {
                setValue(val);
            }
        }
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedString(value) : (defaultValue != null ? defaultValue.toString() : null));
    }
}
