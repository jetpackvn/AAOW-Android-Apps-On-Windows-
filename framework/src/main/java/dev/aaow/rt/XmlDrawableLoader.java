package dev.aaow.rt;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import dev.aaow.apk.ResValue;
import dev.aaow.apk.XmlElement;

/**
 * Chuyển các thẻ XML drawable (<shape>, <vector>, <adaptive-icon>, <selector>, <layer-list>)
 * thành Drawable vẽ được bằng Java2D.
 */
public final class XmlDrawableLoader {
    private XmlDrawableLoader() {
    }

    public static Drawable fromXml(XmlElement root, Resources res) {
        return switch (root.name) {
            case "shape" -> parseShape(root, res);
            case "vector" -> parseVector(root, res);
            case "color" -> new ColorDrawable(colorAttr(root, "color", res, Color.TRANSPARENT));
            case "adaptive-icon" -> parseAdaptiveIcon(root, res);
            case "selector", "ripple", "inset" -> {
                for (XmlElement item : root.children) {
                    XmlElement.Attribute dAttr = item.attr("drawable");
                    if (dAttr != null && dAttr.value.isReference()) {
                        Drawable d = res.getDrawable(dAttr.value.data());
                        if (d != null) yield d;
                    }
                    if (!item.children.isEmpty()) {
                        Drawable d = fromXml(item.children.get(0), res);
                        if (d != null) yield d;
                    }
                }
                yield new ColorDrawable(Color.TRANSPARENT);
            }
            default -> new ColorDrawable(Color.TRANSPARENT);
        };
    }

    private static Drawable parseShape(XmlElement root, Resources res) {
        GradientDrawable gd = new GradientDrawable();
        XmlElement.Attribute shapeAttr = root.attr("shape");
        if (shapeAttr != null) {
            gd.setShape(shapeAttr.value.data());
        }
        for (XmlElement c : root.children) {
            switch (c.name) {
                case "solid" -> gd.setColor(colorAttr(c, "color", res, Color.TRANSPARENT));
                case "corners" -> gd.setCornerRadius(dimenAttr(c, "radius", res, 0f));
                case "stroke" -> {
                    int w = Math.round(dimenAttr(c, "width", res, 0f));
                    int col = colorAttr(c, "color", res, Color.TRANSPARENT);
                    gd.setStroke(w, col);
                }
                case "gradient" -> {
                    int start = colorAttr(c, "startColor", res, 0);
                    int end = colorAttr(c, "endColor", res, 0);
                    if (start != 0 || end != 0) {
                        gd.setColors(new int[]{start, end});
                    }
                }
            }
        }
        return gd;
    }

    private static Drawable parseVector(XmlElement root, Resources res) {
        float vw = floatAttr(root, "viewportWidth", res, 24f);
        float vh = floatAttr(root, "viewportHeight", res, 24f);
        int w = Math.round(dimenAttr(root, "width", res, 24f));
        int h = Math.round(dimenAttr(root, "height", res, 24f));
        int tint = colorAttr(root, "tint", res, 0);
        VectorDrawable vd = new VectorDrawable(vw, vh, w, h, tint);
        collectVectorPaths(root, vd, res);
        return vd;
    }

    private static void collectVectorPaths(XmlElement parent, VectorDrawable vd, Resources res) {
        for (XmlElement c : parent.children) {
            if ("path".equals(c.name)) {
                XmlElement.Attribute pd = c.attr("pathData");
                String pathData = pd != null ? res.resolveString(pd.value) : null;
                int fill = colorAttr(c, "fillColor", res, Color.TRANSPARENT);
                int stroke = colorAttr(c, "strokeColor", res, Color.TRANSPARENT);
                float sw = floatAttr(c, "strokeWidth", res, 0f);
                vd.addPath(pathData, fill, stroke, sw);
            } else if ("group".equals(c.name)) {
                collectVectorPaths(c, vd, res);
            }
        }
    }

    private static Drawable parseAdaptiveIcon(XmlElement root, Resources res) {
        Drawable bg = null, fg = null;
        for (XmlElement c : root.children) {
            XmlElement.Attribute d = c.attr("drawable");
            Drawable resolved = d != null && d.value.isReference() ? res.getDrawable(d.value.data()) : null;
            if ("background".equals(c.name)) bg = resolved;
            else if ("foreground".equals(c.name)) fg = resolved;
        }
        final Drawable fBg = bg, fFg = fg;
        return new Drawable() {
            @Override
            public int getIntrinsicWidth() {
                return 48;
            }

            @Override
            public int getIntrinsicHeight() {
                return 48;
            }

            @Override
            public void draw(Canvas canvas) {
                Rect b = getBounds();
                if (fBg != null) {
                    fBg.setBounds(b);
                    fBg.draw(canvas);
                }
                if (fFg != null) {
                    fFg.setBounds(b);
                    fFg.draw(canvas);
                }
            }
        };
    }

    private static int colorAttr(XmlElement el, String name, Resources res, int def) {
        XmlElement.Attribute a = el.attr(name);
        if (a == null) return def;
        return res.resolveColor(a.value, def);
    }

    private static float dimenAttr(XmlElement el, String name, Resources res, float def) {
        XmlElement.Attribute a = el.attr(name);
        if (a == null) return def;
        return res.resolveDimension(a.value, def);
    }

    private static float floatAttr(XmlElement el, String name, Resources res, float def) {
        XmlElement.Attribute a = el.attr(name);
        if (a == null) return def;
        ResValue v = res.getTable().resolve(a.value);
        if (v == null) return def;
        if (v.type() == ResValue.TYPE_FLOAT) return v.getFloat();
        if (v.isDimension()) return v.complexValue();
        if (v.isInt()) return v.data();
        try {
            return Float.parseFloat(v.coerceToString());
        } catch (Exception e) {
            return def;
        }
    }
}
