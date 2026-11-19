package android.hardware;

import android.os.Handler;
import android.os.Looper;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cảm biến ảo nhẹ trên Windows phát sự kiện định kỳ (gia tốc/la bàn/góc nghiêng)
 * và hỗ trợ điều chỉnh góc nghiêng ảo để tương tác & kiểm thử ứng dụng cảm biến như Bubble Level.
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

    private record Registration(SensorEventListener listener, Sensor sensor, Handler handler) {
    }

    private final List<Sensor> sensors = List.of(
            new Sensor(Sensor.TYPE_ACCELEROMETER, "AAOW Virtual Accelerometer", 19.6f, 0.01f),
            new Sensor(Sensor.TYPE_MAGNETIC_FIELD, "AAOW Virtual Magnetometer", 100f, 0.1f),
            new Sensor(Sensor.TYPE_ORIENTATION, "AAOW Virtual Orientation", 360f, 0.1f),
            new Sensor(Sensor.TYPE_GRAVITY, "AAOW Virtual Gravity", 19.6f, 0.01f),
            new Sensor(Sensor.TYPE_ROTATION_VECTOR, "AAOW Virtual Rotation Vector", 1f, 0.001f)
    );

    private final List<Registration> activeRegistrations = new CopyOnWriteArrayList<>();
    private volatile float virtualAccelX = 0.25f;
    private volatile float virtualAccelY = -0.15f;
    private volatile float virtualAccelZ = GRAVITY_EARTH;
    private Timer pollTimer;

    public void setVirtualGravity(float gx, float gy, float gz) {
        this.virtualAccelX = gx;
        this.virtualAccelY = gy;
        this.virtualAccelZ = gz;
        dispatchAllNow();
    }

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
        Registration reg = new Registration(listener, sensor, h);
        activeRegistrations.add(reg);
        h.post(() -> {
            try {
                listener.onAccuracyChanged(sensor, SENSOR_STATUS_ACCURACY_HIGH);
                listener.onSensorChanged(buildEvent(sensor));
            } catch (Throwable ignored) {
            }
        });
        ensureTimerRunning();
        return true;
    }

    public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, int maxReportLatencyUs, Handler handler) {
        return registerListener(listener, sensor, samplingPeriodUs, handler);
    }

    public void unregisterListener(SensorEventListener listener) {
        if (listener == null) return;
        activeRegistrations.removeIf(r -> r.listener == listener);
        stopTimerIfIdle();
    }

    public void unregisterListener(SensorEventListener listener, Sensor sensor) {
        if (listener == null) return;
        activeRegistrations.removeIf(r -> r.listener == listener && (sensor == null || r.sensor == sensor));
        stopTimerIfIdle();
    }

    private synchronized void ensureTimerRunning() {
        if (pollTimer == null) {
            pollTimer = new Timer(80, e -> dispatchAllNow());
            pollTimer.setRepeats(true);
            pollTimer.start();
        } else if (!pollTimer.isRunning()) {
            pollTimer.start();
        }
    }

    private synchronized void stopTimerIfIdle() {
        if (activeRegistrations.isEmpty() && pollTimer != null) {
            pollTimer.stop();
        }
    }

    private void dispatchAllNow() {
        for (Registration reg : activeRegistrations) {
            reg.handler.post(() -> {
                if (!activeRegistrations.contains(reg)) return;
                try {
                    reg.listener.onSensorChanged(buildEvent(reg.sensor));
                } catch (Throwable ignored) {
                }
            });
        }
    }

    private SensorEvent buildEvent(Sensor sensor) {
        SensorEvent ev = new SensorEvent(3);
        ev.sensor = sensor;
        ev.accuracy = SENSOR_STATUS_ACCURACY_HIGH;
        ev.timestamp = System.nanoTime();
        switch (sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY -> {
                ev.values[0] = virtualAccelX;
                ev.values[1] = virtualAccelY;
                ev.values[2] = virtualAccelZ;
            }
            case Sensor.TYPE_MAGNETIC_FIELD -> {
                ev.values[0] = 0f;
                ev.values[1] = 25f;
                ev.values[2] = -40f;
            }
            case Sensor.TYPE_ORIENTATION -> {
                float pitch = (float) Math.toDegrees(Math.atan2(virtualAccelY, Math.sqrt(virtualAccelX * virtualAccelX + virtualAccelZ * virtualAccelZ)));
                float roll = (float) Math.toDegrees(Math.atan2(virtualAccelX, virtualAccelZ));
                ev.values[0] = 0f;
                ev.values[1] = pitch;
                ev.values[2] = roll;
            }
            default -> {
                ev.values[0] = 0f;
                ev.values[1] = 0f;
                ev.values[2] = 0f;
            }
        }
        return ev;
    }

    public static boolean getRotationMatrix(float[] R, float[] I, float[] gravity, float[] geomagnetic) {
        if (R != null && R.length >= 9) {
            float ax = (gravity != null && gravity.length >= 3) ? gravity[0] : 0f;
            float ay = (gravity != null && gravity.length >= 3) ? gravity[1] : 0f;
            float az = (gravity != null && gravity.length >= 3) ? gravity[2] : GRAVITY_EARTH;
            float ex = (geomagnetic != null && geomagnetic.length >= 3) ? geomagnetic[0] : 0f;
            float ey = (geomagnetic != null && geomagnetic.length >= 3) ? geomagnetic[1] : 25f;
            float ez = (geomagnetic != null && geomagnetic.length >= 3) ? geomagnetic[2] : -40f;

            float hx = ey * az - ez * ay;
            float hy = ez * ax - ex * az;
            float hz = ex * ay - ey * ax;
            float normH = (float) Math.sqrt(hx * hx + hy * hy + hz * hz);
            if (normH < 0.1f) {
                R[0] = 1; R[1] = 0; R[2] = 0;
                R[3] = 0; R[4] = 1; R[5] = 0;
                R[6] = 0; R[7] = 0; R[8] = 1;
                if (R.length == 16) {
                    R[10] = 1; R[15] = 1;
                }
                return true;
            }
            float invH = 1.0f / normH;
            hx *= invH; hy *= invH; hz *= invH;
            float invA = 1.0f / (float) Math.sqrt(ax * ax + ay * ay + az * az);
            ax *= invA; ay *= invA; az *= invA;
            float mx = ay * hz - az * hy;
            float my = az * hx - ax * hz;
            float mz = ax * hy - ay * hx;
            if (R.length == 9) {
                R[0] = hx; R[1] = hy; R[2] = hz;
                R[3] = mx; R[4] = my; R[5] = mz;
                R[6] = ax; R[7] = ay; R[8] = az;
            } else if (R.length == 16) {
                R[0] = hx; R[1] = hy; R[2] = hz; R[3] = 0;
                R[4] = mx; R[5] = my; R[6] = mz; R[7] = 0;
                R[8] = ax; R[9] = ay; R[10] = az; R[11] = 0;
                R[12] = 0; R[13] = 0; R[14] = 0; R[15] = 1;
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
        if (R != null && values != null && values.length >= 3) {
            if (R.length == 9) {
                values[0] = (float) Math.atan2(R[1], R[4]);
                values[1] = (float) Math.asin(-Math.max(-1f, Math.min(1f, R[7])));
                values[2] = (float) Math.atan2(-R[6], R[8]);
            } else if (R.length >= 16) {
                values[0] = (float) Math.atan2(R[1], R[5]);
                values[1] = (float) Math.asin(-Math.max(-1f, Math.min(1f, R[9])));
                values[2] = (float) Math.atan2(-R[8], R[10]);
            }
        }
        return values;
    }
}
