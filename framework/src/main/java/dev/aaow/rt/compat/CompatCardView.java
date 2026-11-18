package dev.aaow.rt.compat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import dev.aaow.apk.XmlElement;

/**
 * Container tương thích CardView / MaterialCardView cho giao diện trên Windows.
 */
public class CompatCardView extends FrameLayout {
    private int cardBgColor = Color.WHITE;
    private float cardCornerRadius = 12f;

    public CompatCardView(Context context) {
        this(context, null, 0);
    }

    public CompatCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CompatCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs instanceof AttributeSet.XmlAttributeSet xas && context != null) {
            var res = context.getResources();
            for (XmlElement.Attribute a : xas.element.attributes) {
                switch (a.name) {
                    case "cardBackgroundColor" -> cardBgColor = res.resolveColor(a.value, cardBgColor);
                    case "cardCornerRadius" -> cardCornerRadius = res.resolveDimension(a.value, cardCornerRadius);
                    case "contentPadding" -> {
                        int p = Math.round(res.resolveDimension(a.value, 0f));
                        setPadding(p, p, p, p);
                    }
                    case "contentPaddingLeft" -> setPadding(Math.round(res.resolveDimension(a.value, 0f)),
                            getPaddingTop(), getPaddingRight(), getPaddingBottom());
                    case "contentPaddingTop" -> setPadding(getPaddingLeft(),
                            Math.round(res.resolveDimension(a.value, 0f)), getPaddingRight(), getPaddingBottom());
                    case "contentPaddingRight" -> setPadding(getPaddingLeft(), getPaddingTop(),
                            Math.round(res.resolveDimension(a.value, 0f)), getPaddingBottom());
                    case "contentPaddingBottom" -> setPadding(getPaddingLeft(), getPaddingTop(),
                            getPaddingRight(), Math.round(res.resolveDimension(a.value, 0f)));
                }
            }
        }
        applyCardStyle();
    }

    private void applyCardStyle() {
        if (getBackground() == null) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(cardBgColor);
            bg.setCornerRadius(cardCornerRadius);
            bg.setStroke(1, 0xFFE2E6F0);
            setBackground(bg);
        }
    }

    public void setCardBackgroundColor(int color) {
        this.cardBgColor = color;
        setBackground(null);
        applyCardStyle();
    }

    public void setRadius(float radius) {
        this.cardCornerRadius = radius;
        setBackground(null);
        applyCardStyle();
    }
}
