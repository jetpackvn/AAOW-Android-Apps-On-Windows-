package android.animation;

import android.util.Property;

public class PropertyValuesHolder implements Cloneable {
    private String propertyName;
    private Property<?, ?> property;
    private TypeEvaluator<?> evaluator;
    private Object[] values = new Object[0];
    private Object animatedValue;

    private PropertyValuesHolder(String propertyName) {
        this.propertyName = propertyName;
    }

    private PropertyValuesHolder(Property<?, ?> property) {
        this.property = property;
        this.propertyName = property != null ? property.getName() : null;
    }

    public static PropertyValuesHolder ofFloat(String propertyName, float... values) {
        PropertyValuesHolder h = new PropertyValuesHolder(propertyName);
        h.setFloatValues(values);
        return h;
    }

    public static PropertyValuesHolder ofFloat(Property<?, Float> property, float... values) {
        PropertyValuesHolder h = new PropertyValuesHolder(property);
        h.setFloatValues(values);
        return h;
    }

    public static PropertyValuesHolder ofInt(String propertyName, int... values) {
        PropertyValuesHolder h = new PropertyValuesHolder(propertyName);
        h.setIntValues(values);
        return h;
    }

    public static PropertyValuesHolder ofInt(Property<?, Integer> property, int... values) {
        PropertyValuesHolder h = new PropertyValuesHolder(property);
        h.setIntValues(values);
        return h;
    }

    public static <V> PropertyValuesHolder ofObject(String propertyName, TypeEvaluator<V> evaluator, V... values) {
        PropertyValuesHolder h = new PropertyValuesHolder(propertyName);
        h.setObjectValues(values);
        h.setEvaluator(evaluator);
        return h;
    }

    public static <T, V> PropertyValuesHolder ofObject(Property<T, V> property, TypeEvaluator<V> evaluator, V... values) {
        PropertyValuesHolder h = new PropertyValuesHolder(property);
        h.setObjectValues(values);
        h.setEvaluator(evaluator);
        return h;
    }

    public void setFloatValues(float... values) {
        if (values == null || values.length == 0) {
            this.values = new Object[]{0f};
        } else {
            this.values = new Object[values.length];
            for (int i = 0; i < values.length; i++) this.values[i] = values[i];
        }
        this.evaluator = new FloatEvaluator();
    }

    public void setIntValues(int... values) {
        if (values == null || values.length == 0) {
            this.values = new Object[]{0};
        } else {
            this.values = new Object[values.length];
            for (int i = 0; i < values.length; i++) this.values[i] = values[i];
        }
        this.evaluator = new IntEvaluator();
    }

    public void setObjectValues(Object... values) {
        this.values = values != null ? values.clone() : new Object[0];
    }

    public void setEvaluator(TypeEvaluator<?> evaluator) {
        if (evaluator != null) this.evaluator = evaluator;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setProperty(Property<?, ?> property) {
        this.property = property;
        if (property != null) this.propertyName = property.getName();
    }

    public Property<?, ?> getProperty() {
        return property;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    void calculateValue(float fraction) {
        if (values == null || values.length == 0) {
            animatedValue = null;
            return;
        }
        if (values.length == 1) {
            animatedValue = values[0];
            return;
        }
        float clamped = Math.max(0f, Math.min(1f, fraction));
        int segments = values.length - 1;
        float scaled = clamped * segments;
        int idx = Math.min(segments - 1, (int) Math.floor(scaled));
        float localFrac = scaled - idx;
        Object start = values[idx];
        Object end = values[idx + 1];
        if (evaluator != null) {
            animatedValue = ((TypeEvaluator) evaluator).evaluate(localFrac, start, end);
        } else if (start instanceof Float sf && end instanceof Float ef) {
            animatedValue = sf + localFrac * (ef - sf);
        } else if (start instanceof Integer si && end instanceof Integer ei) {
            animatedValue = Math.round(si + localFrac * (ei - si));
        } else {
            animatedValue = localFrac < 0.5f ? start : end;
        }
    }

    Object getAnimatedValue() {
        return animatedValue != null ? animatedValue : (values.length > 0 ? values[0] : null);
    }

    void setupStartValue(Object target) {
        if (values.length == 1 && target != null) {
            Object current = readCurrentValue(target);
            if (current != null) {
                values = new Object[]{current, values[0]};
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object readCurrentValue(Object target) {
        if (property != null) {
            try {
                return ((Property) property).get(target);
            } catch (Exception ignored) {
            }
        }
        if (propertyName != null && !propertyName.isEmpty()) {
            String cap = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            try {
                return target.getClass().getMethod("get" + cap).invoke(target);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    void setAnimatedValue(Object target) {
        if (target == null) return;
        Object val = getAnimatedValue();
        if (val == null) return;
        if (property != null) {
            try {
                ((Property) property).set(target, val);
                return;
            } catch (Exception ignored) {
            }
        }
        if (propertyName != null && !propertyName.isEmpty()) {
            String cap = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            String setter = "set" + cap;
            for (var m : target.getClass().getMethods()) {
                if (setter.equals(m.getName()) && m.getParameterCount() == 1) {
                    try {
                        Class<?> pt = m.getParameterTypes()[0];
                        if ((pt == float.class || pt == Float.class) && val instanceof Number n) {
                            m.invoke(target, n.floatValue());
                            return;
                        } else if ((pt == int.class || pt == Integer.class) && val instanceof Number n) {
                            m.invoke(target, n.intValue());
                            return;
                        } else if (pt.isInstance(val)) {
                            m.invoke(target, val);
                            return;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    @Override
    public PropertyValuesHolder clone() {
        try {
            PropertyValuesHolder copy = (PropertyValuesHolder) super.clone();
            copy.values = this.values.clone();
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
