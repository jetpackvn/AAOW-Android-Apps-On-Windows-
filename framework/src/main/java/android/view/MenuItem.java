package android.view;

public interface MenuItem {
    int getItemId();
    int getGroupId();
    int getOrder();
    MenuItem setTitle(CharSequence title);
    MenuItem setTitle(int title);
    CharSequence getTitle();
    MenuItem setVisible(boolean visible);
    boolean isVisible();
    MenuItem setEnabled(boolean enabled);
    boolean isEnabled();
}
