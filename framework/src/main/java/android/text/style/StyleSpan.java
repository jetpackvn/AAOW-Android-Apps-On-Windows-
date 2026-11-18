package android.text.style;

import android.graphics.Typeface;
import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class StyleSpan extends MetricAffectingSpan implements ParcelableSpan {
    private final int style;

    public StyleSpan(int style) {
        this.style = style;
    }

    public StyleSpan(Parcel src) {
        this.style = src != null ? src.readInt() : 0;
    }

    public int getStyle() {
        return style;
    }

    @Override
    public int getSpanTypeId() {
        return 7;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(style);
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        apply(ds);
    }

    @Override
    public void updateMeasureState(TextPaint paint) {
        apply(paint);
    }

    private void apply(TextPaint paint) {
        if (paint == null) return;
        Typeface old = paint.getTypeface();
        int oldStyle = old != null ? old.getStyle() : 0;
        paint.setTypeface(Typeface.create(old, oldStyle | style));
    }
}
