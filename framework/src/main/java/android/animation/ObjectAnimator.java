package android.animation;

import android.util.Property;

import java.lang.ref.WeakReference;

public final class ObjectAnimator extends ValueAnimator {
    private WeakReference<Object> targetRef;
    private String propertyName;
    private Property<?, ?> property;
    private boolean autoCancel;

    public ObjectAnimator() {
    }

    private ObjectAnimator(Object target, String propertyName) {
        setTarget(target);
        setPropertyName(propertyName);
    }

    private <T> ObjectAnimator(T target, Property<T, ?> property) {
        setTarget(target);
        setProperty(property);
    }

    public static ObjectAnimator ofFloat(Object target, String propertyName, float... values) {
        ObjectAnimator anim = new ObjectAnimator(target, propertyName);
        anim.setFloatValues(values);
        return anim;
    }

    public static <T> ObjectAnimator ofFloat(T target, Property<T, Float> property, float... values) {
        ObjectAnimator anim = new ObjectAnimator(target, property);
        anim.setFloatValues(values);
        return anim;
    }

    public static ObjectAnimator ofInt(Object target, String propertyName, int... values) {
        ObjectAnimator anim = new ObjectAnimator(target, propertyName);
        anim.setIntValues(values);
        return anim;
    }

    public static <T> ObjectAnimator ofInt(T target, Property<T, Integer> property, int... values) {
        ObjectAnimator anim = new ObjectAnimator(target, property);
        anim.setIntValues(values);
        return anim;
    }

    public static ObjectAnimator ofArgb(Object target, String propertyName, int... values) {
        ObjectAnimator anim = ofInt(target, propertyName, values);
        anim.setEvaluator(ArgbEvaluator.getInstance());
        return anim;
    }

    public static <T> ObjectAnimator ofArgb(T target, Property<T, Integer> property, int... values) {
        ObjectAnimator anim = ofInt(target, property, values);
        anim.setEvaluator(ArgbEvaluator.getInstance());
        return anim;
    }

    @SafeVarargs
    public static <V> ObjectAnimator ofObject(Object target, String propertyName,
                                              TypeEvaluator<V> evaluator, V... values) {
        ObjectAnimator anim = new ObjectAnimator(target, propertyName);
        anim.setObjectValues((Object[]) values);
        anim.setEvaluator(evaluator);
        return anim;
    }

    @SafeVarargs
    public static <T, V> ObjectAnimator ofObject(T target, Property<T, V> property,
                                                 TypeEvaluator<V> evaluator, V... values) {
        ObjectAnimator anim = new ObjectAnimator(target, property);
        anim.setObjectValues((Object[]) values);
        anim.setEvaluator(evaluator);
        return anim;
    }

    public static ObjectAnimator ofPropertyValuesHolder(Object target, PropertyValuesHolder... values) {
        ObjectAnimator anim = new ObjectAnimator();
        anim.setTarget(target);
        anim.setValues(values);
        return anim;
    }

    @Override
    public void setTarget(Object target) {
        this.targetRef = target != null ? new WeakReference<>(target) : null;
    }

    public Object getTarget() {
        return targetRef != null ? targetRef.get() : null;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
        PropertyValuesHolder[] cur = getValues();
        if (cur.length > 0) {
            cur[0].setPropertyName(propertyName);
        }
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setProperty(Property<?, ?> property) {
        this.property = property;
        this.propertyName = property != null ? property.getName() : null;
        PropertyValuesHolder[] cur = getValues();
        if (cur.length > 0) {
            cur[0].setProperty(property);
        }
    }

    @Override
    public void setFloatValues(float... values) {
        if (property != null) {
            @SuppressWarnings("unchecked")
            Property<?, Float> fp = (Property<?, Float>) property;
            setValues(PropertyValuesHolder.ofFloat(fp, values));
        } else {
            setValues(PropertyValuesHolder.ofFloat(propertyName, values));
        }
    }

    @Override
    public void setIntValues(int... values) {
        if (property != null) {
            @SuppressWarnings("unchecked")
            Property<?, Integer> ip = (Property<?, Integer>) property;
            setValues(PropertyValuesHolder.ofInt(ip, values));
        } else {
            setValues(PropertyValuesHolder.ofInt(propertyName, values));
        }
    }

    @Override
    public void setObjectValues(Object... values) {
        if (property != null) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            PropertyValuesHolder h = PropertyValuesHolder.ofObject((Property) property, null, values);
            setValues(h);
        } else {
            setValues(PropertyValuesHolder.ofObject(propertyName, null, values));
        }
    }

    public void setAutoCancel(boolean cancel) {
        this.autoCancel = cancel;
    }

    @Override
    public ObjectAnimator setDuration(long duration) {
        super.setDuration(duration);
        return this;
    }

    @Override
    public void setupStartValues() {
        Object target = getTarget();
        if (target == null) return;
        for (PropertyValuesHolder h : getValues()) {
            h.setupStartValue(target);
        }
    }

    @Override
    protected void animateValue(float fraction) {
        super.animateValue(fraction);
        Object target = getTarget();
        if (target != null) {
            for (PropertyValuesHolder h : getValues()) {
                h.setAnimatedValue(target);
            }
        }
    }

    @Override
    public ObjectAnimator clone() {
        return (ObjectAnimator) super.clone();
    }
}
