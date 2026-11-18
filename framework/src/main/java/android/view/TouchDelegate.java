package android.view;

import android.graphics.Rect;

public class TouchDelegate {
    public static final int ABOVE = 1;
    public static final int BELOW = 2;
    public static final int TO_LEFT = 4;
    public static final int TO_RIGHT = 8;

    private final Rect bounds;
    private final View delegateView;

    public TouchDelegate(Rect bounds, View delegateView) {
        this.bounds = bounds != null ? new Rect(bounds) : new Rect();
        this.delegateView = delegateView;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (event == null || delegateView == null) return false;
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (bounds.contains(x, y)) {
            return delegateView.dispatchTouchEvent(event);
        }
        return false;
    }

    public boolean onTouchExplorationHoverEvent(MotionEvent event) {
        return false;
    }
}
