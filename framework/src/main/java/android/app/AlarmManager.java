package android.app;

import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Triển khai tương thích cho {@code android.app.AlarmManager} trên Windows,
 * dùng {@link ScheduledExecutorService} luồng daemon để hẹn giờ {@link PendingIntent}
 * hoặc {@link OnAlarmListener}.
 */
public class AlarmManager {
    public static final String ACTION_NEXT_ALARM_CLOCK_CHANGED =
            "android.app.action.NEXT_ALARM_CLOCK_CHANGED";
    public static final String ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED";

    public static final int RTC_WAKEUP = 0;
    public static final int RTC = 1;
    public static final int ELAPSED_REALTIME_WAKEUP = 2;
    public static final int ELAPSED_REALTIME = 3;

    public static final long INTERVAL_FIFTEEN_MINUTES = 15 * 60 * 1000L;
    public static final long INTERVAL_HALF_HOUR = 30 * 60 * 1000L;
    public static final long INTERVAL_HOUR = 60 * 60 * 1000L;
    public static final long INTERVAL_HALF_DAY = 12 * INTERVAL_HOUR;
    public static final long INTERVAL_DAY = 24 * INTERVAL_HOUR;

    public interface OnAlarmListener {
        void onAlarm();
    }

    public static final class AlarmClockInfo implements Parcelable {
        private final long triggerTime;
        private final PendingIntent showIntent;

        public AlarmClockInfo(long triggerTime, PendingIntent showIntent) {
            this.triggerTime = triggerTime;
            this.showIntent = showIntent;
        }

        public long getTriggerTime() {
            return triggerTime;
        }

        public PendingIntent getShowIntent() {
            return showIntent;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(triggerTime);
            dest.writeParcelable(showIntent, flags);
        }

