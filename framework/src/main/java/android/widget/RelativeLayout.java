package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import dev.aaow.apk.XmlElement;

public class RelativeLayout extends ViewGroup {
    public static final int TRUE = -1;
    public static final int LEFT_OF = 0;
    public static final int RIGHT_OF = 1;
    public static final int ABOVE = 2;
    public static final int BELOW = 3;
    public static final int ALIGN_BASELINE = 4;
    public static final int ALIGN_LEFT = 5;
    public static final int ALIGN_TOP = 6;
    public static final int ALIGN_RIGHT = 7;
    public static final int ALIGN_BOTTOM = 8;
    public static final int ALIGN_PARENT_LEFT = 9;
    public static final int ALIGN_PARENT_TOP = 10;
    public static final int ALIGN_PARENT_RIGHT = 11;
    public static final int ALIGN_PARENT_BOTTOM = 12;
    public static final int CENTER_IN_PARENT = 13;
    public static final int CENTER_HORIZONTAL = 14;
    public static final int CENTER_VERTICAL = 15;
    public static final int START_OF = 16;
    public static final int END_OF = 17;
    public static final int ALIGN_PARENT_START = 20;
    public static final int ALIGN_PARENT_END = 21;

    public static class LayoutParams extends MarginLayoutParams {
        private final int[] rules = new int[24];

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            if (attrs instanceof AttributeSet.XmlAttributeSet xas) {
                for (XmlElement.Attribute a : xas.element.attributes) {
                    if (!XmlElement.ANDROID_NS.equals(a.namespace)) continue;
                    int val = a.value.data();
                    switch (a.name) {
                        case "layout_toLeftOf", "layout_toStartOf" -> rules[LEFT_OF] = val;
                        case "layout_toRightOf", "layout_toEndOf" -> rules[RIGHT_OF] = val;
                        case "layout_above" -> rules[ABOVE] = val;
                        case "layout_below" -> rules[BELOW] = val;
                        case "layout_alignParentLeft", "layout_alignParentStart" -> rules[ALIGN_PARENT_LEFT] = val != 0 ? TRUE : 0;
                        case "layout_alignParentTop" -> rules[ALIGN_PARENT_TOP] = val != 0 ? TRUE : 0;
                        case "layout_alignParentRight", "layout_alignParentEnd" -> rules[ALIGN_PARENT_RIGHT] = val != 0 ? TRUE : 0;
                        case "layout_alignParentBottom" -> rules[ALIGN_PARENT_BOTTOM] = val != 0 ? TRUE : 0;
                        case "layout_centerInParent" -> rules[CENTER_IN_PARENT] = val != 0 ? TRUE : 0;
                        case "layout_centerHorizontal" -> rules[CENTER_HORIZONTAL] = val != 0 ? TRUE : 0;
                        case "layout_centerVertical" -> rules[CENTER_VERTICAL] = val != 0 ? TRUE : 0;
                    }
                }
            }
        }

        public LayoutParams(int w, int h) {
            super(w, h);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public void addRule(int verb) {
            addRule(verb, TRUE);
        }

        public void addRule(int verb, int subject) {
            if (verb == START_OF) verb = LEFT_OF;
            else if (verb == END_OF) verb = RIGHT_OF;
            else if (verb == ALIGN_PARENT_START) verb = ALIGN_PARENT_LEFT;
            else if (verb == ALIGN_PARENT_END) verb = ALIGN_PARENT_RIGHT;
            if (verb >= 0 && verb < rules.length) rules[verb] = subject;
        }

        public int getRule(int verb) {
            return verb >= 0 && verb < rules.length ? rules[verb] : 0;
        }
    }

    public RelativeLayout(Context context) {
        super(context);
    }

    public RelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
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
        int maxW = 0, maxH = 0;
        for (View child : children) {
            if (child.getVisibility() == GONE) continue;
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            maxW = Math.max(maxW, child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
            maxH = Math.max(maxH, child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
        }
        setMeasuredDimension(
                resolveSize(maxW + paddingLeft + paddingRight, widthMeasureSpec),
                resolveSize(maxH + paddingTop + paddingBottom, heightMeasureSpec)
        );
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int pw = r - l;
        int ph = b - t;
        for (View child : children) {
            if (child.getVisibility() == GONE) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int cw = child.getMeasuredWidth();
            int ch = child.getMeasuredHeight();

            int cl = paddingLeft + lp.leftMargin;
            int ct = paddingTop + lp.topMargin;

            if (lp.getRule(CENTER_IN_PARENT) == TRUE || lp.getRule(CENTER_HORIZONTAL) == TRUE) {
                cl = (pw - cw) / 2;
            } else if (lp.getRule(ALIGN_PARENT_RIGHT) == TRUE) {
                cl = pw - paddingRight - lp.rightMargin - cw;
            } else if (lp.getRule(RIGHT_OF) != 0) {
                View anchor = findViewById(lp.getRule(RIGHT_OF));
                if (anchor != null) cl = anchor.getRight() + lp.leftMargin;
            } else if (lp.getRule(LEFT_OF) != 0) {
                View anchor = findViewById(lp.getRule(LEFT_OF));
                if (anchor != null) cl = anchor.getLeft() - lp.rightMargin - cw;
            }

            if (lp.getRule(CENTER_IN_PARENT) == TRUE || lp.getRule(CENTER_VERTICAL) == TRUE) {
                ct = (ph - ch) / 2;
            } else if (lp.getRule(ALIGN_PARENT_BOTTOM) == TRUE) {
                ct = ph - paddingBottom - lp.bottomMargin - ch;
            } else if (lp.getRule(BELOW) != 0) {
                View anchor = findViewById(lp.getRule(BELOW));
                if (anchor != null) ct = anchor.getBottom() + lp.topMargin;
            } else if (lp.getRule(ABOVE) != 0) {
                View anchor = findViewById(lp.getRule(ABOVE));
                if (anchor != null) ct = anchor.getTop() - lp.bottomMargin - ch;
            }

            child.layout(cl, ct, cl + cw, ct + ch);
        }
    }
}
