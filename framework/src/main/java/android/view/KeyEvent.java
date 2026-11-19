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

    public static class DispatcherState {
        public void reset() {
        }

        public void reset(Object target) {
        }

        public void startTracking(KeyEvent event, Object target) {
        }

        public boolean isTracking(KeyEvent event) {
            return false;
        }

        public void performedLongPress(KeyEvent event) {
        }

        public void handleUpEvent(KeyEvent event) {
        }
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

    public static int normalizeMetaState(int metaState) {
        return metaState & (META_SHIFT_ON | META_ALT_ON | META_CTRL_ON);
    }

    public static boolean metaStateHasNoModifiers(int metaState) {
        return normalizeMetaState(metaState) == 0;
    }

    public static boolean metaStateHasModifiers(int metaState, int modifiers) {
        return (normalizeMetaState(metaState) & modifiers) == modifiers;
    }

    public final boolean hasNoModifiers() {
        return metaStateHasNoModifiers(metaState);
    }

    public final boolean hasModifiers(int modifiers) {
        return metaStateHasModifiers(metaState, modifiers);
    }

    public final boolean isCanceled() {
        return false;
    }

    public final int getUnicodeChar() {
        return 0;
    }

    public final int getUnicodeChar(int metaState) {
        return 0;
    }

    public final int getRepeatCount() {
        return 0;
    }

    public final void startTracking() {
    }

    public final boolean isTracking() {
        return false;
    }

    public final boolean isLongPress() {
        return false;
    }

    public final boolean dispatch(Callback receiver, DispatcherState state, Object target) {
        if (receiver == null) return false;
        return switch (action) {
            case ACTION_DOWN -> receiver.onKeyDown(keyCode, this);
            case ACTION_UP -> receiver.onKeyUp(keyCode, this);
            case ACTION_MULTIPLE -> receiver.onKeyMultiple(keyCode, 1, this);
            default -> false;
        };
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(action);
        out.writeInt(keyCode);
        out.writeInt(metaState);
    }
}
