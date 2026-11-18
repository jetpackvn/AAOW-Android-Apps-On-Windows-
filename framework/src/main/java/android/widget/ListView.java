package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class ListView extends AbsListView {
    private ListAdapter adapter;
    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ListView(Context context) {
        this(context, null);
    }

    public ListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public ListAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        this.adapter = adapter;
        if (adapter instanceof BaseAdapter ba) {
            ba.registerObserver(this::rebuildItems);
        }
        rebuildItems();
    }

    private void rebuildItems() {
        removeAllViews();
        if (adapter == null) return;
        int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            final int pos = i;
            View itemView = adapter.getView(i, null, this);
            if (itemView != null) {
                itemView.setOnClickListener(v -> performItemClick(v, pos, adapter.getItemId(pos)));
                addView(itemView, new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            }
        }
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int totalH = paddingTop + paddingBottom;
        int maxW = 0;
        for (View child : children) {
            measureChild(child, widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            totalH += child.getMeasuredHeight() + 1;
            maxW = Math.max(maxW, child.getMeasuredWidth());
        }
        setMeasuredDimension(
                resolveSize(maxW + paddingLeft + paddingRight, widthMeasureSpec),
                resolveSize(totalH, heightMeasureSpec)
        );
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int y = paddingTop;
        int w = (r - l) - paddingLeft - paddingRight;
        for (View child : children) {
            int ch = child.getMeasuredHeight();
            child.layout(paddingLeft, y, paddingLeft + w, y + ch);
            y += ch + 1;
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        dividerPaint.setColor(0xFFE4E6EF);
        for (View child : children) {
            int dy = child.getBottom() - scrollY;
            canvas.drawLine(paddingLeft, dy, getWidth() - paddingRight, dy, dividerPaint);
        }
    }
}
