package android.content.res;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import dev.aaow.apk.ResValue;
import dev.aaow.apk.ResourceTable;
import dev.aaow.apk.XmlElement;
import dev.aaow.rt.AndroidSystemResources;

import java.util.ArrayList;
import java.util.List;

public class TypedArray implements AutoCloseable {
    private final Resources resources;
    private final int[] attrs;
    private final ResValue[] values;
    private final int[] rawRefs;
    private final int[] indices;
    private final String positionDescription;

    public TypedArray(Resources resources, int[] attrs) {
        this.resources = resources;
        this.attrs = attrs != null ? attrs : new int[0];
        this.values = new ResValue[this.attrs.length];
        this.rawRefs = new int[this.attrs.length];
        this.indices = new int[0];
        this.positionDescription = "";
    }

    private TypedArray(Resources resources, int[] attrs, ResValue[] values, int[] rawRefs,
                       int[] indices, String positionDescription) {
        this.resources = resources;
        this.attrs = attrs;
        this.values = values;
        this.rawRefs = rawRefs;
        this.indices = indices;
        this.positionDescription = positionDescription != null ? positionDescription : "";
    }

    static TypedArray obtain(Resources res, int themeResId, AttributeSet set, int[] attrs,
                             int defStyleAttr, int defStyleRes) {
        List<Integer> layers = themeResId != 0 ? List.of(themeResId) : List.of();
        return obtain(res, layers, set, attrs, defStyleAttr, defStyleRes);
    }

    static TypedArray obtain(Resources res, List<Integer> themeLayers, AttributeSet set, int[] attrs,
                             int defStyleAttr, int defStyleRes) {
        if (attrs == null) attrs = new int[0];
        if (themeLayers == null) themeLayers = List.of();
        ResValue[] resolved = new ResValue[attrs.length];
        int[] refs = new int[attrs.length];
        List<Integer> idxList = new ArrayList<>();

        int styleFromDefAttr = 0;
        if (defStyleAttr != 0) {
            ResValue styleRef = resolveAttrAcrossThemes(res, themeLayers, defStyleAttr);
            if (styleRef != null && styleRef.isReference()) {
                styleFromDefAttr = styleRef.data();
            }
        }
        int xmlStyleAttr = set != null ? set.getStyleAttribute() : 0;
        AttributeSet.XmlAttributeSet xas = set instanceof AttributeSet.XmlAttributeSet x ? x : null;

        for (int i = 0; i < attrs.length; i++) {
            int attrId = attrs[i];
            ResValue raw = null;
            if (xas != null) {
                raw = findInXmlElement(res, xas.element, attrId);
            } else if (set != null) {
                raw = findInGenericAttributeSet(set, attrId);
            }
            if (raw == null && xmlStyleAttr != 0) {
                raw = res.resolveStyleAttr(xmlStyleAttr, attrId);
            }
            if (raw == null && styleFromDefAttr != 0) {
                raw = res.resolveStyleAttr(styleFromDefAttr, attrId);
            }
            if (raw == null && defStyleRes != 0) {
                raw = res.resolveStyleAttr(defStyleRes, attrId);
            }
            if (raw == null) {
                raw = resolveAttrAcrossThemes(res, themeLayers, attrId);
            }

            if (raw != null) {
                if (raw.isReference()) {
                    refs[i] = raw.data();
                }
                ResValue val = resolveAttrValue(res, themeLayers, raw);
                if (val != null && val.type() != ResValue.TYPE_NULL) {
                    resolved[i] = val;
                    idxList.add(i);
                }
            }
        }

        int[] idxArr = new int[idxList.size()];
        for (int i = 0; i < idxList.size(); i++) {
            idxArr[i] = idxList.get(i);
        }
        String posDesc = set != null ? set.getPositionDescription() : "";
        return new TypedArray(res, attrs, resolved, refs, idxArr, posDesc);
    }

