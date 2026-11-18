package dev.aaow.apk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Thông tin cần thiết lấy từ AndroidManifest.xml. */
public final class AndroidManifest {
    public static final int ATTR_THEME = 0x01010000;
    public static final int ATTR_LABEL = 0x01010001;
    public static final int ATTR_ICON = 0x01010002;
    public static final int ATTR_NAME = 0x01010003;

    public String packageName = "";
    public String versionName = "";
    public int versionCode;
    public int minSdk = 1;
    public int targetSdk;
    public String label;
    public int iconRes;
    public int themeRes;
    public String applicationClass;
    public String launcherActivity;
    public final List<Activity> activities = new ArrayList<>();
    public final List<Service> services = new ArrayList<>();
    public final List<Receiver> receivers = new ArrayList<>();
    public final List<Provider> providers = new ArrayList<>();
    public final Map<String, MetaData> applicationMetaData = new LinkedHashMap<>();
    public final List<String> permissions = new ArrayList<>();
    public final List<String> usesLibraries = new ArrayList<>();
    public XmlElement xml;

    public record MetaData(String name, int type, int intData, String stringValue, int resourceId) {
    }

    public static final class IntentFilterInfo {
        public final List<String> actions = new ArrayList<>();
        public final List<String> categories = new ArrayList<>();
    }

    public static final class Activity {
        public String name;
        public String label;
        public int themeRes;
        public String targetActivity;
        public boolean launcher;
        public final Map<String, MetaData> metaData = new LinkedHashMap<>();

        @Override
        public String toString() {
            return name + (launcher ? " [LAUNCHER]" : "");
        }
    }

    public static final class Service {
        public String name;
        public String permission;
        public boolean enabled = true;
        public boolean exported;
        public final List<IntentFilterInfo> intentFilters = new ArrayList<>();
        public final Map<String, MetaData> metaData = new LinkedHashMap<>();
    }

    public static final class Receiver {
        public String name;
        public String permission;
        public boolean enabled = true;
        public boolean exported;
        public final List<IntentFilterInfo> intentFilters = new ArrayList<>();
        public final Map<String, MetaData> metaData = new LinkedHashMap<>();
    }

    public static final class Provider {
        public String name;
        public String authorities;
        public boolean enabled = true;
        public boolean exported;
        public boolean grantUriPermissions;
        public int initOrder;
        public String readPermission;
        public String writePermission;
        public final Map<String, MetaData> metaData = new LinkedHashMap<>();
    }

    public static AndroidManifest parse(byte[] axml, ResourceTable res) {
        AndroidManifest m = new AndroidManifest();
        XmlElement root = AxmlParser.parse(axml);
        m.xml = root;
        m.packageName = str(root.attr(null, "package"), res);
        if (m.packageName == null) m.packageName = res.getPackageName();
        m.versionName = str(root.attr("versionName"), res);
        m.versionCode = intVal(root.attr("versionCode"), 0);

        for (XmlElement c : root.children) {
            switch (c.name) {
                case "uses-sdk" -> {
                    m.minSdk = intVal(c.attr("minSdkVersion"), 1);
                    m.targetSdk = intVal(c.attr("targetSdkVersion"), m.minSdk);
                }
                case "uses-permission" -> {
                    String p = str(c.attr("name"), res);
                    if (p != null) m.permissions.add(p);
                }
                case "application" -> parseApplication(m, c, res);
                default -> {
                }
            }
        }
        if (m.launcherActivity == null && !m.activities.isEmpty()) {
            m.launcherActivity = m.activities.get(0).name;
        }
        return m;
    }

    private static void parseApplication(AndroidManifest m, XmlElement app, ResourceTable res) {
        m.label = str(app.attr("label"), res);
        m.iconRes = refVal(app.attr("icon"));
        m.themeRes = refVal(app.attr("theme"));
        String appName = str(app.attr("name"), res);
        if (appName != null) m.applicationClass = qualify(m.packageName, appName);

        for (XmlElement c : app.children) {
            boolean alias = c.name.equals("activity-alias");
            if (c.name.equals("uses-library")) {
                String lib = str(c.attr("name"), res);
                if (lib != null) m.usesLibraries.add(lib);
                continue;
            }
            if (c.name.equals("meta-data")) {
                parseMetaData(c, res, m.applicationMetaData);
                continue;
            }
            if (c.name.equals("service")) {
                Service s = new Service();
                s.name = qualify(m.packageName, str(c.attr("name"), res));
                s.permission = str(c.attr("permission"), res);
                s.enabled = boolVal(c.attr("enabled"), res, true);
                s.exported = boolVal(c.attr("exported"), res, false);
                parseIntentFilters(c, res, s.intentFilters);
                parseChildrenMetaData(c, res, s.metaData);
                m.services.add(s);
                continue;
            }
            if (c.name.equals("receiver")) {
                Receiver r = new Receiver();
                r.name = qualify(m.packageName, str(c.attr("name"), res));
                r.permission = str(c.attr("permission"), res);
                r.enabled = boolVal(c.attr("enabled"), res, true);
                r.exported = boolVal(c.attr("exported"), res, false);
                parseIntentFilters(c, res, r.intentFilters);
                parseChildrenMetaData(c, res, r.metaData);
                m.receivers.add(r);
                continue;
            }
            if (c.name.equals("provider")) {
                Provider p = new Provider();
                p.name = qualify(m.packageName, str(c.attr("name"), res));
                p.authorities = str(c.attr("authorities"), res);
                p.enabled = boolVal(c.attr("enabled"), res, true);
                p.exported = boolVal(c.attr("exported"), res, false);
                p.grantUriPermissions = boolVal(c.attr("grantUriPermissions"), res, false);
                p.initOrder = intVal(c.attr("initOrder"), 0);
                p.readPermission = str(c.attr("readPermission"), res);
                p.writePermission = str(c.attr("writePermission"), res);
                parseChildrenMetaData(c, res, p.metaData);
                m.providers.add(p);
                continue;
            }
            if (!c.name.equals("activity") && !alias) continue;
            Activity a = new Activity();
            a.name = qualify(m.packageName, str(c.attr("name"), res));
            a.label = str(c.attr("label"), res);
            a.themeRes = refVal(c.attr("theme"));
            if (alias) a.targetActivity = qualify(m.packageName, str(c.attr("targetActivity"), res));
            for (XmlElement f : c.children("intent-filter")) {
                boolean main = false, launcher = false;
                for (XmlElement x : f.children) {
                    String v = str(x.attr("name"), res);
                    if (x.name.equals("action") && "android.intent.action.MAIN".equals(v)) main = true;
                    if (x.name.equals("category") && "android.intent.category.LAUNCHER".equals(v)) launcher = true;
                }
                if (main && launcher) a.launcher = true;
            }
            parseChildrenMetaData(c, res, a.metaData);
            m.activities.add(a);
            if (a.launcher && m.launcherActivity == null) {
                m.launcherActivity = a.targetActivity != null ? a.targetActivity : a.name;
            }
        }
    }

