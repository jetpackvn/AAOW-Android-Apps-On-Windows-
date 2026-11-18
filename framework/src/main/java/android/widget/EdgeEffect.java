package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

public class EdgeEffect {
    private int color = 0x6633B5E5;
    private int width;
    private int height;
    private float distance;

    public EdgeEffect(Context context) {
    }

    public EdgeEffect(Context context, AttributeSet attrs) {
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public boolean isFinished() {
        return distance == 0f;
    }

    public void finish() {
        distance = 0f;
    }

    public void onPull(float deltaDistance) {
        onPull(deltaDistance, 0.5f);
    }

    public void onPull(float deltaDistance, float displacement) {
        distance = Math.max(0f, Math.min(1f, distance + Math.abs(deltaDistance)));
    }

    public float onPullDistance(float deltaDistance, float displacement) {
        float prev = distance;
        distance = Math.max(0f, Math.min(1f, distance + deltaDistance));
        return distance - prev;
    }

    public float getDistance() {
        return distance;
    }

    public void onRelease() {
        distance = 0f;
    }

    public void onAbsorb(int velocity) {
        distance = 0f;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public int getMaxHeight() {
        return Math.max(0, height / 4);
    }

    public boolean draw(Canvas canvas) {
        distance = 0f;
        return false;
    }
}
