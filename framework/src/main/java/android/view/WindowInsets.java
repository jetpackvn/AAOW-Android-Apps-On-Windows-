package android.view;

import android.graphics.Insets;
import android.graphics.Rect;

public final class WindowInsets {
    public static final WindowInsets CONSUMED = new WindowInsets(Insets.NONE, true);

    private final Insets systemWindowInsets;
    private final Insets stableInsets;
    private final boolean consumed;

    public WindowInsets(WindowInsets src) {
        if (src != null) {
            this.systemWindowInsets = src.systemWindowInsets;
            this.stableInsets = src.stableInsets;
            this.consumed = src.consumed;
        } else {
            this.systemWindowInsets = Insets.NONE;
            this.stableInsets = Insets.NONE;
            this.consumed = false;
        }
    }

    public WindowInsets(Insets systemWindowInsets, boolean consumed) {
        this.systemWindowInsets = systemWindowInsets != null ? systemWindowInsets : Insets.NONE;
        this.stableInsets = this.systemWindowInsets;
        this.consumed = consumed;
    }

    public int getSystemWindowInsetLeft() {
        return systemWindowInsets.left;
    }

    public int getSystemWindowInsetTop() {
        return systemWindowInsets.top;
    }

    public int getSystemWindowInsetRight() {
        return systemWindowInsets.right;
    }

    public int getSystemWindowInsetBottom() {
        return systemWindowInsets.bottom;
    }

    public Insets getSystemWindowInsets() {
        return systemWindowInsets;
    }

    public Insets getInsets(int typeMask) {
        return consumed ? Insets.NONE : systemWindowInsets;
    }

    public Insets getSystemGestureInsets() {
        return consumed ? Insets.NONE : systemWindowInsets;
    }

    public Insets getMandatorySystemGestureInsets() {
        return consumed ? Insets.NONE : systemWindowInsets;
    }

    public Insets getTappableElementInsets() {
        return Insets.NONE;
    }

    public Insets getInsetsIgnoringVisibility(int typeMask) {
        return stableInsets;
    }

    public int getStableInsetLeft() {
        return stableInsets.left;
    }

    public int getStableInsetTop() {
        return stableInsets.top;
    }

    public int getStableInsetRight() {
        return stableInsets.right;
    }

    public int getStableInsetBottom() {
        return stableInsets.bottom;
    }

    public Insets getStableInsets() {
        return stableInsets;
    }

    public boolean hasSystemWindowInsets() {
        return !Insets.NONE.equals(systemWindowInsets);
    }

    public boolean hasStableInsets() {
        return !Insets.NONE.equals(stableInsets);
    }

    public boolean hasInsets() {
        return hasSystemWindowInsets() || hasStableInsets();
    }

    public boolean isConsumed() {
        return consumed;
    }

    public boolean isRound() {
        return false;
    }

    public boolean isVisible(int typeMask) {
        return true;
    }

    public WindowInsets consumeSystemWindowInsets() {
        return CONSUMED;
    }

    public WindowInsets consumeStableInsets() {
        return new WindowInsets(systemWindowInsets, consumed);
    }

    public WindowInsets consumeDisplayCutout() {
        return this;
    }

    public WindowInsets replaceSystemWindowInsets(int left, int top, int right, int bottom) {
        return new WindowInsets(Insets.of(left, top, right, bottom), false);
    }

    public WindowInsets replaceSystemWindowInsets(Rect systemWindowInsets) {
        return replaceSystemWindowInsets(
                systemWindowInsets != null ? systemWindowInsets.left : 0,
                systemWindowInsets != null ? systemWindowInsets.top : 0,
                systemWindowInsets != null ? systemWindowInsets.right : 0,
                systemWindowInsets != null ? systemWindowInsets.bottom : 0);
    }

    public WindowInsets inset(int left, int top, int right, int bottom) {
        return new WindowInsets(Insets.of(
                Math.max(0, systemWindowInsets.left - left),
                Math.max(0, systemWindowInsets.top - top),
                Math.max(0, systemWindowInsets.right - right),
                Math.max(0, systemWindowInsets.bottom - bottom)), consumed);
    }

    public WindowInsets inset(Insets insets) {
        return insets == null ? this : inset(insets.left, insets.top, insets.right, insets.bottom);
    }

    public static final class Builder {
        private Insets systemWindowInsets = Insets.NONE;
        private Insets stableInsets = Insets.NONE;
        private boolean consumed;

        public Builder() {
        }

        public Builder(WindowInsets insets) {
            if (insets != null) {
                this.systemWindowInsets = insets.systemWindowInsets;
                this.stableInsets = insets.stableInsets;
                this.consumed = insets.consumed;
            }
        }

        public Builder setSystemWindowInsets(Insets insets) {
            this.systemWindowInsets = insets != null ? insets : Insets.NONE;
            return this;
        }

        public Builder setStableInsets(Insets insets) {
            this.stableInsets = insets != null ? insets : Insets.NONE;
            return this;
        }

        public Builder setInsets(int typeMask, Insets insets) {
            if (insets != null) {
                this.systemWindowInsets = insets;
            }
            return this;
        }

        public Builder setInsetsIgnoringVisibility(int typeMask, Insets insets) {
            if (insets != null) {
                this.stableInsets = insets;
            }
            return this;
        }

        public Builder setSystemGestureInsets(Insets insets) {
            return this;
        }

        public Builder setMandatorySystemGestureInsets(Insets insets) {
            return this;
        }

        public Builder setTappableElementInsets(Insets insets) {
            return this;
        }

        public Builder setVisible(int typeMask, boolean visible) {
            return this;
        }

        public WindowInsets build() {
            WindowInsets wi = new WindowInsets(systemWindowInsets, consumed);
            return wi;
        }
    }
}
