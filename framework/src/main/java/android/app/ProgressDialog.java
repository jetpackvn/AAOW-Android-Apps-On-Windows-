package android.app;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ProgressBar;

import java.text.NumberFormat;

/**
 * Cài đặt thật android.app.ProgressDialog (kế thừa AlertDialog) dùng trong các ứng dụng tải dữ liệu mạng
 * (như Forecastie Weather, RSS readers...).
 */
public class ProgressDialog extends AlertDialog {
    public static final int STYLE_SPINNER = 0;
    public static final int STYLE_HORIZONTAL = 1;

    private int progressStyle = STYLE_SPINNER;
    private int max = 100;
    private int progressVal = 0;
    private int secondaryProgressVal = 0;
    private boolean indeterminate = true;
    private String progressNumberFormat = "%1d/%2d";
    private NumberFormat progressPercentFormat = NumberFormat.getPercentInstance();

    public ProgressDialog(Context context) {
        super(context);
    }

    public ProgressDialog(Context context, int theme) {
        super(context, theme);
    }

    public static ProgressDialog show(Context context, CharSequence title, CharSequence message) {
        return show(context, title, message, false);
    }

    public static ProgressDialog show(Context context, CharSequence title,
                                      CharSequence message, boolean indeterminate) {
        return show(context, title, message, indeterminate, false, null);
    }

    public static ProgressDialog show(Context context, CharSequence title,
                                      CharSequence message, boolean indeterminate, boolean cancelable) {
        return show(context, title, message, indeterminate, cancelable, null);
    }

    public static ProgressDialog show(Context context, CharSequence title,
                                      CharSequence message, boolean indeterminate,
                                      boolean cancelable, OnCancelListener cancelListener) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setIndeterminate(indeterminate);
        dialog.setCancelable(cancelable);
        dialog.setOnCancelListener(cancelListener);
        dialog.show();
        return dialog;
    }

    public void setProgress(int value) {
        this.progressVal = value;
    }

    public void setSecondaryProgress(int secondaryProgress) {
        this.secondaryProgressVal = secondaryProgress;
    }

    public int getProgress() {
        return progressVal;
    }

    public int getSecondaryProgress() {
        return secondaryProgressVal;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public void incrementProgressBy(int diff) {
        this.progressVal += diff;
    }

    public void incrementSecondaryProgressBy(int diff) {
        this.secondaryProgressVal += diff;
    }

    public void setProgressDrawable(Drawable d) {
    }

    public void setIndeterminateDrawable(Drawable d) {
    }

    public void setIndeterminate(boolean indeterminate) {
        this.indeterminate = indeterminate;
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    public void setProgressStyle(int style) {
        this.progressStyle = style;
    }

    public int getProgressStyle() {
        return progressStyle;
    }

    public void setProgressNumberFormat(String format) {
        this.progressNumberFormat = format;
    }

    public void setProgressPercentFormat(NumberFormat format) {
        this.progressPercentFormat = format;
    }
}
