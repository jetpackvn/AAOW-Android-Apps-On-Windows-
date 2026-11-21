package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.LocaleList;
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
    protected ColorStateList textColorList;
    protected int hintTextColor = 0xFF79747E;
    protected ColorStateList hintTextColorList;
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
                case "textColor" -> this.textColor = res.resolveColor(a.value, context != null ? context.getTheme() : null, this.textColor);
                case "textColorHint" -> this.hintTextColor = res.resolveColor(a.value, context != null ? context.getTheme() : null, this.hintTextColor);
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
        this.textColorList = ColorStateList.valueOf(color);
        invalidate();
    }

    public void setTextColor(ColorStateList colors) {
        if (colors != null) {
            this.textColorList = colors;
            this.textColor = colors.getColorForState(getDrawableState(), colors.getDefaultColor());
            invalidate();
        }
    }

    public void setHintTextColor(int color) {
        this.hintTextColor = color;
        this.hintTextColorList = ColorStateList.valueOf(color);
        invalidate();
    }

    public void setHintTextColor(ColorStateList colors) {
        if (colors != null) {
            this.hintTextColorList = colors;
            this.hintTextColor = colors.getColorForState(getDrawableState(), colors.getDefaultColor());
            invalidate();
        }
    }

    public final ColorStateList getHintTextColors() {
        return hintTextColorList != null ? hintTextColorList : ColorStateList.valueOf(hintTextColor);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (textColorList != null && textColorList.isStateful()) {
            int next = textColorList.getColorForState(getDrawableState(), textColorList.getDefaultColor());
            if (next != textColor) {
                textColor = next;
                invalidate();
            }
        }
        if (hintTextColorList != null && hintTextColorList.isStateful()) {
            int next = hintTextColorList.getColorForState(getDrawableState(), hintTextColorList.getDefaultColor());
            if (next != hintTextColor) {
                hintTextColor = next;
                invalidate();
            }
        }
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
        setTextAppearance(getContext(), resId);
    }

    @Deprecated
    public void setTextAppearance(Context context, int resId) {
        if (context == null || resId == 0) return;
        int[] attrs = new int[] {
                0x01010095, // android:textSize
                0x01010098, // android:textColor
                0x01010097, // android:textStyle
                0x0101038c  // android:textAllCaps
        };
        android.content.res.TypedArray ta = context.obtainStyledAttributes(resId, attrs);
        try {
            if (ta.hasValue(0)) {
                float sz = ta.getDimension(0, -1f);
                if (sz > 0f) this.textSizePx = sz;
            }
            if (ta.hasValue(1)) {
                ColorStateList csl = ta.getColorStateList(1);
                if (csl != null && (csl.getDefaultColor() >>> 24) != 0) {
                    setTextColor(csl);
                }
            }
            if (ta.hasValue(2)) {
                int style = ta.getInt(2, Typeface.NORMAL);
                this.typeface = Typeface.create(this.typeface, style);
            }
            if (ta.hasValue(3)) {
                this.allCaps = ta.getBoolean(3, this.allCaps);
            }
        } catch (Throwable ignored) {
        } finally {
            ta.recycle();
        }
        requestLayout();
        invalidate();
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

    private int minLines = -1;
    private int minEms = -1;
    private int maxEms = Integer.MAX_VALUE;

    public void setMinLines(int minLines) {
        this.minLines = minLines;
        requestLayout();
    }

    public int getMinLines() {
        return minLines;
    }

    public void setEms(int ems) {
        this.minEms = ems;
        this.maxEms = ems;
        requestLayout();
    }

    public void setMinEms(int minEms) {
        this.minEms = minEms;
        requestLayout();
    }

    public int getMinEms() {
        return minEms;
    }

    public void setMaxEms(int maxEms) {
        this.maxEms = maxEms;
        requestLayout();
    }

    public int getMaxEms() {
        return maxEms;
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

    public int getPaintFlags() {
        return textPaint.getFlags();
    }

    public void setPaintFlags(int flags) {
        if (textPaint.getFlags() != flags) {
            textPaint.setFlags(flags);
            invalidate();
        }
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
        int padL = getCompoundPaddingLeft();
        int padR = getCompoundPaddingRight();
        int padT = getCompoundPaddingTop();
        int padB = getCompoundPaddingBottom();
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int availContentW = widthMode == MeasureSpec.UNSPECIFIED
                ? Integer.MAX_VALUE
                : Math.max(0, widthSize - padL - padR);

        List<String> lines = wrapLines(fm, availContentW);
        int maxLineW = 0;
        for (String line : lines) {
            maxLineW = Math.max(maxLineW, fm.stringWidth(line));
        }

        int lineHeight = Math.max((int) (textSizePx * 1.28f), fm.getHeight());
        int textH = (lines.size() == 1 && lines.get(0).isEmpty() && hasAnyCompoundDrawable()) ? 0 : lines.size() * lineHeight;
        int minCompoundH = 0;
        if (compoundDrawables[0] != null) minCompoundH = Math.max(minCompoundH, compoundDrawables[0].getBounds().height());
        if (compoundDrawables[2] != null) minCompoundH = Math.max(minCompoundH, compoundDrawables[2].getBounds().height());
        int minCompoundW = 0;
        if (compoundDrawables[1] != null) minCompoundW = Math.max(minCompoundW, compoundDrawables[1].getBounds().width());
        if (compoundDrawables[3] != null) minCompoundW = Math.max(minCompoundW, compoundDrawables[3].getBounds().width());

        int desiredW = Math.max(getSuggestedMinimumWidth(), Math.max(maxLineW, minCompoundW) + padL + padR);
        int desiredH = Math.max(getSuggestedMinimumHeight(), Math.max(textH, minCompoundH) + padT + padB);
        if (minWidthPx > 0) desiredW = Math.max(desiredW, minWidthPx);
        if (minHeightPx > 0) desiredH = Math.max(desiredH, minHeightPx);
        desiredW = Math.min(desiredW, maxWidthPx);
        desiredH = Math.min(desiredH, maxHeightPx);

        setMeasuredDimension(
                resolveSize(desiredW, widthMeasureSpec),
                resolveSize(desiredH, heightMeasureSpec)
        );
    }

    private boolean hasAnyCompoundDrawable() {
        return compoundDrawables[0] != null || compoundDrawables[1] != null
                || compoundDrawables[2] != null || compoundDrawables[3] != null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Graphics2D g = canvas.getAwtGraphics();
        if (g == null) return;

        int padL = getCompoundPaddingLeft();
        int padR = getCompoundPaddingRight();
        int padT = getCompoundPaddingTop();
        int padB = getCompoundPaddingBottom();

        // Vẽ compoundDrawables (left/top/right/bottom) — dùng bởi ActionMenuItemView, NavigationView, Button icon
        if (compoundDrawables[0] != null) {
            Drawable d = compoundDrawables[0];
            int dw = Math.max(0, d.getBounds().width());
            int dh = Math.max(0, d.getBounds().height());
            int dx = (text == null || text.length() == 0)
                    ? Math.max(paddingLeft, (getWidth() - dw) / 2)
                    : paddingLeft;
            int dy = paddingTop + Math.max(0, (getHeight() - paddingTop - paddingBottom - dh) / 2);
            int save = canvas.save();
            canvas.translate(dx, dy);
            d.draw(canvas);
            canvas.restoreToCount(save);
        }
        if (compoundDrawables[2] != null) {
            Drawable d = compoundDrawables[2];
            int dw = Math.max(0, d.getBounds().width());
            int dh = Math.max(0, d.getBounds().height());
            int dx = getWidth() - paddingRight - dw;
            int dy = paddingTop + Math.max(0, (getHeight() - paddingTop - paddingBottom - dh) / 2);
            int save = canvas.save();
            canvas.translate(dx, dy);
            d.draw(canvas);
            canvas.restoreToCount(save);
        }
        if (compoundDrawables[1] != null) {
            Drawable d = compoundDrawables[1];
            int dw = Math.max(0, d.getBounds().width());
            int dx = paddingLeft + Math.max(0, (getWidth() - paddingLeft - paddingRight - dw) / 2);
            int save = canvas.save();
            canvas.translate(dx, paddingTop);
            d.draw(canvas);
            canvas.restoreToCount(save);
        }
        if (compoundDrawables[3] != null) {
            Drawable d = compoundDrawables[3];
            int dw = Math.max(0, d.getBounds().width());
            int dh = Math.max(0, d.getBounds().height());
            int dx = paddingLeft + Math.max(0, (getWidth() - paddingLeft - paddingRight - dw) / 2);
            int save = canvas.save();
            canvas.translate(dx, getHeight() - paddingBottom - dh);
            d.draw(canvas);
            canvas.restoreToCount(save);
        }

        boolean showingHint = (text == null || text.length() == 0) && hint != null && hint.length() > 0;
        if ((text == null || text.length() == 0) && !showingHint) {
            return;
        }
        int col = showingHint ? hintTextColor : (isEnabled() ? textColor : Color.argb(110, Color.red(textColor), Color.green(textColor), Color.blue(textColor)));

        Font font = typeface.toAwtFont(textSizePx);
        g.setFont(font);
        g.setColor(Color.toAwt(col));
        FontMetrics fm = g.getFontMetrics(font);

        int contentW = Math.max(0, getWidth() - padL - padR);
        int contentH = Math.max(0, getHeight() - padT - padB);
        List<String> lines = wrapLines(fm, contentW);
        int lineHeight = Math.max((int) (textSizePx * 1.28f), fm.getHeight());
        int totalTextH = lines.size() * lineHeight;

        int vg = gravity & Gravity.VERTICAL_GRAVITY_MASK;
        int startY = switch (vg) {
            case Gravity.CENTER_VERTICAL -> padT + (contentH - totalTextH) / 2 + fm.getAscent();
            case Gravity.BOTTOM -> getHeight() - padB - totalTextH + fm.getAscent();
            default -> padT + fm.getAscent();
        };

        int hg = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lw = fm.stringWidth(line);
            int drawX = switch (hg) {
                case Gravity.CENTER_HORIZONTAL -> padL + (contentW - lw) / 2;
                case Gravity.RIGHT -> getWidth() - padR - lw;
                default -> padL;
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
        return textColorList != null ? textColorList : ColorStateList.valueOf(textColor);
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

    private CharSequence errorText;
    private String fontVariationSettings;

    public String getFontVariationSettings() {
        return fontVariationSettings;
    }

    public boolean setFontVariationSettings(String fontVariationSettings) {
        this.fontVariationSettings = fontVariationSettings;
        return true;
    }

    public CharSequence getError() {
        return errorText;
    }

    public void setError(CharSequence error) {
        setError(error, null);
    }

    public void setError(CharSequence error, Drawable icon) {
        this.errorText = error;
        invalidate();
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

    private LocaleList textLocales = LocaleList.getDefault();
    private ColorStateList compoundDrawableTintList;
    private PorterDuff.Mode compoundDrawableTintMode;
    private int autoSizeStepGranularity = 1;
    private int autoSizeMinTextSize = -1;
    private int autoSizeMaxTextSize = -1;
    private int[] autoSizeTextAvailableSizes = new int[0];
    private int breakStrategy = 0;
    private int hyphenationFrequency = 0;
    private int justificationMode = 0;
    private boolean fallbackLineSpacing = true;
    private int firstBaselineToTopHeight = 0;
    private int lastBaselineToBottomHeight = 0;

    public LocaleList getTextLocales() {
        return textLocales != null ? textLocales : LocaleList.getDefault();
    }

    public void setTextLocales(LocaleList locales) {
        this.textLocales = locales != null && !locales.isEmpty() ? locales : LocaleList.getDefault();
    }

    public java.util.Locale getTextLocale() {
        LocaleList ll = getTextLocales();
        return ll.isEmpty() ? java.util.Locale.getDefault() : ll.get(0);
    }

    public void setTextLocale(java.util.Locale locale) {
        if (locale != null) {
            this.textLocales = new LocaleList(locale);
        }
    }

    public ColorStateList getCompoundDrawableTintList() {
        return compoundDrawableTintList;
    }

    public void setCompoundDrawableTintList(ColorStateList tint) {
        this.compoundDrawableTintList = tint;
        invalidate();
    }

    public PorterDuff.Mode getCompoundDrawableTintMode() {
        return compoundDrawableTintMode;
    }

    public void setCompoundDrawableTintMode(PorterDuff.Mode tintMode) {
        this.compoundDrawableTintMode = tintMode;
        invalidate();
    }

    public int getAutoSizeStepGranularity() {
        return autoSizeStepGranularity;
    }

    public int getAutoSizeMinTextSize() {
        return autoSizeMinTextSize;
    }

    public int getAutoSizeMaxTextSize() {
        return autoSizeMaxTextSize;
    }

    public int[] getAutoSizeTextAvailableSizes() {
        return autoSizeTextAvailableSizes;
    }

    public int getBreakStrategy() {
        return breakStrategy;
    }

    public void setBreakStrategy(int breakStrategy) {
        this.breakStrategy = breakStrategy;
    }

    public int getHyphenationFrequency() {
        return hyphenationFrequency;
    }

    public void setHyphenationFrequency(int hyphenationFrequency) {
        this.hyphenationFrequency = hyphenationFrequency;
    }

    public int getJustificationMode() {
        return justificationMode;
    }

    public void setJustificationMode(int justificationMode) {
        this.justificationMode = justificationMode;
    }

    public boolean isFallbackLineSpacing() {
        return fallbackLineSpacing;
    }

    public void setFallbackLineSpacing(boolean enabled) {
        this.fallbackLineSpacing = enabled;
    }

    public int getFirstBaselineToTopHeight() {
        return firstBaselineToTopHeight;
    }

    public void setFirstBaselineToTopHeight(int firstBaselineToTopHeight) {
        this.firstBaselineToTopHeight = Math.max(0, firstBaselineToTopHeight);
    }

    public int getLastBaselineToBottomHeight() {
        return lastBaselineToBottomHeight;
    }

    public void setLastBaselineToBottomHeight(int lastBaselineToBottomHeight) {
        this.lastBaselineToBottomHeight = Math.max(0, lastBaselineToBottomHeight);
    }

    public void setLineHeight(int lineHeight) {
        if (lineHeight > 0) {
            this.lineSpacingAdd = Math.max(0f, lineHeight - textSizePx * 1.28f);
            cachedLayout = null;
            requestLayout();
        }
    }
}
