package android.content;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class ComponentName implements Cloneable, Comparable<ComponentName>, Parcelable {
    private final String pkg;
    private final String cls;

    public ComponentName(String pkg, String cls) {
        this.pkg = pkg != null ? pkg : "";
        this.cls = cls != null ? cls : "";
    }

    public ComponentName(Context pkg, String cls) {
        this(pkg != null ? pkg.getPackageName() : "", cls);
    }

    public ComponentName(Context pkg, Class<?> cls) {
        this(pkg != null ? pkg.getPackageName() : "", cls != null ? cls.getName() : "");
    }

    public String getPackageName() {
        return pkg;
    }

    public String getClassName() {
        return cls;
    }

    public String getShortClassName() {
        if (cls.startsWith(pkg)) {
            int n = pkg.length();
            if (cls.length() > n && cls.charAt(n) == '.') {
                return cls.substring(n);
            }
        }
        return cls;
    }

    public String flattenToString() {
        return pkg + "/" + cls;
    }

    public String flattenToShortString() {
        return pkg + "/" + getShortClassName();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ComponentName c && Objects.equals(pkg, c.pkg) && Objects.equals(cls, c.cls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pkg, cls);
    }

    @Override
    public int compareTo(ComponentName o) {
        int v = pkg.compareTo(o.pkg);
        return v != 0 ? v : cls.compareTo(o.cls);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(pkg);
        out.writeString(cls);
    }

    @Override
    public String toString() {
        return "ComponentInfo{" + pkg + "/" + cls + "}";
    }
}
