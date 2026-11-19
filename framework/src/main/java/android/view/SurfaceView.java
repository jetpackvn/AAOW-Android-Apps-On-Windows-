package android.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cài đặt android.view.SurfaceView sử dụng Bitmap đệm (double buffer) để ứng dụng/game 2D vẽ trực tiếp.
 */
public class SurfaceView extends View {
    private final List<SurfaceHolder.Callback> callbacks = new CopyOnWriteArrayList<>();
    private final Surface surface = new Surface();
    private final Rect surfaceFrame = new Rect();
    private volatile Bitmap frontBuffer;
    private Bitmap backBuffer;
    private Graphics2D lockedGraphics;
    private boolean surfaceReady;
    private boolean creating;

    private final SurfaceHolder holder = new SurfaceHolder() {
        @Override
        public void addCallback(Callback callback) {
            if (callback != null && !callbacks.contains(callback)) {
                callbacks.add(callback);
                if (surfaceReady) {
                    callback.surfaceCreated(this);
                    if (surfaceFrame.width() > 0 && surfaceFrame.height() > 0) {
                        callback.surfaceChanged(this, PixelFormat.RGBA_8888,
                                surfaceFrame.width(), surfaceFrame.height());
                    }
                }
            }
        }

        @Override
        public void removeCallback(Callback callback) {
            callbacks.remove(callback);
        }

        @Override
        public boolean isCreating() {
            return creating;
        }

        @Override
        public void setType(int type) {
        }

        @Override
        public void setFixedSize(int width, int height) {
        }

        @Override
        public void setSizeFromLayout() {
        }

        @Override
        public void setFormat(int format) {
        }

        @Override
        public void setKeepScreenOn(boolean screenOn) {
        }

        @Override
        public synchronized Canvas lockCanvas() {
            return lockCanvas(null);
        }

        @Override
        public synchronized Canvas lockCanvas(Rect dirty) {
            int w = Math.max(1, surfaceFrame.width() > 0 ? surfaceFrame.width() : getWidth());
            int h = Math.max(1, surfaceFrame.height() > 0 ? surfaceFrame.height() : getHeight());
            if (backBuffer == null || backBuffer.getWidth() != w || backBuffer.getHeight() != h) {
                backBuffer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            }
            Graphics2D g2 = backBuffer.getAwtImage().createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (dirty != null) {
                g2.setClip(dirty.left, dirty.top, dirty.width(), dirty.height());
            }
            lockedGraphics = g2;
            return new Canvas(g2);
        }

        @Override
        public synchronized void unlockCanvasAndPost(Canvas canvas) {
            if (lockedGraphics != null) {
                lockedGraphics.dispose();
                lockedGraphics = null;
            }
            if (backBuffer != null) {
                Bitmap copy = Bitmap.createBitmap(backBuffer.getWidth(), backBuffer.getHeight(), Bitmap.Config.ARGB_8888);
                Graphics2D g = copy.getAwtImage().createGraphics();
                g.drawImage(backBuffer.getAwtImage(), 0, 0, null);
                g.dispose();
                frontBuffer = copy;
                postInvalidate();
            }
        }

        @Override
        public Rect getSurfaceFrame() {
            return new Rect(surfaceFrame);
        }

        @Override
        public Surface getSurface() {
            return surface;
        }
    };

    public SurfaceView(Context context) {
        this(context, null, 0, 0);
    }

    public SurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public SurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setWillNotDraw(false);
    }

    public SurfaceHolder getHolder() {
        return holder;
    }

    public void setZOrderOnTop(boolean onTop) {
    }

    public void setZOrderMediaOverlay(boolean isMediaOverlay) {
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;
        surfaceFrame.set(0, 0, w, h);
        if (!surfaceReady) {
            surfaceReady = true;
            creating = true;
            for (SurfaceHolder.Callback cb : callbacks) {
                try {
                    cb.surfaceCreated(holder);
                } catch (Throwable t) {
                    android.util.Log.w("AAOW-SurfaceView", "surfaceCreated error: " + t, t);
                }
            }
            creating = false;
        }
        for (SurfaceHolder.Callback cb : callbacks) {
            try {
                cb.surfaceChanged(holder, PixelFormat.RGBA_8888, w, h);
            } catch (Throwable t) {
                android.util.Log.w("AAOW-SurfaceView", "surfaceChanged error: " + t, t);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap bmp = frontBuffer;
        if (bmp != null) {
            canvas.drawBitmap(bmp, 0, 0, null);
        } else {
            canvas.drawColor(Color.BLACK);
        }
    }
}
