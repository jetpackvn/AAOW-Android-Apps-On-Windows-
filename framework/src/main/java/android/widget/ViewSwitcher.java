package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class ViewSwitcher extends ViewAnimator {
    public interface ViewFactory {
        View makeView();
    }

    public ViewSwitcher(Context context) {
        super(context);
    }

    public ViewSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public View getNextView() {
        int next = getDisplayedChild() == 0 ? 1 : 0;
        return getChildAt(next);
    }

    public void reset() {
    }
}
