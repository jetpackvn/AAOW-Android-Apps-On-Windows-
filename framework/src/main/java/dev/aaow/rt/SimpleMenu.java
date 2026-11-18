package dev.aaow.rt;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Cài đặt chuẩn in-memory cho android.view.Menu, MenuItem và SubMenu.
 */
public class SimpleMenu implements Menu {
    private final Context context;
    private final List<SimpleMenuItem> items = new ArrayList<>();

    public SimpleMenu(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    @Override
    public MenuItem add(CharSequence title) {
        return add(NONE, NONE, NONE, title);
    }

    @Override
    public MenuItem add(int titleRes) {
        return add(NONE, NONE, NONE, titleRes);
    }

    @Override
    public MenuItem add(int groupId, int itemId, int order, CharSequence title) {
        SimpleMenuItem item = new SimpleMenuItem(this, groupId, itemId, order, title);
        insertOrdered(item);
        return item;
    }

    @Override
    public MenuItem add(int groupId, int itemId, int order, int titleRes) {
        CharSequence t = context != null && titleRes != 0 ? context.getText(titleRes) : "";
        return add(groupId, itemId, order, t);
    }

    private void insertOrdered(SimpleMenuItem item) {
        int pos = items.size();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getOrder() > item.getOrder()) {
                pos = i;
                break;
            }
        }
        items.add(pos, item);
    }

    @Override
    public SubMenu addSubMenu(CharSequence title) {
        return addSubMenu(NONE, NONE, NONE, title);
    }

    @Override
    public SubMenu addSubMenu(int titleRes) {
        return addSubMenu(NONE, NONE, NONE, titleRes);
    }

    @Override
    public SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title) {
        SimpleMenuItem item = (SimpleMenuItem) add(groupId, itemId, order, title);
        SimpleSubMenu sub = new SimpleSubMenu(context, item);
        item.setSubMenu(sub);
        return sub;
    }

    @Override
    public SubMenu addSubMenu(int groupId, int itemId, int order, int titleRes) {
        CharSequence t = context != null && titleRes != 0 ? context.getText(titleRes) : "";
        return addSubMenu(groupId, itemId, order, t);
    }

    @Override
    public void removeItem(int id) {
        items.removeIf(i -> i.getItemId() == id);
    }

    @Override
    public void removeGroup(int groupId) {
        items.removeIf(i -> i.getGroupId() == groupId);
    }

    @Override
    public void setGroupCheckable(int group, boolean checkable, boolean exclusive) {
        for (SimpleMenuItem item : items) {
            if (item.getGroupId() == group) {
                item.setCheckable(checkable);
                item.exclusiveCheck = exclusive;
            }
        }
    }

    @Override
    public void setGroupVisible(int group, boolean visible) {
        for (SimpleMenuItem item : items) {
            if (item.getGroupId() == group) item.setVisible(visible);
        }
    }

    @Override
    public void setGroupEnabled(int group, boolean enabled) {
        for (SimpleMenuItem item : items) {
            if (item.getGroupId() == group) item.setEnabled(enabled);
        }
    }

    @Override
    public boolean hasVisibleItems() {
        for (SimpleMenuItem item : items) {
            if (item.isVisible()) return true;
        }
        return false;
    }

    @Override
    public MenuItem findItem(int id) {
        for (SimpleMenuItem item : items) {
            if (item.getItemId() == id) return item;
            if (item.hasSubMenu()) {
                MenuItem sub = item.getSubMenu().findItem(id);
                if (sub != null) return sub;
            }
        }
        return null;
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public MenuItem getItem(int index) {
        return items.get(index);
    }

    @Override
    public void clear() {
        items.clear();
    }

    @Override
    public boolean performShortcut(int keyCode, KeyEvent event, int flags) {
        return false;
    }

    @Override
    public boolean isShortcutKey(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean performIdentifierAction(int id, int flags) {
        MenuItem item = findItem(id);
        return item instanceof SimpleMenuItem smi && smi.invokeClick();
    }

    @Override
    public void close() {
    }

    public List<SimpleMenuItem> getVisibleItems() {
        List<SimpleMenuItem> out = new ArrayList<>();
        for (SimpleMenuItem item : items) {
            if (item.isVisible()) out.add(item);
        }
        return out;
    }

    public static final class SimpleMenuItem implements MenuItem {
        private final SimpleMenu parentMenu;
        private final int groupId;
        private final int itemId;
        private final int order;
        private CharSequence title;
        private CharSequence titleCondensed;
        private boolean visible = true;
        private boolean enabled = true;
        private boolean checkable;
        private boolean checked;
        boolean exclusiveCheck;
        private Drawable icon;
        private SimpleSubMenu subMenu;
        private OnMenuItemClickListener clickListener;
        private int showAsAction = SHOW_AS_ACTION_NEVER;
        private View actionView;
        private boolean actionViewExpanded;
        private Intent intent;
        private char numericShortcut;
        private char alphaShortcut;

        SimpleMenuItem(SimpleMenu parentMenu, int groupId, int itemId, int order, CharSequence title) {
            this.parentMenu = parentMenu;
            this.groupId = groupId;
            this.itemId = itemId;
            this.order = order;
            this.title = title != null ? title : "";
        }

        @Override
        public int getItemId() {
            return itemId;
        }

        @Override
        public int getGroupId() {
            return groupId;
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public MenuItem setTitle(CharSequence title) {
            this.title = title != null ? title : "";
            return this;
        }

        @Override
        public MenuItem setTitle(int title) {
            if (parentMenu.context != null && title != 0) {
                this.title = parentMenu.context.getText(title);
            }
            return this;
        }

        @Override
        public CharSequence getTitle() {
            return title;
        }

        @Override
        public MenuItem setTitleCondensed(CharSequence title) {
            this.titleCondensed = title;
            return this;
        }

        @Override
        public CharSequence getTitleCondensed() {
            return titleCondensed != null ? titleCondensed : title;
        }

        @Override
        public MenuItem setVisible(boolean visible) {
            this.visible = visible;
            return this;
        }

        @Override
        public boolean isVisible() {
            return visible;
        }

        @Override
        public MenuItem setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public MenuItem setChecked(boolean checked) {
            if (exclusiveCheck && checked && groupId != NONE) {
                for (SimpleMenuItem sibling : parentMenu.items) {
                    if (sibling.groupId == groupId) {
                        sibling.checked = (sibling == this);
                    }
                }
            } else {
                this.checked = checked;
            }
            return this;
        }

        @Override
        public boolean isChecked() {
            return checked;
        }

        @Override
        public MenuItem setCheckable(boolean checkable) {
            this.checkable = checkable;
            return this;
        }

        @Override
        public boolean isCheckable() {
            return checkable;
        }

        @Override
        public MenuItem setIcon(Drawable icon) {
            this.icon = icon;
            return this;
        }

        @Override
        public MenuItem setIcon(int iconRes) {
            if (parentMenu.context != null && iconRes != 0) {
                this.icon = parentMenu.context.getDrawable(iconRes);
            }
            return this;
        }

        @Override
        public Drawable getIcon() {
            return icon;
        }

        void setSubMenu(SimpleSubMenu sub) {
            this.subMenu = sub;
        }

        @Override
        public boolean hasSubMenu() {
            return subMenu != null;
        }

        @Override
        public SubMenu getSubMenu() {
            return subMenu;
        }

        @Override
        public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener listener) {
            this.clickListener = listener;
            return this;
        }

        public boolean invokeClick() {
            if (!enabled) return false;
            if (clickListener != null && clickListener.onMenuItemClick(this)) {
                return true;
            }
            return false;
        }

        @Override
        public void setShowAsAction(int actionEnum) {
            this.showAsAction = actionEnum;
        }

        public int getShowAsAction() {
            return showAsAction;
        }

        public boolean requestsActionButton() {
            return (showAsAction & (SHOW_AS_ACTION_ALWAYS | SHOW_AS_ACTION_IF_ROOM)) != 0;
        }

        @Override
        public MenuItem setActionView(View view) {
            this.actionView = view;
            return this;
        }

        @Override
        public MenuItem setActionView(int resId) {
            if (parentMenu.context != null && resId != 0) {
                this.actionView = LayoutInflater.from(parentMenu.context).inflate(resId, null, false);
            }
            return this;
        }

        @Override
        public View getActionView() {
            return actionView;
        }

        @Override
        public boolean expandActionView() {
            if (actionView == null) return false;
            actionViewExpanded = true;
            return true;
        }

        @Override
        public boolean collapseActionView() {
            if (!actionViewExpanded) return false;
            actionViewExpanded = false;
            return true;
        }

        @Override
        public boolean isActionViewExpanded() {
            return actionViewExpanded;
        }

        @Override
        public MenuItem setIntent(Intent intent) {
            this.intent = intent;
            return this;
        }

        @Override
        public Intent getIntent() {
            return intent;
        }

        @Override
        public MenuItem setShortcut(char numericChar, char alphaChar) {
            this.numericShortcut = numericChar;
            this.alphaShortcut = alphaChar;
            return this;
        }

        @Override
        public MenuItem setNumericShortcut(char numericChar) {
            this.numericShortcut = numericChar;
            return this;
        }

        @Override
        public char getNumericShortcut() {
            return numericShortcut;
        }

        @Override
        public MenuItem setAlphabeticShortcut(char alphaChar) {
            this.alphaShortcut = alphaChar;
            return this;
        }

        @Override
        public char getAlphabeticShortcut() {
            return alphaShortcut;
        }
    }

    public static final class SimpleSubMenu extends SimpleMenu implements SubMenu {
        private final SimpleMenuItem item;
        private CharSequence headerTitle;

        SimpleSubMenu(Context context, SimpleMenuItem item) {
            super(context);
            this.item = item;
            this.headerTitle = item.getTitle();
        }

        @Override
        public SubMenu setHeaderTitle(int titleRes) {
            if (getContext() != null && titleRes != 0) {
                this.headerTitle = getContext().getText(titleRes);
            }
            return this;
        }

        @Override
        public SubMenu setHeaderTitle(CharSequence title) {
            this.headerTitle = title;
            return this;
        }

        public CharSequence getHeaderTitle() {
            return headerTitle;
        }

        @Override
        public SubMenu setHeaderIcon(int iconRes) {
            return this;
        }

        @Override
        public SubMenu setHeaderIcon(Drawable icon) {
            return this;
        }

        @Override
        public SubMenu setHeaderView(View view) {
            return this;
        }

        @Override
        public void clearHeader() {
            this.headerTitle = null;
        }

        @Override
        public SubMenu setIcon(int iconRes) {
            item.setIcon(iconRes);
            return this;
        }

        @Override
        public SubMenu setIcon(Drawable icon) {
            item.setIcon(icon);
            return this;
        }

        @Override
        public MenuItem getItem() {
            return item;
        }
    }
}
