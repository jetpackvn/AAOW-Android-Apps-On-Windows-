package android.animation;

public class ArgbEvaluator implements TypeEvaluator<Object> {
    private static final ArgbEvaluator INSTANCE = new ArgbEvaluator();

    public static ArgbEvaluator getInstance() {
        return INSTANCE;
    }

    @Override
    public Object evaluate(float fraction, Object startValue, Object endValue) {
        int startInt = (Integer) startValue;
        int endInt = (Integer) endValue;
        int startA = (startInt >>> 24) & 0xff;
        int startR = (startInt >>> 16) & 0xff;
        int startG = (startInt >>> 8) & 0xff;
        int startB = startInt & 0xff;

        int endA = (endInt >>> 24) & 0xff;
        int endR = (endInt >>> 16) & 0xff;
        int endG = (endInt >>> 8) & 0xff;
        int endB = endInt & 0xff;

        int a = startA + Math.round(fraction * (endA - startA));
        int r = startR + Math.round(fraction * (endR - startR));
        int g = startG + Math.round(fraction * (endG - startG));
        int b = startB + Math.round(fraction * (endB - startB));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
