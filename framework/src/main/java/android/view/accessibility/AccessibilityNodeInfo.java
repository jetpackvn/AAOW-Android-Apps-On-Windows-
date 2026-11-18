package android.view.accessibility;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

public class AccessibilityNodeInfo implements Parcelable {
    public static final int ACTION_FOCUS = 0x00000001;
    public static final int ACTION_CLEAR_FOCUS = 0x00000002;
    public static final int ACTION_CLICK = 0x00000010;
    public static final int ACTION_LONG_CLICK = 0x00000020;
    public static final int ACTION_SCROLL_FORWARD = 0x00001000;
    public static final int ACTION_SCROLL_BACKWARD = 0x00002000;

    public static final class AccessibilityAction implements Parcelable {
        public static final AccessibilityAction ACTION_FOCUS = new AccessibilityAction(AccessibilityNodeInfo.ACTION_FOCUS, null);
        public static final AccessibilityAction ACTION_CLEAR_FOCUS = new AccessibilityAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS, null);
        public static final AccessibilityAction ACTION_SELECT = new AccessibilityAction(0x00000004, null);
        public static final AccessibilityAction ACTION_CLEAR_SELECTION = new AccessibilityAction(0x00000008, null);
        public static final AccessibilityAction ACTION_CLICK = new AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null);
        public static final AccessibilityAction ACTION_LONG_CLICK = new AccessibilityAction(AccessibilityNodeInfo.ACTION_LONG_CLICK, null);
        public static final AccessibilityAction ACTION_ACCESSIBILITY_FOCUS = new AccessibilityAction(0x00000040, null);
        public static final AccessibilityAction ACTION_CLEAR_ACCESSIBILITY_FOCUS = new AccessibilityAction(0x00000080, null);
        public static final AccessibilityAction ACTION_NEXT_AT_MOVEMENT_GRANULARITY = new AccessibilityAction(0x00000100, null);
        public static final AccessibilityAction ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY = new AccessibilityAction(0x00000200, null);
        public static final AccessibilityAction ACTION_NEXT_HTML_ELEMENT = new AccessibilityAction(0x00000400, null);
        public static final AccessibilityAction ACTION_PREVIOUS_HTML_ELEMENT = new AccessibilityAction(0x00000800, null);
        public static final AccessibilityAction ACTION_SCROLL_FORWARD = new AccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null);
        public static final AccessibilityAction ACTION_SCROLL_BACKWARD = new AccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, null);
        public static final AccessibilityAction ACTION_COPY = new AccessibilityAction(0x00004000, null);
        public static final AccessibilityAction ACTION_PASTE = new AccessibilityAction(0x00008000, null);
        public static final AccessibilityAction ACTION_CUT = new AccessibilityAction(0x00010000, null);
        public static final AccessibilityAction ACTION_SET_SELECTION = new AccessibilityAction(0x00020000, null);
        public static final AccessibilityAction ACTION_EXPAND = new AccessibilityAction(0x00040000, null);
        public static final AccessibilityAction ACTION_COLLAPSE = new AccessibilityAction(0x00080000, null);
        public static final AccessibilityAction ACTION_DISMISS = new AccessibilityAction(0x00100000, null);
        public static final AccessibilityAction ACTION_SET_TEXT = new AccessibilityAction(0x00200000, null);
        public static final AccessibilityAction ACTION_SHOW_ON_SCREEN = new AccessibilityAction(0x01020036, null);
        public static final AccessibilityAction ACTION_SCROLL_TO_POSITION = new AccessibilityAction(0x01020037, null);
        public static final AccessibilityAction ACTION_SCROLL_UP = new AccessibilityAction(0x01020038, null);
        public static final AccessibilityAction ACTION_SCROLL_LEFT = new AccessibilityAction(0x01020039, null);
        public static final AccessibilityAction ACTION_SCROLL_DOWN = new AccessibilityAction(0x0102003a, null);
        public static final AccessibilityAction ACTION_SCROLL_RIGHT = new AccessibilityAction(0x0102003b, null);
        public static final AccessibilityAction ACTION_PAGE_UP = new AccessibilityAction(0x01020046, null);
        public static final AccessibilityAction ACTION_PAGE_DOWN = new AccessibilityAction(0x01020047, null);
        public static final AccessibilityAction ACTION_PAGE_LEFT = new AccessibilityAction(0x01020048, null);
        public static final AccessibilityAction ACTION_PAGE_RIGHT = new AccessibilityAction(0x01020049, null);
        public static final AccessibilityAction ACTION_CONTEXT_CLICK = new AccessibilityAction(0x0102003c, null);
        public static final AccessibilityAction ACTION_SET_PROGRESS = new AccessibilityAction(0x0102003d, null);
        public static final AccessibilityAction ACTION_MOVE_WINDOW = new AccessibilityAction(0x01020042, null);
        public static final AccessibilityAction ACTION_SHOW_TOOLTIP = new AccessibilityAction(0x01020044, null);
        public static final AccessibilityAction ACTION_HIDE_TOOLTIP = new AccessibilityAction(0x01020045, null);
        public static final AccessibilityAction ACTION_PRESS_AND_HOLD = new AccessibilityAction(0x0102004a, null);
        public static final AccessibilityAction ACTION_IME_ENTER = new AccessibilityAction(0x01020054, null);
        public static final AccessibilityAction ACTION_DRAG_START = new AccessibilityAction(0x01020055, null);
        public static final AccessibilityAction ACTION_DRAG_DROP = new AccessibilityAction(0x01020056, null);
        public static final AccessibilityAction ACTION_DRAG_CANCEL = new AccessibilityAction(0x01020057, null);
        public static final AccessibilityAction ACTION_SHOW_TEXT_SUGGESTIONS = new AccessibilityAction(0x01020058, null);
        public static final AccessibilityAction ACTION_SCROLL_IN_DIRECTION = new AccessibilityAction(0x0102005e, null);
        public static final AccessibilityAction ACTION_SET_EXTENDED_SELECTION = new AccessibilityAction(0x0102005f, null);

        private final int id;
        private final CharSequence label;

        public AccessibilityAction(int actionId, CharSequence label) {
            this.id = actionId;
            this.label = label;
        }

        public int getId() {
            return id;
        }

        public CharSequence getLabel() {
            return label;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(id);
        }

        public static final Creator<AccessibilityAction> CREATOR = new Creator<>() {
            @Override
            public AccessibilityAction createFromParcel(Parcel source) {
                return new AccessibilityAction(source.readInt(), null);
            }

            @Override
            public AccessibilityAction[] newArray(int size) {
                return new AccessibilityAction[size];
            }
        };
    }

    private CharSequence className;
    private CharSequence packageName;
    private CharSequence text;
    private CharSequence contentDescription;
    private CharSequence stateDescription;
    private boolean scrollable;
    private boolean clickable;
    private boolean checkable;
    private boolean checked;
    private boolean focusable;
    private boolean focused;
    private boolean enabled = true;
    private int actions;

    public AccessibilityNodeInfo() {
    }

    public static AccessibilityNodeInfo obtain() {
        return new AccessibilityNodeInfo();
    }

    public static AccessibilityNodeInfo obtain(View source) {
        return new AccessibilityNodeInfo();
    }

    public void recycle() {
    }

    public void addAction(int action) {
        actions |= action;
    }

    public int getActions() {
        return actions;
    }

    public void setClassName(CharSequence className) {
        this.className = className;
    }

    public CharSequence getClassName() {
        return className;
    }

    public void setPackageName(CharSequence packageName) {
        this.packageName = packageName;
    }

    public CharSequence getPackageName() {
        return packageName;
    }

    public void setText(CharSequence text) {
        this.text = text;
    }

    public CharSequence getText() {
        return text;
    }

    public void setContentDescription(CharSequence contentDescription) {
        this.contentDescription = contentDescription;
    }

    public CharSequence getContentDescription() {
        return contentDescription;
    }

    public void setStateDescription(CharSequence stateDescription) {
        this.stateDescription = stateDescription;
    }

    public CharSequence getStateDescription() {
        return stateDescription;
    }

    public void setScrollable(boolean scrollable) {
        this.scrollable = scrollable;
    }

    public boolean isScrollable() {
        return scrollable;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }

    public boolean isClickable() {
        return clickable;
    }

    public void setCheckable(boolean checkable) {
        this.checkable = checkable;
    }

    public boolean isCheckable() {
        return checkable;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    public boolean isFocusable() {
        return focusable;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void getBoundsInParent(Rect outBounds) {
        if (outBounds != null) outBounds.setEmpty();
    }

    public void getBoundsInScreen(Rect outBounds) {
        if (outBounds != null) outBounds.setEmpty();
    }

    public Bundle getExtras() {
        return new Bundle();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    public static final Creator<AccessibilityNodeInfo> CREATOR = new Creator<>() {
        @Override
        public AccessibilityNodeInfo createFromParcel(Parcel source) {
            return new AccessibilityNodeInfo();
        }

        @Override
        public AccessibilityNodeInfo[] newArray(int size) {
            return new AccessibilityNodeInfo[size];
        }
    };
}
