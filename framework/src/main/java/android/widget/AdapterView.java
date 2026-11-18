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
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(this, view, position, id);
            return true;
        }
        return false;
    }
}
