package android.content.res;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.StateSet;
import dev.aaow.apk.ResValue;
import dev.aaow.apk.XmlElement;
import dev.aaow.rt.AndroidSystemResources;
import dev.aaow.rt.AxmlXmlResourceParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Cài đặt android.content.res.ColorStateList hỗ trợ đầy đủ so khớp trạng thái View
 * và giải mã tài nguyên XML &lt;selector&gt; (kể cả tham chiếu ?attr/... theo Theme và alpha).
 */
public class ColorStateList implements Parcelable {
    private static final int[][] EMPTY = new int[][] { new int[0] };

    private final int[][] stateSpecs;
    private final int[] colors;
    private final int defaultColor;

    public ColorStateList(int[][] states, int[] colors) {
        if (states == null || colors == null || states.length == 0 || colors.length == 0) {
            this.stateSpecs = EMPTY;
            this.colors = new int[] { 0xFF000000 };
            this.defaultColor = 0xFF000000;
        } else {
            int n = Math.min(states.length, colors.length);
            this.stateSpecs = new int[n][];
            this.colors = new int[n];
            int def = colors[n - 1];
            for (int i = 0; i < n; i++) {
                this.stateSpecs[i] = states[i] != null ? states[i].clone() : new int[0];
                this.colors[i] = colors[i];
                if (this.stateSpecs[i].length == 0) {
                    def = colors[i];
                }
            }
            this.defaultColor = def;
        }
    }

    public static ColorStateList valueOf(int color) {
        return new ColorStateList(EMPTY, new int[] { color });
    }

    public static ColorStateList createFromXml(Resources r, XmlPullParser parser)
            throws XmlPullParserException, IOException {
        return createFromXml(r, parser, null);
    }

