package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class Uri implements Comparable<Uri>, Parcelable {
    public static final Uri EMPTY = new Uri("");

    private final String uriString;

    private Uri(String uriString) {
        this.uriString = uriString != null ? uriString : "";
    }

    public static Uri parse(String uriString) {
        return new Uri(uriString);
    }

    public static Uri fromFile(File file) {
        if (file == null) throw new NullPointerException("file");
        return new Uri(file.toURI().toString());
    }

    public static Uri withAppendedPath(Uri baseUri, String pathSegment) {
        return baseUri.buildUpon().appendEncodedPath(pathSegment).build();
    }

    public static String encode(String s) {
        return s == null ? null : URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    public static String decode(String s) {
        return s == null ? null : URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private URI toJavaUri() {
        try {
            return URI.create(uriString);
        } catch (Exception e) {
            return null;
        }
    }

    public String getScheme() {
        URI u = toJavaUri();
        if (u != null) return u.getScheme();
        int i = uriString.indexOf(':');
        return i > 0 ? uriString.substring(0, i) : null;
    }

    public String getAuthority() {
        URI u = toJavaUri();
        return u != null ? u.getAuthority() : null;
    }

    public String getHost() {
        URI u = toJavaUri();
        return u != null ? u.getHost() : null;
    }

    public int getPort() {
        URI u = toJavaUri();
        return u != null ? u.getPort() : -1;
    }

    public String getPath() {
        URI u = toJavaUri();
        return u != null ? u.getPath() : uriString;
    }

    public String getEncodedPath() {
        URI u = toJavaUri();
        return u != null ? u.getRawPath() : uriString;
    }

    public List<String> getPathSegments() {
        String p = getPath();
        if (p == null || p.isEmpty()) return Collections.emptyList();
        List<String> segs = new ArrayList<>();
        for (String part : p.split("/")) {
            if (!part.isEmpty()) segs.add(part);
        }
        return Collections.unmodifiableList(segs);
    }

    public String getLastPathSegment() {
        List<String> segs = getPathSegments();
        return segs.isEmpty() ? null : segs.get(segs.size() - 1);
    }

    public String getQuery() {
        URI u = toJavaUri();
        return u != null ? u.getQuery() : null;
    }

    public String getEncodedQuery() {
        URI u = toJavaUri();
        return u != null ? u.getRawQuery() : null;
    }

    public String getFragment() {
        URI u = toJavaUri();
        return u != null ? u.getFragment() : null;
    }

    public Set<String> getQueryParameterNames() {
        String q = getEncodedQuery();
        if (q == null || q.isEmpty()) return Collections.emptySet();
        Set<String> names = new LinkedHashSet<>();
        for (String pair : q.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            names.add(decode(key));
        }
        return Collections.unmodifiableSet(names);
    }

    public List<String> getQueryParameters(String key) {
        if (key == null) return Collections.emptyList();
        String q = getEncodedQuery();
        if (q == null || q.isEmpty()) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (String pair : q.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            String k = decode(eq >= 0 ? pair.substring(0, eq) : pair);
            if (key.equals(k)) {
                out.add(eq >= 0 ? decode(pair.substring(eq + 1)) : "");
            }
        }
        return Collections.unmodifiableList(out);
    }

    public String getQueryParameter(String key) {
        List<String> vals = getQueryParameters(key);
        return vals.isEmpty() ? null : vals.get(0);
    }

    public Builder buildUpon() {
        return new Builder()
                .scheme(getScheme())
                .authority(getAuthority())
                .path(getPath())
                .query(getQuery())
                .fragment(getFragment());
    }

    public static final class Builder {
        private String scheme;
        private String authority;
        private String path = "";
        private String query;
        private String fragment;

        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        public Builder authority(String authority) {
            this.authority = authority;
            return this;
        }

        public Builder path(String path) {
            this.path = path != null ? path : "";
            return this;
        }

        public Builder appendPath(String newSegment) {
            return appendEncodedPath(encode(newSegment));
        }

        public Builder appendEncodedPath(String newSegment) {
            if (newSegment == null || newSegment.isEmpty()) return this;
            if (path.isEmpty()) {
                path = newSegment.startsWith("/") ? newSegment : "/" + newSegment;
            } else if (path.endsWith("/")) {
                path = path + (newSegment.startsWith("/") ? newSegment.substring(1) : newSegment);
            } else {
                path = path + (newSegment.startsWith("/") ? newSegment : "/" + newSegment);
            }
            return this;
        }

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder appendQueryParameter(String key, String value) {
            String pair = encode(key) + "=" + encode(value != null ? value : "");
            this.query = (this.query == null || this.query.isEmpty()) ? pair : this.query + "&" + pair;
            return this;
        }

        public Builder fragment(String fragment) {
            this.fragment = fragment;
            return this;
        }

        public Uri build() {
            StringBuilder sb = new StringBuilder();
            if (scheme != null && !scheme.isEmpty()) {
                sb.append(scheme).append(':');
            }
            if (authority != null) {
                sb.append("//").append(authority);
            }
            if (path != null && !path.isEmpty()) {
                sb.append(path);
            }
            if (query != null && !query.isEmpty()) {
                sb.append('?').append(query);
            }
            if (fragment != null && !fragment.isEmpty()) {
                sb.append('#').append(fragment);
            }
            return new Uri(sb.toString());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(uriString);
    }

    public static final Creator<Uri> CREATOR = new Creator<>() {
        @Override
        public Uri createFromParcel(Parcel in) {
            return parse(in.readString());
        }

        @Override
        public Uri[] newArray(int size) {
            return new Uri[size];
        }
    };

    @Override
    public int compareTo(Uri o) {
        return uriString.compareTo(o != null ? o.uriString : "");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Uri u && Objects.equals(uriString, u.uriString);
    }

    @Override
    public int hashCode() {
        return uriString.hashCode();
    }

    @Override
    public String toString() {
        return uriString;
    }
}
