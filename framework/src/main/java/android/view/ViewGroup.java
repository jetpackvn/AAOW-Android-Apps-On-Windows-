package android.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import dev.aaow.apk.ResValue;
import dev.aaow.apk.XmlElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp cơ sở android.view.ViewGroup chứa các View con, điều phối đo kích thước,
 * vẽ có cắt vùng (clip) và phân phối sự kiện chuột/chạm từ trên xuống.
 */
public abstract class ViewGroup extends View implements ViewManager, ViewParent {
    public interface OnHierarchyChangeListener {
        void onChildViewAdded(View parent, View child);

        void onChildViewRemoved(View parent, View child);
    }

    public static class LayoutParams {
        public static final int FILL_PARENT = -1;
        public static final int MATCH_PARENT = -1;
        public static final int WRAP_CONTENT = -2;

        public int width;
        public int height;

        public LayoutParams(Context c, AttributeSet attrs) {
            this.width = WRAP_CONTENT;
            this.height = WRAP_CONTENT;
            if (attrs instanceof AttributeSet.XmlAttributeSet xas && c != null) {
                Resources res = c.getResources();
                Resources.Theme theme = c.getTheme();
                XmlElement.Attribute w = xas.element.attr("layout_width");
                XmlElement.Attribute h = xas.element.attr("layout_height");
                if (w != null) this.width = parseSize(w.value, res, theme);
                if (h != null) this.height = parseSize(h.value, res, theme);
            }
        }

        public LayoutParams(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public LayoutParams(LayoutParams source) {
            if (source != null) {
                this.width = source.width;
                this.height = source.height;
            }
        }

        static int parseSize(ResValue v, Resources res) {
            return parseSize(v, res, null);
        }

        static int parseSize(ResValue v, Resources res, Resources.Theme theme) {
            if (v == null || res == null) return WRAP_CONTENT;
            ResValue cur = v;
            for (int depth = 0; depth < 6 && cur != null && cur.type() == ResValue.TYPE_ATTRIBUTE; depth++) {
                ResValue themed = theme != null ? theme.resolveAttributeValue(cur.data()) : null;
                if (themed == null) {
                    themed = res.resolveStyleAttrRaw(0, cur.data());
                }
                if (themed == null) break;
                cur = themed;
            }
            ResValue r = res.getTable().resolve(cur);
            if (r == null) return WRAP_CONTENT;
            if (r.isDimension()) return Math.round(res.resolveDimension(r, 0f));
            if (r.isInt()) return r.data();
            return WRAP_CONTENT;
        }
    }

    public static class MarginLayoutParams extends LayoutParams {
        public int leftMargin;
        public int topMargin;
        public int rightMargin;
        public int bottomMargin;

        public MarginLayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            if (attrs instanceof AttributeSet.XmlAttributeSet xas && c != null) {
                Resources res = c.getResources();
                for (XmlElement.Attribute a : xas.element.attributes) {
                    if (!XmlElement.ANDROID_NS.equals(a.namespace)) continue;
                    switch (a.name) {
                        case "layout_margin" -> {
                            int m = Math.round(res.resolveDimension(a.value, 0f));
                            leftMargin = topMargin = rightMargin = bottomMargin = m;
                        }
                        case "layout_marginHorizontal" -> {
                            int m = Math.round(res.resolveDimension(a.value, 0f));
                            leftMargin = rightMargin = m;
                        }
                        case "layout_marginVertical" -> {
                            int m = Math.round(res.resolveDimension(a.value, 0f));
                            topMargin = bottomMargin = m;
                        }
                        case "layout_marginLeft", "layout_marginStart" -> leftMargin = Math.round(res.resolveDimension(a.value, 0f));
                        case "layout_marginTop" -> topMargin = Math.round(res.resolveDimension(a.value, 0f));
                        case "layout_marginRight", "layout_marginEnd" -> rightMargin = Math.round(res.resolveDimension(a.value, 0f));
                        case "layout_marginBottom" -> bottomMargin = Math.round(res.resolveDimension(a.value, 0f));
                    }
                }
            }
        }

        public MarginLayoutParams(int width, int height) {
            super(width, height);
        }

