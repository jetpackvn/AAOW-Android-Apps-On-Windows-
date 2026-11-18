package android.view;

import android.content.Context;
import android.content.res.Resources;
import dev.aaow.apk.ResValue;
import dev.aaow.apk.XmlElement;

import java.lang.reflect.Constructor;

/**
 * Giải mã file XML menu (res/menu/*.xml) thành các MenuItem / SubMenu sống.
 */
public class MenuInflater {
    private final Context context;

    public MenuInflater(Context context) {
        this.context = context;
    }

    public void inflate(int menuRes, Menu menu) {
        if (context == null || menu == null || menuRes == 0) return;
        Resources res = context.getResources();
        XmlElement root = res.loadXmlElement(menuRes);
        parseMenu(root, menu, res, Menu.NONE, true, true, false, false);
    }

    private void parseMenu(XmlElement element, Menu menu, Resources res, int groupId,
                           boolean groupVisible, boolean groupEnabled,
                           boolean groupCheckable, boolean groupExclusive) {
        if (element == null) return;
        for (XmlElement child : element.children) {
            switch (child.name) {
                case "group" -> {
                    int gid = refId(child.attr("id"), groupId);
                    boolean vis = boolAttr(child, "visible", res, groupVisible);
                    boolean en = boolAttr(child, "enabled", res, groupEnabled);
                    int cbMode = intAttr(child, "checkableBehavior", res, 0);
                    boolean checkable = cbMode != 0;
                    boolean exclusive = cbMode == 1;
                    parseMenu(child, menu, res, gid, vis, en, checkable, exclusive);
                }
                case "item" -> {
                    int itemId = refId(child.attr("id"), Menu.NONE);
                    int order = intAttr(child, "orderInCategory", res, Menu.NONE);
                    CharSequence title = strAttr(child, "title", res);
                    if (title == null) title = "";

                    XmlElement nestedMenu = findChild(child, "menu");
                    MenuItem item;
                    if (nestedMenu != null) {
                        SubMenu sub = menu.addSubMenu(groupId, itemId, order, title);
                        item = sub.getItem();
                        parseMenu(nestedMenu, sub, res, Menu.NONE, true, true, false, false);
                    } else {
                        item = menu.add(groupId, itemId, order, title);
                    }

                    CharSequence condensed = strAttr(child, "titleCondensed", res);
                    if (condensed != null) item.setTitleCondensed(condensed);

                    int iconRes = refId(child.attr("icon"), 0);
                    if (iconRes != 0) item.setIcon(iconRes);

                    item.setVisible(boolAttr(child, "visible", res, groupVisible));
                    item.setEnabled(boolAttr(child, "enabled", res, groupEnabled));

                    boolean itemCheckable = boolAttr(child, "checkable", res, groupCheckable);
                    if (itemCheckable) item.setCheckable(true);
                    if (boolAttr(child, "checked", res, false)) item.setChecked(true);

                    if (groupCheckable && groupExclusive) {
                        menu.setGroupCheckable(groupId, true, true);
                    }

                    int showAsAction = readShowAsAction(child, res);
                    item.setShowAsAction(showAsAction);

                    int actionLayout = refId(attrAnyNs(child, "actionLayout"), 0);
                    if (actionLayout != 0) {
                        item.setActionView(actionLayout);
                    } else {
                        String actionViewClass = strAttrAnyNs(child, "actionViewClass", res);
                        if (actionViewClass != null) {
                            View av = createActionView(actionViewClass);
                            if (av != null) item.setActionView(av);
                        }
                    }
                }
            }
        }
    }

    private View createActionView(String className) {
        try {
            ClassLoader cl = context.getClassLoader() != null
                    ? context.getClassLoader()
                    : getClass().getClassLoader();
            Class<?> cls = Class.forName(className, true, cl);
            if (View.class.isAssignableFrom(cls)) {
                Constructor<?> c = cls.getConstructor(Context.class);
                return (View) c.newInstance(context);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static XmlElement findChild(XmlElement parent, String tag) {
        for (XmlElement c : parent.children) {
            if (tag.equals(c.name)) return c;
        }
        return null;
    }

    private static XmlElement.Attribute attrAnyNs(XmlElement el, String name) {
        XmlElement.Attribute a = el.attr(XmlElement.ANDROID_NS, name);
        if (a != null) return a;
        a = el.attr(XmlElement.AUTO_NS, name);
        if (a != null) return a;
        for (XmlElement.Attribute attr : el.attributes) {
            if (name.equals(attr.name)) return attr;
        }
        return null;
    }

    private static String strAttr(XmlElement el, String name, Resources res) {
        XmlElement.Attribute a = el.attr(name);
        return a != null ? res.resolveString(a.value) : null;
    }

    private static String strAttrAnyNs(XmlElement el, String name, Resources res) {
        XmlElement.Attribute a = attrAnyNs(el, name);
        return a != null ? res.resolveString(a.value) : null;
    }

    private static int refId(XmlElement.Attribute a, int def) {
        return a != null && a.value.isReference() ? a.value.data() : def;
    }

    private static boolean boolAttr(XmlElement el, String name, Resources res, boolean def) {
        XmlElement.Attribute a = el.attr(name);
        if (a == null) return def;
        ResValue v = res.getTable().resolve(a.value);
        return v != null ? v.data() != 0 : def;
    }

    private static int intAttr(XmlElement el, String name, Resources res, int def) {
        XmlElement.Attribute a = el.attr(name);
        if (a == null) return def;
        ResValue v = res.getTable().resolve(a.value);
        return v != null && v.isInt() ? v.data() : def;
    }

    private static int readShowAsAction(XmlElement el, Resources res) {
        XmlElement.Attribute a = attrAnyNs(el, "showAsAction");
        if (a == null) return MenuItem.SHOW_AS_ACTION_NEVER;
        ResValue v = res.getTable().resolve(a.value);
        if (v != null && v.isInt()) return v.data();
        String s = res.resolveString(a.value);
        if (s == null) return MenuItem.SHOW_AS_ACTION_NEVER;
        int flags = 0;
        if (s.contains("always")) flags |= MenuItem.SHOW_AS_ACTION_ALWAYS;
        if (s.contains("ifRoom")) flags |= MenuItem.SHOW_AS_ACTION_IF_ROOM;
        if (s.contains("withText")) flags |= MenuItem.SHOW_AS_ACTION_WITH_TEXT;
        if (s.contains("collapseActionView")) flags |= MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW;
        return flags;
    }
}
