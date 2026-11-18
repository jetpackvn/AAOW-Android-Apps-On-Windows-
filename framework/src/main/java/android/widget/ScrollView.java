package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * ScrollView hỗ trợ cả cuộn bằng con lăn chuột Windows và kéo giữ chuột.
 */
public class ScrollView extends FrameLayout {
    private float lastTouchY;
    private boolean dragging;
    private final Paint scrollbarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ScrollView(Context context) {
        super(context);
    }

    public ScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (children.isEmpty()) return;
        View child = children.get(0);
        if (child.getVisibility() == GONE) return;
        MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        int childWidthSpec = getChildMeasureSpec(widthMeasureSpec,
                paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin, lp.width);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
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
                child.layout(cl, ct, cl + child.getMeasuredWidth(), ct + child.getMeasuredHeight());
            }
        }
        clampScroll();
    }

    private int maxScrollY() {
        if (children.isEmpty()) return 0;
        View child = children.get(0);
        int contentH = child.getMeasuredHeight() + paddingTop + paddingBottom;
        return Math.max(0, contentH - getHeight());
    }

    private void clampScroll() {
        scrollY = Math.max(0, Math.min(scrollY, maxScrollY()));
    }

    public final void smoothScrollTo(int x, int y) {
        scrollTo(x, Math.max(0, Math.min(maxScrollY(), y)));
    }

    public final void smoothScrollBy(int dx, int dy) {
        smoothScrollTo(scrollX + dx, scrollY + dy);
    }

    public boolean fullScroll(int direction) {
        if (direction == View.VISIBLE || direction == 33) {
            smoothScrollTo(0, 0);
        } else {
            smoothScrollTo(0, maxScrollY());
        }
        return true;
    }

    @Override
    public boolean onMouseWheel(int wheelRotation, float x, float y) {
        if (super.onMouseWheel(wheelRotation, x, y)) return true;
        int max = maxScrollY();
        if (max <= 0) return false;
        scrollTo(scrollX, Math.max(0, Math.min(max, scrollY + wheelRotation * 36)));
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            lastTouchY = ev.getRawY();
            dragging = false;
        } else if (action == MotionEvent.ACTION_MOVE) {
            float dy = lastTouchY - ev.getRawY();
            if (!dragging && Math.abs(dy) > 8 && maxScrollY() > 0) {
                dragging = true;
            }
            if (dragging) {
                lastTouchY = ev.getRawY();
                scrollTo(scrollX, Math.max(0, Math.min(maxScrollY(), Math.round(scrollY + dy))));
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

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        int max = maxScrollY();
        if (max > 0 && getHeight() > 0) {
            int h = getHeight();
            int contentH = h + max;
            int barH = Math.max(24, h * h / contentH);
            int barTop = (h - barH) * scrollY / max;
            scrollbarPaint.setColor(Color.argb(90, 80, 80, 95));
            canvas.drawRoundRect(getWidth() - 6, barTop + 2, getWidth() - 2, barTop + barH - 2, 2, 2, scrollbarPaint);
        }
    }
}
