package android.view;

import android.graphics.Point;
import android.graphics.Rect;
import android.window.OnBackInvokedDispatcher;

public interface ViewParent {
    void requestLayout();

    boolean isLayoutRequested();

    ViewParent getParent();

    void bringChildToFront(View child);

    void requestDisallowInterceptTouchEvent(boolean disallowIntercept);

    void invalidateChild(View child, Rect dirty);

    ViewParent invalidateChildInParent(int[] location, Rect dirty);

    default void onDescendantInvalidated(View child, View target) {
        if (getParent() != null) {
            getParent().onDescendantInvalidated(child, target);
        }
    }

    boolean getChildVisibleRect(View child, Rect r, Point offset);

    void requestChildFocus(View child, View focused);

    void clearChildFocus(View child);

    boolean onStartNestedScroll(View child, View target, int nestedScrollAxes);

    void onNestedScrollAccepted(View child, View target, int nestedScrollAxes);

    void onStopNestedScroll(View target);

    void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed);

    void onNestedPreScroll(View target, int dx, int dy, int[] consumed);

    boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed);

    boolean onNestedPreFling(View target, float velocityX, float velocityY);

    void requestFitSystemWindows();

    boolean canResolveLayoutDirection();

    boolean isLayoutDirectionResolved();

    int getLayoutDirection();

    boolean canResolveTextDirection();

    boolean isTextDirectionResolved();

    int getTextDirection();

    boolean canResolveTextAlignment();

    boolean isTextAlignmentResolved();

    int getTextAlignment();

    default void childHasTransientStateChanged(View child, boolean hasTransientState) {
    }

    default OnBackInvokedDispatcher findOnBackInvokedDispatcherForChild(View child, View requester) {
        ViewParent p = getParent();
        return p != null ? p.findOnBackInvokedDispatcherForChild(child, requester) : null;
    }
}
