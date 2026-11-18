package android.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import dev.aaow.apk.ResValue;
import dev.aaow.apk.XmlElement;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lớp cơ sở android.view.View chạy trên Windows: quản lý đo kích thước (measure),
 * bố cục (layout), vẽ bằng Java2D (draw), và xử lý chuột/bàn phím.
 */
public class View {
    public static final int NO_ID = -1;
    public static final int VISIBLE = 0x00000000;
    public static final int INVISIBLE = 0x00000004;
    public static final int GONE = 0x00000008;

    private static final AtomicInteger NEXT_GENERATED_ID = new AtomicInteger(1);

    public static class MeasureSpec {
        private static final int MODE_SHIFT = 30;
        private static final int MODE_MASK = 0x3 << MODE_SHIFT;
        public static final int UNSPECIFIED = 0 << MODE_SHIFT;
        public static final int EXACTLY = 1 << MODE_SHIFT;
        public static final int AT_MOST = 2 << MODE_SHIFT;

        public static int makeMeasureSpec(int size, int mode) {
            return (size & ~MODE_MASK) | (mode & MODE_MASK);
        }

        public static int getMode(int measureSpec) {
            return measureSpec & MODE_MASK;
        }

        public static int getSize(int measureSpec) {
            return measureSpec & ~MODE_MASK;
        }

        public static String toString(int measureSpec) {
            int mode = getMode(measureSpec);
            int size = getSize(measureSpec);
            return switch (mode) {
                case UNSPECIFIED -> "UNSPECIFIED(" + size + ")";
                case EXACTLY -> "EXACTLY(" + size + ")";
                case AT_MOST -> "AT_MOST(" + size + ")";
                default -> size + "";
            };
        }
    }

    public interface OnClickListener {
        void onClick(View v);
    }

    public interface OnLongClickListener {
        boolean onLongClick(View v);
    }

    public interface OnTouchListener {
        boolean onTouch(View v, MotionEvent event);
    }

    public interface OnKeyListener {
        boolean onKey(View v, int keyCode, KeyEvent event);
    }

    public interface OnFocusChangeListener {
        void onFocusChange(View v, boolean hasFocus);
    }

    protected final Context context;
    ViewParent parent;
    private ViewGroup.LayoutParams layoutParams;

    private int id = NO_ID;
    private Object tag;
    private int visibility = VISIBLE;
    private boolean enabled = true;
    private boolean clickable;
    private boolean focusable;
    private boolean focused;
    private boolean pressed;
    private float alpha = 1.0f;
    private float elevation;
    private float translationX;
    private float translationY;
    private float rotation;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;

    protected int paddingLeft;
    protected int paddingTop;
    protected int paddingRight;
    protected int paddingBottom;
    private int minWidth;
    private int minHeight;

    protected int measuredWidth;
    protected int measuredHeight;
    protected int left;
    protected int top;
    protected int right;
    protected int bottom;
    protected int scrollX;
    protected int scrollY;

    private Drawable background;
    protected OnClickListener onClickListener;
    protected OnLongClickListener onLongClickListener;
    protected OnTouchListener onTouchListener;
    protected OnKeyListener onKeyListener;
    protected OnFocusChangeListener onFocusChangeListener;
    private Runnable invalidateCallback;

    public View(Context context) {
        this(context, null, 0, 0);
    }

