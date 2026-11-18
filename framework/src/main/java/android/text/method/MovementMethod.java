package android.text.method;

import android.text.Spannable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

public interface MovementMethod {
    void initialize(TextView widget, Spannable text);

    boolean onKeyDown(TextView widget, Spannable text, int keyCode, KeyEvent event);

    boolean onKeyUp(TextView widget, Spannable text, int keyCode, KeyEvent event);

    boolean onTouchEvent(TextView widget, Spannable text, MotionEvent event);
}
