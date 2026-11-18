package android.view;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import dev.aaow.apk.XmlElement;

import java.lang.reflect.Constructor;

/**
 * Cài đặt android.view.LayoutInflater: biến cây XML nhị phân (AXML) trong APK
 * thành cây đối tượng View/ViewGroup sống.
 */
public class LayoutInflater {
    public interface Factory {
        View onCreateView(String name, Context context, AttributeSet attrs);
    }

    public interface Factory2 extends Factory {
        View onCreateView(View parent, String name, Context context, AttributeSet attrs);
    }

    public interface Filter {
        boolean onLoadClass(Class clazz);
    }

    private final Context context;
    private Factory factory;
    private Factory2 factory2;

    public LayoutInflater(Context context) {
        this.context = context;
    }

    public static LayoutInflater from(Context context) {
        if (context != null) {
            Object svc = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (svc instanceof LayoutInflater li) return li;
        }
        return new LayoutInflater(context);
    }

    public LayoutInflater cloneInContext(Context newContext) {
        LayoutInflater li = new LayoutInflater(newContext);
        li.factory = this.factory;
        li.factory2 = this.factory2;
        return li;
    }

    public Context getContext() {
        return context;
    }

    public final Factory getFactory() {
        return factory;
    }

    public final Factory2 getFactory2() {
        return factory2;
    }

    public void setFactory(Factory factory) {
        this.factory = factory;
    }

    public void setFactory2(Factory2 factory) {
        this.factory2 = factory;
        this.factory = factory;
    }

    public View inflate(int resource, ViewGroup root) {
        return inflate(resource, root, root != null);
    }

    public View inflate(int resource, ViewGroup root, boolean attachToRoot) {
        Resources res = context.getResources();
        XmlElement xmlRoot = res.loadXmlElement(resource);
        return inflateElement(xmlRoot, root, attachToRoot);
    }

    private View inflateElement(XmlElement el, ViewGroup parent, boolean attachToRoot) {
        Resources res = context.getResources();
        if ("include".equals(el.name)) {
            XmlElement.Attribute layoutAttr = el.attr(null, "layout");
            if (layoutAttr != null && layoutAttr.value.isReference()) {
                XmlElement included = res.loadXmlElement(layoutAttr.value.data());
                return inflateElement(included, parent, attachToRoot);
            }
        }
        if ("merge".equals(el.name)) {
            if (parent == null) throw new IllegalStateException("<merge /> requires a non-null ViewGroup root");
            for (XmlElement childEl : el.children) {
                inflateElement(childEl, parent, true);
            }
            return parent;
        }

        AttributeSet attrs = new AttributeSet.XmlAttributeSet(el, res.getTable());
        View view = createViewFromTag(parent, el.name, attrs);
        ViewGroup.LayoutParams lp = parent != null ? parent.generateLayoutParams(attrs) : null;
        if (lp != null) view.setLayoutParams(lp);

        if (view instanceof ViewGroup vg) {
            for (XmlElement childEl : el.children) {
                if ("requestFocus".equals(childEl.name)) continue;
                inflateElement(childEl, vg, true);
            }
        }

        if (parent != null && attachToRoot) {
            parent.addView(view, lp);
            return parent;
        }
        return view;
    }

    private View createViewFromTag(View parent, String tagName, AttributeSet attrs) {
        if ("view".equals(tagName) && attrs != null) {
            String classAttr = attrs.getClassAttribute();
            if (classAttr != null && !classAttr.isEmpty()) {
                tagName = classAttr;
            }
        }
        if (factory2 != null) {
            View v = factory2.onCreateView(parent, tagName, context, attrs);
            if (v != null) return v;
        } else if (factory != null) {
            View v = factory.onCreateView(tagName, context, attrs);
            if (v != null) return v;
        }
        if (context instanceof Factory2 priv && priv != factory2) {
            View v = priv.onCreateView(parent, tagName, context, attrs);
            if (v != null) return v;
        }

        ClassLoader cl = context.getClassLoader() != null ? context.getClassLoader() : getClass().getClassLoader();
        View direct = instantiateByName(tagName, attrs, cl);
        if (direct != null) return direct;

        String mapped = mapTagName(tagName);
        if (!mapped.equals(tagName)) {
            View mappedView = instantiateByName(mapped, attrs, cl);
            if (mappedView != null) return mappedView;
        }
        return createSupportFallbackView(tagName, attrs);
    }

