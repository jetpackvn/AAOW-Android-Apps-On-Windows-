package android.os;

/** Dịch vụ quản lý người dùng android.os.UserManager trên Windows. */
public class UserManager {
    public boolean isUserUnlocked() {
        return true;
    }

    public boolean isSystemUser() {
        return true;
    }

    public boolean isAdminUser() {
        return true;
    }

    public boolean isManagedProfile() {
        return false;
    }

    public boolean isProfile() {
        return false;
    }

    public boolean isDemoUser() {
        return false;
    }

    public boolean isUserForeground() {
        return true;
    }

    public String getUserName() {
        return System.getProperty("user.name", "WindowsUser");
    }

    public Bundle getUserRestrictions() {
        return new Bundle();
    }

    public Bundle getApplicationRestrictions(String packageName) {
        return new Bundle();
    }

    public boolean hasUserRestriction(String restrictionKey) {
        return false;
    }

    public static boolean supportsMultipleUsers() {
        return false;
    }
}