        public MarginLayoutParams(MarginLayoutParams source) {
            super(source);
            if (source != null) {
                this.leftMargin = source.leftMargin;
                this.topMargin = source.topMargin;
                this.rightMargin = source.rightMargin;
                this.bottomMargin = source.bottomMargin;
            }
        }

        public MarginLayoutParams(LayoutParams source) {
            super(source);
        }

        public void setMargins(int left, int top, int right, int bottom) {
            this.leftMargin = left;
            this.topMargin = top;
            this.rightMargin = right;
            this.bottomMargin = bottom;
        }

        public int getMarginStart() {
            return leftMargin;
        }

        public int getMarginEnd() {
            return rightMargin;
        }

        public void setMarginStart(int start) {
            this.leftMargin = start;
        }

        public void setMarginEnd(int end) {
            this.rightMargin = end;
        }

        public boolean isMarginRelative() {
            return false;
        }

        public void resolveLayoutDirection(int layoutDirection) {
        }
    }

    public static final int FOCUS_BEFORE_DESCENDANTS = 0x20000;
    public static final int FOCUS_AFTER_DESCENDANTS = 0x40000;
    public static final int FOCUS_BLOCK_DESCENDANTS = 0x60000;

    protected final List<View> children = new ArrayList<>();
    private View touchTarget;
    private View focusedChild;
    private boolean clipChildren = true;
    private boolean clipToPadding = true;
    private int descendantFocusability = FOCUS_BEFORE_DESCENDANTS;
    private boolean disallowIntercept;
    private OnHierarchyChangeListener hierarchyChangeListener;

    public ViewGroup(Context context) {
        super(context);
    }

    public ViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ViewGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public int getChildCount() {
        return children.size();
    }

    public View getChildAt(int index) {
        return index >= 0 && index < children.size() ? children.get(index) : null;
    }

    public int indexOfChild(View child) {
        return children.indexOf(child);
    }

    public void addView(View child) {
        addView(child, -1);
    }

    public void addView(View child, int index) {
        if (child == null) return;
        LayoutParams params = child.getLayoutParams();
        if (params == null) {
            params = generateDefaultLayoutParams();
        }
        addView(child, index, params);
    }

    public void addView(View child, int width, int height) {
        LayoutParams params = generateDefaultLayoutParams();
        params.width = width;
        params.height = height;
        addView(child, -1, params);
    }

    @Override
    public void addView(View child, LayoutParams params) {
        addView(child, -1, params);
    }

    public void addView(View child, int index, LayoutParams params) {
        if (child == null) return;
        if (child.parent instanceof ViewGroup oldGroup) {
            oldGroup.removeView(child);
        }
        child.parent = this;
        child.setLayoutParams(checkLayoutParams(params) ? params : generateLayoutParams(params));
        if (index < 0 || index > children.size()) {
            children.add(child);
        } else {
            children.add(index, child);
        }
        if (isAttachedToWindow() && !child.isAttachedToWindow()) {
            child.dispatchAttachedToWindow();
        }
        if (hierarchyChangeListener != null) {
            hierarchyChangeListener.onChildViewAdded(this, child);
        }
        requestLayout();
    }

    @Override
    public void updateViewLayout(View view, LayoutParams params) {
        if (view != null) {
            view.setLayoutParams(params);
            requestLayout();
        }
    }

    @Override
    public void removeView(View view) {
        if (children.remove(view)) {
            if (view.isAttachedToWindow()) {
                view.dispatchDetachedFromWindow();
            }
            view.parent = null;
            if (touchTarget == view) touchTarget = null;
            if (hierarchyChangeListener != null) {
                hierarchyChangeListener.onChildViewRemoved(this, view);
            }
            requestLayout();
        }
    }

    public void removeViewAt(int index) {
        if (index >= 0 && index < children.size()) {
            View v = children.remove(index);
            if (v.isAttachedToWindow()) {
                v.dispatchDetachedFromWindow();
            }
            v.parent = null;
            if (touchTarget == v) touchTarget = null;
            if (hierarchyChangeListener != null) {
                hierarchyChangeListener.onChildViewRemoved(this, v);
            }
            requestLayout();
        }
    }

