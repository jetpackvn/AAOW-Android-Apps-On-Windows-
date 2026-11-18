package android.hardware;

public final class Sensor {
    public static final int TYPE_ACCELEROMETER = 1;
    public static final int TYPE_MAGNETIC_FIELD = 2;
    public static final int TYPE_ORIENTATION = 3;
    public static final int TYPE_GYROSCOPE = 4;
    public static final int TYPE_LIGHT = 5;
    public static final int TYPE_PRESSURE = 6;
    public static final int TYPE_GRAVITY = 9;
    public static final int TYPE_LINEAR_ACCELERATION = 10;
    public static final int TYPE_ROTATION_VECTOR = 11;
    public static final int TYPE_ALL = -1;

    public static final String STRING_TYPE_ACCELEROMETER = "android.sensor.accelerometer";
    public static final String STRING_TYPE_MAGNETIC_FIELD = "android.sensor.magnetic_field";
    public static final String STRING_TYPE_ORIENTATION = "android.sensor.orientation";
    public static final String STRING_TYPE_GYROSCOPE = "android.sensor.gyroscope";
    public static final String STRING_TYPE_GRAVITY = "android.sensor.gravity";
    public static final String STRING_TYPE_ROTATION_VECTOR = "android.sensor.rotation_vector";

    private final int type;
    private final String name;
    private final float maximumRange;
    private final float resolution;

    Sensor(int type, String name, float maximumRange, float resolution) {
        this.type = type;
        this.name = name;
        this.maximumRange = maximumRange;
        this.resolution = resolution;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getVendor() {
        return "AAOW Virtual Sensor";
    }

    public int getVersion() {
        return 1;
    }

    public float getMaximumRange() {
        return maximumRange;
    }

    public float getResolution() {
        return resolution;
    }

    public float getPower() {
        return 0.1f;
    }

    public int getMinDelay() {
        return 10000;
    }

    public boolean isWakeUpSensor() {
        return false;
    }

    public boolean isDynamicSensor() {
        return false;
    }
}
