package android.os;

/**
 * Cài đặt thật của {@code android.os.Messenger} hỗ trợ truyền {@link Message} qua {@link Handler}
 * hoặc {@link IBinder} (dùng bởi Firebase Sessions / Messaging / GMS IPC).
 */
public final class Messenger implements Parcelable {
    private static final String DESCRIPTOR = "android.os.IMessenger";
    private static final int TRANSACTION_SEND = IBinder.FIRST_CALL_TRANSACTION;

    private final IBinder target;

    public Messenger(Handler target) {
        this.target = target != null ? new HandlerMessengerBinder(target) : new Binder(DESCRIPTOR);
    }

    public Messenger(IBinder target) {
        this.target = target;
    }

    public void send(Message message) throws RemoteException {
        if (target == null || message == null) return;
        if (target instanceof HandlerMessengerBinder hmb) {
            hmb.sendLocal(message);
            return;
        }
        Parcel data = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeParcelable(message, 0);
            target.transact(TRANSACTION_SEND, data, null, IBinder.FLAG_ONEWAY);
        } finally {
            data.recycle();
        }
    }

    public IBinder getBinder() {
        return target;
    }

    @Override
    public boolean equals(Object otherObj) {
        if (otherObj instanceof Messenger other) {
            return target != null ? target.equals(other.target) : other.target == null;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return target != null ? target.hashCode() : 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeStrongBinder(target);
    }

    public static void writeMessengerOrNullToParcel(Messenger messenger, Parcel out) {
        out.writeStrongBinder(messenger != null ? messenger.target : null);
    }

    public static Messenger readMessengerOrNullFromParcel(Parcel in) {
        IBinder b = in.readStrongBinder();
        return b != null ? new Messenger(b) : null;
    }

    public static final Creator<Messenger> CREATOR = new Creator<>() {
        @Override
        public Messenger createFromParcel(Parcel in) {
            IBinder target = in.readStrongBinder();
            return target != null ? new Messenger(target) : null;
        }

        @Override
        public Messenger[] newArray(int size) {
            return new Messenger[size];
        }
    };

    private static final class HandlerMessengerBinder extends Binder {
        private final Handler handler;

        HandlerMessengerBinder(Handler handler) {
            super(DESCRIPTOR);
            this.handler = handler;
        }

        void sendLocal(Message message) {
            Message copy = Message.obtain(message);
            handler.sendMessage(copy);
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == TRANSACTION_SEND && data != null) {
                data.enforceInterface(DESCRIPTOR);
                Message msg = data.readParcelable(Message.class.getClassLoader());
                if (msg != null) {
                    sendLocal(msg);
                }
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }
    }
}
