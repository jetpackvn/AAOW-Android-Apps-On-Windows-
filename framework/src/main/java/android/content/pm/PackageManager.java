package android.content.pm;

import android.content.Intent;
import dev.aaow.apk.AndroidManifest;

public class PackageManager {
    public static final int MATCH_DEFAULT_ONLY = 0x00010000;
    public static final int GET_ACTIVITIES = 0x00000001;
    public static final int GET_META_DATA = 0x00000080;
    public static final int PERMISSION_GRANTED = 0;
    public static final int PERMISSION_DENIED = -1;

    public static class NameNotFoundException extends Exception {
        public NameNotFoundException() {
        }

        public NameNotFoundException(String name) {
            super(name);
        }
    }

    private final AndroidManifest manifest;
    private final ApplicationInfo appInfo;

    public PackageManager(AndroidManifest manifest, ApplicationInfo appInfo) {
        this.manifest = manifest;
        this.appInfo = appInfo;
    }

    public PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException {
        if (manifest != null && (packageName == null || packageName.equals(manifest.packageName))) {
            PackageInfo pi = new PackageInfo();
            pi.packageName = manifest.packageName;
            pi.versionName = manifest.versionName;
            pi.versionCode = manifest.versionCode;
            pi.longVersionCode = manifest.versionCode;
            pi.applicationInfo = appInfo;
            return pi;
        }
        throw new NameNotFoundException(packageName);
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags) throws NameNotFoundException {
        if (appInfo != null && (packageName == null || packageName.equals(appInfo.packageName))) {
            return appInfo;
        }
        throw new NameNotFoundException(packageName);
    }

    public CharSequence getApplicationLabel(ApplicationInfo info) {
        if (info != null && info.nonLocalizedLabel != null) return info.nonLocalizedLabel;
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
}
