package android.view;

import android.os.Parcel;

public final class MotionEvent extends InputEvent {
    public static final int ACTION_MASK = 0xff;
    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP = 1;
    public static final int ACTION_MOVE = 2;
    public static final int ACTION_CANCEL = 3;
    public static final int ACTION_OUTSIDE = 4;
    public static final int ACTION_POINTER_DOWN = 5;
    public static final int ACTION_POINTER_UP = 6;
    public static final int ACTION_SCROLL = 8;
    public static final int ACTION_POINTER_INDEX_SHIFT = 8;
    public static final int ACTION_POINTER_INDEX_MASK = 0xff00;

    private int action;
    private float x;
    private float y;
    private final float rawX;
    private final float rawY;
    private final long downTime;
    private final long eventTime;
    private float[] pointerXs;
    private float[] pointerYs;

    public MotionEvent(int action, float x, float y, float rawX, float rawY, long downTime, long eventTime) {
        this.action = action;
        this.x = x;
        this.y = y;
        this.rawX = rawX;
        this.rawY = rawY;
        this.downTime = downTime;
        this.eventTime = eventTime;
    }

    public static MotionEvent obtain(long downTime, long eventTime, int action, float x, float y, int metaState) {
        return new MotionEvent(action, x, y, x, y, downTime, eventTime);
    }

    public static MotionEvent obtainMultiPointer(long downTime, long eventTime, int action,
                                                 float[] xs, float[] ys) {
        float x0 = xs != null && xs.length > 0 ? xs[0] : 0f;
        float y0 = ys != null && ys.length > 0 ? ys[0] : 0f;
        MotionEvent ev = new MotionEvent(action, x0, y0, x0, y0, downTime, eventTime);
        if (xs != null && ys != null && xs.length == ys.length && xs.length > 1) {
            ev.pointerXs = xs.clone();
            ev.pointerYs = ys.clone();
        }
        return ev;
    }

    public static MotionEvent obtain(MotionEvent other) {
        MotionEvent copy = new MotionEvent(other.action, other.x, other.y, other.rawX, other.rawY,
                other.downTime, other.eventTime);
        if (other.pointerXs != null) copy.pointerXs = other.pointerXs.clone();
        if (other.pointerYs != null) copy.pointerYs = other.pointerYs.clone();
        return copy;
    }

    public int getAction() {
        return action;
    }

    public int getActionMasked() {
        return action & ACTION_MASK;
    }

    public int getActionIndex() {
        return (action & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getX(int pointerIndex) {
        if (pointerXs != null && pointerIndex >= 0 && pointerIndex < pointerXs.length) {
            return pointerXs[pointerIndex];
        }
        return x;
    }

    public float getY(int pointerIndex) {
        if (pointerYs != null && pointerIndex >= 0 && pointerIndex < pointerYs.length) {
            return pointerYs[pointerIndex];
        }
        return y;
    }

    public float getRawX() {
        return rawX;
    }

    public float getRawY() {
        return rawY;
    }

    public int getPointerCount() {
        return pointerXs != null ? pointerXs.length : 1;
    }

    public int getPointerId(int pointerIndex) {
        return pointerIndex;
    }

    public int findPointerIndex(int pointerId) {
        return pointerId >= 0 && pointerId < getPointerCount() ? pointerId : -1;
    }

    public float getPressure() {
        return 1.0f;
    }

    public float getPressure(int pointerIndex) {
        return 1.0f;
    }

    public int getHistorySize() {
        return 0;
    }

    public float getHistoricalX(int pos) {
        return getX();
    }

    public float getHistoricalY(int pos) {
        return getY();
    }

    public float getHistoricalX(int pointerIndex, int pos) {
        return getX(pointerIndex);
    }

    public float getHistoricalY(int pointerIndex, int pos) {
        return getY(pointerIndex);
    }

    public float getHistoricalPressure(int pos) {
        return getPressure();
    }

    public float getHistoricalPressure(int pointerIndex, int pos) {
        return getPressure(pointerIndex);
    }

    public long getHistoricalEventTime(int pos) {
        return getEventTime();
    }

    public float getSize() {
        return 1.0f;
    }

    public float getSize(int pointerIndex) {
        return 1.0f;
    }

    public int getToolType(int pointerIndex) {
        return 1; // TOOL_TYPE_FINGER
    }

    public float getAxisValue(int axis) {
        return getAxisValue(axis, 0);
    }

    public float getAxisValue(int axis, int pointerIndex) {
        if (axis == 0) return getX(pointerIndex);
        if (axis == 1) return getY(pointerIndex);
        if (axis == 2) return getPressure(pointerIndex);
        if (axis == 3) return getSize(pointerIndex);
        return 0f;
    }

    public int getMetaState() {
        return 0;
    }

    public int getButtonState() {
        return 0;
    }

    public void recycle() {
    }

    public long getDownTime() {
        return downTime;
    }

    @Override
    public long getEventTime() {
        return eventTime;
    }

    public void offsetLocation(float deltaX, float deltaY) {
        this.x += deltaX;
        this.y += deltaY;
        if (pointerXs != null) {
            for (int i = 0; i < pointerXs.length; i++) pointerXs[i] += deltaX;
        }
        if (pointerYs != null) {
            for (int i = 0; i < pointerYs.length; i++) pointerYs[i] += deltaY;
        }
    }

    public void setLocation(float x, float y) {
        this.x = x;
        this.y = y;
        if (pointerXs != null && pointerXs.length > 0) pointerXs[0] = x;
        if (pointerYs != null && pointerYs.length > 0) pointerYs[0] = y;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(action);
        out.writeFloat(x);
        out.writeFloat(y);
    }
}
