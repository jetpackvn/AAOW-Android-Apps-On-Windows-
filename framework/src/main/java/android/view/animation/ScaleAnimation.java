package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;

public class ScaleAnimation extends Animation {
    private float fromX = 1.0f;
    private float toX = 1.0f;
    private float fromY = 1.0f;
    private float toY = 1.0f;

    public ScaleAnimation(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScaleAnimation(float fromX, float toX, float fromY, float toY) {
        this.fromX = fromX;
        this.toX = toX;
        this.fromY = fromY;
        this.toY = toY;
    }

    public ScaleAnimation(float fromX, float toX, float fromY, float toY, float pivotX, float pivotY) {
        this(fromX, toX, fromY, toY);
    }

    public ScaleAnimation(float fromX, float toX, float fromY, float toY,
                          int pivotXType, float pivotXValue, int pivotYType, float pivotYValue) {
        this(fromX, toX, fromY, toY);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        float sx = fromX + (toX - fromX) * interpolatedTime;
        float sy = fromY + (toY - fromY) * interpolatedTime;
        t.setScale(sx, sy);
    }
}
