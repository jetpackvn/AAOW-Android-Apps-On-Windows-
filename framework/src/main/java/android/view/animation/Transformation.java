package android.view.animation;

public class Transformation {
    public static final int TYPE_IDENTITY = 0x0;
    public static final int TYPE_ALPHA = 0x1;
    public static final int TYPE_MATRIX = 0x2;
    public static final int TYPE_BOTH = TYPE_ALPHA | TYPE_MATRIX;

    protected float alpha = 1.0f;
    protected int transformationType = TYPE_BOTH;
    private float translateX;
    private float translateY;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float rotation;

    public Transformation() {
    }

    public void clear() {
        alpha = 1.0f;
        translateX = 0f;
        translateY = 0f;
        scaleX = 1.0f;
        scaleY = 1.0f;
        rotation = 0f;
        transformationType = TYPE_BOTH;
    }

    public void set(Transformation t) {
        if (t == null) return;
        this.alpha = t.alpha;
        this.translateX = t.translateX;
        this.translateY = t.translateY;
        this.scaleX = t.scaleX;
        this.scaleY = t.scaleY;
        this.rotation = t.rotation;
        this.transformationType = t.transformationType;
    }

    public void compose(Transformation t) {
        if (t == null) return;
        this.alpha *= t.alpha;
        this.translateX += t.translateX;
        this.translateY += t.translateY;
        this.scaleX *= t.scaleX;
        this.scaleY *= t.scaleY;
        this.rotation += t.rotation;
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public int getTransformationType() {
        return transformationType;
    }

    public void setTransformationType(int transformationType) {
        this.transformationType = transformationType;
    }

    public float getTranslateX() {
        return translateX;
    }

    public float getTranslateY() {
        return translateY;
    }

    public void setTranslation(float tx, float ty) {
        this.translateX = tx;
        this.translateY = ty;
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public void setScale(float sx, float sy) {
        this.scaleX = sx;
        this.scaleY = sy;
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }
}
