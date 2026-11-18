package android.hardware;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cảm biến ảo nhẹ trên Windows cho các ứng dụng đọc gia tốc/la bàn/góc nghiêng mà không lỗi NPE.
 */
public class SensorManager {
    public static final int SENSOR_DELAY_FASTEST = 0;
    public static final int SENSOR_DELAY_GAME = 1;
    public static final int SENSOR_DELAY_UI = 2;
    public static final int SENSOR_DELAY_NORMAL = 3;

    public static final int SENSOR_STATUS_NO_CONTACT = -1;
    public static final int SENSOR_STATUS_UNRELIABLE = 0;
    public static final int SENSOR_STATUS_ACCURACY_LOW = 1;
    public static final int SENSOR_STATUS_ACCURACY_MEDIUM = 2;
    public static final int SENSOR_STATUS_ACCURACY_HIGH = 3;

    public static final int AXIS_X = 1;
    public static final int AXIS_Y = 2;
    public static final int AXIS_Z = 3;
    public static final int AXIS_MINUS_X = AXIS_X | 0x80;
    public static final int AXIS_MINUS_Y = AXIS_Y | 0x80;
    public static final int AXIS_MINUS_Z = AXIS_Z | 0x80;

    public static final float GRAVITY_EARTH = 9.80665f;

    private final List<Sensor> sensors = List.of(
            new Sensor(Sensor.TYPE_ACCELEROMETER, "AAOW Virtual Accelerometer", 19.6f, 0.01f),
            new Sensor(Sensor.TYPE_MAGNETIC_FIELD, "AAOW Virtual Magnetometer", 100f, 0.1f),
            new Sensor(Sensor.TYPE_ORIENTATION, "AAOW Virtual Orientation", 360f, 0.1f),
            new Sensor(Sensor.TYPE_GRAVITY, "AAOW Virtual Gravity", 19.6f, 0.01f),
            new Sensor(Sensor.TYPE_ROTATION_VECTOR, "AAOW Virtual Rotation Vector", 1f, 0.001f)
    );

    public Sensor getDefaultSensor(int type) {
        for (Sensor s : sensors) {
            if (s.getType() == type) return s;
        }
        return null;
    }

    public Sensor getDefaultSensor(int type, boolean wakeUp) {
        return getDefaultSensor(type);
    }

    public List<Sensor> getSensorList(int type) {
        if (type == Sensor.TYPE_ALL) {
            return Collections.unmodifiableList(sensors);
        }
        List<Sensor> out = new ArrayList<>();
        for (Sensor s : sensors) {
            if (s.getType() == type) out.add(s);
        }
        return out;
    }

    public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs) {
        return registerListener(listener, sensor, samplingPeriodUs, null);
    }

    public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, int maxReportLatencyUs) {
        return registerListener(listener, sensor, samplingPeriodUs, null);
    }

    public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, Handler handler) {
        if (listener == null || sensor == null) return false;
        Handler h = handler != null ? handler : new Handler(Looper.getMainLooper());
        h.post(() -> {
            SensorEvent ev = new SensorEvent(3);
            ev.sensor = sensor;
            ev.accuracy = SENSOR_STATUS_ACCURACY_HIGH;
            ev.timestamp = System.nanoTime();
            switch (sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY -> {
                    ev.values[0] = 0f;
                    ev.values[1] = 0f;
                    ev.values[2] = GRAVITY_EARTH;
                }
                case Sensor.TYPE_MAGNETIC_FIELD -> {
                    ev.values[0] = 0f;
                    ev.values[1] = 25f;
                    ev.values[2] = -40f;
                }
                default -> {
                    ev.values[0] = 0f;
                    ev.values[1] = 0f;
                    ev.values[2] = 0f;
                }
            }
            listener.onAccuracyChanged(sensor, SENSOR_STATUS_ACCURACY_HIGH);
            listener.onSensorChanged(ev);
        });
        return true;
    }

    public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, int maxReportLatencyUs, Handler handler) {
        return registerListener(listener, sensor, samplingPeriodUs, handler);
    }

    public void unregisterListener(SensorEventListener listener) {
    }

    public void unregisterListener(SensorEventListener listener, Sensor sensor) {
    }

    public static boolean getRotationMatrix(float[] R, float[] I, float[] gravity, float[] geomagnetic) {
        if (R != null && R.length >= 9) {
            R[0] = 1; R[1] = 0; R[2] = 0;
            R[3] = 0; R[4] = 1; R[5] = 0;
            R[6] = 0; R[7] = 0; R[8] = 1;
            if (R.length == 16) {
                R[10] = 1; R[15] = 1;
            }
        }
        return true;
    }

    public static boolean remapCoordinateSystem(float[] inR, int X, int Y, float[] outR) {
        if (inR != null && outR != null && inR.length == outR.length) {
            System.arraycopy(inR, 0, outR, 0, inR.length);
            return true;
        }
        return false;
    }

    public static float[] getOrientation(float[] R, float[] values) {
        if (values != null && values.length >= 3) {
            values[0] = 0f;
            values[1] = 0f;
            values[2] = 0f;
        }
        return values;
    }
}
