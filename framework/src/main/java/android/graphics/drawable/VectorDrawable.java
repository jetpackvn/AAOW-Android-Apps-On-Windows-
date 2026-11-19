package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Rect;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cài đặt android.graphics.drawable.VectorDrawable vẽ &lt;vector&gt; drawable của Android
 * (SVG pathData đầy đủ M/L/H/V/C/S/Q/T/A/Z) bằng Java2D, hỗ trợ tint/ColorFilter & ConstantState.
 */
public class VectorDrawable extends Drawable {
    public record PathSpec(Path2D.Float path, int fillColor, int strokeColor, float strokeWidth) {
    }

    private static final class CachedRenderSpec {
        final PathSpec spec;
        final java.awt.Color fillAwt;
        final java.awt.Color strokeAwt;
        final BasicStroke stroke;

        CachedRenderSpec(PathSpec spec) {
            this.spec = spec;
            this.fillAwt = (spec.fillColor >>> 24) != 0 ? Color.toAwt(spec.fillColor) : null;
            this.strokeAwt = (spec.strokeColor >>> 24) != 0 ? Color.toAwt(spec.strokeColor) : null;
            this.stroke = (spec.strokeWidth > 0 && strokeAwt != null)
                    ? new BasicStroke(spec.strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                    : null;
        }
    }

    private final float viewportWidth;
    private final float viewportHeight;
    private final int intrinsicWidth;
    private final int intrinsicHeight;
    private final int tintColor;
    private final java.awt.Color tintAwt;
    private final List<PathSpec> paths;
    private final List<CachedRenderSpec> renderSpecs;

    public VectorDrawable() {
        this(24f, 24f, 24, 24, 0);
    }

    public VectorDrawable(float viewportWidth, float viewportHeight, int intrinsicWidth, int intrinsicHeight, int tintColor) {
        this.viewportWidth = viewportWidth > 0 ? viewportWidth : 24f;
        this.viewportHeight = viewportHeight > 0 ? viewportHeight : 24f;
        this.intrinsicWidth = intrinsicWidth > 0 ? intrinsicWidth : 24;
        this.intrinsicHeight = intrinsicHeight > 0 ? intrinsicHeight : 24;
        this.tintColor = tintColor;
        this.tintAwt = tintColor != 0 ? Color.toAwt(tintColor) : null;
        this.paths = new ArrayList<>();
        this.renderSpecs = new ArrayList<>();
    }

    private VectorDrawable(VectorDrawable src) {
        this.viewportWidth = src.viewportWidth;
        this.viewportHeight = src.viewportHeight;
        this.intrinsicWidth = src.intrinsicWidth;
        this.intrinsicHeight = src.intrinsicHeight;
        this.tintColor = src.tintColor;
        this.tintAwt = src.tintAwt;
        this.paths = Collections.unmodifiableList(new ArrayList<>(src.paths));
        this.renderSpecs = Collections.unmodifiableList(new ArrayList<>(src.renderSpecs));
    }

    public void addPath(String pathData, int fillColor, int strokeColor, float strokeWidth) {
        addPath(pathData, fillColor, strokeColor, strokeWidth, false);
    }

    public void addPath(String pathData, int fillColor, int strokeColor, float strokeWidth, boolean evenOdd) {
        if (pathData == null || pathData.isEmpty()) return;
        Path2D.Float p = parseSvgPath(pathData);
        if (evenOdd) {
            p.setWindingRule(Path2D.WIND_EVEN_ODD);
        }
        PathSpec spec = new PathSpec(p, fillColor, strokeColor, strokeWidth);
        paths.add(spec);
        renderSpecs.add(new CachedRenderSpec(spec));
    }

    @Override
    public int getIntrinsicWidth() {
        return intrinsicWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return intrinsicHeight;
    }

