package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;

/**
 * Bố cục dạng bảng tương thích {@code android.widget.TableLayout} (mặc định xếp hàng theo chiều dọc).
 */
public class TableLayout extends LinearLayout {
    private boolean shrinkAllColumns;
    private boolean stretchAllColumns;
    private final SparseBooleanArray collapsedColumns = new SparseBooleanArray();
    private final SparseBooleanArray shrinkableColumns = new SparseBooleanArray();
    private final SparseBooleanArray stretchableColumns = new SparseBooleanArray();

    public static class LayoutParams extends LinearLayout.LayoutParams {
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            if (width == 0) {
                width = MATCH_PARENT;
            }
        }

        public LayoutParams(int w, int h) {
            super(MATCH_PARENT, h);
        }

        public LayoutParams(int w, int h, float initWeight) {
            super(MATCH_PARENT, h, initWeight);
        }

        public LayoutParams() {
            super(MATCH_PARENT, WRAP_CONTENT);
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
            this.width = MATCH_PARENT;
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
            this.width = MATCH_PARENT;
        }

        protected void setBaseAttributes(TypedArray a, int widthAttr, int heightAttr) {
            this.width = MATCH_PARENT;
            this.height = WRAP_CONTENT;
        }
    }

    public TableLayout(Context context) {
        super(context);
        setOrientation(VERTICAL);
    }

    public TableLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        if (attrs instanceof AttributeSet.XmlAttributeSet xas) {
            parseColumnList(xas.getAttributeValue(null, "stretchColumns"), stretchableColumns, true);
            parseColumnList(xas.getAttributeValue(null, "shrinkColumns"), shrinkableColumns, false);
            parseColumnList(xas.getAttributeValue(null, "collapseColumns"), collapsedColumns, false);
        }
    }

    private void parseColumnList(String spec, SparseBooleanArray target, boolean isStretch) {
        if (spec == null || spec.isEmpty()) return;
        String trimmed = spec.trim();
        if ("*".equals(trimmed)) {
            if (isStretch) stretchAllColumns = true;
            else if (target == shrinkableColumns) shrinkAllColumns = true;
            return;
        }
        for (String part : trimmed.split(",")) {
            try {
                int col = Integer.parseInt(part.trim());
                if (col >= 0) target.put(col, true);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public boolean isShrinkAllColumns() {
        return shrinkAllColumns;
    }

    public void setShrinkAllColumns(boolean shrinkAllColumns) {
        this.shrinkAllColumns = shrinkAllColumns;
    }

    public boolean isStretchAllColumns() {
        return stretchAllColumns;
    }

    public void setStretchAllColumns(boolean stretchAllColumns) {
        this.stretchAllColumns = stretchAllColumns;
    }

    public void setColumnCollapsed(int columnIndex, boolean isCollapsed) {
        collapsedColumns.put(columnIndex, isCollapsed);
        requestLayout();
    }

    public boolean isColumnCollapsed(int columnIndex) {
        return collapsedColumns.get(columnIndex);
    }

    public void setColumnStretchable(int columnIndex, boolean isStretchable) {
        stretchableColumns.put(columnIndex, isStretchable);
        requestLayout();
    }

    public boolean isColumnStretchable(int columnIndex) {
        return stretchAllColumns || stretchableColumns.get(columnIndex);
    }

    public void setColumnShrinkable(int columnIndex, boolean isShrinkable) {
        shrinkableColumns.put(columnIndex, isShrinkable);
        requestLayout();
    }

    public boolean isColumnShrinkable(int columnIndex) {
        return shrinkAllColumns || shrinkableColumns.get(columnIndex);
    }

    @Override
    public void addView(View child) {
        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        super.addView(child, index);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        super.addView(child, params != null ? params : generateDefaultLayoutParams());
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params != null ? params : generateDefaultLayoutParams());
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
        return TableLayout.class.getName();
    }
}
