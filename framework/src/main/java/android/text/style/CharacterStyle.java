package android.text.style;

import android.text.TextPaint;

public abstract class CharacterStyle {
    public abstract void updateDrawState(TextPaint tp);

    public static CharacterStyle wrap(CharacterStyle cs) {
        return cs;
    }

    public CharacterStyle getUnderlying() {
        return this;
    }
}
