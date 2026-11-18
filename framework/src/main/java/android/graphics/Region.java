package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;

public class Region implements Parcelable {
    public enum Op {
        DIFFERENCE,
        INTERSECT,
        UNION,
        XOR,
        REVERSE_DIFFERENCE,
        REPLACE
    }

    private final Rect bounds = new Rect();

    public Region() {
    }

    public Region(Region region) {
        if (region != null) {
            this.bounds.set(region.bounds);
        }
    }

    public Region(Rect r) {
        if (r != null) {
            this.bounds.set(r);
        }
    }

    public Region(int left, int top, int right, int bottom) {
        this.bounds.set(left, top, right, bottom);
    }

    public void setEmpty() {
        bounds.setEmpty();
    }

    public boolean set(Region region) {
        if (region == null) {
            setEmpty();
            return false;
        }
        bounds.set(region.bounds);
        return !bounds.isEmpty();
    }

    public boolean set(Rect r) {
        if (r == null) {
            setEmpty();
            return false;
        }
        bounds.set(r);
        return !bounds.isEmpty();
    }

    public boolean set(int left, int top, int right, int bottom) {
        bounds.set(left, top, right, bottom);
        return !bounds.isEmpty();
    }

    public boolean isEmpty() {
        return bounds.isEmpty();
    }

    public boolean isRect() {
        return !bounds.isEmpty();
    }

    public Rect getBounds() {
        return new Rect(bounds);
    }

    public boolean getBounds(Rect r) {
        if (r != null) r.set(bounds);
        return !bounds.isEmpty();
    }

    public boolean contains(int x, int y) {
        return bounds.contains(x, y);
    }

    public void translate(int dx, int dy) {
        bounds.offset(dx, dy);
    }

    public boolean op(Rect r, Op op) {
        if (op == Op.REPLACE && r != null) {
            bounds.set(r);
        } else if (op == Op.UNION && r != null) {
            if (bounds.isEmpty()) {
                bounds.set(r);
            } else if (!r.isEmpty()) {
                bounds.left = Math.min(bounds.left, r.left);
                bounds.top = Math.min(bounds.top, r.top);
                bounds.right = Math.max(bounds.right, r.right);
                bounds.bottom = Math.max(bounds.bottom, r.bottom);
            }
        }
        return !bounds.isEmpty();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(bounds.left);
        dest.writeInt(bounds.top);
        dest.writeInt(bounds.right);
        dest.writeInt(bounds.bottom);
    }

    public static final Creator<Region> CREATOR = new Creator<>() {
        @Override
        public Region createFromParcel(Parcel source) {
            return new Region(source.readInt(), source.readInt(), source.readInt(), source.readInt());
        }

        @Override
        public Region[] newArray(int size) {
            return new Region[size];
        }
    };
}
