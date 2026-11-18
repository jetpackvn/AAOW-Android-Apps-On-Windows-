package android.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.AttributeSet;
import dev.aaow.apk.XmlElement;

import java.lang.reflect.Constructor;

public class PreferenceManager {
    public interface OnActivityDestroyListener {
        void onActivityDestroy();
    }

    private final Context context;
    private PreferenceScreen preferenceScreen;

    public PreferenceManager(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public SharedPreferences getSharedPreferences() {
        return context != null ? getDefaultSharedPreferences(context) : null;
    }

    public PreferenceScreen createPreferenceScreen(Context context) {
        PreferenceScreen ps = new PreferenceScreen(context, null);
        ps.onAttachedToHierarchy(this);
        return ps;
    }

    public PreferenceScreen getPreferenceScreen() {
        return preferenceScreen;
    }

    public boolean setPreferences(PreferenceScreen screen) {
        if (screen != preferenceScreen) {
            this.preferenceScreen = screen;
            return true;
        }
        return false;
    }

    public PreferenceScreen inflateFromResource(Context context, int resId, PreferenceScreen root) {
        if (context == null || resId == 0) return root;
        Resources res = context.getResources();
        XmlElement xml = res.loadXmlElement(resId);
        PreferenceScreen screen = root != null ? root : createPreferenceScreen(context);
        inflateElement(context, xml, screen, res);
        return screen;
    }

    private void inflateElement(Context ctx, XmlElement el, PreferenceGroup parent, Resources res) {
        for (XmlElement child : el.children) {
            Preference pref = createPreference(ctx, child.name, new AttributeSet.XmlAttributeSet(child, res.getTable()));
            if (pref != null) {
                pref.onAttachedToHierarchy(this);
                parent.addItemFromInflater(pref);
                if (pref instanceof PreferenceGroup pg) {
                    inflateElement(ctx, child, pg, res);
                }
            }
        }
    }

    private static Preference createPreference(Context ctx, String tag, AttributeSet attrs) {
        return switch (tag) {
            case "PreferenceScreen" -> new PreferenceScreen(ctx, attrs);
            case "PreferenceCategory" -> new PreferenceGroup(ctx, attrs) {
            };
            case "CheckBoxPreference", "SwitchPreference" -> new CheckBoxPreference(ctx, attrs);
            case "EditTextPreference" -> new EditTextPreference(ctx, attrs);
            case "ListPreference" -> new ListPreference(ctx, attrs);
            case "Preference" -> new Preference(ctx, attrs);
            default -> {
                if (tag.contains(".")) {
                    try {
                        ClassLoader cl = ctx.getClassLoader() != null ? ctx.getClassLoader() : PreferenceManager.class.getClassLoader();
                        Class<?> cls = Class.forName(tag, true, cl);
                        if (Preference.class.isAssignableFrom(cls)) {
                            try {
                                Constructor<?> c2 = cls.getConstructor(Context.class, AttributeSet.class);
                                yield (Preference) c2.newInstance(ctx, attrs);
                            } catch (NoSuchMethodException e) {
                                yield (Preference) cls.getConstructor(Context.class).newInstance(ctx);
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
                yield new Preference(ctx, attrs);
            }
        };
    }

    public static String getDefaultSharedPreferencesName(Context context) {
        return (context != null ? context.getPackageName() : "default") + "_preferences";
    }

    public static int getDefaultSharedPreferencesMode() {
        return Context.MODE_PRIVATE;
    }

    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return context.getSharedPreferences(getDefaultSharedPreferencesName(context), getDefaultSharedPreferencesMode());
    }

    public static void setDefaultValues(Context context, int resId, boolean readAgain) {
    }

    public static void setDefaultValues(Context context, String sharedPreferencesName, int sharedPreferencesMode,
                                        int resId, boolean readAgain) {
    }
}
