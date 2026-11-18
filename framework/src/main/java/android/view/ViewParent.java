package android.view;

public interface ViewParent {
    void requestLayout();
    boolean isLayoutRequested();
    ViewParent getParent();
}
