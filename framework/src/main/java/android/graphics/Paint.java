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

    public static class FontMetricsInt {
        public int top;
        public int ascent;
        public int descent;
        public int bottom;
        public int leading;
    }

    private int flags = ANTI_ALIAS_FLAG;
    private int color = Color.BLACK;
    private Style style = Style.FILL;
    private Cap cap = Cap.BUTT;
    private Join join = Join.MITER;
    private float strokeWidth = 1.0f;
    private float textSize = 14.0f;
    private float textScaleX = 1.0f;
    private float textSkewX = 0.0f;
    private float letterSpacing = 0.0f;
    private Align textAlign = Align.LEFT;
    private Typeface typeface = Typeface.DEFAULT;
    private Xfermode xfermode;
    private Shader shader;
    private ColorFilter colorFilter;
    private PathEffect pathEffect;

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
        textScaleX = 1.0f;
        textSkewX = 0.0f;
        letterSpacing = 0.0f;
        textAlign = Align.LEFT;
        typeface = Typeface.DEFAULT;
        xfermode = null;
        shader = null;
        colorFilter = null;
        pathEffect = null;
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
        this.textScaleX = src.textScaleX;
        this.textSkewX = src.textSkewX;
        this.letterSpacing = src.letterSpacing;
        this.textAlign = src.textAlign;
        this.typeface = src.typeface;
        this.xfermode = src.xfermode;
        this.shader = src.shader;
        this.colorFilter = src.colorFilter;
        this.pathEffect = src.pathEffect;
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

    public float getTextScaleX() {
        return textScaleX;
    }

    public void setTextScaleX(float scaleX) {
        this.textScaleX = scaleX;
    }

    public float getTextSkewX() {
        return textSkewX;
    }

    public void setTextSkewX(float skewX) {
        this.textSkewX = skewX;
    }

    public float getLetterSpacing() {
        return letterSpacing;
    }

    public void setLetterSpacing(float letterSpacing) {
        this.letterSpacing = letterSpacing;
    }

    public Shader getShader() {
        return shader;
    }

    public Shader setShader(Shader shader) {
        this.shader = shader;
        return shader;
    }

    public ColorFilter getColorFilter() {
        return colorFilter;
    }

    public ColorFilter setColorFilter(ColorFilter filter) {
        this.colorFilter = filter;
        return filter;
    }

    public PathEffect getPathEffect() {
        return pathEffect;
    }

    public PathEffect setPathEffect(PathEffect effect) {
        this.pathEffect = effect;
        return effect;
    }

    public void setSubpixelText(boolean subpixelText) {
        if (subpixelText) flags |= SUBPIXEL_TEXT_FLAG;
        else flags &= ~SUBPIXEL_TEXT_FLAG;
    }

    public void setLinearText(boolean linearText) {
        if (linearText) flags |= LINEAR_TEXT_FLAG;
        else flags &= ~LINEAR_TEXT_FLAG;
    }

    public void setUnderlineText(boolean underlineText) {
        if (underlineText) flags |= UNDERLINE_TEXT_FLAG;
        else flags &= ~UNDERLINE_TEXT_FLAG;
    }

    public void setStrikeThruText(boolean strikeThruText) {
        if (strikeThruText) flags |= STRIKE_THRU_TEXT_FLAG;
        else flags &= ~STRIKE_THRU_TEXT_FLAG;
    }

    public void setFakeBoldText(boolean fakeBoldText) {
        if (fakeBoldText) flags |= FAKE_BOLD_TEXT_FLAG;
        else flags &= ~FAKE_BOLD_TEXT_FLAG;
    }

    public void getTextBounds(String text, int start, int end, Rect bounds) {
        if (bounds == null) return;
        float w = measureText(text, start, end);
        FontMetrics fm = getFontMetrics();
        bounds.set(0, Math.round(fm.ascent), Math.round(w), Math.round(fm.descent));
    }

    public void getTextBounds(char[] text, int index, int count, Rect bounds) {
        if (text == null) return;
        getTextBounds(new String(text, index, count), 0, count, bounds);
    }

    public int breakText(CharSequence text, int start, int end, boolean measureForwards,
                         float maxWidth, float[] measuredWidth) {
        if (text == null || start >= end) return 0;
        String sub = text.subSequence(start, end).toString();
        int len = sub.length();
        float total = 0f;
        for (int i = 1; i <= len; i++) {
            float w = measureText(sub, 0, i);
            if (w > maxWidth) {
                if (measuredWidth != null && measuredWidth.length > 0) measuredWidth[0] = total;
                return i - 1;
            }
            total = w;
        }
        if (measuredWidth != null && measuredWidth.length > 0) measuredWidth[0] = total;
        return len;
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

    public int getFontMetricsInt(FontMetricsInt fmi) {
        FontMetrics fm = getFontMetrics();
        if (fmi != null) {
            fmi.top = Math.round(fm.top);
            fmi.ascent = Math.round(fm.ascent);
            fmi.descent = Math.round(fm.descent);
            fmi.bottom = Math.round(fm.bottom);
            fmi.leading = Math.round(fm.leading);
        }
        return Math.round(textSize * 1.15f);
    }

    public FontMetricsInt getFontMetricsInt() {
        FontMetricsInt fmi = new FontMetricsInt();
        getFontMetricsInt(fmi);
        return fmi;
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

    public float measureText(CharSequence text, int start, int end) {
        if (text == null || start >= end) return 0f;
        return measureText(text.subSequence(start, end).toString());
    }

    public float measureText(char[] text, int index, int count) {
        if (text == null || count <= 0) return 0f;
        return measureText(new String(text, index, count));
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
        if (pathEffect instanceof DashPathEffect dash) {
            float[] intervals = dash.getIntervals();
            boolean anyPositive = false;
            for (float v : intervals) {
                if (v > 0f) {
                    anyPositive = true;
                    break;
                }
            }
            if (anyPositive) {
                return new BasicStroke(Math.max(1f, strokeWidth), awtCap, awtJoin, 10.0f, intervals, dash.getPhase());
            }
        }
        return new BasicStroke(Math.max(1f, strokeWidth), awtCap, awtJoin);
    }

    public Xfermode getXfermode() {
        return xfermode;
    }

    public Xfermode setXfermode(Xfermode xfermode) {
        this.xfermode = xfermode;
        return xfermode;
    }

    public void setShadowLayer(float radius, float dx, float dy, int shadowColor) {
    }

    public void clearShadowLayer() {
    }
}
