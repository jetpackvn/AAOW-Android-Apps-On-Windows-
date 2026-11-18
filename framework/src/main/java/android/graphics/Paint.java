package android.graphics;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Stroke;

public class Paint {
    public static final int ANTI_ALIAS_FLAG = 0x01;
    public static final int FILTER_BITMAP_FLAG = 0x02;
    public static final int DITHER_FLAG = 0x04;
    public static final int UNDERLINE_TEXT_FLAG = 0x08;
    public static final int STRIKE_THRU_TEXT_FLAG = 0x10;
    public static final int FAKE_BOLD_TEXT_FLAG = 0x20;
    public static final int LINEAR_TEXT_FLAG = 0x40;
    public static final int SUBPIXEL_TEXT_FLAG = 0x80;

    public enum Style {
        FILL,
        STROKE,
        FILL_AND_STROKE
    }

    public enum Cap {
        BUTT,
        ROUND,
        SQUARE
    }

    public enum Join {
        MITER,
        ROUND,
        BEVEL
    }

    public enum Align {
        LEFT,
        CENTER,
        RIGHT
    }

    public static class FontMetrics {
        public float top;
        public float ascent;
        public float descent;
        public float bottom;
        public float leading;
    }

    private int flags = ANTI_ALIAS_FLAG;
    private int color = Color.BLACK;
    private Style style = Style.FILL;
    private Cap cap = Cap.BUTT;
    private Join join = Join.MITER;
    private float strokeWidth = 1.0f;
    private float textSize = 14.0f;
    private Align textAlign = Align.LEFT;
    private Typeface typeface = Typeface.DEFAULT;

    public Paint() {
    }

    public Paint(int flags) {
        this.flags = flags;
    }

    public Paint(Paint paint) {
        set(paint);
    }

    public void reset() {
        flags = ANTI_ALIAS_FLAG;
        color = Color.BLACK;
        style = Style.FILL;
        cap = Cap.BUTT;
        join = Join.MITER;
        strokeWidth = 1.0f;
        textSize = 14.0f;
        textAlign = Align.LEFT;
        typeface = Typeface.DEFAULT;
    }

    public void set(Paint src) {
        if (src == null) return;
        this.flags = src.flags;
        this.color = src.color;
        this.style = src.style;
        this.cap = src.cap;
        this.join = src.join;
        this.strokeWidth = src.strokeWidth;
        this.textSize = src.textSize;
        this.textAlign = src.textAlign;
        this.typeface = src.typeface;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public final boolean isAntiAlias() {
        return (flags & ANTI_ALIAS_FLAG) != 0;
    }

    public void setAntiAlias(boolean aa) {
        if (aa) flags |= ANTI_ALIAS_FLAG;
        else flags &= ~ANTI_ALIAS_FLAG;
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle(Style style) {
        if (style != null) this.style = style;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getAlpha() {
        return Color.alpha(color);
    }

    public void setAlpha(int a) {
        color = ((a & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    public void setARGB(int a, int r, int g, int b) {
        setColor(Color.argb(a, r, g, b));
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float width) {
        this.strokeWidth = Math.max(0f, width);
    }

    public Cap getStrokeCap() {
        return cap;
    }

    public void setStrokeCap(Cap cap) {
        if (cap != null) this.cap = cap;
    }

    public Join getStrokeJoin() {
        return join;
    }

    public void setStrokeJoin(Join join) {
        if (join != null) this.join = join;
    }

    public Align getTextAlign() {
        return textAlign;
    }

    public void setTextAlign(Align align) {
        if (align != null) this.textAlign = align;
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = Math.max(1f, textSize);
    }

    public Typeface getTypeface() {
        return typeface;
    }

    public Typeface setTypeface(Typeface typeface) {
        this.typeface = typeface != null ? typeface : Typeface.DEFAULT;
        return this.typeface;
    }

    public float ascent() {
        return -textSize * 0.8f;
    }

    public float descent() {
        return textSize * 0.2f;
    }

    public float getFontMetrics(FontMetrics metrics) {
        if (metrics != null) {
            metrics.top = -textSize * 0.95f;
            metrics.ascent = -textSize * 0.8f;
            metrics.descent = textSize * 0.2f;
            metrics.bottom = textSize * 0.28f;
            metrics.leading = 0f;
        }
        return textSize * 1.15f;
    }

    public FontMetrics getFontMetrics() {
        FontMetrics fm = new FontMetrics();
        getFontMetrics(fm);
        return fm;
    }

    public float measureText(String text) {
        if (text == null || text.isEmpty()) return 0f;
        return measureText(text, 0, text.length());
    }

    public float measureText(String text, int start, int end) {
        if (text == null || start >= end) return 0f;
        Font f = toAwtFont();
        java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, true, true);
        return (float) f.getStringBounds(text.substring(start, end), frc).getWidth();
    }

    public Font toAwtFont() {
        int extraStyle = (flags & FAKE_BOLD_TEXT_FLAG) != 0 ? Typeface.BOLD : 0;
        Typeface tf = extraStyle != 0
                ? Typeface.create(typeface, typeface.getStyle() | extraStyle)
                : typeface;
        return tf.toAwtFont(textSize);
    }

    public Stroke toAwtStroke() {
        int awtCap = switch (cap) {
            case ROUND -> BasicStroke.CAP_ROUND;
            case SQUARE -> BasicStroke.CAP_SQUARE;
            default -> BasicStroke.CAP_BUTT;
        };
        int awtJoin = switch (join) {
            case ROUND -> BasicStroke.JOIN_ROUND;
            case BEVEL -> BasicStroke.JOIN_BEVEL;
            default -> BasicStroke.JOIN_MITER;
        };
        return new BasicStroke(Math.max(1f, strokeWidth), awtCap, awtJoin);
    }
}
