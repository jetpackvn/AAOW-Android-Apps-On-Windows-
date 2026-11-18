package android.util;

public abstract class Property<T, V> {
    private final Class<V> type;
    private final String name;

    public Property(Class<V> type, String name) {
        this.type = type;
        this.name = name;
    }

    public static <T, V> Property<T, V> of(Class<T> hostType, Class<V> valueType, String name) {
        return new ReflectiveProperty<>(hostType, valueType, name);
    }

    public boolean isReadOnly() {
        return false;
    }

    public void set(T object, V value) {
        throw new UnsupportedOperationException("Property " + getName() + " is read-only");
    }

    public abstract V get(T object);

    public String getName() {
        return name;
    }

    public Class<V> getType() {
        return type;
    }

    private static final class ReflectiveProperty<T, V> extends Property<T, V> {
        private final Class<T> hostType;

        ReflectiveProperty(Class<T> hostType, Class<V> valueType, String name) {
            super(valueType, name);
            this.hostType = hostType;
        }

        @Override
        @SuppressWarnings("unchecked")
        public V get(T object) {
            if (object == null || getName() == null || getName().isEmpty()) return null;
            String cap = Character.toUpperCase(getName().charAt(0)) + getName().substring(1);
            try {
                var m = hostType.getMethod("get" + cap);
                return (V) m.invoke(object);
            } catch (Exception e) {
                try {
                    var f = hostType.getField(getName());
                    return (V) f.get(object);
                } catch (Exception ignored) {
                    return null;
                }
            }
        }

        @Override
        public void set(T object, V value) {
            if (object == null || getName() == null || getName().isEmpty()) return;
            String cap = Character.toUpperCase(getName().charAt(0)) + getName().substring(1);
            String setter = "set" + cap;
            for (var m : hostType.getMethods()) {
                if (setter.equals(m.getName()) && m.getParameterCount() == 1) {
                    try {
                        m.invoke(object, value);
                        return;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }
}
