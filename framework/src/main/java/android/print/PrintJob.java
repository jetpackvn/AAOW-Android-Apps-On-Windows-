package android.print;

public final class PrintJob {
    private final PrintJobInfo info;

    PrintJob(PrintJobInfo info) {
        this.info = info;
    }

    public PrintJobId getId() { return info.getId(); }
    public PrintJobInfo getInfo() { return info; }
    public boolean isQueued() { return info.getState() == PrintJobInfo.STATE_QUEUED; }
    public boolean isStarted() { return info.getState() == PrintJobInfo.STATE_STARTED; }
    public boolean isBlocked() { return info.getState() == PrintJobInfo.STATE_BLOCKED; }
    public boolean isCompleted() { return info.getState() == PrintJobInfo.STATE_COMPLETED; }
    public boolean isFailed() { return info.getState() == PrintJobInfo.STATE_FAILED; }
    public boolean isCancelled() { return info.getState() == PrintJobInfo.STATE_CANCELED; }

    public void cancel() {
        info.setState(PrintJobInfo.STATE_CANCELED);
    }

    public void restart() {
        info.setState(PrintJobInfo.STATE_QUEUED);
    }
}
