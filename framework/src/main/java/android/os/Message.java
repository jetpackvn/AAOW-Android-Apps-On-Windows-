package android.os;

public final class Message implements Parcelable {
    public int what;
    public int arg1;
    public int arg2;
    public Object obj;
    public int sendingUid = -1;
    public Handler target;
    public Runnable callback;
    Bundle data;

    public static Message obtain() {
        return new Message();
    }

    public static Message obtain(Handler h) {
        Message m = new Message();
        m.target = h;
        return m;
    }

    public static Message obtain(Handler h, Runnable callback) {
        Message m = new Message();
        m.target = h;
        m.callback = callback;
        return m;
    }

    public static Message obtain(Handler h, int what) {
        Message m = new Message();
        m.target = h;
        m.what = what;
        return m;
    }

    public static Message obtain(Handler h, int what, Object obj) {
        Message m = new Message();
        m.target = h;
        m.what = what;
        m.obj = obj;
        return m;
    }

    public static Message obtain(Handler h, int what, int arg1, int arg2) {
        Message m = new Message();
        m.target = h;
        m.what = what;
        m.arg1 = arg1;
        m.arg2 = arg2;
        return m;
    }

    public static Message obtain(Handler h, int what, int arg1, int arg2, Object obj) {
        Message m = new Message();
        m.target = h;
        m.what = what;
        m.arg1 = arg1;
        m.arg2 = arg2;
        m.obj = obj;
        return m;
    }

    public void recycle() {
    }

    public void sendToTarget() {
        if (target != null) target.sendMessage(this);
    }

    public Bundle getData() {
        if (data == null) data = new Bundle();
        return data;
    }

    public void setData(Bundle data) {
        this.data = data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(what);
        dest.writeInt(arg1);
        dest.writeInt(arg2);
        dest.writeBundle(data);
    }
}
