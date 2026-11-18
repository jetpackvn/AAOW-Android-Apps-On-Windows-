package android.view.accessibility;

public final class AccessibilityManager {
    private static final AccessibilityManager INSTANCE = new AccessibilityManager();

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
}
