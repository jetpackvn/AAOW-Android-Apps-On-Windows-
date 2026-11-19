package dev.aaow.rt.microg;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import dev.aaow.apk.ApkSignatureExtractor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Lớp tương thích dịch vụ Google Play Services / microG-lite ở tầng hệ thống AAOW:
 * - Giả lập gói hệ thống {@code com.google.android.gms} & {@code com.android.vending} trong {@code PackageManager}
 *   với chứng chỉ X.509 v3 DER hợp lệ chuẩn RFC 5280.
 * - Phản hồi {@code bindService()} cho {@code AdvertisingIdClient}, Firebase Analytics ({@code IMeasurementService}),
 *   Google Analytics v4 ({@code IAnalyticsService}), {@code AppSet} và {@code IGmsServiceBroker} ({@code code = 46})
 *   với đúng AIDL interface descriptor và callback {@code IGmsCallbacks.onPostInitComplete}.
 * - Cung cấp {@code GServicesProvider} ({@code content://com.google.android.gsf.gservices}).
 */
public final class MicroGServiceRegistry {
    private static final String TAG = "AAOW-MicroG";
    public static final String GMS_PACKAGE = "com.google.android.gms";
    public static final String VENDING_PACKAGE = "com.android.vending";
    public static final int GMS_VERSION_CODE = 244630000;
    public static final String GMS_VERSION_NAME = "24.46.30-aaow";

    public static final String ACTION_ADS_IDENTIFIER = "com.google.android.gms.ads.identifier.service.START";
    public static final String ACTION_MEASUREMENT = "com.google.android.gms.measurement.START";
    public static final String ACTION_ANALYTICS = "com.google.android.gms.analytics.service.START";
    public static final String ACTION_APPSET = "com.google.android.gms.appset.service.START";
    public static final String ACTION_COMMON_SERVICE = "com.google.android.gms.common.service.START";

    public static final String DESC_ADVERTISING_ID = "com.google.android.gms.ads.identifier.internal.IAdvertisingIdService";
    public static final String DESC_MEASUREMENT = "com.google.android.gms.measurement.internal.IMeasurementService";
    public static final String DESC_ANALYTICS = "com.google.android.gms.analytics.internal.IAnalyticsService";
    public static final String DESC_APPSET = "com.google.android.gms.appset.internal.IAppSetService";
    public static final String DESC_GMS_BROKER = "com.google.android.gms.common.internal.IGmsServiceBroker";
    public static final String DESC_GMS_CALLBACKS = "com.google.android.gms.common.internal.IGmsCallbacks";

    public static final int TRANSACTION_GET_SERVICE = 46;
    public static final int CALLBACK_ON_POST_INIT_COMPLETE = 1;

    private static final Map<String, String> ACTION_TO_DESCRIPTOR;
    static {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(ACTION_ADS_IDENTIFIER, DESC_ADVERTISING_ID);
        map.put(ACTION_MEASUREMENT, DESC_MEASUREMENT);
        map.put(ACTION_ANALYTICS, DESC_ANALYTICS);
        map.put(ACTION_APPSET, DESC_APPSET);
        map.put(ACTION_COMMON_SERVICE, DESC_GMS_BROKER);
        map.put("com.google.android.gms.signin.service.START", "com.google.android.gms.signin.internal.ISignInService");
        map.put("com.google.android.gms.auth.api.signin.service.START", "com.google.android.gms.auth.api.signin.internal.ISignInService");
        map.put("com.google.android.gms.phenotype.service.START", "com.google.android.gms.phenotype.internal.IPhenotypeService");
        map.put("com.google.android.gms.clearcut.service.START", "com.google.android.gms.clearcut.internal.IClearcutLoggerService");
        map.put("com.google.android.gms.chimera.container.moduleinstall.ModuleInstallService.START",
                "com.google.android.gms.common.moduleinstall.internal.IModuleInstallService");
        ACTION_TO_DESCRIPTOR = Collections.unmodifiableMap(map);
    }

    private static volatile Signature gmsSignature;
    private static volatile Signature appDefaultSignature;

    private static final String AAOW_ADVERTISING_ID = UUID.nameUUIDFromBytes(
            ("aaow-ad-id:" + System.getProperty("user.name", "windows")).getBytes(StandardCharsets.UTF_8)).toString();
    private static final String AAOW_APP_SET_ID = UUID.nameUUIDFromBytes(
            ("aaow-appset-id:" + System.getProperty("user.name", "windows")).getBytes(StandardCharsets.UTF_8)).toString();
    private static final String AAOW_APP_INSTANCE_ID = UUID.nameUUIDFromBytes(
            ("aaow-app-instance:" + System.getProperty("user.name", "windows")).getBytes(StandardCharsets.UTF_8))
            .toString().replace("-", "");

    private static final Set<ServiceConnection> ACTIVE_VIRTUAL_CONNECTIONS = ConcurrentHashMap.newKeySet();

    private MicroGServiceRegistry() {
    }

    /** Chứng chỉ X.509 v3 DER hợp lệ cho gói hệ thống ảo microG-lite. */
    public static Signature getGmsSignature() {
        Signature sig = gmsSignature;
        if (sig == null) {
            synchronized (MicroGServiceRegistry.class) {
                if (gmsSignature == null) {
                    gmsSignature = new Signature(
                            ApkSignatureExtractor.buildSelfSignedX509Der("AAOW MicroG Services", "AAOW"));
                }
                sig = gmsSignature;
            }
        }
        return sig;
    }

    /** Chứng chỉ X.509 v3 DER hợp lệ mặc định khi APK chưa được ký (ví dụ mẫu dev). */
    public static Signature getDefaultAppSignature() {
        Signature sig = appDefaultSignature;
        if (sig == null) {
            synchronized (MicroGServiceRegistry.class) {
                if (appDefaultSignature == null) {
                    appDefaultSignature = new Signature(
                            ApkSignatureExtractor.buildSelfSignedX509Der("AAOW App", "AAOW"));
                }
                sig = appDefaultSignature;
            }
        }
        return sig;
    }

    /**
     * Trả về trạng thái sẵn sàng của dịch vụ microG-lite trên AAOW
     * (được gọi trực tiếp bởi {@code GooglePlayServicesUtilLight.isGooglePlayServicesAvailable}).
     */
    public static int isGooglePlayServicesAvailable(Context context, int minApkVersion) {
        if (minApkVersion > GMS_VERSION_CODE) {
            return 2; // ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED
        }
        return 0; // ConnectionResult.SUCCESS
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
        Signature[] sigs = new Signature[]{getGmsSignature()};
        pi.signatures = sigs;
        pi.signingInfo = SigningInfo.fromSignatures(SigningInfo.VERSION_SIGNING_BLOCK_V2, sigs);
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

    public static String resolveDescriptorForAction(String action) {
        if (action == null || action.isEmpty()) {
            return DESC_GMS_BROKER;
        }
        String mapped = ACTION_TO_DESCRIPTOR.get(action);
        return mapped != null ? mapped : DESC_GMS_BROKER;
    }

    public static ResolveInfo resolveVirtualService(Intent service) {
        if (!canHandleService(service)) return null;
        String action = service.getAction() != null ? service.getAction() : ACTION_COMMON_SERVICE;
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

        IBinder binder = createBinderForIntent(service, action);
        ACTIVE_VIRTUAL_CONNECTIONS.add(conn);
        Log.i(TAG, "bindVirtualService(" + action + ") -> " + safeDescriptor(binder));

        Runnable deliver = () -> {
            try {
                conn.onServiceConnected(cmp, binder);
            } catch (Throwable t) {
                Log.w(TAG, "Lỗi trong microG onServiceConnected(" + action + "): " + t);
            }
        };

        // BlockingServiceConnection (AdvertisingIdClient) không yêu cầu Main Thread:
        // giao ngay để tránh chờ đợi/deadlock nếu Swing EDT đang bận khởi tạo.
        if (executor == null || isThreadSafeBlockingConnection(conn)) {
            deliver.run();
        } else {
            executor.execute(deliver);
        }
        return true;
    }

    public static boolean unbindVirtualService(ServiceConnection conn) {
        return conn != null && ACTIVE_VIRTUAL_CONNECTIONS.remove(conn);
    }

    private static String safeDescriptor(IBinder binder) {
        if (binder == null) return "null";
        try {
            return binder.getInterfaceDescriptor();
        } catch (RemoteException e) {
            return binder.getClass().getSimpleName();
        }
    }

    private static boolean isThreadSafeBlockingConnection(ServiceConnection conn) {
        String name = conn.getClass().getName();
        return name.endsWith(".BlockingServiceConnection") || name.equals("com.google.android.gms.common.BlockingServiceConnection");
    }

    public static String getAdvertisingId() {
        return AAOW_ADVERTISING_ID;
    }

    public static String getAppSetId() {
        return AAOW_APP_SET_ID;
    }

    public static String getAppInstanceId() {
        return AAOW_APP_INSTANCE_ID;
    }

    public static IBinder createBinderForAction(String action) {
        return createBinderForIntent(null, action);
    }

    private static IBinder createBinderForIntent(Intent intent, String action) {
        String effectiveAction = action != null ? action : "";
        if (effectiveAction.isEmpty() && intent != null && intent.getComponent() != null) {
            String cls = intent.getComponent().getClassName();
            if (cls != null) {
                if (cls.contains("AnalyticsService")) {
                    effectiveAction = ACTION_ANALYTICS;
                } else if (cls.contains("MeasurementService")) {
                    effectiveAction = ACTION_MEASUREMENT;
                }
            }
        }
        return switch (effectiveAction) {
            case ACTION_ADS_IDENTIFIER -> new AdvertisingIdBinder();
            case ACTION_MEASUREMENT -> new MeasurementServiceBinder();
            case ACTION_ANALYTICS -> new AnalyticsServiceBinder();
            case ACTION_APPSET -> new AppSetServiceBinder();
            default -> new GmsServiceBrokerBinder(effectiveAction);
        };
    }

    /**
     * Xử lý giao dịch {@code IGmsServiceBroker.getService(IGmsCallbacks, GetServiceRequest)} (code = 46):
     * gọi ngược {@code IGmsCallbacks.onPostInitComplete(ConnectionResult.SUCCESS, serviceBinder, null)} (code = 1).
     */
    private static boolean handleBrokerGetService(Parcel data, Parcel reply, IBinder defaultTargetBinder, String action)
            throws RemoteException {
        if (data == null) {
            if (reply != null) reply.writeNoException();
            return true;
        }
        data.enforceInterface(DESC_GMS_BROKER);
        IBinder callbacksBinder = data.readStrongBinder();

        IBinder targetBinder = defaultTargetBinder;
        if (targetBinder == null || DESC_GMS_BROKER.equals(targetBinder.getInterfaceDescriptor())) {
            String mappedDesc = ACTION_TO_DESCRIPTOR.get(action);
            if (mappedDesc != null && !DESC_GMS_BROKER.equals(mappedDesc)) {
                targetBinder = new GenericGmsServiceBinder(mappedDesc);
            } else {
                String clientDesc = inspectClientServiceDescriptor(callbacksBinder);
                targetBinder = new GenericGmsServiceBinder(
                        clientDesc != null && !clientDesc.isEmpty() ? clientDesc : DESC_GMS_BROKER);
            }
        }

        if (callbacksBinder != null) {
            Parcel cbData = Parcel.obtain();
            Parcel cbReply = Parcel.obtain();
            try {
                cbData.writeInterfaceToken(DESC_GMS_CALLBACKS);
                cbData.writeInt(0); // ConnectionResult.SUCCESS
                cbData.writeStrongBinder(targetBinder);
                cbData.writeInt(0); // Bundle = null (tương thích hoàn toàn với SafeParcel/zzc.zza + zzc.zzb)
                callbacksBinder.transact(CALLBACK_ON_POST_INIT_COMPLETE, cbData, cbReply, 0);
                cbReply.readException();
                Log.i(TAG, "IGmsServiceBroker.getService(" + action + ") -> "
                        + (targetBinder != null ? targetBinder.getInterfaceDescriptor() : "null"));
            } catch (Throwable t) {
                Log.w(TAG, "Lỗi khi gửi IGmsCallbacks.onPostInitComplete(" + action + "): " + t);
            } finally {
                cbData.recycle();
                cbReply.recycle();
            }
        }
        if (reply != null) {
            reply.writeNoException();
        }
        return true;
    }

    /**
     * Đọc {@code BaseGmsClient.getServiceDescriptor()} từ đối tượng {@code IGmsCallbacks} (nếu là client GMS chuẩn)
     * để trả về đúng interface descriptor cho mọi dịch vụ GMS mở rộng.
     */
    private static String inspectClientServiceDescriptor(IBinder callbacksBinder) {
        if (callbacksBinder == null) return null;
        try {
            for (Class<?> c = callbacksBinder.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (f.getType().isPrimitive() || f.getType() == String.class) continue;
                    f.setAccessible(true);
                    Object owner = f.get(callbacksBinder);
                    if (owner == null) continue;
                    Method m = findNoArgMethod(owner.getClass(), "getServiceDescriptor");
                    if (m != null) {
                        m.setAccessible(true);
                        Object desc = m.invoke(owner);
                        if (desc instanceof String s && !s.isEmpty()) {
                            return s;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Method findNoArgMethod(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    /**
     * AIDL stub chuẩn cho {@code com.google.android.gms.ads.identifier.internal.IAdvertisingIdService}:
     * - code 1: {@code getId() -> String}
     * - code 2: {@code isLimitAdTrackingEnabled(boolean) -> int (0 = false)}
     * - code 6: {@code isAdIdFakeForDebugLogging() -> int (0 = false)}
     */
    private static final class AdvertisingIdBinder extends Binder {
        AdvertisingIdBinder() {
            super(DESC_ADVERTISING_ID);
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == INTERFACE_TRANSACTION) {
                return super.onTransact(code, data, reply, flags);
            }
            if (code == TRANSACTION_GET_SERVICE) {
                return handleBrokerGetService(data, reply, this, ACTION_ADS_IDENTIFIER);
            }
            if (reply == null) return true;
            if (code == 1) { // getId()
                reply.writeNoException();
                reply.writeString(AAOW_ADVERTISING_ID);
                return true;
            }
            if (code == 2 || code == 6) { // isLimitAdTrackingEnabled / isAdIdFakeForDebugLogging
                reply.writeNoException();
                reply.writeInt(0);
                return true;
            }
            reply.writeNoException();
            reply.writeInt(0);
            return true;
        }
    }

    /**
     * AIDL stub chuẩn cho {@code com.google.android.gms.measurement.internal.IMeasurementService}
     * (Firebase Analytics / AppMeasurement). Hỗ trợ cả gắn kết trực tiếp lẫn qua {@code IGmsServiceBroker} (code = 46).
     */
    private static final class MeasurementServiceBinder extends Binder {
        MeasurementServiceBinder() {
            super(DESC_MEASUREMENT);
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == INTERFACE_TRANSACTION) {
                return super.onTransact(code, data, reply, flags);
            }
            if (code == TRANSACTION_GET_SERVICE) {
                return handleBrokerGetService(data, reply, this, ACTION_MEASUREMENT);
            }
            if (reply == null) return true;
            switch (code) {
                case 11 -> { // getAppInstanceId(zzq) -> String
                    reply.writeNoException();
                    reply.writeString(AAOW_APP_INSTANCE_ID);
                }
                case 7, 14, 15, 16, 17 -> { // getAllUserProperties / getConditionalUserProperties -> List<Parcelable>
                    reply.writeNoException();
                    reply.writeTypedList(Collections.emptyList());
                }
                case 9 -> { // upload(zzaw, String) -> byte[]
                    reply.writeNoException();
                    reply.writeByteArray(new byte[0]);
                }
                case 21 -> { // getSessionId(zzq) -> Bundle (nullable via presence int)
                    reply.writeNoException();
                    reply.writeInt(0);
                }
                default -> { // Các hàm void (logEvent, setUserProperty, sendMeasurementEnabled, setCurrentScreen...)
                    reply.writeNoException();
                }
            }
            return true;
        }
    }

    /**
     * AIDL stub chuẩn cho {@code com.google.android.gms.analytics.internal.IAnalyticsService} (Google Analytics v4):
     * - code 1: {@code sendHit(Map, long, String, List)}
     * - code 2: {@code clearHits()}
     * - code 3: {@code getVersion() -> String}
     */
    private static final class AnalyticsServiceBinder extends Binder {
        AnalyticsServiceBinder() {
            super(DESC_ANALYTICS);
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == INTERFACE_TRANSACTION) {
                return super.onTransact(code, data, reply, flags);
            }
            if (code == TRANSACTION_GET_SERVICE) {
                return handleBrokerGetService(data, reply, this, ACTION_ANALYTICS);
            }
            if (reply == null) return true;
            if (code == 3) { // getVersion()
                reply.writeNoException();
                reply.writeString("12.4.51-aaow");
                return true;
            }
            reply.writeNoException();
            return true;
        }
    }

    /**
     * AIDL stub chuẩn cho {@code com.google.android.gms.appset.internal.IAppSetService}.
     */
    private static final class AppSetServiceBinder extends Binder {
        AppSetServiceBinder() {
            super(DESC_APPSET);
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == INTERFACE_TRANSACTION) {
                return super.onTransact(code, data, reply, flags);
            }
            if (code == TRANSACTION_GET_SERVICE) {
                return handleBrokerGetService(data, reply, this, ACTION_APPSET);
            }
            if (reply != null) {
                reply.writeNoException();
            }
            return true;
        }
    }

    /**
     * Broker tổng quát chuẩn {@code IGmsServiceBroker}: xử lý {@code code = 46} để bàn giao
     * {@link GenericGmsServiceBinder} khớp đúng interface descriptor qua {@code IGmsCallbacks.onPostInitComplete}.
     */
    private static final class GmsServiceBrokerBinder extends Binder {
        private final String action;

        GmsServiceBrokerBinder(String action) {
            super(resolveDescriptorForAction(action));
            this.action = action != null ? action : "";
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == INTERFACE_TRANSACTION) {
                return super.onTransact(code, data, reply, flags);
            }
            if (code == TRANSACTION_GET_SERVICE) {
                return handleBrokerGetService(data, reply, null, action);
            }
            if (reply != null) {
                reply.writeNoException();
                reply.writeInt(0);
            }
            return true;
        }
    }

    private static final class GenericGmsServiceBinder extends Binder {
        GenericGmsServiceBinder(String descriptor) {
            super(descriptor != null && !descriptor.isEmpty() ? descriptor : DESC_GMS_BROKER);
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == INTERFACE_TRANSACTION) {
                return super.onTransact(code, data, reply, flags);
            }
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