    private static ResValue resolveAttrAcrossThemes(Resources res, List<Integer> themeLayers, int attrId) {
        if (res == null || attrId == 0) return null;
        for (int styleId : themeLayers) {
            ResValue rv = res.resolveStyleAttr(styleId, attrId);
            if (rv != null) return rv;
        }
        return null;
    }

    private static ResValue findInXmlElement(Resources res, XmlElement el, int attrId) {
        if (el == null) return null;
        String sysName = AndroidSystemResources.attrName(attrId);
        String appName = null;
        ResourceTable.Entry entry = res != null ? res.getTable().getEntry(attrId) : null;
        if (entry != null) appName = entry.key;

        for (XmlElement.Attribute a : el.attributes) {
            if (a.resourceId == attrId && attrId != 0) {
                return a.value;
            }
            if (sysName != null && sysName.equals(a.name) && XmlElement.ANDROID_NS.equals(a.namespace)) {
                return a.value;
            }
            if (appName != null && appName.equals(a.name) && !XmlElement.ANDROID_NS.equals(a.namespace)) {
                return a.value;
            }
        }
        return null;
    }

    private static ResValue findInGenericAttributeSet(AttributeSet set, int attrId) {
        int count = set.getAttributeCount();
        String sysName = AndroidSystemResources.attrName(attrId);
        for (int j = 0; j < count; j++) {
            int resId = set.getAttributeNameResource(j);
            String name = set.getAttributeName(j);
            if ((attrId != 0 && resId == attrId) || (sysName != null && sysName.equals(name))) {
                String v = set.getAttributeValue(j);
                if (v != null) {
                    return new ResValue(ResValue.TYPE_STRING, 0, v);
                }
            }
        }
        return null;
    }

    private static ResValue resolveAttrValue(Resources res, List<Integer> themeLayers, ResValue raw) {
        if (raw == null || res == null) return raw;
        if (raw.type() == ResValue.TYPE_ATTRIBUTE) {
            ResValue themed = resolveAttrAcrossThemes(res, themeLayers, raw.data());
            if (themed != null) return themed;
        }
        return res.getTable().resolve(raw);
    }

    private ResValue valueAt(int index) {
        return index >= 0 && index < values.length ? values[index] : null;
    }

    public int length() {
        return attrs.length;
    }

    public int getIndexCount() {
        return indices.length;
    }

    public int getIndex(int at) {
        return at >= 0 && at < indices.length ? indices[at] : 0;
    }

    public Resources getResources() {
        return resources;
    }

    public int getChangingConfigurations() {
        return 0;
    }

    public String getPositionDescription() {
        return positionDescription;
    }

    public int getType(int index) {
        ResValue v = valueAt(index);
        return v != null ? v.type() : TypedValue.TYPE_NULL;
    }

    public boolean hasValue(int index) {
        return valueAt(index) != null;
    }

    public boolean hasValueOrEmpty(int index) {
        return hasValue(index);
    }

    public TypedValue peekValue(int index) {
        TypedValue tv = new TypedValue();
        return getValue(index, tv) ? tv : null;
    }

    public boolean getValue(int index, TypedValue outValue) {
        ResValue v = valueAt(index);
        if (v == null) return false;
        if (outValue != null) {
            outValue.type = v.type();
            outValue.data = v.data();
            outValue.string = v.string();
            int ref = index >= 0 && index < rawRefs.length ? rawRefs[index] : 0;
            outValue.resourceId = ref != 0 ? ref : (v.isReference() ? v.data() : 0);
        }
        return true;
    }

    public CharSequence getText(int index) {
        return getString(index);
    }

    public String getString(int index) {
        ResValue v = valueAt(index);
        if (v == null) return null;
        return resources != null ? resources.resolveString(v) : v.coerceToString();
    }

    public String getNonResourceString(int index) {
        ResValue v = valueAt(index);
        if (v != null && v.isString() && (index >= rawRefs.length || rawRefs[index] == 0)) {
            return v.string();
        }
        return null;
    }

