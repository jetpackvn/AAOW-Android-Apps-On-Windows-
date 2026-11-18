package dev.aaow.rt.compat;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import dev.aaow.apk.ResValue;
import dev.aaow.apk.XmlElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Bộ dàn trang tương thích ConstraintLayout giải quyết ràng buộc Start/End/Top/Bottom/Bias/0dp.
 */
public class CompatConstraintLayout extends ViewGroup {
    public static final int UNSET = -1;
    public static final int PARENT_ID = 0;

    public static class LayoutParams extends MarginLayoutParams {
        public int leftToLeft = UNSET;
        public int leftToRight = UNSET;
        public int rightToLeft = UNSET;
        public int rightToRight = UNSET;
        public int topToTop = UNSET;
        public int topToBottom = UNSET;
        public int bottomToTop = UNSET;
        public int bottomToBottom = UNSET;
        public float horizontalBias = 0.5f;
        public float verticalBias = 0.5f;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            if (attrs instanceof AttributeSet.XmlAttributeSet xas && c != null) {
                Resources res = c.getResources();
                for (XmlElement.Attribute a : xas.element.attributes) {
                    switch (a.name) {
                        case "layout_constraintLeft_toLeftOf", "layout_constraintStart_toStartOf" ->
                                leftToLeft = parseConstraintTarget(a.value, res);
                        case "layout_constraintLeft_toRightOf", "layout_constraintStart_toEndOf" ->
                                leftToRight = parseConstraintTarget(a.value, res);
                        case "layout_constraintRight_toLeftOf", "layout_constraintEnd_toStartOf" ->
                                rightToLeft = parseConstraintTarget(a.value, res);
                        case "layout_constraintRight_toRightOf", "layout_constraintEnd_toEndOf" ->
                                rightToRight = parseConstraintTarget(a.value, res);
                        case "layout_constraintTop_toTopOf" ->
                                topToTop = parseConstraintTarget(a.value, res);
                        case "layout_constraintTop_toBottomOf" ->
                                topToBottom = parseConstraintTarget(a.value, res);
                        case "layout_constraintBottom_toTopOf" ->
                                bottomToTop = parseConstraintTarget(a.value, res);
                        case "layout_constraintBottom_toBottomOf" ->
                                bottomToBottom = parseConstraintTarget(a.value, res);
                        case "layout_constraintHorizontal_bias" ->
                                horizontalBias = parseFloat(a.value, res, 0.5f);
                        case "layout_constraintVertical_bias" ->
                                verticalBias = parseFloat(a.value, res, 0.5f);
                    }
                }
            }
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        private static int parseConstraintTarget(ResValue v, Resources res) {
            if (v == null) return UNSET;
            if (v.isReference()) return v.data();
            ResValue r = res.getTable().resolve(v);
            if (r != null && r.isInt()) return r.data(); // 0 = parent
            String s = r != null ? r.coerceToString() : v.coerceToString();
            if ("parent".equals(s)) return PARENT_ID;
            return UNSET;
        }

        private static float parseFloat(ResValue v, Resources res, float def) {
            ResValue r = res.getTable().resolve(v);
            if (r == null) return def;
            if (r.type() == ResValue.TYPE_FLOAT) return r.getFloat();
            if (r.isInt()) return r.data();
            return def;
        }
    }

    public CompatConstraintLayout(Context context) {
        super(context);
    }

    public CompatConstraintLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CompatConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentW = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED
                ? 420 : MeasureSpec.getSize(widthMeasureSpec);
        int parentH = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED
                ? 820 : MeasureSpec.getSize(heightMeasureSpec);

        int maxR = paddingLeft + paddingRight;
        int maxB = paddingTop + paddingBottom;

        for (View child : children) {
            if (child.getVisibility() == GONE) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int cwSpec = lp.width == 0
                    ? MeasureSpec.makeMeasureSpec(Math.max(0, parentW - paddingLeft - paddingRight - lp.leftMargin - lp.rightMargin), MeasureSpec.AT_MOST)
                    : getChildMeasureSpec(widthMeasureSpec, paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin, lp.width);
            int chSpec = lp.height == 0
                    ? MeasureSpec.makeMeasureSpec(Math.max(0, parentH - paddingTop - paddingBottom - lp.topMargin - lp.bottomMargin), MeasureSpec.AT_MOST)
                    : getChildMeasureSpec(heightMeasureSpec, paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin, lp.height);
            child.measure(cwSpec, chSpec);
            maxR = Math.max(maxR, child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin + paddingLeft + paddingRight);
            maxB = Math.max(maxB, child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin + paddingTop + paddingBottom);
        }

