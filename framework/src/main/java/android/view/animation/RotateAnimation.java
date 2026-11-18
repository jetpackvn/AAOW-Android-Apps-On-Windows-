package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;

public class RotateAnimation extends Animation {
    private float fromDegrees;
    private float toDegrees;

    public RotateAnimation(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RotateAnimation(float fromDegrees, float toDegrees) {
        this.fromDegrees = fromDegrees;
        this.toDegrees = toDegrees;
    }

    public RotateAnimation(float fromDegrees, float toDegrees, float pivotX, float pivotY) {
        this(fromDegrees, toDegrees);
    }

    public RotateAnimation(float fromDegrees, float toDegrees,
                           int pivotXType, float pivotXValue, int pivotYType, float pivotYValue) {
        this(fromDegrees, toDegrees);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        t.setRotation(fromDegrees + (toDegrees - fromDegrees) * interpolatedTime);
    }
}
