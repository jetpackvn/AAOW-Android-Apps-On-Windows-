package android.widget;

import android.content.Context;

import java.util.function.BiConsumer;

/**
 * Hiển thị thông báo Toast dạng viên thuốc (pill) ngay trên cửa sổ Windows.
 */
public class Toast {
    public static final int LENGTH_SHORT = 0;
    public static final int LENGTH_LONG = 1;

    private static BiConsumer<String, Integer> hostToastBridge;

    private final CharSequence text;
    private int duration;

    public Toast(Context context, CharSequence text, int duration) {
        this.text = text;
        this.duration = duration;
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
