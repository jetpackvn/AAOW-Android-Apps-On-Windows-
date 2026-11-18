package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import dev.aaow.apk.XmlElement;
import dev.aaow.rt.SimpleMenu;

import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Thanh công cụ android.widget.Toolbar có hỗ trợ menu, icon điều hướng và tương tác click.
 */
public class Toolbar extends ViewGroup {
    public interface OnMenuItemClickListener {
        boolean onMenuItemClick(MenuItem item);
    }

    private CharSequence title = "";
    private CharSequence subtitle = "";
    private Drawable navigationIcon;
    private OnClickListener navigationListener;
    private OnMenuItemClickListener menuItemClickListener;
    private int titleTextColor = 0xFFFFFFFF;
    private int subtitleTextColor = 0xDDEEEEEE;
    private SimpleMenu menu;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Toolbar(Context context) {
        this(context, null, 0);
    }

    public Toolbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Toolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.menu = new SimpleMenu(context);
        setMinimumHeight(56);
        setClickable(true);
        if (attrs instanceof AttributeSet.XmlAttributeSet xas && context != null) {
            XmlElement.Attribute t = xas.element.attr("title");
            if (t != null) {
                String s = context.getResources().resolveString(t.value);
                if (s != null) this.title = s;
            }
            XmlElement.Attribute st = xas.element.attr("subtitle");
            if (st != null) {
                String s = context.getResources().resolveString(st.value);
                if (s != null) this.subtitle = s;
            }
        }
    }

    public CharSequence getTitle() {
        return title;
    }

    public void setTitle(CharSequence title) {
        this.title = title != null ? title : "";
        invalidate();
    }

    public void setTitle(int resId) {
        if (getContext() != null) setTitle(getContext().getText(resId));
    }

    public CharSequence getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(CharSequence subtitle) {
        this.subtitle = subtitle != null ? subtitle : "";
        invalidate();
    }

    public void setSubtitle(int resId) {
        if (getContext() != null) setSubtitle(getContext().getText(resId));
    }

    public void setTitleTextColor(int color) {
        this.titleTextColor = color;
        invalidate();
    }

    public void setSubtitleTextColor(int color) {
        this.subtitleTextColor = color;
        invalidate();
    }

    public void setNavigationIcon(int resId) {
        if (getResources() != null) {
            setNavigationIcon(getResources().getDrawable(resId));
        }
    }

    public void setNavigationIcon(Drawable icon) {
        this.navigationIcon = icon;
        invalidate();
    }

    public Drawable getNavigationIcon() {
        return navigationIcon;
    }

    public void setNavigationContentDescription(int resId) {
    }

    public void setNavigationContentDescription(CharSequence description) {
    }

    public void setNavigationOnClickListener(OnClickListener listener) {
        this.navigationListener = listener;
        setClickable(true);
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        this.menuItemClickListener = listener;
    }

    public Menu getMenu() {
        if (menu == null) menu = new SimpleMenu(getContext());
        return menu;
    }

    public void inflateMenu(int resId) {
        if (getContext() != null && resId != 0) {
            new MenuInflater(getContext()).inflate(resId, getMenu());
            invalidate();
        }
    }

    public void setOverflowIcon(Drawable icon) {
    }

    public void collapseActionView() {
    }

    public boolean dispatchMenuItemClick(MenuItem item) {
        if (item instanceof SimpleMenu.SimpleMenuItem smi && smi.invokeClick()) return true;
        return menuItemClickListener != null && menuItemClickListener.onMenuItemClick(item);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int h = Math.max(56, getMinimumHeight());
        int w = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED
                ? 400
                : MeasureSpec.getSize(widthMeasureSpec);
        for (View child : children) {
            if (child.getVisibility() != GONE) {
                measureChild(child, widthMeasureSpec, MeasureSpec.makeMeasureSpec(h, MeasureSpec.AT_MOST));
            }
        }
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int x = paddingLeft + (navigationIcon != null ? 44 : 16);
        for (View child : children) {
            if (child.getVisibility() == GONE) continue;
            int cw = child.getMeasuredWidth();
            int ch = child.getMeasuredHeight();
            int cy = (getHeight() - ch) / 2;
            child.layout(x, cy, x + cw, cy + ch);
            x += cw + 8;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            if (navigationIcon != null && x < 52 && navigationListener != null) {
                navigationListener.onClick(this);
                return true;
            }
            List<SimpleMenu.SimpleMenuItem> visible = menu != null ? menu.getVisibleItems() : List.of();
            if (!visible.isEmpty() && x > getWidth() - 48) {
                PopupMenu pm = new PopupMenu(getContext(), this);
                for (SimpleMenu.SimpleMenuItem item : visible) {
                    MenuItem copied = pm.getMenu().add(item.getGroupId(), item.getItemId(), item.getOrder(), item.getTitle());
                    copied.setEnabled(item.isEnabled());
                    copied.setIcon(item.getIcon());
                }
                pm.setOnMenuItemClickListener(clicked -> {
                    MenuItem orig = menu.findItem(clicked.getItemId());
                    return dispatchMenuItemClick(orig != null ? orig : clicked);
                });
                pm.show();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Graphics2D g = canvas.getAwtGraphics();
        if (g == null) return;
        int startX = paddingLeft + 16;
        if (navigationIcon != null) {
            int iy = (getHeight() - 24) / 2;
            navigationIcon.setBounds(startX, iy, startX + 24, iy + 24);
            navigationIcon.draw(canvas);
            startX += 36;
        }
        if (title != null && title.length() > 0 && children.isEmpty()) {
            g.setColor(Color.toAwt(titleTextColor));
            g.setFont(new Font("Segoe UI", Font.BOLD, 17));
            int ty = (subtitle != null && subtitle.length() > 0) ? getHeight() / 2 - 2 : getHeight() / 2 + 6;
            g.drawString(title.toString(), startX, ty);
            if (subtitle != null && subtitle.length() > 0) {
                g.setColor(Color.toAwt(subtitleTextColor));
                g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g.drawString(subtitle.toString(), startX, getHeight() / 2 + 15);
            }
        }
        if (menu != null && menu.hasVisibleItems()) {
            int rx = getWidth() - 22;
            int cy = getHeight() / 2;
            g.setColor(Color.toAwt(titleTextColor));
            g.fillOval(rx - 2, cy - 8, 4, 4);
            g.fillOval(rx - 2, cy - 2, 4, 4);
            g.fillOval(rx - 2, cy + 4, 4, 4);
        }
    }
}
