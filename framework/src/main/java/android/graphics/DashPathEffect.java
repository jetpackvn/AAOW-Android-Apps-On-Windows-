package android.graphics;

/** Hiệu ứng đường nét đứt (dash intervals & phase) áp dụng cho Paint. */
public class DashPathEffect extends PathEffect {
    private final float[] intervals;
    private final float phase;

    public DashPathEffect(float[] intervals, float phase) {
        if (intervals == null || intervals.length < 2) {
            throw new ArrayIndexOutOfBoundsException();
        }
        this.intervals = intervals.clone();
        this.phase = Math.max(0f, phase);
    }

    public float[] getIntervals() {
        return intervals.clone();
    }

    public float getPhase() {
        return phase;
    }
}
