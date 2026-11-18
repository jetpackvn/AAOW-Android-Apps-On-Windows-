package android.view.accessibility;

import android.os.Parcel;
import android.os.Parcelable;

public final class AccessibilityEvent extends AccessibilityRecord implements Parcelable {
    public static final int TYPE_VIEW_CLICKED = 0x00000001;
    public static final int TYPE_VIEW_LONG_CLICKED = 0x00000002;
    public static final int TYPE_VIEW_SELECTED = 0x00000004;
    public static final int TYPE_VIEW_FOCUSED = 0x00000008;
    public static final int TYPE_VIEW_TEXT_CHANGED = 0x00000010;
    public static final int TYPE_WINDOW_STATE_CHANGED = 0x00000020;
    public static final int TYPE_NOTIFICATION_STATE_CHANGED = 0x00000040;
    public static final int TYPE_VIEW_SCROLLED = 0x00001000;
    public static final int TYPE_ANNOUNCEMENT = 0x00004000;
    public static final int TYPES_ALL_MASK = 0xFFFFFFFF;
    public static final int CONTENT_CHANGE_TYPE_UNDEFINED = 0;
    public static final int CONTENT_CHANGE_TYPE_SUBTREE = 1 << 0;
    public static final int CONTENT_CHANGE_TYPE_TEXT = 1 << 1;
    public static final int CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION = 1 << 2;

    private int eventType;
    private CharSequence packageName;
    private long eventTime;
    private int action;
    private int movementGranularity;
    private int contentChangeTypes;

    public AccessibilityEvent() {
    }

    public AccessibilityEvent(int eventType) {
        this.eventType = eventType;
    }

    public AccessibilityEvent(AccessibilityEvent event) {
        super(event);
        if (event != null) {
            this.eventType = event.eventType;
            this.packageName = event.packageName;
            this.eventTime = event.eventTime;
        }
    }

    public static AccessibilityEvent obtain() {
        return new AccessibilityEvent();
    }

    public static AccessibilityEvent obtain(int eventType) {
        return new AccessibilityEvent(eventType);
    }

    public static AccessibilityEvent obtain(AccessibilityEvent event) {
        return new AccessibilityEvent(event);
    }

    public int getEventType() {
        return eventType;
    }

    public void setEventType(int eventType) {
        this.eventType = eventType;
    }

    public CharSequence getPackageName() {
        return packageName;
    }

    public void setPackageName(CharSequence packageName) {
        this.packageName = packageName;
    }

    public long getEventTime() {
        return eventTime;
    }

    public void setEventTime(long eventTime) {
        this.eventTime = eventTime;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public int getMovementGranularity() {
        return movementGranularity;
    }

    public void setMovementGranularity(int granularity) {
        this.movementGranularity = granularity;
    }

    public int getContentChangeTypes() {
        return contentChangeTypes;
    }

    public void setContentChangeTypes(int changeTypes) {
        this.contentChangeTypes = changeTypes;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(eventType);
        dest.writeLong(eventTime);
    }

    public static final Creator<AccessibilityEvent> CREATOR = new Creator<>() {
        @Override
        public AccessibilityEvent createFromParcel(Parcel source) {
            AccessibilityEvent e = new AccessibilityEvent();
            e.eventType = source.readInt();
            e.eventTime = source.readLong();
            return e;
        }

        @Override
        public AccessibilityEvent[] newArray(int size) {
            return new AccessibilityEvent[size];
        }
    };
}
