package android.content;

import android.os.IBinder;

/** Giao diện nhận kết quả kết nối khi bindService() / unbindService(). */
public interface ServiceConnection {
    void onServiceConnected(ComponentName name, IBinder service);

    void onServiceDisconnected(ComponentName name);

    default void onBindingDied(ComponentName name) {
    }

    default void onNullBinding(ComponentName name) {
    }
}
