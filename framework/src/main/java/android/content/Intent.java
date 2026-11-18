package android.content;

import android.net.Uri;
import android.os.Bundle;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Intent implements Cloneable {
    public static final String ACTION_MAIN = "android.intent.action.MAIN";
    public static final String ACTION_VIEW = "android.intent.action.VIEW";
    public static final String ACTION_SEND = "android.intent.action.SEND";
    public static final String CATEGORY_DEFAULT = "android.intent.category.DEFAULT";
    public static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";

    public static final String EXTRA_TEXT = "android.intent.extra.TEXT";
    public static final String EXTRA_TITLE = "android.intent.extra.TITLE";

    public static final int FLAG_ACTIVITY_NEW_TASK = 0x10000000;
    public static final int FLAG_ACTIVITY_CLEAR_TOP = 0x04000000;
    public static final int FLAG_ACTIVITY_SINGLE_TOP = 0x20000000;

    private String action;
    private Uri data;
    private String type;
    private ComponentName component;
    private int flags;
    private final Set<String> categories = new HashSet<>();
    private final Bundle extras = new Bundle();

    public Intent() {
    }

    public Intent(Intent o) {
        if (o != null) {
            this.action = o.action;
            this.data = o.data;
            this.type = o.type;
            this.component = o.component;
            this.flags = o.flags;
            this.categories.addAll(o.categories);
            this.extras.putAll(o.extras);
        }
    }

    public Intent(String action) {
        this.action = action;
    }

    public Intent(String action, Uri uri) {
        this.action = action;
        this.data = uri;
    }

    public Intent(Context packageContext, Class<?> cls) {
        this.component = new ComponentName(packageContext, cls);
    }

    public String getAction() {
        return action;
    }

    public Intent setAction(String action) {
        this.action = action;
        return this;
    }

    public Uri getData() {
        return data;
    }

    public String getDataString() {
        return data != null ? data.toString() : null;
    }

    public Intent setData(Uri data) {
        this.data = data;
        return this;
    }

    public String getType() {
        return type;
    }

    public Intent setType(String type) {
        this.type = type;
        return this;
    }

    public ComponentName getComponent() {
        return component;
    }

    public Intent setComponent(ComponentName component) {
        this.component = component;
        return this;
    }

    public Intent setClass(Context packageContext, Class<?> cls) {
        this.component = new ComponentName(packageContext, cls);
        return this;
    }

    public Intent setClassName(Context packageContext, String className) {
        this.component = new ComponentName(packageContext, className);
        return this;
    }

    public Intent setClassName(String packageName, String className) {
        this.component = new ComponentName(packageName, className);
        return this;
    }

    public Intent addCategory(String category) {
        categories.add(category);
        return this;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public int getFlags() {
        return flags;
    }

    public Intent setFlags(int flags) {
        this.flags = flags;
        return this;
    }

    public Intent addFlags(int flags) {
        this.flags |= flags;
        return this;
    }

    public Intent putExtra(String name, boolean value) {
        extras.putBoolean(name, value);
        return this;
    }

    public Intent putExtra(String name, int value) {
        extras.putInt(name, value);
        return this;
    }

    public Intent putExtra(String name, long value) {
        extras.putLong(name, value);
        return this;
    }

    public Intent putExtra(String name, float value) {
        extras.putFloat(name, value);
        return this;
    }

    public Intent putExtra(String name, double value) {
        extras.putDouble(name, value);
        return this;
    }

    public Intent putExtra(String name, String value) {
        extras.putString(name, value);
        return this;
    }

    public Intent putExtra(String name, CharSequence value) {
        extras.putCharSequence(name, value);
        return this;
    }

    public Intent putExtra(String name, Bundle value) {
        extras.putBundle(name, value);
        return this;
    }

    public Intent putExtra(String name, Serializable value) {
        extras.putSerializable(name, value);
        return this;
    }

    public Intent putExtras(Bundle extras) {
        this.extras.putAll(extras);
        return this;
    }

    public boolean hasExtra(String name) {
        return extras.containsKey(name);
    }

    public boolean getBooleanExtra(String name, boolean defaultValue) {
        return extras.getBoolean(name, defaultValue);
    }

    public int getIntExtra(String name, int defaultValue) {
        return extras.getInt(name, defaultValue);
    }

    public long getLongExtra(String name, long defaultValue) {
        return extras.getLong(name, defaultValue);
    }

    public float getFloatExtra(String name, float defaultValue) {
        return extras.getFloat(name, defaultValue);
    }

    public double getDoubleExtra(String name, double defaultValue) {
        return extras.getDouble(name, defaultValue);
    }

    public String getStringExtra(String name) {
        return extras.getString(name);
    }

    public CharSequence getCharSequenceExtra(String name) {
        return extras.getCharSequence(name);
    }

    public Bundle getBundleExtra(String name) {
        return extras.getBundle(name);
    }

    public Serializable getSerializableExtra(String name) {
        return extras.getSerializable(name);
    }

    public Bundle getExtras() {
        return new Bundle(extras);
    }

    @Override
    public String toString() {
        return "Intent{action=" + action + ", cmp=" + component + ", data=" + data + "}";
    }
}
