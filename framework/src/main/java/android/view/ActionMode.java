package android.view;

public abstract class ActionMode {
    public interface Callback {
        boolean onCreateActionMode(ActionMode mode, Menu menu);

        boolean onPrepareActionMode(ActionMode mode, Menu menu);

        boolean onActionItemClicked(ActionMode mode, MenuItem item);

        void onDestroyActionMode(ActionMode mode);
    }

    public static final int TYPE_PRIMARY = 0;
    public static final int TYPE_FLOATING = 1;
    public static final int DEFAULT_HIDE_DURATION = -1;

    private Object tag;
    private boolean titleOptionalHint;
    private int type = TYPE_PRIMARY;

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public Object getTag() {
        return tag;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setTitleOptionalHint(boolean titleOptional) {
        this.titleOptionalHint = titleOptional;
    }

    public boolean getTitleOptionalHint() {
        return titleOptionalHint;
    }

    public boolean isTitleOptional() {
        return false;
    }

    public abstract void setTitle(CharSequence title);

    public abstract void setTitle(int resId);

    public abstract void setSubtitle(CharSequence subtitle);

    public abstract void setSubtitle(int resId);

    public abstract void setCustomView(View view);

    public abstract void invalidate();

    public abstract void finish();

    public abstract Menu getMenu();

    public abstract CharSequence getTitle();

    public abstract CharSequence getSubtitle();

    public abstract View getCustomView();

    public abstract MenuInflater getMenuInflater();
}
