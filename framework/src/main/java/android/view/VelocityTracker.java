package android.view;

public final class VelocityTracker {
    private float prevX, prevY;
    private long prevTime;
    private boolean hasPrev;
    private float vx, vy;

    private VelocityTracker() {
    }

    public static VelocityTracker obtain() {
        return new VelocityTracker();
    }

    public void recycle() {
        clear();
    }

    public void clear() {
        hasPrev = false;
        vx = 0f;
        vy = 0f;
    }

    public void addMovement(MotionEvent event) {
        if (event == null) return;
        float x = event.getX();
        float y = event.getY();
        long t = event.getEventTime();
        if (hasPrev && t > prevTime) {
            float dtSec = (t - prevTime) / 1000f;
            vx = (x - prevX) / dtSec;
            vy = (y - prevY) / dtSec;
        }
        prevX = x;
        prevY = y;
        prevTime = t;
        hasPrev = true;
    }

    public void computeCurrentVelocity(int units) {
        computeCurrentVelocity(units, Float.MAX_VALUE);
    }

    public void computeCurrentVelocity(int units, float maxVelocity) {
        float scale = units / 1000f;
        vx = clamp(vx * scale, maxVelocity);
        vy = clamp(vy * scale, maxVelocity);
    }

    private static float clamp(float val, float max) {
        if (val > max) return max;
        if (val < -max) return -max;
        return val;
    }

    public float getXVelocity() {
        return vx;
    }

    public float getYVelocity() {
        return vy;
    }

    public float getXVelocity(int id) {
        return vx;
    }

    public float getYVelocity(int id) {
        return vy;
    }
}
