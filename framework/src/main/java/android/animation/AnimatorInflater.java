package android.animation;

import android.content.Context;
import android.content.res.Resources;

public class AnimatorInflater {
    private AnimatorInflater() {
    }

    public static Animator loadAnimator(Context context, int id) throws Resources.NotFoundException {
        ObjectAnimator anim = new ObjectAnimator();
        anim.setPropertyName("alpha");
        anim.setFloatValues(0f, 1f);
        anim.setDuration(250);
        return anim;
    }

    public static StateListAnimator loadStateListAnimator(Context context, int id) throws Resources.NotFoundException {
        return new StateListAnimator();
    }
}
