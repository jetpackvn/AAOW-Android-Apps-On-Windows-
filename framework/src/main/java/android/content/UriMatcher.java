package android.content;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/** Bộ khớp đường dẫn URI chuẩn Android SDK (hỗ trợ * và #). */
public class UriMatcher {
    public static final int NO_MATCH = -1;

    private static final int EXACT = 0;
    private static final int NUMBER = 1;
    private static final int TEXT = 2;

    private int code;
    private final int which;
    private final String text;
    private final List<UriMatcher> children = new ArrayList<>();

    public UriMatcher(int code) {
        this.code = code;
        this.which = -1;
        this.text = null;
    }

    private UriMatcher(int which, String text) {
        this.code = NO_MATCH;
        this.which = which;
        this.text = text;
    }

    public void addURI(String authority, String path, int code) {
        if (code < 0) {
            throw new IllegalArgumentException("code " + code + " is invalid: it must be positive");
        }
        List<String> tokens = new ArrayList<>();
        if (path != null) {
            String clean = path.startsWith("/") ? path.substring(1) : path;
            if (!clean.isEmpty()) {
                for (String seg : clean.split("/")) {
                    if (!seg.isEmpty()) tokens.add(seg);
                }
            }
        }
        UriMatcher node = this;
        for (int i = -1; i < tokens.size(); i++) {
            String token = i < 0 ? authority : tokens.get(i);
            if (token == null) continue;
            UriMatcher found = null;
            for (UriMatcher child : node.children) {
                if (token.equals(child.text)) {
                    found = child;
                    break;
                }
            }
            if (found == null) {
                int type = "#".equals(token) ? NUMBER : ("*".equals(token) ? TEXT : EXACT);
                found = new UriMatcher(type, token);
                node.children.add(found);
            }
            node = found;
        }
        node.code = code;
    }

    public int match(Uri uri) {
        if (uri == null) return NO_MATCH;
        List<String> pathSegments = uri.getPathSegments();
        int li = pathSegments.size();
        if (li == 0 && uri.getAuthority() == null) {
            return this.code;
        }
        UriMatcher node = this;
        for (int i = -1; i < li; i++) {
            String u = i < 0 ? uri.getAuthority() : pathSegments.get(i);
            if (u == null) return NO_MATCH;
            UriMatcher match = null;
            for (UriMatcher child : node.children) {
                switch (child.which) {
                    case EXACT -> {
                        if (u.equals(child.text)) match = child;
                    }
                    case NUMBER -> {
                        if (isDigits(u)) match = child;
                    }
                    case TEXT -> match = child;
                    default -> {
                    }
                }
                if (match != null) break;
            }
            node = match;
            if (node == null) {
                return NO_MATCH;
            }
        }
        return node.code;
    }

    private static boolean isDigits(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }
}
