package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * HorizontalScrollView hỗ trợ cuộn ngang nội dung rộng hơn khung nhìn.
 */
public class HorizontalScrollView extends FrameLayout {
    private float lastTouchX;
    private boolean dragging;

    public HorizontalScrollView(Context context) {
        super(context);
    }

    public HorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (children.isEmpty()) return;
        View child = children.get(0);
        if (child.getVisibility() == GONE) return;
        MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        int childWidthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int childHeightSpec = getChildMeasureSpec(heightMeasureSpec,
                paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin, lp.height);
        child.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (!children.isEmpty()) {
            View child = children.get(0);
            if (child.getVisibility() != GONE) {
                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                int cl = paddingLeft + lp.leftMargin;
                int ct = paddingTop + lp.topMargin;
                int cw = Math.max(child.getMeasuredWidth(), getWidth() - paddingLeft - paddingRight);
                int ch = Math.max(child.getMeasuredHeight(), getHeight() - paddingTop - paddingBottom);
                child.layout(cl, ct, cl + cw, ct + ch);
            }
        }
        clampScroll();
    }

    private int maxScrollX() {
        if (children.isEmpty()) return 0;
        View child = children.get(0);
        return Math.max(0, child.getMeasuredWidth() + paddingLeft + paddingRight - getWidth());
    }

    private void clampScroll() {
        scrollX = Math.max(0, Math.min(scrollX, maxScrollX()));
    }

    public final void smoothScrollTo(int x, int y) {
        scrollTo(Math.max(0, Math.min(maxScrollX(), x)), scrollY);
    }

    public boolean fullScroll(int direction) {
        if (direction == View.FOCUS_LEFT) {
            scrollTo(0, scrollY);
            return true;
        }
        if (direction == View.FOCUS_RIGHT) {
            scrollTo(maxScrollX(), scrollY);
            return true;
        }
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            lastTouchX = ev.getRawX();
            dragging = false;
        } else if (action == MotionEvent.ACTION_MOVE) {
            float dx = lastTouchX - ev.getRawX();
            if (!dragging && Math.abs(dx) > 8 && maxScrollX() > 0) {
                dragging = true;
            }
            if (dragging) {
                lastTouchX = ev.getRawX();
                scrollTo(Math.max(0, Math.min(maxScrollX(), Math.round(scrollX + dx))), scrollY);
                return true;
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (dragging) {
                dragging = false;
                return true;
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}
