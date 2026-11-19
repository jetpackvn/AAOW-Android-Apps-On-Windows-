package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Hàng trong {@link TableLayout}, tương thích {@code android.widget.TableRow}.
 */
public class TableRow extends LinearLayout {
    public static class LayoutParams extends LinearLayout.LayoutParams {
        public int column = -1;
        public int span = 1;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            if (attrs instanceof AttributeSet.XmlAttributeSet xas) {
                this.column = xas.getAttributeIntValue(null, "layout_column", -1);
                this.span = Math.max(1, xas.getAttributeIntValue(null, "layout_span", 1));
            }
        }

        public LayoutParams(int w, int h) {
            super(w, h);
        }

        public LayoutParams(int w, int h, float initWeight) {
            super(w, h, initWeight);
        }

        public LayoutParams() {
            super(MATCH_PARENT, WRAP_CONTENT);
        }

        public LayoutParams(int column) {
            this();
            this.column = column;
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        protected void setBaseAttributes(TypedArray a, int widthAttr, int heightAttr) {
            this.width = MATCH_PARENT;
            this.height = WRAP_CONTENT;
        }
    }

    public TableRow(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
    }

    public TableRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
    }

    public View getVirtualChildAt(int i) {
        return (i >= 0 && i < getChildCount()) ? getChildAt(i) : null;
    }

    public int getVirtualChildCount() {
        return getChildCount();
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LinearLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected LinearLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    public CharSequence getAccessibilityClassName() {
        return TableRow.class.getName();
    }
}
