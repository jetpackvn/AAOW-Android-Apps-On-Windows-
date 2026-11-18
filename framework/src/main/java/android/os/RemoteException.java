package android.os;

import android.util.AndroidException;

/** Ngoại lệ IPC tương thích Android SDK. */
public class RemoteException extends AndroidException {
    public RemoteException() {
        super();
    }

    public RemoteException(String message) {
        super(message);
    }

    public RuntimeException rethrowAsRuntimeException() {
        return new RuntimeException(this);
    }

    public RuntimeException rethrowFromSystemServer() {
        return new RuntimeException(this);
    }
}
