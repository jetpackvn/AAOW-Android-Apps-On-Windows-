package dev.aaow.apk;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Đọc resources.arsc: bảng ánh xạ ID resource (0xPPTTEEEE) sang giá trị.
 * Với mỗi ID, chọn cấu hình (ngôn ngữ, mật độ điểm ảnh, phiên bản SDK...) phù hợp nhất với máy.
 */
public final class ResourceTable {
    private static final int RES_STRING_POOL_TYPE = 0x0001;
    private static final int RES_TABLE_TYPE = 0x0002;
    private static final int RES_TABLE_PACKAGE_TYPE = 0x0200;
    private static final int RES_TABLE_TYPE_TYPE = 0x0201;

    private static final int FLAG_COMPLEX = 0x0001;
    private static final int FLAG_COMPACT = 0x0008;
    private static final int TYPE_FLAG_SPARSE = 0x01;
    private static final int TYPE_FLAG_OFFSET16 = 0x02;

    /** Độ phân giải mục tiêu khi chọn drawable (xxhdpi cho ảnh sắc nét rồi thu nhỏ). */
    public static int preferredDensity = 480;
    public static int maxSdk = 35;

    /** Một mục resource đã được chọn cấu hình. */
    public static final class Entry {
        public final int id;
        public final String type;
        public final String key;
        public final Config config;
        /** Giá trị đơn (null nếu là bag). */
        public final ResValue value;
        /** Bag (style, array, plurals...): parent + danh sách (attrId -> value). */
        public final int parent;
        public final Map<Integer, ResValue> bag;

        Entry(int id, String type, String key, Config config, ResValue value, int parent, Map<Integer, ResValue> bag) {
            this.id = id;
            this.type = type;
            this.key = key;
            this.config = config;
            this.value = value;
            this.parent = parent;
            this.bag = bag;
        }

        public boolean isBag() {
            return bag != null;
        }
    }

    /** Một phần của ResTable_config đủ để chọn cấu hình. */
    public record Config(int mcc, String language, String country, int orientation, int density,
                         int sdkVersion, int uiMode, int smallestWidthDp, int screenWidthDp, int screenHeightDp) {
        public boolean isDefault() {
            return mcc == 0 && language.isEmpty() && orientation == 0 && density == 0 && sdkVersion == 0
                    && uiMode == 0 && smallestWidthDp == 0 && screenWidthDp == 0 && screenHeightDp == 0;
        }
    }

    private final Map<Integer, Entry> entries = new HashMap<>();
    private final Map<Integer, Integer> scores = new HashMap<>();
    private final Map<String, Integer> nameToId = new HashMap<>();
    private final Map<String, Integer> attrKeyCounts = new HashMap<>();
    private String packageName = "";
    private int packageId;
    private final String systemLanguage = Locale.getDefault().getLanguage();

    private ResourceTable() {
    }

    public static ResourceTable empty() {
        return new ResourceTable();
    }

    public static ResourceTable parse(byte[] data) {
        ResourceTable t = new ResourceTable();
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        if (Short.toUnsignedInt(buf.getShort(0)) != RES_TABLE_TYPE) {
            throw new IllegalArgumentException("resources.arsc không hợp lệ");
        }
        int headerSize = Short.toUnsignedInt(buf.getShort(2));
        int size = Math.min(buf.getInt(4), data.length);
        StringPool values = null;
        int p = headerSize;
        while (p + 8 <= size) {
            int type = Short.toUnsignedInt(buf.getShort(p));
            int chunkSize = buf.getInt(p + 4);
            if (chunkSize <= 0) break;
            if (type == RES_STRING_POOL_TYPE) {
                values = StringPool.read(buf, p);
            } else if (type == RES_TABLE_PACKAGE_TYPE) {
                t.readPackage(buf, p, values);
            }
            p += chunkSize;
        }
        return t;
    }

