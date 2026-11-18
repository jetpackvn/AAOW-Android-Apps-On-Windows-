package android.view;

public interface Menu {
    int NONE = 0;
    int FIRST = 1;

    int CATEGORY_CONTAINER = 0x00010000;
    int CATEGORY_SYSTEM = 0x00020000;
    int CATEGORY_SECONDARY = 0x00030000;
    int CATEGORY_ALTERNATIVE = 0x00040000;

    int FLAG_PERFORM_NO_CLOSE = 0x0001;
    int FLAG_ALWAYS_PERFORM_CLOSE = 0x0002;

    MenuItem add(CharSequence title);

    MenuItem add(int titleRes);

    MenuItem add(int groupId, int itemId, int order, CharSequence title);

    MenuItem add(int groupId, int itemId, int order, int titleRes);

    SubMenu addSubMenu(CharSequence title);

    SubMenu addSubMenu(int titleRes);

    SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title);

    SubMenu addSubMenu(int groupId, int itemId, int order, int titleRes);

    void removeItem(int id);

    void removeGroup(int groupId);

    void setGroupCheckable(int group, boolean checkable, boolean exclusive);

    void setGroupVisible(int group, boolean visible);

    void setGroupEnabled(int group, boolean enabled);

    boolean hasVisibleItems();

    MenuItem findItem(int id);

    int size();

    MenuItem getItem(int index);

    void clear();

    boolean performShortcut(int keyCode, KeyEvent event, int flags);

    boolean isShortcutKey(int keyCode, KeyEvent event);

    boolean performIdentifierAction(int id, int flags);

    void close();
}
