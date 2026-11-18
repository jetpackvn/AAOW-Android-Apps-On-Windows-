package dev.aaow.rt.compat;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Container tương thích RecyclerView với Adapter / ViewHolder và hỗ trợ cuộn dọc trên Windows.
 */
public class CompatRecyclerView extends ViewGroup {
    public static abstract class ViewHolder {
        public final View itemView;

        public ViewHolder(View itemView) {
            this.itemView = itemView;
        }
    }

    public static abstract class Adapter<VH extends ViewHolder> {
        private final List<Runnable> observers = new ArrayList<>();

        public abstract VH onCreateViewHolder(ViewGroup parent, int viewType);

        public abstract void onBindViewHolder(VH holder, int position);

        public int getItemViewType(int position) {
            return 0;
        }

        public abstract int getItemCount();

        public final void notifyDataSetChanged() {
            for (Runnable r : observers) r.run();
        }

        void registerObserver(Runnable r) {
            if (r != null && !observers.contains(r)) observers.add(r);
        }
    }

    private Adapter<ViewHolder> adapter;
    private int spanCount = 1;
    private int contentHeight;
    private float dragLastY;
    private boolean dragging;

    public CompatRecyclerView(Context context) {
        this(context, null, 0);
    }

    public CompatRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CompatRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressWarnings("unchecked")
    public void setAdapter(Adapter<?> adapter) {
        this.adapter = (Adapter<ViewHolder>) adapter;
        if (this.adapter != null) {
            this.adapter.registerObserver(this::rebuildItems);
        }
        rebuildItems();
    }

    public Adapter<?> getAdapter() {
        return adapter;
    }

    public void setSpanCount(int spanCount) {
        this.spanCount = Math.max(1, spanCount);
        requestLayout();
    }

    private void rebuildItems() {
        removeAllViews();
        if (adapter == null) return;
        int count = adapter.getItemCount();
        for (int i = 0; i < count; i++) {
            int type = adapter.getItemViewType(i);
            ViewHolder vh = adapter.onCreateViewHolder(this, type);
            if (vh != null && vh.itemView != null) {
                adapter.onBindViewHolder(vh, i);
                addView(vh.itemView);
            }
        }
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentW = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED
                ? 420 : MeasureSpec.getSize(widthMeasureSpec);
        int innerW = Math.max(0, parentW - paddingLeft - paddingRight);
        int cellW = innerW / spanCount;

        int totalH = paddingTop + paddingBottom;
        int rowMaxH = 0;
        int col = 0;
        for (View child : children) {
            if (child.getVisibility() == GONE) continue;
            int cwSpec = MeasureSpec.makeMeasureSpec(cellW, spanCount > 1 ? MeasureSpec.EXACTLY : MeasureSpec.AT_MOST);
            int chSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            child.measure(cwSpec, chSpec);
            rowMaxH = Math.max(rowMaxH, child.getMeasuredHeight());
            col++;
            if (col >= spanCount) {
                totalH += rowMaxH;
                rowMaxH = 0;
                col = 0;
            }
        }
        if (col > 0) totalH += rowMaxH;
        this.contentHeight = totalH;

        setMeasuredDimension(
                resolveSize(parentW, widthMeasureSpec),
                resolveSize(totalH, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int innerW = Math.max(0, (r - l) - paddingLeft - paddingRight);
        int cellW = innerW / spanCount;
        int y = paddingTop;
        int rowMaxH = 0;
        int col = 0;
        for (View child : children) {
            if (child.getVisibility() == GONE) continue;
            int ch = child.getMeasuredHeight();
            int cw = spanCount > 1 ? cellW : Math.min(innerW, Math.max(child.getMeasuredWidth(), innerW));
            int cx = paddingLeft + col * cellW;
            child.layout(cx, y, cx + cw, y + ch);
            rowMaxH = Math.max(rowMaxH, ch);
            col++;
            if (col >= spanCount) {
                y += rowMaxH;
                rowMaxH = 0;
                col = 0;
            }
        }
        int maxScroll = Math.max(0, contentHeight - getHeight());
        if (scrollY > maxScroll) scrollY = maxScroll;
    }

    @Override
    public boolean canScrollVertically(int direction) {
        int max = Math.max(0, contentHeight - getHeight());
        if (direction < 0) return scrollY > 0;
        if (direction > 0) return scrollY < max;
        return max > 0;
    }

    @Override
    public boolean onMouseWheel(int wheelRotation, float x, float y) {
        if (super.onMouseWheel(wheelRotation, x, y)) return true;
        int max = Math.max(0, contentHeight - getHeight());
        if (max <= 0) return false;
        scrollTo(0, Math.max(0, Math.min(max, scrollY + wheelRotation * 36)));
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int max = Math.max(0, contentHeight - getHeight());
        if (max > 0) {
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
                        scrollTo(0, Math.max(0, Math.min(max, Math.round(scrollY + dy))));
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
}
