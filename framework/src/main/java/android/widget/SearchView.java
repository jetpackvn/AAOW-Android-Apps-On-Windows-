package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.CollapsibleActionView;
import android.view.View;
import android.view.ViewGroup;

public class SearchView extends LinearLayout implements CollapsibleActionView {
    public interface OnQueryTextListener {
        boolean onQueryTextSubmit(String query);

        boolean onQueryTextChange(String newText);
    }

    public interface OnCloseListener {
        boolean onClose();
    }

    private final EditText queryEdit;
    private OnQueryTextListener queryTextListener;
    private OnCloseListener closeListener;
    private boolean iconifiedByDefault = true;
    private boolean iconified = true;
    private boolean submitButtonEnabled;
    private int imeOptions;
    private int inputType = 1;
    private int maxWidth = Integer.MAX_VALUE;
    private OnClickListener searchClickListener;
    private OnFocusChangeListener queryFocusChangeListener;

    public SearchView(Context context) {
        this(context, null, 0, 0);
    }

    public SearchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public SearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SearchView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        queryEdit = new EditText(context);
        queryEdit.setHint("Search…");
        queryEdit.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (queryTextListener != null) {
                    queryTextListener.onQueryTextChange(s != null ? s.toString() : "");
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
        addView(queryEdit, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public void setOnQueryTextListener(OnQueryTextListener listener) {
        this.queryTextListener = listener;
    }

    public void setOnCloseListener(OnCloseListener listener) {
        this.closeListener = listener;
    }

    public CharSequence getQuery() {
        return queryEdit.getText();
    }

    public void setQuery(CharSequence query, boolean submit) {
        queryEdit.setText(query);
        if (submit && queryTextListener != null) {
            queryTextListener.onQueryTextSubmit(query != null ? query.toString() : "");
        }
    }

    public void setQueryHint(CharSequence hint) {
        queryEdit.setHint(hint);
    }

    public CharSequence getQueryHint() {
        return queryEdit.getHint();
    }

    public void setIconifiedByDefault(boolean iconified) {
        this.iconifiedByDefault = iconified;
    }

    public boolean isIconfiedByDefault() {
        return iconifiedByDefault;
    }

    public boolean isIconifiedByDefault() {
        return iconifiedByDefault;
    }

    public void setIconified(boolean iconify) {
        this.iconified = iconify;
        if (iconify && closeListener != null) {
            closeListener.onClose();
        }
    }

    public boolean isIconified() {
        return iconified;
    }

    public void setSubmitButtonEnabled(boolean enabled) {
        this.submitButtonEnabled = enabled;
    }

    public boolean isSubmitButtonEnabled() {
        return submitButtonEnabled;
    }

    public void setMaxWidth(int maxpixels) {
        this.maxWidth = maxpixels;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setImeOptions(int imeOptions) {
        this.imeOptions = imeOptions;
    }

    public int getImeOptions() {
        return imeOptions;
    }

    public void setInputType(int inputType) {
        this.inputType = inputType;
        queryEdit.setInputType(inputType);
    }

    public int getInputType() {
        return inputType;
    }

    public void setOnSearchClickListener(OnClickListener listener) {
        this.searchClickListener = listener;
    }

    public void setOnQueryTextFocusChangeListener(OnFocusChangeListener listener) {
        this.queryFocusChangeListener = listener;
        queryEdit.setOnFocusChangeListener(listener);
    }

    @Override
    public void onActionViewExpanded() {
        setIconified(false);
    }

    @Override
    public void onActionViewCollapsed() {
        setQuery("", false);
        setIconified(true);
    }
}
