package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;

public class TranslateAnimation extends Animation {
    private final int fromXType;
    private final int toXType;
    private final int fromYType;
    private final int toYType;
    private final float fromXValue;
    private final float toXValue;
    private final float fromYValue;
    private final float toYValue;
    private float fromXDelta;
    private float toXDelta;
    private float fromYDelta;
    private float toYDelta;

    public TranslateAnimation(Context context, AttributeSet attrs) {
        super(context, attrs);
        fromXType = toXType = fromYType = toYType = ABSOLUTE;
        fromXValue = toXValue = fromYValue = toYValue = 0f;
    }

    public TranslateAnimation(float fromXDelta, float toXDelta, float fromYDelta, float toYDelta) {
        this(ABSOLUTE, fromXDelta, ABSOLUTE, toXDelta, ABSOLUTE, fromYDelta, ABSOLUTE, toYDelta);
    }

    public TranslateAnimation(int fromXType, float fromXValue, int toXType, float toXValue,
                              int fromYType, float fromYValue, int toYType, float toYValue) {
        this.fromXType = fromXType;
        this.fromXValue = fromXValue;
        this.toXType = toXType;
        this.toXValue = toXValue;
        this.fromYType = fromYType;
        this.fromYValue = fromYValue;
        this.toYType = toYType;
        this.toYValue = toYValue;
        this.fromXDelta = fromXValue;
        this.toXDelta = toXValue;
        this.fromYDelta = fromYValue;
        this.toYDelta = toYValue;
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        fromXDelta = resolveSize(fromXType, fromXValue, width, parentWidth);
        toXDelta = resolveSize(toXType, toXValue, width, parentWidth);
        fromYDelta = resolveSize(fromYType, fromYValue, height, parentHeight);
        toYDelta = resolveSize(toYType, toYValue, height, parentHeight);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        float dx = fromXDelta + (toXDelta - fromXDelta) * interpolatedTime;
        float dy = fromYDelta + (toYDelta - fromYDelta) * interpolatedTime;
        t.setTranslation(dx, dy);
    }
}
