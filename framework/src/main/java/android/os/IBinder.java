package android.os;

import java.io.FileDescriptor;

public interface IBinder {
    int FIRST_CALL_TRANSACTION = 0x00000001;
    int LAST_CALL_TRANSACTION = 0x00ffffff;
    int PING_TRANSACTION = ('_' << 24) | ('P' << 16) | ('N' << 8) | 'G';
    int DUMP_TRANSACTION = ('_' << 24) | ('D' << 16) | ('M' << 8) | 'P';
    int INTERFACE_TRANSACTION = ('_' << 24) | ('N' << 16) | ('T' << 8) | 'F';
    int TWEET_TRANSACTION = ('_' << 24) | ('T' << 16) | ('W' << 8) | 'T';
    int LIKE_TRANSACTION = ('_' << 24) | ('L' << 16) | ('I' << 8) | 'K';
    int FLAG_ONEWAY = 0x00000001;

    interface DeathRecipient {
        void binderDied();
    }

    static int getSuggestedMaxIpcSizeBytes() {
        return 64 * 1024;
    }

    String getInterfaceDescriptor() throws RemoteException;

    boolean pingBinder();

    boolean isBinderAlive();

    IInterface queryLocalInterface(String descriptor);

    void dump(FileDescriptor fd, String[] args) throws RemoteException;

    void dumpAsync(FileDescriptor fd, String[] args) throws RemoteException;

    boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException;

    void linkToDeath(DeathRecipient recipient, int flags) throws RemoteException;

    boolean unlinkToDeath(DeathRecipient recipient, int flags);
}
