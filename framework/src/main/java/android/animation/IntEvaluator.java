package android.animation;

public class IntEvaluator implements TypeEvaluator<Integer> {
    @Override
    public Integer evaluate(float fraction, Integer startValue, Integer endValue) {
        int start = startValue;
        return (int) (start + fraction * (endValue - start));
    }
}
