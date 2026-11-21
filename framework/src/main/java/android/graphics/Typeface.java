package android.graphics;

import android.content.res.AssetManager;

import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Typeface {
    public static final int NORMAL = 0;
    public static final int BOLD = 1;
    public static final int ITALIC = 2;
    public static final int BOLD_ITALIC = 3;

    private static final java.util.concurrent.atomic.AtomicLong NEXT_NATIVE_PTR = new java.util.concurrent.atomic.AtomicLong(0x5000_0001L);

    public static final Typeface DEFAULT = new Typeface("Segoe UI", NORMAL);
    public static final Typeface DEFAULT_BOLD = new Typeface("Segoe UI", BOLD);
    public static final Typeface SANS_SERIF = new Typeface("Segoe UI", NORMAL);
    public static final Typeface SERIF = new Typeface("Georgia", NORMAL);
    public static final Typeface MONOSPACE = new Typeface("Consolas", NORMAL);

    private static final Map<String, Typeface> ASSET_CACHE = new ConcurrentHashMap<>();

    public long native_instance = NEXT_NATIVE_PTR.getAndIncrement();
    private long mNativePtr = native_instance;
    private final String family;
    private final int style;
    private final Font customBaseFont;

    public Typeface(String family, int style) {
        this(family, style, null);
    }

    private Typeface(String family, int style, Font customBaseFont) {
        this.family = mapFamily(family);
        this.style = style & 0x3;
        this.customBaseFont = customBaseFont;
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
        if (family == null) return new Typeface("Segoe UI", style);
        return new Typeface(family.family, style, family.customBaseFont);
    }

    public static Typeface create(Typeface family, int weight, boolean italic) {
        int st = (weight >= 600 ? BOLD : NORMAL) | (italic ? ITALIC : NORMAL);
        return create(family, st);
    }

    public static Typeface defaultFromStyle(int style) {
        return new Typeface("Segoe UI", style);
    }

    public static Typeface createFromAsset(AssetManager mgr, String path) {
        if (mgr == null || path == null || path.isEmpty()) return DEFAULT;
        return ASSET_CACHE.computeIfAbsent(System.identityHashCode(mgr) + ":" + path, k -> {
            try (InputStream in = mgr.open(path)) {
                if (in != null) {
                    Font loaded = Font.createFont(Font.TRUETYPE_FONT, in);
                    return new Typeface(loaded.getFamily(), NORMAL, loaded);
                }
            } catch (Throwable ignored) {
            }
            return DEFAULT;
        });
    }

    public static Typeface createFromFile(File file) {
        if (file == null || !file.isFile()) return DEFAULT;
        try (InputStream in = new FileInputStream(file)) {
            Font loaded = Font.createFont(Font.TRUETYPE_FONT, in);
            return new Typeface(loaded.getFamily(), NORMAL, loaded);
        } catch (Throwable ignored) {
            return DEFAULT;
        }
    }

    public static Typeface createFromFile(String path) {
        return path != null ? createFromFile(new File(path)) : DEFAULT;
    }

    public Font toAwtFont(float sizePx) {
        int awtStyle = Font.PLAIN;
        if (isBold()) awtStyle |= Font.BOLD;
        if (isItalic()) awtStyle |= Font.ITALIC;
        float safeSize = Math.max(1f, sizePx);
        if (customBaseFont != null) {
            return customBaseFont.deriveFont(awtStyle, safeSize);
        }
        return new Font(family, awtStyle, Math.max(1, Math.round(safeSize))).deriveFont(safeSize);
    }
}
