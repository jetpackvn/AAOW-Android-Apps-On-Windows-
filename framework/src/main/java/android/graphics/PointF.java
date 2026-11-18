package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;

public class PointF implements Parcelable {
    public float x;
    public float y;

    public PointF() {
    }

    public PointF(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public PointF(PointF p) {
        if (p != null) {
            this.x = p.x;
            this.y = p.y;
        }
    }

    public final void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public final void set(PointF p) {
        if (p != null) {
            this.x = p.x;
            this.y = p.y;
        }
    }

    public final void negate() {
        this.x = -this.x;
        this.y = -this.y;
    }

    public final void offset(float dx, float dy) {
        this.x += dx;
        this.y += dy;
    }

    public final float length() {
        return (float) Math.hypot(x, y);
    }

    public static float length(float x, float y) {
        return (float) Math.hypot(x, y);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeFloat(x);
        out.writeFloat(y);
    }

    public void readFromParcel(Parcel in) {
        this.x = in.readFloat();
        this.y = in.readFloat();
    }

    public static final Creator<PointF> CREATOR = new Creator<>() {
        @Override
        public PointF createFromParcel(Parcel in) {
            PointF r = new PointF();
            r.readFromParcel(in);
            return r;
        }

        @Override
        public PointF[] newArray(int size) {
            return new PointF[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        return o instanceof PointF p && Float.compare(p.x, x) == 0 && Float.compare(p.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(x) * 31 + Float.hashCode(y);
    }

    @Override
    public String toString() {
        return "PointF(" + x + ", " + y + ")";
    }
}
