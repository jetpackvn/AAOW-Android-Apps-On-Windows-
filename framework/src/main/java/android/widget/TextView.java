package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import dev.aaow.apk.ResValue;
import dev.aaow.apk.XmlElement;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TextView extends View {
    private static final Graphics2D METRICS_G =
            new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();

    protected CharSequence text = "";
    protected CharSequence hint = "";
    protected int textColor = 0xFF1C1B1F;
    protected int hintTextColor = 0xFF79747E;
    protected float textSizePx = 14f;
    protected Typeface typeface = Typeface.DEFAULT;
    protected int gravity = Gravity.TOP | Gravity.START;
    protected int maxLines = Integer.MAX_VALUE;
    protected boolean singleLine;
    protected boolean allCaps;
    protected final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected final List<TextWatcher> watchers = new CopyOnWriteArrayList<>();

    public TextView(Context context) {
        this(context, null, 0);
    }

    public TextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs instanceof AttributeSet.XmlAttributeSet xas && context != null) {
            applyTextAttributes(xas.element, context.getResources());
        }
    }

    private void applyTextAttributes(XmlElement el, Resources res) {
        int textStyle = Typeface.NORMAL;
        String fontFamily = null;
        for (XmlElement.Attribute a : el.attributes) {
            if (!XmlElement.ANDROID_NS.equals(a.namespace)) continue;
            switch (a.name) {
                case "text" -> {
                    String s = res.resolveString(a.value);
                    if (s != null) this.text = s;
                }
                case "hint" -> {
                    String s = res.resolveString(a.value);
                    if (s != null) this.hint = s;
                }
                case "textSize" -> this.textSizePx = res.resolveDimension(a.value, 14f);
                case "textColor" -> this.textColor = res.resolveColor(a.value, this.textColor);
                case "textColorHint" -> this.hintTextColor = res.resolveColor(a.value, this.hintTextColor);
                case "textStyle" -> {
                    ResValue rv = res.getTable().resolve(a.value);
                    if (rv != null) textStyle = rv.data();
                }
                case "fontFamily", "typeface" -> fontFamily = res.resolveString(a.value);
                case "gravity" -> {
                    ResValue rv = res.getTable().resolve(a.value);
                    if (rv != null) this.gravity = rv.data();
                }
                case "maxLines" -> {
                    ResValue rv = res.getTable().resolve(a.value);
                    if (rv != null && rv.data() > 0) this.maxLines = rv.data();
                }
                case "singleLine" -> {
                    ResValue rv = res.getTable().resolve(a.value);
                    if (rv != null && rv.data() != 0) {
                        this.singleLine = true;
                        this.maxLines = 1;
                    }
                }
                case "textAllCaps" -> {
                    ResValue rv = res.getTable().resolve(a.value);
                    if (rv != null) this.allCaps = rv.data() != 0;
                }
            }
        }
        if (fontFamily != null || textStyle != Typeface.NORMAL) {
            this.typeface = Typeface.create(fontFamily, textStyle);
        }
    }

    public CharSequence getText() {
        return text;
    }

    public void setText(CharSequence text) {
        CharSequence newText = text != null ? text : "";
        CharSequence oldText = this.text;
        for (TextWatcher w : watchers) {
            w.beforeTextChanged(oldText, 0, oldText.length(), newText.length());
        }
        this.text = newText;
        for (TextWatcher w : watchers) {
            w.onTextChanged(newText, 0, oldText.length(), newText.length());
            w.afterTextChanged(new Editable.SimpleEditable(newText));
        }
        requestLayout();
        invalidate();
    }

    public final void setText(int resid) {
        if (getContext() != null) {
            setText(getContext().getResources().getText(resid));
        }
    }

    public CharSequence getHint() {
        return hint;
    }

    public void setHint(CharSequence hint) {
        this.hint = hint != null ? hint : "";
        requestLayout();
        invalidate();
    }

    public final void setHint(int resid) {
        if (getContext() != null) {
            setHint(getContext().getResources().getText(resid));
        }
    }

    public int getCurrentTextColor() {
        return textColor;
    }

    public void setTextColor(int color) {
        this.textColor = color;
        invalidate();
    }

    public void setTextColor(ColorStateList colors) {
        if (colors != null) setTextColor(colors.getDefaultColor());
    }

    public void setHintTextColor(int color) {
        this.hintTextColor = color;
        invalidate();
    }

    public float getTextSize() {
        return textSizePx;
    }

    public void setTextSize(float sizeSp) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
    }

    public void setTextSize(int unit, float size) {
        DisplayMetrics dm = getResources() != null ? getResources().getDisplayMetrics() : new DisplayMetrics();
        this.textSizePx = Math.max(1f, TypedValue.applyDimension(unit, size, dm));
        requestLayout();
        invalidate();
    }

    public Typeface getTypeface() {
        return typeface;
    }

    public void setTypeface(Typeface tf) {
        this.typeface = tf != null ? tf : Typeface.DEFAULT;
        requestLayout();
        invalidate();
    }

    public void setTypeface(Typeface tf, int style) {
        this.typeface = Typeface.create(tf, style);
        requestLayout();
        invalidate();
    }

    public int getGravity() {
        return gravity;
    }

    public void setGravity(int gravity) {
        this.gravity = gravity;
        invalidate();
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = Math.max(1, maxLines);
        requestLayout();
        invalidate();
    }

    public int getMaxLines() {
        return maxLines;
    }

    public void setSingleLine(boolean singleLine) {
        this.singleLine = singleLine;
        if (singleLine) this.maxLines = 1;
        requestLayout();
        invalidate();
    }

    public void setAllCaps(boolean allCaps) {
        this.allCaps = allCaps;
        requestLayout();
        invalidate();
    }

    public void addTextChangedListener(TextWatcher watcher) {
        if (watcher != null) watchers.add(watcher);
    }

    public void removeTextChangedListener(TextWatcher watcher) {
        watchers.remove(watcher);
    }

    public int length() {
        return text != null ? text.length() : 0;
    }

    public void append(CharSequence text) {
        if (text != null) {
            setText(this.text.toString() + text);
        }
    }

    protected String displayString() {
        String s = (text != null && text.length() > 0) ? text.toString() : (hint != null ? hint.toString() : "");
        return allCaps ? s.toUpperCase() : s;
    }

    protected List<String> wrapLines(FontMetrics fm, int maxWidth) {
        String raw = displayString();
        List<String> out = new ArrayList<>();
        if (raw.isEmpty()) {
            out.add("");
            return out;
        }
        String[] paragraphs = raw.split("\n", -1);
        for (String para : paragraphs) {
            if (singleLine || maxWidth <= 0 || fm.stringWidth(para) <= maxWidth) {
                out.add(para);
            } else {
                String[] words = para.split(" ");
                StringBuilder line = new StringBuilder();
                for (String w : words) {
                    if (line.isEmpty()) {
                        line.append(w);
                    } else if (fm.stringWidth(line + " " + w) <= maxWidth) {
                        line.append(' ').append(w);
                    } else {
                        out.add(line.toString());
                        line.setLength(0);
                        line.append(w);
                        if (out.size() >= maxLines) break;
                    }
                }
                if (!line.isEmpty() && out.size() < maxLines) {
                    out.add(line.toString());
                }
            }
            if (out.size() >= maxLines) break;
        }
        return out;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Font font = typeface.toAwtFont(textSizePx);
        FontMetrics fm = METRICS_G.getFontMetrics(font);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int availContentW = widthMode == MeasureSpec.UNSPECIFIED
                ? Integer.MAX_VALUE
                : Math.max(0, widthSize - paddingLeft - paddingRight);

        List<String> lines = wrapLines(fm, availContentW);
        int maxLineW = 0;
        for (String line : lines) {
            maxLineW = Math.max(maxLineW, fm.stringWidth(line));
        }

        int lineHeight = Math.max((int) (textSizePx * 1.28f), fm.getHeight());
        int desiredW = Math.max(getSuggestedMinimumWidth(), maxLineW + paddingLeft + paddingRight);
        int desiredH = Math.max(getSuggestedMinimumHeight(), lines.size() * lineHeight + paddingTop + paddingBottom);

        setMeasuredDimension(
                resolveSize(desiredW, widthMeasureSpec),
                resolveSize(desiredH, heightMeasureSpec)
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Graphics2D g = canvas.getAwtGraphics();
        if (g == null) return;

        boolean showingHint = (text == null || text.length() == 0) && hint != null && hint.length() > 0;
        int col = showingHint ? hintTextColor : (isEnabled() ? textColor : Color.argb(110, Color.red(textColor), Color.green(textColor), Color.blue(textColor)));

        Font font = typeface.toAwtFont(textSizePx);
        g.setFont(font);
        g.setColor(Color.toAwt(col));
        FontMetrics fm = g.getFontMetrics(font);

        int contentW = Math.max(0, getWidth() - paddingLeft - paddingRight);
        int contentH = Math.max(0, getHeight() - paddingTop - paddingBottom);
        List<String> lines = wrapLines(fm, contentW);
        int lineHeight = Math.max((int) (textSizePx * 1.28f), fm.getHeight());
        int totalTextH = lines.size() * lineHeight;

        int vg = gravity & Gravity.VERTICAL_GRAVITY_MASK;
        int startY = switch (vg) {
            case Gravity.CENTER_VERTICAL -> paddingTop + (contentH - totalTextH) / 2 + fm.getAscent();
            case Gravity.BOTTOM -> getHeight() - paddingBottom - totalTextH + fm.getAscent();
            default -> paddingTop + fm.getAscent();
        };

        int hg = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lw = fm.stringWidth(line);
            int drawX = switch (hg) {
                case Gravity.CENTER_HORIZONTAL -> paddingLeft + (contentW - lw) / 2;
                case Gravity.RIGHT -> getWidth() - paddingRight - lw;
                default -> paddingLeft;
            };
            g.drawString(line, drawX, startY + i * lineHeight);
        }
    }
}
