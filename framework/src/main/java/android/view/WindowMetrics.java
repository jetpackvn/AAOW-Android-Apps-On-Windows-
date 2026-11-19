package android.view;

import android.graphics.Insets;
import android.graphics.Rect;

/** Kích thước và WindowInsets của cửa sổ hiện tại (API 30+, dùng bởi Material NavigationView / WindowUtils). */
public final class WindowMetrics {
    private final Rect bounds;
    private final WindowInsets windowInsets;
    private final float density;

    public WindowMetrics(Rect bounds, WindowInsets windowInsets) {
        this(bounds, windowInsets, 1.5f);
    }

    public WindowMetrics(Rect bounds, WindowInsets windowInsets, float density) {
        this.bounds = bounds != null ? new Rect(bounds) : new Rect(0, 0, 420, 820);
        this.windowInsets = windowInsets != null ? windowInsets : new WindowInsets(Insets.NONE, false);
        this.density = density > 0f ? density : 1.5f;
    }

    public Rect getBounds() {
        return new Rect(bounds);
    }

    public WindowInsets getWindowInsets() {
        return windowInsets;
    }

    public float getDensity() {
        return density;
    }

    @Override
    public String toString() {
        return "WindowMetrics{bounds=" + bounds + ", density=" + density + "}";
    }
}