    private View instantiateByName(String name, AttributeSet attrs, ClassLoader cl) {
        String[] prefixes = name.contains(".")
                ? new String[]{""}
                : new String[]{"android.widget.", "android.view.", "android.webkit."};

        for (String prefix : prefixes) {
            String fullName = prefix + name;
            try {
                Class<?> cls = Class.forName(fullName, true, cl);
                if (View.class.isAssignableFrom(cls)) {
                    try {
                        Constructor<?> c2 = cls.getConstructor(Context.class, AttributeSet.class);
                        return (View) c2.newInstance(context, attrs);
                    } catch (NoSuchMethodException e) {
                        Constructor<?> c1 = cls.getConstructor(Context.class);
                        return (View) c1.newInstance(context);
                    }
                }
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable t) {
                Throwable root = (t instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null)
                        ? ite.getCause() : t;
                if (isSupportOrMaterialTag(name)) {
                    System.err.println("[AAOW LayoutInflater] Fallback cho <" + name + "> sau lỗi: " + root);
                    return createSupportFallbackView(name, attrs);
                }
                if (root instanceof RuntimeException re) throw re;
                throw new RuntimeException("Lỗi khởi tạo View <" + name + ">: " + root, root);
            }
        }
        return null;
    }

    private static boolean isSupportOrMaterialTag(String tag) {
        return tag != null && (tag.startsWith("androidx.")
                || tag.startsWith("android.support.")
                || tag.startsWith("com.google.android.material."));
    }

    private View createSupportFallbackView(String tagName, AttributeSet attrs) {
        if (tagName.endsWith("ConstraintLayout")) {
            return new dev.aaow.rt.compat.CompatConstraintLayout(context, attrs);
        }
        if (tagName.endsWith("RecyclerView")) {
            return new dev.aaow.rt.compat.CompatRecyclerView(context, attrs);
        }
        if (tagName.endsWith("ViewPager") || tagName.endsWith("ViewPager2")) {
            return new dev.aaow.rt.compat.CompatViewPager(context, attrs);
        }
        if (tagName.endsWith("CardView")) {
            return new dev.aaow.rt.compat.CompatCardView(context, attrs);
        }
        if (tagName.endsWith("ScrollView")) {
            return new android.widget.ScrollView(context, attrs);
        }
        if (tagName.contains("AppBarLayout") || tagName.contains("Linear") || tagName.endsWith("TabLayout")) {
            android.widget.LinearLayout ll = new android.widget.LinearLayout(context, attrs);
            if (tagName.contains("AppBarLayout")) {
                ll.setOrientation(android.widget.LinearLayout.VERTICAL);
            }
            return ll;
        }
        if (tagName.endsWith("Toolbar")) {
            return new android.widget.Toolbar(context, attrs);
        }
        if (tagName.contains("FloatingActionButton") || tagName.endsWith("ImageButton")) {
            return new android.widget.ImageButton(context, attrs);
        }
        if (tagName.endsWith("Layout") || tagName.endsWith("ContainerView") || tagName.endsWith("Group")) {
            return new android.widget.FrameLayout(context, attrs);
        }
        return new android.widget.TextView(context, attrs);
    }

    private static String mapTagName(String tag) {
        return switch (tag) {
            case "androidx.appcompat.widget.AppCompatTextView",
                 "com.google.android.material.textview.MaterialTextView" -> "android.widget.TextView";
            case "androidx.appcompat.widget.AppCompatButton",
                 "com.google.android.material.button.MaterialButton" -> "android.widget.Button";
            case "androidx.appcompat.widget.AppCompatEditText",
                 "com.google.android.material.textfield.TextInputEditText" -> "android.widget.EditText";
            case "androidx.appcompat.widget.AppCompatImageView",
                 "androidx.appcompat.widget.AppCompatImageButton" -> "android.widget.ImageView";
            case "androidx.appcompat.widget.AppCompatCheckBox",
                 "com.google.android.material.checkbox.MaterialCheckBox" -> "android.widget.CheckBox";
            case "androidx.appcompat.widget.SwitchCompat",
                 "com.google.android.material.switchmaterial.SwitchMaterial",
                 "com.google.android.material.materialswitch.MaterialSwitch" -> "android.widget.Switch";
            case "androidx.appcompat.widget.LinearLayoutCompat" -> "android.widget.LinearLayout";
            case "androidx.core.widget.NestedScrollView" -> "android.widget.ScrollView";
            case "androidx.cardview.widget.CardView",
                 "com.google.android.material.card.MaterialCardView" -> "dev.aaow.rt.compat.CompatCardView";
            default -> tag;
        };
    }
}
