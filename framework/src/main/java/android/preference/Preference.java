package android.preference;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.AbsSavedState;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import dev.aaow.apk.ResValue;
import dev.aaow.apk.XmlElement;

public class Preference implements Comparable<Preference> {
    public static final int DEFAULT_ORDER = Integer.MAX_VALUE;

    public interface OnPreferenceChangeListener {
        boolean onPreferenceChange(Preference preference, Object newValue);
    }

    public interface OnPreferenceClickListener {
        boolean onPreferenceClick(Preference preference);
    }

    private final Context context;
    private PreferenceManager preferenceManager;
    private PreferenceGroup parent;
    private String key;
    private CharSequence title = "";
    private CharSequence summary = "";
    private int titleRes;
    private Drawable icon;
    private Intent intent;
    private String fragment;
    private int order = DEFAULT_ORDER;
    private boolean enabled = true;
    private boolean selectable = true;
    private boolean persistent = true;
    private Object defaultValue;
    private Bundle extras;
    private OnPreferenceChangeListener onChangeListener;
    private OnPreferenceClickListener onClickListener;
    private Runnable onChangeObserver;

    public Preference(Context context) {
        this(context, null, 0, 0);
    }

    public Preference(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public Preference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public Preference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this.context = context;
        if (attrs instanceof AttributeSet.XmlAttributeSet xas && context != null) {
            Resources res = context.getResources();
            XmlElement el = xas.element;
            XmlElement.Attribute k = el.attr("key");
            if (k != null) this.key = res.resolveString(k.value);
            XmlElement.Attribute t = el.attr("title");
            if (t != null) {
                String s = res.resolveString(t.value);
                if (s != null) this.title = s;
            }
            XmlElement.Attribute sum = el.attr("summary");
            if (sum != null) {
                String s = res.resolveString(sum.value);
                if (s != null) this.summary = s;
            }
            XmlElement.Attribute def = el.attr("defaultValue");
            if (def != null) {
                ResValue rv = res.getTable().resolve(def.value);
                if (rv != null) {
                    if (rv.isString()) this.defaultValue = rv.string();
                    else if (rv.type() == ResValue.TYPE_INT_BOOLEAN) this.defaultValue = rv.data() != 0;
                    else if (rv.isInt()) this.defaultValue = rv.data();
                }
            }
        }
    }

    void setChangeObserver(Runnable r) {
        this.onChangeObserver = r;
    }

    void setParent(PreferenceGroup parent) {
        this.parent = parent;
    }

    public PreferenceGroup getParent() {
        return parent;
    }

    public Context getContext() {
        return context;
    }

