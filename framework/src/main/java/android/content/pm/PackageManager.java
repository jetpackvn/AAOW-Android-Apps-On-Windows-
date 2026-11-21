package android.content.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.AndroidException;
import dev.aaow.apk.AndroidManifest;
import dev.aaow.apk.ApkFile;
import dev.aaow.apk.ApkSignatureExtractor;
import dev.aaow.apk.ResValue;
import dev.aaow.rt.microg.MicroGServiceRegistry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PackageManager {
    public static final int MATCH_DEFAULT_ONLY = 0x00010000;
    public static final int MATCH_ALL = 0x00020000;
    public static final int GET_ACTIVITIES = 0x00000001;
    public static final int GET_RECEIVERS = 0x00000002;
    public static final int GET_SERVICES = 0x00000004;
    public static final int GET_PROVIDERS = 0x00000008;
    public static final int GET_SIGNATURES = 0x00000040;
    public static final int GET_META_DATA = 0x00000080;
    public static final int GET_PERMISSIONS = 0x00001000;
    public static final int GET_SIGNING_CERTIFICATES = 0x08000000;
    public static final int PERMISSION_GRANTED = 0;
    public static final int PERMISSION_DENIED = -1;
    public static final int COMPONENT_ENABLED_STATE_DEFAULT = 0;
    public static final int COMPONENT_ENABLED_STATE_ENABLED = 1;
    public static final int COMPONENT_ENABLED_STATE_DISABLED = 2;
    public static final int COMPONENT_ENABLED_STATE_DISABLED_USER = 3;
    public static final int COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED = 4;
    public static final int DONT_KILL_APP = 1;
    public static final int CERT_INPUT_RAW_X509 = 0;
    public static final int CERT_INPUT_SHA256 = 1;

    public static class NameNotFoundException extends AndroidException {
        public NameNotFoundException() {
        }

        public NameNotFoundException(String name) {
            super(name);
        }
    }

    public static final class PackageInfoFlags {
        private final long value;

        PackageInfoFlags() {
            this(0L);
        }

        private PackageInfoFlags(long value) {
            this.value = value;
        }

        public long getValue() {
            return value;
        }

        public static PackageInfoFlags of(long value) {
            return new PackageInfoFlags(value);
        }
    }

    public static final class ApplicationInfoFlags {
        private final long value;

        ApplicationInfoFlags() {
            this(0L);
        }

        private ApplicationInfoFlags(long value) {
            this.value = value;
        }

        public long getValue() {
            return value;
        }

        public static ApplicationInfoFlags of(long value) {
            return new ApplicationInfoFlags(value);
        }
    }

    public static final class ComponentInfoFlags {
        private final long value;

        ComponentInfoFlags() {
            this(0L);
        }

        private ComponentInfoFlags(long value) {
            this.value = value;
        }

        public long getValue() {
            return value;
        }

        public static ComponentInfoFlags of(long value) {
            return new ComponentInfoFlags(value);
        }
    }

    public static final class ResolveInfoFlags {
        private final long value;

        ResolveInfoFlags() {
            this(0L);
        }

        private ResolveInfoFlags(long value) {
            this.value = value;
        }

        public long getValue() {
            return value;
        }

        public static ResolveInfoFlags of(long value) {
            return new ResolveInfoFlags(value);
        }
    }

    private final AndroidManifest manifest;
    private final ApplicationInfo appInfo;
    private final Resources resources;
    private final ApkFile apkFile;
    private volatile Signature[] cachedAppSignatures;
    private volatile int cachedSchemeVersion;
    private final Map<ComponentName, Integer> componentEnabledStates = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, String> installerPackages = new java.util.concurrent.ConcurrentHashMap<>();

    public PackageManager(AndroidManifest manifest, ApplicationInfo appInfo) {
        this(manifest, appInfo, null, null);
    }

    public PackageManager(AndroidManifest manifest, ApplicationInfo appInfo, Resources resources) {
        this(manifest, appInfo, resources, null);
    }

    public PackageManager(AndroidManifest manifest, ApplicationInfo appInfo, Resources resources, ApkFile apkFile) {
        this.manifest = manifest;
        this.appInfo = appInfo;
        this.resources = resources;
        this.apkFile = apkFile;
        if (this.appInfo != null && manifest != null && !manifest.applicationMetaData.isEmpty()) {
            this.appInfo.metaData = toBundle(manifest.applicationMetaData);
        }
    }

    private static Bundle toBundle(Map<String, AndroidManifest.MetaData> meta) {
        if (meta == null || meta.isEmpty())
            return null;
        Bundle b = new Bundle();
        for (var e : meta.entrySet()) {
            AndroidManifest.MetaData md = e.getValue();
            if (md.resourceId() != 0) {
                b.putInt(md.name(), md.resourceId());
            } else if (md.type() == ResValue.TYPE_INT_BOOLEAN) {
                b.putBoolean(md.name(), md.intData() != 0);
            } else if (md.type() == ResValue.TYPE_INT_DEC || md.type() == ResValue.TYPE_INT_HEX) {
                b.putInt(md.name(), md.intData());
            } else if (md.type() == ResValue.TYPE_FLOAT) {
                b.putFloat(md.name(), Float.intBitsToFloat(md.intData()));
            } else if (md.stringValue() != null) {
                b.putString(md.name(), md.stringValue());
            }
        }
        return b;
    }

    public Resources getResourcesForApplication(ApplicationInfo app) throws NameNotFoundException {
        if (resources != null)
            return resources;
        throw new NameNotFoundException(app != null ? app.packageName : "null");
    }

    public Resources getResourcesForApplication(String appPackageName) throws NameNotFoundException {
        if (resources != null && (manifest == null || manifest.packageName.equals(appPackageName))) {
            return resources;
        }
        throw new NameNotFoundException(appPackageName);
    }

    public XmlResourceParser getXml(String packageName, int resid, ApplicationInfo appInfo) {
        if (resources != null && resid != 0) {
            try {
                return resources.getXml(resid);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public android.graphics.drawable.Drawable getDrawable(String packageName, int resid, ApplicationInfo appInfo) {
        if (resources != null && resid != 0) {
            try {
                return resources.getDrawable(resid);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public android.graphics.drawable.Drawable getApplicationIcon(ApplicationInfo info) {
        if (info != null && info.icon != 0) {
            android.graphics.drawable.Drawable d = getDrawable(info.packageName, info.icon, info);
            if (d != null) return d;
        }
        return new android.graphics.drawable.ColorDrawable(0xFF4F5BD5);
    }

    public android.graphics.drawable.Drawable getApplicationIcon(String packageName) throws NameNotFoundException {
        return getApplicationIcon(getApplicationInfo(packageName, 0));
    }

    public PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException {
        if (MicroGServiceRegistry.isVirtualGmsPackage(packageName)) {
            return MicroGServiceRegistry.createVirtualPackageInfo(packageName, flags);
        }
        if (manifest != null && (packageName == null || packageName.equals(manifest.packageName))) {
            return buildPackageInfo(manifest, appInfo, flags);
        }
        throw new NameNotFoundException(packageName);
    }

    public PackageInfo getPackageInfo(String packageName, PackageInfoFlags flags) throws NameNotFoundException {
        return getPackageInfo(packageName, flags != null ? (int) flags.getValue() : 0);
    }

    public PackageInfo getPackageArchiveInfo(String archiveFilePath, PackageInfoFlags flags) {
        return getPackageArchiveInfo(archiveFilePath, flags != null ? (int) flags.getValue() : 0);
    }

    public List<PackageInfo> getInstalledPackages(PackageInfoFlags flags) {
        return getInstalledPackages(flags != null ? (int) flags.getValue() : 0);
    }

    public PackageInfo getPackageArchiveInfo(String archiveFilePath, int flags) {
        if (archiveFilePath == null || archiveFilePath.isEmpty())
            return null;
        Path p = Path.of(archiveFilePath);
        if (!Files.isRegularFile(p))
            return null;
        try (ApkFile apk = new ApkFile(p)) {
            AndroidManifest mf = apk.getManifest();
            ApplicationInfo ai = new ApplicationInfo();
            ai.packageName = mf.packageName;
            ai.sourceDir = p.toAbsolutePath().toString();
            ai.publicSourceDir = ai.sourceDir;
            ai.nonLocalizedLabel = mf.label;
            ai.icon = mf.iconRes;
            ai.theme = mf.themeRes;
            ai.minSdkVersion = mf.minSdk;
            ai.targetSdkVersion = mf.targetSdk;
            ai.metaData = toBundle(mf.applicationMetaData);
            return buildPackageInfo(mf, ai, flags, apk);
        } catch (Exception e) {
            return null;
        }
    }

    public List<PackageInfo> getInstalledPackages(int flags) {
        List<PackageInfo> list = new ArrayList<>();
        if (manifest != null) {
            list.add(buildPackageInfo(manifest, appInfo, flags));
        }
        list.add(MicroGServiceRegistry.createVirtualPackageInfo(MicroGServiceRegistry.GMS_PACKAGE, flags));
        list.add(MicroGServiceRegistry.createVirtualPackageInfo(MicroGServiceRegistry.VENDING_PACKAGE, flags));
        return list;
    }

    private PackageInfo buildPackageInfo(AndroidManifest mf, ApplicationInfo ai, int flags) {
        return buildPackageInfo(mf, ai, flags, apkFile);
    }

    private PackageInfo buildPackageInfo(AndroidManifest mf, ApplicationInfo ai, int flags, ApkFile sourceApk) {
        PackageInfo pi = new PackageInfo();
        pi.packageName = mf.packageName;
        pi.versionName = mf.versionName;
        pi.versionCode = mf.versionCode;
        pi.longVersionCode = mf.versionCode;
        pi.applicationInfo = ai;
        ResolvedSignatures resolved = resolveSignatures(sourceApk);
        pi.signatures = resolved.signatures.clone();
        pi.signingInfo = SigningInfo.fromSignatures(resolved.schemeVersion, resolved.signatures);
        if ((flags & GET_PERMISSIONS) != 0 || !mf.permissions.isEmpty()) {
            pi.requestedPermissions = mf.permissions.toArray(new String[0]);
        }
        if ((flags & GET_ACTIVITIES) != 0) {
            pi.activities = mf.activities.stream().map(a -> toActivityInfo(mf, ai, a)).toArray(ActivityInfo[]::new);
        }
        if ((flags & GET_RECEIVERS) != 0) {
            pi.receivers = mf.receivers.stream().map(r -> toReceiverInfo(mf, ai, r)).toArray(ActivityInfo[]::new);
        }
        if ((flags & GET_SERVICES) != 0) {
            pi.services = mf.services.stream().map(s -> toServiceInfo(mf, ai, s)).toArray(ServiceInfo[]::new);
        }
        if ((flags & GET_PROVIDERS) != 0) {
            pi.providers = mf.providers.stream().map(p -> toProviderInfo(mf, ai, p)).toArray(ProviderInfo[]::new);
        }
        return pi;
    }

    private record ResolvedSignatures(Signature[] signatures, int schemeVersion) {
    }

    private ResolvedSignatures resolveSignatures(ApkFile sourceApk) {
        if (sourceApk == this.apkFile && cachedAppSignatures != null) {
            return new ResolvedSignatures(cachedAppSignatures, cachedSchemeVersion);
        }
        Signature[] sigs = null;
        int scheme = SigningInfo.VERSION_JAR;
        if (sourceApk != null) {
            try {
                ApkSignatureExtractor.SigningResult res = sourceApk.getSigningResult();
                if (res != null && res.hasCertificates()) {
                    List<byte[]> certs = res.certificates();
                    sigs = new Signature[certs.size()];
                    for (int i = 0; i < certs.size(); i++) {
                        sigs[i] = new Signature(certs.get(i));
                    }
                    scheme = res.schemeVersion() > 0 ? res.schemeVersion() : SigningInfo.VERSION_JAR;
                }
            } catch (Throwable ignored) {
            }
        }
        if (sigs == null || sigs.length == 0) {
            sigs = new Signature[]{MicroGServiceRegistry.getDefaultAppSignature()};
            scheme = SigningInfo.VERSION_JAR;
        }
        if (sourceApk == this.apkFile) {
            cachedAppSignatures = sigs;
            cachedSchemeVersion = scheme;
        }
        return new ResolvedSignatures(sigs, scheme);
    }

    public boolean hasSigningCertificate(String packageName, byte[] certificate, int type) {
        if (packageName == null || certificate == null || certificate.length == 0) {
            return false;
        }
        try {
            PackageInfo pi = getPackageInfo(packageName, GET_SIGNING_CERTIFICATES | GET_SIGNATURES);
            if (pi == null || pi.signatures == null) return false;
            for (Signature sig : pi.signatures) {
                byte[] raw = sig.toByteArray();
                if (type == CERT_INPUT_RAW_X509 && Arrays.equals(raw, certificate)) {
                    return true;
                }
                if (type == CERT_INPUT_SHA256) {
                    byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw);
                    if (Arrays.equals(digest, certificate)) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags) throws NameNotFoundException {
        if (MicroGServiceRegistry.isVirtualGmsPackage(packageName)) {
            return MicroGServiceRegistry.createVirtualApplicationInfo(packageName);
        }
        if (appInfo != null && (packageName == null || packageName.equals(appInfo.packageName))) {
            return appInfo;
        }
        throw new NameNotFoundException(packageName);
    }

    public ApplicationInfo getApplicationInfo(String packageName, ApplicationInfoFlags flags) throws NameNotFoundException {
        return getApplicationInfo(packageName, flags != null ? (int) flags.getValue() : 0);
    }

    public CharSequence getApplicationLabel(ApplicationInfo info) {
        if (info != null && info.nonLocalizedLabel != null)
            return info.nonLocalizedLabel;
        return manifest != null && manifest.label != null ? manifest.label : "";
    }

    public Intent getLaunchIntentForPackage(String packageName) {
        if (manifest != null && manifest.launcherActivity != null && manifest.packageName.equals(packageName)) {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setClassName(manifest.packageName, manifest.launcherActivity);
            return i;
        }
        return null;
    }

    public int checkPermission(String permName, String pkgName) {
        return PERMISSION_GRANTED;
    }

    public boolean hasSystemFeature(String name) {
        return false;
    }

    public boolean isInstantApp() {
        return false;
    }

    public boolean isInstantApp(String packageName) {
        return false;
    }

    public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags) {
        if (componentName != null) {
            componentEnabledStates.put(componentName, newState);
        }
    }

    public int getComponentEnabledSetting(ComponentName componentName) {
        if (componentName == null)
            return COMPONENT_ENABLED_STATE_DEFAULT;
        return componentEnabledStates.getOrDefault(componentName, COMPONENT_ENABLED_STATE_DEFAULT);
    }

    public String getInstallerPackageName(String packageName) {
        if (packageName == null)
            return null;
        String recorded = installerPackages.get(packageName);
        if (recorded != null)
            return recorded;
        if ((manifest != null && packageName.equals(manifest.packageName))
                || MicroGServiceRegistry.isVirtualGmsPackage(packageName)) {
            return "com.android.vending";
        }
        return null;
    }

    public void setInstallerPackageName(String targetPackage, String installerPackageName) {
        if (targetPackage != null && installerPackageName != null) {
            installerPackages.put(targetPackage, installerPackageName);
        }
    }

    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        List<ResolveInfo> out = new ArrayList<>();
        if (intent == null || manifest == null)
            return out;
        ComponentName cmp = intent.getComponent();
        String targetPkg = intent.getPackage();
        for (AndroidManifest.Activity act : manifest.activities) {
            if (act.name == null)
                continue;
            if (cmp != null) {
                if (act.name.equals(cmp.getClassName())) {
                    out.add(toResolveActivity(act));
                }
            } else if ((targetPkg == null || targetPkg.equals(manifest.packageName))
                    && (act.launcher || Intent.ACTION_MAIN.equals(intent.getAction()))) {
                out.add(toResolveActivity(act));
            }
        }
        return out;
    }

    public ResolveInfo resolveActivity(Intent intent, int flags) {
        List<ResolveInfo> list = queryIntentActivities(intent, flags);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<ResolveInfo> queryIntentActivities(Intent intent, ResolveInfoFlags flags) {
        return queryIntentActivities(intent, flags != null ? (int) flags.getValue() : 0);
    }

    public ResolveInfo resolveActivity(Intent intent, ResolveInfoFlags flags) {
        return resolveActivity(intent, flags != null ? (int) flags.getValue() : 0);
    }

    public List<ResolveInfo> queryIntentServices(Intent intent, ResolveInfoFlags flags) {
        return queryIntentServices(intent, flags != null ? (int) flags.getValue() : 0);
    }

    public ResolveInfo resolveService(Intent intent, ResolveInfoFlags flags) {
        return resolveService(intent, flags != null ? (int) flags.getValue() : 0);
    }

    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, ResolveInfoFlags flags) {
        return queryBroadcastReceivers(intent, flags != null ? (int) flags.getValue() : 0);
    }

    public List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
        List<ResolveInfo> out = new ArrayList<>();
        if (intent == null)
            return out;
        if (MicroGServiceRegistry.canHandleService(intent)) {
            out.addAll(MicroGServiceRegistry.queryVirtualServices(intent));
        }
        if (manifest != null) {
            ComponentName cmp = intent.getComponent();
            String action = intent.getAction();
            for (AndroidManifest.Service svc : manifest.services) {
                if (svc.enabled && matchesComponentOrAction(svc.name, cmp, action, svc.intentFilters)) {
                    ResolveInfo ri = new ResolveInfo();
                    ri.serviceInfo = toServiceInfo(manifest, appInfo, svc);
                    ri.resolvePackageName = manifest.packageName;
                    out.add(ri);
                }
            }
        }
        return out;
    }

    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
        List<ResolveInfo> out = new ArrayList<>();
        if (intent == null || manifest == null)
            return out;
        ComponentName cmp = intent.getComponent();
        String action = intent.getAction();
        for (AndroidManifest.Receiver r : manifest.receivers) {
            if (r.enabled && matchesComponentOrAction(r.name, cmp, action, r.intentFilters)) {
                ResolveInfo ri = new ResolveInfo();
                ri.activityInfo = toReceiverInfo(manifest, appInfo, r);
                ri.resolvePackageName = manifest.packageName;
                out.add(ri);
            }
        }
        return out;
    }

    private static boolean matchesComponentOrAction(String name, ComponentName cmp, String action,
            List<AndroidManifest.IntentFilterInfo> filters) {
        if (name == null)
            return false;
        if (cmp != null)
            return name.equals(cmp.getClassName());
        if (action == null)
            return false;
        for (AndroidManifest.IntentFilterInfo f : filters) {
            if (f.actions.contains(action))
                return true;
        }
        return false;
    }

    public ResolveInfo resolveService(Intent intent, int flags) {
        List<ResolveInfo> list = queryIntentServices(intent, flags);
        return list.isEmpty() ? null : list.get(0);
    }

    public ServiceInfo getServiceInfo(ComponentName component, int flags) throws NameNotFoundException {
        if (manifest != null && component != null) {
            AndroidManifest.Service svc = manifest.findService(component.getClassName());
            if (svc != null) {
                return toServiceInfo(manifest, appInfo, svc);
            }
        }
        throw new NameNotFoundException(component != null ? component.flattenToString() : "null");
    }

    public ActivityInfo getReceiverInfo(ComponentName component, int flags) throws NameNotFoundException {
        if (manifest != null && component != null) {
            String target = component.getClassName();
            for (AndroidManifest.Receiver r : manifest.receivers) {
                if (target.equals(r.name)) {
                    return toReceiverInfo(manifest, appInfo, r);
                }
            }
        }
        throw new NameNotFoundException(component != null ? component.flattenToString() : "null");
    }

    public ProviderInfo getProviderInfo(ComponentName component, int flags) throws NameNotFoundException {
        if (manifest != null && component != null) {
            String target = component.getClassName();
            for (AndroidManifest.Provider p : manifest.providers) {
                if (target.equals(p.name)) {
                    return toProviderInfo(manifest, appInfo, p);
                }
            }
        }
        throw new NameNotFoundException(component != null ? component.flattenToString() : "null");
    }

    public ProviderInfo resolveContentProvider(String name, int flags) {
        if (manifest == null || name == null)
            return null;
        AndroidManifest.Provider p = manifest.findProviderByAuthority(name);
        if (p == null)
            return null;
        return toProviderInfo(manifest, appInfo, p);
    }

    public ActivityInfo getActivityInfo(ComponentName component, int flags) throws NameNotFoundException {
        if (manifest != null && component != null) {
            AndroidManifest.Activity act = manifest.findActivity(component.getClassName());
            if (act != null) {
                return toActivityInfo(manifest, appInfo, act);
            }
        }
        throw new NameNotFoundException(component != null ? component.flattenToString() : "null");
    }

    public ActivityInfo getActivityInfo(ComponentName component, ComponentInfoFlags flags) throws NameNotFoundException {
        return getActivityInfo(component, flags != null ? (int) flags.getValue() : 0);
    }

    public ServiceInfo getServiceInfo(ComponentName component, ComponentInfoFlags flags) throws NameNotFoundException {
        return getServiceInfo(component, flags != null ? (int) flags.getValue() : 0);
    }

    public ActivityInfo getReceiverInfo(ComponentName component, ComponentInfoFlags flags) throws NameNotFoundException {
        return getReceiverInfo(component, flags != null ? (int) flags.getValue() : 0);
    }

    public ProviderInfo getProviderInfo(ComponentName component, ComponentInfoFlags flags) throws NameNotFoundException {
        return getProviderInfo(component, flags != null ? (int) flags.getValue() : 0);
    }

    public ProviderInfo resolveContentProvider(String name, ComponentInfoFlags flags) {
        return resolveContentProvider(name, flags != null ? (int) flags.getValue() : 0);
    }

    private ResolveInfo toResolveActivity(AndroidManifest.Activity act) {
        ResolveInfo ri = new ResolveInfo();
        ri.activityInfo = toActivityInfo(manifest, appInfo, act);
        ri.resolvePackageName = manifest.packageName;
        ri.nonLocalizedLabel = act.label;
        return ri;
    }

    private static ActivityInfo toActivityInfo(AndroidManifest mf, ApplicationInfo ai, AndroidManifest.Activity act) {
        ActivityInfo info = new ActivityInfo();
        info.packageName = mf.packageName;
        info.name = act.name;
        info.nonLocalizedLabel = act.label;
        info.theme = act.themeRes;
        info.applicationInfo = ai;
        info.metaData = toBundle(act.metaData);
        return info;
    }

    private static ActivityInfo toReceiverInfo(AndroidManifest mf, ApplicationInfo ai, AndroidManifest.Receiver r) {
        ActivityInfo info = new ActivityInfo();
        info.packageName = mf.packageName;
        info.name = r.name;
        info.permission = r.permission;
        info.enabled = r.enabled;
        info.exported = r.exported;
        info.applicationInfo = ai;
        info.metaData = toBundle(r.metaData);
        return info;
    }

    private static ServiceInfo toServiceInfo(AndroidManifest mf, ApplicationInfo ai, AndroidManifest.Service svc) {
        ServiceInfo si = new ServiceInfo();
        si.packageName = mf.packageName;
        si.name = svc.name;
        si.permission = svc.permission;
        si.enabled = svc.enabled;
        si.exported = svc.exported;
        si.applicationInfo = ai;
        si.metaData = toBundle(svc.metaData);
        return si;
    }

    private static ProviderInfo toProviderInfo(AndroidManifest mf, ApplicationInfo ai, AndroidManifest.Provider p) {
        ProviderInfo pi = new ProviderInfo();
        pi.packageName = mf.packageName;
        pi.name = p.name;
        pi.authority = p.authorities;
        pi.enabled = p.enabled;
        pi.exported = p.exported;
        pi.grantUriPermissions = p.grantUriPermissions;
        pi.initOrder = p.initOrder;
        pi.readPermission = p.readPermission;
        pi.writePermission = p.writePermission;
        pi.applicationInfo = ai;
        pi.metaData = toBundle(p.metaData);
        return pi;
    }
}
