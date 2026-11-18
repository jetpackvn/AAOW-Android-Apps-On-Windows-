package android.view;

import android.graphics.Outline;

public abstract class ViewOutlineProvider {
    public static final ViewOutlineProvider BACKGROUND = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            if (view != null && outline != null) {
                outline.setRect(0, 0, view.getWidth(), view.getHeight());
            }
        }
    };

    public static final ViewOutlineProvider BOUNDS = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            if (view != null && outline != null) {
                outline.setRect(0, 0, view.getWidth(), view.getHeight());
            }
        }
    };

    public static final ViewOutlineProvider PADDED_BOUNDS = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            if (view != null && outline != null) {
                outline.setRect(view.getPaddingLeft(), view.getPaddingTop(),
                        Math.max(view.getPaddingLeft(), view.getWidth() - view.getPaddingRight()),
                        Math.max(view.getPaddingTop(), view.getHeight() - view.getPaddingBottom()));
            }
        }
    };

    public abstract void getOutline(View view, Outline outline);
}
