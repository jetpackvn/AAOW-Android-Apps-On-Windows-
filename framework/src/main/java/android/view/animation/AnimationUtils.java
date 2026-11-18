package android.view.animation;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemClock;
import android.util.AttributeSet;
import dev.aaow.apk.XmlElement;

public class AnimationUtils {
    public static long currentAnimationTimeMillis() {
        return SystemClock.uptimeMillis();
    }

    public static Animation loadAnimation(Context context, int id) throws Resources.NotFoundException {
        if (context == null) throw new Resources.NotFoundException("Null context");
        XmlElement el = context.getResources().loadXmlElement(id);
        return createAnimationFromXml(context, el);
    }

    private static Animation createAnimationFromXml(Context context, XmlElement el) {
        AttributeSet attrs = new AttributeSet.XmlAttributeSet(el, context.getResources().getTable());
        Animation anim = switch (el.name) {
            case "set" -> {
                AnimationSet set = new AnimationSet(context, attrs);
                for (XmlElement child : el.children) {
                    set.addAnimation(createAnimationFromXml(context, child));
                }
                yield set;
            }
            case "alpha" -> new AlphaAnimation(context, attrs);
            case "scale" -> new ScaleAnimation(context, attrs);
            case "rotate" -> new RotateAnimation(context, attrs);
            case "translate" -> new TranslateAnimation(context, attrs);
            default -> new AlphaAnimation(1.0f, 1.0f);
        };
        XmlElement.Attribute dur = el.attr("duration");
        if (dur != null && dur.value.isInt()) {
            anim.setDuration(dur.value.data());
        }
        return anim;
    }

    public static Interpolator loadInterpolator(Context context, int id) throws Resources.NotFoundException {
        return new AccelerateDecelerateInterpolator();
    }

    public static Animation makeInAnimation(Context c, boolean fromLeft) {
        TranslateAnimation a = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, fromLeft ? -1f : 1f,
                Animation.RELATIVE_TO_PARENT, 0f,
                Animation.ABSOLUTE, 0f,
                Animation.ABSOLUTE, 0f);
        a.setDuration(250);
        return a;
    }

    public static Animation makeOutAnimation(Context c, boolean toRight) {
        TranslateAnimation a = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0f,
                Animation.RELATIVE_TO_PARENT, toRight ? 1f : -1f,
                Animation.ABSOLUTE, 0f,
                Animation.ABSOLUTE, 0f);
        a.setDuration(250);
        return a;
    }

    public static Animation makeInChildBottomAnimation(Context c) {
        TranslateAnimation a = new TranslateAnimation(
                Animation.ABSOLUTE, 0f,
                Animation.ABSOLUTE, 0f,
                Animation.RELATIVE_TO_PARENT, 1f,
                Animation.RELATIVE_TO_PARENT, 0f);
        a.setDuration(250);
        return a;
    }
}
