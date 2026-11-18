package android.content;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Intent implements Cloneable, Parcelable {
    public static final String ACTION_MAIN = "android.intent.action.MAIN";
    public static final String ACTION_VIEW = "android.intent.action.VIEW";
    public static final String ACTION_SEND = "android.intent.action.SEND";
    public static final String ACTION_CHOOSER = "android.intent.action.CHOOSER";
    public static final String ACTION_GET_CONTENT = "android.intent.action.GET_CONTENT";
    public static final String ACTION_OPEN_DOCUMENT = "android.intent.action.OPEN_DOCUMENT";
    public static final String ACTION_CREATE_DOCUMENT = "android.intent.action.CREATE_DOCUMENT";
    public static final String ACTION_OPEN_DOCUMENT_TREE = "android.intent.action.OPEN_DOCUMENT_TREE";
    public static final String CATEGORY_DEFAULT = "android.intent.category.DEFAULT";
    public static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
    public static final String CATEGORY_OPENABLE = "android.intent.category.OPENABLE";

    public static final String EXTRA_TEXT = "android.intent.extra.TEXT";
    public static final String EXTRA_TITLE = "android.intent.extra.TITLE";
    public static final String EXTRA_STREAM = "android.intent.extra.STREAM";
    public static final String EXTRA_INTENT = "android.intent.extra.INTENT";
    public static final String EXTRA_ALLOW_MULTIPLE = "android.intent.extra.ALLOW_MULTIPLE";
    public static final String EXTRA_MIME_TYPES = "android.intent.extra.MIME_TYPES";

    public static final int URI_INTENT_SCHEME = 1 << 0;

    public static final int FLAG_ACTIVITY_NEW_TASK = 0x10000000;
    public static final int FLAG_ACTIVITY_CLEAR_TOP = 0x04000000;
    public static final int FLAG_ACTIVITY_SINGLE_TOP = 0x20000000;
    public static final int FLAG_ACTIVITY_CLEAR_TASK = 0x00008000;
    public static final int FLAG_GRANT_READ_URI_PERMISSION = 0x00000001;
    public static final int FLAG_GRANT_WRITE_URI_PERMISSION = 0x00000002;

    private String action;
    private Uri data;
    private String type;
    private String packageName;
    private ComponentName component;
    private int flags;
    private ClipData clipData;
    private final Set<String> categories = new HashSet<>();
    private final Bundle extras = new Bundle();

    public Intent() {
    }

    public Intent(Intent o) {
        if (o != null) {
            this.action = o.action;
            this.data = o.data;
            this.type = o.type;
            this.packageName = o.packageName;
            this.component = o.component;
            this.flags = o.flags;
            this.clipData = o.clipData != null ? new ClipData(o.clipData) : null;
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

    public Intent(String action, Uri uri, Context packageContext, Class<?> cls) {
        this.action = action;
        this.data = uri;
        this.component = new ComponentName(packageContext, cls);
    }

    public static Intent createChooser(Intent target, CharSequence title) {
        Intent intent = new Intent(ACTION_CHOOSER);
        intent.putExtra(EXTRA_INTENT, (Parcelable) target);
        if (title != null) intent.putExtra(EXTRA_TITLE, title);
        return intent;
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

    public Intent setDataAndType(Uri data, String type) {
        this.data = data;
        this.type = type;
        return this;
    }

    public String getType() {
        return type;
    }

    public Intent setType(String type) {
        this.type = type;
        return this;
    }

    public String getPackage() {
        return packageName;
    }

    public Intent setPackage(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public ComponentName getComponent() {
        return component;
    }

    public Intent setComponent(ComponentName component) {
        this.component = component;
        return this;
    }

    public ComponentName resolveActivity(PackageManager pm) {
        return component;
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

    public boolean hasCategory(String category) {
        return categories.contains(category);
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

    public Intent putExtra(String name, Parcelable value) {
        extras.putParcelable(name, value);
        return this;
    }

    public Intent putExtra(String name, Serializable value) {
        extras.putSerializable(name, value);
        return this;
    }

    public Intent putExtra(String name, String[] value) {
        extras.putStringArray(name, value);
        return this;
    }

    public Intent putExtra(String name, int[] value) {
        extras.putIntArray(name, value);
        return this;
    }

    public Intent putExtra(String name, byte[] value) {
        extras.putByteArray(name, value);
        return this;
    }

    public ClipData getClipData() {
        return clipData;
    }

    public void setClipData(ClipData clip) {
        this.clipData = clip;
    }

    @Deprecated
    public String toUri() {
        return toUri(0);
    }

    public String toUri(int flags) {
        StringBuilder sb = new StringBuilder();
        if (data != null) sb.append(data.toString());
        else sb.append("intent:");
        sb.append("#Intent;");
        if (action != null) sb.append("action=").append(action).append(';');
        if (packageName != null) sb.append("package=").append(packageName).append(';');
        if (component != null) sb.append("component=").append(component.flattenToShortString()).append(';');
        sb.append("end");
        return sb.toString();
    }

    public Intent putParcelableArrayListExtra(String name, ArrayList<? extends Parcelable> value) {
        extras.putParcelableArrayList(name, value);
        return this;
    }

    public Intent putStringArrayListExtra(String name, ArrayList<String> value) {
        extras.putStringArrayList(name, value);
        return this;
    }

    public Intent putExtras(Bundle extras) {
        this.extras.putAll(extras);
        return this;
    }

    public Intent putExtras(Intent src) {
        if (src != null) this.extras.putAll(src.extras);
        return this;
    }

    public boolean hasExtra(String name) {
        return extras.containsKey(name);
    }

    public void removeExtra(String name) {
        extras.remove(name);
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

    public <T extends Parcelable> T getParcelableExtra(String name) {
        return extras.getParcelable(name);
    }

    public <T extends Parcelable> ArrayList<T> getParcelableArrayListExtra(String name) {
        return extras.getParcelableArrayList(name);
    }

    public ArrayList<String> getStringArrayListExtra(String name) {
        return extras.getStringArrayList(name);
    }

    public Serializable getSerializableExtra(String name) {
        return extras.getSerializable(name);
    }

    public String[] getStringArrayExtra(String name) {
        return extras.getStringArray(name);
    }

    public int[] getIntArrayExtra(String name) {
        return extras.getIntArray(name);
    }

    public byte[] getByteArrayExtra(String name) {
        return extras.getByteArray(name);
    }

    public Bundle getExtras() {
        return new Bundle(extras);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(action);
        out.writeParcelable(data, flags);
        out.writeString(type);
        out.writeString(packageName);
        out.writeParcelable(component, flags);
        out.writeInt(this.flags);
        out.writeBundle(extras);
    }

    public static final Creator<Intent> CREATOR = new Creator<>() {
        @Override
        public Intent createFromParcel(Parcel in) {
            Intent i = new Intent();
            i.action = in.readString();
            i.data = in.readParcelable(null);
            i.type = in.readString();
            i.packageName = in.readString();
            i.component = in.readParcelable(null);
            i.flags = in.readInt();
            Bundle b = in.readBundle();
            if (b != null) i.extras.putAll(b);
            return i;
        }

        @Override
        public Intent[] newArray(int size) {
            return new Intent[size];
        }
    };

    @Override
    public Object clone() {
        return new Intent(this);
    }

    public static Intent parseUri(String uri, int flags) throws URISyntaxException {
        if (uri == null) return new Intent();
        Intent intent = new Intent(ACTION_VIEW);
        if (uri.startsWith("intent:")) {
            int hashIdx = uri.indexOf("#Intent;");
            if (hashIdx >= 0) {
                String schemePart = uri.substring("intent:".length(), hashIdx);
                if (!schemePart.isEmpty()) {
                    intent.setData(Uri.parse(schemePart));
                }
                String meta = uri.substring(hashIdx + "#Intent;".length());
                String pkg = null;
                String cls = null;
                for (String token : meta.split(";")) {
                    if ("end".equals(token)) break;
                    int eq = token.indexOf('=');
                    if (eq <= 0) continue;
                    String k = token.substring(0, eq);
                    String v = token.substring(eq + 1);
                    switch (k) {
                        case "action" -> intent.setAction(v);
                        case "category" -> intent.addCategory(v);
                        case "package" -> {
                            pkg = v;
                            intent.setPackage(v);
                        }
                        case "component" -> {
                            int slash = v.indexOf('/');
                            if (slash > 0) {
                                pkg = v.substring(0, slash);
                                cls = v.substring(slash + 1);
                                if (cls.startsWith(".")) cls = pkg + cls;
                                intent.setClassName(pkg, cls);
                            }
                        }
                        case "scheme" -> {
                            if (intent.getData() == null) {
                                intent.setData(Uri.parse(v + ":" + schemePart));
                            }
                        }
                        case "type" -> intent.setType(v);
                        default -> {
                            if (k.startsWith("S.")) intent.putExtra(k.substring(2), v);
                        }
                    }
                }
                return intent;
            }
        }
        intent.setData(Uri.parse(uri));
        return intent;
    }

    @Override
    public String toString() {
        return "Intent{action=" + action + ", cmp=" + component + ", data=" + data + "}";
    }
}
