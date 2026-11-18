package android.view.accessibility;

import android.os.Parcelable;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class AccessibilityRecord {
    private final List<CharSequence> text = new ArrayList<>();
    private CharSequence className;
    private CharSequence contentDescription;
    private CharSequence beforeText;
    private boolean checked;
    private boolean enabled = true;
    private boolean password;
    private boolean fullScreen;
    private boolean scrollable;
    private int itemCount;
    private int currentItemIndex;
    private int fromIndex;
    private int toIndex;
    private int addedCount;
    private int removedCount;
    private int scrollX;
    private int scrollY;
    private int maxScrollX;
    private int maxScrollY;
    private int scrollDeltaX;
    private int scrollDeltaY;
    private Parcelable parcelableData;

    public AccessibilityRecord() {
    }

    public AccessibilityRecord(AccessibilityRecord record) {
        if (record != null) {
            this.className = record.className;
            this.contentDescription = record.contentDescription;
            this.text.addAll(record.text);
        }
    }

    public static AccessibilityRecord obtain() {
        return new AccessibilityRecord();
    }

    public static AccessibilityRecord obtain(AccessibilityRecord record) {
        return new AccessibilityRecord(record);
    }

    public void recycle() {
    }

    public void setSource(View source) {
    }

    public void setSource(View root, int virtualDescendantId) {
    }

    public AccessibilityNodeInfo getSource() {
        return null;
    }

    public List<CharSequence> getText() {
        return text;
    }

    public CharSequence getClassName() {
        return className;
    }

    public void setClassName(CharSequence className) {
        this.className = className;
    }

    public CharSequence getContentDescription() {
        return contentDescription;
    }

    public void setContentDescription(CharSequence contentDescription) {
        this.contentDescription = contentDescription;
    }

    public CharSequence getBeforeText() {
        return beforeText;
    }

    public void setBeforeText(CharSequence beforeText) {
        this.beforeText = beforeText;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean isChecked) {
        this.checked = isChecked;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.enabled = isEnabled;
    }

    public boolean isPassword() {
        return password;
    }

    public void setPassword(boolean isPassword) {
        this.password = isPassword;
    }

    public boolean isFullScreen() {
        return fullScreen;
    }

    public void setFullScreen(boolean isFullScreen) {
        this.fullScreen = isFullScreen;
    }

    public boolean isScrollable() {
        return scrollable;
    }

    public void setScrollable(boolean scrollable) {
        this.scrollable = scrollable;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public int getCurrentItemIndex() {
        return currentItemIndex;
    }

    public void setCurrentItemIndex(int currentItemIndex) {
        this.currentItemIndex = currentItemIndex;
    }

    public int getFromIndex() {
        return fromIndex;
    }

    public void setFromIndex(int fromIndex) {
        this.fromIndex = fromIndex;
    }

    public int getToIndex() {
        return toIndex;
    }

    public void setToIndex(int toIndex) {
        this.toIndex = toIndex;
    }

    public int getAddedCount() {
        return addedCount;
    }

    public void setAddedCount(int addedCount) {
        this.addedCount = addedCount;
    }

    public int getRemovedCount() {
        return removedCount;
    }

    public void setRemovedCount(int removedCount) {
        this.removedCount = removedCount;
    }

    public int getScrollX() {
        return scrollX;
    }

    public void setScrollX(int scrollX) {
        this.scrollX = scrollX;
    }

    public int getScrollY() {
        return scrollY;
    }

    public void setScrollY(int scrollY) {
        this.scrollY = scrollY;
    }

    public int getMaxScrollX() {
        return maxScrollX;
    }

    public void setMaxScrollX(int maxScrollX) {
        this.maxScrollX = maxScrollX;
    }

    public int getMaxScrollY() {
        return maxScrollY;
    }

    public void setMaxScrollY(int maxScrollY) {
        this.maxScrollY = maxScrollY;
    }

    public int getScrollDeltaX() {
        return scrollDeltaX;
    }

    public void setScrollDeltaX(int scrollDeltaX) {
        this.scrollDeltaX = scrollDeltaX;
    }

    public int getScrollDeltaY() {
        return scrollDeltaY;
    }

    public void setScrollDeltaY(int scrollDeltaY) {
        this.scrollDeltaY = scrollDeltaY;
    }

    public Parcelable getParcelableData() {
        return parcelableData;
    }

    public void setParcelableData(Parcelable parcelableData) {
        this.parcelableData = parcelableData;
    }
}
