package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cài đặt android.graphics.drawable.VectorDrawable vẽ &lt;vector&gt; drawable của Android
 * (SVG pathData) bằng Java2D, chia sẻ PathSpec qua ConstantState để tiết kiệm CPU/RAM.
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
        if (pathData == null || pathData.isEmpty()) return;
        PathSpec spec = new PathSpec(parseSvgPath(pathData), fillColor, strokeColor, strokeWidth);
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
                g.setColor(tintAwt != null ? tintAwt : rs.fillAwt);
                g.fill(rs.spec.path);
            }
            if (rs.stroke != null && rs.strokeAwt != null) {
                g.setColor(rs.strokeAwt);
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

    /** Bộ đọc lệnh SVG pathData cơ bản (M, L, H, V, C, Q, Z và bản chữ thường tương đối). */
    public static Path2D.Float parseSvgPath(String d) {
        Path2D.Float p = new Path2D.Float();
        int i = 0, n = d.length();
        char cmd = 0;
        float cx = 0, cy = 0, sx = 0, sy = 0;
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
                    continue;
                }
            }
            int needed = switch (Character.toUpperCase(cmd)) {
                case 'M', 'L', 'T' -> 2;
                case 'H', 'V' -> 1;
                case 'Q', 'S' -> 4;
                case 'C' -> 6;
                case 'A' -> 7;
                default -> 0;
            };
            if (needed == 0) break;
            if (needed == 7) {
                float[] a = new float[7];
                for (int k = 0; k < 7; k++) {
                    int[] next = {i};
                    a[k] = readFloat(d, next);
                    i = next[0];
                }
                float nx = cmd == 'a' ? cx + a[5] : a[5];
                float ny = cmd == 'a' ? cy + a[6] : a[6];
                p.lineTo(nx, ny);
                cx = nx;
                cy = ny;
                continue;
            }
            for (int k = 0; k < needed; k++) {
                int[] next = {i};
                args[k] = readFloat(d, next);
                i = next[0];
            }
            boolean rel = Character.isLowerCase(cmd);
            switch (Character.toUpperCase(cmd)) {
                case 'M' -> {
                    cx = rel ? cx + args[0] : args[0];
                    cy = rel ? cy + args[1] : args[1];
                    p.moveTo(cx, cy);
                    sx = cx;
                    sy = cy;
                    cmd = rel ? 'l' : 'L';
                }
                case 'L', 'T' -> {
                    cx = rel ? cx + args[0] : args[0];
                    cy = rel ? cy + args[1] : args[1];
                    p.lineTo(cx, cy);
                }
                case 'H' -> {
                    cx = rel ? cx + args[0] : args[0];
                    p.lineTo(cx, cy);
                }
                case 'V' -> {
                    cy = rel ? cy + args[0] : args[0];
                    p.lineTo(cx, cy);
                }
                case 'Q', 'S' -> {
                    float x1 = rel ? cx + args[0] : args[0];
                    float y1 = rel ? cy + args[1] : args[1];
                    float x2 = rel ? cx + args[2] : args[2];
                    float y2 = rel ? cy + args[3] : args[3];
                    p.quadTo(x1, y1, x2, y2);
                    cx = x2;
                    cy = y2;
                }
                case 'C' -> {
                    float x1 = rel ? cx + args[0] : args[0];
                    float y1 = rel ? cy + args[1] : args[1];
                    float x2 = rel ? cx + args[2] : args[2];
                    float y2 = rel ? cy + args[3] : args[3];
                    float x3 = rel ? cx + args[4] : args[4];
                    float y3 = rel ? cy + args[5] : args[5];
                    p.curveTo(x1, y1, x2, y2, x3, y3);
                    cx = x3;
                    cy = y3;
                }
            }
        }
        return p;
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
