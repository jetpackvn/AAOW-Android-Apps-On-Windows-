package android.util;

/**
 * In văn bản trực tiếp sang {@link Log} tương thích {@code android.util.LogPrinter}.
 */
public class LogPrinter implements Printer {
    private final int priority;
    private final String tag;

    public LogPrinter(int priority, String tag) {
        this.priority = priority;
        this.tag = tag != null ? tag : "";
    }

    @Override
    public void println(String x) {
        Log.println(priority, tag, x != null ? x : "");
    }
}
