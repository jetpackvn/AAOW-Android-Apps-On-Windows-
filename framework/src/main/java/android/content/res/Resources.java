package android.content.res;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import dev.aaow.apk.ApkFile;
import dev.aaow.apk.AxmlParser;
import dev.aaow.apk.ResValue;
import dev.aaow.apk.ResourceTable;
import dev.aaow.apk.XmlElement;
import dev.aaow.rt.AndroidSystemResources;
import dev.aaow.rt.AxmlXmlResourceParser;
import dev.aaow.rt.XmlDrawableLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Cài đặt android.content.res.Resources đọc trực tiếp từ ApkFile và ResourceTable.
 */
public class Resources {
    public static class NotFoundException extends RuntimeException {
        public NotFoundException() {
        }

        public NotFoundException(String name) {
            super(name);
        }
    }

    public final class Theme {
        private final List<Integer> styleLayers = new ArrayList<>();

        public void applyStyle(int resId, boolean force) {
            if (resId == 0) return;
            styleLayers.remove(Integer.valueOf(resId));
            if (force) {
                styleLayers.add(0, resId);
            } else {
                styleLayers.add(resId);
            }
        }

        public void setTo(Theme other) {
            styleLayers.clear();
            if (other != null) {
                styleLayers.addAll(other.styleLayers);
            }
        }

        public int getThemeResId() {
            return styleLayers.isEmpty() ? 0 : styleLayers.get(0);
        }

        public List<Integer> getStyleLayers() {
            return Collections.unmodifiableList(styleLayers);
        }

        public void rebase() {
        }

        public TypedArray obtainStyledAttributes(int[] attrs) {
            return TypedArray.obtain(Resources.this, styleLayers, null, attrs, 0, 0);
        }

        public TypedArray obtainStyledAttributes(int resid, int[] attrs) {
            return TypedArray.obtain(Resources.this, styleLayers, null, attrs, 0, resid);
        }

        public TypedArray obtainStyledAttributes(AttributeSet set, int[] attrs, int defStyleAttr, int defStyleRes) {
            return TypedArray.obtain(Resources.this, styleLayers, set, attrs, defStyleAttr, defStyleRes);
        }

        public boolean resolveAttribute(int resid, TypedValue outValue, boolean resolveRefs) {
            ResValue rv = null;
            for (int styleId : styleLayers) {
                rv = resolveStyleAttr(styleId, resid);
                if (rv != null) break;
            }
            if (rv == null) return false;
            if (outValue != null) {
                outValue.type = rv.type();
                outValue.data = rv.data();
                outValue.string = rv.string();
                outValue.resourceId = rv.isReference() ? rv.data() : resid;
            }
            return true;
        }
    }

    private final ApkFile apk;
    private final ResourceTable table;
    private final AssetManager assets;
    private final DisplayMetrics metrics = new DisplayMetrics();
    private final Configuration config = new Configuration();

    private static final Resources SYSTEM_RESOURCES = new Resources(null);

    public Resources(ApkFile apk) {
        this.apk = apk;
        this.table = apk != null ? apk.getResources() : ResourceTable.empty();
        this.assets = new AssetManager(apk);
    }

    public static Resources getSystem() {
        return SYSTEM_RESOURCES;
    }

    public ApkFile getApk() {
        return apk;
    }

    public ResourceTable getTable() {
        return table;
    }

    public final AssetManager getAssets() {
        return assets;
    }

    public DisplayMetrics getDisplayMetrics() {
        return metrics;
    }

    public Configuration getConfiguration() {
        return config;
    }

    public final Theme newTheme() {
        return new Theme();
    }

    public CharSequence getText(int id) {
        return getString(id);
    }

    public CharSequence getText(int id, CharSequence def) {
        try {
            return getString(id);
        } catch (Exception e) {
            return def;
        }
    }

