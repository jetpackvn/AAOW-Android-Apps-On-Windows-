package android.view.accessibility;

import android.os.Handler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AccessibilityManager {
    public interface AccessibilityStateChangeListener {
        void onAccessibilityStateChanged(boolean enabled);
    }

    public interface TouchExplorationStateChangeListener {
        void onTouchExplorationStateChanged(boolean enabled);
    }

    private static final AccessibilityManager INSTANCE = new AccessibilityManager();
    private final List<AccessibilityStateChangeListener> stateListeners = new CopyOnWriteArrayList<>();
    private final List<TouchExplorationStateChangeListener> touchListeners = new CopyOnWriteArrayList<>();

    public AccessibilityManager() {
    }

    public static AccessibilityManager getInstance() {
        return INSTANCE;
    }

    public boolean isEnabled() {
        return false;
    }

    public boolean isTouchExplorationEnabled() {
        return false;
    }

    public void sendAccessibilityEvent(AccessibilityEvent event) {
    }

    public boolean addAccessibilityStateChangeListener(AccessibilityStateChangeListener listener) {
        if (listener == null) return false;
        if (!stateListeners.contains(listener)) stateListeners.add(listener);
        return true;
    }

    public void addAccessibilityStateChangeListener(AccessibilityStateChangeListener listener, Handler handler) {
        addAccessibilityStateChangeListener(listener);
    }

    public boolean removeAccessibilityStateChangeListener(AccessibilityStateChangeListener listener) {
        return listener != null && stateListeners.remove(listener);
    }

    public boolean addTouchExplorationStateChangeListener(TouchExplorationStateChangeListener listener) {
        if (listener == null) return false;
        if (!touchListeners.contains(listener)) touchListeners.add(listener);
        return true;
    }

    public void addTouchExplorationStateChangeListener(TouchExplorationStateChangeListener listener, Handler handler) {
        addTouchExplorationStateChangeListener(listener);
    }

    public boolean removeTouchExplorationStateChangeListener(TouchExplorationStateChangeListener listener) {
        return listener != null && touchListeners.remove(listener);
    }
}
