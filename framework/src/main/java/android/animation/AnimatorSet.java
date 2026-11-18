package android.animation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class AnimatorSet extends Animator {
    public class Builder {
        private final Animator current;

        Builder(Animator anim) {
            this.current = anim;
            addChild(anim);
        }

        public Builder with(Animator anim) {
            if (anim != null) {
                addChild(anim);
                anim.setStartDelay(Math.max(anim.getStartDelay(), current.getStartDelay()));
            }
            return this;
        }

        public Builder before(Animator anim) {
            if (anim != null) {
                addChild(anim);
                long delay = current.getStartDelay() + Math.max(0L, current.getDuration());
                anim.setStartDelay(Math.max(anim.getStartDelay(), delay));
            }
            return this;
        }

        public Builder after(Animator anim) {
            if (anim != null) {
                addChild(anim);
                long delay = anim.getStartDelay() + Math.max(0L, anim.getDuration());
                current.setStartDelay(Math.max(current.getStartDelay(), delay));
            }
            return this;
        }

        public Builder after(long delay) {
            current.setStartDelay(Math.max(current.getStartDelay(), delay));
            return this;
        }
    }

    private final ArrayList<Animator> children = new ArrayList<>();
    private long duration = -1L;
    private long startDelay = 0L;
    private TimeInterpolator interpolator;
    private boolean started;
    private boolean running;
    private int pendingEndCount;

    private void addChild(Animator anim) {
        if (anim != null && !children.contains(anim)) {
            children.add(anim);
        }
    }

    public Builder play(Animator anim) {
        return anim != null ? new Builder(anim) : null;
    }

    public void playTogether(Animator... items) {
        if (items != null) {
            playTogether(Arrays.asList(items));
        }
    }

    public void playTogether(Collection<Animator> items) {
        if (items == null) return;
        for (Animator a : items) {
            addChild(a);
        }
    }

    public void playSequentially(Animator... items) {
        if (items != null) {
            playSequentially(Arrays.asList(items));
        }
    }

    public void playSequentially(List<Animator> items) {
        if (items == null || items.isEmpty()) return;
        long offset = 0L;
        for (Animator a : items) {
            if (a == null) continue;
            addChild(a);
            a.setStartDelay(a.getStartDelay() + offset);
            long d = a.getDuration();
            if (d > 0) offset += d;
        }
    }

    public ArrayList<Animator> getChildAnimations() {
        return new ArrayList<>(children);
    }

    @Override
    public long getStartDelay() {
        return startDelay;
    }

    @Override
    public void setStartDelay(long startDelay) {
        this.startDelay = Math.max(0L, startDelay);
    }

    @Override
    public AnimatorSet setDuration(long duration) {
        this.duration = duration;
        if (duration >= 0) {
            for (Animator a : children) {
                a.setDuration(duration);
            }
        }
        return this;
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public void setInterpolator(TimeInterpolator interpolator) {
        this.interpolator = interpolator;
        for (Animator a : children) {
            a.setInterpolator(interpolator);
        }
    }

    @Override
    public TimeInterpolator getInterpolator() {
        return interpolator;
    }

    @Override
    public void setTarget(Object target) {
        for (Animator a : children) {
            a.setTarget(target);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void start() {
        started = true;
        running = true;
        notifyStart(false);
        if (children.isEmpty()) {
            running = false;
            started = false;
            notifyEnd(false);
            return;
        }
        pendingEndCount = children.size();
        for (Animator child : List.copyOf(children)) {
            if (startDelay > 0) {
                child.setStartDelay(child.getStartDelay() + startDelay);
            }
            child.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    child.removeListener(this);
                    if (--pendingEndCount <= 0 && running) {
                        running = false;
                        started = false;
                        notifyEnd(false);
                    }
                }
            });
            child.start();
        }
    }

    @Override
    public void cancel() {
        if (!started && !running) return;
        running = false;
        started = false;
        for (Animator child : List.copyOf(children)) {
            child.cancel();
        }
        notifyCancel();
        notifyEnd(false);
    }

    @Override
    public void end() {
        for (Animator child : List.copyOf(children)) {
            child.end();
        }
        running = false;
        started = false;
    }

    @Override
    public AnimatorSet clone() {
        AnimatorSet copy = (AnimatorSet) super.clone();
        copy.children.clear();
        for (Animator a : this.children) {
            copy.children.add(a.clone());
        }
        copy.started = false;
        copy.running = false;
        return copy;
    }
}
