package dev.aaow.apk;

import java.util.ArrayList;
import java.util.List;

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
    public final List<String> permissions = new ArrayList<>();
    public final List<String> usesLibraries = new ArrayList<>();
    public XmlElement xml;

    public static final class Activity {
        public String name;
        public String label;
        public int themeRes;
        public String targetActivity;
        public boolean launcher;

        @Override
        public String toString() {
            return name + (launcher ? " [LAUNCHER]" : "");
        }
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
            m.activities.add(a);
            if (a.launcher && m.launcherActivity == null) {
                m.launcherActivity = a.targetActivity != null ? a.targetActivity : a.name;
            }
        }
    }

    public Activity findActivity(String className) {
        for (Activity a : activities) if (a.name.equals(className)) return a;
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
