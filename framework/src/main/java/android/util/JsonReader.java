package android.util;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/** Bộ đọc JSON dạng luồng android.util.JsonReader tuân thủ đặc tả RFC 7159. */
public final class JsonReader implements Closeable {
    private enum Scope {
        EMPTY_DOCUMENT,
        NONEMPTY_DOCUMENT,
        EMPTY_ARRAY,
        NONEMPTY_ARRAY,
        EMPTY_OBJECT,
        DANGLING_NAME,
        NONEMPTY_OBJECT,
        CLOSED
    }

    private final Reader in;
    private final List<Scope> stack = new ArrayList<>();
    private final char[] buffer = new char[1024];
    private int pos = 0;
    private int limit = 0;
    private boolean lenient = false;
    private JsonToken peekedToken;
    private String peekedValue;

    public JsonReader(Reader in) {
        if (in == null) throw new NullPointerException("in == null");
        this.in = in;
        stack.add(Scope.EMPTY_DOCUMENT);
    }

    public void setLenient(boolean lenient) {
        this.lenient = lenient;
    }

    public boolean isLenient() {
        return lenient;
    }

    public void beginArray() throws IOException {
        expect(JsonToken.BEGIN_ARRAY);
        stack.set(stack.size() - 1, advanceParent(top()));
        stack.add(Scope.EMPTY_ARRAY);
        peekedToken = null;
    }

    public void endArray() throws IOException {
        expect(JsonToken.END_ARRAY);
        stack.remove(stack.size() - 1);
        peekedToken = null;
    }

    public void beginObject() throws IOException {
        expect(JsonToken.BEGIN_OBJECT);
        stack.set(stack.size() - 1, advanceParent(top()));
        stack.add(Scope.EMPTY_OBJECT);
        peekedToken = null;
    }

    public void endObject() throws IOException {
        expect(JsonToken.END_OBJECT);
        stack.remove(stack.size() - 1);
        peekedToken = null;
    }

    public boolean hasNext() throws IOException {
        JsonToken token = peek();
        return token != JsonToken.END_OBJECT && token != JsonToken.END_ARRAY && token != JsonToken.END_DOCUMENT;
    }

    public JsonToken peek() throws IOException {
        if (peekedToken != null) return peekedToken;
        Scope scope = top();
        if (scope == Scope.CLOSED) throw new IllegalStateException("JsonReader is closed");

        if (scope == Scope.EMPTY_ARRAY) {
            int c = nextNonWhitespace(true);
            if (c == ']') return peekedToken = JsonToken.END_ARRAY;
            pos--;
        } else if (scope == Scope.NONEMPTY_ARRAY) {
            int c = nextNonWhitespace(true);
            if (c == ']') return peekedToken = JsonToken.END_ARRAY;
            if (c != ',') throw syntaxError("Expected ',' or ']'");
        } else if (scope == Scope.EMPTY_OBJECT || scope == Scope.NONEMPTY_OBJECT) {
            if (scope == Scope.NONEMPTY_OBJECT) {
                int c = nextNonWhitespace(true);
                if (c == '}') return peekedToken = JsonToken.END_OBJECT;
                if (c != ',') throw syntaxError("Expected ',' or '}'");
            }
            int c = nextNonWhitespace(true);
            if (c == '}' && scope == Scope.EMPTY_OBJECT) {
                return peekedToken = JsonToken.END_OBJECT;
            }
            if (c == '"') {
                peekedValue = readQuoted('"');
                return peekedToken = JsonToken.NAME;
            }
            if (c == '\'' && lenient) {
                peekedValue = readQuoted('\'');
                return peekedToken = JsonToken.NAME;
            }
            if (lenient && c != -1) {
                pos--;
                peekedValue = readUnquoted();
                return peekedToken = JsonToken.NAME;
            }
            throw syntaxError("Expected name");
        } else if (scope == Scope.DANGLING_NAME) {
            int c = nextNonWhitespace(true);
            if (c != ':') throw syntaxError("Expected ':'");
        } else if (scope == Scope.EMPTY_DOCUMENT) {
            int c = nextNonWhitespace(false);
            if (c == -1) return peekedToken = JsonToken.END_DOCUMENT;
            pos--;
        } else if (scope == Scope.NONEMPTY_DOCUMENT) {
            int c = nextNonWhitespace(false);
            if (c == -1) return peekedToken = JsonToken.END_DOCUMENT;
            if (!lenient) throw syntaxError("Expected EOF");
            pos--;
        }

        return peekValue();
    }

