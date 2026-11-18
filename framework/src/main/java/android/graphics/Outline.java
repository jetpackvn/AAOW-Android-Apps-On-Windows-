package android.graphics;

public final class Outline {
    private final Rect rect = new Rect();
    private float radius;
    private float alpha = 1.0f;
    private boolean empty = true;

    public Outline() {
    }

    public Outline(Outline src) {
        set(src);
    }

    public void setEmpty() {
        empty = true;
        rect.set(0, 0, 0, 0);
        radius = 0f;
    }

    public boolean isEmpty() {
        return empty;
    }

    public boolean canClip() {
        return !empty;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public float getAlpha() {
        return alpha;
    }

    public void set(Outline src) {
        if (src == null) return;
        this.rect.set(src.rect);
        this.radius = src.radius;
        this.alpha = src.alpha;
        this.empty = src.empty;
    }

    public void setRect(int left, int top, int right, int bottom) {
        setRoundRect(left, top, right, bottom, 0f);
    }

    public void setRect(Rect rect) {
        if (rect != null) setRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    public void setRoundRect(int left, int top, int right, int bottom, float radius) {
        this.rect.set(left, top, right, bottom);
        this.radius = radius;
        this.empty = left >= right || top >= bottom;
    }

    public void setRoundRect(Rect rect, float radius) {
        if (rect != null) setRoundRect(rect.left, rect.top, rect.right, rect.bottom, radius);
    }

    public boolean getRect(Rect outRect) {
        if (empty || outRect == null) return false;
        outRect.set(rect);
        return true;
    }

    public float getRadius() {
        return radius;
    }

    public void setOval(int left, int top, int right, int bottom) {
        setRoundRect(left, top, right, bottom, Math.min(right - left, bottom - top) / 2f);
    }

    public void setOval(Rect rect) {
        if (rect != null) setOval(rect.left, rect.top, rect.right, rect.bottom);
    }

    public void setConvexPath(Path convexPath) {
        empty = false;
    }

    public void setPath(Path path) {
        empty = false;
    }

    public void offset(int dx, int dy) {
        rect.offset(dx, dy);
    }
}