        setMeasuredDimension(
                resolveSize(Math.max(maxR, getSuggestedMinimumWidth()), widthMeasureSpec),
                resolveSize(Math.max(maxB, getSuggestedMinimumHeight()), heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int pw = r - l;
        int ph = b - t;
        int pLeft = paddingLeft;
        int pTop = paddingTop;
        int pRight = pw - paddingRight;
        int pBottom = ph - paddingBottom;

        Map<Integer, int[]> rects = new HashMap<>();
        int passes = Math.max(1, children.size());
        for (int pass = 0; pass < passes; pass++) {
            for (View child : children) {
                if (child.getVisibility() == GONE) continue;
                LayoutParams lp = (LayoutParams) child.getLayoutParams();

                Integer leftBound = resolveX(lp.leftToLeft, true, lp.leftToRight, false, pLeft, pRight, rects);
                Integer rightBound = resolveX(lp.rightToRight, false, lp.rightToLeft, true, pLeft, pRight, rects);
                Integer topBound = resolveY(lp.topToTop, true, lp.topToBottom, false, pTop, pBottom, rects);
                Integer bottomBound = resolveY(lp.bottomToBottom, false, lp.bottomToTop, true, pTop, pBottom, rects);

                int cw = child.getMeasuredWidth();
                int ch = child.getMeasuredHeight();
                if (lp.width == 0 && leftBound != null && rightBound != null) {
                    cw = Math.max(0, rightBound - leftBound - lp.leftMargin - lp.rightMargin);
                }
                if (lp.height == 0 && topBound != null && bottomBound != null) {
                    ch = Math.max(0, bottomBound - topBound - lp.topMargin - lp.bottomMargin);
                }
                if (cw != child.getMeasuredWidth() || ch != child.getMeasuredHeight()) {
                    child.measure(
                            MeasureSpec.makeMeasureSpec(cw, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(ch, MeasureSpec.EXACTLY));
                }

                int cl;
                if (leftBound != null && rightBound != null) {
                    int space = Math.max(0, (rightBound - lp.rightMargin) - (leftBound + lp.leftMargin) - cw);
                    cl = leftBound + lp.leftMargin + Math.round(space * lp.horizontalBias);
                } else if (leftBound != null) {
                    cl = leftBound + lp.leftMargin;
                } else if (rightBound != null) {
                    cl = rightBound - lp.rightMargin - cw;
                } else {
                    cl = pLeft + lp.leftMargin;
                }

                int ct;
                if (topBound != null && bottomBound != null) {
                    int space = Math.max(0, (bottomBound - lp.bottomMargin) - (topBound + lp.topMargin) - ch);
                    ct = topBound + lp.topMargin + Math.round(space * lp.verticalBias);
                } else if (topBound != null) {
                    ct = topBound + lp.topMargin;
                } else if (bottomBound != null) {
                    ct = bottomBound - lp.bottomMargin - ch;
                } else {
                    ct = pTop + lp.topMargin;
                }

                child.layout(cl, ct, cl + cw, ct + ch);
                if (child.getId() != NO_ID) {
                    rects.put(child.getId(), new int[]{cl, ct, cl + cw, ct + ch});
                }
            }
        }
    }

    private static Integer resolveX(int aId, boolean aIsLeft, int bId, boolean bIsLeft,
                                    int pLeft, int pRight, Map<Integer, int[]> rects) {
        if (aId == PARENT_ID) return aIsLeft ? pLeft : pRight;
        if (aId != UNSET && rects.containsKey(aId)) {
            int[] r = rects.get(aId);
            return aIsLeft ? r[0] : r[2];
        }
        if (bId == PARENT_ID) return bIsLeft ? pLeft : pRight;
        if (bId != UNSET && rects.containsKey(bId)) {
            int[] r = rects.get(bId);
            return bIsLeft ? r[0] : r[2];
        }
        return null;
    }

    private static Integer resolveY(int aId, boolean aIsTop, int bId, boolean bIsTop,
                                    int pTop, int pBottom, Map<Integer, int[]> rects) {
        if (aId == PARENT_ID) return aIsTop ? pTop : pBottom;
        if (aId != UNSET && rects.containsKey(aId)) {
            int[] r = rects.get(aId);
            return aIsTop ? r[1] : r[3];
        }
        if (bId == PARENT_ID) return bIsTop ? pTop : pBottom;
        if (bId != UNSET && rects.containsKey(bId)) {
            int[] r = rects.get(bId);
            return bIsTop ? r[1] : r[3];
        }
        return null;
    }
}