    private JsonToken peekValue() throws IOException {
        int c = nextNonWhitespace(true);
        switch (c) {
            case '{' -> { return peekedToken = JsonToken.BEGIN_OBJECT; }
            case '[' -> { return peekedToken = JsonToken.BEGIN_ARRAY; }
            case '"' -> {
                peekedValue = readQuoted('"');
                return peekedToken = JsonToken.STRING;
            }
            case '\'' -> {
                if (!lenient) throw syntaxError("Unexpected single quote");
                peekedValue = readQuoted('\'');
                return peekedToken = JsonToken.STRING;
            }
            default -> {
                pos--;
                String literal = readUnquoted();
                if (literal.isEmpty()) throw syntaxError("Unexpected value");
                if ("null".equals(literal)) {
                    peekedValue = null;
                    return peekedToken = JsonToken.NULL;
                }
                if ("true".equals(literal) || "false".equals(literal)) {
                    peekedValue = literal;
                    return peekedToken = JsonToken.BOOLEAN;
                }
                if (looksLikeNumber(literal)) {
                    peekedValue = literal;
                    return peekedToken = JsonToken.NUMBER;
                }
                if (lenient) {
                    peekedValue = literal;
                    return peekedToken = JsonToken.STRING;
                }
                throw syntaxError("Unexpected literal: " + literal);
            }
        }
    }

    public String nextName() throws IOException {
        expect(JsonToken.NAME);
        stack.set(stack.size() - 1, Scope.DANGLING_NAME);
        String name = peekedValue;
        consume();
        return name;
    }

    public String nextString() throws IOException {
        JsonToken t = peek();
        if (t != JsonToken.STRING && t != JsonToken.NUMBER) {
            throw new IllegalStateException("Expected STRING but was " + t);
        }
        String res = peekedValue;
        consumeAndAdvance();
        return res;
    }

    public boolean nextBoolean() throws IOException {
        expect(JsonToken.BOOLEAN);
        boolean res = "true".equals(peekedValue);
        consumeAndAdvance();
        return res;
    }

    public void nextNull() throws IOException {
        expect(JsonToken.NULL);
        consumeAndAdvance();
    }

    public double nextDouble() throws IOException {
        JsonToken t = peek();
        if (t != JsonToken.NUMBER && t != JsonToken.STRING) {
            throw new IllegalStateException("Expected NUMBER but was " + t);
        }
        double d = Double.parseDouble(peekedValue);
        consumeAndAdvance();
        return d;
    }

    public long nextLong() throws IOException {
        JsonToken t = peek();
        if (t != JsonToken.NUMBER && t != JsonToken.STRING) {
            throw new IllegalStateException("Expected NUMBER but was " + t);
        }
        long val;
        try {
            val = Long.parseLong(peekedValue);
        } catch (NumberFormatException e) {
            double d = Double.parseDouble(peekedValue);
            val = (long) d;
            if ((double) val != d) throw e;
        }
        consumeAndAdvance();
        return val;
    }

    public int nextInt() throws IOException {
        JsonToken t = peek();
        if (t != JsonToken.NUMBER && t != JsonToken.STRING) {
            throw new IllegalStateException("Expected NUMBER but was " + t);
        }
        int val;
        try {
            val = Integer.parseInt(peekedValue);
        } catch (NumberFormatException e) {
            double d = Double.parseDouble(peekedValue);
            val = (int) d;
            if ((double) val != d) throw e;
        }
        consumeAndAdvance();
        return val;
    }

