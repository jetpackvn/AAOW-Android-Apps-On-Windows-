package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;

public class Switch extends CompoundButton {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private CharSequence textOn = "ON";
    private CharSequence textOff = "OFF";
    private boolean showText = false;

    public Switch(Context context) {
        this(context, null);
    }

    public Switch(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Switch(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public Switch(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        setTypeface(Typeface.DEFAULT);
        setPadding(8, 8, 56, 8);
        setMinimumHeight(40);
        setMinimumWidth(80);
    }

    public CharSequence getTextOn() {
        return textOn;
    }

    public void setTextOn(CharSequence textOn) {
        this.textOn = textOn;
        invalidate();
    }

    public CharSequence getTextOff() {
        return textOff;
    }

    public void setTextOff(CharSequence textOff) {
        this.textOff = textOff;
        invalidate();
    }

    public boolean getShowText() {
        return showText;
    }

    public void setShowText(boolean showText) {
        this.showText = showText;
        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        if (getVisibility() != VISIBLE) return;
        onDraw(canvas);

        int trackW = 44;
        int trackH = 24;
        int tx = getWidth() - trackW - 6;
        int ty = (getHeight() - trackH) / 2;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(isChecked() ? 0xFF4F5BD5 : 0xFFE0E2EC);
        canvas.drawRoundRect(tx, ty, tx + trackW, ty + trackH, trackH / 2f, trackH / 2f, paint);

        if (!isChecked()) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f);
            paint.setColor(0xFF79747E);
            canvas.drawRoundRect(tx, ty, tx + trackW, ty + trackH, trackH / 2f, trackH / 2f, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(isChecked() ? Color.WHITE : 0xFF79747E);
        float knobCx = isChecked() ? (tx + trackW - 12) : (tx + 12);
        float knobCy = ty + trackH / 2f;
        canvas.drawCircle(knobCx, knobCy, isChecked() ? 8.5f : 7f, paint);
    }
}
