package android.os;

public interface IBinder {
    int FIRST_CALL_TRANSACTION = 0x00000001;
    int LAST_CALL_TRANSACTION = 0x00ffffff;
    int INTERFACE_TRANSACTION = ('_' << 24) | ('N' << 16) | ('T' << 8) | 'F';

    String getInterfaceDescriptor();

    boolean pingBinder();

    boolean isBinderAlive();

    boolean transact(int code, Parcel data, Parcel reply, int flags);
}