    public String getString(int id) {
        String sys = AndroidSystemResources.systemString(id);
        if (sys != null) return sys;
        String s = table.getString(id);
        if (s != null) return s;
        throw new NotFoundException("String resource ID #0x" + Integer.toHexString(id));
    }

    public String getString(int id, Object... formatArgs) {
        return String.format(config.locale, getString(id), formatArgs);
    }

    public String[] getStringArray(int id) {
        ResourceTable.Entry e = table.getEntry(id);
        if (e == null || !e.isBag()) return new String[0];
        String[] out = new String[e.bag.size()];
        int i = 0;
        for (ResValue rv : e.bag.values()) {
            ResValue r = table.resolve(rv);
            out[i++] = r != null ? r.coerceToString() : "";
        }
        return out;
    }

    public int[] getIntArray(int id) {
        ResourceTable.Entry e = table.getEntry(id);
        if (e == null || !e.isBag()) return new int[0];
        int[] out = new int[e.bag.size()];
        int i = 0;
        for (ResValue rv : e.bag.values()) {
            ResValue r = table.resolve(rv);
            out[i++] = r != null ? r.data() : 0;
        }
        return out;
    }

    public int getColor(int id) {
        return getColor(id, null);
    }

    public int getColor(int id, Theme theme) {
        Integer sys = AndroidSystemResources.systemColor(id);
        if (sys != null) return sys;
        ResValue v = table.getValue(id);
        if (v != null) {
            if (v.isColor() || v.isInt()) return v.data();
            if (v.isString() && v.string() != null && v.string().startsWith("#")) {
                return Color.parseColor(v.string());
            }
        }
        return Color.BLACK;
    }

    public ColorStateList getColorStateList(int id) {
        return ColorStateList.valueOf(getColor(id));
    }

    public ColorStateList getColorStateList(int id, Theme theme) {
        return ColorStateList.valueOf(getColor(id, theme));
    }

    public boolean getBoolean(int id) {
        ResValue v = table.getValue(id);
        return v != null && v.data() != 0;
    }

    public int getInteger(int id) {
        ResValue v = table.getValue(id);
        return v != null ? v.data() : 0;
    }

    public float getDimension(int id) {
        ResValue v = table.getValue(id);
        if (v == null) return 0f;
        return resolveDimension(v, 0f);
    }

    public int getDimensionPixelOffset(int id) {
        return Math.round(getDimension(id));
    }

    public int getDimensionPixelSize(int id) {
        float d = getDimension(id);
        int res = Math.round(d);
        if (res != 0) return res;
        return d == 0f ? 0 : (d > 0 ? 1 : -1);
    }

    public Drawable getDrawable(int id) {
        return getDrawable(id, null);
    }

