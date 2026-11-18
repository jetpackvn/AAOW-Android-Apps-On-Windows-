package org.json;

public class JSONTokener {
    private final String in;
    private int pos;

    public JSONTokener(String in) {
        this.in = in != null && in.startsWith("\ufeff") ? in.substring(1) : (in != null ? in : "");
    }

    public Object nextValue() throws JSONException {
        int c = nextCleanInternal();
        return switch (c) {
            case -1 -> throw syntaxError("End of input");
            case '{' -> {
                pos--;
                yield new JSONObject(this);
            }
            case '[' -> {
                pos--;
                yield new JSONArray(this);
            }
            case '\'', '"' -> nextString((char) c);
            default -> {
                pos--;
                yield readLiteral();
            }
        };
    }

    private int nextCleanInternal() throws JSONException {
        while (pos < in.length()) {
            int c = in.charAt(pos++);
            switch (c) {
                case '\t', ' ', '\n', '\r' -> { }
                case '/' -> {
                    if (pos == in.length()) return c;
                    char peek = in.charAt(pos);
                    if (peek == '*') {
                        pos++;
                        int end = in.indexOf("*/", pos);
                        if (end == -1) throw syntaxError("Unterminated comment");
                        pos = end + 2;
                    } else if (peek == '/') {
                        pos++;
                        skipToEndOfLine();
                    } else {
                        return c;
                    }
                }
                case '#' -> skipToEndOfLine();
                default -> {
                    return c;
                }
            }
        }
        return -1;
    }

    private void skipToEndOfLine() {
        while (pos < in.length()) {
            char c = in.charAt(pos++);
            if (c == '\r' || c == '\n') break;
        }
    }

    public String nextString(char quote) throws JSONException {
        StringBuilder builder = null;
        int start = pos;
        while (pos < in.length()) {
            char c = in.charAt(pos++);
            if (c == quote) {
                if (builder == null) return in.substring(start, pos - 1);
                builder.append(in, start, pos - 1);
                return builder.toString();
            }
            if (c == '\\') {
                if (builder == null) builder = new StringBuilder();
                builder.append(in, start, pos - 1);
                builder.append(readEscapeCharacter());
                start = pos;
            }
        }
        throw syntaxError("Unterminated string");
    }

    private char readEscapeCharacter() throws JSONException {
        if (pos >= in.length()) throw syntaxError("Unterminated escape sequence");
        char escaped = in.charAt(pos++);
        return switch (escaped) {
            case 'u' -> {
                if (pos + 4 > in.length()) throw syntaxError("Unterminated \\u escape");
                String hex = in.substring(pos, pos + 4);
                pos += 4;
                try {
                    yield (char) Integer.parseInt(hex, 16);
                } catch (NumberFormatException e) {
                    throw syntaxError("Invalid \\u escape: " + hex);
                }
            }
            case 't' -> '\t';
            case 'b' -> '\b';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 'f' -> '\f';
            default -> escaped;
        };
    }

    private Object readLiteral() throws JSONException {
        String literal = nextToInternal("{}[]/\\:,=;# \t\f");
        if (literal.isEmpty()) throw syntaxError("Expected literal value");
        if ("null".equalsIgnoreCase(literal)) return JSONObject.NULL;
        if ("true".equalsIgnoreCase(literal)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(literal)) return Boolean.FALSE;

        if (literal.indexOf('.') == -1 && literal.indexOf('e') == -1 && literal.indexOf('E') == -1) {
            try {
                long v = Long.parseLong(literal);
                if (v <= Integer.MAX_VALUE && v >= Integer.MIN_VALUE) return (int) v;
                return v;
            } catch (NumberFormatException ignored) {
            }
        }
        try {
            return Double.valueOf(literal);
        } catch (NumberFormatException ignored) {
            return literal;
        }
    }

    private String nextToInternal(String excluded) {
        int start = pos;
        while (pos < in.length()) {
            char c = in.charAt(pos);
            if (c == '\r' || c == '\n' || excluded.indexOf(c) != -1) {
                return in.substring(start, pos);
            }
            pos++;
        }
        return in.substring(start);
    }

    public boolean more() {
        return pos < in.length();
    }

    public char next() {
        return pos < in.length() ? in.charAt(pos++) : '\0';
    }

    public char nextClean() throws JSONException {
        int c = nextCleanInternal();
        return c == -1 ? '\0' : (char) c;
    }

    public void back() {
        if (pos > 0) pos--;
    }

    public JSONException syntaxError(String message) {
        return new JSONException(message + " at index " + pos);
    }
}
