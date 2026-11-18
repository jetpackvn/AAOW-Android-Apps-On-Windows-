package android.graphics.drawable;

import java.util.ArrayList;
import java.util.List;

public class StateListDrawable extends DrawableContainer {
    private static final class StateEntry {
        final int[] stateSet;
        final int index;

        StateEntry(int[] stateSet, int index) {
            this.stateSet = stateSet != null ? stateSet.clone() : new int[0];
            this.index = index;
        }

        boolean matches(int[] state) {
            if (stateSet.length == 0) return true;
            if (state == null) return false;
            for (int req : stateSet) {
                boolean wantPresent = req > 0;
                int attr = Math.abs(req);
                boolean found = false;
                for (int s : state) {
                    if (s == attr) {
                        found = true;
                        break;
                    }
                }
                if (wantPresent != found) return false;
            }
            return true;
        }
    }

    private final DrawableContainerState containerState = new DrawableContainerState() {
        @Override
        public Drawable newDrawable() {
            return new StateListDrawable();
        }
    };
    private final List<StateEntry> entries = new ArrayList<>();

    public StateListDrawable() {
        setConstantState(containerState);
    }

    public void addState(int[] stateSet, Drawable drawable) {
        if (drawable == null) return;
        int idx = containerState.addChild(drawable);
        entries.add(new StateEntry(stateSet, idx));
        onStateChange(getState());
    }

    public int getStateCount() {
        return entries.size();
    }

    public int[] getStateSet(int index) {
        return entries.get(index).stateSet;
    }

    public Drawable getStateDrawable(int index) {
        return containerState.getChild(entries.get(index).index);
    }

    public int findStateDrawableIndex(int[] stateSet) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).matches(stateSet)) return i;
        }
        return -1;
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        boolean changed = super.onStateChange(stateSet);
        int idx = findStateDrawableIndex(stateSet);
        if (idx < 0 && !entries.isEmpty()) idx = 0;
        if (idx >= 0) {
            return selectDrawable(entries.get(idx).index) || changed;
        }
        return changed;
    }
}
