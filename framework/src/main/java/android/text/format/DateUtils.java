package android.text.format;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

/** Tiện ích định dạng ngày giờ tương đối và khoảng thời gian android.text.format.DateUtils. */
public class DateUtils {
    public static final long SECOND_IN_MILLIS = 1000L;
    public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;
    public static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60;
    public static final long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24;
    public static final long WEEK_IN_MILLIS = DAY_IN_MILLIS * 7;
    public static final long YEAR_IN_MILLIS = WEEK_IN_MILLIS * 52;

    public static final int FORMAT_SHOW_TIME = 0x00001;
    public static final int FORMAT_SHOW_WEEKDAY = 0x00002;
    public static final int FORMAT_SHOW_YEAR = 0x00004;
    public static final int FORMAT_NO_YEAR = 0x00008;
    public static final int FORMAT_SHOW_DATE = 0x00010;
    public static final int FORMAT_NO_MONTH_DAY = 0x00020;
    public static final int FORMAT_12HOUR = 0x00040;
    public static final int FORMAT_24HOUR = 0x00080;
    public static final int FORMAT_CAP_AMPM = 0x00100;
    public static final int FORMAT_NO_NOON = 0x00200;
    public static final int FORMAT_CAP_NOON = 0x00400;
    public static final int FORMAT_NO_MIDNIGHT = 0x00800;
    public static final int FORMAT_CAP_MIDNIGHT = 0x01000;
    public static final int FORMAT_UTC = 0x02000;
    public static final int FORMAT_ABBREV_TIME = 0x04000;
    public static final int FORMAT_ABBREV_WEEKDAY = 0x08000;
    public static final int FORMAT_ABBREV_MONTH = 0x10000;
    public static final int FORMAT_NUMERIC_DATE = 0x20000;
    public static final int FORMAT_ABBREV_RELATIVE = 0x40000;
    public static final int FORMAT_ABBREV_ALL = 0x80000;

    public DateUtils() {
    }

    public static boolean isToday(long when) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(when);
        return now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR);
    }

    public static String formatElapsedTime(long elapsedSeconds) {
        return formatElapsedTime(null, elapsedSeconds);
    }

    public static String formatElapsedTime(StringBuilder recycle, long elapsedSeconds) {
        long s = Math.max(0L, elapsedSeconds);
        long hours = s / 3600;
        long minutes = (s % 3600) / 60;
        long seconds = s % 60;
        StringBuilder sb = recycle != null ? recycle : new StringBuilder(8);
        sb.setLength(0);
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    public static String formatDateTime(Context context, long millis, int flags) {
        return new SimpleDateFormat("EEE, MMM d, HH:mm", Locale.getDefault()).format(new Date(millis));
    }

    public static String formatDateRange(Context context, long startMillis, long endMillis, int flags) {
        return formatDateTime(context, startMillis, flags);
    }

    public static Formatter formatDateRange(Context context, Formatter formatter, long startMillis, long endMillis, int flags) {
        return formatDateRange(context, formatter, startMillis, endMillis, flags, null);
    }

    public static Formatter formatDateRange(Context context, Formatter formatter, long startMillis, long endMillis, int flags, String timeZone) {
        Formatter f = formatter != null ? formatter : new Formatter(new StringBuilder(), Locale.getDefault());
        f.format("%s", formatDateTime(context, startMillis, flags));
        return f;
    }

    public static CharSequence getRelativeTimeSpanString(long startTime) {
        return getRelativeTimeSpanString(startTime, System.currentTimeMillis(), MINUTE_IN_MILLIS);
    }

    public static CharSequence getRelativeTimeSpanString(long time, long now, long minResolution) {
        long diff = Math.abs(now - time);
        if (diff < MINUTE_IN_MILLIS) return "Just now";
        if (diff < HOUR_IN_MILLIS) return (diff / MINUTE_IN_MILLIS) + " min. ago";
        if (diff < DAY_IN_MILLIS) return (diff / HOUR_IN_MILLIS) + " hr. ago";
        return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(new Date(time));
    }

    public static CharSequence getRelativeTimeSpanString(long time, long now, long minResolution, int flags) {
        return getRelativeTimeSpanString(time, now, minResolution);
    }
}
