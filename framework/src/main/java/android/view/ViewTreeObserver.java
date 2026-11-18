package android.view;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ViewTreeObserver {
    public interface OnGlobalLayoutListener {
        void onGlobalLayout();
    }

    public interface OnPreDrawListener {
        boolean onPreDraw();
    }

    public interface OnDrawListener {
        void onDraw();
    }

    public interface OnScrollChangedListener {
        void onScrollChanged();
    }

    public interface OnGlobalFocusChangeListener {
        void onGlobalFocusChanged(View oldFocus, View newFocus);
    }

    private final List<OnGlobalLayoutListener> globalLayoutListeners = new CopyOnWriteArrayList<>();
    private final List<OnPreDrawListener> preDrawListeners = new CopyOnWriteArrayList<>();
    private final List<OnDrawListener> drawListeners = new CopyOnWriteArrayList<>();
    private final List<OnScrollChangedListener> scrollListeners = new CopyOnWriteArrayList<>();

    public boolean isAlive() {
        return true;
    }

    public void addOnGlobalLayoutListener(OnGlobalLayoutListener listener) {
        if (listener != null) globalLayoutListeners.add(listener);
    }

    public void removeOnGlobalLayoutListener(OnGlobalLayoutListener victim) {
        globalLayoutListeners.remove(victim);
    }

    @Deprecated
    public void removeGlobalOnLayoutListener(OnGlobalLayoutListener victim) {
        removeOnGlobalLayoutListener(victim);
    }

    public void addOnPreDrawListener(OnPreDrawListener listener) {
        if (listener != null) preDrawListeners.add(listener);
    }

    public void removeOnPreDrawListener(OnPreDrawListener victim) {
        preDrawListeners.remove(victim);
    }

    public void addOnDrawListener(OnDrawListener listener) {
        if (listener != null) drawListeners.add(listener);
    }

    public void removeOnDrawListener(OnDrawListener victim) {
        drawListeners.remove(victim);
    }

    public void addOnScrollChangedListener(OnScrollChangedListener listener) {
        if (listener != null) scrollListeners.add(listener);
    }

    public void removeOnScrollChangedListener(OnScrollChangedListener victim) {
        scrollListeners.remove(victim);
    }

    public void dispatchOnGlobalLayout() {
        for (OnGlobalLayoutListener l : globalLayoutListeners) {
            l.onGlobalLayout();
        }
    }

    public boolean dispatchOnPreDraw() {
        boolean cancel = false;
        for (OnPreDrawListener l : preDrawListeners) {
            if (!l.onPreDraw()) cancel = true;
        }
        return cancel;
    }
}
