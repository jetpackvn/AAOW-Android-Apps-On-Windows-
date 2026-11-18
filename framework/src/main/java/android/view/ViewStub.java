package android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import dev.aaow.apk.XmlElement;

public final class ViewStub extends View {
    public interface OnInflateListener {
        void onInflate(ViewStub stub, View inflated);
    }

    private int layoutResource;
    private int inflatedId = NO_ID;
    private LayoutInflater inflater;
    private OnInflateListener inflateListener;
    private View inflatedView;

    public ViewStub(Context context) {
        this(context, 0);
    }

    public ViewStub(Context context, int layoutResource) {
        this(context, null, 0, 0);
        this.layoutResource = layoutResource;
    }

    public ViewStub(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public ViewStub(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ViewStub(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setVisibility(GONE);
        setWillNotDraw(true);
        if (attrs instanceof AttributeSet.XmlAttributeSet xas) {
            XmlElement.Attribute layoutAttr = xas.element.attr("layout");
            if (layoutAttr != null && layoutAttr.value.isReference()) {
                this.layoutResource = layoutAttr.value.data();
            }
            XmlElement.Attribute infAttr = xas.element.attr("inflatedId");
            if (infAttr != null && infAttr.value.isReference()) {
                this.inflatedId = infAttr.value.data();
            }
        }
    }

    public int getInflatedId() {
        return inflatedId;
    }

    public void setInflatedId(int inflatedId) {
        this.inflatedId = inflatedId;
    }

    public int getLayoutResource() {
        return layoutResource;
    }

    public void setLayoutResource(int layoutResource) {
        this.layoutResource = layoutResource;
    }

    public void setLayoutInflater(LayoutInflater inflater) {
        this.inflater = inflater;
    }

    public LayoutInflater getLayoutInflater() {
        return inflater;
    }

    public void setOnInflateListener(OnInflateListener inflateListener) {
        this.inflateListener = inflateListener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(0, 0);
    }

    @Override
    public void draw(Canvas canvas) {
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
    }

    @Override
    public void setVisibility(int visibility) {
        if (inflatedView != null) {
            inflatedView.setVisibility(visibility);
        } else {
            super.setVisibility(visibility);
            if (visibility == VISIBLE || visibility == INVISIBLE) {
                inflate();
            }
        }
    }

    public View inflate() {
        if (inflatedView != null) {
            return inflatedView;
        }
        ViewParent vp = getParent();
        if (!(vp instanceof ViewGroup parentGroup)) {
            throw new IllegalStateException("ViewStub must have a non-null ViewGroup viewParent");
        }
        if (layoutResource == 0) {
            throw new IllegalArgumentException("ViewStub must have a valid layoutResource");
        }
        LayoutInflater li = inflater != null ? inflater : LayoutInflater.from(getContext());
        View view = li.inflate(layoutResource, parentGroup, false);
        if (inflatedId != NO_ID) {
            view.setId(inflatedId);
        }
        int index = parentGroup.indexOfChild(this);
        parentGroup.removeView(this);
        ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp != null) {
            parentGroup.addView(view, index, lp);
        } else {
            parentGroup.addView(view, index);
        }
        inflatedView = view;
        if (inflateListener != null) {
            inflateListener.onInflate(this, view);
        }
        return view;
    }
}
