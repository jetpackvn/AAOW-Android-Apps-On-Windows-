package android.graphics;

import java.awt.Font;

public class Typeface {
    public static final int NORMAL = 0;
    public static final int BOLD = 1;
    public static final int ITALIC = 2;
    public static final int BOLD_ITALIC = 3;

    public static final Typeface DEFAULT = new Typeface("Segoe UI", NORMAL);
    public static final Typeface DEFAULT_BOLD = new Typeface("Segoe UI", BOLD);
    public static final Typeface SANS_SERIF = new Typeface("Segoe UI", NORMAL);
    public static final Typeface SERIF = new Typeface("Georgia", NORMAL);
    public static final Typeface MONOSPACE = new Typeface("Consolas", NORMAL);

    private final String family;
    private final int style;

    public Typeface(String family, int style) {
        this.family = mapFamily(family);
        this.style = style & 0x3;
    }

    private static String mapFamily(String family) {
        if (family == null) return "Segoe UI";
        String f = family.toLowerCase();
        if (f.contains("mono")) return "Consolas";
        if (f.contains("serif") && !f.contains("sans")) return "Georgia";
        return "Segoe UI";
    }

    public int getStyle() {
        return style;
    }

    public final boolean isBold() {
        return (style & BOLD) != 0;
    }

    public final boolean isItalic() {
        return (style & ITALIC) != 0;
    }

    public static Typeface create(String familyName, int style) {
        return new Typeface(familyName, style);
    }

    public static Typeface create(Typeface family, int style) {
        return new Typeface(family != null ? family.family : "Segoe UI", style);
    }

    public static Typeface defaultFromStyle(int style) {
        return new Typeface("Segoe UI", style);
    }

    public Font toAwtFont(float sizePx) {
        int awtStyle = Font.PLAIN;
        if (isBold()) awtStyle |= Font.BOLD;
        if (isItalic()) awtStyle |= Font.ITALIC;
        return new Font(family, awtStyle, Math.max(1, Math.round(sizePx))).deriveFont(sizePx);
    }
}
