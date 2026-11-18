package android.webkit;

public class ConsoleMessage {
    public enum MessageLevel {
        TIP,
        LOG,
        WARNING,
        ERROR,
        DEBUG
    }

    private final String message;
    private final String sourceId;
    private final int lineNumber;
    private final MessageLevel messageLevel;

    public ConsoleMessage(String message, String sourceId, int lineNumber, MessageLevel msgLevel) {
        this.message = message;
        this.sourceId = sourceId;
        this.lineNumber = lineNumber;
        this.messageLevel = msgLevel;
    }

    public MessageLevel messageLevel() {
        return messageLevel;
    }

    public String message() {
        return message;
    }

    public String sourceId() {
        return sourceId;
    }

    public int lineNumber() {
        return lineNumber;
    }
}
