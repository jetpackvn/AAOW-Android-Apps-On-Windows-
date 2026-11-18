package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import dev.aaow.apk.XmlElement;

import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Thanh công cụ android.widget.Toolbar cơ bản dùng cho các ứng dụng Android trên Windows.
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
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Toolbar(Context context) {
        this(context, null, 0);
    }

    public Toolbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Toolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setMinimumHeight(56);
        if (attrs instanceof AttributeSet.XmlAttributeSet xas && context != null) {
            XmlElement.Attribute t = xas.element.attr("title");
            if (t != null) {
                String s = context.getResources().resolveString(t.value);
                if (s != null) this.title = s;
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

    public void inflateMenu(int resId) {
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
            g.drawString(title.toString(), startX, getHeight() / 2 + 6);
        }
    }
}
