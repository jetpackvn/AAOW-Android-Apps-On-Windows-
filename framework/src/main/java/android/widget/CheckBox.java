package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;

public class CheckBox extends CompoundButton {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CheckBox(Context context) {
        this(context, null);
    }

    public CheckBox(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CheckBox(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        setTypeface(Typeface.DEFAULT);
        setPadding(32, 8, 12, 8);
        setMinimumHeight(36);
    }

    @Override
    public void draw(Canvas canvas) {
        if (getVisibility() != VISIBLE) return;
        int boxSize = 20;
        int cy = getHeight() / 2;
        int bx = 4;
        int by = cy - boxSize / 2;

        if (isChecked()) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF4F5BD5);
            canvas.drawRoundRect(bx, by, bx + boxSize, by + boxSize, 5, 5, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.4f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(Color.WHITE);
            canvas.drawLine(bx + 5, cy, bx + 9, by + boxSize - 5, paint);
            canvas.drawLine(bx + 9, by + boxSize - 5, bx + boxSize - 4, by + 5, paint);
        } else {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setColor(0xFF79747E);
            canvas.drawRoundRect(bx + 1, by + 1, bx + boxSize - 1, by + boxSize - 1, 5, 5, paint);
        }
        onDraw(canvas);
    }
}