    public PreferenceManager getPreferenceManager() {
        return preferenceManager;
    }

    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        this.preferenceManager = preferenceManager;
        onSetInitialValue(shouldPersist(), defaultValue);
    }

    protected void onAttachedToActivity() {
    }

    public SharedPreferences getSharedPreferences() {
        if (preferenceManager != null) return preferenceManager.getSharedPreferences();
        return context != null ? PreferenceManager.getDefaultSharedPreferences(context) : null;
    }

    public SharedPreferences.Editor getEditor() {
        SharedPreferences sp = getSharedPreferences();
        return sp != null ? sp.edit() : null;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public boolean hasKey() {
        return key != null && !key.isEmpty();
    }

    public void setTitle(CharSequence title) {
        this.title = title != null ? title : "";
        notifyChanged();
    }

    public void setTitle(int titleResId) {
        this.titleRes = titleResId;
        if (context != null) setTitle(context.getText(titleResId));
    }

    public CharSequence getTitle() {
        return title;
    }

    public int getTitleRes() {
        return titleRes;
    }

    public void setSummary(CharSequence summary) {
        this.summary = summary != null ? summary : "";
        notifyChanged();
    }

    public void setSummary(int summaryResId) {
        if (context != null) setSummary(context.getText(summaryResId));
    }

    public CharSequence getSummary() {
        return summary;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
        notifyChanged();
    }

    public void setIcon(int iconResId) {
        if (context != null) setIcon(context.getDrawable(iconResId));
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setFragment(String fragment) {
        this.fragment = fragment;
    }

    public String getFragment() {
        return fragment;
    }

    public Bundle getExtras() {
        if (extras == null) extras = new Bundle();
        return extras;
    }

    public Bundle peekExtras() {
        return extras;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        notifyChanged();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public boolean isPersistent() {
        return persistent;
    }

    protected boolean shouldPersist() {
        return preferenceManager != null && isPersistent() && hasKey();
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    protected Object onGetDefaultValue(TypedArray a, int index) {
        return null;
    }

    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    }

    public void setOnPreferenceChangeListener(OnPreferenceChangeListener listener) {
        this.onChangeListener = listener;
    }

    public OnPreferenceChangeListener getOnPreferenceChangeListener() {
        return onChangeListener;
    }

    public void setOnPreferenceClickListener(OnPreferenceClickListener listener) {
        this.onClickListener = listener;
    }

    public OnPreferenceClickListener getOnPreferenceClickListener() {
        return onClickListener;
    }

    protected boolean callChangeListener(Object newValue) {
        return onChangeListener == null || onChangeListener.onPreferenceChange(this, newValue);
    }

    protected void onClick() {
    }

    public void performClick(PreferenceScreen preferenceScreen) {
        if (!isEnabled()) return;
        onClick();
        if (onClickListener != null && onClickListener.onPreferenceClick(this)) return;
        if (intent != null && context != null) {
            context.startActivity(intent);
        }
    }

    protected void notifyChanged() {
        if (onChangeObserver != null) onChangeObserver.run();
    }

    protected void notifyHierarchyChanged() {
        notifyChanged();
    }

    protected boolean persistString(String value) {
        if (!shouldPersist()) return false;
        SharedPreferences.Editor ed = getEditor();
        if (ed != null) {
            ed.putString(key, value);
            ed.apply();
            return true;
        }
        return false;
    }

    protected String getPersistedString(String defaultReturnValue) {
        if (!shouldPersist()) return defaultReturnValue;
        SharedPreferences sp = getSharedPreferences();
        return sp != null ? sp.getString(key, defaultReturnValue) : defaultReturnValue;
    }

    protected boolean persistBoolean(boolean value) {
        if (!shouldPersist()) return false;
        SharedPreferences.Editor ed = getEditor();
        if (ed != null) {
            ed.putBoolean(key, value);
            ed.apply();
            return true;
        }
        return false;
    }

    protected boolean getPersistedBoolean(boolean defaultReturnValue) {
        if (!shouldPersist()) return defaultReturnValue;
        SharedPreferences sp = getSharedPreferences();
        return sp != null ? sp.getBoolean(key, defaultReturnValue) : defaultReturnValue;
    }

    protected boolean persistInt(int value) {
        if (!shouldPersist()) return false;
        SharedPreferences.Editor ed = getEditor();
        if (ed != null) {
            ed.putInt(key, value);
            ed.apply();
            return true;
        }
        return false;
    }

    protected int getPersistedInt(int defaultReturnValue) {
        if (!shouldPersist()) return defaultReturnValue;
        SharedPreferences sp = getSharedPreferences();
        return sp != null ? sp.getInt(key, defaultReturnValue) : defaultReturnValue;
    }

    public View getView(View convertView, ViewGroup parent) {
        View v = onCreateView(parent);
        onBindView(v);
        return v;
    }

    protected View onCreateView(ViewGroup parent) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(16, 12, 16, 12);
        TextView tvTitle = new TextView(context);
        tvTitle.setId(0x01020016); // android.R.id.title
        tvTitle.setTextSize(15f);
        tvTitle.setTextColor(0xFF1A1C29);
        TextView tvSummary = new TextView(context);
        tvSummary.setId(0x01020010); // android.R.id.summary
        tvSummary.setTextSize(12f);
        tvSummary.setTextColor(0xFF6B7280);
        row.addView(tvTitle);
        row.addView(tvSummary);
        return row;
    }

    protected void onBindView(View view) {
        if (view == null) return;
        TextView tvTitle = view.findViewById(0x01020016);
        if (tvTitle != null) tvTitle.setText(getTitle());
        TextView tvSummary = view.findViewById(0x01020010);
        if (tvSummary != null) {
            CharSequence sum = getSummary();
            tvSummary.setText(sum);
            tvSummary.setVisibility(sum != null && sum.length() > 0 ? View.VISIBLE : View.GONE);
        }
    }

    protected Parcelable onSaveInstanceState() {
        return AbsSavedState.EMPTY_STATE;
    }

    protected void onRestoreInstanceState(Parcelable state) {
    }

    @Override
    public int compareTo(Preference another) {
        if (another == null) return -1;
        return Integer.compare(this.order, another.order);
    }
}
