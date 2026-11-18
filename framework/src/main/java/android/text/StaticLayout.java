package android.text;

import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

public class StaticLayout extends Layout {
    private final int lineCount;
    private final int[] lineStarts;
    private final int[] lineTops;
    private final int descent;

    public StaticLayout(CharSequence source, TextPaint paint, int width,
                        Alignment align, float spacingmult, float spacingadd, boolean includepad) {
        this(source, 0, source != null ? source.length() : 0, paint, width, align,
                spacingmult, spacingadd, includepad, null, width);
    }

    public StaticLayout(CharSequence source, int bufstart, int bufend,
                        TextPaint paint, int outerwidth,
                        Alignment align, float spacingmult, float spacingadd, boolean includepad) {
        this(source, bufstart, bufend, paint, outerwidth, align,
                spacingmult, spacingadd, includepad, null, outerwidth);
    }

    public StaticLayout(CharSequence source, int bufstart, int bufend,
                        TextPaint paint, int outerwidth,
                        Alignment align, float spacingmult, float spacingadd, boolean includepad,
                        TextUtils.TruncateAt ellipsize, int ellipsizedWidth) {
        super(source, paint, outerwidth, align, spacingmult, spacingadd);
        CharSequence src = source != null ? source : "";
        int start = Math.max(0, Math.min(bufstart, src.length()));
        int end = Math.max(start, Math.min(bufend, src.length()));
        TextPaint tp = getPaint();
        Paint.FontMetrics fm = tp.getFontMetrics();
        int baseLineH = Math.max(12, Math.round((fm.descent - fm.ascent) * spacingmult + spacingadd));
        this.descent = Math.max(2, Math.round(fm.descent));

        List<Integer> starts = new ArrayList<>();
        starts.add(start);
        int maxW = Math.max(1, outerwidth);
        int curLineStart = start;
        for (int i = start; i < end; i++) {
            char c = src.charAt(i);
            if (c == '\n') {
                curLineStart = i + 1;
                starts.add(curLineStart);
            } else if (outerwidth > 0 && i > curLineStart) {
                float w = tp.measureText(src.subSequence(curLineStart, i + 1).toString());
                if (w > maxW) {
                    curLineStart = i;
                    starts.add(curLineStart);
                }
            }
        }
        this.lineCount = starts.size();
        this.lineStarts = new int[this.lineCount + 1];
        this.lineTops = new int[this.lineCount + 1];
        for (int i = 0; i < this.lineCount; i++) {
            this.lineStarts[i] = starts.get(i);
            this.lineTops[i] = i * baseLineH;
        }
        this.lineStarts[this.lineCount] = end;
        this.lineTops[this.lineCount] = this.lineCount * baseLineH;
    }

    @Override
    public int getLineCount() {
        return lineCount;
    }

    @Override
    public int getLineTop(int line) {
        int clamped = Math.max(0, Math.min(line, lineCount));
        return lineTops[clamped];
    }

    @Override
    public int getLineDescent(int line) {
        return descent;
    }

    @Override
    public int getLineStart(int line) {
        int clamped = Math.max(0, Math.min(line, lineCount));
        return lineStarts[clamped];
    }

    @Override
    public int getParagraphDirection(int line) {
        return DIR_LEFT_TO_RIGHT;
    }

    @Override
    public boolean getLineContainsTab(int line) {
        int s = getLineStart(line);
        int e = getLineEnd(line);
        CharSequence t = getText();
        for (int i = s; i < e; i++) {
            if (t.charAt(i) == '\t') return true;
        }
        return false;
    }

    @Override
    public Directions getLineDirections(int line) {
        return new Directions(new int[]{0, getLineEnd(line) - getLineStart(line)});
    }

    @Override
    public int getTopPadding() {
        return 0;
    }

    @Override
    public int getBottomPadding() {
        return 0;
    }

    public static final class Builder {
        private CharSequence text;
        private int start;
        private int end;
        private TextPaint paint;
        private int width;
        private Alignment alignment = Alignment.ALIGN_NORMAL;
        private float spacingMult = 1.0f;
        private float spacingAdd = 0.0f;
        private boolean includePad = true;
        private TextUtils.TruncateAt ellipsize;
        private int ellipsizedWidth;

        private Builder(CharSequence source, int start, int end, TextPaint paint, int width) {
            this.text = source != null ? source : "";
            this.start = Math.max(0, start);
            this.end = Math.max(this.start, Math.min(end, this.text.length()));
            this.paint = paint;
            this.width = Math.max(0, width);
            this.ellipsizedWidth = this.width;
        }

        public static Builder obtain(CharSequence source, int start, int end, TextPaint paint, int width) {
            return new Builder(source, start, end, paint, width);
        }

        public Builder setText(CharSequence source) {
            this.text = source != null ? source : "";
            this.start = 0;
            this.end = this.text.length();
            return this;
        }

        public Builder setAlignment(Alignment alignment) {
            if (alignment != null) this.alignment = alignment;
            return this;
        }

        public Builder setTextDirection(TextDirectionHeuristic textDir) { return this; }
        public Builder setLineSpacing(float spacingAdd, float spacingMult) {
            this.spacingAdd = spacingAdd;
            this.spacingMult = spacingMult;
            return this;
        }
        public Builder setIncludePad(boolean includePad) { this.includePad = includePad; return this; }
        public Builder setUseLineSpacingFromFallbacks(boolean use) { return this; }
        public Builder setEllipsizedWidth(int ellipsizedWidth) { this.ellipsizedWidth = ellipsizedWidth; return this; }
        public Builder setEllipsize(TextUtils.TruncateAt ellipsize) { this.ellipsize = ellipsize; return this; }
        public Builder setMaxLines(int maxLines) { return this; }
        public Builder setBreakStrategy(int breakStrategy) { return this; }
        public Builder setHyphenationFrequency(int hyphenationFrequency) { return this; }
        public Builder setJustificationMode(int justificationMode) { return this; }
        public Builder setIndents(int[] leftIndents, int[] rightIndents) { return this; }

        public StaticLayout build() {
            return new StaticLayout(text, start, end, paint, width, alignment,
                    spacingMult, spacingAdd, includePad, ellipsize, ellipsizedWidth);
        }
    }
}
