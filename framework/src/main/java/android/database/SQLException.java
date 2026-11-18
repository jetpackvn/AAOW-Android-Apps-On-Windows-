package android.database;

/** Ngoại lệ SQL cơ sở tương thích Android SDK. */
public class SQLException extends RuntimeException {
    public SQLException() {
    }

    public SQLException(String error) {
        super(error);
    }

    public SQLException(String error, Throwable cause) {
        super(error, cause);
    }
}
