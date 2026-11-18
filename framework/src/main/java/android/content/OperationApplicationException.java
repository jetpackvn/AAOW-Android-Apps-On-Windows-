package android.content;

public class OperationApplicationException extends Exception {
    private final int numSuccessfulYieldPoints;

    public OperationApplicationException() {
        this.numSuccessfulYieldPoints = 0;
    }

    public OperationApplicationException(String message) {
        super(message);
        this.numSuccessfulYieldPoints = 0;
    }

    public OperationApplicationException(String message, Throwable cause) {
        super(message, cause);
        this.numSuccessfulYieldPoints = 0;
    }

    public OperationApplicationException(Throwable cause) {
        super(cause);
        this.numSuccessfulYieldPoints = 0;
    }

    public OperationApplicationException(int numSuccessfulYieldPoints) {
        this.numSuccessfulYieldPoints = numSuccessfulYieldPoints;
    }

    public OperationApplicationException(String message, int numSuccessfulYieldPoints) {
        super(message);
        this.numSuccessfulYieldPoints = numSuccessfulYieldPoints;
    }

    public int getNumSuccessfulYieldPoints() {
        return numSuccessfulYieldPoints;
    }
}
