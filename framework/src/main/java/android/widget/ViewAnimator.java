package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class ViewAnimator extends FrameLayout {
    private int whichChild;

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
}
