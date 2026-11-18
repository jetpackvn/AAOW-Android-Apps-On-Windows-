package android.util;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/** Bộ ghi JSON dạng luồng android.util.JsonWriter. */
public final class JsonWriter implements Closeable, Flushable {
    private enum Scope {
        EMPTY_DOCUMENT,
        NONEMPTY_DOCUMENT,
        EMPTY_ARRAY,
        NONEMPTY_ARRAY,
        EMPTY_OBJECT,
        DANGLING_NAME,
        NONEMPTY_OBJECT
    }

    private final Writer out;
    private final List<Scope> stack = new ArrayList<>();
    private String indent;
    private boolean lenient;
    private String deferredName;

    public JsonWriter(Writer out) {
        if (out == null) throw new NullPointerException("out == null");
        this.out = out;
        stack.add(Scope.EMPTY_DOCUMENT);
    }

    public void setIndent(String indent) {
        this.indent = (indent == null || indent.isEmpty()) ? null : indent;
    }

    public void setLenient(boolean lenient) {
        this.lenient = lenient;
    }

    public boolean isLenient() {
        return lenient;
    }

    public JsonWriter beginArray() throws IOException {
        writeDeferredName();
        beforeValue();
        stack.add(Scope.EMPTY_ARRAY);
        out.write('[');
        return this;
    }

    public JsonWriter endArray() throws IOException {
        Scope s = stack.remove(stack.size() - 1);
        if (s == Scope.NONEMPTY_ARRAY && indent != null) {
            newline();
        }
        out.write(']');
        return this;
    }

    public JsonWriter beginObject() throws IOException {
        writeDeferredName();
        beforeValue();
        stack.add(Scope.EMPTY_OBJECT);
        out.write('{');
        return this;
    }

    public JsonWriter endObject() throws IOException {
        Scope s = stack.remove(stack.size() - 1);
        if (s == Scope.NONEMPTY_OBJECT && indent != null) {
            newline();
        }
        out.write('}');
        return this;
    }

    public JsonWriter name(String name) throws IOException {
        if (name == null) throw new NullPointerException("name == null");
        deferredName = name;
        return this;
    }

    public JsonWriter value(String value) throws IOException {
        if (value == null) return nullValue();
        writeDeferredName();
        beforeValue();
        writeString(value);
        return this;
    }

    public JsonWriter nullValue() throws IOException {
        if (deferredName != null) {
            writeDeferredName();
        }
        beforeValue();
        out.write("null");
        return this;
    }

    public JsonWriter value(boolean value) throws IOException {
        writeDeferredName();
        beforeValue();
        out.write(value ? "true" : "false");
        return this;
    }

    public JsonWriter value(double value) throws IOException {
        writeDeferredName();
        beforeValue();
        out.write(Double.toString(value));
        return this;
    }

    public JsonWriter value(long value) throws IOException {
        writeDeferredName();
        beforeValue();
        out.write(Long.toString(value));
        return this;
    }

    public JsonWriter value(Number value) throws IOException {
        if (value == null) return nullValue();
        writeDeferredName();
        beforeValue();
        out.write(value.toString());
        return this;
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    private void writeDeferredName() throws IOException {
        if (deferredName == null) return;
        Scope top = stack.get(stack.size() - 1);
        if (top == Scope.NONEMPTY_OBJECT) {
            out.write(',');
        }
        if (indent != null) newline();
        writeString(deferredName);
        out.write(indent != null ? ": " : ":");
        stack.set(stack.size() - 1, Scope.DANGLING_NAME);
        deferredName = null;
    }

    private void beforeValue() throws IOException {
        int idx = stack.size() - 1;
        Scope top = stack.get(idx);
        switch (top) {
            case EMPTY_DOCUMENT -> stack.set(idx, Scope.NONEMPTY_DOCUMENT);
            case EMPTY_ARRAY -> {
                stack.set(idx, Scope.NONEMPTY_ARRAY);
                if (indent != null) newline();
            }
            case NONEMPTY_ARRAY -> {
                out.write(',');
                if (indent != null) newline();
            }
            case DANGLING_NAME -> stack.set(idx, Scope.NONEMPTY_OBJECT);
            default -> {
            }
        }
    }

    private void newline() throws IOException {
        if (indent == null) return;
        out.write('\n');
        for (int i = 1; i < stack.size(); i++) {
            out.write(indent);
        }
    }

    private void writeString(String s) throws IOException {
        out.write('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.write("\\\"");
                case '\\' -> out.write("\\\\");
                case '\b' -> out.write("\\b");
                case '\f' -> out.write("\\f");
                case '\n' -> out.write("\\n");
                case '\r' -> out.write("\\r");
                case '\t' -> out.write("\\t");
                default -> {
                    if (c < 0x20) {
                        out.write(String.format("\\u%04x", (int) c));
                    } else {
                        out.write(c);
                    }
                }
            }
        }
        out.write('"');
    }
}
