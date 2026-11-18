package android.text;

import android.graphics.Paint;

public class TextPaint extends Paint {
    public int bgColor;
    public int baselineShift;
    public int linkColor;
    public int[] drawableState;
    public float density = 1.0f;

    public TextPaint() {
        super();
    }

    public TextPaint(int flags) {
        super(flags);
    }

    public TextPaint(Paint p) {
        super(p);
    }

    public void set(TextPaint tp) {
        if (tp != null) {
            super.set(tp);
            this.bgColor = tp.bgColor;
            this.baselineShift = tp.baselineShift;
            this.linkColor = tp.linkColor;
            this.drawableState = tp.drawableState;
            this.density = tp.density;
        }
    }
}
