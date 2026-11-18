package android.os;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/** Cài đặt nội bộ của android.os.Binder cho giao tiếp Service/IInterface trong tiến trình AAOW. */
public class Binder implements IBinder {
    private IInterface owner;
    private String descriptor;

    public Binder() {
    }

    public Binder(String descriptor) {
        this.descriptor = descriptor;
    }

    public void attachInterface(IInterface owner, String descriptor) {
        this.owner = owner;
        this.descriptor = descriptor;
    }

    @Override
    public String getInterfaceDescriptor() {
        return descriptor;
    }

    @Override
    public boolean pingBinder() {
        return true;
    }

    @Override
    public boolean isBinderAlive() {
        return true;
    }

    @Override
    public IInterface queryLocalInterface(String descriptor) {
        if (this.descriptor != null && this.descriptor.equals(descriptor)) {
            return owner;
        }
        return null;
    }

    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == INTERFACE_TRANSACTION) {
            if (reply != null) {
                reply.writeString(getInterfaceDescriptor());
            }
            return true;
        }
        return false;
    }

    @Override
    public final boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (data != null) {
            data.setDataPosition(0);
        }
        boolean r = onTransact(code, data, reply, flags);
        if (reply != null) {
            reply.setDataPosition(0);
        }
        return r;
    }

    @Override
    public void dump(FileDescriptor fd, String[] args) {
    }

    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
    }

    @Override
    public void dumpAsync(FileDescriptor fd, String[] args) {
    }

    @Override
    public void linkToDeath(DeathRecipient recipient, int flags) {
    }

    @Override
    public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
        return true;
    }

    public static long clearCallingIdentity() {
        return 0L;
    }

    public static void restoreCallingIdentity(long token) {
    }

    public static int getCallingPid() {
        return Process.myPid();
    }

    public static int getCallingUid() {
        return Process.myUid();
    }

    public static int getCallingUidOrThrow() {
        return Process.myUid();
    }

    public static void flushPendingCommands() {
    }
}