        public static final Creator<AlarmClockInfo> CREATOR = new Creator<>() {
            @Override
            public AlarmClockInfo createFromParcel(Parcel in) {
                long time = in.readLong();
                PendingIntent intent = in.readParcelable(PendingIntent.class.getClassLoader());
                return new AlarmClockInfo(time, intent);
            }

            @Override
            public AlarmClockInfo[] newArray(int size) {
                return new AlarmClockInfo[size];
            }
        };
    }

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "aaow-alarm-manager");
        t.setDaemon(true);
        return t;
    });
    private final Map<PendingIntent, ScheduledFuture<?>> pendingIntentAlarms = new ConcurrentHashMap<>();
    private final Map<OnAlarmListener, ScheduledFuture<?>> listenerAlarms = new ConcurrentHashMap<>();
    private volatile AlarmClockInfo nextAlarmClock;

    public AlarmManager() {
    }

    public void set(int type, long triggerAtMillis, PendingIntent operation) {
        schedulePendingIntent(type, triggerAtMillis, 0L, operation);
    }

    public void set(int type, long triggerAtMillis, String tag,
                    OnAlarmListener listener, Handler targetHandler) {
        scheduleListener(type, triggerAtMillis, listener, targetHandler, null);
    }

    public void setExact(int type, long triggerAtMillis, PendingIntent operation) {
        schedulePendingIntent(type, triggerAtMillis, 0L, operation);
    }

    public void setExact(int type, long triggerAtMillis, String tag,
                         OnAlarmListener listener, Handler targetHandler) {
        scheduleListener(type, triggerAtMillis, listener, targetHandler, null);
    }

    public void setAndAllowWhileIdle(int type, long triggerAtMillis, PendingIntent operation) {
        schedulePendingIntent(type, triggerAtMillis, 0L, operation);
    }

    public void setExactAndAllowWhileIdle(int type, long triggerAtMillis, PendingIntent operation) {
        schedulePendingIntent(type, triggerAtMillis, 0L, operation);
    }

    public void setExactAndAllowWhileIdle(int type, long triggerAtMillis, String tag,
                                          Executor executor, OnAlarmListener listener) {
        scheduleListener(type, triggerAtMillis, listener, null, executor);
    }

    public void setRepeating(int type, long triggerAtMillis, long intervalMillis, PendingIntent operation) {
        long safeInterval = Math.max(250L, intervalMillis);
        schedulePendingIntent(type, triggerAtMillis, safeInterval, operation);
    }

    public void setInexactRepeating(int type, long triggerAtMillis, long intervalMillis, PendingIntent operation) {
        setRepeating(type, triggerAtMillis, intervalMillis, operation);
    }

    public void setWindow(int type, long windowStartMillis, long windowLengthMillis, PendingIntent operation) {
        schedulePendingIntent(type, windowStartMillis, 0L, operation);
    }

    public void setWindow(int type, long windowStartMillis, long windowLengthMillis, String tag,
                          OnAlarmListener listener, Handler targetHandler) {
        scheduleListener(type, windowStartMillis, listener, targetHandler, null);
    }

    public void setWindow(int type, long windowStartMillis, long windowLengthMillis, String tag,
                          Executor executor, OnAlarmListener listener) {
        scheduleListener(type, windowStartMillis, listener, null, executor);
    }

    public void setAlarmClock(AlarmClockInfo info, PendingIntent operation) {
        this.nextAlarmClock = info;
        if (info != null) {
            schedulePendingIntent(RTC_WAKEUP, info.getTriggerTime(), 0L, operation);
        }
    }

    public AlarmClockInfo getNextAlarmClock() {
        return nextAlarmClock;
    }

    public boolean canScheduleExactAlarms() {
        return true;
    }

    public void setTime(long millis) {
    }

    public void setTimeZone(String timeZone) {
    }

    public void cancel(PendingIntent operation) {
        if (operation == null) return;
        ScheduledFuture<?> future = pendingIntentAlarms.remove(operation);
        if (future != null) {
            future.cancel(false);
        }
    }

    public void cancel(OnAlarmListener listener) {
        if (listener == null) return;
        ScheduledFuture<?> future = listenerAlarms.remove(listener);
        if (future != null) {
            future.cancel(false);
        }
    }

    public void cancelAll() {
        for (ScheduledFuture<?> f : pendingIntentAlarms.values()) {
            f.cancel(false);
        }
        pendingIntentAlarms.clear();
        for (ScheduledFuture<?> f : listenerAlarms.values()) {
            f.cancel(false);
        }
        listenerAlarms.clear();
        nextAlarmClock = null;
    }

    private void schedulePendingIntent(int type, long triggerAtMillis, long intervalMillis,
                                       PendingIntent operation) {
        if (operation == null) return;
        cancel(operation);
        long delayMs = computeDelayMillis(type, triggerAtMillis);
        Runnable fire = () -> {
            try {
                operation.send();
            } catch (Throwable ignored) {
            }
        };
        ScheduledFuture<?> future = intervalMillis > 0
                ? scheduler.scheduleAtFixedRate(fire, delayMs, intervalMillis, TimeUnit.MILLISECONDS)
                : scheduler.schedule(() -> {
                    pendingIntentAlarms.remove(operation);
                    fire.run();
                }, delayMs, TimeUnit.MILLISECONDS);
        pendingIntentAlarms.put(operation, future);
    }

    private void scheduleListener(int type, long triggerAtMillis, OnAlarmListener listener,
                                  Handler targetHandler, Executor executor) {
        if (listener == null) return;
        cancel(listener);
        long delayMs = computeDelayMillis(type, triggerAtMillis);
        Runnable deliver = () -> {
            listenerAlarms.remove(listener);
            if (targetHandler != null) {
                targetHandler.post(listener::onAlarm);
            } else if (executor != null) {
                executor.execute(listener::onAlarm);
            } else {
                listener.onAlarm();
            }
        };
        listenerAlarms.put(listener, scheduler.schedule(deliver, delayMs, TimeUnit.MILLISECONDS));
    }

    private static long computeDelayMillis(int type, long triggerAtMillis) {
        long now = (type == ELAPSED_REALTIME || type == ELAPSED_REALTIME_WAKEUP)
                ? SystemClock.elapsedRealtime()
                : System.currentTimeMillis();
        return Math.max(0L, triggerAtMillis - now);
    }
}
