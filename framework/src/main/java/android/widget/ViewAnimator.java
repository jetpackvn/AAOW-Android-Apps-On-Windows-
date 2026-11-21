package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class ViewAnimator extends FrameLayout {
    private int whichChild;
    private Animation inAnimation;
    private Animation outAnimation;
    private boolean animateFirstTime = true;

    public ViewAnimator(Context context) {
        super(context);
    }

    public ViewAnimator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setDisplayedChild(int whichChild) {
        this.whichChild = Math.max(0, Math.min(whichChild, Math.max(0, getChildCount() - 1)));
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setVisibility(i == this.whichChild ? View.VISIBLE : View.GONE);
        }
        requestLayout();
        invalidate();
    }

    public int getDisplayedChild() {
        return whichChild;
    }

    public View getCurrentView() {
        return getChildAt(whichChild);
    }

    public void showNext() {
        if (getChildCount() > 0) {
            setDisplayedChild((whichChild + 1) % getChildCount());
        }
    }

    public void showPrevious() {
        if (getChildCount() > 0) {
            setDisplayedChild((whichChild - 1 + getChildCount()) % getChildCount());
        }
    }

    public Animation getInAnimation() {
        return inAnimation;
    }

    public void setInAnimation(Animation inAnimation) {
        this.inAnimation = inAnimation;
    }

    public void setInAnimation(Context context, int resourceID) {
        try {
            if (context != null && resourceID != 0) {
                setInAnimation(AnimationUtils.loadAnimation(context, resourceID));
            }
        } catch (Throwable ignored) {
            setInAnimation((Animation) null);
        }
    }

    public Animation getOutAnimation() {
        return outAnimation;
    }

    public void setOutAnimation(Animation outAnimation) {
        this.outAnimation = outAnimation;
    }

    public void setOutAnimation(Context context, int resourceID) {
        try {
            if (context != null && resourceID != 0) {
                setOutAnimation(AnimationUtils.loadAnimation(context, resourceID));
            }
        } catch (Throwable ignored) {
            setOutAnimation((Animation) null);
        }
    }

    public boolean getAnimateFirstView() {
        return animateFirstTime;
    }

    public void setAnimateFirstView(boolean animate) {
        this.animateFirstTime = animate;
    }
}
