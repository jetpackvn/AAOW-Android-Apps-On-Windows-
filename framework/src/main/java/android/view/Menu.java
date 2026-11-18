package android.view;

public interface Menu {
    int NONE = 0;
    MenuItem add(CharSequence title);
    MenuItem add(int groupId, int itemId, int order, CharSequence title);
    MenuItem add(int groupId, int itemId, int order, int titleRes);
    MenuItem findItem(int id);
    int size();
    MenuItem getItem(int index);
    void clear();
}
