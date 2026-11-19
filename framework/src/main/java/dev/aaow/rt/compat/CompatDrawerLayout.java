package dev.aaow.rt.compat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Container tương thích cơ bản cho DrawerLayout khi APK không nhúng hoặc gặp lỗi khởi tạo DrawerLayout.
 * Child đầu tiên là nội dung chính, child thứ 2 trở đi là ngăn kéo (drawer) mở từ cạnh trái/phải.
 */
public class CompatDrawerLayout extends FrameLayout {
    public interface DrawerListener {
        default void onDrawerSlide(View drawerView, float slideOffset) {}
        default void onDrawerOpened(View drawerView) {}
        default void onDrawerClosed(View drawerView) {}
        default void onDrawerStateChanged(int newState) {}
    }

    private boolean drawerOpen = false;
    private final List<DrawerListener> listeners = new ArrayList<>();
    private final Paint scrimPaint = new Paint();

    public CompatDrawerLayout(Context context) {
        this(context, null, 0);
    }

    public CompatDrawerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CompatDrawerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        scrimPaint.setColor(0x99000000);
    }

    public void addDrawerListener(DrawerListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeDrawerListener(DrawerListener listener) {
        listeners.remove(listener);
    }

    public boolean isDrawerOpen(int drawerGravity) {
        return drawerOpen;
    }

    public boolean isDrawerOpen(View drawer) {
        return drawerOpen && drawer != null && indexOfChild(drawer) > 0;
    }

    public void openDrawer(int gravity) {
        openDrawer(getDrawerChild());
    }

    public void openDrawer(View drawer) {
        if (drawer == null) return;
        if (!drawerOpen) {
            drawerOpen = true;
            requestLayout();
            invalidate();
            for (DrawerListener l : List.copyOf(listeners)) {
                l.onDrawerSlide(drawer, 1f);
                l.onDrawerOpened(drawer);
            }
        }
    }

    public void closeDrawer(int gravity) {
        closeDrawers();
    }

    public void closeDrawer(View drawer) {
        closeDrawers();
    }

    public void closeDrawers() {
        if (drawerOpen) {
            drawerOpen = false;
            View drawer = getDrawerChild();
            requestLayout();
            invalidate();
            if (drawer != null) {
                for (DrawerListener l : List.copyOf(listeners)) {
                    l.onDrawerSlide(drawer, 0f);
                    l.onDrawerClosed(drawer);
                }
            }
        }
    }

    private View getDrawerChild() {
        return getChildCount() > 1 ? getChildAt(1) : null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED
                ? 420 : MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED
                ? 700 : MeasureSpec.getSize(heightMeasureSpec);

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            if (i == 0) {
                child.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
            } else {
                int maxDrawerW = Math.min(280, Math.max(200, w - 64));
                ViewGroup.LayoutParams lp = child.getLayoutParams();
                int dw = (lp != null && lp.width > 0) ? Math.min(lp.width, w - 48) : maxDrawerW;
                child.measure(MeasureSpec.makeMeasureSpec(dw, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
            }
        }
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l;
        int h = b - t;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            if (i == 0) {
                child.layout(0, 0, w, h);
            } else {
                int dw = child.getMeasuredWidth();
                if (drawerOpen) {
                    boolean rightDrawer = child.getLayoutParams() instanceof FrameLayout.LayoutParams flp
                            && (flp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.RIGHT;
                    if (rightDrawer) {
                        child.layout(w - dw, 0, w, h);
                    } else {
                        child.layout(0, 0, dw, h);
                    }
                } else {
                    child.layout(-dw, 0, 0, h);
                }
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        long drawingTime = android.os.SystemClock.uptimeMillis();
        if (getChildCount() > 0) {
            drawChild(canvas, getChildAt(0), drawingTime);
        }
        if (drawerOpen) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), scrimPaint);
            for (int i = 1; i < getChildCount(); i++) {
                drawChild(canvas, getChildAt(i), drawingTime);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (drawerOpen) {
            View drawer = getDrawerChild();
            if (drawer != null) {
                float x = ev.getX();
                float y = ev.getY();
                boolean insideDrawer = x >= drawer.getLeft() && x < drawer.getRight()
                        && y >= drawer.getTop() && y < drawer.getBottom();
                if (insideDrawer) {
                    MotionEvent cev = new MotionEvent(ev.getActionMasked(),
                            x - drawer.getLeft(), y - drawer.getTop(),
                            ev.getRawX(), ev.getRawY(), ev.getDownTime(), ev.getEventTime());
                    return drawer.dispatchTouchEvent(cev);
                }
                if (ev.getActionMasked() == MotionEvent.ACTION_UP) {
                    closeDrawers();
                }
                return true;
            }
        }
        if (getChildCount() > 0) {
            return getChildAt(0).dispatchTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }
}