    public boolean getBoolean(int index, boolean defValue) {
        ResValue v = valueAt(index);
        if (v == null) return defValue;
        if (v.isInt()) return v.data() != 0;
        if (v.isString() && v.string() != null) {
            if ("true".equalsIgnoreCase(v.string())) return true;
            if ("false".equalsIgnoreCase(v.string())) return false;
        }
        return defValue;
    }

    public int getInt(int index, int defValue) {
        ResValue v = valueAt(index);
        if (v == null) return defValue;
        if (v.isInt()) return v.data();
        if (v.isString() && v.string() != null) {
            try {
                return Integer.decode(v.string());
            } catch (NumberFormatException ignored) {
            }
        }
        return defValue;
    }

    public float getFloat(int index, float defValue) {
        ResValue v = valueAt(index);
        if (v == null) return defValue;
        if (v.type() == ResValue.TYPE_FLOAT) return v.getFloat();
        if (v.isInt()) return v.data();
        if (v.isString() && v.string() != null) {
            try {
                return Float.parseFloat(v.string());
            } catch (NumberFormatException ignored) {
            }
        }
        return defValue;
    }

    public int getColor(int index, int defValue) {
        ResValue v = valueAt(index);
        if (v == null) return defValue;
        if (resources != null) return resources.resolveColor(v, defValue);
        if (v.isColor() || v.isInt()) return v.data();
        if (v.isString() && v.string() != null && v.string().startsWith("#")) {
            try {
                return Color.parseColor(v.string());
            } catch (Exception ignored) {
            }
        }
        return defValue;
    }

    public ColorStateList getColorStateList(int index) {
        ResValue v = valueAt(index);
        if (v == null) return null;
        int c = getColor(index, 0);
        return ColorStateList.valueOf(c);
    }

    public int getInteger(int index, int defValue) {
        return getInt(index, defValue);
    }

    public float getDimension(int index, float defValue) {
        ResValue v = valueAt(index);
        if (v == null) return defValue;
        return resources != null ? resources.resolveDimension(v, defValue) : defValue;
    }

    public int getDimensionPixelOffset(int index, int defValue) {
        ResValue v = valueAt(index);
        if (v == null) return defValue;
        return Math.round(getDimension(index, defValue));
    }

    public int getDimensionPixelSize(int index, int defValue) {
        ResValue v = valueAt(index);
        if (v == null) return defValue;
        float f = getDimension(index, defValue);
        int res = Math.round(f);
        if (res != 0) return res;
        return f == 0f ? 0 : (f > 0 ? 1 : -1);
    }

    public int getLayoutDimension(int index, int defValue) {
        ResValue v = valueAt(index);
        if (v == null) return defValue;
        if (v.isDimension()) return getDimensionPixelSize(index, defValue);
        if (v.isInt()) return v.data();
        return defValue;
    }

    public int getLayoutDimension(int index, String name) {
        return getLayoutDimension(index, -2);
    }

    public float getFraction(int index, int base, int pbase, float defValue) {
        ResValue v = valueAt(index);
        if (v == null) return defValue;
        if (v.type() == ResValue.TYPE_FRACTION) {
            float val = v.complexValue();
            return (v.complexUnit() == 1 ? pbase : base) * val;
        }
        return getFloat(index, defValue);
    }

    public int getResourceId(int index, int defValue) {
        if (index >= 0 && index < rawRefs.length && rawRefs[index] != 0) {
            return rawRefs[index];
        }
        ResValue v = valueAt(index);
        if (v != null && v.isReference()) return v.data();
        return defValue;
    }

    public Drawable getDrawable(int index) {
        int resId = getResourceId(index, 0);
        if (resId != 0 && resources != null) {
            return resources.getDrawable(resId);
        }
        ResValue v = valueAt(index);
        if (v != null && v.isColor()) {
            return new android.graphics.drawable.ColorDrawable(v.data());
        }
        return null;
    }

    public CharSequence[] getTextArray(int index) {
        int resId = getResourceId(index, 0);
        if (resId != 0 && resources != null) {
            return resources.getStringArray(resId);
        }
        return null;
    }

    public void recycle() {
    }

    @Override
    public void close() {
        recycle();
    }
}
