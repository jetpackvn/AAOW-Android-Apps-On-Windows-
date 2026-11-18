package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

public class AnimationSet extends Animation {
    private final ArrayList<Animation> animations = new ArrayList<>();

    public AnimationSet(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimationSet(boolean shareInterpolator) {
    }

    public void addAnimation(Animation a) {
        if (a != null) animations.add(a);
    }

    public List<Animation> getAnimations() {
        return animations;
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        for (Animation a : animations) {
            a.initialize(width, height, parentWidth, parentHeight);
        }
    }

    @Override
    public boolean getTransformation(long currentTime, Transformation t) {
        boolean more = false;
        Transformation temp = new Transformation();
        if (t != null) t.clear();
        for (Animation a : animations) {
            temp.clear();
            if (a.getTransformation(currentTime, temp)) {
                more = true;
            }
            if (t != null) t.compose(temp);
        }
        return more;
    }
}
