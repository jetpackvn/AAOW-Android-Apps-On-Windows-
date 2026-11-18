package android.view;

public final class MotionEvent {
    public static final int ACTION_MASK = 0xff;
    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP = 1;
    public static final int ACTION_MOVE = 2;
    public static final int ACTION_CANCEL = 3;
    public static final int ACTION_OUTSIDE = 4;
    public static final int ACTION_SCROLL = 8;

    private int action;
    private float x;
    private float y;
    private final float rawX;
    private final float rawY;
    private final long downTime;
    private final long eventTime;

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

    public int getAction() {
        return action;
    }

    public int getActionMasked() {
        return action & ACTION_MASK;
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
        return x;
    }

    public float getY(int pointerIndex) {
        return y;
    }

    public float getRawX() {
        return rawX;
    }

    public float getRawY() {
        return rawY;
    }

    public int getPointerCount() {
        return 1;
    }

    public long getDownTime() {
        return downTime;
    }

    public long getEventTime() {
        return eventTime;
    }

    public void offsetLocation(float deltaX, float deltaY) {
        this.x += deltaX;
        this.y += deltaY;
    }

    public void setLocation(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void recycle() {
    }
}
