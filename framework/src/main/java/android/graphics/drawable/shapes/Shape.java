package android.graphics.drawable.shapes;

import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;

/** Lớp cơ sở cho các hình học 2D vẽ trong ShapeDrawable. */
public abstract class Shape implements Cloneable {
    private float width;
    private float height;

    public Shape() {
    }

    public final float getWidth() {
        return width;
    }

    public final float getHeight() {
        return height;
    }

    public abstract void draw(Canvas canvas, Paint paint);

    public final void resize(float width, float height) {
        float w = Math.max(0f, width);
        float h = Math.max(0f, height);
        if (this.width != w || this.height != h) {
            this.width = w;
            this.height = h;
            onResize(w, h);
        }
    }

    public boolean hasAlpha() {
        return true;
    }

    protected void onResize(float width, float height) {
    }

    public void getOutline(Outline outline) {
    }

    @Override
    public Shape clone() throws CloneNotSupportedException {
        return (Shape) super.clone();
    }
}
