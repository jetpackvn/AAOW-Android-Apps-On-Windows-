package android.media;

import android.os.Handler;

public final class AudioFocusRequest {
    private int focusGain;
    private AudioAttributes audioAttributes;
    private boolean acceptsDelayedFocusGain;
    private boolean willPauseWhenDucked;
    private AudioManager.OnAudioFocusChangeListener listener;
    private Handler listenerHandler;

    AudioFocusRequest() {
    }

    public int getFocusGain() {
        return focusGain;
    }

    public AudioAttributes getAudioAttributes() {
        return audioAttributes;
    }

    public boolean acceptsDelayedFocusGain() {
        return acceptsDelayedFocusGain;
    }

    public boolean willPauseWhenDucked() {
        return willPauseWhenDucked;
    }

    AudioManager.OnAudioFocusChangeListener getListener() {
        return listener;
    }

    public static final class Builder {
        private final AudioFocusRequest req = new AudioFocusRequest();

        public Builder(int focusGain) {
            req.focusGain = focusGain;
        }

        public Builder(AudioFocusRequest requestToCopy) {
            if (requestToCopy != null) {
                req.focusGain = requestToCopy.focusGain;
                req.audioAttributes = requestToCopy.audioAttributes;
                req.acceptsDelayedFocusGain = requestToCopy.acceptsDelayedFocusGain;
                req.willPauseWhenDucked = requestToCopy.willPauseWhenDucked;
                req.listener = requestToCopy.listener;
                req.listenerHandler = requestToCopy.listenerHandler;
            }
        }

        public Builder setFocusGain(int focusGain) {
            req.focusGain = focusGain;
            return this;
        }

        public Builder setAudioAttributes(AudioAttributes attributes) {
            req.audioAttributes = attributes;
            return this;
        }

        public Builder setAcceptsDelayedFocusGain(boolean acceptsDelayedFocusGain) {
            req.acceptsDelayedFocusGain = acceptsDelayedFocusGain;
            return this;
        }

        public Builder setWillPauseWhenDucked(boolean pauseOnDuck) {
            req.willPauseWhenDucked = pauseOnDuck;
            return this;
        }

        public Builder setForceDucking(boolean forceDucking) {
            return this;
        }

        public Builder setOnAudioFocusChangeListener(AudioManager.OnAudioFocusChangeListener listener) {
            req.listener = listener;
            return this;
        }

        public Builder setOnAudioFocusChangeListener(AudioManager.OnAudioFocusChangeListener listener, Handler handler) {
            req.listener = listener;
            req.listenerHandler = handler;
            return this;
        }

        public AudioFocusRequest build() {
            return req;
        }
    }
}
