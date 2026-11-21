package android.content;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PatternMatcher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class IntentFilter implements Parcelable {
    public static final int SYSTEM_HIGH_PRIORITY = 1000;
    public static final int SYSTEM_LOW_PRIORITY = -1000;

    public static final int MATCH_CATEGORY_MASK = 0xfff0000;
    public static final int MATCH_ADJUSTMENT_MASK = 0x000ffff;
    public static final int MATCH_ADJUSTMENT_NORMAL = 0x8000;
    public static final int MATCH_CATEGORY_EMPTY = 0x0100000;
    public static final int MATCH_CATEGORY_SCHEME = 0x0200000;
    public static final int MATCH_CATEGORY_HOST = 0x0300000;
    public static final int MATCH_CATEGORY_PORT = 0x0400000;
    public static final int MATCH_CATEGORY_PATH = 0x0500000;
    public static final int MATCH_CATEGORY_SCHEME_SPECIFIC_PART = 0x0580000;
    public static final int MATCH_CATEGORY_TYPE = 0x0600000;

    public static final int NO_MATCH_TYPE = -1;
    public static final int NO_MATCH_DATA = -2;
    public static final int NO_MATCH_ACTION = -3;
    public static final int NO_MATCH_CATEGORY = -4;

    public static final class AuthorityEntry {
        private final String host;
        private final int port;

        public AuthorityEntry(String host, String port) {
            this.host = host != null ? host : "";
            int p = -1;
            if (port != null && !port.isEmpty()) {
                try {
                    p = Integer.parseInt(port);
                } catch (NumberFormatException ignored) {
                }
            }
            this.port = p;
        }

        public String getHost() { return host; }
        public int getPort() { return port; }

        public int match(Uri data) {
            if (data == null) return NO_MATCH_DATA;
            String h = data.getHost();
            if (h == null || !host.equalsIgnoreCase(h)) return NO_MATCH_DATA;
            if (port >= 0) {
                return port == data.getPort() ? MATCH_CATEGORY_PORT : NO_MATCH_DATA;
            }
            return MATCH_CATEGORY_HOST;
        }
    }

    private int priority;
    private final List<String> actions = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private final List<String> dataSchemes = new ArrayList<>();
    private final List<String> dataTypes = new ArrayList<>();
    private final List<AuthorityEntry> dataAuthorities = new ArrayList<>();
    private final List<PatternMatcher> dataPaths = new ArrayList<>();

    public IntentFilter() {
    }

    public IntentFilter(String action) {
        addAction(action);
    }

    public IntentFilter(String action, String dataType) {
        addAction(action);
        addDataType(dataType);
    }

    public IntentFilter(IntentFilter o) {
        if (o != null) {
            this.priority = o.priority;
            this.actions.addAll(o.actions);
            this.categories.addAll(o.categories);
            this.dataSchemes.addAll(o.dataSchemes);
            this.dataTypes.addAll(o.dataTypes);
        }
    }

    public final void setPriority(int priority) {
        this.priority = priority;
    }

    public final int getPriority() {
        return priority;
    }

    public final void addAction(String action) {
        if (action != null && !actions.contains(action)) actions.add(action);
    }

    public final int countActions() {
        return actions.size();
    }

    public final String getAction(int index) {
        return actions.get(index);
    }

    public final boolean hasAction(String action) {
        return action != null && actions.contains(action);
    }

    public final Iterator<String> actionsIterator() {
        return actions.iterator();
    }

    public final void addCategory(String category) {
        if (category != null && !categories.contains(category)) categories.add(category);
    }

    public final int countCategories() {
        return categories.size();
    }

    public final String getCategory(int index) {
        return categories.get(index);
    }

    public final boolean hasCategory(String category) {
        return category != null && categories.contains(category);
    }

    public final void addDataScheme(String scheme) {
        if (scheme != null && !dataSchemes.contains(scheme)) dataSchemes.add(scheme);
    }

    public final int countDataSchemes() {
        return dataSchemes.size();
    }

    public final String getDataScheme(int index) {
        return dataSchemes.get(index);
    }

    public final boolean hasDataScheme(String scheme) {
        return scheme != null && dataSchemes.contains(scheme);
    }

    public final void addDataType(String type) {
        if (type != null && !dataTypes.contains(type)) dataTypes.add(type);
    }

    public final int countDataTypes() {
        return dataTypes.size();
    }

    public final boolean hasDataType(String type) {
        return type != null && dataTypes.contains(type);
    }

    public final boolean matchAction(String action) {
        return hasAction(action);
    }

    public final Iterator<String> categoriesIterator() {
        return categories.iterator();
    }

    public final Iterator<String> schemesIterator() {
        return dataSchemes.iterator();
    }

    public final Iterator<String> typesIterator() {
        return dataTypes.iterator();
    }

    public final void addDataAuthority(String host, String port) {
        dataAuthorities.add(new AuthorityEntry(host, port));
    }

    public final int countDataAuthorities() {
        return dataAuthorities.size();
    }

    public final AuthorityEntry getDataAuthority(int index) {
        return dataAuthorities.get(index);
    }

    public final void addDataPath(String path, int type) {
        dataPaths.add(new PatternMatcher(path, type));
    }

    public final int countDataPaths() {
        return dataPaths.size();
    }

    public final PatternMatcher getDataPath(int index) {
        return dataPaths.get(index);
    }

    public final String matchCategories(Set<String> cats) {
        if (cats == null) return null;
        for (String c : cats) {
            if (!categories.contains(c)) {
                return c;
            }
        }
        return null;
    }

    public final int matchDataAuthority(Uri data) {
        if (dataAuthorities.isEmpty()) return NO_MATCH_DATA;
        for (AuthorityEntry ae : dataAuthorities) {
            int match = ae.match(data);
            if (match >= 0) return match;
        }
        return NO_MATCH_DATA;
    }

    public final int matchData(String type, String scheme, Uri data) {
        boolean hasTypes = !dataTypes.isEmpty();
        boolean hasSchemes = !dataSchemes.isEmpty();
        if (!hasTypes && !hasSchemes) {
            return (type == null && data == null) ? (MATCH_CATEGORY_EMPTY + MATCH_ADJUSTMENT_NORMAL) : NO_MATCH_DATA;
        }
        int match = MATCH_CATEGORY_EMPTY;
        if (hasSchemes) {
            if (scheme == null || !dataSchemes.contains(scheme)) {
                return NO_MATCH_DATA;
            }
            match = MATCH_CATEGORY_SCHEME;
            if (!dataAuthorities.isEmpty()) {
                int authMatch = matchDataAuthority(data);
                if (authMatch < 0) return NO_MATCH_DATA;
                match = authMatch;
            }
            if (!dataPaths.isEmpty()) {
                String p = data != null ? data.getPath() : null;
                boolean matched = false;
                for (PatternMatcher pm : dataPaths) {
                    if (pm.match(p)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) return NO_MATCH_DATA;
                match = MATCH_CATEGORY_PATH;
            }
        } else if (scheme != null && !scheme.isEmpty() && !"content".equals(scheme) && !"file".equals(scheme)) {
            return NO_MATCH_DATA;
        }
        if (hasTypes) {
            if (!findMimeType(type)) return NO_MATCH_TYPE;
            match = MATCH_CATEGORY_TYPE;
        } else if (type != null) {
            return NO_MATCH_TYPE;
        }
        return match + MATCH_ADJUSTMENT_NORMAL;
    }

    private boolean findMimeType(String type) {
        if (type == null) return false;
        if (dataTypes.contains(type) || dataTypes.contains("*/*") || dataTypes.contains("*")) {
            return true;
        }
        int slash = type.indexOf('/');
        if (slash > 0) {
            String prefix = type.substring(0, slash);
            if (dataTypes.contains(prefix) || dataTypes.contains(prefix + "/*")) {
                return true;
            }
        }
        return false;
    }

    public final int match(ContentResolver resolver, Intent intent, boolean resolve, String logTag) {
        if (intent == null) return NO_MATCH_ACTION;
        String type = resolve ? intent.resolveType(resolver) : intent.getType();
        return match(intent.getAction(), type, intent.getScheme(), intent.getData(), intent.getCategories(), logTag);
    }

    public final int match(String action, String type, String scheme, Uri data, Set<String> categories, String logTag) {
        if (action != null && !matchAction(action)) {
            return NO_MATCH_ACTION;
        }
        int dataMatch = matchData(type, scheme, data);
        if (dataMatch < 0) {
            return dataMatch;
        }
        if (matchCategories(categories) != null) {
            return NO_MATCH_CATEGORY;
        }
        return dataMatch;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(priority);
        dest.writeStringList(actions);
        dest.writeStringList(categories);
    }

    public static final Creator<IntentFilter> CREATOR = new Creator<>() {
        @Override
        public IntentFilter createFromParcel(Parcel source) {
            IntentFilter f = new IntentFilter();
            f.setPriority(source.readInt());
            f.actions.addAll(source.createStringArrayList());
            f.categories.addAll(source.createStringArrayList());
            return f;
        }

        @Override
        public IntentFilter[] newArray(int size) {
            return new IntentFilter[size];
        }
    };
}
