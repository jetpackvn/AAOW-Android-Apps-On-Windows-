package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;

public class AccelerateInterpolator extends BaseInterpolator {
    private final float factor;
    private final double doubledFactor;

    public AccelerateInterpolator() {
        this(1.0f);
    }

    public AccelerateInterpolator(float factor) {
        this.factor = factor;
        this.doubledFactor = factor * 2.0;
    }

    public AccelerateInterpolator(Context context, AttributeSet attrs) {
        this(1.0f);
    }

    @Override
    public float getInterpolation(float input) {
        if (factor == 1.0f) {
            return input * input;
        }
        return (float) Math.pow(input, doubledFactor);
    }
}
