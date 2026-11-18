package android.webkit;

import java.net.CookieHandler;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CookieManager {
    private static final CookieManager INSTANCE = new DefaultCookieManager();

    public CookieManager() {
    }

    public static CookieManager getInstance() {
        return INSTANCE;
    }

    public abstract void setAcceptCookie(boolean accept);

    public abstract boolean acceptCookie();

    public abstract void setAcceptThirdPartyCookies(WebView webview, boolean accept);

    public abstract boolean acceptThirdPartyCookies(WebView webview);

    public abstract void setCookie(String url, String value);

    public abstract void setCookie(String url, String value, ValueCallback<Boolean> callback);

    public abstract String getCookie(String url);

    @Deprecated
    public abstract void removeSessionCookie();

    public abstract void removeSessionCookies(ValueCallback<Boolean> callback);

    @Deprecated
    public abstract void removeAllCookie();

    public abstract void removeAllCookies(ValueCallback<Boolean> callback);

    public abstract boolean hasCookies();

    @Deprecated
    public abstract void removeExpiredCookie();

    public abstract void flush();

    public static boolean allowFileSchemeCookies() {
        return true;
    }

    public static void setAcceptFileSchemeCookies(boolean accept) {
    }

    private static final class DefaultCookieManager extends CookieManager {
        private volatile boolean acceptCookie = true;
        private final java.net.CookieManager netCookies = new java.net.CookieManager(null, CookiePolicy.ACCEPT_ALL);
        private final Map<String, String> rawCookies = new ConcurrentHashMap<>();

        DefaultCookieManager() {
            if (CookieHandler.getDefault() == null) {
                CookieHandler.setDefault(netCookies);
            }
        }

        private static String keyForUrl(String url) {
            if (url == null) return "";
            try {
                URI uri = URI.create(url);
                return uri.getHost() != null ? uri.getHost() : url;
            } catch (Exception e) {
                return url;
            }
        }

        @Override
        public void setAcceptCookie(boolean accept) {
            this.acceptCookie = accept;
        }

        @Override
        public boolean acceptCookie() {
            return acceptCookie;
        }

        @Override
        public void setAcceptThirdPartyCookies(WebView webview, boolean accept) {
        }

        @Override
        public boolean acceptThirdPartyCookies(WebView webview) {
            return acceptCookie;
        }

        @Override
        public void setCookie(String url, String value) {
            if (!acceptCookie || url == null || value == null) return;
            String host = keyForUrl(url);
            rawCookies.merge(host, value, (oldV, newV) -> oldV + "; " + newV);
            try {
                URI uri = URI.create(url);
                for (HttpCookie c : HttpCookie.parse(value)) {
                    netCookies.getCookieStore().add(uri, c);
                }
            } catch (Exception ignored) {
            }
        }

        @Override
        public void setCookie(String url, String value, ValueCallback<Boolean> callback) {
            setCookie(url, value);
            if (callback != null) callback.onReceiveValue(Boolean.TRUE);
        }

        @Override
        public String getCookie(String url) {
            if (url == null) return null;
            try {
                URI uri = URI.create(url);
                List<HttpCookie> list = netCookies.getCookieStore().get(uri);
                if (!list.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (HttpCookie c : list) {
                        if (!sb.isEmpty()) sb.append("; ");
                        sb.append(c.getName()).append('=').append(c.getValue());
                    }
                    return sb.toString();
                }
            } catch (Exception ignored) {
            }
            return rawCookies.get(keyForUrl(url));
        }

        @Override
        public void removeSessionCookie() {
            removeAllCookie();
        }

        @Override
        public void removeSessionCookies(ValueCallback<Boolean> callback) {
            removeAllCookies(callback);
        }

        @Override
        public void removeAllCookie() {
            rawCookies.clear();
            netCookies.getCookieStore().removeAll();
        }

        @Override
        public void removeAllCookies(ValueCallback<Boolean> callback) {
            boolean had = hasCookies();
            removeAllCookie();
            if (callback != null) callback.onReceiveValue(had);
        }

        @Override
        public boolean hasCookies() {
            return !rawCookies.isEmpty() || !netCookies.getCookieStore().getCookies().isEmpty();
        }

        @Override
        public void removeExpiredCookie() {
        }

        @Override
        public void flush() {
        }
    }
}
