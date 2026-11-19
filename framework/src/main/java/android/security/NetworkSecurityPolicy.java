package android.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cài đặt android.security.NetworkSecurityPolicy dùng bởi OkHttp (AndroidPlatform),
 * Retrofit và HttpURLConnection để kiểm tra quyền kết nối HTTP/HTTPS theo domain.
 */
public class NetworkSecurityPolicy {
    public static final int DOMAIN_ENCRYPTION_MODE_UNKNOWN = 0;
    public static final int DOMAIN_ENCRYPTION_MODE_DISABLED = 1;
    public static final int DOMAIN_ENCRYPTION_MODE_OPPORTUNISTIC = 2;
    public static final int DOMAIN_ENCRYPTION_MODE_ENABLED = 3;

    private static final NetworkSecurityPolicy INSTANCE = new NetworkSecurityPolicy();

    private volatile boolean cleartextTrafficPermitted = true;
    private final Map<String, Boolean> domainCleartextRules = new ConcurrentHashMap<>();

    public NetworkSecurityPolicy() {
    }

    public static NetworkSecurityPolicy getInstance() {
        return INSTANCE;
    }

    public boolean isCleartextTrafficPermitted() {
        return cleartextTrafficPermitted;
    }

    public boolean isCleartextTrafficPermitted(String hostname) {
        if (hostname != null) {
            Boolean rule = domainCleartextRules.get(hostname.toLowerCase());
            if (rule != null) return rule;
        }
        return cleartextTrafficPermitted;
    }

    public void setCleartextTrafficPermitted(boolean permitted) {
        this.cleartextTrafficPermitted = permitted;
    }

    public void setDomainCleartextTrafficPermitted(String hostname, boolean permitted) {
        if (hostname != null) {
            domainCleartextRules.put(hostname.toLowerCase(), permitted);
        }
    }

    public void clearDomainCleartextRules() {
        domainCleartextRules.clear();
    }

    public boolean isCertificateTransparencyVerificationRequired(String hostname) {
        return false;
    }

    public int getDomainEncryptionMode(String hostname) {
        return isCleartextTrafficPermitted(hostname)
                ? DOMAIN_ENCRYPTION_MODE_OPPORTUNISTIC
                : DOMAIN_ENCRYPTION_MODE_ENABLED;
    }
}