    public Drawable getDrawable(int id, Theme theme) {
        Integer sysColor = AndroidSystemResources.systemColor(id);
        if (sysColor != null) return new ColorDrawable(sysColor);

        ResValue v = table.getValue(id);
        if (v == null) return null;
        if (v.isColor()) return new ColorDrawable(v.data());
        if (v.isString() && v.string() != null && apk != null) {
            String path = v.string();
            try {
                byte[] bytes = apk.read(path);
                if (bytes == null) return null;
                if (path.endsWith(".xml")) {
                    XmlElement xml = AxmlParser.parse(bytes);
                    return XmlDrawableLoader.fromXml(xml, this);
                } else {
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bmp != null) return new BitmapDrawable(this, bmp);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public Bitmap loadBitmap(int id) {
        ResValue v = table.getValue(id);
        if (v != null && v.isString() && v.string() != null && apk != null) {
            try {
                byte[] bytes = apk.read(v.string());
                if (bytes != null && !v.string().endsWith(".xml")) {
                    return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                }
            } catch (Exception ignored) {
            }
        }
        Drawable d = getDrawable(id);
        if (d instanceof BitmapDrawable bd) return bd.getBitmap();
        if (d != null) {
            int w = Math.max(48, d.getIntrinsicWidth());
            int h = Math.max(48, d.getIntrinsicHeight());
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas c = new android.graphics.Canvas(bmp);
            d.setBounds(0, 0, w, h);
            d.draw(c);
            return bmp;
        }
        return null;
    }

    /** Đọc một file XML nhị phân (layout, menu, drawable...) theo ID resource. */
    public XmlElement loadXmlElement(int id) {
        ResValue v = table.getValue(id);
        if (v == null || !v.isString() || v.string() == null || apk == null) {
            throw new NotFoundException("XML resource ID #0x" + Integer.toHexString(id));
        }
        try {
            byte[] bytes = apk.read(v.string());
            if (bytes == null) throw new NotFoundException(v.string());
            return AxmlParser.parse(bytes);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new NotFoundException("Lỗi đọc XML " + v.string() + ": " + e.getMessage());
        }
    }

    public XmlResourceParser getXml(int id) throws NotFoundException {
        return new AxmlXmlResourceParser(loadXmlElement(id), table);
    }

    public XmlResourceParser getLayout(int id) throws NotFoundException {
        return getXml(id);
    }

    public XmlResourceParser getAnimation(int id) throws NotFoundException {
        return getXml(id);
    }

    public void getValue(int id, TypedValue outValue, boolean resolveRefs) throws NotFoundException {
        if (outValue == null) return;
        ResValue v = resolveRefs ? table.getValue(id) : null;
        if (v == null) {
            ResourceTable.Entry e = table.getEntry(id);
            if (e != null && !e.isBag()) {
                v = resolveRefs ? table.resolve(e.value) : e.value;
            }
        }
        if (v == null) {
            String sys = AndroidSystemResources.systemString(id);
            if (sys != null) {
                outValue.type = TypedValue.TYPE_STRING;
                outValue.string = sys;
                outValue.resourceId = id;
                return;
            }
            Integer sysCol = AndroidSystemResources.systemColor(id);
            if (sysCol != null) {
                outValue.type = TypedValue.TYPE_INT_COLOR_ARGB8;
                outValue.data = sysCol;
                outValue.resourceId = id;
                return;
            }
            throw new NotFoundException("Resource ID #0x" + Integer.toHexString(id));
        }
        outValue.type = v.type();
        outValue.data = v.data();
        outValue.string = v.string();
        outValue.resourceId = v.isReference() ? v.data() : id;
    }

    public void getValueForDensity(int id, int density, TypedValue outValue, boolean resolveRefs) throws NotFoundException {
        getValue(id, outValue, resolveRefs);
    }

    public float getFraction(int id, int base, int pbase) {
        ResValue v = table.getValue(id);
        if (v == null) return 0f;
        if (v.type() == ResValue.TYPE_FRACTION) {
            float val = v.complexValue();
            int type = v.complexUnit();
            return (type == 1 ? pbase : base) * val;
        }
        return v.getFloat();
    }

    public void updateConfiguration(Configuration config, DisplayMetrics metrics) {
        if (config != null) {
            this.config.setTo(config);
        }
        if (metrics != null) {
            this.metrics.setTo(metrics);
        }
    }

    public InputStream openRawResource(int id) {
        ResValue v = table.getValue(id);
        if (v != null && v.isString() && v.string() != null && apk != null) {
            try {
                byte[] bytes = apk.read(v.string());
                if (bytes != null) return new ByteArrayInputStream(bytes);
            } catch (Exception ignored) {
            }
        }
        throw new NotFoundException("Raw resource #0x" + Integer.toHexString(id));
    }

    public AssetFileDescriptor openRawResourceFd(int id) throws NotFoundException {
        ResValue v = table.getValue(id);
        if (v != null && v.isString() && v.string() != null && apk != null) {
            try {
                return assets.openNonAssetFd(v.string());
            } catch (Exception ignored) {
            }
        }
        throw new NotFoundException("Raw resource FD #0x" + Integer.toHexString(id));
    }

    public CharSequence[] getTextArray(int id) throws NotFoundException {
        return getStringArray(id);
    }

    public TypedArray obtainAttributes(AttributeSet set, int[] attrs) {
        return newTheme().obtainStyledAttributes(set, attrs, 0, 0);
    }

    public int getIdentifier(String name, String defType, String defPackage) {
        if (name == null) return 0;
        String pkg = defPackage;
        String type = defType;
        String entry = name;
        int colon = entry.indexOf(':');
        if (colon >= 0) {
            pkg = entry.substring(0, colon);
            entry = entry.substring(colon + 1);
        }
        int slash = entry.indexOf('/');
        if (slash >= 0) {
            type = entry.substring(0, slash);
            entry = entry.substring(slash + 1);
        }
        if ("android".equals(pkg) && "id".equals(type)) {
            if ("action_bar".equals(entry)) return AndroidSystemResources.ID_ACTION_BAR;
            if ("content".equals(entry)) return AndroidSystemResources.ID_ANDROID_CONTENT;
        }
        if (type == null) return 0;
        return table.getIdentifier(type, entry);
    }

    public String getResourceName(int resid) {
        String n = table.getResourceName(resid);
        if (n == null) throw new NotFoundException("#0x" + Integer.toHexString(resid));
        return n;
    }

    public String getResourceEntryName(int resid) {
        ResourceTable.Entry e = table.getEntry(resid);
        if (e == null) throw new NotFoundException("#0x" + Integer.toHexString(resid));
        return e.key;
    }

    public String getResourceTypeName(int resid) {
        ResourceTable.Entry e = table.getEntry(resid);
        if (e == null) throw new NotFoundException("#0x" + Integer.toHexString(resid));
        return e.type;
    }

    public String getResourcePackageName(int resid) {
        return table.getPackageName();
    }

    // --------------------------------------------------------- hỗ trợ giải quyết giá trị

    public String resolveString(ResValue v) {
        if (v == null) return null;
        if (v.isReference()) {
            String sys = AndroidSystemResources.systemString(v.data());
            if (sys != null) return sys;
        }
        ResValue r = table.resolve(v);
        return r != null ? r.coerceToString() : null;
    }

    public int resolveColor(ResValue v, int def) {
        if (v == null) return def;
        if (v.isReference()) {
            Integer sys = AndroidSystemResources.systemColor(v.data());
            if (sys != null) return sys;
        }
        ResValue r = table.resolve(v);
        if (r == null) return def;
        if (r.isColor() || r.isInt()) return r.data();
        if (r.isString() && r.string() != null && r.string().startsWith("#")) {
            try {
                return Color.parseColor(r.string());
            } catch (Exception ignored) {
            }
        }
        return def;
    }

    public float resolveDimension(ResValue v, float def) {
        if (v == null) return def;
        ResValue r = table.resolve(v);
        if (r == null) return def;
        if (r.isDimension()) {
            return TypedValue.applyDimension(r.complexUnit(), r.complexValue(), metrics);
        }
        if (r.type() == ResValue.TYPE_FLOAT) return r.getFloat();
        if (r.isInt()) return r.data();
        return def;
    }

    /** Tìm thuộc tính trong một style và chuỗi parent của nó. */
    public ResValue resolveStyleAttr(int styleResId, int attrResId) {
        String targetName = AndroidSystemResources.attrName(attrResId);
        int cur = styleResId;
        for (int depth = 0; depth < 32 && cur != 0; depth++) {
            ResourceTable.Entry e = table.getEntry(cur);
            if (e == null || !e.isBag()) break;
            ResValue direct = e.bag.get(attrResId);
            if (direct != null) return table.resolve(direct);
            if (targetName != null) {
                for (Map.Entry<Integer, ResValue> kv : e.bag.entrySet()) {
                    ResourceTable.Entry ke = table.getEntry(kv.getKey());
                    if (ke != null && targetName.equals(ke.key)) {
                        return table.resolve(kv.getValue());
                    }
                }
            }
            cur = e.parent;
        }
        return null;
    }
}
