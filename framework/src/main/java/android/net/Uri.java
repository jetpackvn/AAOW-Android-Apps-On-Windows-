package android.net;

import java.net.URI;

public final class Uri {
    private final String uriString;

    private Uri(String uriString) {
        this.uriString = uriString != null ? uriString : "";
    }

    public static Uri parse(String uriString) {
        return new Uri(uriString);
    }

    public String getScheme() {
        try {
            return URI.create(uriString).getScheme();
        } catch (Exception e) {
            int i = uriString.indexOf(':');
            return i > 0 ? uriString.substring(0, i) : null;
        }
    }

    public String getHost() {
        try {
            return URI.create(uriString).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    public String getPath() {
        try {
            return URI.create(uriString).getPath();
        } catch (Exception e) {
            return uriString;
        }
    }

    @Override
    public String toString() {
        return uriString;
    }
}
