package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class ForegroundColorSpan extends CharacterStyle implements UpdateAppearance, ParcelableSpan {
    private final int color;

    public ForegroundColorSpan(int color) {
        this.color = color;
    }

    public ForegroundColorSpan(Parcel src) {
        this.color = src != null ? src.readInt() : 0;
    }

    public int getForegroundColor() {
        return color;
    }

    @Override
    public int getSpanTypeId() {
        return 2;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(color);
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        if (textPaint != null) textPaint.setColor(color);
    }
}
