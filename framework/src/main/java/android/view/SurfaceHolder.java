package android.view;

import android.graphics.Canvas;
import android.graphics.Rect;

public interface SurfaceHolder {
    interface Callback {
        void surfaceCreated(SurfaceHolder holder);

        void surfaceChanged(SurfaceHolder holder, int format, int width, int height);

        void surfaceDestroyed(SurfaceHolder holder);
    }

    interface Callback2 extends Callback {
        void surfaceRedrawNeeded(SurfaceHolder holder);

        default void surfaceRedrawNeededAsync(SurfaceHolder holder, Runnable drawingFinished) {
            surfaceRedrawNeeded(holder);
            if (drawingFinished != null) drawingFinished.run();
        }
    }

    void addCallback(Callback callback);

    void removeCallback(Callback callback);

    boolean isCreating();

    void setType(int type);

    void setFixedSize(int width, int height);

    void setSizeFromLayout();

    void setFormat(int format);

    void setKeepScreenOn(boolean screenOn);

    Canvas lockCanvas();

    Canvas lockCanvas(Rect dirty);

    void unlockCanvasAndPost(Canvas canvas);

    Rect getSurfaceFrame();

    Surface getSurface();
}
