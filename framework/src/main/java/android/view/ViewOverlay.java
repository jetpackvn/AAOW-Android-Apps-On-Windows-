package android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lớp phủ hiển thị Drawable phía trên nội dung của một View (dùng bởi MaterialContainerTransform / AndroidX Transition).
 */
public class ViewOverlay {
    protected final Context context;
    protected final View hostView;
    protected final List<Drawable> drawables = new CopyOnWriteArrayList<>();

    ViewOverlay(Context context, View hostView) {
        this.context = context;
        this.hostView = hostView;
    }

    public void add(Drawable drawable) {
        if (drawable != null && !drawables.contains(drawable)) {
            drawables.add(drawable);
            if (hostView != null) {
                drawable.setCallback(hostView);
                hostView.invalidate();
            }
        }
    }

    public void remove(Drawable drawable) {
        if (drawable != null && drawables.remove(drawable)) {
            if (hostView != null) {
                hostView.invalidate();
            }
        }
    }

    public void clear() {
        drawables.clear();
        if (hostView != null) {
            hostView.invalidate();
        }
    }

    boolean isEmpty() {
        return drawables.isEmpty();
    }

    void dispatchDraw(Canvas canvas) {
        for (Drawable d : drawables) {
            d.draw(canvas);
        }
    }
}
