package android.os;

/** Ngoại lệ ném ra khi thao tác bị hủy thông qua CancellationSignal. */
public class OperationCanceledException extends RuntimeException {
    public OperationCanceledException() {
        super("The operation has been canceled.");
    }

    public OperationCanceledException(String message) {
        super(message != null ? message : "The operation has been canceled.");
    }
}