    public void skipValue() throws IOException {
        int depth = 0;
        do {
            JsonToken t = peek();
            switch (t) {
                case BEGIN_ARRAY -> {
                    beginArray();
                    depth++;
                }
                case END_ARRAY -> {
                    endArray();
                    depth--;
                }
                case BEGIN_OBJECT -> {
                    beginObject();
                    depth++;
                }
                case END_OBJECT -> {
                    endObject();
                    depth--;
                }
                case NAME -> nextName();
                case END_DOCUMENT -> { return; }
                default -> consumeAndAdvance();
            }
        } while (depth > 0);
    }

    @Override
    public void close() throws IOException {
        peekedToken = null;
        stack.clear();
        stack.add(Scope.CLOSED);
        in.close();
    }

    @Override
    public String toString() {
        return "JsonReader(scope=" + top() + ")";
    }

    private Scope top() {
        return stack.get(stack.size() - 1);
    }

    private Scope advanceParent(Scope scope) {
        return switch (scope) {
            case EMPTY_DOCUMENT -> Scope.NONEMPTY_DOCUMENT;
            case EMPTY_ARRAY -> Scope.NONEMPTY_ARRAY;
            case DANGLING_NAME -> Scope.NONEMPTY_OBJECT;
            default -> scope;
        };
    }

    private void expect(JsonToken expected) throws IOException {
        JsonToken actual = peek();
        if (actual != expected) {
            throw new IllegalStateException("Expected " + expected + " but was " + actual);
        }
    }

    private void consume() {
        peekedToken = null;
        peekedValue = null;
    }

    private void consumeAndAdvance() {
        consume();
        stack.set(stack.size() - 1, advanceParent(top()));
    }

    private int nextChar() throws IOException {
        if (pos < limit) return buffer[pos++];
        int read = in.read(buffer);
        if (read == -1) return -1;
        pos = 1;
        limit = read;
        return buffer[0];
    }

    private int nextNonWhitespace(boolean throwOnEof) throws IOException {
        while (true) {
            int c = nextChar();
            if (c == -1) {
                if (throwOnEof) throw new EOFException("End of JSON input");
                return -1;
            }
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') continue;
            if (c == '/' && lenient) {
                int next = nextChar();
                if (next == '/') {
                    while ((next = nextChar()) != -1 && next != '\n' && next != '\r') {
                    }
                    continue;
                } else if (next == '*') {
                    while ((next = nextChar()) != -1) {
                        if (next == '*') {
                            int after = nextChar();
                            if (after == '/') break;
                        }
                    }
                    continue;
                } else if (next != -1) {
                    pos--;
                }
            }
            return c;
        }
    }

    private String readQuoted(char quote) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int c = nextChar();
            if (c == -1) throw syntaxError("Unterminated string");
            if (c == quote) return sb.toString();
            if (c == '\\') {
                int esc = nextChar();
                if (esc == -1) throw syntaxError("Unterminated escape");
                switch (esc) {
                    case '"' -> sb.append('"');
                    case '\'' -> sb.append('\'');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        char[] hex = new char[4];
                        for (int i = 0; i < 4; i++) {
                            int hc = nextChar();
                            if (hc == -1) throw syntaxError("Unterminated unicode escape");
                            hex[i] = (char) hc;
                        }
                        sb.append((char) Integer.parseInt(new String(hex), 16));
                    }
                    default -> sb.append((char) esc);
                }
            } else {
                sb.append((char) c);
            }
        }
    }

    private String readUnquoted() throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int c = nextChar();
            if (c == -1) break;
            if (c <= ' ' || c == ',' || c == ']' || c == '}' || c == ':' || c == '{' || c == '[') {
                pos--;
                break;
            }
            sb.append((char) c);
        }
        return sb.toString();
    }

    private static boolean looksLikeNumber(String s) {
        char c0 = s.charAt(0);
        if ((c0 < '0' || c0 > '9') && c0 != '-') return false;
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private IOException syntaxError(String msg) {
        return new IOException("Malformed JSON: " + msg);
    }
}
