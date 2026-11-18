package android.text.method;

import android.text.Spannable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

public class BaseMovementMethod implements MovementMethod {
    @Override
    public void initialize(TextView widget, Spannable text) {
    }

    @Override
    public boolean onKeyDown(TextView widget, Spannable text, int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(TextView widget, Spannable text, int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable text, MotionEvent event) {
        return false;
    }
}
