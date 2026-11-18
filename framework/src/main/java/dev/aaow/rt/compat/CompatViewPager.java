package dev.aaow.rt.compat;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Container tương thích ViewPager / ViewPager2 quản lý các trang trượt ngang.
 */
public class CompatViewPager extends ViewGroup {
    private int currentItem = 0;
    private float downX;

    public CompatViewPager(Context context) {
        this(context, null, 0);
    }

    public CompatViewPager(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CompatViewPager(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public int getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(int item) {
        setCurrentItem(item, true);
    }

    public void setCurrentItem(int item, boolean smoothScroll) {
        if (getChildCount() == 0) {
            currentItem = 0;
            return;
        }
        currentItem = Math.max(0, Math.min(item, getChildCount() - 1));
        scrollTo(currentItem * getWidth(), 0);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED
                ? 420 : MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED
                ? 400 : MeasureSpec.getSize(heightMeasureSpec);
        for (View child : children) {
            if (child.getVisibility() == GONE) continue;
            child.measure(
                    MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l;
        int h = b - t;
        int x = 0;
        for (View child : children) {
            if (child.getVisibility() == GONE) continue;
            child.layout(x, 0, x + w, h);
            x += w;
        }
        if (currentItem > 0 && currentItem < getChildCount()) {
            scrollTo(currentItem * w, 0);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downX = ev.getX();
        } else if (ev.getActionMasked() == MotionEvent.ACTION_UP && getChildCount() > 1) {
            float dx = ev.getX() - downX;
            if (dx < -60 && currentItem + 1 < getChildCount()) {
                setCurrentItem(currentItem + 1);
                return true;
            } else if (dx > 60 && currentItem > 0) {
                setCurrentItem(currentItem - 1);
                return true;
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}
