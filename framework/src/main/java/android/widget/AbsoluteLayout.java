package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class AbsoluteLayout extends ViewGroup {
    public static class LayoutParams extends ViewGroup.LayoutParams {
        public int x;
        public int y;

        public LayoutParams(int width, int height, int x, int y) {
            super(width, height);
            this.x = x;
            this.y = y;
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    public AbsoluteLayout(Context context) {
        super(context);
    }

    public AbsoluteLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AbsoluteLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (View child : children) {
            if (child.getVisibility() == GONE) continue;
            int cx = paddingLeft;
            int cy = paddingTop;
            if (child.getLayoutParams() instanceof LayoutParams lp) {
                cx += lp.x;
                cy += lp.y;
            }
            child.layout(cx, cy, cx + child.getMeasuredWidth(), cy + child.getMeasuredHeight());
        }
    }
}
