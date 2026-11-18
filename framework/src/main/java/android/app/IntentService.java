package android.app;

import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

/** Dịch vụ xử lý tuần tự các Intent trên luồng nền HandlerThread riêng. */
public abstract class IntentService extends Service {
    private final String name;
    private boolean redelivery;
    private volatile Looper serviceLooper;
    private volatile ServiceHandler serviceHandler;

    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent) msg.obj);
            stopSelf(msg.arg1);
        }
    }

    public IntentService(String name) {
        this.name = name != null ? name : "IntentService";
    }

    public void setIntentRedelivery(boolean enabled) {
        this.redelivery = enabled;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("IntentService[" + name + "]");
        thread.start();
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (serviceHandler != null) {
            Message msg = serviceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = intent;
            serviceHandler.sendMessage(msg);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return redelivery ? START_REDELIVER_INTENT : START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (serviceLooper != null) {
            serviceLooper.quitSafely();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected abstract void onHandleIntent(Intent intent);
}
