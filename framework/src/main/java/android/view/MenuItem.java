package android.view;

import android.graphics.drawable.Drawable;

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

    default MenuItem setChecked(boolean checked) {
        return this;
    }

    default boolean isChecked() {
        return false;
    }

    default MenuItem setCheckable(boolean checkable) {
        return this;
    }

    default boolean isCheckable() {
        return false;
    }

    default MenuItem setIcon(Drawable icon) {
        return this;
    }

    default MenuItem setIcon(int iconRes) {
        return this;
    }

    default Drawable getIcon() {
        return null;
    }

    default View getActionView() {
        return null;
    }

    default MenuItem setActionView(View view) {
        return this;
    }

    default boolean expandActionView() {
        return false;
    }

    default boolean collapseActionView() {
        return false;
    }

    default boolean isActionViewExpanded() {
        return false;
    }
}