    private java.awt.Color resolveDynamicColor(int origColor, java.awt.Color origAwt) {
        ColorFilter cf = getColorFilter();
        ColorStateList tl = getTintListInternal();
        if (cf != null) {
            return Color.toAwt(Canvas.applyColorFilter(origColor, cf));
        }
        if (tl != null) {
            int tc = tl.getColorForState(getState(), tl.getDefaultColor());
            int a = (Color.alpha(origColor) * Color.alpha(tc)) / 255;
            return Color.toAwt((tc & 0x00FFFFFF) | (a << 24));
        }
        return tintAwt != null ? tintAwt : origAwt;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        Graphics2D g = canvas.getAwtGraphics();
        if (g == null) return;
        AffineTransform old = g.getTransform();
        Composite oldComp = null;
        int alpha = getAlpha();
        if (alpha < 255) {
            oldComp = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha / 255f))));
        }
        g.translate(b.left, b.top);
        g.scale(b.width() / viewportWidth, b.height() / viewportHeight);
        for (int i = 0, n = renderSpecs.size(); i < n; i++) {
            CachedRenderSpec rs = renderSpecs.get(i);
            if (rs.fillAwt != null) {
                g.setColor(resolveDynamicColor(rs.spec.fillColor, rs.fillAwt));
                g.fill(rs.spec.path);
            }
            if (rs.stroke != null && rs.strokeAwt != null) {
                g.setColor(resolveDynamicColor(rs.spec.strokeColor, rs.strokeAwt));
                g.setStroke(rs.stroke);
                g.draw(rs.spec.path);
            }
        }
        if (oldComp != null) g.setComposite(oldComp);
        g.setTransform(old);
    }

    @Override
    public ConstantState getConstantState() {
        final VectorDrawable snapshot = new VectorDrawable(this);
        return new ConstantState() {
            @Override
            public Drawable newDrawable() {
                return new VectorDrawable(snapshot);
            }

            @Override
            public Drawable newDrawable(Resources res) {
                return new VectorDrawable(snapshot);
            }
        };
    }

    /** Bộ đọc lệnh SVG pathData đầy đủ (M, L, H, V, C, S, Q, T, A, Z và dạng chữ thường tương đối). */
    public static Path2D.Float parseSvgPath(String d) {
        Path2D.Float p = new Path2D.Float();
        int i = 0, n = d.length();
        char cmd = 0;
        char prevCmd = 0;
        float cx = 0, cy = 0, sx = 0, sy = 0;
        float lastCtrlX = 0, lastCtrlY = 0;
        float[] args = new float[6];

        while (i < n) {
            while (i < n && (Character.isWhitespace(d.charAt(i)) || d.charAt(i) == ',')) i++;
            if (i >= n) break;
            char c = d.charAt(i);
            if (Character.isLetter(c)) {
                cmd = c;
                i++;
                if (cmd == 'Z' || cmd == 'z') {
                    p.closePath();
                    cx = sx;
                    cy = sy;
                    lastCtrlX = cx;
                    lastCtrlY = cy;
                    prevCmd = cmd;
                    continue;
                }
            }
            char upper = Character.toUpperCase(cmd);
            int needed = switch (upper) {
                case 'M', 'L', 'T' -> 2;
                case 'H', 'V' -> 1;
                case 'Q', 'S' -> 4;
                case 'C' -> 6;
                case 'A' -> 7;
                default -> 0;
            };
            if (needed == 0) break;
            boolean rel = Character.isLowerCase(cmd);
            if (upper == 'A') {
                int[] pos = {i};
                float rx = readFloat(d, pos);
                float ry = readFloat(d, pos);
                float xAxisRot = readFloat(d, pos);
                int largeArc = readFlag(d, pos);
                int sweep = readFlag(d, pos);
                float x = readFloat(d, pos);
                float y = readFloat(d, pos);
                i = pos[0];
                float nx = rel ? cx + x : x;
                float ny = rel ? cy + y : y;
                appendSvgArc(p, cx, cy, nx, ny, rx, ry, xAxisRot, largeArc != 0, sweep != 0);
                cx = nx;
                cy = ny;
                lastCtrlX = cx;
                lastCtrlY = cy;
                prevCmd = cmd;
                continue;
            }
            for (int k = 0; k < needed; k++) {
                int[] next = {i};
                args[k] = readFloat(d, next);
                i = next[0];
            }
            switch (upper) {
                case 'M' -> {
                    cx = rel ? cx + args[0] : args[0];
                    cy = rel ? cy + args[1] : args[1];
                    p.moveTo(cx, cy);
                    sx = cx;
                    sy = cy;
                    lastCtrlX = cx;
                    lastCtrlY = cy;
                    prevCmd = cmd;
                    cmd = rel ? 'l' : 'L';
                }
                case 'L' -> {
                    cx = rel ? cx + args[0] : args[0];
                    cy = rel ? cy + args[1] : args[1];
                    p.lineTo(cx, cy);
                    lastCtrlX = cx;
                    lastCtrlY = cy;
                    prevCmd = cmd;
                }
                case 'H' -> {
                    cx = rel ? cx + args[0] : args[0];
                    p.lineTo(cx, cy);
                    lastCtrlX = cx;
                    lastCtrlY = cy;
                    prevCmd = cmd;
                }
                case 'V' -> {
                    cy = rel ? cy + args[0] : args[0];
                    p.lineTo(cx, cy);
                    lastCtrlX = cx;
                    lastCtrlY = cy;
                    prevCmd = cmd;
                }
                case 'Q' -> {
                    float x1 = rel ? cx + args[0] : args[0];
                    float y1 = rel ? cy + args[1] : args[1];
                    float x2 = rel ? cx + args[2] : args[2];
                    float y2 = rel ? cy + args[3] : args[3];
                    p.quadTo(x1, y1, x2, y2);
                    lastCtrlX = x1;
                    lastCtrlY = y1;
                    cx = x2;
                    cy = y2;
                    prevCmd = cmd;
                }
                case 'T' -> {
                    float x1 = (prevCmd == 'Q' || prevCmd == 'q' || prevCmd == 'T' || prevCmd == 't')
                            ? (2f * cx - lastCtrlX) : cx;
                    float y1 = (prevCmd == 'Q' || prevCmd == 'q' || prevCmd == 'T' || prevCmd == 't')
                            ? (2f * cy - lastCtrlY) : cy;
                    float x2 = rel ? cx + args[0] : args[0];
                    float y2 = rel ? cy + args[1] : args[1];
                    p.quadTo(x1, y1, x2, y2);
                    lastCtrlX = x1;
                    lastCtrlY = y1;
                    cx = x2;
                    cy = y2;
                    prevCmd = cmd;
                }
                case 'C' -> {
                    float x1 = rel ? cx + args[0] : args[0];
                    float y1 = rel ? cy + args[1] : args[1];
                    float x2 = rel ? cx + args[2] : args[2];
                    float y2 = rel ? cy + args[3] : args[3];
                    float x3 = rel ? cx + args[4] : args[4];
                    float y3 = rel ? cy + args[5] : args[5];
                    p.curveTo(x1, y1, x2, y2, x3, y3);
                    lastCtrlX = x2;
                    lastCtrlY = y2;
                    cx = x3;
                    cy = y3;
                    prevCmd = cmd;
                }
                case 'S' -> {
                    float x1 = (prevCmd == 'C' || prevCmd == 'c' || prevCmd == 'S' || prevCmd == 's')
                            ? (2f * cx - lastCtrlX) : cx;
                    float y1 = (prevCmd == 'C' || prevCmd == 'c' || prevCmd == 'S' || prevCmd == 's')
                            ? (2f * cy - lastCtrlY) : cy;
                    float x2 = rel ? cx + args[0] : args[0];
                    float y2 = rel ? cy + args[1] : args[1];
                    float x3 = rel ? cx + args[2] : args[2];
                    float y3 = rel ? cy + args[3] : args[3];
                    p.curveTo(x1, y1, x2, y2, x3, y3);
                    lastCtrlX = x2;
                    lastCtrlY = y2;
                    cx = x3;
                    cy = y3;
                    prevCmd = cmd;
                }
            }
        }
        return p;
    }

    /** Chuyển cung elip SVG (A/a) sang Path2D đúng chuẩn W3C SVG F.6.5/F.6.6. */
    private static void appendSvgArc(Path2D.Float path, double x0, double y0, double x, double y,
                                     double rxIn, double ryIn, double angleDeg,
                                     boolean largeArcFlag, boolean sweepFlag) {
        if (Math.abs(x0 - x) < 1e-6 && Math.abs(y0 - y) < 1e-6) return;
        double rx = Math.abs(rxIn);
        double ry = Math.abs(ryIn);
        if (rx < 1e-6 || ry < 1e-6) {
            path.lineTo((float) x, (float) y);
            return;
        }
        double dx2 = (x0 - x) / 2.0;
        double dy2 = (y0 - y) / 2.0;
        double phi = Math.toRadians(angleDeg % 360.0);
        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);

        double x1 = cosPhi * dx2 + sinPhi * dy2;
        double y1 = -sinPhi * dx2 + cosPhi * dy2;

        double rxSq = rx * rx;
        double rySq = ry * ry;
        double x1Sq = x1 * x1;
        double y1Sq = y1 * y1;

        double radiiCheck = x1Sq / rxSq + y1Sq / rySq;
        if (radiiCheck > 1.0) {
            double scale = Math.sqrt(radiiCheck);
            rx *= scale;
            ry *= scale;
            rxSq = rx * rx;
            rySq = ry * ry;
        }

        double sign = (largeArcFlag == sweepFlag) ? -1.0 : 1.0;
        double num = Math.max(0.0, rxSq * rySq - rxSq * y1Sq - rySq * x1Sq);
        double den = rxSq * y1Sq + rySq * x1Sq;
        double coef = den > 1e-12 ? sign * Math.sqrt(num / den) : 0.0;
        double cx1 = coef * ((rx * y1) / ry);
        double cy1 = coef * -((ry * x1) / rx);

        double sx2 = (x0 + x) / 2.0;
        double sy2 = (y0 + y) / 2.0;
        double cx = sx2 + (cosPhi * cx1 - sinPhi * cy1);
        double cy = sy2 + (sinPhi * cx1 + cosPhi * cy1);

        double ux = (x1 - cx1) / rx;
        double uy = (y1 - cy1) / ry;
        double vx = (-x1 - cx1) / rx;
        double vy = (-y1 - cy1) / ry;

        double angleStart = vectorAngle(1.0, 0.0, ux, uy);
        double angleExtent = vectorAngle(ux, uy, vx, vy);
        if (!sweepFlag && angleExtent > 0) {
            angleExtent -= 2.0 * Math.PI;
        } else if (sweepFlag && angleExtent < 0) {
            angleExtent += 2.0 * Math.PI;
        }

        Arc2D.Double arc = new Arc2D.Double(
                -rx, -ry, 2.0 * rx, 2.0 * ry,
                -Math.toDegrees(angleStart),
                -Math.toDegrees(angleExtent),
                Arc2D.OPEN);
        AffineTransform at = AffineTransform.getTranslateInstance(cx, cy);
        if (phi != 0.0) {
            at.rotate(phi);
        }
        path.append(at.createTransformedShape(arc), true);
    }

    private static double vectorAngle(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy;
        double len = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
        if (len < 1e-12) return 0.0;
        double cos = Math.max(-1.0, Math.min(1.0, dot / len));
        double angle = Math.acos(cos);
        return (ux * vy - uy * vx) < 0 ? -angle : angle;
    }

    private static int readFlag(String s, int[] pos) {
        int i = pos[0], n = s.length();
        while (i < n && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ',')) i++;
        if (i < n && (s.charAt(i) == '0' || s.charAt(i) == '1')) {
            int val = s.charAt(i) - '0';
            pos[0] = i + 1;
            return val;
        }
        return readFloat(s, pos) != 0f ? 1 : 0;
    }

    private static float readFloat(String s, int[] pos) {
        int i = pos[0], n = s.length();
        while (i < n && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ',')) i++;
        int start = i;
        if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
        boolean seenDot = false;
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                i++;
            } else if (c == '.' && !seenDot) {
                seenDot = true;
                i++;
            } else if ((c == 'e' || c == 'E') && i + 1 < n) {
                i++;
                if (s.charAt(i) == '+' || s.charAt(i) == '-') i++;
            } else {
                break;
            }
        }
        pos[0] = i;
        if (start >= i) return 0f;
        try {
            return Float.parseFloat(s.substring(start, i));
        } catch (NumberFormatException e) {
            return 0f;
        }
    }
}
