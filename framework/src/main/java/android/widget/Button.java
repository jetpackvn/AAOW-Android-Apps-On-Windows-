package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import dev.aaow.apk.XmlElement;

public class Button extends TextView {
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean hasExplicitTextColor;

    public Button(Context context) {
        this(context, null, 0);
    }

    public Button(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Button(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setClickable(true);
        if (paddingLeft == 0 && paddingRight == 0) {
            setPadding(18, 10, 18, 10);
        }
        setMinimumHeight(40);
        if (attrs instanceof AttributeSet.XmlAttributeSet xas) {
            if (xas.element.attr("gravity") == null) {
                setGravity(Gravity.CENTER);
            }
            if (xas.element.attr("textColor") != null) {
                hasExplicitTextColor = true;
            }
            if (xas.element.attr("textStyle") == null) {
                setTypeface(Typeface.DEFAULT_BOLD);
            }
        } else {
            setGravity(Gravity.CENTER);
            setTypeface(Typeface.DEFAULT_BOLD);
        }
        if (getBackground() == null && !hasExplicitTextColor && defStyleAttr == 0) {
            this.textColor = Color.WHITE;
        }
    }

    @Override
    public void setTextColor(int color) {
        hasExplicitTextColor = true;
        super.setTextColor(color);
    }

    @Override
    public void setTextColor(android.content.res.ColorStateList colors) {
        if (colors != null) hasExplicitTextColor = true;
        super.setTextColor(colors);
    }

    @Override
    public void draw(Canvas canvas) {
        if (getVisibility() != VISIBLE) return;
        int w = getWidth();
        int h = getHeight();
        int savedTextColor = this.textColor;
        boolean drewDefaultPill = false;
        if (getBackground() == null) {
            float r = Math.min(20f, h / 2f);
            int baseColor = !isEnabled()
                    ? 0xFFBDBDBD
                    : (isPressed() ? 0xFF3949AB : 0xFF4F5BD5);
            bgPaint.setStyle(Paint.Style.FILL);
            bgPaint.setColor(baseColor);
            canvas.drawRoundRect(0, 1, w, h - 1, r, r, bgPaint);
            if (!hasExplicitTextColor) {
                this.textColor = Color.WHITE;
                drewDefaultPill = true;
            }
        }
        try {
            super.draw(canvas);
        } finally {
            if (drewDefaultPill) {
                this.textColor = savedTextColor;
            }
        }
    }
}
