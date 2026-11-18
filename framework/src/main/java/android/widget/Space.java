package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

public final class Space extends View {
    public Space(Context context) {
        super(context);
        setVisibility(INVISIBLE);
    }

    public Space(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (getVisibility() == VISIBLE) setVisibility(INVISIBLE);
    }

    @Override
    public void draw(Canvas canvas) {
    }
}
