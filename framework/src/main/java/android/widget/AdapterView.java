package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public abstract class AdapterView<T extends Adapter> extends ViewGroup {
    public static final int INVALID_POSITION = -1;
    public static final long INVALID_ROW_ID = Long.MIN_VALUE;

    public interface OnItemClickListener {
        void onItemClick(AdapterView<?> parent, View view, int position, long id);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id);
    }

    public interface OnItemSelectedListener {
        void onItemSelected(AdapterView<?> parent, View view, int position, long id);

        void onNothingSelected(AdapterView<?> parent);
    }

    protected OnItemClickListener onItemClickListener;
    protected OnItemLongClickListener onItemLongClickListener;
    protected OnItemSelectedListener onItemSelectedListener;
    protected int selectedPosition = INVALID_POSITION;
    protected int firstVisiblePosition = 0;

    public AdapterView(Context context) {
        super(context);
    }

    public AdapterView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AdapterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public abstract T getAdapter();
    public abstract void setAdapter(T adapter);

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public final OnItemClickListener getOnItemClickListener() {
        return onItemClickListener;
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.onItemSelectedListener = listener;
    }

    public final OnItemSelectedListener getOnItemSelectedListener() {
        return onItemSelectedListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }

    public boolean performItemClick(View view, int position, long id) {
        this.selectedPosition = position;
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(this, view, position, id);
            return true;
        }
        return false;
    }

    public int getSelectedItemPosition() {
        return selectedPosition;
    }

    public long getSelectedItemId() {
        T adapter = getAdapter();
        if (adapter != null && selectedPosition >= 0 && selectedPosition < adapter.getCount()) {
            return adapter.getItemId(selectedPosition);
        }
        return INVALID_ROW_ID;
    }

    public Object getSelectedItem() {
        T adapter = getAdapter();
        if (adapter != null && selectedPosition >= 0 && selectedPosition < adapter.getCount()) {
            return adapter.getItem(selectedPosition);
        }
        return null;
    }

    public View getSelectedView() {
        if (selectedPosition >= 0 && selectedPosition < getChildCount()) {
            return getChildAt(selectedPosition);
        }
        return null;
    }

    public void setSelection(int position) {
        this.selectedPosition = position;
    }

    public int getFirstVisiblePosition() {
        return firstVisiblePosition;
    }

    public int getLastVisiblePosition() {
        return Math.max(0, getChildCount() - 1);
    }

    public int getCount() {
        T adapter = getAdapter();
        return adapter != null ? adapter.getCount() : 0;
    }

    public Object getItemAtPosition(int position) {
        T adapter = getAdapter();
        return (adapter == null || position < 0 || position >= adapter.getCount()) ? null : adapter.getItem(position);
    }

    public long getItemIdAtPosition(int position) {
        T adapter = getAdapter();
        return (adapter == null || position < 0 || position >= adapter.getCount()) ? INVALID_ROW_ID : adapter.getItemId(position);
    }
}
