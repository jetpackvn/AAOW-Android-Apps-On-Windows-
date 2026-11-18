package android.widget;

import android.content.Context;
import android.view.Gravity;
import android.view.View;

import java.util.function.BiConsumer;

/**
 * Hiển thị thông báo Toast dạng viên thuốc (pill) ngay trên cửa sổ Windows.
 */
public class Toast {
    public static final int LENGTH_SHORT = 0;
    public static final int LENGTH_LONG = 1;

    private static BiConsumer<String, Integer> hostToastBridge;

    private final Context context;
    private final CharSequence text;
    private int duration;
    private View view;
    private int gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;

    public Toast(Context context) {
        this(context, "", LENGTH_SHORT);
    }

    public Toast(Context context, CharSequence text, int duration) {
        this.context = context;
        this.text = text;
        this.duration = duration;
    }

    public void setView(View view) {
        this.view = view;
    }

    public View getView() {
        if (view == null && context != null) {
            TextView tv = new TextView(context);
            tv.setText(text);
            view = tv;
        }
        return view;
    }

    public void setGravity(int gravity, int xOffset, int yOffset) {
        this.gravity = gravity;
    }

    public int getGravity() {
        return gravity;
    }

    public static void setHostToastBridge(BiConsumer<String, Integer> bridge) {
        hostToastBridge = bridge;
    }

    public static Toast makeText(Context context, CharSequence text, int duration) {
        return new Toast(context, text, duration);
    }

    public static Toast makeText(Context context, int resId, int duration) {
        CharSequence cs = context != null ? context.getText(resId) : "";
        return new Toast(context, cs, duration);
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getDuration() {
        return duration;
    }

    public void show() {
        String msg = text != null ? text.toString() : "";
        System.out.println("[Toast] " + msg);
        if (hostToastBridge != null) {
            hostToastBridge.accept(msg, duration == LENGTH_LONG ? 3500 : 2200);
        }
    }

    public void cancel() {
    }
}
