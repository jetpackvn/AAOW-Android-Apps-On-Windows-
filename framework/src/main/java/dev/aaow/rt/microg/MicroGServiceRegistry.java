package dev.aaow.rt.microg;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Lớp tương thích dịch vụ Google Play Services / microG-lite ở tầng hệ thống AAOW:
 * - Giả lập gói hệ thống com.google.android.gms & com.android.vending trong PackageManager.
 * - Phản hồi bindService() cho AdvertisingIdClient, AppSet, GMS Broker qua AIDL Binder chuẩn.
 * - Cung cấp GServicesProvider (content://com.google.android.gsf.gservices).
 */
public final class MicroGServiceRegistry {
    private static final String TAG = "AAOW-MicroG";
    public static final String GMS_PACKAGE = "com.google.android.gms";
    public static final String VENDING_PACKAGE = "com.android.vending";
    public static final int GMS_VERSION_CODE = 244630000;
    public static final String GMS_VERSION_NAME = "24.46.30-aaow";

    // Chữ ký giả lập chuẩn (32 byte hex hợp lệ cho mọi parser kiểm tra packageInfo.signatures)
    private static final Signature GMS_SIGNATURE = new Signature(
            "30820120300d06092a864886f70d01010105000382010d00308201080282010100aa00112233445566778899aabbccddeeff");
    private static final Signature APP_DEFAULT_SIGNATURE = new Signature(
            "30820100300d06092a864886f70d01010105000381ed00aa00bb11cc22dd33ee44ff5566778899");

    private static final String AAOW_ADVERTISING_ID = UUID.nameUUIDFromBytes(
            ("aaow-ad-id:" + System.getProperty("user.name", "windows")).getBytes(StandardCharsets.UTF_8)).toString();
    private static final String AAOW_APP_SET_ID = UUID.nameUUIDFromBytes(
            ("aaow-appset-id:" + System.getProperty("user.name", "windows")).getBytes(StandardCharsets.UTF_8)).toString();

    private MicroGServiceRegistry() {
    }

    public static Signature getDefaultAppSignature() {
        return APP_DEFAULT_SIGNATURE;
    }

    public static boolean isVirtualGmsPackage(String pkg) {
        return GMS_PACKAGE.equals(pkg) || VENDING_PACKAGE.equals(pkg);
    }

    public static ApplicationInfo createVirtualApplicationInfo(String pkg) {
        if (!isVirtualGmsPackage(pkg)) return null;
        ApplicationInfo ai = new ApplicationInfo();
        ai.packageName = pkg;
        ai.processName = pkg;
        ai.nonLocalizedLabel = GMS_PACKAGE.equals(pkg) ? "Google Play services (AAOW microG)" : "Google Play Store (AAOW)";
        ai.flags = ApplicationInfo.FLAG_SYSTEM;
        ai.enabled = true;
        ai.uid = 10001;
        ai.targetSdkVersion = 34;
        ai.minSdkVersion = 21;
        return ai;
    }

    public static PackageInfo createVirtualPackageInfo(String pkg, int flags) {
        if (!isVirtualGmsPackage(pkg)) return null;
        PackageInfo pi = new PackageInfo();
        pi.packageName = pkg;
        if (GMS_PACKAGE.equals(pkg)) {
            pi.versionCode = GMS_VERSION_CODE;
            pi.longVersionCode = GMS_VERSION_CODE;
            pi.versionName = GMS_VERSION_NAME;
        } else {
            pi.versionCode = 84351200;
            pi.longVersionCode = 84351200L;
            pi.versionName = "43.5.12-aaow";
        }
        pi.applicationInfo = createVirtualApplicationInfo(pkg);
        pi.signatures = new Signature[]{GMS_SIGNATURE};
        return pi;
    }

    public static boolean canHandleService(Intent service) {
        if (service == null) return false;
        String pkg = service.getPackage();
        ComponentName cmp = service.getComponent();
        if (cmp != null && isVirtualGmsPackage(cmp.getPackageName())) return true;
        if (isVirtualGmsPackage(pkg)) return true;
        String action = service.getAction();
        return action != null && (action.startsWith("com.google.android.gms.")
                || action.startsWith("com.google.firebase.")
                || action.startsWith("com.google.android.c2dm.")
                || action.startsWith("com.android.vending."));
    }

    public static ResolveInfo resolveVirtualService(Intent service) {
        if (!canHandleService(service)) return null;
        String action = service.getAction() != null ? service.getAction() : "com.google.android.gms.common.service.START";
        String pkg = service.getPackage() != null ? service.getPackage() : GMS_PACKAGE;
        ServiceInfo si = new ServiceInfo();
        si.packageName = pkg;
        si.name = "dev.aaow.microg.ServiceBroker$" + action.replace('.', '_');
        si.enabled = true;
        si.exported = true;
        si.applicationInfo = createVirtualApplicationInfo(pkg);

        ResolveInfo ri = new ResolveInfo();
        ri.serviceInfo = si;
        ri.resolvePackageName = pkg;
        return ri;
    }

    public static List<ResolveInfo> queryVirtualServices(Intent service) {
        ResolveInfo ri = resolveVirtualService(service);
        return ri != null ? List.of(ri) : Collections.emptyList();
    }

    public static boolean bindVirtualService(Intent service, Executor executor, ServiceConnection conn) {
        if (conn == null || !canHandleService(service)) return false;
        String action = service.getAction() != null ? service.getAction() : "";
        String pkg = service.getPackage() != null ? service.getPackage() : GMS_PACKAGE;
        ComponentName cmp = service.getComponent() != null
                ? service.getComponent()
                : new ComponentName(pkg, "dev.aaow.microg.GmsBrokerService");

        IBinder binder = createBinderForAction(action);
        Runnable deliver = () -> {
            try {
                conn.onServiceConnected(cmp, binder);
            } catch (Throwable t) {
                Log.w(TAG, "Lỗi trong microG onServiceConnected(" + action + "): " + t);
            }
        };
        if (executor != null) {
            executor.execute(deliver);
        } else {
            deliver.run();
        }
        return true;
    }

    public static String getAdvertisingId() {
        return AAOW_ADVERTISING_ID;
    }

    public static String getAppSetId() {
        return AAOW_APP_SET_ID;
    }

    private static IBinder createBinderForAction(String action) {
        if ("com.google.android.gms.ads.identifier.service.START".equals(action)) {
            return new AdvertisingIdBinder();
        }
        return new GenericGmsBrokerBinder(action);
    }

    /**
     * AIDL stub chuẩn cho com.google.android.gms.ads.identifier.internal.IAdvertisingIdService:
     * - code 1: getId() -> String
     * - code 2: isLimitAdTrackingEnabled(boolean) -> int (0 = false)
     */
    private static final class AdvertisingIdBinder extends Binder {
        AdvertisingIdBinder() {
            super("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            if (reply == null) return true;
            if (code == 1) { // getId()
                reply.writeNoException();
                reply.writeString(AAOW_ADVERTISING_ID);
                return true;
            }
            if (code == 2) { // isLimitAdTrackingEnabled(boolean)
                reply.writeNoException();
                reply.writeInt(0);
                return true;
            }
            reply.writeNoException();
            reply.writeInt(0);
            return true;
        }
    }

    private static final class GenericGmsBrokerBinder extends Binder {
        GenericGmsBrokerBinder(String action) {
            super(action != null && !action.isEmpty() ? action : "com.google.android.gms.common.internal.IGmsServiceBroker");
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            if (reply != null) {
                reply.writeNoException();
                reply.writeInt(0);
            }
            return true;
        }
    }

    /** ContentProvider giả lập GServices (content://com.google.android.gsf.gservices). */
    public static final class GServicesProvider extends ContentProvider {
        private static final Map<String, String> DEFAULTS = Map.of(
                "android_id", "1234567890abcdef",
                "gms:ads:ad_id_enabled", "true"
        );

        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
            MatrixCursor mc = new MatrixCursor(new String[]{"key", "value"});
            if (selectionArgs != null) {
                for (String k : selectionArgs) {
                    if (k != null && DEFAULTS.containsKey(k)) {
                        mc.addRow(new Object[]{k, DEFAULTS.get(k)});
                    }
                }
            }
            return mc;
        }

        @Override
        public String getType(Uri uri) {
            return "vnd.android.cursor.dir/gservices";
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            return null;
        }

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            return 0;
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            return 0;
        }
    }
}
