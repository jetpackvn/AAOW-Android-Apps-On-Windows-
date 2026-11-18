package android.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import java.util.function.Consumer;

public class PopupWindow {
    public interface OnDismissListener {
        void onDismiss();
    }

    public static final int INPUT_METHOD_FROM_FOCUSABLE = 0;
    public static final int INPUT_METHOD_NEEDED = 1;
    public static final int INPUT_METHOD_NOT_NEEDED = 2;

    private static Consumer<PopupWindow> hostPopupBridge;

    private View contentView;
    private int width = -2;
    private int height = -2;
    private boolean focusable;
    private boolean touchable = true;
    private boolean outsideTouchable;
    private boolean clippingEnabled = true;
    private boolean showing;
    private Drawable background;
    private float elevation;
    private int anchorX;
    private int anchorY;
    private OnDismissListener onDismissListener;

    public PopupWindow() {
    }

    public PopupWindow(Context context) {
    }

    public PopupWindow(Context context, AttributeSet attrs) {
    }

    public PopupWindow(Context context, AttributeSet attrs, int defStyleAttr) {
    }

    public PopupWindow(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    }

    public PopupWindow(View contentView) {
        this(contentView, -2, -2, false);
    }

    public PopupWindow(int width, int height) {
        this(null, width, height, false);
    }

    public PopupWindow(View contentView, int width, int height) {
        this(contentView, width, height, false);
    }

    public PopupWindow(View contentView, int width, int height, boolean focusable) {
        this.contentView = contentView;
        this.width = width;
        this.height = height;
        this.focusable = focusable;
    }

    public static void setHostPopupBridge(Consumer<PopupWindow> bridge) {
        hostPopupBridge = bridge;
    }

    public void setContentView(View contentView) {
        this.contentView = contentView;
    }

    public View getContentView() {
        return contentView;
    }

    public void setBackgroundDrawable(Drawable background) {
        this.background = background;
    }

    public Drawable getBackground() {
        return background;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getWidth() {
        return width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    public boolean isFocusable() {
        return focusable;
    }

    public void setTouchable(boolean touchable) {
        this.touchable = touchable;
    }

    public boolean isTouchable() {
        return touchable;
    }

    public void setOutsideTouchable(boolean touchable) {
        this.outsideTouchable = touchable;
    }

    public boolean isOutsideTouchable() {
        return outsideTouchable;
    }

    public void setClippingEnabled(boolean enabled) {
        this.clippingEnabled = enabled;
    }

    public boolean isClippingEnabled() {
        return clippingEnabled;
    }

    public void setElevation(float elevation) {
        this.elevation = elevation;
    }

    public float getElevation() {
        return elevation;
    }

    public void setAnimationStyle(int animationStyle) {
    }

    public int getAnimationStyle() {
        return -1;
    }

    public void setInputMethodMode(int mode) {
    }

    public int getInputMethodMode() {
        return INPUT_METHOD_FROM_FOCUSABLE;
    }

    public void setTouchInterceptor(View.OnTouchListener l) {
    }

    public void setOnDismissListener(OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    public void setOverlapAnchor(boolean overlapAnchor) {
    }

    public boolean getOverlapAnchor() {
        return false;
    }

    public int getAnchorX() {
        return anchorX;
    }

    public int getAnchorY() {
        return anchorY;
    }

    public boolean isShowing() {
        return showing;
    }

    public void showAsDropDown(View anchor) {
        showAsDropDown(anchor, 0, 0);
    }

    public void showAsDropDown(View anchor, int xoff, int yoff) {
        showAsDropDown(anchor, xoff, yoff, 0);
    }

    public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
        int[] loc = new int[2];
        if (anchor != null) {
            anchor.getLocationInWindow(loc);
            this.anchorX = loc[0] + xoff;
            this.anchorY = loc[1] + anchor.getHeight() + yoff;
        } else {
            this.anchorX = xoff;
            this.anchorY = yoff;
        }
        this.showing = true;
        if (hostPopupBridge != null) hostPopupBridge.accept(this);
    }

    public void showAtLocation(View parent, int gravity, int x, int y) {
        this.anchorX = x;
        this.anchorY = y;
        this.showing = true;
        if (hostPopupBridge != null) hostPopupBridge.accept(this);
    }

    public void update() {
        if (showing && hostPopupBridge != null) hostPopupBridge.accept(this);
    }

    public void update(int width, int height) {
        this.width = width;
        this.height = height;
        update();
    }

    public void update(int x, int y, int width, int height) {
        update(x, y, width, height, false);
    }

    public void update(int x, int y, int width, int height, boolean force) {
        this.anchorX = x;
        this.anchorY = y;
        this.width = width;
        this.height = height;
        update();
    }

    public void update(View anchor, int width, int height) {
        update(anchor, 0, 0, width, height);
    }

    public void update(View anchor, int xoff, int yoff, int width, int height) {
        this.width = width;
        this.height = height;
        showAsDropDown(anchor, xoff, yoff);
    }

    public void dismiss() {
        if (!showing) return;
        showing = false;
        if (hostPopupBridge != null) hostPopupBridge.accept(null);
        if (onDismissListener != null) onDismissListener.onDismiss();
    }

    public int getMaxAvailableHeight(View anchor) {
        return 480;
    }

    public int getMaxAvailableHeight(View anchor, int yOffset) {
        return 480;
    }

    public int getMaxAvailableHeight(View anchor, int yOffset, boolean ignoreBottomDecorations) {
        return 480;
    }
}
