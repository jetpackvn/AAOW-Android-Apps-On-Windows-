package android.graphics;

import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;

public class LinearGradient extends Shader {
    private final float x0, y0, x1, y1;
    private final int[] colors;
    private final float[] positions;
    private final TileMode tileMode;

    public LinearGradient(float x0, float y0, float x1, float y1, int[] colors, float[] positions, TileMode tile) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.colors = colors != null ? colors.clone() : new int[] { Color.BLACK, Color.WHITE };
        this.positions = positions != null ? positions.clone() : null;
        this.tileMode = tile != null ? tile : TileMode.CLAMP;
    }

    public LinearGradient(float x0, float y0, float x1, float y1, int color0, int color1, TileMode tile) {
        this(x0, y0, x1, y1, new int[] { color0, color1 }, null, tile);
    }

    public java.awt.Paint toAwtPaint() {
        if (colors.length == 0) return java.awt.Color.BLACK;
        if (colors.length == 1 || (Math.abs(x1 - x0) < 0.001f && Math.abs(y1 - y0) < 0.001f)) {
            return Color.toAwt(colors[0]);
        }
        float[] fracs = new float[colors.length];
        if (positions != null && positions.length == colors.length) {
            float prev = -1f;
            for (int i = 0; i < colors.length; i++) {
                float f = Math.max(0f, Math.min(1f, positions[i]));
                if (f <= prev) f = Math.min(1f, prev + 0.0001f);
                fracs[i] = f;
                prev = f;
            }
        } else {
            for (int i = 0; i < colors.length; i++) {
                fracs[i] = (float) i / (colors.length - 1);
            }
        }
        java.awt.Color[] awtColors = new java.awt.Color[colors.length];
        for (int i = 0; i < colors.length; i++) {
            awtColors[i] = Color.toAwt(colors[i]);
        }
        MultipleGradientPaint.CycleMethod cycle = switch (tileMode) {
            case REPEAT -> MultipleGradientPaint.CycleMethod.REPEAT;
            case MIRROR -> MultipleGradientPaint.CycleMethod.REFLECT;
            default -> MultipleGradientPaint.CycleMethod.NO_CYCLE;
        };
        try {
            return new LinearGradientPaint(x0, y0, x1, y1, fracs, awtColors, cycle);
        } catch (IllegalArgumentException e) {
            return awtColors[0];
        }
    }
}