    public void removeAllViews() {
        List<View> removed = List.copyOf(children);
        children.clear();
        touchTarget = null;
        for (View v : removed) {
            if (v.isAttachedToWindow()) {
                v.dispatchDetachedFromWindow();
            }
            v.parent = null;
            if (hierarchyChangeListener != null) {
                hierarchyChangeListener.onChildViewRemoved(this, v);
            }
        }
        requestLayout();
    }

    protected void detachViewFromParent(View child) {
        if (child != null && children.remove(child)) {
            child.parent = null;
        }
    }

    protected void detachViewFromParent(int index) {
        if (index >= 0 && index < children.size()) {
            View v = children.remove(index);
            v.parent = null;
        }
    }

    protected void detachViewsFromParent(int start, int count) {
        for (int i = 0; i < count && start < children.size(); i++) {
            View v = children.remove(start);
            v.parent = null;
        }
    }

    protected void detachAllViewsFromParent() {
        for (View v : children) {
            v.parent = null;
        }
        children.clear();
    }

    protected void attachViewToParent(View child, int index, LayoutParams params) {
        if (child == null) return;
        child.parent = this;
        if (params != null) child.setLayoutParams(params);
        if (index < 0 || index > children.size()) {
            children.add(child);
        } else {
            children.add(index, child);
        }
    }

    protected void removeDetachedView(View child, boolean animate) {
        if (child != null && child.parent == this) {
            child.parent = null;
        }
        if (touchTarget == child) touchTarget = null;
    }

    protected void cleanupLayoutState(View child) {
    }

    protected boolean addViewInLayout(View child, int index, LayoutParams params) {
        return addViewInLayout(child, index, params, false);
    }

    protected boolean addViewInLayout(View child, int index, LayoutParams params, boolean preventRequestLayout) {
        if (child == null) return false;
        child.parent = this;
        child.setLayoutParams(checkLayoutParams(params) ? params : generateLayoutParams(params));
        if (index < 0 || index > children.size()) {
            children.add(child);
        } else {
            children.add(index, child);
        }
        if (!preventRequestLayout) requestLayout();
        return true;
    }

    public void removeAllViewsInLayout() {
        detachAllViewsFromParent();
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return p instanceof MarginLayoutParams mp ? new MarginLayoutParams(mp) : new MarginLayoutParams(p);
    }

    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    protected boolean checkLayoutParams(LayoutParams p) {
        return p != null;
    }

    @Override
    protected View findViewTraversal(int id) {
        if (getId() == id) return this;
        for (View child : children) {
            View v = child.findViewById(id);
            if (v != null) return v;
        }
        return null;
    }

