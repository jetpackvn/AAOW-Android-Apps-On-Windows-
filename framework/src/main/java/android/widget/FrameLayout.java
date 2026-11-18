package android.widget;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import dev.aaow.apk.XmlElement;

public class FrameLayout extends ViewGroup {
    public static class LayoutParams extends MarginLayoutParams {
        public int gravity = -1;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            if (attrs instanceof AttributeSet.XmlAttributeSet xas && c != null) {
                XmlElement.Attribute g = xas.element.attr("layout_gravity");
                if (g != null) {
                    this.gravity = c.getResources().getTable().resolve(g.value).data();
                }
            }
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }
    }

    public FrameLayout(Context context) {
        super(context);
    }

    public FrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (p instanceof LayoutParams lp) return new LayoutParams(lp);
        if (p instanceof MarginLayoutParams mlp) return new LayoutParams(mlp);
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxW = 0;
        int maxH = 0;
        for (View child : children) {
            if (child.getVisibility() == GONE) continue;
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            maxW = Math.max(maxW, child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
            maxH = Math.max(maxH, child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
        }
        maxW += paddingLeft + paddingRight;
        maxH += paddingTop + paddingBottom;
        maxW = Math.max(maxW, getSuggestedMinimumWidth());
        maxH = Math.max(maxH, getSuggestedMinimumHeight());
        setMeasuredDimension(resolveSize(maxW, widthMeasureSpec), resolveSize(maxH, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int parentLeft = paddingLeft;
        int parentRight = (right - left) - paddingRight;
        int parentTop = paddingTop;
        int parentBottom = (bottom - top) - paddingBottom;

        for (View child : children) {
            if (child.getVisibility() == GONE) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int w = child.getMeasuredWidth();
            int h = child.getMeasuredHeight();

            int g = lp.gravity;
            if (g == -1) g = Gravity.TOP | Gravity.START;
            int hg = g & Gravity.HORIZONTAL_GRAVITY_MASK;
            int vg = g & Gravity.VERTICAL_GRAVITY_MASK;

            int cl = switch (hg) {
                case Gravity.CENTER_HORIZONTAL -> parentLeft + (parentRight - parentLeft - w) / 2 + lp.leftMargin - lp.rightMargin;
                case Gravity.RIGHT -> parentRight - w - lp.rightMargin;
                default -> parentLeft + lp.leftMargin;
            };
            int ct = switch (vg) {
                case Gravity.CENTER_VERTICAL -> parentTop + (parentBottom - parentTop - h) / 2 + lp.topMargin - lp.bottomMargin;
                case Gravity.BOTTOM -> parentBottom - h - lp.bottomMargin;
                default -> parentTop + lp.topMargin;
            };
            child.layout(cl, ct, cl + w, ct + h);
        }
    }
}
