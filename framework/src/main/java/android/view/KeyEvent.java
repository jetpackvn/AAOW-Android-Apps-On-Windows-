package android.view;

import android.os.Parcel;

public class KeyEvent extends InputEvent {
    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP = 1;
    public static final int ACTION_MULTIPLE = 2;

    public static final int META_SHIFT_ON = 0x1;
    public static final int META_ALT_ON = 0x2;
    public static final int META_CTRL_ON = 0x1000;

    public static final int KEYCODE_UNKNOWN = 0;
    public static final int KEYCODE_BACK = 4;
    public static final int KEYCODE_DPAD_UP = 19;
    public static final int KEYCODE_DPAD_DOWN = 20;
    public static final int KEYCODE_DPAD_LEFT = 21;
    public static final int KEYCODE_DPAD_RIGHT = 22;
    public static final int KEYCODE_ENTER = 66;
    public static final int KEYCODE_DEL = 67;
    public static final int KEYCODE_ESCAPE = 111;

    public interface Callback {
        boolean onKeyDown(int keyCode, KeyEvent event);

        boolean onKeyLongPress(int keyCode, KeyEvent event);

        boolean onKeyUp(int keyCode, KeyEvent event);

        boolean onKeyMultiple(int keyCode, int count, KeyEvent event);
    }

    private final int action;
    private final int keyCode;
    private int metaState;

    public KeyEvent(int action, int keyCode) {
        this.action = action;
        this.keyCode = keyCode;
    }

    public KeyEvent(long downTime, long eventTime, int action, int code, int repeat, int metaState) {
        this.action = action;
        this.keyCode = code;
        this.metaState = metaState;
    }

    public final int getAction() {
        return action;
    }

    public final int getKeyCode() {
        return keyCode;
    }

    public final int getMetaState() {
        return metaState;
    }

    public final boolean isShiftPressed() {
        return (metaState & META_SHIFT_ON) != 0;
    }

    public final boolean isCtrlPressed() {
        return (metaState & META_CTRL_ON) != 0;
    }

    public final boolean isAltPressed() {
        return (metaState & META_ALT_ON) != 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(action);
        out.writeInt(keyCode);
        out.writeInt(metaState);
    }
}
