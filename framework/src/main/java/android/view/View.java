package android.view;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.app.Activity;
import android.window.OnBackInvokedDispatcher;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Property;
import android.util.SparseArray;
import dev.aaow.apk.ResValue;
import dev.aaow.apk.XmlElement;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lớp cơ sở android.view.View chạy trên Windows: quản lý đo kích thước
 * (measure),
 * bố cục (layout), vẽ bằng Java2D (draw), và xử lý chuột/bàn phím.
 */
public class View implements Drawable.Callback, KeyEvent.Callback {
    public static final int NO_ID = -1;
    public static final int VISIBLE = 0x00000000;
    public static final int INVISIBLE = 0x00000004;
    public static final int GONE = 0x00000008;
    public static final int LAYOUT_DIRECTION_LTR = 0;
    public static final int LAYOUT_DIRECTION_RTL = 1;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_AUTO = 0;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_YES = 1;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_NO = 2;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS = 4;
    public static final int IMPORTANT_FOR_AUTOFILL_AUTO = 0;
    public static final int IMPORTANT_FOR_AUTOFILL_YES = 1;
    public static final int IMPORTANT_FOR_AUTOFILL_NO = 2;
    public static final int IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS = 4;
    public static final int IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS = 8;
    public static final int ACCESSIBILITY_LIVE_REGION_NONE = 0;
    public static final int ACCESSIBILITY_LIVE_REGION_POLITE = 1;
    public static final int ACCESSIBILITY_LIVE_REGION_ASSERTIVE = 2;

    public static final Property<View, Float> ALPHA = floatProp("alpha", View::getAlpha, View::setAlpha);
    public static final Property<View, Float> TRANSLATION_X = floatProp("translationX", View::getTranslationX,
            View::setTranslationX);
    public static final Property<View, Float> TRANSLATION_Y = floatProp("translationY", View::getTranslationY,
            View::setTranslationY);
    public static final Property<View, Float> TRANSLATION_Z = floatProp("translationZ", View::getTranslationZ,
            View::setTranslationZ);
    public static final Property<View, Float> X = floatProp("x", View::getX, View::setX);
    public static final Property<View, Float> Y = floatProp("y", View::getY, View::setY);
    public static final Property<View, Float> Z = floatProp("z", View::getZ, View::setZ);
    public static final Property<View, Float> ROTATION = floatProp("rotation", View::getRotation, View::setRotation);
    public static final Property<View, Float> ROTATION_X = floatProp("rotationX", View::getRotationX,
            View::setRotationX);
    public static final Property<View, Float> ROTATION_Y = floatProp("rotationY", View::getRotationY,
            View::setRotationY);
    public static final Property<View, Float> SCALE_X = floatProp("scaleX", View::getScaleX, View::setScaleX);
    public static final Property<View, Float> SCALE_Y = floatProp("scaleY", View::getScaleY, View::setScaleY);

    private static Property<View, Float> floatProp(
            String name,
            java.util.function.Function<View, Float> getter,
            java.util.function.BiConsumer<View, Float> setter) {
        return new Property<>(Float.class, name) {
            @Override
            public void set(View object, Float value) {
                if (object != null && value != null)
                    setter.accept(object, value);
            }

            @Override
            public Float get(View object) {
                return object != null ? getter.apply(object) : 0f;
            }
        };
    }

    public static final int MEASURED_SIZE_MASK = 0x00ffffff;
    public static final int MEASURED_STATE_MASK = 0xff000000;
    public static final int MEASURED_HEIGHT_STATE_SHIFT = 16;
    public static final int MEASURED_STATE_TOO_SMALL = 0x01000000;
    public static final int OVER_SCROLL_ALWAYS = 0;
    public static final int OVER_SCROLL_IF_CONTENT_SCROLLS = 1;
    public static final int OVER_SCROLL_NEVER = 2;
    public static final int LAYER_TYPE_NONE = 0;
    public static final int LAYER_TYPE_SOFTWARE = 1;
    public static final int LAYER_TYPE_HARDWARE = 2;
    public static final int FOCUS_BACKWARD = 0x00000001;
    public static final int FOCUS_FORWARD = 0x00000002;
    public static final int FOCUS_LEFT = 0x00000011;
    public static final int FOCUS_UP = 0x00000021;
    public static final int FOCUS_RIGHT = 0x00000042;
    public static final int FOCUS_DOWN = 0x00000082;
    public static final int TEXT_DIRECTION_INHERIT = 0;
    public static final int TEXT_DIRECTION_FIRST_STRONG = 1;
    public static final int TEXT_DIRECTION_LTR = 3;
    public static final int TEXT_DIRECTION_RTL = 4;
    public static final int TEXT_ALIGNMENT_INHERIT = 0;
    public static final int TEXT_ALIGNMENT_GRAVITY = 1;
    public static final int TEXT_ALIGNMENT_CENTER = 4;
    public static final int SYSTEM_UI_FLAG_VISIBLE = 0;
    public static final int SYSTEM_UI_FLAG_LOW_PROFILE = 0x00000001;
    public static final int SYSTEM_UI_FLAG_HIDE_NAVIGATION = 0x00000002;
    public static final int SYSTEM_UI_FLAG_FULLSCREEN = 0x00000004;
    public static final int SYSTEM_UI_FLAG_LAYOUT_STABLE = 0x00000100;
    public static final int SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION = 0x00000200;
    public static final int SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN = 0x00000400;
    public static final int SYSTEM_UI_FLAG_IMMERSIVE = 0x00000800;
    public static final int SYSTEM_UI_FLAG_IMMERSIVE_STICKY = 0x00001000;
    public static final int SYSTEM_UI_FLAG_LIGHT_STATUS_BAR = 0x00002000;
    public static final int SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR = 0x00000010;
    public static final int SCROLL_INDICATOR_TOP = 0x1;
    public static final int SCROLL_INDICATOR_BOTTOM = 0x2;
    public static final int SCROLL_INDICATOR_LEFT = 0x4;
    public static final int SCROLL_INDICATOR_RIGHT = 0x8;
    public static final int SCROLL_INDICATOR_START = 0x10;
    public static final int SCROLL_INDICATOR_END = 0x20;

