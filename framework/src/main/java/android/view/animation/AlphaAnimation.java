package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;

public class AlphaAnimation extends Animation {
    private float fromAlpha = 1.0f;
    private float toAlpha = 1.0f;

    public AlphaAnimation(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AlphaAnimation(float fromAlpha, float toAlpha) {
        this.fromAlpha = fromAlpha;
        this.toAlpha = toAlpha;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        t.setAlpha(fromAlpha + (toAlpha - fromAlpha) * interpolatedTime);
    }

    @Override
    public boolean willChangeTransformationMatrix() {
        return false;
    }

    @Override
    public boolean willChangeBounds() {
        return false;
    }
}
