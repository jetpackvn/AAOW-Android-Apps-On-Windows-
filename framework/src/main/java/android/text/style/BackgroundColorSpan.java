package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;

public class BackgroundColorSpan extends CharacterStyle implements UpdateAppearance, ParcelableSpan {
    private final int color;

    public BackgroundColorSpan(int color) {
        this.color = color;
    }

    public BackgroundColorSpan(Parcel src) {
        this.color = src != null ? src.readInt() : 0;
    }

    public int getBackgroundColor() {
        return color;
    }

    @Override
    public int getSpanTypeId() {
        return 12;
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
    }
}
