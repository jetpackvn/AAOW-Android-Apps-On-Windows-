package android.text;

public class TextUtils {
    public enum TruncateAt {
        START,
        MIDDLE,
        END,
        MARQUEE
    }

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static boolean equals(CharSequence a, CharSequence b) {
        if (a == b) return true;
        if (a != null && b != null && a.length() == b.length()) {
            if (a instanceof String && b instanceof String) {
                return a.equals(b);
            }
            for (int i = 0; i < a.length(); i++) {
                if (a.charAt(i) != b.charAt(i)) return false;
            }
            return true;
        }
        return false;
    }

    public static String join(CharSequence delimiter, Object[] tokens) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) sb.append(delimiter);
            sb.append(tokens[i]);
        }
        return sb.toString();
    }

    public static String join(CharSequence delimiter, Iterable<?> tokens) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object token : tokens) {
            if (first) first = false;
            else sb.append(delimiter);
            sb.append(token);
        }
        return sb.toString();
    }

    public static CharSequence ellipsize(CharSequence text, TextPaint p, float avail, TruncateAt where) {
        if (text == null) return "";
        String s = text.toString();
        if (p == null || p.measureText(s) <= avail) return text;
        String ellipsis = "…";
        float ellW = p.measureText(ellipsis);
        if (avail <= ellW) return "";
        int len = s.length();
        while (len > 0 && p.measureText(s, 0, len) + ellW > avail) {
            len--;
        }
        return s.substring(0, len) + ellipsis;
    }

    public static String htmlEncode(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '&' -> sb.append("&amp;");
                case '\'' -> sb.append("&#39;");
                case '"' -> sb.append("&quot;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    public static CharSequence stringOrSpannedString(CharSequence source) {
        return source;
    }

    public static String[] split(String text, String expression) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        return text.split(expression, -1);
    }

    public static String[] split(String text, java.util.regex.Pattern pattern) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        return pattern.split(text, -1);
    }
}
