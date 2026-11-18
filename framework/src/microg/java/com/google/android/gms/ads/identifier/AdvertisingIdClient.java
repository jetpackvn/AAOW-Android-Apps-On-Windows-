package com.google.android.gms.ads.identifier;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcel;
import com.google.android.gms.common.internal.Preconditions;
import dev.aaow.rt.microg.MicroGServiceRegistry;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cài đặt tương thích AdvertisingIdClient (microG-lite) kết nối qua AIDL Binder của AAOW.
 */
public class AdvertisingIdClient {
    public static final class Info {
        private final String advertisingId;
        private final boolean limitAdTrackingEnabled;

        public Info(String advertisingId, boolean limitAdTrackingEnabled) {
            this.advertisingId = advertisingId;
            this.limitAdTrackingEnabled = limitAdTrackingEnabled;
        }

        public String getId() {
            return advertisingId;
        }

        public boolean isLimitAdTrackingEnabled() {
            return limitAdTrackingEnabled;
        }

        @Override
        public String toString() {
            return "{" + advertisingId + "}" + limitAdTrackingEnabled;
        }
    }

    private final Context context;

    public AdvertisingIdClient(Context context) {
        this.context = Preconditions.checkNotNull(context);
    }

    public static Info getAdvertisingIdInfo(Context context) throws IOException {
        Preconditions.checkNotMainThread("Calling this from your main thread can lead to deadlock");
        if (context == null) {
            return new Info(MicroGServiceRegistry.getAdvertisingId(), false);
        }
        Intent intent = new Intent("com.google.android.gms.ads.identifier.service.START");
        intent.setPackage(MicroGServiceRegistry.GMS_PACKAGE);
        AtomicReference<IBinder> binderRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                binderRef.set(service);
                latch.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                latch.countDown();
            }
        };
        try {
            if (context.bindService(intent, conn, Context.BIND_AUTO_CREATE) && latch.await(1500, TimeUnit.MILLISECONDS)) {
                IBinder b = binderRef.get();
                if (b != null) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    try {
                        data.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
                        b.transact(1, data, reply, 0);
                        reply.readException();
                        String id = reply.readString();

                        Parcel data2 = Parcel.obtain();
                        Parcel reply2 = Parcel.obtain();
                        try {
                            data2.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
                            data2.writeInt(1);
                            b.transact(2, data2, reply2, 0);
                            reply2.readException();
                            boolean lat = reply2.readInt() != 0;
                            if (id != null && !id.isEmpty()) {
                                return new Info(id, lat);
                            }
                        } finally {
                            data2.recycle();
                            reply2.recycle();
                        }
                    } finally {
                        data.recycle();
                        reply.recycle();
                    }
                }
            }
        } catch (Throwable ignored) {
        } finally {
            try {
                context.unbindService(conn);
            } catch (Throwable ignored) {
            }
        }
        return new Info(MicroGServiceRegistry.getAdvertisingId(), false);
    }

    public static void setShouldSkipGmsCoreVersionCheck(boolean shouldSkip) {
    }

    public void start() {
    }

    public Info getInfo() throws IOException {
        return new Info(MicroGServiceRegistry.getAdvertisingId(), false);
    }

    public void finish() {
    }
}