    protected static final int[] EMPTY_STATE_SET = new int[0];
    protected static final int[] ENABLED_STATE_SET = new int[] { 0x0101009e };
    protected static final int[] FOCUSED_STATE_SET = new int[] { 0x0101009c };
    protected static final int[] SELECTED_STATE_SET = new int[] { 0x010100a1 };
    protected static final int[] PRESSED_STATE_SET = new int[] { 0x010100a7 };
    protected static final int[] WINDOW_FOCUSED_STATE_SET = new int[] { 0x0101009d };
    protected static final int[] ENABLED_FOCUSED_STATE_SET = new int[] { 0x0101009e, 0x0101009c };
    protected static final int[] ENABLED_SELECTED_STATE_SET = new int[] { 0x0101009e, 0x010100a1 };
    protected static final int[] ENABLED_WINDOW_FOCUSED_STATE_SET = new int[] { 0x0101009e, 0x0101009d };
    protected static final int[] FOCUSED_SELECTED_STATE_SET = new int[] { 0x0101009c, 0x010100a1 };
    protected static final int[] FOCUSED_WINDOW_FOCUSED_STATE_SET = new int[] { 0x0101009c, 0x0101009d };
    protected static final int[] SELECTED_WINDOW_FOCUSED_STATE_SET = new int[] { 0x010100a1, 0x0101009d };
    protected static final int[] ENABLED_FOCUSED_SELECTED_STATE_SET = new int[] { 0x0101009e, 0x0101009c, 0x010100a1 };
    protected static final int[] ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = new int[] { 0x0101009e, 0x0101009c, 0x0101009d };
    protected static final int[] ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = new int[] { 0x0101009e, 0x010100a1, 0x0101009d };
    protected static final int[] ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = new int[] { 0x0101009e, 0x0101009c, 0x010100a1, 0x0101009d };
    protected static final int[] PRESSED_ENABLED_STATE_SET = new int[] { 0x010100a7, 0x0101009e };
    protected static final int[] PRESSED_FOCUSED_STATE_SET = new int[] { 0x010100a7, 0x0101009c };
    protected static final int[] PRESSED_SELECTED_STATE_SET = new int[] { 0x010100a7, 0x010100a1 };
    protected static final int[] PRESSED_WINDOW_FOCUSED_STATE_SET = new int[] { 0x010100a7, 0x0101009d };
    protected static final int[] PRESSED_ENABLED_FOCUSED_STATE_SET = new int[] { 0x010100a7, 0x0101009e, 0x0101009c };
    protected static final int[] PRESSED_ENABLED_SELECTED_STATE_SET = new int[] { 0x010100a7, 0x0101009e, 0x010100a1 };
    protected static final int[] PRESSED_ENABLED_WINDOW_FOCUSED_STATE_SET = new int[] { 0x010100a7, 0x0101009e, 0x0101009d };
    protected static final int[] PRESSED_FOCUSED_SELECTED_STATE_SET = new int[] { 0x010100a7, 0x0101009c, 0x010100a1 };
    protected static final int[] PRESSED_FOCUSED_WINDOW_FOCUSED_STATE_SET = new int[] { 0x010100a7, 0x0101009c, 0x0101009d };
    protected static final int[] PRESSED_SELECTED_WINDOW_FOCUSED_STATE_SET = new int[] { 0x010100a7, 0x010100a1, 0x0101009d };
    protected static final int[] PRESSED_ENABLED_FOCUSED_SELECTED_STATE_SET = new int[] { 0x010100a7, 0x0101009e, 0x0101009c, 0x010100a1 };
    protected static final int[] PRESSED_ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = new int[] { 0x010100a7, 0x0101009e, 0x0101009c, 0x0101009d };
    protected static final int[] PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = new int[] { 0x010100a7, 0x0101009e, 0x010100a1, 0x0101009d };
    protected static final int[] PRESSED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = new int[] { 0x010100a7, 0x0101009c, 0x010100a1, 0x0101009d };
    protected static final int[] PRESSED_ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = new int[] { 0x010100a7, 0x0101009e, 0x0101009c, 0x010100a1, 0x0101009d };

    public static class BaseSavedState extends AbsSavedState {
        public BaseSavedState(Parcel source) {
            super(source);
        }

        public BaseSavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
        }

