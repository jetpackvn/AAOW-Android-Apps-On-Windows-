package android.os;

public final class Trace {
    private Trace() {
    }

    public static void beginSection(String sectionName) {
    }

    public static void endSection() {
    }

    public static void beginAsyncSection(String methodName, int cookie) {
    }

    public static void endAsyncSection(String methodName, int cookie) {
    }

    public static void setCounter(String counterName, long counterValue) {
    }

    public static boolean isEnabled() {
        return false;
    }
}
