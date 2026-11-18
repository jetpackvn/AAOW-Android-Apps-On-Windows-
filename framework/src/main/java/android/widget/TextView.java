package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.text.method.MovementMethod;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import dev.aaow.apk.ResValue;
import dev.aaow.apk.XmlElement;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TextView extends View implements ViewTreeObserver.OnPreDrawListener {
    public interface OnEditorActionListener {
        boolean onEditorAction(TextView v, int actionId, KeyEvent event);
    }

    private static final Graphics2D METRICS_G =
            new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();

    protected CharSequence text = "";
    protected CharSequence hint = "";
    protected int textColor = 0xFF1C1B1F;
    protected int hintTextColor = 0xFF79747E;
    protected float textSizePx = 14f;
    protected Typeface typeface = Typeface.DEFAULT;
    protected int gravity = Gravity.TOP | Gravity.START;
    protected int maxLines = Integer.MAX_VALUE;
    protected boolean singleLine;
    protected boolean allCaps;
    protected int inputType;
    protected int imeOptions;
    protected Editable.SimpleEditable editableText;
    protected final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected final List<TextWatcher> watchers = new CopyOnWriteArrayList<>();
    protected final Drawable[] compoundDrawables = new Drawable[4];
    protected int compoundDrawablePadding;
    protected boolean includeFontPadding = true;
    protected TextUtils.TruncateAt ellipsize;
    protected float lineSpacingAdd = 0f;
    protected float lineSpacingMult = 1.0f;
    protected int autoSizeTextType = 0;
    private TransformationMethod transformationMethod;
    private KeyListener keyListener;
    private Editable.Factory editableFactory = Editable.Factory.getInstance();
    private OnEditorActionListener onEditorActionListener;
    private InputFilter[] filters = new InputFilter[0];
    private ActionMode.Callback customSelectionActionModeCallback;
    private Drawable textCursorDrawable;
    private Layout cachedLayout;
    private int cachedLayoutWidth = -1;
    private CharSequence cachedLayoutText;

    public TextView(Context context) {
        this(context, null, 0);
    }

    public TextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs instanceof AttributeSet.XmlAttributeSet xas && context != null) {
            applyTextAttributes(xas.element, context.getResources());
        }
    }

    private void applyTextAttributes(XmlElement el, Resources res) {
        int textStyle = Typeface.NORMAL;
        String fontFamily = null;
        for (XmlElement.Attribute a : el.attributes) {
            if (!XmlElement.ANDROID_NS.equals(a.namespace)) continue;
            switch (a.name) {
                case "text" -> {
                    String s = res.resolveString(a.value);
                    if (s != null) this.text = s;
                }
                case "hint" -> {
                    String s = res.resolveString(a.value);
                    if (s != null) this.hint = s;
                }
                case "textSize" -> this.textSizePx = res.resolveDimension(a.value, 14f);
                case "textColor" -> this.textColor = res.resolveColor(a.value, this.textColor);
                case "textColorHint" -> this.hintTextColor = res.resolveColor(a.value, this.hintTextColor);
                case "textStyle" -> {
                    ResValue rv = res.getTable().resolve(a.value);
                    if (rv != null) textStyle = rv.data();
                }
                case "fontFamily", "typeface" -> fontFamily = res.resolveString(a.value);
                case "gravity" -> {
                    ResValue rv = res.getTable().resolve(a.value);
                    if (rv != null) this.gravity = rv.data();
                }
                case "maxLines" -> {
                    ResValue rv = res.getTable().resolve(a.value);
                    if (rv != null && rv.data() > 0) this.maxLines = rv.data();
                }
                case "singleLine" -> {
                    ResValue rv = res.getTable().resolve(a.value);
                    if (rv != null && rv.data() != 0) {
                        this.singleLine = true;
                        this.maxLines = 1;
                    }
                }
                case "textAllCaps" -> {
                    ResValue rv = res.getTable().resolve(a.value);
                    if (rv != null) this.allCaps = rv.data() != 0;
                }
                case "inputType" -> {
                    ResValue rv = res.getTable().resolve(a.value);
                    if (rv != null) this.inputType = rv.data();
                }
                case "lines", "minLines" -> {
                    ResValue rv = res.getTable().resolve(a.value);
                    if (rv != null && rv.data() > 0 && "lines".equals(a.name)) {
                        this.maxLines = rv.data();
                    }
                }
                case "drawablePadding" -> this.compoundDrawablePadding = Math.round(res.resolveDimension(a.value, 0f));
                case "drawableLeft", "drawableStart" -> {
                    if (a.value.isReference()) compoundDrawables[0] = res.getDrawable(a.value.data());
                }
                case "drawableTop" -> {
                    if (a.value.isReference()) compoundDrawables[1] = res.getDrawable(a.value.data());
                }
                case "drawableRight", "drawableEnd" -> {
                    if (a.value.isReference()) compoundDrawables[2] = res.getDrawable(a.value.data());
                }
                case "drawableBottom" -> {
                    if (a.value.isReference()) compoundDrawables[3] = res.getDrawable(a.value.data());
                }
                case "includeFontPadding" -> {
                    ResValue rv = res.getTable().resolve(a.value);
                    if (rv != null) this.includeFontPadding = rv.data() != 0;
                }
            }
        }
        if (fontFamily != null || textStyle != Typeface.NORMAL) {
            this.typeface = Typeface.create(fontFamily, textStyle);
        }
    }

    public CharSequence getText() {
        return text;
    }

    public Editable getEditableText() {
        if (editableText == null) {
            editableText = text instanceof Editable.SimpleEditable se ? se : new Editable.SimpleEditable(text);
        }
        return editableText;
    }

    public final void setEditableFactory(Editable.Factory factory) {
        if (factory != null) {
            this.editableFactory = factory;
        }
    }

    public void setText(CharSequence text) {
        CharSequence newText = text != null ? text : "";
        CharSequence oldText = this.text;
        for (TextWatcher w : watchers) {
            w.beforeTextChanged(oldText, 0, oldText.length(), newText.length());
        }
        this.text = newText;
        this.editableText = newText instanceof Editable.SimpleEditable se ? se : new Editable.SimpleEditable(newText);
        for (TextWatcher w : watchers) {
            w.onTextChanged(newText, 0, oldText.length(), newText.length());
            w.afterTextChanged(this.editableText);
        }
        requestLayout();
        invalidate();
    }

    public final void setText(int resid) {
        if (getContext() != null) {
            setText(getContext().getResources().getText(resid));
        }
    }

    public CharSequence getHint() {
        return hint;
    }

    public void setHint(CharSequence hint) {
        this.hint = hint != null ? hint : "";
        requestLayout();
        invalidate();
    }

    public final void setHint(int resid) {
        if (getContext() != null) {
            setHint(getContext().getResources().getText(resid));
        }
    }

    public int getCurrentTextColor() {
        return textColor;
    }

    public void setTextColor(int color) {
        this.textColor = color;
        invalidate();
    }

    public void setTextColor(ColorStateList colors) {
        if (colors != null) setTextColor(colors.getDefaultColor());
    }

    public void setHintTextColor(int color) {
        this.hintTextColor = color;
        invalidate();
    }

    public float getTextSize() {
        return textSizePx;
    }

    public void setTextSize(float sizeSp) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
    }

    public void setTextSize(int unit, float size) {
        DisplayMetrics dm = getResources() != null ? getResources().getDisplayMetrics() : new DisplayMetrics();
        this.textSizePx = Math.max(1f, TypedValue.applyDimension(unit, size, dm));
        requestLayout();
        invalidate();
    }

    public Typeface getTypeface() {
        return typeface;
    }

    public void setTypeface(Typeface tf) {
        this.typeface = tf != null ? tf : Typeface.DEFAULT;
        requestLayout();
        invalidate();
    }

    public void setTypeface(Typeface tf, int style) {
        this.typeface = Typeface.create(tf, style);
        requestLayout();
        invalidate();
    }

    public int getGravity() {
        return gravity;
    }

    public void setGravity(int gravity) {
        this.gravity = gravity;
        invalidate();
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = Math.max(1, maxLines);
        requestLayout();
        invalidate();
    }

    public int getMaxLines() {
        return maxLines;
    }

    public void setSingleLine() {
        setSingleLine(true);
    }

    public boolean isSingleLine() {
        return singleLine;
    }

    public void setSingleLine(boolean singleLine) {
        this.singleLine = singleLine;
        if (singleLine) this.maxLines = 1;
        requestLayout();
        invalidate();
    }

    public void setAllCaps(boolean allCaps) {
        this.allCaps = allCaps;
        requestLayout();
        invalidate();
    }

    public final TransformationMethod getTransformationMethod() {
        return transformationMethod;
    }

    public final void setTransformationMethod(TransformationMethod method) {
        this.transformationMethod = method;
        requestLayout();
        invalidate();
    }

    public void addTextChangedListener(TextWatcher watcher) {
        if (watcher != null) watchers.add(watcher);
    }

    public void removeTextChangedListener(TextWatcher watcher) {
        watchers.remove(watcher);
    }

    public void setOnEditorActionListener(OnEditorActionListener l) {
        this.onEditorActionListener = l;
    }

    public void onEditorAction(int actionCode) {
        if (onEditorActionListener != null) {
            onEditorActionListener.onEditorAction(this, actionCode, null);
        }
    }

    public int length() {
        return text != null ? text.length() : 0;
    }

    public void append(CharSequence text) {
        if (text != null) {
            setText(this.text.toString() + text);
        }
    }

    public int getInputType() {
        return inputType;
    }

    public void setInputType(int type) {
        this.inputType = type;
    }

    public void setRawInputType(int type) {
        this.inputType = type;
    }

    public int getImeOptions() {
        return imeOptions;
    }

    public void setImeOptions(int imeOptions) {
        this.imeOptions = imeOptions;
    }

    public void setImeActionLabel(CharSequence label, int actionId) {
    }

    public void setPrivateImeOptions(String type) {
    }

    public final KeyListener getKeyListener() {
        return keyListener;
    }

    public void setKeyListener(KeyListener input) {
        this.keyListener = input;
        if (input != null) {
            this.inputType = input.getInputType();
        }
    }

    public void setTextAppearance(int resId) {
    }

    @Deprecated
    public void setTextAppearance(Context context, int resId) {
    }

    public void setCompoundDrawables(Drawable left, Drawable top, Drawable right, Drawable bottom) {
        compoundDrawables[0] = left;
        compoundDrawables[1] = top;
        compoundDrawables[2] = right;
        compoundDrawables[3] = bottom;
        requestLayout();
        invalidate();
    }

    public void setCompoundDrawablesRelative(Drawable start, Drawable top, Drawable end, Drawable bottom) {
        setCompoundDrawables(start, top, end, bottom);
    }

    public void setCompoundDrawablesWithIntrinsicBounds(int left, int top, int right, int bottom) {
        Resources r = getResources();
        setCompoundDrawablesWithIntrinsicBounds(
                left != 0 && r != null ? r.getDrawable(left) : null,
                top != 0 && r != null ? r.getDrawable(top) : null,
                right != 0 && r != null ? r.getDrawable(right) : null,
                bottom != 0 && r != null ? r.getDrawable(bottom) : null);
    }

    public void setCompoundDrawablesWithIntrinsicBounds(Drawable left, Drawable top, Drawable right, Drawable bottom) {
        setBoundsIntrinsic(left);
        setBoundsIntrinsic(top);
        setBoundsIntrinsic(right);
        setBoundsIntrinsic(bottom);
        setCompoundDrawables(left, top, right, bottom);
    }

    public void setCompoundDrawablesRelativeWithIntrinsicBounds(int start, int top, int end, int bottom) {
        setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
    }

    public void setCompoundDrawablesRelativeWithIntrinsicBounds(Drawable start, Drawable top, Drawable end, Drawable bottom) {
        setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
    }

    private static void setBoundsIntrinsic(Drawable d) {
        if (d != null) {
            int w = d.getIntrinsicWidth();
            int h = d.getIntrinsicHeight();
            d.setBounds(0, 0, w > 0 ? w : 24, h > 0 ? h : 24);
        }
    }

    public Drawable[] getCompoundDrawables() {
        return compoundDrawables.clone();
    }

    public Drawable[] getCompoundDrawablesRelative() {
        return compoundDrawables.clone();
    }

    public void setCompoundDrawablePadding(int pad) {
        this.compoundDrawablePadding = pad;
        requestLayout();
    }

    public int getCompoundDrawablePadding() {
        return compoundDrawablePadding;
    }

    public int getCompoundPaddingLeft() {
        Drawable d = compoundDrawables[0];
        return d == null ? paddingLeft : paddingLeft + compoundDrawablePadding + Math.max(0, d.getBounds().width());
    }

    public int getCompoundPaddingTop() {
        Drawable d = compoundDrawables[1];
        return d == null ? paddingTop : paddingTop + compoundDrawablePadding + Math.max(0, d.getBounds().height());
    }

    public int getCompoundPaddingRight() {
        Drawable d = compoundDrawables[2];
        return d == null ? paddingRight : paddingRight + compoundDrawablePadding + Math.max(0, d.getBounds().width());
    }

    public int getCompoundPaddingBottom() {
        Drawable d = compoundDrawables[3];
        return d == null ? paddingBottom : paddingBottom + compoundDrawablePadding + Math.max(0, d.getBounds().height());
    }

    public int getCompoundPaddingStart() {
        return getCompoundPaddingLeft();
    }

    public int getCompoundPaddingEnd() {
        return getCompoundPaddingRight();
    }

    public int getExtendedPaddingTop() {
        return getCompoundPaddingTop();
    }

    public int getExtendedPaddingBottom() {
        return getCompoundPaddingBottom();
    }

    public int getTotalPaddingLeft() {
        return getCompoundPaddingLeft();
    }

    public int getTotalPaddingTop() {
        return getCompoundPaddingTop();
    }

    public int getTotalPaddingRight() {
        return getCompoundPaddingRight();
    }

    public int getTotalPaddingBottom() {
        return getCompoundPaddingBottom();
    }

    public int getTotalPaddingStart() {
        return getCompoundPaddingStart();
    }

    public int getTotalPaddingEnd() {
        return getCompoundPaddingEnd();
    }

    public void setLines(int lines) {
        this.maxLines = Math.max(1, lines);
        this.singleLine = lines == 1;
        requestLayout();
    }

    public void setMinLines(int minLines) {
        requestLayout();
    }

    public void setEms(int ems) {
        requestLayout();
    }

    public void setMinEms(int minEms) {
        requestLayout();
    }

    public void setMaxEms(int maxEms) {
        requestLayout();
    }

    public void setEllipsize(TextUtils.TruncateAt where) {
        this.ellipsize = where;
        invalidate();
    }

    public TextUtils.TruncateAt getEllipsize() {
        return ellipsize;
    }

    public void setIncludeFontPadding(boolean includepad) {
        this.includeFontPadding = includepad;
    }

    public boolean getIncludeFontPadding() {
        return includeFontPadding;
    }

    public void setHighlightColor(int color) {
    }

    public void setLinkTextColor(int color) {
    }

    public void setLinkTextColor(ColorStateList colors) {
    }

    public void setLineSpacing(float add, float mult) {
        this.lineSpacingAdd = add;
        this.lineSpacingMult = mult;
        cachedLayout = null;
        requestLayout();
    }

    public int getAutoSizeTextType() {
        return autoSizeTextType;
    }

    public void setAutoSizeTextTypeWithDefaults(int autoSizeTextType) {
        this.autoSizeTextType = autoSizeTextType;
    }

    public void setAutoSizeTextTypeUniformWithConfiguration(int min, int max, int step, int unit) {
        this.autoSizeTextType = 1;
    }

    public void setAutoSizeTextTypeUniformWithPresetSizes(int[] presetSizes, int unit) {
        this.autoSizeTextType = 1;
    }

    public int getLineHeight() {
        return Math.max(12, Math.round(textSizePx * 1.28f * lineSpacingMult + lineSpacingAdd));
    }

    public int getLineCount() {
        Layout l = getLayout();
        return l != null ? l.getLineCount() : 1;
    }

    public Layout getLayout() {
        int contentW = Math.max(200, getWidth() - paddingLeft - paddingRight);
        CharSequence cur = text != null ? text : "";
        if (cachedLayout == null || cachedLayoutWidth != contentW || !cur.equals(cachedLayoutText)) {
            cachedLayout = new StaticLayout(cur, getPaint(), contentW,
                    Layout.Alignment.ALIGN_NORMAL, lineSpacingMult, lineSpacingAdd, includeFontPadding);
            cachedLayoutWidth = contentW;
            cachedLayoutText = cur.toString();
        }
        return cachedLayout;
    }

    @Override
    public int getBaseline() {
        Layout l = getLayout();
        return l != null ? paddingTop + l.getLineBaseline(0) : -1;
    }

    public void setTextIsSelectable(boolean selectable) {
    }

    public void setMovementMethod(MovementMethod movement) {
    }

    public TextPaint getPaint() {
        TextPaint tp = new TextPaint(textPaint);
        tp.setTextSize(textSizePx);
        tp.setTypeface(typeface);
        tp.setColor(textColor);
        return tp;
    }

    @Override
    public boolean onPreDraw() {
        return true;
    }

    protected String displayString() {
        CharSequence raw = (text != null && text.length() > 0) ? text : (hint != null ? hint : "");
        if (transformationMethod != null && text != null && text.length() > 0) {
            CharSequence transformed = transformationMethod.getTransformation(raw, this);
            if (transformed != null) raw = transformed;
        }
        String s = raw.toString();
        return allCaps ? s.toUpperCase() : s;
    }

    protected List<String> wrapLines(FontMetrics fm, int maxWidth) {
        String raw = displayString();
        List<String> out = new ArrayList<>();
        if (raw.isEmpty()) {
            out.add("");
            return out;
        }
        String[] paragraphs = raw.split("\n", -1);
        for (String para : paragraphs) {
            if (singleLine || maxWidth <= 0 || fm.stringWidth(para) <= maxWidth) {
                out.add(para);
            } else {
                String[] words = para.split(" ");
                StringBuilder line = new StringBuilder();
                for (String w : words) {
                    if (line.isEmpty()) {
                        line.append(w);
                    } else if (fm.stringWidth(line + " " + w) <= maxWidth) {
                        line.append(' ').append(w);
                    } else {
                        out.add(line.toString());
                        line.setLength(0);
                        line.append(w);
                        if (out.size() >= maxLines) break;
                    }
                }
                if (!line.isEmpty() && out.size() < maxLines) {
                    out.add(line.toString());
                }
            }
            if (out.size() >= maxLines) break;
        }
        return out;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Font font = typeface.toAwtFont(textSizePx);
        FontMetrics fm = METRICS_G.getFontMetrics(font);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int availContentW = widthMode == MeasureSpec.UNSPECIFIED
                ? Integer.MAX_VALUE
                : Math.max(0, widthSize - paddingLeft - paddingRight);

        List<String> lines = wrapLines(fm, availContentW);
        int maxLineW = 0;
        for (String line : lines) {
            maxLineW = Math.max(maxLineW, fm.stringWidth(line));
        }

        int lineHeight = Math.max((int) (textSizePx * 1.28f), fm.getHeight());
        int desiredW = Math.max(getSuggestedMinimumWidth(), maxLineW + paddingLeft + paddingRight);
        int desiredH = Math.max(getSuggestedMinimumHeight(), lines.size() * lineHeight + paddingTop + paddingBottom);

        setMeasuredDimension(
                resolveSize(desiredW, widthMeasureSpec),
                resolveSize(desiredH, heightMeasureSpec)
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Graphics2D g = canvas.getAwtGraphics();
        if (g == null) return;

        boolean showingHint = (text == null || text.length() == 0) && hint != null && hint.length() > 0;
        int col = showingHint ? hintTextColor : (isEnabled() ? textColor : Color.argb(110, Color.red(textColor), Color.green(textColor), Color.blue(textColor)));

        Font font = typeface.toAwtFont(textSizePx);
        g.setFont(font);
        g.setColor(Color.toAwt(col));
        FontMetrics fm = g.getFontMetrics(font);

        int contentW = Math.max(0, getWidth() - paddingLeft - paddingRight);
        int contentH = Math.max(0, getHeight() - paddingTop - paddingBottom);
        List<String> lines = wrapLines(fm, contentW);
        int lineHeight = Math.max((int) (textSizePx * 1.28f), fm.getHeight());
        int totalTextH = lines.size() * lineHeight;

        int vg = gravity & Gravity.VERTICAL_GRAVITY_MASK;
        int startY = switch (vg) {
            case Gravity.CENTER_VERTICAL -> paddingTop + (contentH - totalTextH) / 2 + fm.getAscent();
            case Gravity.BOTTOM -> getHeight() - paddingBottom - totalTextH + fm.getAscent();
            default -> paddingTop + fm.getAscent();
        };

        int hg = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lw = fm.stringWidth(line);
            int drawX = switch (hg) {
                case Gravity.CENTER_HORIZONTAL -> paddingLeft + (contentW - lw) / 2;
                case Gravity.RIGHT -> getWidth() - paddingRight - lw;
                default -> paddingLeft;
            };
            g.drawString(line, drawX, startY + i * lineHeight);
        }
    }

    private boolean showSoftInputOnFocus = true;
    private float letterSpacing = 0f;
    private int minWidthPx = -1;
    private int maxWidthPx = Integer.MAX_VALUE;
    private int minHeightPx = -1;
    private int maxHeightPx = Integer.MAX_VALUE;

    public void setMinWidth(int minPixels) {
        this.minWidthPx = minPixels;
        requestLayout();
    }

    public int getMinWidth() {
        return minWidthPx;
    }

    public void setMaxWidth(int maxPixels) {
        this.maxWidthPx = maxPixels;
        requestLayout();
    }

    public int getMaxWidth() {
        return maxWidthPx;
    }

    public void setMinHeight(int minPixels) {
        this.minHeightPx = minPixels;
        requestLayout();
    }

    public int getMinHeight() {
        return minHeightPx;
    }

    public void setMaxHeight(int maxPixels) {
        this.maxHeightPx = maxPixels;
        requestLayout();
    }

    public int getMaxHeight() {
        return maxHeightPx;
    }

    public float getLetterSpacing() {
        return letterSpacing;
    }

    public void setLetterSpacing(float letterSpacing) {
        this.letterSpacing = letterSpacing;
        invalidate();
    }

    public void setShowSoftInputOnFocus(boolean show) {
        this.showSoftInputOnFocus = show;
    }

    public final boolean getShowSoftInputOnFocus() {
        return showSoftInputOnFocus;
    }

    public final ColorStateList getTextColors() {
        return ColorStateList.valueOf(textColor);
    }

    public void setFilters(InputFilter[] filters) {
        this.filters = filters != null ? filters : new InputFilter[0];
    }

    public InputFilter[] getFilters() {
        return filters;
    }

    public void setCustomSelectionActionModeCallback(ActionMode.Callback actionModeCallback) {
        this.customSelectionActionModeCallback = actionModeCallback;
    }

    public ActionMode.Callback getCustomSelectionActionModeCallback() {
        return customSelectionActionModeCallback;
    }

    public Drawable getTextCursorDrawable() {
        if (textCursorDrawable == null) {
            textCursorDrawable = new ColorDrawable(0xFF4F5BD5);
        }
        return textCursorDrawable;
    }

    public void setTextCursorDrawable(Drawable textCursorDrawable) {
        this.textCursorDrawable = textCursorDrawable;
        invalidate();
    }

    public void setTextCursorDrawable(int resId) {
        if (resId != 0 && getResources() != null) {
            setTextCursorDrawable(getResources().getDrawable(resId));
        } else {
            setTextCursorDrawable((Drawable) null);
        }
    }
}