    private static void parseIntentFilters(XmlElement comp, ResourceTable res, List<IntentFilterInfo> out) {
        for (XmlElement f : comp.children("intent-filter")) {
            IntentFilterInfo info = new IntentFilterInfo();
            for (XmlElement x : f.children) {
                String v = str(x.attr("name"), res);
                if (v == null) continue;
                if (x.name.equals("action")) info.actions.add(v);
                else if (x.name.equals("category")) info.categories.add(v);
            }
            out.add(info);
        }
    }

    private static void parseChildrenMetaData(XmlElement comp, ResourceTable res, Map<String, MetaData> out) {
        for (XmlElement mc : comp.children("meta-data")) {
            parseMetaData(mc, res, out);
        }
    }

    private static void parseMetaData(XmlElement mc, ResourceTable res, Map<String, MetaData> out) {
        String key = str(mc.attr("name"), res);
        if (key == null || key.isEmpty()) return;
        XmlElement.Attribute valAttr = mc.attr("value");
        XmlElement.Attribute resAttr = mc.attr("resource");
        int resId = refVal(resAttr);
        int type = ResValue.TYPE_NULL;
        int intData = 0;
        String sVal = null;
        if (valAttr != null) {
            ResValue rv = res.resolve(valAttr.value);
            if (rv != null) {
                type = rv.type();
                intData = rv.data();
                sVal = rv.coerceToString();
            }
        }
        out.put(key, new MetaData(key, type, intData, sVal, resId));
    }

    public Activity findActivity(String className) {
        for (Activity a : activities) if (className != null && className.equals(a.name)) return a;
        return null;
    }

    public Service findService(String className) {
        for (Service s : services) if (className != null && className.equals(s.name)) return s;
        return null;
    }

    public Service findServiceByAction(String action) {
        if (action == null) return null;
        for (Service s : services) {
            if (!s.enabled) continue;
            for (IntentFilterInfo f : s.intentFilters) {
                if (f.actions.contains(action)) return s;
            }
        }
        return null;
    }

    public Provider findProviderByAuthority(String authority) {
        if (authority == null || authority.isEmpty()) return null;
        for (Provider p : providers) {
            if (!p.enabled || p.authorities == null) continue;
            for (String part : p.authorities.split(";")) {
                if (authority.equals(part.trim())) return p;
            }
        }
        return null;
    }

    static String qualify(String pkg, String name) {
        if (name == null) return null;
        if (name.startsWith(".")) return pkg + name;
        if (!name.contains(".")) return pkg + "." + name;
        return name;
    }

    private static String str(XmlElement.Attribute a, ResourceTable res) {
        if (a == null) return null;
        ResValue v = res.resolve(a.value);
        return v == null ? null : v.coerceToString();
    }

    private static boolean boolVal(XmlElement.Attribute a, ResourceTable res, boolean def) {
        if (a == null) return def;
        ResValue v = res.resolve(a.value);
        if (v == null) return def;
        if (v.type() == ResValue.TYPE_INT_BOOLEAN || v.isInt()) return v.data() != 0;
        String s = v.coerceToString();
        if ("true".equalsIgnoreCase(s)) return true;
        if ("false".equalsIgnoreCase(s)) return false;
        return def;
    }

    private static int intVal(XmlElement.Attribute a, int def) {
        if (a == null) return def;
        if (a.value.isInt()) return a.value.data();
        try {
            return Integer.parseInt(a.value.coerceToString());
        } catch (Exception e) {
            return def;
        }
    }

    private static int refVal(XmlElement.Attribute a) {
        return a != null && a.value.isReference() ? a.value.data() : 0;
    }
}
