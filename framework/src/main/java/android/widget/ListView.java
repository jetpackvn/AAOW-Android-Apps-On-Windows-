package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ListView extends AbsListView {
    private ListAdapter adapter;
    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int contentHeight;
    private float dragLastY;
    private boolean dragging;

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

    @Override
    public void setSelection(int position) {
        super.setSelection(position);
    }

    public void setSelectionFromTop(int position, int y) {
        super.setSelection(position);
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
        this.contentHeight = totalH;
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
        clampScroll();
    }

    private int maxScrollY() {
        return Math.max(0, contentHeight - getHeight());
    }

    private void clampScroll() {
        int max = maxScrollY();
        if (scrollY < 0) scrollY = 0;
        if (scrollY > max) scrollY = max;
    }

    @Override
    public boolean canScrollVertically(int direction) {
        if (direction < 0) return scrollY > 0;
        if (direction > 0) return scrollY < maxScrollY();
        return maxScrollY() > 0;
    }

    @Override
    public boolean onMouseWheel(int wheelRotation, float x, float y) {
        if (super.onMouseWheel(wheelRotation, x, y)) return true;
        int max = maxScrollY();
        if (max <= 0) return false;
        scrollTo(0, Math.max(0, Math.min(max, scrollY + wheelRotation * 36)));
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (maxScrollY() > 0) {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN -> {
                    dragLastY = ev.getY();
                    dragging = false;
                }
                case MotionEvent.ACTION_MOVE -> {
                    float dy = dragLastY - ev.getY();
                    if (Math.abs(dy) > 8f) dragging = true;
                    if (dragging) {
                        dragLastY = ev.getY();
                        scrollTo(0, Math.max(0, Math.min(maxScrollY(), Math.round(scrollY + dy))));
                        return true;
                    }
                }
                case MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        dragging = false;
                        return true;
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
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