    protected void measureChildren(int widthMeasureSpec, int heightMeasureSpec) {
        final int size = children.size();
        for (int i = 0; i < size; ++i) {
            final View child = children.get(i);
            if (child.getVisibility() != GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
            }
        }
    }

    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        LayoutParams lp = child.getLayoutParams();
        int cw = getChildMeasureSpec(parentWidthMeasureSpec, paddingLeft + paddingRight, lp.width);
        int ch = getChildMeasureSpec(parentHeightMeasureSpec, paddingTop + paddingBottom, lp.height);
        child.measure(cw, ch);
    }

    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
                                           int parentHeightMeasureSpec, int heightUsed) {
        LayoutParams lp = child.getLayoutParams();
        int ml = 0, mr = 0, mt = 0, mb = 0;
        if (lp instanceof MarginLayoutParams mlp) {
            ml = mlp.leftMargin;
            mr = mlp.rightMargin;
            mt = mlp.topMargin;
            mb = mlp.bottomMargin;
        }
        int cw = getChildMeasureSpec(parentWidthMeasureSpec,
                paddingLeft + paddingRight + ml + mr + widthUsed, lp.width);
        int ch = getChildMeasureSpec(parentHeightMeasureSpec,
                paddingTop + paddingBottom + mt + mb + heightUsed, lp.height);
        child.measure(cw, ch);
    }

    public static int getChildMeasureSpec(int spec, int padding, int childDimension) {
        int specMode = MeasureSpec.getMode(spec);
        int specSize = MeasureSpec.getSize(spec);
        int size = Math.max(0, specSize - padding);

        int resultSize = 0;
        int resultMode = 0;

        switch (specMode) {
            case MeasureSpec.EXACTLY -> {
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    resultSize = size;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    resultSize = size;
                    resultMode = MeasureSpec.AT_MOST;
                }
            }
            case MeasureSpec.AT_MOST -> {
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    resultSize = size;
                    resultMode = MeasureSpec.AT_MOST;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    resultSize = size;
                    resultMode = MeasureSpec.AT_MOST;
                }
            }
            case MeasureSpec.UNSPECIFIED -> {
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else {
                    resultSize = 0;
                    resultMode = MeasureSpec.UNSPECIFIED;
                }
            }
        }
        return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
    }

    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (child == null || child.getVisibility() != VISIBLE || child.getAlpha() <= 0f) return false;
        int cw = child.getWidth();
        int ch = child.getHeight();
        if (cw <= 0 || ch <= 0) return false;
        float dx = child.getLeft() - scrollX + child.getTranslationX();
        float dy = child.getTop() - scrollY + child.getTranslationY();
        int save = canvas.save();
        canvas.translate(dx, dy);
        if (clipChildren) {
            canvas.clipRect(0, 0, cw, ch);
        }
        child.draw(canvas);
        canvas.restoreToCount(save);
        return false;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        java.awt.Graphics2D g2 = canvas.getAwtGraphics();
        java.awt.Rectangle clipBounds = g2 != null ? g2.getClipBounds() : null;
        int selfW = getWidth();
        int selfH = getHeight();
        long drawingTime = android.os.SystemClock.uptimeMillis();
        boolean useCustomOrder = isChildrenDrawingOrderEnabled();
        for (int i = 0, n = children.size(); i < n; i++) {
            int childIndex = useCustomOrder ? getChildDrawingOrder(n, i) : i;
            if (childIndex < 0 || childIndex >= n) childIndex = i;
            View child = children.get(childIndex);
            if (child.getVisibility() != VISIBLE || child.getAlpha() <= 0f) continue;
            int cw = child.getWidth();
            int ch = child.getHeight();
            if (cw <= 0 || ch <= 0) continue;
            float dx = child.getLeft() - scrollX + child.getTranslationX();
            float dy = child.getTop() - scrollY + child.getTranslationY();
            if (clipChildren && child.getRotation() == 0f && child.getScaleX() == 1f && child.getScaleY() == 1f) {
                if (selfW > 0 && selfH > 0) {
                    if (dx + cw <= 0 || dx >= selfW || dy + ch <= 0 || dy >= selfH) continue;
                }
                if (clipBounds != null) {
                    if (dx + cw <= clipBounds.x || dx >= clipBounds.x + clipBounds.width
                            || dy + ch <= clipBounds.y || dy >= clipBounds.y + clipBounds.height) {
                        continue;
                    }
                }
            }
            drawChild(canvas, child, drawingTime);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!isEnabled() || getVisibility() != VISIBLE) return false;
        int action = ev.getActionMasked();
        float x = ev.getX() + scrollX;
        float y = ev.getY() + scrollY;

        if (action == MotionEvent.ACTION_DOWN) {
            disallowIntercept = false;
            touchTarget = null;
            boolean intercepted = onInterceptTouchEvent(ev);
            if (!intercepted) {
                for (int i = children.size() - 1; i >= 0; i--) {
                    View child = children.get(i);
                    if (child.getVisibility() != VISIBLE || !child.isEnabled()) continue;
                    float cx = x - child.getLeft() - child.getTranslationX();
                    float cy = y - child.getTop() - child.getTranslationY();
                    if (cx >= 0 && cx < child.getWidth() && cy >= 0 && cy < child.getHeight()) {
                        MotionEvent childEv = new MotionEvent(action, cx, cy, ev.getRawX(), ev.getRawY(), ev.getDownTime(), ev.getEventTime());
                        if (child.dispatchTouchEvent(childEv)) {
                            touchTarget = child;
                            return true;
                        }
                    }
                }
            }
        } else if (touchTarget != null) {
            if (!disallowIntercept && action == MotionEvent.ACTION_MOVE && onInterceptTouchEvent(ev)) {
                MotionEvent cancelEv = new MotionEvent(MotionEvent.ACTION_CANCEL, 0, 0, ev.getRawX(), ev.getRawY(), ev.getDownTime(), ev.getEventTime());
                touchTarget.dispatchTouchEvent(cancelEv);
                touchTarget = null;
                return super.dispatchTouchEvent(ev);
            }
            float cx = x - touchTarget.getLeft() - touchTarget.getTranslationX();
            float cy = y - touchTarget.getTop() - touchTarget.getTranslationY();
            MotionEvent childEv = new MotionEvent(action, cx, cy, ev.getRawX(), ev.getRawY(), ev.getDownTime(), ev.getEventTime());
            boolean handled = touchTarget.dispatchTouchEvent(childEv);
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                touchTarget = null;
            }
            return handled;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onMouseWheel(int wheelRotation, float x, float y) {
        float sx = x + scrollX;
        float sy = y + scrollY;
        for (int i = children.size() - 1; i >= 0; i--) {
            View child = children.get(i);
            if (child.getVisibility() != VISIBLE) continue;
            float cx = sx - child.getLeft();
            float cy = sy - child.getTop();
            if (cx >= 0 && cx < child.getWidth() && cy >= 0 && cy < child.getHeight()) {
                if (child.onMouseWheel(wheelRotation, cx, cy)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean isLayoutRequested() {
        return super.isLayoutRequested();
    }

    public void setOnHierarchyChangeListener(OnHierarchyChangeListener listener) {
        this.hierarchyChangeListener = listener;
    }

    public boolean getClipChildren() {
        return clipChildren;
    }

    public void setClipChildren(boolean clipChildren) {
        this.clipChildren = clipChildren;
        invalidate();
    }

    public boolean getClipToPadding() {
        return clipToPadding;
    }

    public void setClipToPadding(boolean clipToPadding) {
        this.clipToPadding = clipToPadding;
        invalidate();
    }

    public void setMotionEventSplittingEnabled(boolean split) {
    }

    public int getDescendantFocusability() {
        return descendantFocusability;
    }

    public void setDescendantFocusability(int focusability) {
        this.descendantFocusability = focusability;
    }

    @Override
    public boolean hasFocus() {
        if (super.hasFocus()) return true;
        for (View child : children) {
            if (child.hasFocus()) return true;
        }
        return false;
    }

    @Override
    public View findFocus() {
        if (isFocused()) return this;
        for (View child : children) {
            View f = child.findFocus();
            if (f != null) return f;
        }
        return null;
    }

    public View getFocusedChild() {
        return focusedChild;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public void bringChildToFront(View child) {
        int idx = children.indexOf(child);
        if (idx >= 0 && idx < children.size() - 1) {
            children.remove(idx);
            children.add(child);
            invalidate();
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        this.disallowIntercept = disallowIntercept;
        if (parent != null) parent.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public void invalidateChild(View child, Rect dirty) {
        invalidate();
    }

    @Override
    public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
        invalidate();
        return parent;
    }

    @Override
    public boolean getChildVisibleRect(View child, Rect r, Point offset) {
        if (r != null && child != null) {
            r.offset(child.getLeft() - scrollX, child.getTop() - scrollY);
        }
        if (offset != null && child != null) {
            offset.x += child.getLeft() - scrollX;
            offset.y += child.getTop() - scrollY;
        }
        return true;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        this.focusedChild = child;
        if (parent != null) parent.requestChildFocus(this, focused);
    }

    @Override
    public void clearChildFocus(View child) {
        if (this.focusedChild == child) {
            this.focusedChild = null;
        }
        if (parent != null) parent.clearChildFocus(this);
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return false;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
    }

    @Override
    public void onStopNestedScroll(View target) {
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return false;
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public void requestFitSystemWindows() {
        if (parent != null) parent.requestFitSystemWindows();
    }

    @Override
    public boolean canResolveLayoutDirection() {
        return true;
    }

    @Override
    public boolean isLayoutDirectionResolved() {
        return true;
    }

    @Override
    public boolean canResolveTextDirection() {
        return true;
    }

    @Override
    public boolean isTextDirectionResolved() {
        return true;
    }

    @Override
    public boolean canResolveTextAlignment() {
        return true;
    }

    @Override
    public boolean isTextAlignmentResolved() {
        return true;
    }

    @Override
    public void dispatchWindowFocusChanged(boolean hasFocus) {
        super.dispatchWindowFocusChanged(hasFocus);
        for (View child : List.copyOf(children)) {
            child.dispatchWindowFocusChanged(hasFocus);
        }
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        for (View child : List.copyOf(children)) {
            child.jumpDrawablesToCurrentState();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    public void dispatchAttachedToWindow() {
        super.dispatchAttachedToWindow();
        for (View child : List.copyOf(children)) {
            child.dispatchAttachedToWindow();
        }
    }

    @Override
    public void dispatchDetachedFromWindow() {
        for (View child : List.copyOf(children)) {
            child.dispatchDetachedFromWindow();
        }
        super.dispatchDetachedFromWindow();
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        super.dispatchSaveInstanceState(container);
        for (View child : children) {
            child.saveHierarchyState(container);
        }
    }

    protected void dispatchFreezeSelfOnly(SparseArray<Parcelable> container) {
        super.dispatchSaveInstanceState(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        super.dispatchRestoreInstanceState(container);
        for (View child : children) {
            child.restoreHierarchyState(container);
        }
    }

    protected void dispatchThawSelfOnly(SparseArray<Parcelable> container) {
        super.dispatchRestoreInstanceState(container);
    }

    @Override
    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
        WindowInsets res = super.dispatchApplyWindowInsets(insets);
        if (!res.isConsumed()) {
            for (View child : children) {
                child.dispatchApplyWindowInsets(res);
            }
        }
        return res;
    }

    private boolean addStatesFromChildren;
    private int transientStateCount;
    private boolean childrenDrawingOrderEnabled;
    private android.animation.LayoutTransition layoutTransition;

    public android.animation.LayoutTransition getLayoutTransition() {
        return layoutTransition;
    }

    public void setLayoutTransition(android.animation.LayoutTransition transition) {
        this.layoutTransition = transition;
    }

    @Override
    public void focusableViewAvailable(View v) {
        if (parent != null) {
            parent.focusableViewAvailable(v);
        }
    }

    protected boolean isChildrenDrawingOrderEnabled() {
        return childrenDrawingOrderEnabled;
    }

    protected void setChildrenDrawingOrderEnabled(boolean enabled) {
        this.childrenDrawingOrderEnabled = enabled;
    }

    protected int getChildDrawingOrder(int childCount, int drawingPosition) {
        return drawingPosition;
    }

    public boolean addStatesFromChildren() {
        return addStatesFromChildren;
    }

    public void setAddStatesFromChildren(boolean addsStates) {
        this.addStatesFromChildren = addsStates;
    }

    @Override
    public void childHasTransientStateChanged(View child, boolean hasTransientState) {
        boolean old = hasTransientState();
        if (hasTransientState) {
            transientStateCount++;
        } else if (transientStateCount > 0) {
            transientStateCount--;
        }
        boolean now = hasTransientState();
        if (old != now && getParent() != null) {
            getParent().childHasTransientStateChanged(this, now);
        }
    }

    @Override
    public boolean hasTransientState() {
        return transientStateCount > 0 || super.hasTransientState();
    }

    @Override
    public ViewGroupOverlay getOverlay() {
        if (!(overlay instanceof ViewGroupOverlay)) {
            overlay = new ViewGroupOverlay(getContext(), this);
        }
        return (ViewGroupOverlay) overlay;
    }

    public void startViewTransition(View view) {
    }

    public void endViewTransition(View view) {
    }

    public final void offsetDescendantRectToMyCoords(View descendant, Rect rect) {
        if (descendant == null || rect == null) return;
        View cur = descendant;
        while (cur != null && cur != this) {
            rect.offset(cur.getLeft() - cur.getScrollX(), cur.getTop() - cur.getScrollY());
            ViewParent p = cur.getParent();
            cur = p instanceof View pv ? pv : null;
        }
    }

    public final void offsetRectIntoDescendantCoords(View descendant, Rect rect) {
        if (descendant == null || rect == null) return;
        View cur = descendant;
        while (cur != null && cur != this) {
            rect.offset(-(cur.getLeft() - cur.getScrollX()), -(cur.getTop() - cur.getScrollY()));
            ViewParent p = cur.getParent();
            cur = p instanceof View pv ? pv : null;
        }
    }
}
