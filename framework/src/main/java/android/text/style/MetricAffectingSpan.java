package android.text.style;

import android.text.TextPaint;

public abstract class MetricAffectingSpan extends CharacterStyle implements UpdateAppearance {
    public abstract void updateMeasureState(TextPaint textPaint);

    @Override
    public MetricAffectingSpan getUnderlying() {
        return this;
    }
}
