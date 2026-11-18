package android.view;

import android.content.Context;
import android.os.Handler;

public class GestureDetector {
    public interface OnGestureListener {
        boolean onDown(MotionEvent e);

        void onShowPress(MotionEvent e);

        boolean onSingleTapUp(MotionEvent e);

        boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY);

        void onLongPress(MotionEvent e);

        boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY);
    }

    public interface OnDoubleTapListener {
        boolean onSingleTapConfirmed(MotionEvent e);

        boolean onDoubleTap(MotionEvent e);

        boolean onDoubleTapEvent(MotionEvent e);
    }

    public interface OnContextClickListener {
        boolean onContextClick(MotionEvent e);
    }

    public static class SimpleOnGestureListener implements OnGestureListener, OnDoubleTapListener, OnContextClickListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onContextClick(MotionEvent e) {
            return false;
        }
    }

    private static final float TOUCH_SLOP = 8f;
    private static final long DOUBLE_TAP_TIMEOUT = 300L;

    private final OnGestureListener listener;
    private OnDoubleTapListener doubleTapListener;
    private OnContextClickListener contextClickListener;
    private final Handler handler;
    private boolean longpressEnabled = true;

    private MotionEvent currentDownEvent;
    private MotionEvent previousUpEvent;
    private float lastFocusX;
    private float lastFocusY;
    private float downFocusX;
    private float downFocusY;
    private float lastMoveDx;
    private float lastMoveDy;
    private long lastMoveDt = 16;
    private long lastMoveTime;
    private boolean alwaysInTapRegion;
    private boolean inLongPress;
    private boolean isDoubleTapping;

    public GestureDetector(OnGestureListener listener) {
        this(null, listener, null);
    }

    public GestureDetector(OnGestureListener listener, Handler handler) {
        this(null, listener, handler);
    }

    public GestureDetector(Context context, OnGestureListener listener) {
        this(context, listener, null);
    }

    public GestureDetector(Context context, OnGestureListener listener, Handler handler) {
        this(context, listener, handler, true);
    }

    public GestureDetector(Context context, OnGestureListener listener, Handler handler, boolean ignoreMultitouch) {
        this.listener = listener;
        this.handler = handler != null ? handler : new Handler();
        if (listener instanceof OnDoubleTapListener dtl) {
            this.doubleTapListener = dtl;
        }
        if (listener instanceof OnContextClickListener ccl) {
            this.contextClickListener = ccl;
        }
    }

    public void setOnDoubleTapListener(OnDoubleTapListener onDoubleTapListener) {
        this.doubleTapListener = onDoubleTapListener;
    }

    public void setContextClickListener(OnContextClickListener onContextClickListener) {
        this.contextClickListener = onContextClickListener;
    }

    public void setIsLongpressEnabled(boolean isLongpressEnabled) {
        this.longpressEnabled = isLongpressEnabled;
    }

    public boolean isLongpressEnabled() {
        return longpressEnabled;
    }

    public boolean onGenericMotionEvent(MotionEvent ev) {
        return false;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (ev == null || listener == null) return false;
        int action = ev.getActionMasked();
        float focusX = ev.getX();
        float focusY = ev.getY();
        boolean handled = false;

        switch (action) {
            case MotionEvent.ACTION_DOWN -> {
                if (doubleTapListener != null && previousUpEvent != null
                        && (ev.getEventTime() - previousUpEvent.getEventTime()) <= DOUBLE_TAP_TIMEOUT) {
                    float dx = focusX - previousUpEvent.getX();
                    float dy = focusY - previousUpEvent.getY();
                    if (dx * dx + dy * dy < 40 * 40) {
                        isDoubleTapping = true;
                        handled |= doubleTapListener.onDoubleTap(ev);
                        handled |= doubleTapListener.onDoubleTapEvent(ev);
                    }
                }
                downFocusX = lastFocusX = focusX;
                downFocusY = lastFocusY = focusY;
                currentDownEvent = MotionEvent.obtain(ev);
                alwaysInTapRegion = true;
                inLongPress = false;
                lastMoveDx = lastMoveDy = 0f;
                lastMoveTime = ev.getEventTime();
                handled |= listener.onDown(ev);
            }
            case MotionEvent.ACTION_MOVE -> {
                if (isDoubleTapping && doubleTapListener != null) {
                    handled |= doubleTapListener.onDoubleTapEvent(ev);
                } else if (!inLongPress) {
                    float scrollX = lastFocusX - focusX;
                    float scrollY = lastFocusY - focusY;
                    if (alwaysInTapRegion) {
                        float dx = focusX - downFocusX;
                        float dy = focusY - downFocusY;
                        if (dx * dx + dy * dy > TOUCH_SLOP * TOUCH_SLOP) {
                            alwaysInTapRegion = false;
                            handled = listener.onScroll(currentDownEvent, ev, scrollX, scrollY);
                            lastFocusX = focusX;
                            lastFocusY = focusY;
                        }
                    } else if (Math.abs(scrollX) >= 1f || Math.abs(scrollY) >= 1f) {
                        handled = listener.onScroll(currentDownEvent, ev, scrollX, scrollY);
                        lastMoveDx = -scrollX;
                        lastMoveDy = -scrollY;
                        long dt = Math.max(1L, ev.getEventTime() - lastMoveTime);
                        lastMoveDt = dt;
                        lastMoveTime = ev.getEventTime();
                        lastFocusX = focusX;
                        lastFocusY = focusY;
                    }
                }
            }
            case MotionEvent.ACTION_UP -> {
                if (isDoubleTapping && doubleTapListener != null) {
                    handled |= doubleTapListener.onDoubleTapEvent(ev);
                } else if (alwaysInTapRegion) {
                    handled = listener.onSingleTapUp(ev);
                    if (doubleTapListener != null) {
                        doubleTapListener.onSingleTapConfirmed(ev);
                    }
                } else {
                    float vx = (lastMoveDx / Math.max(1f, lastMoveDt)) * 1000f;
                    float vy = (lastMoveDy / Math.max(1f, lastMoveDt)) * 1000f;
                    if (Math.hypot(vx, vy) > 50f) {
                        handled = listener.onFling(currentDownEvent, ev, vx, vy);
                    }
                }
                previousUpEvent = MotionEvent.obtain(ev);
                isDoubleTapping = false;
                inLongPress = false;
            }
            case MotionEvent.ACTION_CANCEL -> {
                isDoubleTapping = false;
                inLongPress = false;
            }
        }
        return handled;
    }
}
