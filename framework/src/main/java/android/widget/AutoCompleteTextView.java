package android.widget;

import android.content.Context;
import android.util.AttributeSet;

public class AutoCompleteTextView extends EditText implements Filter.FilterListener {
    public interface OnDismissListener {
        void onDismiss();
    }

    private ListAdapter adapter;
    private int threshold = 2;
    private int dropDownAnchor = NO_ID;
    private int dropDownWidth = -2;
    private int listSelection = -1;
    private AdapterView.OnItemClickListener itemClickListener;
    private AdapterView.OnItemSelectedListener itemSelectedListener;
    private OnDismissListener dismissListener;
    private android.graphics.drawable.Drawable dropDownBackground;

    public AutoCompleteTextView(Context context) {
        this(context, null, 0);
    }

    public AutoCompleteTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
    }

    public void setThreshold(int threshold) {
        this.threshold = Math.max(1, threshold);
    }

    public int getThreshold() {
        return threshold;
    }

    public ListAdapter getAdapter() {
        return adapter;
    }

    public <T extends ListAdapter> void setAdapter(T adapter) {
        this.adapter = adapter;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener l) {
        this.itemClickListener = l;
    }

    public AdapterView.OnItemClickListener getOnItemClickListener() {
        return itemClickListener;
    }

    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener l) {
        this.itemSelectedListener = l;
    }

    public AdapterView.OnItemSelectedListener getOnItemSelectedListener() {
        return itemSelectedListener;
    }

    public void setOnDismissListener(OnDismissListener dismissListener) {
        this.dismissListener = dismissListener;
    }

    public void setText(CharSequence text, boolean filter) {
        setText(text);
    }

    public void showDropDown() {
    }

    public void dismissDropDown() {
    }

    public boolean isPopupShowing() {
        return false;
    }

    public int getDropDownAnchor() {
        return dropDownAnchor;
    }

    public void setDropDownAnchor(int id) {
        this.dropDownAnchor = id;
    }

    public int getDropDownWidth() {
        return dropDownWidth;
    }

    public void setDropDownWidth(int width) {
        this.dropDownWidth = width;
    }

    public android.graphics.drawable.Drawable getDropDownBackground() {
        return dropDownBackground;
    }

    public void setDropDownBackgroundDrawable(android.graphics.drawable.Drawable d) {
        this.dropDownBackground = d;
    }

    public void setDropDownBackgroundResource(int id) {
        if (getResources() != null && id != 0) {
            this.dropDownBackground = getResources().getDrawable(id);
        }
    }

    public int getListSelection() {
        return listSelection;
    }

    public void setListSelection(int position) {
        this.listSelection = position;
    }

    public void clearListSelection() {
        this.listSelection = -1;
    }

    @Override
    public void onFilterComplete(int count) {
    }
}