        public BaseSavedState(Parcelable superState) {
            super(superState);
        }
    }

    public interface OnAttachStateChangeListener {
        void onViewAttachedToWindow(View v);

        void onViewDetachedFromWindow(View v);
    }

    public interface OnApplyWindowInsetsListener {
        WindowInsets onApplyWindowInsets(View v, WindowInsets insets);
    }

    public static class AccessibilityDelegate {
        public void sendAccessibilityEvent(View host, int eventType) {
        }

        public boolean performAccessibilityAction(View host, int action, android.os.Bundle args) {
            return false;
        }

        public void sendAccessibilityEventUnchecked(View host, android.view.accessibility.AccessibilityEvent event) {
        }

        public boolean dispatchPopulateAccessibilityEvent(View host,
                android.view.accessibility.AccessibilityEvent event) {
            return false;
        }

        public void onPopulateAccessibilityEvent(View host, android.view.accessibility.AccessibilityEvent event) {
        }

        public void onInitializeAccessibilityEvent(View host, android.view.accessibility.AccessibilityEvent event) {
        }

        public void onInitializeAccessibilityNodeInfo(View host,
                android.view.accessibility.AccessibilityNodeInfo info) {
        }

        public void addExtraDataToAccessibilityNodeInfo(View host,
                android.view.accessibility.AccessibilityNodeInfo info,
                String extraDataKey, android.os.Bundle arguments) {
        }

        public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child,
                android.view.accessibility.AccessibilityEvent event) {
            return true;
        }

        public android.view.accessibility.AccessibilityNodeProvider getAccessibilityNodeProvider(View host) {
            return null;
        }
    }

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

    public interface OnCreateContextMenuListener {
        void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo);
    }

    public interface OnScrollChangeListener {
        void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY);
    }

    public interface OnLayoutChangeListener {
        void onLayoutChange(View v, int left, int top, int right, int bottom,
                int oldLeft, int oldTop, int oldRight, int oldBottom);
    }

    protected final Context context;
    ViewParent parent;
    private ViewGroup.LayoutParams layoutParams;

    private int id = NO_ID;
    private Object tag;
    private SparseArray<Object> keyedTags;
    private int visibility = VISIBLE;
    private int layoutDirection = LAYOUT_DIRECTION_LTR;
    private int importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_AUTO;
    private boolean accessibilityHeading;
    private boolean screenReaderFocusable;
    private CharSequence accessibilityPaneTitle;
    private CharSequence stateDescription;
    private String transitionName;
    private boolean willNotDraw;
    private boolean enabled = true;
    private boolean clickable;
    private boolean longClickable;
    private boolean focusable;
    private boolean focused;
    private boolean pressed;
    private boolean hovered;
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
    private Drawable foreground;
    private int foregroundGravity = Gravity.FILL;
    private ColorStateList foregroundTintList;
    private PorterDuff.Mode foregroundTintMode;
    protected OnClickListener onClickListener;
    protected OnLongClickListener onLongClickListener;
    protected OnTouchListener onTouchListener;
    protected OnKeyListener onKeyListener;
    protected OnFocusChangeListener onFocusChangeListener;
    protected OnScrollChangeListener onScrollChangeListener;
    protected OnCreateContextMenuListener onCreateContextMenuListener;
    private Runnable invalidateCallback;
    private final Handler viewHandler = new Handler();
    private ViewTreeObserver viewTreeObserver;
    private ViewPropertyAnimator animator;
    private final List<OnAttachStateChangeListener> attachListeners = new CopyOnWriteArrayList<>();
    private final List<OnLayoutChangeListener> layoutChangeListeners = new CopyOnWriteArrayList<>();
    private android.view.animation.Animation currentAnimation;
    private OnApplyWindowInsetsListener applyWindowInsetsListener;
    private AccessibilityDelegate accessibilityDelegate;
    private CharSequence contentDescription;
    private CharSequence tooltipText;
    private int overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS;
    private int layerType = LAYER_TYPE_NONE;
    private boolean clipToOutline;
    private ViewOutlineProvider outlineProvider = ViewOutlineProvider.BACKGROUND;
    private android.animation.StateListAnimator stateListAnimator;
    private boolean focusableInTouchMode;
    private boolean scrollContainer;
    private boolean keyboardNavigationCluster;
    private int importantForAutofill = IMPORTANT_FOR_AUTOFILL_AUTO;
    private int accessibilityLiveRegion = ACCESSIBILITY_LIVE_REGION_NONE;
    private boolean laidOut;
    private boolean transientState;
    private boolean layoutRequested;
    private boolean selected;
    private boolean activated;
    private float pivotX;
    private float pivotY;
    private boolean pivotXSet;
    private boolean pivotYSet;
    private float translationZ;
    private int textDirection = TEXT_DIRECTION_INHERIT;
    private int textAlignment = TEXT_ALIGNMENT_GRAVITY;
    private ColorStateList backgroundTintList;
    private PorterDuff.Mode backgroundTintMode;
    private final Matrix viewMatrix = new Matrix();
    private boolean attachedToWindow;
    private IBinder windowToken;
    private boolean hasWindowFocus = true;

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
        if (context != null && (defStyleAttr != 0 || defStyleRes != 0 || (attrs != null && attrs.getStyleAttribute() != 0))) {
            applyStyledViewAttributes(context, attrs, defStyleAttr, defStyleRes);
        }
    }

    private void applyStyledViewAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        int[] styleAttrs = new int[] {
                0x010100d4, // android:background
                0x0101013f, // android:minWidth
                0x01010140, // android:minHeight
                0x0101046b  // android:backgroundTint
        };
        try {
            TypedArray ta = context.obtainStyledAttributes(attrs, styleAttrs, defStyleAttr, defStyleRes);
            try {
                if (this.background == null && ta.hasValue(0)) {
                    Drawable d = ta.getDrawable(0);
                    if (d != null) {
                        this.background = d;
                        this.background.setCallback(this);
                    }
                }
                if (this.minWidth == 0 && ta.hasValue(1)) {
                    this.minWidth = ta.getDimensionPixelSize(1, 0);
                }
                if (this.minHeight == 0 && ta.hasValue(2)) {
                    this.minHeight = ta.getDimensionPixelSize(2, 0);
                }
                if (this.backgroundTintList == null && ta.hasValue(3)) {
                    ColorStateList csl = ta.getColorStateList(3);
                    if (csl != null) {
                        this.backgroundTintList = csl;
                        if (this.background != null) {
                            this.background.setTintList(csl);
                        }
                    }
                }
            } finally {
                ta.recycle();
            }
        } catch (Throwable ignored) {
        }
    }

    protected void applyXmlAttributes(XmlElement el, Resources res) {
        if (el == null || res == null)
            return;
        for (XmlElement.Attribute a : el.attributes) {
            if (!XmlElement.ANDROID_NS.equals(a.namespace))
                continue;
            ResValue v = a.value;
            switch (a.name) {
                case "id" -> {
                    if (v.isReference())
                        this.id = v.data();
                }
                case "tag" -> this.tag = res.resolveString(v);
                case "visibility" -> {
                    int val = res.getTable().resolve(v).data();
                    if (val == 1 || val == INVISIBLE)
                        this.visibility = INVISIBLE;
                    else if (val == 2 || val == GONE)
                        this.visibility = GONE;
                    else
                        this.visibility = VISIBLE;
                }
                case "background" -> {
                    Resources.Theme theme = context != null ? context.getTheme() : null;
                    ResValue resolved = v;
                    if (resolved.type() == ResValue.TYPE_ATTRIBUTE && theme != null) {
                        ResValue themed = theme.resolveAttributeValue(resolved.data());
                        if (themed != null)
                            resolved = themed;
                    }
                    int refId = resolved.isReference() ? resolved.data() : 0;
                    ResValue rv = res.getTable().resolve(resolved);
                    if (rv != null && rv.isColor()) {
                        this.background = new ColorDrawable(rv.data());
                    } else if (refId != 0) {
                        Drawable d = res.getDrawable(refId, theme);
                        if (d != null)
                            this.background = d;
                    }
                }
                case "backgroundTint" -> {
                    Resources.Theme theme = context != null ? context.getTheme() : null;
                    int col = res.resolveColor(v, theme, 0);
                    if ((col >>> 24) != 0) {
                        this.backgroundTintList = ColorStateList.valueOf(col);
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
                    if (rv != null)
                        this.alpha = rv.type() == ResValue.TYPE_FLOAT ? rv.getFloat() : 1f;
                }
                case "elevation" -> this.elevation = res.resolveDimension(v, 0f);
                case "contentDescription" -> this.contentDescription = res.resolveString(v);
            }
        }
    }

    public static int generateViewId() {
        return NEXT_GENERATED_ID.getAndIncrement();
    }

    public static View inflate(Context context, int resource, ViewGroup root) {
        return LayoutInflater.from(context).inflate(resource, root);
    }

    public final Context getContext() {
        return context;
    }

    public Display getDisplay() {
        return context != null ? context.getDisplay() : null;
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

    public Object getTag(int key) {
        return keyedTags != null ? keyedTags.get(key) : null;
    }

    public void setTag(int key, Object tag) {
        if (keyedTags == null) {
            keyedTags = new SparseArray<>();
        }
        keyedTags.put(key, tag);
    }

    public int getLayoutDirection() {
        return layoutDirection;
    }

    public void setLayoutDirection(int layoutDirection) {
        this.layoutDirection = layoutDirection;
    }

    public int getImportantForAccessibility() {
        return importantForAccessibility;
    }

    public void setAccessibilityHeading(boolean isHeading) {
        this.accessibilityHeading = isHeading;
    }

    public boolean isAccessibilityHeading() {
        return accessibilityHeading;
    }

    public void setScreenReaderFocusable(boolean screenReaderFocusable) {
        this.screenReaderFocusable = screenReaderFocusable;
    }

    public boolean isScreenReaderFocusable() {
        return screenReaderFocusable;
    }

    public void setAccessibilityPaneTitle(CharSequence accessibilityPaneTitle) {
        this.accessibilityPaneTitle = accessibilityPaneTitle;
    }

    public CharSequence getAccessibilityPaneTitle() {
        return accessibilityPaneTitle;
    }

    public void setStateDescription(CharSequence stateDescription) {
        this.stateDescription = stateDescription;
    }

    public CharSequence getStateDescription() {
        return stateDescription;
    }

    public String getTransitionName() {
        return transitionName;
    }

    public void setTransitionName(String transitionName) {
        this.transitionName = transitionName;
    }

    public void setImportantForAccessibility(int mode) {
        this.importantForAccessibility = mode;
    }

    public boolean isAttachedToWindow() {
        return attachedToWindow;
    }

    public void addOnAttachStateChangeListener(OnAttachStateChangeListener listener) {
        if (listener != null && !attachListeners.contains(listener)) {
            attachListeners.add(listener);
        }
    }

    public void removeOnAttachStateChangeListener(OnAttachStateChangeListener listener) {
        attachListeners.remove(listener);
    }

    protected void onAttachedToWindow() {
        attachedToWindow = true;
    }

    protected void onDetachedFromWindow() {
        attachedToWindow = false;
    }

    public void dispatchAttachedToWindow() {
        if (!attachedToWindow) {
            attachedToWindow = true;
            onAttachedToWindow();
            for (OnAttachStateChangeListener l : attachListeners) {
                l.onViewAttachedToWindow(this);
            }
        }
    }

    public void dispatchDetachedFromWindow() {
        if (attachedToWindow) {
            attachedToWindow = false;
            onDetachedFromWindow();
            for (OnAttachStateChangeListener l : attachListeners) {
                l.onViewDetachedFromWindow(this);
            }
        }
    }

    public View getRootView() {
        View cur = this;
        while (cur.parent instanceof View p) {
            cur = p;
        }
        return cur;
    }

    public Handler getHandler() {
        return viewHandler;
    }

    public boolean isInEditMode() {
        return false;
    }

    public CharSequence getContentDescription() {
        return contentDescription;
    }

    public void setContentDescription(CharSequence contentDescription) {
        this.contentDescription = contentDescription;
    }

    public CharSequence getTooltipText() {
        return tooltipText;
    }

    public void setTooltipText(CharSequence tooltipText) {
        this.tooltipText = tooltipText;
    }

    public int getOverScrollMode() {
        return overScrollMode;
    }

    public void setOverScrollMode(int overScrollMode) {
        this.overScrollMode = overScrollMode;
    }

    public boolean canScrollVertically(int direction) {
        return false;
    }

    public boolean canScrollHorizontally(int direction) {
        return false;
    }

    public Matrix getMatrix() {
        viewMatrix.reset();
        return viewMatrix;
    }

    public boolean getClipToOutline() {
        return clipToOutline;
    }

    public void setClipToOutline(boolean clipToOutline) {
        this.clipToOutline = clipToOutline;
        invalidate();
    }

    public ViewOutlineProvider getOutlineProvider() {
        return outlineProvider;
    }

    public void setOutlineProvider(ViewOutlineProvider provider) {
        this.outlineProvider = provider;
        invalidate();
    }

    public void invalidateOutline() {
        invalidate();
    }

    public int getLayerType() {
        return layerType;
    }

    public void setLayerType(int layerType, Paint paint) {
        this.layerType = layerType;
    }

    public boolean isHardwareAccelerated() {
        return true;
    }

    public void setOnApplyWindowInsetsListener(OnApplyWindowInsetsListener listener) {
        this.applyWindowInsetsListener = listener;
    }

    public void setAccessibilityDelegate(AccessibilityDelegate delegate) {
        this.accessibilityDelegate = delegate;
    }

    public AccessibilityDelegate getAccessibilityDelegate() {
        return accessibilityDelegate;
    }

    public boolean performAccessibilityAction(int action, android.os.Bundle arguments) {
        return accessibilityDelegate != null
                && accessibilityDelegate.performAccessibilityAction(this, action, arguments);
    }

    public void sendAccessibilityEvent(int eventType) {
        if (accessibilityDelegate != null) {
            accessibilityDelegate.sendAccessibilityEvent(this, eventType);
        }
    }

    public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info) {
    }

    public void onInitializeAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {
    }

    public void requestApplyInsets() {
        dispatchApplyWindowInsets(new WindowInsets(android.graphics.Insets.NONE, false));
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        return insets;
    }

    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
        if (applyWindowInsetsListener != null) {
            return applyWindowInsetsListener.onApplyWindowInsets(this, insets);
        }
        return onApplyWindowInsets(insets);
    }

    protected boolean fitSystemWindows(Rect insets) {
        return false;
    }

    private boolean fitsSystemWindows;

    public boolean getFitsSystemWindows() {
        return fitsSystemWindows;
    }

    public void setFitsSystemWindows(boolean fitSystemWindows) {
        this.fitsSystemWindows = fitSystemWindows;
        requestLayout();
    }

    public WindowInsets computeSystemWindowInsets(WindowInsets in, Rect outLocalInsets) {
        if (in != null && outLocalInsets != null) {
            outLocalInsets.set(in.getSystemWindowInsetLeft(), in.getSystemWindowInsetTop(),
                    in.getSystemWindowInsetRight(), in.getSystemWindowInsetBottom());
        }
        return in;
    }

    public WindowInsets getRootWindowInsets() {
        return new WindowInsets(android.graphics.Insets.NONE, false);
    }

    public final void saveAttributeDataForStyleable(Context context, int[] styleable, AttributeSet attrs,
            TypedArray t, int defStyleAttr, int defStyleRes) {
    }

    public void setForeground(Drawable foreground) {
        if (this.foreground != foreground) {
            this.foreground = foreground;
            if (foreground != null) {
                foreground.setCallback(this);
                if (foregroundTintList != null)
                    foreground.setTintList(foregroundTintList);
            }
            invalidate();
        }
    }

    public Drawable getForeground() {
        return foreground;
    }

    public void setForegroundGravity(int gravity) {
        this.foregroundGravity = gravity;
        invalidate();
    }

    public int getForegroundGravity() {
        return foregroundGravity;
    }

    public void setForegroundTintList(ColorStateList tint) {
        this.foregroundTintList = tint;
        if (foreground != null)
            foreground.setTintList(tint);
        invalidate();
    }

    public ColorStateList getForegroundTintList() {
        return foregroundTintList;
    }

    public void setForegroundTintMode(PorterDuff.Mode tintMode) {
        this.foregroundTintMode = tintMode;
        if (foreground != null)
            foreground.setTintMode(tintMode);
        invalidate();
    }

    public PorterDuff.Mode getForegroundTintMode() {
        return foregroundTintMode;
    }

    public void onDrawForeground(Canvas canvas) {
        if (foreground != null) {
            foreground.setBounds(0, 0, getWidth(), getHeight());
            foreground.draw(canvas);
        }
    }

    public void setBackgroundTintList(ColorStateList tint) {
        this.backgroundTintList = tint;
        if (background != null)
            background.setTintList(tint);
        invalidate();
    }

    public ColorStateList getBackgroundTintList() {
        return backgroundTintList;
    }

    public void setBackgroundTintMode(PorterDuff.Mode tintMode) {
        this.backgroundTintMode = tintMode;
        if (background != null)
            background.setTintMode(tintMode);
        invalidate();
    }

    public PorterDuff.Mode getBackgroundTintMode() {
        return backgroundTintMode;
    }

    public void refreshDrawableState() {
        drawableStateChanged();
    }

    public void jumpDrawablesToCurrentState() {
        if (background != null) {
            background.jumpToCurrentState();
        }
        if (foreground != null) {
            foreground.jumpToCurrentState();
        }
    }

    protected void drawableStateChanged() {
        if (background != null && background.isStateful()) {
            background.setState(getDrawableState());
        }
    }

    protected int[] onCreateDrawableState(int extraSpace) {
        return new int[extraSpace];
    }

    protected static int[] mergeDrawableStates(int[] baseState, int[] additionalState) {
        if (baseState == null)
            return additionalState != null ? additionalState : new int[0];
        if (additionalState == null)
            return baseState;
        int[] out = new int[baseState.length + additionalState.length];
        System.arraycopy(baseState, 0, out, 0, baseState.length);
        System.arraycopy(additionalState, 0, out, baseState.length, additionalState.length);
        return out;
    }

    private boolean saveEnabled = true;
    private int scrollIndicators;

    public void setSaveEnabled(boolean enabled) {
        this.saveEnabled = enabled;
    }

    public boolean isSaveEnabled() {
        return saveEnabled;
    }

    public void setScrollIndicators(int indicators) {
        this.scrollIndicators = indicators;
    }

    public void setScrollIndicators(int indicators, int mask) {
        this.scrollIndicators = (this.scrollIndicators & ~mask) | (indicators & mask);
    }

    public int getScrollIndicators() {
        return scrollIndicators;
    }

    public void saveHierarchyState(SparseArray<Parcelable> container) {
        dispatchSaveInstanceState(container);
    }

    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        if (saveEnabled && id != NO_ID && container != null) {
            Parcelable state = onSaveInstanceState();
            if (state != null)
                container.put(id, state);
        }
    }

    protected Parcelable onSaveInstanceState() {
        return AbsSavedState.EMPTY_STATE;
    }

    public void restoreHierarchyState(SparseArray<Parcelable> container) {
        dispatchRestoreInstanceState(container);
    }

    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        if (id != NO_ID && container != null) {
            Parcelable state = container.get(id);
            if (state != null)
                onRestoreInstanceState(state);
        }
    }

    protected void onRestoreInstanceState(Parcelable state) {
    }

    public void offsetTopAndBottom(int offset) {
        top += offset;
        bottom += offset;
        invalidate();
    }

    public void offsetLeftAndRight(int offset) {
        left += offset;
        right += offset;
        invalidate();
    }

    public boolean isLaidOut() {
        return laidOut || (right > left && bottom > top);
    }

    public boolean isLayoutRequested() {
        return layoutRequested;
    }

    public int getWindowVisibility() {
        return visibility;
    }

    public int getBaseline() {
        return -1;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;
            refreshDrawableState();
            invalidate();
        }
    }

    public boolean isActivated() {
        return activated;
    }

    public boolean isHovered() {
        return hovered;
    }

    public void setHovered(boolean hovered) {
        if (this.hovered != hovered) {
            this.hovered = hovered;
            refreshDrawableState();
            invalidate();
        }
    }

    public void setActivated(boolean activated) {
        if (this.activated != activated) {
            this.activated = activated;
            refreshDrawableState();
            invalidate();
        }
    }

    public float getPivotX() {
        return pivotXSet ? pivotX : getWidth() / 2f;
    }

    public void setPivotX(float pivotX) {
        this.pivotX = pivotX;
        this.pivotXSet = true;
        invalidate();
    }

    public float getPivotY() {
        return pivotYSet ? pivotY : getHeight() / 2f;
    }

    public void setPivotY(float pivotY) {
        this.pivotY = pivotY;
        this.pivotYSet = true;
        invalidate();
    }

    public float getTranslationZ() {
        return translationZ;
    }

    public void setTranslationZ(float translationZ) {
        this.translationZ = translationZ;
        invalidate();
    }

    public float getZ() {
        return elevation + translationZ;
    }

    public void setZ(float z) {
        setTranslationZ(z - elevation);
    }

    public int getTextDirection() {
        return textDirection;
    }

    public void setTextDirection(int textDirection) {
        this.textDirection = textDirection;
    }

    public int getTextAlignment() {
        return textAlignment;
    }

    public void setTextAlignment(int textAlignment) {
        this.textAlignment = textAlignment;
    }

    public void computeScroll() {
    }

    public void announceForAccessibility(CharSequence text) {
    }

    public void postInvalidateOnAnimation() {
        postInvalidate();
    }

    public void postInvalidateOnAnimation(int left, int top, int right, int bottom) {
        postInvalidate();
    }

    public void postOnAnimation(Runnable action) {
        postDelayed(action, 16);
    }

    public void postOnAnimationDelayed(Runnable action, long delayMillis) {
        postDelayed(action, delayMillis);
    }

    public void setWindowTokenInternal(IBinder token) {
        this.windowToken = token;
    }

    public IBinder getWindowToken() {
        if (windowToken != null)
            return windowToken;
        ViewParent p = parent;
        while (p instanceof View pv) {
            if (pv.windowToken != null)
                return pv.windowToken;
            p = pv.parent;
        }
        if (attachedToWindow) {
            windowToken = new android.os.Binder();
            return windowToken;
        }
        return null;
    }

    public IBinder getApplicationWindowToken() {
        return getWindowToken();
    }

    public boolean hasWindowFocus() {
        return attachedToWindow && hasWindowFocus;
    }

    public void dispatchWindowFocusChanged(boolean hasFocus) {
        this.hasWindowFocus = hasFocus;
        onWindowFocusChanged(hasFocus);
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
        this.hasWindowFocus = hasWindowFocus;
    }

    public ViewTreeObserver getViewTreeObserver() {
        if (viewTreeObserver == null) {
            viewTreeObserver = new ViewTreeObserver();
        }
        return viewTreeObserver;
    }

    public ViewPropertyAnimator animate() {
        if (animator == null) {
            animator = new ViewPropertyAnimator(this);
        }
        return animator;
    }

    public int[] getDrawableState() {
        int[] buf = new int[6];
        int n = 0;
        if (enabled)
            buf[n++] = 0x0101009e; // state_enabled
        if (hasWindowFocus)
            buf[n++] = 0x0101009d; // state_window_focused
        if (focused)
            buf[n++] = 0x0101009c; // state_focused
        if (selected)
            buf[n++] = 0x010100a1; // state_selected
        if (pressed)
            buf[n++] = 0x010100a7; // state_pressed
        if (activated)
            buf[n++] = 0x010102fe; // state_activated
        int[] out = new int[n];
        System.arraycopy(buf, 0, out, 0, n);
        return out;
    }

    public void getLocationOnScreen(int[] outLocation) {
        getLocationInWindow(outLocation);
    }

    public void getLocationInWindow(int[] outLocation) {
        if (outLocation == null || outLocation.length < 2)
            return;
        int x = left;
        int y = top;
        ViewParent p = parent;
        while (p instanceof View pv) {
            x += pv.left - pv.scrollX;
            y += pv.top - pv.scrollY;
            p = pv.parent;
        }
        outLocation[0] = x;
        outLocation[1] = y;
    }

    public boolean hasFocusable() {
        return focusable && visibility == VISIBLE;
    }

    public boolean requestFocus() {
        return requestFocus(0);
    }

    public boolean requestFocus(int direction) {
        if (!focusable || !enabled || visibility != VISIBLE)
            return false;
        setFocusedInternal(true);
        return true;
    }

    public void clearFocus() {
        setFocusedInternal(false);
    }

    public void cancelPendingInputEvents() {
        setPressed(false);
    }

    public void setWillNotDraw(boolean willNotDraw) {
        this.willNotDraw = willNotDraw;
    }

    public boolean willNotDraw() {
        return willNotDraw;
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
            onVisibilityChanged(this, visibility);
            requestLayout();
            invalidate();
        }
    }

    protected void onVisibilityChanged(View changedView, int visibility) {
    }

    private boolean drawingCacheEnabled;
    private android.graphics.Bitmap drawingCache;

    @Deprecated
    public void setDrawingCacheEnabled(boolean enabled) {
        this.drawingCacheEnabled = enabled;
        if (!enabled) {
            drawingCache = null;
        }
    }

    @Deprecated
    public boolean isDrawingCacheEnabled() {
        return drawingCacheEnabled;
    }

    @Deprecated
    public void buildDrawingCache() {
        buildDrawingCache(false);
    }

    @Deprecated
    public void buildDrawingCache(boolean autoScale) {
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());
        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        draw(c);
        this.drawingCache = bmp;
    }

    @Deprecated
    public android.graphics.Bitmap getDrawingCache() {
        return getDrawingCache(false);
    }

    @Deprecated
    public android.graphics.Bitmap getDrawingCache(boolean autoScale) {
        if (drawingCache == null && drawingCacheEnabled) {
            buildDrawingCache(autoScale);
        }
        return drawingCache;
    }

    @Deprecated
    public void destroyDrawingCache() {
        this.drawingCache = null;
    }

    @SuppressWarnings("unchecked")
    public final <T extends View> T findViewWithTag(Object tag) {
        if (tag == null) return null;
        return (T) findViewWithTagTraversal(tag);
    }

    protected View findViewWithTagTraversal(Object tag) {
        if (tag != null && tag.equals(this.tag)) {
            return this;
        }
        return null;
    }

    public void getWindowVisibleDisplayFrame(Rect outRect) {
        if (outRect == null) return;
        Display d = getDisplay();
        if (d != null) {
            android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
            d.getMetrics(dm);
            outRect.set(0, 0, Math.max(1, dm.widthPixels), Math.max(1, dm.heightPixels));
        } else {
            outRect.set(0, 0, Math.max(412, getWidth()), Math.max(820, getHeight()));
        }
    }

    private int systemUiVisibility;

    public int getSystemUiVisibility() {
        return systemUiVisibility;
    }

    public int getWindowSystemUiVisibility() {
        return systemUiVisibility;
    }

    public void setSystemUiVisibility(int visibility) {
        this.systemUiVisibility = visibility;
        invalidate();
    }

    public WindowInsetsController getWindowInsetsController() {
        return null;
    }

    public final OnBackInvokedDispatcher findOnBackInvokedDispatcher() {
        Context c = context;
        while (c instanceof ContextWrapper cw) {
            if (c instanceof Activity act) {
                return act.getOnBackInvokedDispatcher();
            }
            c = cw.getBaseContext();
        }
        return null;
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

    public boolean isLongClickable() {
        return longClickable || onLongClickListener != null;
    }

    public void setLongClickable(boolean longClickable) {
        this.longClickable = longClickable;
    }

    public boolean isFocusable() {
        return focusable;
    }

    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    public boolean isFocusableInTouchMode() {
        return focusableInTouchMode;
    }

    public void setFocusableInTouchMode(boolean focusableInTouchMode) {
        this.focusableInTouchMode = focusableInTouchMode;
        if (focusableInTouchMode)
            this.focusable = true;
    }

    public boolean isScrollContainer() {
        return scrollContainer;
    }

    public void setScrollContainer(boolean isScrollContainer) {
        this.scrollContainer = isScrollContainer;
    }

    public final boolean isKeyboardNavigationCluster() {
        return keyboardNavigationCluster;
    }

    public void setKeyboardNavigationCluster(boolean isCluster) {
        this.keyboardNavigationCluster = isCluster;
    }

    private boolean touchscreenBlocksFocus;
    private boolean saveFromParentEnabled = true;
    private TouchDelegate touchDelegate;

    public final boolean getTouchscreenBlocksFocus() {
        return touchscreenBlocksFocus;
    }

    public void setTouchscreenBlocksFocus(boolean touchscreenBlocksFocus) {
        this.touchscreenBlocksFocus = touchscreenBlocksFocus;
    }

    public boolean isSaveFromParentEnabled() {
        return saveFromParentEnabled;
    }

    public void setSaveFromParentEnabled(boolean enabled) {
        this.saveFromParentEnabled = enabled;
    }

    public TouchDelegate getTouchDelegate() {
        return touchDelegate;
    }

    public void setTouchDelegate(TouchDelegate delegate) {
        this.touchDelegate = delegate;
    }

    public int getImportantForAutofill() {
        return importantForAutofill;
    }

    public void setImportantForAutofill(int mode) {
        this.importantForAutofill = mode;
    }

    public final boolean isImportantForAutofill() {
        return importantForAutofill != IMPORTANT_FOR_AUTOFILL_NO
                && importantForAutofill != IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS;
    }

    public int getAccessibilityLiveRegion() {
        return accessibilityLiveRegion;
    }

    public void setAccessibilityLiveRegion(int mode) {
        this.accessibilityLiveRegion = mode;
    }

    public android.animation.StateListAnimator getStateListAnimator() {
        return stateListAnimator;
    }

    public void setStateListAnimator(android.animation.StateListAnimator stateListAnimator) {
        this.stateListAnimator = stateListAnimator;
    }

    public boolean gatherTransparentRegion(android.graphics.Region region) {
        return true;
    }

    public boolean isFocused() {
        return focused;
    }

    public boolean hasFocus() {
        return focused;
    }

    public View findFocus() {
        return focused ? this : null;
    }

    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        return requestFocus(direction);
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

    private float rotationX;
    private float rotationY;
    private boolean duplicateParentStateEnabled;

    public float getRotationX() {
        return rotationX;
    }

    public void setRotationX(float rotationX) {
        this.rotationX = rotationX;
        invalidate();
    }

    public float getRotationY() {
        return rotationY;
    }

    public void setRotationY(float rotationY) {
        this.rotationY = rotationY;
        invalidate();
    }

    public void setX(float x) {
        setTranslationX(x - left);
    }

    public void setY(float y) {
        setTranslationY(y - top);
    }

    public boolean isDuplicateParentStateEnabled() {
        return duplicateParentStateEnabled;
    }

    public void setDuplicateParentStateEnabled(boolean enabled) {
        this.duplicateParentStateEnabled = enabled;
    }

    public void setPadding(int left, int top, int right, int bottom) {
        this.paddingLeft = left;
        this.paddingTop = top;
        this.paddingRight = right;
        this.paddingBottom = bottom;
        requestLayout();
    }

    public void setPaddingRelative(int start, int top, int end, int bottom) {
        setPadding(start, top, end, bottom);
    }

    public boolean isPaddingRelative() {
        return false;
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
        setBackgroundDrawable(background);
    }

    public void setBackgroundDrawable(Drawable background) {
        if (this.background != null) {
            this.background.setCallback(null);
        }
        this.background = background;
        if (this.background != null) {
            this.background.setCallback(this);
            if (this.backgroundTintList != null) {
                this.background.setTintList(this.backgroundTintList);
            }
        }
        invalidate();
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
        if (l != null)
            this.clickable = true;
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

    public void setOnScrollChangeListener(OnScrollChangeListener l) {
        this.onScrollChangeListener = l;
    }

    public void setOnCreateContextMenuListener(OnCreateContextMenuListener l) {
        this.onCreateContextMenuListener = l;
        if (l != null) {
            setLongClickable(true);
        }
    }

    public boolean performClick() {
        if (onClickListener != null && enabled) {
            onClickListener.onClick(this);
            return true;
        }
        return false;
    }

    public boolean performLongClick() {
        if (onLongClickListener != null && enabled) {
            return onLongClickListener.onLongClick(this);
        }
        return false;
    }

    public boolean callOnClick() {
        return performClick();
    }

    @SuppressWarnings("unchecked")
    public final <T extends View> T findViewById(int id) {
        if (id == NO_ID)
            return null;
        return (T) findViewTraversal(id);
    }

    protected View findViewTraversal(int id) {
        return this.id == id ? this : null;
    }

    // --------------------------------------------------------- Measure & Layout

    public final int getMeasuredWidth() {
        return measuredWidth & MEASURED_SIZE_MASK;
    }

    public final int getMeasuredWidthAndState() {
        return measuredWidth;
    }

    public final int getMeasuredHeight() {
        return measuredHeight & MEASURED_SIZE_MASK;
    }

    public final int getMeasuredHeightAndState() {
        return measuredHeight;
    }

    public final void setMeasuredDimension(int measuredWidth, int measuredHeight) {
        this.measuredWidth = measuredWidth;
        this.measuredHeight = measuredHeight;
    }

    public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
        onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(
                getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
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

    public static int resolveSizeAndState(int size, int measureSpec, int childMeasuredState) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        int result = switch (specMode) {
            case MeasureSpec.AT_MOST -> Math.min(size, specSize);
            case MeasureSpec.EXACTLY -> specSize;
            default -> size;
        };
        return result | (childMeasuredState & MEASURED_STATE_MASK);
    }

    public static int combineMeasuredStates(int curState, int newState) {
        return curState | newState;
    }

    public final int getMeasuredState() {
        return 0;
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
            int oldX = scrollX;
            int oldY = scrollY;
            scrollX = x;
            scrollY = y;
            onScrollChanged(scrollX, scrollY, oldX, oldY);
            if (onScrollChangeListener != null) {
                onScrollChangeListener.onScrollChange(this, scrollX, scrollY, oldX, oldY);
            }
            invalidate();
        }
    }

    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
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
        laidOut = true;
        layoutRequested = false;
        int newW = r - l;
        int newH = b - t;
        if (newW != oldW || newH != oldH) {
            onSizeChanged(newW, newH, oldW, oldH);
        }
        onLayout(changed, l, t, r, b);
        if (viewTreeObserver != null) {
            viewTreeObserver.dispatchOnGlobalLayout();
        }
        if (changed && !layoutChangeListeners.isEmpty()) {
            for (OnLayoutChangeListener lcl : layoutChangeListeners) {
                lcl.onLayoutChange(this, l, t, r, b, l, t, l + oldW, t + oldH);
            }
        }
    }

    public boolean isShown() {
        View cur = this;
        while (cur != null) {
            if (cur.getVisibility() != VISIBLE)
                return false;
            ViewParent p = cur.getParent();
            cur = p instanceof View pv ? pv : null;
        }
        return true;
    }

    public boolean isOpaque() {
        return background != null;
    }

    public void getDrawingRect(Rect outRect) {
        if (outRect != null) {
            outRect.set(scrollX, scrollY, scrollX + getWidth(), scrollY + getHeight());
        }
    }

    public boolean getGlobalVisibleRect(Rect r) {
        if (r == null || visibility != VISIBLE)
            return false;
        int[] loc = new int[2];
        getLocationInWindow(loc);
        r.set(loc[0], loc[1], loc[0] + getWidth(), loc[1] + getHeight());
        return getWidth() > 0 && getHeight() > 0;
    }

    public void addOnLayoutChangeListener(OnLayoutChangeListener listener) {
        if (listener != null && !layoutChangeListeners.contains(listener)) {
            layoutChangeListeners.add(listener);
        }
    }

    public void removeOnLayoutChangeListener(OnLayoutChangeListener listener) {
        layoutChangeListeners.remove(listener);
    }

    public void setAnimation(android.view.animation.Animation animation) {
        this.currentAnimation = animation;
    }

    public android.view.animation.Animation getAnimation() {
        return currentAnimation;
    }

    public void startAnimation(android.view.animation.Animation animation) {
        this.currentAnimation = animation;
        if (animation != null) {
            animation.startNow();
        }
        invalidate();
    }

    public void clearAnimation() {
        if (currentAnimation != null) {
            currentAnimation.cancel();
            currentAnimation = null;
        }
        invalidate();
    }

    public void invalidate(Rect dirty) {
        invalidate();
    }

    public void invalidate(int l, int t, int r, int b) {
        invalidate();
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    }

    // --------------------------------------------------------- Draw & Invalidate

    private static final Paint PRESSED_OVERLAY_PAINT = createPressedOverlayPaint();

    private static Paint createPressedOverlayPaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.argb(28, 0, 0, 0));
        return p;
    }

    public void draw(Canvas canvas) {
        if (visibility != VISIBLE || alpha <= 0f)
            return;
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0)
            return;

        boolean hasTransform = rotation != 0f || scaleX != 1f || scaleY != 1f;
        int saveCount = hasTransform ? canvas.save() : -1;
        if (hasTransform) {
            float px = getPivotX();
            float py = getPivotY();
            canvas.translate(px, py);
            if (rotation != 0f)
                canvas.rotate(rotation);
            if (scaleX != 1f || scaleY != 1f)
                canvas.scale(scaleX, scaleY);
            canvas.translate(-px, -py);
        }

        java.awt.Graphics2D g2 = canvas.getAwtGraphics();
        java.awt.Composite oldComposite = null;
        if (g2 != null && alpha < 0.999f) {
            oldComposite = g2.getComposite();
            float effectiveAlpha = alpha;
            if (oldComposite instanceof java.awt.AlphaComposite ac) {
                effectiveAlpha *= ac.getAlpha();
            }
            g2.setComposite(java.awt.AlphaComposite.getInstance(
                    java.awt.AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, effectiveAlpha))));
        }

        try {
            if (viewTreeObserver != null) {
                viewTreeObserver.dispatchOnPreDraw();
                viewTreeObserver.dispatchOnDraw();
            }
            computeScroll();
            if (background != null) {
                background.setBounds(0, 0, w, h);
                background.draw(canvas);
            }
            if (!willNotDraw) {
                onDraw(canvas);
            }
            dispatchDraw(canvas);
            if (overlay != null && !overlay.isEmpty()) {
                overlay.dispatchDraw(canvas);
            }
            onDrawForeground(canvas);

            if (pressed && isClickable() && !(this instanceof ViewGroup)) {
                canvas.drawRoundRect(0, 0, w, h, 8, 8, PRESSED_OVERLAY_PAINT);
            }
        } finally {
            if (g2 != null && oldComposite != null) {
                g2.setComposite(oldComposite);
            }
            if (saveCount >= 0) {
                canvas.restoreToCount(saveCount);
            }
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
        layoutRequested = true;
        if (parent != null) {
            parent.requestLayout();
        }
        invalidate();
    }

    public void bringToFront() {
        if (parent != null) {
            parent.bringChildToFront(this);
        }
    }

    public boolean post(Runnable action) {
        return viewHandler.post(action);
    }

    public boolean postDelayed(Runnable action, long delayMillis) {
        return viewHandler.postDelayed(action, delayMillis);
    }

    public boolean removeCallbacks(Runnable action) {
        viewHandler.removeCallbacks(action);
        return true;
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        invalidate();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        if (what != null) {
            postDelayed(what, Math.max(0L, when - android.os.SystemClock.uptimeMillis()));
        }
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        removeCallbacks(what);
    }

    public void unscheduleDrawable(Drawable who) {
    }

    protected void onFinishInflate() {
    }

    public boolean hasTransientState() {
        return transientState;
    }

    public void setHasTransientState(boolean hasTransientState) {
        boolean old = this.transientState;
        this.transientState = hasTransientState;
        if (old != hasTransientState && parent != null) {
            parent.childHasTransientStateChanged(this, hasTransientState);
        }
    }

    private boolean nestedScrollingEnabled;

    public void setNestedScrollingEnabled(boolean enabled) {
        if (nestedScrollingEnabled && !enabled) {
            stopNestedScroll();
        }
        this.nestedScrollingEnabled = enabled;
    }

    public boolean isNestedScrollingEnabled() {
        return nestedScrollingEnabled;
    }

    public boolean startNestedScroll(int axes) {
        return false;
    }

    public void stopNestedScroll() {
    }

    public boolean hasNestedScrollingParent() {
        return false;
    }

    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return false;
    }

    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return false;
    }

    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return false;
    }

    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
        return false;
    }

    // --------------------------------------------------------- Touch & Keyboard

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!enabled || visibility != VISIBLE)
            return false;
        if (onTouchListener != null && onTouchListener.onTouch(this, event)) {
            return true;
        }
        return onTouchEvent(event);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!isClickable() && !focusable)
            return false;
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

    private boolean horizontalScrollBarEnabled = true;
    private boolean verticalScrollBarEnabled = true;
    private boolean scrollbarFadingEnabled = true;
    private int scrollBarStyle;
    private Rect clipBounds;
    private KeyEvent.DispatcherState keyDispatcherState;
    private PointerIcon pointerIcon;
    ViewOverlay overlay;

    public ViewOverlay getOverlay() {
        if (overlay == null) {
            overlay = new ViewOverlay(context, this);
        }
        return overlay;
    }

    public void setPointerIcon(PointerIcon pointerIcon) {
        this.pointerIcon = pointerIcon;
    }

    public PointerIcon getPointerIcon() {
        return pointerIcon;
    }

    public WindowId getWindowId() {
        View root = getRootView();
        return new WindowId(root != null ? root : this);
    }

    public boolean isHorizontalScrollBarEnabled() {
        return horizontalScrollBarEnabled;
    }

    public void setHorizontalScrollBarEnabled(boolean horizontalScrollBarEnabled) {
        this.horizontalScrollBarEnabled = horizontalScrollBarEnabled;
    }

    public boolean isVerticalScrollBarEnabled() {
        return verticalScrollBarEnabled;
    }

    public void setVerticalScrollBarEnabled(boolean verticalScrollBarEnabled) {
        this.verticalScrollBarEnabled = verticalScrollBarEnabled;
    }

    public void setScrollbarFadingEnabled(boolean fadeScrollbars) {
        this.scrollbarFadingEnabled = fadeScrollbars;
    }

    public boolean isScrollbarFadingEnabled() {
        return scrollbarFadingEnabled;
    }

    public void setScrollBarStyle(int style) {
        this.scrollBarStyle = style;
    }

    public int getScrollBarStyle() {
        return scrollBarStyle;
    }

    public boolean awakenScrollBars() {
        return false;
    }

    protected boolean awakenScrollBars(int startDelay) {
        return false;
    }

    protected boolean awakenScrollBars(int startDelay, boolean invalidate) {
        return false;
    }

    public void setClipBounds(Rect clipBounds) {
        this.clipBounds = clipBounds != null ? new Rect(clipBounds) : null;
        invalidate();
    }

    public Rect getClipBounds() {
        return clipBounds != null ? new Rect(clipBounds) : null;
    }

    public boolean getClipBounds(Rect outRect) {
        if (clipBounds != null) {
            if (outRect != null) outRect.set(clipBounds);
            return true;
        }
        return false;
    }

    protected boolean verifyDrawable(Drawable who) {
        return who == background || who == foreground;
    }

    public KeyEvent.DispatcherState getKeyDispatcherState() {
        if (keyDispatcherState == null) {
            keyDispatcherState = new KeyEvent.DispatcherState();
        }
        return keyDispatcherState;
    }

    public boolean onHoverEvent(MotionEvent event) {
        return false;
    }

    public void playSoundEffect(int soundConstant) {
    }

    protected void onConfigurationChanged(android.content.res.Configuration newConfig) {
    }
}
