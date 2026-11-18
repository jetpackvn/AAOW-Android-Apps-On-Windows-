package android.view.animation;

import android.content.Context;
import android.graphics.Path;
import android.util.AttributeSet;

public class PathInterpolator extends BaseInterpolator {
    private float x1;
    private float y1;
    private float x2;
    private float y2;

    public PathInterpolator(Path path) {
        this(0.4f, 0f, 0.2f, 1f);
    }

    public PathInterpolator(float controlX, float controlY) {
        this.x1 = controlX;
        this.y1 = controlY;
        this.x2 = controlX;
        this.y2 = controlY;
    }

    public PathInterpolator(float controlX1, float controlY1, float controlX2, float controlY2) {
        this.x1 = controlX1;
        this.y1 = controlY1;
        this.x2 = controlX2;
        this.y2 = controlY2;
    }

    public PathInterpolator(Context context, AttributeSet attrs) {
        this(0.4f, 0f, 0.2f, 1f);
    }

    @Override
    public float getInterpolation(float t) {
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        // Xấp xỉ cubic Bezier (tức thời và mượt 60fps)
        float u = 1f - t;
        float mt = u;
        return 3f * mt * mt * t * y1 + 3f * mt * t * t * y2 + t * t * t;
    }
}
