package android.view;

import android.content.Context;

public class ViewConfiguration {
    private final float density;

    public ViewConfiguration() {
        this.density = 1.0f;
    }

    private ViewConfiguration(Context context) {
        this.density = context != null && context.getResources() != null
                ? context.getResources().getDisplayMetrics().density : 1.0f;
    }

    public static ViewConfiguration get(Context context) {
        return new ViewConfiguration(context);
    }

    public static int getScrollBarSize() {
        return 4;
    }

    public int getScaledScrollBarSize() {
        return Math.round(4 * density);
    }

    public static int getScrollBarFadeDuration() {
        return 250;
    }

    public static int getScrollDefaultDelay() {
        return 300;
    }

    public static int getPressedStateDuration() {
        return 64;
    }

    public static int getLongPressTimeout() {
        return 400;
    }

    public static int getTapTimeout() {
        return 100;
    }

    public static int getDoubleTapTimeout() {
        return 300;
    }

    public static int getTouchSlop() {
        return 8;
    }

    public int getScaledTouchSlop() {
        return Math.round(8 * density);
    }

    public int getScaledPagingTouchSlop() {
        return Math.round(16 * density);
    }

    public int getScaledDoubleTapSlop() {
        return Math.round(100 * density);
    }

    public int getScaledEdgeSlop() {
        return Math.round(12 * density);
    }

    public int getScaledMinimumFlingVelocity() {
        return Math.round(50 * density);
    }

    public int getScaledMaximumFlingVelocity() {
        return Math.round(8000 * density);
    }

    public float getScaledHorizontalScrollFactor() {
        return 64 * density;
    }

    public float getScaledVerticalScrollFactor() {
        return 64 * density;
    }

    public int getScaledOverflingDistance() {
        return Math.round(6 * density);
    }

    public int getScaledOverscrollDistance() {
        return 0;
    }

    public static float getScrollFriction() {
        return 0.015f;
    }

    public boolean hasPermanentMenuKey() {
        return false;
    }

    public boolean shouldShowMenuShortcutsWhenKeyboardPresent() {
        return true;
    }
}
