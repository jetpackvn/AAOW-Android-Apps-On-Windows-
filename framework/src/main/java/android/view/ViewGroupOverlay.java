package android.view;

import android.content.Context;
import android.graphics.Canvas;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lớp phủ của ViewGroup hỗ trợ vẽ cả Drawable lẫn View tạm phía trên toàn bộ View con.
 */
public class ViewGroupOverlay extends ViewOverlay {
    private final List<View> views = new CopyOnWriteArrayList<>();

    ViewGroupOverlay(Context context, View hostView) {
        super(context, hostView);
    }

    public void add(View view) {
        if (view != null && !views.contains(view)) {
            views.add(view);
            if (hostView != null) {
                hostView.invalidate();
            }
        }
    }

    public void remove(View view) {
        if (view != null && views.remove(view)) {
            if (hostView != null) {
                hostView.invalidate();
            }
        }
    }

    @Override
    public void clear() {
        views.clear();
        super.clear();
    }

    @Override
    boolean isEmpty() {
        return views.isEmpty() && super.isEmpty();
    }

    @Override
    void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        for (View v : views) {
            if (v.getVisibility() == View.VISIBLE) {
                v.draw(canvas);
            }
        }
    }
}
