package org.apache.commons.logging;

public abstract class LogFactory {
    public static Log getLog(Class<?> clazz) {
        return getLog(clazz != null ? clazz.getName() : "AAOW");
    }

    public static Log getLog(String name) {
        String tag = name != null ? name : "AAOW";
        return new Log() {
            @Override public boolean isDebugEnabled() { return false; }
            @Override public boolean isErrorEnabled() { return true; }
            @Override public boolean isFatalEnabled() { return true; }
            @Override public boolean isInfoEnabled() { return true; }
            @Override public boolean isTraceEnabled() { return false; }
            @Override public boolean isWarnEnabled() { return true; }
            @Override public void trace(Object message) {}
            @Override public void trace(Object message, Throwable t) {}
            @Override public void debug(Object message) {}
            @Override public void debug(Object message, Throwable t) {}
            @Override public void info(Object message) { android.util.Log.i(tag, String.valueOf(message)); }
            @Override public void info(Object message, Throwable t) { android.util.Log.i(tag, String.valueOf(message)); }
            @Override public void warn(Object message) { android.util.Log.w(tag, String.valueOf(message)); }
            @Override public void warn(Object message, Throwable t) { android.util.Log.w(tag, String.valueOf(message)); }
            @Override public void error(Object message) { android.util.Log.e(tag, String.valueOf(message)); }
            @Override public void error(Object message, Throwable t) { android.util.Log.e(tag, String.valueOf(message), t); }
            @Override public void fatal(Object message) { android.util.Log.e(tag, String.valueOf(message)); }
            @Override public void fatal(Object message, Throwable t) { android.util.Log.e(tag, String.valueOf(message), t); }
        };
    }
}
