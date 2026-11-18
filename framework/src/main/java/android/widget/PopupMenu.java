package android.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import dev.aaow.rt.SimpleMenu;

public class PopupMenu {
    public interface OnMenuItemClickListener {
        boolean onMenuItemClick(MenuItem item);
    }

    public interface OnDismissListener {
        void onDismiss(PopupMenu menu);
    }

    private final Context context;
    private final View anchor;
    private final SimpleMenu menu;
    private int gravity = Gravity.NO_GRAVITY;
    private OnMenuItemClickListener menuItemClickListener;
    private OnDismissListener onDismissListener;
    private PopupWindow popupWindow;

    public PopupMenu(Context context, View anchor) {
        this(context, anchor, Gravity.NO_GRAVITY);
    }

    public PopupMenu(Context context, View anchor, int gravity) {
        this(context, anchor, gravity, 0, 0);
    }

    public PopupMenu(Context context, View anchor, int gravity, int popupStyleAttr, int popupStyleRes) {
        this.context = context;
        this.anchor = anchor;
        this.gravity = gravity;
        this.menu = new SimpleMenu(context);
    }

    public Menu getMenu() {
        return menu;
    }

    public MenuInflater getMenuInflater() {
        return new MenuInflater(context);
    }

    public void inflate(int menuRes) {
        getMenuInflater().inflate(menuRes, menu);
    }

    public void setGravity(int gravity) {
        this.gravity = gravity;
    }

    public int getGravity() {
        return gravity;
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        this.menuItemClickListener = listener;
    }

    public void setOnDismissListener(OnDismissListener listener) {
        this.onDismissListener = listener;
    }

    public void setForceShowIcon(boolean forceShowIcon) {
    }

    public void show() {
        if (context == null) return;
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(6, 6, 6, 6);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(10f);
        bg.setStroke(1, 0xFFD0D5E4);
        box.setBackground(bg);

        for (SimpleMenu.SimpleMenuItem item : menu.getVisibleItems()) {
            TextView tv = new TextView(context);
            tv.setText(item.getTitle());
            tv.setTextSize(14f);
            tv.setTextColor(item.isEnabled() ? 0xFF1A1C29 : 0xFF9CA3AF);
            tv.setPadding(14, 10, 18, 10);
            tv.setClickable(true);
            tv.setOnClickListener(v -> {
                dismiss();
                if (!item.invokeClick() && menuItemClickListener != null) {
                    menuItemClickListener.onMenuItemClick(item);
                }
            });
            box.addView(tv, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        popupWindow = new PopupWindow(box, 200, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setOnDismissListener(() -> {
            if (onDismissListener != null) onDismissListener.onDismiss(PopupMenu.this);
        });
        popupWindow.showAsDropDown(anchor);
    }

    public void dismiss() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }
}
