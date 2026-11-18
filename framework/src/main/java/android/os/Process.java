package android.os;

public class Process {
    public static final int SYSTEM_UID = 1000;
    public static final int FIRST_APPLICATION_UID = 10000;

    public static final int myPid() {
        return (int) ProcessHandle.current().pid();
    }

    public static final int myTid() {
        return (int) Thread.currentThread().threadId();
    }

    public static final int myUid() {
        return FIRST_APPLICATION_UID;
    }

    public static final boolean is64Bit() {
        return true;
    }

    public static String myProcessName() {
        return android.app.ActivityThread.currentProcessName();
    }

    public static final void killProcess(int pid) {
    }

    public static final void setThreadPriority(int priority) {
    }

    public static final void setThreadPriority(int tid, int priority) {
    }

    public static final int getThreadPriority(int tid) {
        return 0;
    }
}
