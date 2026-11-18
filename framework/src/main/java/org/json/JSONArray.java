package org.json;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JSONArray {
    private final List<Object> values = new ArrayList<>();

    public JSONArray() {
    }

    public JSONArray(Collection<?> copyFrom) {
        if (copyFrom != null) {
            for (Object o : copyFrom) {
                values.add(JSONObject.wrap(o));
            }
        }
    }

    public JSONArray(JSONTokener readFrom) throws JSONException {
        char c = readFrom.nextClean();
        if (c != '[') throw readFrom.syntaxError("A JSONArray text must start with '['");
        if (readFrom.nextClean() == ']') return;
        readFrom.back();
        while (true) {
            if (readFrom.nextClean() == ',') {
                readFrom.back();
                values.add(JSONObject.NULL);
            } else {
                readFrom.back();
                values.add(readFrom.nextValue());
            }
            switch (readFrom.nextClean()) {
                case ';', ',' -> {
                    if (readFrom.nextClean() == ']') return;
                    readFrom.back();
                }
                case ']' -> {
                    return;
                }
                default -> throw readFrom.syntaxError("Expected a ',' or ']'");
            }
        }
    }

    public JSONArray(String json) throws JSONException {
        this(new JSONTokener(json));
    }

    public JSONArray(Object array) throws JSONException {
        if (array == null || !array.getClass().isArray()) {
            throw new JSONException("Not a primitive array");
        }
        int len = Array.getLength(array);
        for (int i = 0; i < len; i++) {
            values.add(JSONObject.wrap(Array.get(array, i)));
        }
    }

    public int length() {
        return values.size();
    }

    public JSONArray put(boolean value) {
        values.add(value);
        return this;
    }

    public JSONArray put(double value) throws JSONException {
        if (Double.isNaN(value) || Double.isInfinite(value)) throw new JSONException("Forbidden numeric value");
        values.add(value);
        return this;
    }

    public JSONArray put(int value) {
        values.add(value);
        return this;
    }

    public JSONArray put(long value) {
        values.add(value);
        return this;
    }

    public JSONArray put(Object value) {
        values.add(value != null ? value : JSONObject.NULL);
        return this;
    }

    public JSONArray put(int index, Object value) throws JSONException {
        if (index < 0) throw new JSONException("JSONArray[" + index + "] not found.");
        while (values.size() <= index) values.add(JSONObject.NULL);
        values.set(index, value != null ? value : JSONObject.NULL);
        return this;
    }

    public boolean isNull(int index) {
        Object o = opt(index);
        return o == null || o == JSONObject.NULL;
    }

    public Object get(int index) throws JSONException {
        if (index < 0 || index >= values.size()) {
            throw new JSONException("Index " + index + " out of range [0.." + values.size() + ")");
        }
        return values.get(index);
    }

    public Object opt(int index) {
        if (index < 0 || index >= values.size()) return null;
        return values.get(index);
    }

    public boolean getBoolean(int index) throws JSONException {
        Object o = get(index);
        Boolean b = JSONObject.toBoolean(o);
        if (b == null) throw new JSONException("Value at " + index + " is not a boolean");
        return b;
    }

    public boolean optBoolean(int index) {
        return optBoolean(index, false);
    }

    public boolean optBoolean(int index, boolean fallback) {
        Boolean b = JSONObject.toBoolean(opt(index));
        return b != null ? b : fallback;
    }

    public double getDouble(int index) throws JSONException {
        Object o = get(index);
        Double d = JSONObject.toDouble(o);
        if (d == null) throw new JSONException("Value at " + index + " is not a double");
        return d;
    }

    public double optDouble(int index) {
        return optDouble(index, Double.NaN);
    }

    public double optDouble(int index, double fallback) {
        Double d = JSONObject.toDouble(opt(index));
        return d != null ? d : fallback;
    }

    public int getInt(int index) throws JSONException {
        Object o = get(index);
        Integer i = JSONObject.toInteger(o);
        if (i == null) throw new JSONException("Value at " + index + " is not an int");
        return i;
    }

    public int optInt(int index) {
        return optInt(index, 0);
    }

    public int optInt(int index, int fallback) {
        Integer i = JSONObject.toInteger(opt(index));
        return i != null ? i : fallback;
    }

    public long getLong(int index) throws JSONException {
        Object o = get(index);
        Long l = JSONObject.toLong(o);
        if (l == null) throw new JSONException("Value at " + index + " is not a long");
        return l;
    }

    public long optLong(int index) {
        return optLong(index, 0L);
    }

    public long optLong(int index, long fallback) {
        Long l = JSONObject.toLong(opt(index));
        return l != null ? l : fallback;
    }

    public String getString(int index) throws JSONException {
        Object o = get(index);
        if (o == JSONObject.NULL) throw new JSONException("Value at " + index + " is null");
        return String.valueOf(o);
    }

    public String optString(int index) {
        return optString(index, "");
    }

    public String optString(int index, String fallback) {
        Object o = opt(index);
        if (o == null || o == JSONObject.NULL) return fallback;
        return String.valueOf(o);
    }

    public JSONArray getJSONArray(int index) throws JSONException {
        Object o = get(index);
        if (o instanceof JSONArray a) return a;
        throw new JSONException("Value at " + index + " is not a JSONArray");
    }

    public JSONArray optJSONArray(int index) {
        return opt(index) instanceof JSONArray a ? a : null;
    }

    public JSONObject getJSONObject(int index) throws JSONException {
        Object o = get(index);
        if (o instanceof JSONObject obj) return obj;
        throw new JSONException("Value at " + index + " is not a JSONObject");
    }

    public JSONObject optJSONObject(int index) {
        return opt(index) instanceof JSONObject obj ? obj : null;
    }

    public Object remove(int index) {
        if (index < 0 || index >= values.size()) return null;
        return values.remove(index);
    }

    public String join(String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(separator);
            sb.append(JSONObject.valueToString(values.get(i)));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "[" + join(",") + "]";
    }

    public String toString(int indentSpaces) {
        return toString();
    }
}