    public static ColorStateList createFromXml(Resources r, XmlPullParser parser, Resources.Theme theme)
            throws XmlPullParserException, IOException {
        if (parser instanceof AxmlXmlResourceParser axml && axml.getRootElement() != null) {
            return fromXmlElement(axml.getRootElement(), r, theme);
        }
        List<int[]> specs = new ArrayList<>();
        List<Integer> cols = new ArrayList<>();
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && "item".equals(parser.getName())) {
                int baseColor = 0xFF1C1B1F;
                float alphaMod = 1.0f;
                List<Integer> stateList = new ArrayList<>();
                int count = parser.getAttributeCount();
                for (int i = 0; i < count; i++) {
                    String name = parser.getAttributeName(i);
                    String val = parser.getAttributeValue(i);
                    if ("color".equals(name)) {
                        baseColor = parseColorString(val, r, theme, baseColor);
                    } else if ("alpha".equals(name)) {
                        try {
                            if (val != null) alphaMod = Float.parseFloat(val);
                        } catch (NumberFormatException ignored) {
                        }
                    } else if (name != null && name.startsWith("state_")) {
                        int attrId = AndroidSystemResources.attrId(name);
                        if (attrId != 0) {
                            boolean positive = "true".equalsIgnoreCase(val) || "1".equals(val);
                            stateList.add(positive ? attrId : -attrId);
                        }
                    }
                }
                specs.add(toIntArray(stateList));
                cols.add(modulateColorAlpha(baseColor, alphaMod));
            }
            event = parser.next();
        }
        if (cols.isEmpty()) {
            return valueOf(0xFF1C1B1F);
        }
        return new ColorStateList(specs.toArray(new int[0][]), toIntArray(cols));
    }

    public static ColorStateList fromXmlElement(XmlElement root, Resources res, Resources.Theme theme) {
        if (root == null) return valueOf(0xFF1C1B1F);
        List<int[]> specs = new ArrayList<>();
        List<Integer> cols = new ArrayList<>();

        for (XmlElement child : root.children) {
            if (!"item".equals(child.name)) continue;
            int baseColor = 0xFF1C1B1F;
            boolean hasColor = false;
            float alphaMod = 1.0f;
            List<Integer> stateList = new ArrayList<>();

            for (XmlElement.Attribute a : child.attributes) {
                if ("color".equals(a.name)) {
                    if (res != null) {
                        baseColor = res.resolveColor(a.value, theme, baseColor);
                        hasColor = true;
                    } else if (a.value.isColor()) {
                        baseColor = a.value.data();
                        hasColor = true;
                    }
                } else if ("alpha".equals(a.name)) {
                    alphaMod = resolveAlpha(a.value, res, theme, 1.0f);
                } else if (a.name != null && a.name.startsWith("state_")) {
                    int attrId = a.resourceId != 0 ? a.resourceId : AndroidSystemResources.attrId(a.name);
                    if (attrId != 0) {
                        boolean positive = a.value != null && a.value.data() != 0;
                        stateList.add(positive ? attrId : -attrId);
                    }
                }
            }
            if (hasColor) {
                specs.add(toIntArray(stateList));
                cols.add(modulateColorAlpha(baseColor, alphaMod));
            }
        }
        if (cols.isEmpty()) {
            return valueOf(0xFF1C1B1F);
        }
        return new ColorStateList(specs.toArray(new int[0][]), toIntArray(cols));
    }

    private static float resolveAlpha(ResValue raw, Resources res, Resources.Theme theme, float def) {
        if (raw == null) return def;
        ResValue v = raw;
        if (res != null) {
            if (v.type() == ResValue.TYPE_ATTRIBUTE && theme != null) {
                ResValue themed = theme.resolveAttributeValue(v.data());
                if (themed != null) v = themed;
            }
            v = res.getTable().resolve(v);
        }
        if (v == null) return def;
        if (v.type() == ResValue.TYPE_FLOAT) return v.getFloat();
        if (v.isInt()) return v.data();
        if (v.isString() && v.string() != null) {
            try {
                return Float.parseFloat(v.string());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    private static int parseColorString(String val, Resources res, Resources.Theme theme, int def) {
        if (val == null || val.isEmpty()) return def;
        if (val.startsWith("#")) {
            try {
                return Color.parseColor(val);
            } catch (Exception ignored) {
            }
        }
        if (val.startsWith("@0x") && res != null) {
            try {
                int id = (int) Long.parseLong(val.substring(3), 16);
                return res.getColor(id, theme);
            } catch (Exception ignored) {
            }
        }
        if (val.startsWith("?0x") && res != null && theme != null) {
            try {
                int id = (int) Long.parseLong(val.substring(3), 16);
                ResValue rv = theme.resolveAttributeValue(id);
                if (rv != null) return res.resolveColor(rv, theme, def);
            } catch (Exception ignored) {
            }
        }
        return def;
    }

    private static int modulateColorAlpha(int baseColor, float alphaMod) {
        if (alphaMod >= 0.999f) return baseColor;
        int origAlpha = Color.alpha(baseColor);
        int newAlpha = Math.max(0, Math.min(255, Math.round(origAlpha * Math.max(0f, alphaMod))));
        return (baseColor & 0x00FFFFFF) | (newAlpha << 24);
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] out = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    public ColorStateList withAlpha(int alpha) {
        int a = (alpha & 0xFF) << 24;
        int[] next = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            next[i] = (colors[i] & 0x00FFFFFF) | a;
        }
        return new ColorStateList(stateSpecs, next);
    }

    public int getDefaultColor() {
        return defaultColor;
    }

    public int getColorForState(int[] stateSet, int defaultColor) {
        for (int i = 0; i < stateSpecs.length; i++) {
            if (StateSet.stateSetMatches(stateSpecs[i], stateSet)) {
                return colors[i];
            }
        }
        return defaultColor;
    }

    public boolean isStateful() {
        return stateSpecs.length > 1 || (stateSpecs.length == 1 && stateSpecs[0].length > 0);
    }

    public boolean isOpaque() {
        for (int c : colors) {
            if ((c >>> 24) != 0xFF) return false;
        }
        return true;
    }

    public int getChangingConfigurations() {
        return 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(stateSpecs.length);
        for (int[] spec : stateSpecs) {
            dest.writeIntArray(spec);
        }
        dest.writeIntArray(colors);
    }

    public static final Creator<ColorStateList> CREATOR = new Creator<>() {
        @Override
        public ColorStateList createFromParcel(Parcel source) {
            int n = source.readInt();
            int[][] specs = new int[Math.max(0, n)][];
            for (int i = 0; i < specs.length; i++) {
                specs[i] = source.createIntArray();
            }
            int[] cols = source.createIntArray();
            return new ColorStateList(specs, cols);
        }

        @Override
        public ColorStateList[] newArray(int size) {
            return new ColorStateList[size];
        }
    };
}
