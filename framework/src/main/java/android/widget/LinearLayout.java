package android.widget;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import dev.aaow.apk.ResValue;
import dev.aaow.apk.XmlElement;

public class LinearLayout extends ViewGroup {
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    public static class LayoutParams extends MarginLayoutParams {
        public float weight;
        public int gravity = -1;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            if (attrs instanceof AttributeSet.XmlAttributeSet xas && c != null) {
                Resources res = c.getResources();
                XmlElement.Attribute w = xas.element.attr("layout_weight");
                if (w != null) {
                    ResValue rv = res.getTable().resolve(w.value);
                    if (rv != null) {
                        this.weight = rv.type() == ResValue.TYPE_FLOAT ? rv.getFloat() : rv.data();
                    }
                }
                XmlElement.Attribute g = xas.element.attr("layout_gravity");
                if (g != null) {
                    this.gravity = res.getTable().resolve(g.value).data();
                }
            }
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, float weight) {
            super(width, height);
            this.weight = weight;
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }
    }

    private int orientation = HORIZONTAL;
    private int gravity = Gravity.START | Gravity.TOP;
    private float weightSum = -1.0f;

    public LinearLayout(Context context) {
        super(context);
    }

    public LinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public LinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (getClass().getName().contains("AppBarLayout")) {
            this.orientation = VERTICAL;
        }
        if (attrs instanceof AttributeSet.XmlAttributeSet xas && context != null) {
            Resources res = context.getResources();
            XmlElement.Attribute o = xas.element.attr("orientation");
            if (o != null) {
                this.orientation = res.getTable().resolve(o.value).data() == VERTICAL ? VERTICAL : HORIZONTAL;
            }
            XmlElement.Attribute g = xas.element.attr("gravity");
            if (g != null) {
                this.gravity = res.getTable().resolve(g.value).data();
            }
            XmlElement.Attribute ws = xas.element.attr("weightSum");
            if (ws != null) {
                ResValue rv = res.getTable().resolve(ws.value);
                if (rv != null) this.weightSum = rv.type() == ResValue.TYPE_FLOAT ? rv.getFloat() : rv.data();
            }
        }
    }

    public void setOrientation(int orientation) {
        if (this.orientation != orientation) {
            this.orientation = orientation;
            requestLayout();
        }
    }

    public int getOrientation() {
        return orientation;
    }

    public void setGravity(int gravity) {
        if (this.gravity != gravity) {
            this.gravity = gravity;
            requestLayout();
        }
    }

    public int getGravity() {
        return gravity;
    }

    public float getWeightSum() {
        return weightSum;
    }

    public void setWeightSum(float weightSum) {
        this.weightSum = weightSum;
        requestLayout();
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        if (orientation == HORIZONTAL) {
            return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        } else {
            return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (p instanceof LayoutParams lp) return new LayoutParams(lp);
        if (p instanceof MarginLayoutParams mlp) return new LayoutParams(mlp);
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (orientation == VERTICAL) {
            measureVertical(widthMeasureSpec, heightMeasureSpec);
        } else {
            measureHorizontal(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private void measureVertical(int widthMeasureSpec, int heightMeasureSpec) {
        int totalLength = paddingTop + paddingBottom;
        int maxWidth = 0;
        float totalWeight = 0f;

        for (View child : children) {
            if (child.getVisibility() == GONE) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            totalWeight += lp.weight;
            if (lp.height == 0 && lp.weight > 0) {
                totalLength += lp.topMargin + lp.bottomMargin;
            } else {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                totalLength += child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
                maxWidth = Math.max(maxWidth, child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
            }
        }

        int heightSize = resolveSize(Math.max(totalLength, getSuggestedMinimumHeight()), heightMeasureSpec);
        int delta = heightSize - totalLength;
        if (totalWeight > 0f && (delta != 0 || hasZeroWeightedChild( true))) {
            float ws = weightSum > 0f ? weightSum : totalWeight;
            int actualTotal = paddingTop + paddingBottom;
            for (View child : children) {
                if (child.getVisibility() == GONE) continue;
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.weight > 0) {
                    int share = (int) (lp.weight * Math.max(0, delta) / ws);
                    int baseH = lp.height == 0 ? 0 : child.getMeasuredHeight();
                    int chSpec = MeasureSpec.makeMeasureSpec(Math.max(0, baseH + share), MeasureSpec.EXACTLY);
                    int cwSpec = getChildMeasureSpec(widthMeasureSpec,
                            paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin, lp.width);
                    child.measure(cwSpec, chSpec);
                }
                actualTotal += child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
                maxWidth = Math.max(maxWidth, child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
            }
            totalLength = actualTotal;
        }

        maxWidth += paddingLeft + paddingRight;
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());
        setMeasuredDimension(
                resolveSize(maxWidth, widthMeasureSpec),
                resolveSize(Math.max(totalLength, getSuggestedMinimumHeight()), heightMeasureSpec)
        );
    }

    private void measureHorizontal(int widthMeasureSpec, int heightMeasureSpec) {
        int totalLength = paddingLeft + paddingRight;
        int maxHeight = 0;
        float totalWeight = 0f;

        for (View child : children) {
            if (child.getVisibility() == GONE) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            totalWeight += lp.weight;
            if (lp.width == 0 && lp.weight > 0) {
                totalLength += lp.leftMargin + lp.rightMargin;
            } else {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                totalLength += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
            }
        }

        int widthSize = resolveSize(Math.max(totalLength, getSuggestedMinimumWidth()), widthMeasureSpec);
        int delta = widthSize - totalLength;
        if (totalWeight > 0f && (delta != 0 || hasZeroWeightedChild(false))) {
            float ws = weightSum > 0f ? weightSum : totalWeight;
            int actualTotal = paddingLeft + paddingRight;
            for (View child : children) {
                if (child.getVisibility() == GONE) continue;
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.weight > 0) {
                    int share = (int) (lp.weight * Math.max(0, delta) / ws);
                    int baseW = lp.width == 0 ? 0 : child.getMeasuredWidth();
                    int cwSpec = MeasureSpec.makeMeasureSpec(Math.max(0, baseW + share), MeasureSpec.EXACTLY);
                    int chSpec = getChildMeasureSpec(heightMeasureSpec,
                            paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin, lp.height);
                    child.measure(cwSpec, chSpec);
                }
                actualTotal += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
            }
            totalLength = actualTotal;
        }

        maxHeight += paddingTop + paddingBottom;
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        setMeasuredDimension(
                resolveSize(Math.max(totalLength, getSuggestedMinimumWidth()), widthMeasureSpec),
                resolveSize(maxHeight, heightMeasureSpec)
        );
    }

    private boolean hasZeroWeightedChild(boolean vertical) {
        for (View child : children) {
            if (child.getVisibility() == GONE) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.weight > 0 && ((vertical && lp.height == 0) || (!vertical && lp.width == 0))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (orientation == VERTICAL) {
            layoutVertical(l, t, r, b);
        } else {
            layoutHorizontal(l, t, r, b);
        }
    }

    private void layoutVertical(int l, int t, int r, int b) {
        int width = r - l;
        int height = b - t;
        int childSpace = width - paddingLeft - paddingRight;

        int totalChildrenHeight = 0;
        for (View child : children) {
            if (child.getVisibility() == GONE) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            totalChildrenHeight += child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
        }

        int vg = gravity & Gravity.VERTICAL_GRAVITY_MASK;
        int childTop = switch (vg) {
            case Gravity.BOTTOM -> height - paddingBottom - totalChildrenHeight;
            case Gravity.CENTER_VERTICAL -> paddingTop + (height - paddingTop - paddingBottom - totalChildrenHeight) / 2;
            default -> paddingTop;
        };

        for (View child : children) {
            if (child.getVisibility() == GONE) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int cw = child.getMeasuredWidth();
            int ch = child.getMeasuredHeight();

            int g = lp.gravity >= 0 ? lp.gravity : (gravity & Gravity.HORIZONTAL_GRAVITY_MASK);
            int hg = g & Gravity.HORIZONTAL_GRAVITY_MASK;
            int childLeft = switch (hg) {
                case Gravity.CENTER_HORIZONTAL -> paddingLeft + (childSpace - cw) / 2 + lp.leftMargin - lp.rightMargin;
                case Gravity.RIGHT -> width - paddingRight - cw - lp.rightMargin;
                default -> paddingLeft + lp.leftMargin;
            };

            childTop += lp.topMargin;
            child.layout(childLeft, childTop, childLeft + cw, childTop + ch);
            childTop += ch + lp.bottomMargin;
        }
    }

    private void layoutHorizontal(int l, int t, int r, int b) {
        int width = r - l;
        int height = b - t;
        int childSpace = height - paddingTop - paddingBottom;

        int totalChildrenWidth = 0;
        for (View child : children) {
            if (child.getVisibility() == GONE) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            totalChildrenWidth += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
        }

        int hg = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        int childLeft = switch (hg) {
            case Gravity.RIGHT -> width - paddingRight - totalChildrenWidth;
            case Gravity.CENTER_HORIZONTAL -> paddingLeft + (width - paddingLeft - paddingRight - totalChildrenWidth) / 2;
            default -> paddingLeft;
        };

        for (View child : children) {
            if (child.getVisibility() == GONE) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int cw = child.getMeasuredWidth();
            int ch = child.getMeasuredHeight();

            int g = lp.gravity >= 0 ? lp.gravity : (gravity & Gravity.VERTICAL_GRAVITY_MASK);
            int vg = g & Gravity.VERTICAL_GRAVITY_MASK;
            int childTop = switch (vg) {
                case Gravity.CENTER_VERTICAL -> paddingTop + (childSpace - ch) / 2 + lp.topMargin - lp.bottomMargin;
                case Gravity.BOTTOM -> height - paddingBottom - ch - lp.bottomMargin;
                default -> paddingTop + lp.topMargin;
            };

            childLeft += lp.leftMargin;
            child.layout(childLeft, childTop, childLeft + cw, childTop + ch);
            childLeft += cw + lp.rightMargin;
        }
    }
}
