package android.widget;

import android.content.Context;
import android.util.AttributeSet;

public abstract class AbsListView extends AdapterView<ListAdapter> {
    public static final int CHOICE_MODE_NONE = 0;
    public static final int CHOICE_MODE_SINGLE = 1;
    public static final int CHOICE_MODE_MULTIPLE = 2;
    public static final int CHOICE_MODE_MULTIPLE_MODAL = 3;

    public interface OnScrollListener {
        int SCROLL_STATE_IDLE = 0;
        int SCROLL_STATE_TOUCH_SCROLL = 1;
        int SCROLL_STATE_FLING = 2;

        void onScrollStateChanged(AbsListView view, int scrollState);

        void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount);
    }

    private int choiceMode = CHOICE_MODE_NONE;
    private int checkedPosition = INVALID_POSITION;
    private OnScrollListener onScrollListener;

    public AbsListView(Context context) {
        super(context);
    }

    public AbsListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AbsListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setChoiceMode(int choiceMode) {
        this.choiceMode = choiceMode;
    }

    public int getChoiceMode() {
        return choiceMode;
    }

    public int getCheckedItemPosition() {
        return checkedPosition;
    }

    public void setItemChecked(int position, boolean value) {
        this.checkedPosition = value ? position : INVALID_POSITION;
        invalidate();
    }

    public void setOnScrollListener(OnScrollListener l) {
        this.onScrollListener = l;
    }

    public OnScrollListener getOnScrollListener() {
        return onScrollListener;
    }
}