    private void readPackage(ByteBuffer buf, int p, StringPool values) {
        int headerSize = Short.toUnsignedInt(buf.getShort(p + 2));
        int size = buf.getInt(p + 4);
        int id = buf.getInt(p + 8);
        char[] nameChars = new char[128];
        int n = 0;
        for (int i = 0; i < 128; i++) {
            char c = buf.getChar(p + 12 + i * 2);
            if (c == 0) break;
            nameChars[n++] = c;
        }
        StringPool typeStrings = StringPool.read(buf, p + buf.getInt(p + 268));
        StringPool keyStrings = StringPool.read(buf, p + buf.getInt(p + 276));
        if (packageName.isEmpty() || id == 0x7f) {
            packageName = new String(nameChars, 0, n);
            packageId = id;
        }

        int q = p + headerSize;
        while (q + 8 <= p + size) {
            int type = Short.toUnsignedInt(buf.getShort(q));
            int chunkSize = buf.getInt(q + 4);
            if (chunkSize <= 0) break;
            if (type == RES_TABLE_TYPE_TYPE) {
                readType(buf, q, id, values, typeStrings, keyStrings);
            }
            q += chunkSize;
        }
    }

    private void readType(ByteBuffer buf, int p, int pkgId, StringPool values, StringPool typeStrings, StringPool keyStrings) {
        int headerSize = Short.toUnsignedInt(buf.getShort(p + 2));
        int typeId = buf.get(p + 8) & 0xff;
        int flags = buf.get(p + 9) & 0xff;
        int entryCount = buf.getInt(p + 12);
        int entriesStart = buf.getInt(p + 16);
        Config config = readConfig(buf, p + 20);
        int score = score(config);
        if (score < 0) return; // Cấu hình không dùng được trên máy này.

        String typeName = typeStrings.get(typeId - 1);
        int offsets = p + headerSize;

        for (int i = 0; i < entryCount; i++) {
            int index;
            int offset;
            if ((flags & TYPE_FLAG_SPARSE) != 0) {
                index = Short.toUnsignedInt(buf.getShort(offsets + i * 4));
                offset = Short.toUnsignedInt(buf.getShort(offsets + i * 4 + 2)) * 4;
            } else if ((flags & TYPE_FLAG_OFFSET16) != 0) {
                index = i;
                int o = Short.toUnsignedInt(buf.getShort(offsets + i * 2));
                if (o == 0xffff) continue;
                offset = o * 4;
            } else {
                index = i;
                offset = buf.getInt(offsets + i * 4);
                if (offset == -1) continue;
            }
            int resId = (pkgId << 24) | (typeId << 16) | index;
            Integer old = scores.get(resId);
            if (old != null && old >= score) continue;

            int e = p + entriesStart + offset;
            int eSize = Short.toUnsignedInt(buf.getShort(e));
            int eFlags = Short.toUnsignedInt(buf.getShort(e + 2));
            String key;
            ResValue value = null;
            Map<Integer, ResValue> bag = null;
            int parent = 0;
            if ((eFlags & FLAG_COMPACT) != 0) {
                key = keyStrings.get(eSize);
                int vType = eFlags >>> 8;
                int vData = buf.getInt(e + 4);
                value = new ResValue(vType, vData, vType == ResValue.TYPE_STRING ? values.get(vData) : null);
            } else {
                key = keyStrings.get(buf.getInt(e + 4));
                if ((eFlags & FLAG_COMPLEX) != 0) {
                    parent = buf.getInt(e + 8);
                    int count = buf.getInt(e + 12);
                    bag = new LinkedHashMap<>();
                    int m = e + eSize;
                    for (int k = 0; k < count; k++, m += 12) {
                        int name = buf.getInt(m);
                        int vType = buf.get(m + 7) & 0xff;
                        int vData = buf.getInt(m + 8);
                        bag.put(name, new ResValue(vType, vData, vType == ResValue.TYPE_STRING ? values.get(vData) : null));
                    }
                } else {
                    int v = e + eSize;
                    int vType = buf.get(v + 3) & 0xff;
                    int vData = buf.getInt(v + 4);
                    value = new ResValue(vType, vData, vType == ResValue.TYPE_STRING ? values.get(vData) : null);
                }
            }
            boolean isNewEntry = !entries.containsKey(resId);
            entries.put(resId, new Entry(resId, typeName, key, config, value, parent, bag));
            scores.put(resId, score);
            nameToId.put(typeName + "/" + key, resId);
            if (isNewEntry && "attr".equals(typeName) && key != null && !key.isEmpty()) {
                attrKeyCounts.merge(key, 1, Integer::sum);
            }
        }
    }

