package android.text.format;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Cài đặt thật android.text.format.DateFormat cung cấp các bộ định dạng ngày/giờ chuẩn
 * cho ứng dụng thời tiết, tin tức, lịch và ghi chú.
 */
public class DateFormat {
    public DateFormat() {
    }

    public static boolean is24HourFormat(Context context) {
        return true;
    }

    public static java.text.DateFormat getTimeFormat(Context context) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    public static java.text.DateFormat getDateFormat(Context context) {
        return java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, Locale.getDefault());
    }

    public static java.text.DateFormat getLongDateFormat(Context context) {
        return java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG, Locale.getDefault());
    }

    public static java.text.DateFormat getMediumDateFormat(Context context) {
        return java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.getDefault());
    }

    public static char[] getDateFormatOrder(Context context) {
        return new char[]{'d', 'M', 'y'};
    }

    public static String getBestDateTimePattern(Locale locale, String skeleton) {
        if (skeleton == null || skeleton.isEmpty()) return "yyyy-MM-dd HH:mm";
        return skeleton;
    }

    public static CharSequence format(CharSequence inFormat, long inTimeInMillis) {
        return format(inFormat, new Date(inTimeInMillis));
    }

    public static CharSequence format(CharSequence inFormat, Date inDate) {
        if (inFormat == null || inDate == null) return "";
        try {
            String pattern = inFormat.toString().replace('k', 'H');
            return new SimpleDateFormat(pattern, Locale.getDefault()).format(inDate);
        } catch (Exception e) {
            return java.text.DateFormat.getDateTimeInstance().format(inDate);
        }
    }

    public static CharSequence format(CharSequence inFormat, Calendar inDate) {
        return inDate != null ? format(inFormat, inDate.getTime()) : "";
    }
}
