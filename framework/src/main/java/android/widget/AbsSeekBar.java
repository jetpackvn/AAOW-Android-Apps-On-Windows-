package android.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public abstract class AbsSeekBar extends ProgressBar {
    private Drawable thumb;
    private int keyProgressIncrement = 1;
    private boolean splitTrack;

    public AbsSeekBar(Context context) {
        super(context);
    }

    public AbsSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AbsSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AbsSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
    }

    public void setThumb(Drawable thumb) {
        this.thumb = thumb;
        invalidate();
    }

    public Drawable getThumb() {
        return thumb;
    }

    public void setKeyProgressIncrement(int increment) {
        this.keyProgressIncrement = Math.abs(increment);
    }

    public int getKeyProgressIncrement() {
        return keyProgressIncrement;
    }

    public void setSplitTrack(boolean splitTrack) {
        this.splitTrack = splitTrack;
        invalidate();
    }

    public boolean getSplitTrack() {
        return splitTrack;
    }
}
