package org.json;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class JSONObject {
    public static final Object NULL = new Object() {
        @Override
        public boolean equals(Object o) {
            return o == this || o == null;
        }

        @Override
        public String toString() {
            return "null";
        }
    };

    private final LinkedHashMap<String, Object> nameValuePairs = new LinkedHashMap<>();

    public JSONObject() {
    }

    public JSONObject(Map<?, ?> copyFrom) {
        if (copyFrom != null) {
            for (Map.Entry<?, ?> e : copyFrom.entrySet()) {
                if (e.getKey() != null) {
                    nameValuePairs.put(String.valueOf(e.getKey()), wrap(e.getValue()));
                }
            }
        }
    }

    public JSONObject(JSONTokener readFrom) throws JSONException {
        char c = readFrom.nextClean();
        if (c != '{') throw readFrom.syntaxError("A JSONObject text must begin with '{'");
        while (true) {
            c = readFrom.nextClean();
            switch (c) {
                case '\0' -> throw readFrom.syntaxError("A JSONObject text must end with '}'");
                case '}' -> {
                    return;
                }
                default -> {
                    readFrom.back();
                    String key = String.valueOf(readFrom.nextValue());
                    if (readFrom.nextClean() != ':') {
                        throw readFrom.syntaxError("Expected ':' after key " + key);
                    }
                    nameValuePairs.put(key, readFrom.nextValue());
                }
            }
            switch (readFrom.nextClean()) {
                case ';', ',' -> {
                    if (readFrom.nextClean() == '}') return;
                    readFrom.back();
                }
                case '}' -> {
                    return;
                }
                default -> throw readFrom.syntaxError("Expected a ',' or '}'");
            }
        }
    }

    public JSONObject(String json) throws JSONException {
        this(new JSONTokener(json));
    }

    public int length() {
        return nameValuePairs.size();
    }

    public JSONObject put(String name, boolean value) throws JSONException {
        nameValuePairs.put(checkName(name), value);
        return this;
    }

    public JSONObject put(String name, double value) throws JSONException {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new JSONException("Forbidden numeric value: " + value);
        }
        nameValuePairs.put(checkName(name), value);
        return this;
    }

    public JSONObject put(String name, int value) throws JSONException {
        nameValuePairs.put(checkName(name), value);
        return this;
    }

    public JSONObject put(String name, long value) throws JSONException {
        nameValuePairs.put(checkName(name), value);
        return this;
    }

    public JSONObject put(String name, Object value) throws JSONException {
        checkName(name);
        if (value == null) {
            nameValuePairs.remove(name);
            return this;
        }
        nameValuePairs.put(name, value);
        return this;
    }

    public JSONObject putOpt(String name, Object value) throws JSONException {
        if (name == null || value == null) return this;
        return put(name, value);
    }

    public Object remove(String name) {
        return nameValuePairs.remove(name);
    }

    public boolean isNull(String name) {
        Object v = nameValuePairs.get(name);
        return v == null || v == NULL;
    }

    public boolean has(String name) {
        return nameValuePairs.containsKey(name);
    }

    public Object get(String name) throws JSONException {
        Object result = nameValuePairs.get(name);
        if (result == null) throw new JSONException("No value for " + name);
        return result;
    }

    public Object opt(String name) {
        return name == null ? null : nameValuePairs.get(name);
    }

    public boolean getBoolean(String name) throws JSONException {
        Object o = get(name);
        Boolean b = toBoolean(o);
        if (b == null) throw typeMismatch(name, o, "boolean");
        return b;
    }

    public boolean optBoolean(String name) {
        return optBoolean(name, false);
    }

    public boolean optBoolean(String name, boolean fallback) {
        Boolean b = toBoolean(opt(name));
        return b != null ? b : fallback;
    }

    public double getDouble(String name) throws JSONException {
        Object o = get(name);
        Double d = toDouble(o);
        if (d == null) throw typeMismatch(name, o, "double");
        return d;
    }

    public double optDouble(String name) {
        return optDouble(name, Double.NaN);
    }

    public double optDouble(String name, double fallback) {
        Double d = toDouble(opt(name));
        return d != null ? d : fallback;
    }

    public int getInt(String name) throws JSONException {
        Object o = get(name);
        Integer i = toInteger(o);
        if (i == null) throw typeMismatch(name, o, "int");
        return i;
    }

    public int optInt(String name) {
        return optInt(name, 0);
    }

    public int optInt(String name, int fallback) {
        Integer i = toInteger(opt(name));
        return i != null ? i : fallback;
    }

    public long getLong(String name) throws JSONException {
        Object o = get(name);
        Long l = toLong(o);
        if (l == null) throw typeMismatch(name, o, "long");
        return l;
    }

    public long optLong(String name) {
        return optLong(name, 0L);
    }

    public long optLong(String name, long fallback) {
        Long l = toLong(opt(name));
        return l != null ? l : fallback;
    }

    public String getString(String name) throws JSONException {
        Object o = get(name);
        if (o == NULL) throw typeMismatch(name, o, "String");
        return String.valueOf(o);
    }

    public String optString(String name) {
        return optString(name, "");
    }

    public String optString(String name, String fallback) {
        Object o = opt(name);
        if (o == null || o == NULL) return fallback;
        return String.valueOf(o);
    }

    public JSONArray getJSONArray(String name) throws JSONException {
        Object o = get(name);
        if (o instanceof JSONArray a) return a;
        throw typeMismatch(name, o, "JSONArray");
    }

    public JSONArray optJSONArray(String name) {
        return opt(name) instanceof JSONArray a ? a : null;
    }

    public JSONObject getJSONObject(String name) throws JSONException {
        Object o = get(name);
        if (o instanceof JSONObject obj) return obj;
        throw typeMismatch(name, o, "JSONObject");
    }

    public JSONObject optJSONObject(String name) {
        return opt(name) instanceof JSONObject obj ? obj : null;
    }

    public Iterator<String> keys() {
        return nameValuePairs.keySet().iterator();
    }

    public JSONArray names() {
        return nameValuePairs.isEmpty() ? null : new JSONArray(nameValuePairs.keySet());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : nameValuePairs.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(quote(e.getKey())).append(':').append(valueToString(e.getValue()));
        }
        return sb.append('}').toString();
    }

    public String toString(int indentSpaces) {
        return toString();
    }

    public static String quote(String data) {
        if (data == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            switch (c) {
                case '"', '\\' -> sb.append('\\').append(c);
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }

    public static String numberToString(Number number) throws JSONException {
        if (number == null) throw new JSONException("Number must be non-null");
        double d = number.doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d)) throw new JSONException("Forbidden numeric value");
        if (number instanceof Long || number instanceof Integer || number instanceof Short || number instanceof Byte) {
            return Long.toString(number.longValue());
        }
        if (d == (long) d) return Long.toString((long) d);
        return Double.toString(d);
    }

    public static Object wrap(Object o) {
        if (o == null) return NULL;
        if (o instanceof JSONArray || o instanceof JSONObject || o.equals(NULL)
                || o instanceof Boolean || o instanceof Byte || o instanceof Character
                || o instanceof Double || o instanceof Float || o instanceof Integer
                || o instanceof Long || o instanceof Short || o instanceof String) {
            return o;
        }
        if (o instanceof Collection<?> c) return new JSONArray(c);
        if (o instanceof Map<?, ?> m) return new JSONObject(m);
        return o.toString();
    }

    static String valueToString(Object value) {
        if (value == null || value == NULL) return "null";
        if (value instanceof JSONObject || value instanceof JSONArray || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Number n) {
            try {
                return numberToString(n);
            } catch (JSONException e) {
                return "null";
            }
        }
        return quote(String.valueOf(value));
    }

    private static String checkName(String name) throws JSONException {
        if (name == null) throw new JSONException("Names must be non-null");
        return name;
    }

    private static JSONException typeMismatch(String indexOrName, Object actual, String requiredType) {
        return new JSONException("Value " + actual + " at " + indexOrName + " cannot be converted to " + requiredType);
    }

    static Boolean toBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) {
            if ("true".equalsIgnoreCase(s)) return Boolean.TRUE;
            if ("false".equalsIgnoreCase(s)) return Boolean.FALSE;
        }
        return null;
    }

    static Double toDouble(Object value) {
        if (value instanceof Double d) return d;
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try {
                return Double.valueOf(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    static Integer toInteger(Object value) {
        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try {
                return (int) Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    static Long toLong(Object value) {
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) {
            try {
                return (long) Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
