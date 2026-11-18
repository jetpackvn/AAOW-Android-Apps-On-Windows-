package android.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

public abstract class Layout {
    public enum Alignment {
        ALIGN_NORMAL,
        ALIGN_OPPOSITE,
        ALIGN_CENTER,
        ALIGN_LEFT,
        ALIGN_RIGHT
    }

    public static class Directions {
        final int[] directions;

        Directions(int[] dirs) {
            this.directions = dirs;
        }
    }

    public static final int DIR_LEFT_TO_RIGHT = 1;
    public static final int DIR_RIGHT_TO_LEFT = -1;

    private final CharSequence text;
    private final TextPaint paint;
    private int width;
    private final Alignment alignment;
    private final float spacingMult;
    private final float spacingAdd;

    protected Layout(CharSequence text, TextPaint paint, int width,
                     Alignment align, float spacingMult, float spacingAdd) {
        this.text = text != null ? text : "";
        this.paint = paint != null ? paint : new TextPaint();
        this.width = Math.max(0, width);
        this.alignment = align != null ? align : Alignment.ALIGN_NORMAL;
        this.spacingMult = spacingMult;
        this.spacingAdd = spacingAdd;
    }

    public final CharSequence getText() {
        return text;
    }

    public final TextPaint getPaint() {
        return paint;
    }

    public final int getWidth() {
        return width;
    }

    public final void increaseWidthTo(int wid) {
        if (wid > width) width = wid;
    }

    public int getHeight() {
        int count = getLineCount();
        return count > 0 ? getLineBottom(count - 1) : 0;
    }

    public final Alignment getAlignment() {
        return alignment;
    }

    public final float getSpacingMultiplier() {
        return spacingMult;
    }

    public final float getSpacingAdd() {
        return spacingAdd;
    }

    public abstract int getLineCount();

    public abstract int getLineTop(int line);

    public abstract int getLineDescent(int line);

    public abstract int getLineStart(int line);

    public abstract int getParagraphDirection(int line);

    public abstract boolean getLineContainsTab(int line);

    public abstract Directions getLineDirections(int line);

    public abstract int getTopPadding();

    public abstract int getBottomPadding();

    public int getEllipsisStart(int line) {
        return 0;
    }

    public int getEllipsisCount(int line) {
        return 0;
    }

    public final int getLineBottom(int line) {
        return getLineTop(line + 1);
    }

    public final int getLineBaseline(int line) {
        return getLineTop(line + 1) - getLineDescent(line);
    }

    public final int getLineAscent(int line) {
        return getLineTop(line) - (getLineTop(line + 1) - getLineDescent(line));
    }

    public final int getLineEnd(int line) {
        return getLineStart(line + 1);
    }

    public int getLineVisibleEnd(int line) {
        int end = getLineEnd(line);
        int start = getLineStart(line);
        while (end > start && text.charAt(end - 1) == '\n') {
            end--;
        }
        return end;
    }

    public int getLineForVertical(int vertical) {
        int high = getLineCount();
        int low = -1;
        while (high - low > 1) {
            int guess = (high + low) / 2;
            if (getLineTop(guess) > vertical) high = guess;
            else low = guess;
        }
        return Math.max(0, Math.min(getLineCount() - 1, low));
    }

    public int getLineForOffset(int offset) {
        int high = getLineCount();
        int low = -1;
        while (high - low > 1) {
            int guess = (high + low) / 2;
            if (getLineStart(guess) > offset) high = guess;
            else low = guess;
        }
        return Math.max(0, Math.min(getLineCount() - 1, low));
    }

    public int getOffsetForHorizontal(int line, float horiz) {
        int start = getLineStart(line);
        int end = getLineVisibleEnd(line);
        String lineStr = text.subSequence(start, end).toString();
        float bestDist = Float.MAX_VALUE;
        int bestOffset = start;
        for (int i = 0; i <= lineStr.length(); i++) {
            float w = paint.measureText(lineStr, 0, i);
            float dist = Math.abs(w - horiz);
            if (dist < bestDist) {
                bestDist = dist;
                bestOffset = start + i;
            }
        }
        return bestOffset;
    }

    public float getPrimaryHorizontal(int offset) {
        int line = getLineForOffset(offset);
        int start = getLineStart(line);
        int clamped = Math.max(start, Math.min(offset, text.length()));
        return paint.measureText(text.subSequence(start, clamped).toString());
    }

    public float getSecondaryHorizontal(int offset) {
        return getPrimaryHorizontal(offset);
    }

    public float getLineLeft(int line) {
        return 0f;
    }

    public float getLineRight(int line) {
        return getLineWidth(line);
    }

    public float getLineWidth(int line) {
        int start = getLineStart(line);
        int end = getLineVisibleEnd(line);
        return paint.measureText(text.subSequence(start, end).toString());
    }

    public float getLineMax(int line) {
        return getLineWidth(line);
    }

    public int getLineBounds(int line, Rect bounds) {
        if (bounds != null) {
            bounds.top = getLineTop(line);
            bounds.bottom = getLineBottom(line);
            bounds.left = 0;
            bounds.right = width;
        }
        return getLineBaseline(line);
    }

    public void getSelectionPath(int start, int end, Path dest) {
    }

    public void draw(Canvas canvas) {
        if (canvas == null) return;
        int count = getLineCount();
        for (int i = 0; i < count; i++) {
            int s = getLineStart(i);
            int e = getLineVisibleEnd(i);
            canvas.drawText(text, s, e, 0, getLineBaseline(i), paint);
        }
    }

    public static float getDesiredWidth(CharSequence source, TextPaint paint) {
        return source == null || paint == null ? 0f : paint.measureText(source.toString());
    }
}