    public View(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public View(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public View(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this.context = context;
        if (attrs instanceof AttributeSet.XmlAttributeSet xas) {
            applyXmlAttributes(xas.element, context != null ? context.getResources() : null);
        }
    }

    protected void applyXmlAttributes(XmlElement el, Resources res) {
        if (el == null || res == null) return;
        for (XmlElement.Attribute a : el.attributes) {
            if (!XmlElement.ANDROID_NS.equals(a.namespace)) continue;
            ResValue v = a.value;
            switch (a.name) {
                case "id" -> {
                    if (v.isReference()) this.id = v.data();
                }
                case "tag" -> this.tag = res.resolveString(v);
                case "visibility" -> {
                    int val = res.getTable().resolve(v).data();
                    if (val == 1 || val == INVISIBLE) this.visibility = INVISIBLE;
                    else if (val == 2 || val == GONE) this.visibility = GONE;
                    else this.visibility = VISIBLE;
                }
                case "background" -> {
                    ResValue rv = res.getTable().resolve(v);
                    if (rv != null && rv.isColor()) {
                        this.background = new ColorDrawable(rv.data());
                    } else if (v.isReference()) {
                        Drawable d = res.getDrawable(v.data());
                        if (d != null) this.background = d;
                    }
                }
                case "padding" -> {
                    int p = Math.round(res.resolveDimension(v, 0f));
                    setPadding(p, p, p, p);
                }
                case "paddingHorizontal" -> {
                    int p = Math.round(res.resolveDimension(v, 0f));
                    this.paddingLeft = this.paddingRight = p;
                }
                case "paddingVertical" -> {
                    int p = Math.round(res.resolveDimension(v, 0f));
                    this.paddingTop = this.paddingBottom = p;
                }
                case "paddingLeft", "paddingStart" -> this.paddingLeft = Math.round(res.resolveDimension(v, 0f));
                case "paddingTop" -> this.paddingTop = Math.round(res.resolveDimension(v, 0f));
                case "paddingRight", "paddingEnd" -> this.paddingRight = Math.round(res.resolveDimension(v, 0f));
                case "paddingBottom" -> this.paddingBottom = Math.round(res.resolveDimension(v, 0f));
                case "minWidth" -> this.minWidth = Math.round(res.resolveDimension(v, 0f));
                case "minHeight" -> this.minHeight = Math.round(res.resolveDimension(v, 0f));
                case "clickable" -> this.clickable = res.getTable().resolve(v).data() != 0;
                case "enabled" -> this.enabled = res.getTable().resolve(v).data() != 0;
                case "alpha" -> {
                    ResValue rv = res.getTable().resolve(v);
                    if (rv != null) this.alpha = rv.type() == ResValue.TYPE_FLOAT ? rv.getFloat() : 1f;
                }
                case "elevation" -> this.elevation = res.resolveDimension(v, 0f);
            }
        }
    }

    public static int generateViewId() {
        return NEXT_GENERATED_ID.getAndIncrement();
    }

    public final Context getContext() {
        return context;
    }

    public Resources getResources() {
        return context != null ? context.getResources() : null;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Object getTag() {
        return tag;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public final ViewParent getParent() {
        return parent;
    }

    public ViewGroup.LayoutParams getLayoutParams() {
        return layoutParams;
    }

    public void setLayoutParams(ViewGroup.LayoutParams params) {
        this.layoutParams = params;
        requestLayout();
    }

    public int getVisibility() {
        return visibility;
    }

    public void setVisibility(int visibility) {
        if (this.visibility != visibility) {
            this.visibility = visibility;
            requestLayout();
            invalidate();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        invalidate();
    }

    public boolean isClickable() {
        return clickable || onClickListener != null;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }

    public boolean isFocusable() {
        return focusable;
    }

    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocusedInternal(boolean focused) {
        if (this.focused != focused) {
            this.focused = focused;
            if (onFocusChangeListener != null) {
                onFocusChangeListener.onFocusChange(this, focused);
            }
            invalidate();
        }
    }

    public boolean isPressed() {
        return pressed;
    }

    public void setPressed(boolean pressed) {
        if (this.pressed != pressed) {
            this.pressed = pressed;
            invalidate();
        }
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = Math.max(0f, Math.min(1f, alpha));
        invalidate();
    }

    public float getElevation() {
        return elevation;
    }

    public void setElevation(float elevation) {
        this.elevation = elevation;
        invalidate();
    }

    public float getTranslationX() {
        return translationX;
    }

    public void setTranslationX(float translationX) {
        this.translationX = translationX;
        invalidate();
    }

    public float getTranslationY() {
        return translationY;
    }

    public void setTranslationY(float translationY) {
        this.translationY = translationY;
        invalidate();
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
        invalidate();
    }

    public float getScaleX() {
        return scaleX;
    }

    public void setScaleX(float scaleX) {
        this.scaleX = scaleX;
        invalidate();
    }

    public float getScaleY() {
        return scaleY;
    }

    public void setScaleY(float scaleY) {
        this.scaleY = scaleY;
        invalidate();
    }

    public void setPadding(int left, int top, int right, int bottom) {
        this.paddingLeft = left;
        this.paddingTop = top;
        this.paddingRight = right;
        this.paddingBottom = bottom;
        requestLayout();
    }

    public int getPaddingLeft() {
        return paddingLeft;
    }

    public int getPaddingTop() {
        return paddingTop;
    }

    public int getPaddingRight() {
        return paddingRight;
    }

    public int getPaddingBottom() {
        return paddingBottom;
    }

    public int getPaddingStart() {
        return paddingLeft;
    }

    public int getPaddingEnd() {
        return paddingRight;
    }

    public int getMinimumWidth() {
        return minWidth;
    }

    public void setMinimumWidth(int minWidth) {
        this.minWidth = minWidth;
        requestLayout();
    }

    public int getMinimumHeight() {
        return minHeight;
    }

    public void setMinimumHeight(int minHeight) {
        this.minHeight = minHeight;
        requestLayout();
    }

    public Drawable getBackground() {
        return background;
    }

    public void setBackground(Drawable background) {
        this.background = background;
        invalidate();
    }

    public void setBackgroundDrawable(Drawable background) {
        setBackground(background);
    }

    public void setBackgroundColor(int color) {
        this.background = new ColorDrawable(color);
        invalidate();
    }

    public void setBackgroundResource(int resid) {
        if (getResources() != null) {
            setBackground(getResources().getDrawable(resid));
        }
    }

    public void setOnClickListener(OnClickListener l) {
        this.onClickListener = l;
        if (l != null) this.clickable = true;
    }

    public boolean hasOnClickListeners() {
        return onClickListener != null;
    }

    public void setOnLongClickListener(OnLongClickListener l) {
        this.onLongClickListener = l;
    }

    public void setOnTouchListener(OnTouchListener l) {
        this.onTouchListener = l;
    }

    public void setOnKeyListener(OnKeyListener l) {
        this.onKeyListener = l;
    }

    public void setOnFocusChangeListener(OnFocusChangeListener l) {
        this.onFocusChangeListener = l;
    }

    public boolean performClick() {
        if (onClickListener != null && enabled) {
            onClickListener.onClick(this);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public final <T extends View> T findViewById(int id) {
        if (id == NO_ID) return null;
        return (T) findViewTraversal(id);
    }

    protected View findViewTraversal(int id) {
        return this.id == id ? this : null;
    }

    // --------------------------------------------------------- Measure & Layout

    public final int getMeasuredWidth() {
        return measuredWidth;
    }

    public final int getMeasuredHeight() {
        return measuredHeight;
    }

    public final void setMeasuredDimension(int measuredWidth, int measuredHeight) {
        this.measuredWidth = Math.max(0, measuredWidth);
        this.measuredHeight = Math.max(0, measuredHeight);
    }

    public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
        onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(
                getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec)
        );
    }

    protected int getSuggestedMinimumWidth() {
        int bgW = background != null ? background.getIntrinsicWidth() : 0;
        return Math.max(minWidth, bgW);
    }

    protected int getSuggestedMinimumHeight() {
        int bgH = background != null ? background.getIntrinsicHeight() : 0;
        return Math.max(minHeight, bgH);
    }

    public static int getDefaultSize(int size, int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        return switch (specMode) {
            case MeasureSpec.AT_MOST, MeasureSpec.EXACTLY -> specSize;
            default -> size;
        };
    }

    public static int resolveSize(int size, int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        return switch (specMode) {
            case MeasureSpec.AT_MOST -> Math.min(size, specSize);
            case MeasureSpec.EXACTLY -> specSize;
            default -> size;
        };
    }

    public final int getLeft() {
        return left;
    }

    public final int getTop() {
        return top;
    }

    public final int getRight() {
        return right;
    }

    public final int getBottom() {
        return bottom;
    }

    public final int getWidth() {
        return right - left;
    }

    public final int getHeight() {
        return bottom - top;
    }

    public final float getX() {
        return left + translationX;
    }

    public final float getY() {
        return top + translationY;
    }

    public final int getScrollX() {
        return scrollX;
    }

    public final int getScrollY() {
        return scrollY;
    }

    public void scrollTo(int x, int y) {
        if (scrollX != x || scrollY != y) {
            scrollX = x;
            scrollY = y;
            invalidate();
        }
    }

    public void scrollBy(int x, int y) {
        scrollTo(scrollX + x, scrollY + y);
    }

    public void layout(int l, int t, int r, int b) {
        int oldW = right - left;
        int oldH = bottom - top;
        boolean changed = (left != l || top != t || right != r || bottom != b);
        left = l;
        top = t;
        right = r;
        bottom = b;
        int newW = r - l;
        int newH = b - t;
        if (newW != oldW || newH != oldH) {
            onSizeChanged(newW, newH, oldW, oldH);
        }
        onLayout(changed, l, t, r, b);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    }

    // --------------------------------------------------------- Draw & Invalidate

    public void draw(Canvas canvas) {
        if (visibility != VISIBLE || alpha <= 0f) return;
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (background != null) {
            background.setBounds(0, 0, w, h);
            background.draw(canvas);
        }
        onDraw(canvas);
        dispatchDraw(canvas);

        if (pressed && isClickable() && !(this instanceof ViewGroup)) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.argb(28, 0, 0, 0));
            canvas.drawRoundRect(0, 0, w, h, 8, 8, p);
        }
    }

    protected void onDraw(Canvas canvas) {
    }

    protected void dispatchDraw(Canvas canvas) {
    }

    public void setInvalidateCallback(Runnable callback) {
        this.invalidateCallback = callback;
    }

    public void invalidate() {
        if (invalidateCallback != null) {
            invalidateCallback.run();
        } else if (parent instanceof View pv) {
            pv.invalidate();
        }
    }

    public void postInvalidate() {
        post(this::invalidate);
    }

    public void requestLayout() {
        if (parent != null) {
            parent.requestLayout();
        }
        invalidate();
    }

    public boolean post(Runnable action) {
        new Handler().post(action);
        return true;
    }

    public boolean postDelayed(Runnable action, long delayMillis) {
        new Handler().postDelayed(action, delayMillis);
        return true;
    }

    // --------------------------------------------------------- Touch & Keyboard

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!enabled || visibility != VISIBLE) return false;
        if (onTouchListener != null && onTouchListener.onTouch(this, event)) {
            return true;
        }
        return onTouchEvent(event);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!isClickable() && !focusable) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                setPressed(true);
                return true;
            }
            case MotionEvent.ACTION_MOVE -> {
                boolean inside = event.getX() >= 0 && event.getX() < getWidth()
                        && event.getY() >= 0 && event.getY() < getHeight();
                setPressed(inside);
                return true;
            }
            case MotionEvent.ACTION_UP -> {
                boolean wasPressed = isPressed();
                setPressed(false);
                if (wasPressed) {
                    performClick();
                }
                return true;
            }
            case MotionEvent.ACTION_CANCEL -> {
                setPressed(false);
                return true;
            }
        }
        return true;
    }

    public boolean onMouseWheel(int wheelRotation, float x, float y) {
        return false;
    }

    public void onHostKeyTyped(char ch) {
    }

    public void onHostKeyPressed(java.awt.event.KeyEvent e) {
    }
}
