package android.view;

import android.content.Intent;
import android.graphics.drawable.Drawable;

public interface MenuItem {
    int SHOW_AS_ACTION_NEVER = 0;
    int SHOW_AS_ACTION_IF_ROOM = 1;
    int SHOW_AS_ACTION_ALWAYS = 2;
    int SHOW_AS_ACTION_WITH_TEXT = 4;
    int SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW = 8;

    interface OnMenuItemClickListener {
        boolean onMenuItemClick(MenuItem item);
    }

    interface OnActionExpandListener {
        boolean onMenuItemActionExpand(MenuItem item);

        boolean onMenuItemActionCollapse(MenuItem item);
    }

    int getItemId();

    int getGroupId();

    int getOrder();

    MenuItem setTitle(CharSequence title);

    MenuItem setTitle(int title);

    CharSequence getTitle();

    default MenuItem setTitleCondensed(CharSequence title) {
        return this;
    }

    default CharSequence getTitleCondensed() {
        return getTitle();
    }

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

    default boolean hasSubMenu() {
        return false;
    }

    default SubMenu getSubMenu() {
        return null;
    }

    default MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
        return this;
    }

    default void setShowAsAction(int actionEnum) {
    }

    default MenuItem setShowAsActionFlags(int actionEnum) {
        setShowAsAction(actionEnum);
        return this;
    }

    default View getActionView() {
        return null;
    }

    default MenuItem setActionView(View view) {
        return this;
    }

    default MenuItem setActionView(int resId) {
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

    default MenuItem setIntent(Intent intent) {
        return this;
    }

    default Intent getIntent() {
        return null;
    }

    default MenuItem setShortcut(char numericChar, char alphaChar) {
        return this;
    }

    default MenuItem setNumericShortcut(char numericChar) {
        return this;
    }

    default char getNumericShortcut() {
        return 0;
    }

    default MenuItem setAlphabeticShortcut(char alphaChar) {
        return this;
    }

    default char getAlphabeticShortcut() {
        return 0;
    }
}