    private static Config readConfig(ByteBuffer buf, int c) {
        int size = buf.getInt(c);
        int mcc = size >= 8 ? Short.toUnsignedInt(buf.getShort(c + 4)) : 0;
        String lang = size >= 12 ? locale(buf, c + 8) : "";
        String country = size >= 12 ? locale(buf, c + 10) : "";
        int orientation = size >= 16 ? buf.get(c + 12) & 0xff : 0;
        int density = size >= 16 ? Short.toUnsignedInt(buf.getShort(c + 14)) : 0;
        int sdk = size >= 28 ? Short.toUnsignedInt(buf.getShort(c + 24)) : 0;
        int uiMode = size >= 32 ? buf.get(c + 29) & 0xff : 0;
        int sw = size >= 32 ? Short.toUnsignedInt(buf.getShort(c + 30)) : 0;
        int w = size >= 36 ? Short.toUnsignedInt(buf.getShort(c + 32)) : 0;
        int h = size >= 36 ? Short.toUnsignedInt(buf.getShort(c + 34)) : 0;
        return new Config(mcc, lang, country, orientation, density, sdk, uiMode, sw, w, h);
    }

    private static String locale(ByteBuffer buf, int p) {
        byte a = buf.get(p), b = buf.get(p + 1);
        if (a == 0) return "";
        if ((a & 0x80) != 0) return ""; // Mã ngôn ngữ 3 ký tự dạng nén: bỏ qua cho đơn giản.
        return new String(new byte[]{a, b}, StandardCharsets.US_ASCII);
    }

    /** Điểm phù hợp của một cấu hình; âm nghĩa là không dùng được. */
    private int score(Config c) {
        if (c.mcc() != 0) return -1;
        if (!c.language().isEmpty() && !c.language().equals(systemLanguage)) return -1;
        if (c.orientation() == 2) return -1; // landscape
        if ((c.uiMode() & 0x30) == 0x20) return -1; // night
        if ((c.uiMode() & 0x0f) > 1) return -1; // car, tv, watch...
        if (c.sdkVersion() > maxSdk) return -1;
        if (c.smallestWidthDp() > 400 || c.screenWidthDp() > 400) return -1;

        int s = 1;
        if (!c.language().isEmpty()) s += 100_000;
        s += c.sdkVersion() * 100;
        int d = c.density();
        if (d != 0 && d < 0xfffe) {
            // Ưu tiên mật độ gần preferredDensity, thà lớn hơn còn hơn nhỏ hơn.
            s += d >= preferredDensity ? 90 - (d - preferredDensity) / 20 : 80 - (preferredDensity - d) / 10;
        } else if (d == 0xfffe) {
            s += 1; // anydpi (thường là vector/adaptive icon)
        }
        return s;
    }

    // ---------------------------------------------------------------- truy vấn

    public String getPackageName() {
        return packageName;
    }

    public int getPackageId() {
        return packageId;
    }

    public Entry getEntry(int id) {
        return entries.get(id);
    }

    /** Giá trị của resource, tự đi theo chuỗi tham chiếu @x -> @y -> giá trị. */
    public ResValue getValue(int id) {
        ResValue v = null;
        for (int depth = 0; depth < 16; depth++) {
            Entry e = entries.get(id);
            if (e == null || e.value == null) return v;
            v = e.value;
            if (!v.isReference() || v.data() == 0) return v;
            id = v.data();
        }
        return v;
    }

    /** Nếu giá trị là tham chiếu thì giải quyết nó. */
    public ResValue resolve(ResValue v) {
        if (v != null && v.isReference() && v.data() != 0) {
            ResValue r = getValue(v.data());
            return r != null ? r : v;
        }
        return v;
    }

    public String getString(int id) {
        ResValue v = getValue(id);
        return v == null ? null : v.coerceToString();
    }

    public int getIdentifier(String type, String name) {
        Integer id = nameToId.get(type + "/" + name);
        return id == null ? 0 : id;
    }

    public String getResourceName(int id) {
        Entry e = entries.get(id);
        return e == null ? null : packageName + ":" + e.type + "/" + e.key;
    }

    public int size() {
        return entries.size();
    }

    /** Trả true nếu tên khóa attr là duy nhất (không bị làm rối kiểu AndResGuard/AAPT2 trùng tên 'arg'). */
    public boolean isUniqueAttrKey(String key) {
        return key != null && !key.isEmpty() && attrKeyCounts.getOrDefault(key, 0) == 1;
    }
}
