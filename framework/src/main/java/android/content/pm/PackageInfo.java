package android.content.pm;

public class PackageInfo {
    public String packageName;
    public String versionName;
    public int versionCode;
    public long longVersionCode;
    public ApplicationInfo applicationInfo;
    public ActivityInfo[] activities;

    public long getLongVersionCode() {
        return longVersionCode != 0 ? longVersionCode : versionCode;
    }
}
