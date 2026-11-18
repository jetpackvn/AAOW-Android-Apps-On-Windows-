package android.media;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.os.Handler;

import java.awt.Toolkit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Quản lý hệ thống âm thanh Windows cho ứng dụng Android.
 */
public class AudioManager {
    public static final int STREAM_VOICE_CALL = 0;
    public static final int STREAM_SYSTEM = 1;
    public static final int STREAM_RING = 2;
    public static final int STREAM_MUSIC = 3;
    public static final int STREAM_ALARM = 4;
    public static final int STREAM_NOTIFICATION = 5;
    public static final int STREAM_DTMF = 8;
    public static final int STREAM_ACCESSIBILITY = 10;
    public static final int USE_DEFAULT_STREAM_TYPE = Integer.MIN_VALUE;

    public static final int ADJUST_LOWER = -1;
    public static final int ADJUST_SAME = 0;
    public static final int ADJUST_RAISE = 1;
    public static final int ADJUST_MUTE = -100;
    public static final int ADJUST_UNMUTE = 100;
    public static final int ADJUST_TOGGLE_MUTE = 101;

    public static final int AUDIOFOCUS_NONE = 0;
    public static final int AUDIOFOCUS_GAIN = 1;
    public static final int AUDIOFOCUS_GAIN_TRANSIENT = 2;
    public static final int AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK = 3;
    public static final int AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE = 4;
    public static final int AUDIOFOCUS_LOSS = -1;
    public static final int AUDIOFOCUS_LOSS_TRANSIENT = -2;
    public static final int AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK = -3;
    public static final int AUDIOFOCUS_REQUEST_FAILED = 0;
    public static final int AUDIOFOCUS_REQUEST_GRANTED = 1;
    public static final int AUDIOFOCUS_REQUEST_DELAYED = 2;

    public static final int MODE_NORMAL = 0;
    public static final int MODE_RINGTONE = 1;
    public static final int MODE_IN_CALL = 2;
    public static final int MODE_IN_COMMUNICATION = 3;

    public static final int RINGER_MODE_SILENT = 0;
    public static final int RINGER_MODE_VIBRATE = 1;
    public static final int RINGER_MODE_NORMAL = 2;

    public static final int FX_KEY_CLICK = 0;
    public static final int FX_FOCUS_NAVIGATION_UP = 1;
    public static final int FX_FOCUS_NAVIGATION_DOWN = 2;
    public static final int FX_FOCUS_NAVIGATION_LEFT = 3;
    public static final int FX_FOCUS_NAVIGATION_RIGHT = 4;
    public static final int FX_KEYPRESS_STANDARD = 5;
    public static final int FX_KEYPRESS_SPACEBAR = 6;
    public static final int FX_KEYPRESS_DELETE = 7;
    public static final int FX_KEYPRESS_RETURN = 8;
    public static final int FX_KEYPRESS_INVALID = 9;
    public static final int FX_BACK = 10;

    public static final String PROPERTY_OUTPUT_SAMPLE_RATE = "android.media.property.OUTPUT_SAMPLE_RATE";
    public static final String PROPERTY_OUTPUT_FRAMES_PER_BUFFER = "android.media.property.OUTPUT_FRAMES_PER_BUFFER";

    public interface OnAudioFocusChangeListener {
        void onAudioFocusChange(int focusChange);
    }

    private final Map<Integer, Integer> streamVolumes = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> streamMuted = new ConcurrentHashMap<>();
    private final AtomicInteger mode = new AtomicInteger(MODE_NORMAL);
    private final AtomicInteger ringerMode = new AtomicInteger(RINGER_MODE_NORMAL);
    private final AtomicInteger sessionGen = new AtomicInteger(1);
    private final AtomicBoolean micMuted = new AtomicBoolean(false);
    private final AtomicBoolean speakerphoneOn = new AtomicBoolean(true);
    private volatile boolean musicActive;
    private volatile OnAudioFocusChangeListener activeFocusListener;

    public AudioManager() {
        for (int s = 0; s <= 10; s++) {
            streamVolumes.put(s, 11);
            streamMuted.put(s, false);
        }
    }

    public int getStreamMaxVolume(int streamType) {
        return 15;
    }

    public int getStreamMinVolume(int streamType) {
        return 0;
    }

    public int getStreamVolume(int streamType) {
        return streamVolumes.getOrDefault(streamType, 11);
    }

    public void setStreamVolume(int streamType, int index, int flags) {
        int clamped = Math.max(0, Math.min(getStreamMaxVolume(streamType), index));
        streamVolumes.put(streamType, clamped);
    }

    public void adjustStreamVolume(int streamType, int direction, int flags) {
        int cur = getStreamVolume(streamType);
        switch (direction) {
            case ADJUST_RAISE -> setStreamVolume(streamType, cur + 1, flags);
            case ADJUST_LOWER -> setStreamVolume(streamType, cur - 1, flags);
            case ADJUST_MUTE -> streamMuted.put(streamType, true);
            case ADJUST_UNMUTE -> streamMuted.put(streamType, false);
            case ADJUST_TOGGLE_MUTE -> streamMuted.put(streamType, !isStreamMute(streamType));
            default -> {
            }
        }
    }

    public void adjustVolume(int direction, int flags) {
        adjustStreamVolume(STREAM_MUSIC, direction, flags);
    }

    public void adjustSuggestedStreamVolume(int direction, int suggestedStreamType, int flags) {
        int stream = suggestedStreamType == USE_DEFAULT_STREAM_TYPE ? STREAM_MUSIC : suggestedStreamType;
        adjustStreamVolume(stream, direction, flags);
    }

    public boolean isStreamMute(int streamType) {
        return streamMuted.getOrDefault(streamType, false);
    }

    public void setStreamMute(int streamType, boolean state) {
        streamMuted.put(streamType, state);
    }

    public void setStreamSolo(int streamType, boolean state) {
    }

    public int getMode() {
        return mode.get();
    }

    public void setMode(int mode) {
        this.mode.set(mode);
    }

    public int getRingerMode() {
        return ringerMode.get();
    }

    public void setRingerMode(int ringerMode) {
        this.ringerMode.set(ringerMode);
    }

    public boolean isMicrophoneMute() {
        return micMuted.get();
    }

    public void setMicrophoneMute(boolean on) {
        micMuted.set(on);
    }

    public boolean isSpeakerphoneOn() {
        return speakerphoneOn.get();
    }

    public void setSpeakerphoneOn(boolean on) {
        speakerphoneOn.set(on);
    }

    public boolean isWiredHeadsetOn() {
        return false;
    }

    public boolean isBluetoothA2dpOn() {
        return false;
    }

    public boolean isBluetoothScoOn() {
        return false;
    }

    public boolean isMusicActive() {
        return musicActive;
    }

    public void setMusicActiveInternal(boolean active) {
        this.musicActive = active;
    }

    public boolean isVolumeFixed() {
        return false;
    }

    public int generateAudioSessionId() {
        return sessionGen.getAndIncrement();
    }

    public int requestAudioFocus(OnAudioFocusChangeListener l, int streamType, int durationHint) {
        if (activeFocusListener != null && activeFocusListener != l) {
            try {
                activeFocusListener.onAudioFocusChange(AUDIOFOCUS_LOSS_TRANSIENT);
            } catch (Throwable ignored) {
            }
        }
        activeFocusListener = l;
        return AUDIOFOCUS_REQUEST_GRANTED;
    }

    public int requestAudioFocus(AudioFocusRequest focusRequest) {
        OnAudioFocusChangeListener l = focusRequest != null ? focusRequest.getListener() : null;
        int hint = focusRequest != null ? focusRequest.getFocusGain() : AUDIOFOCUS_GAIN;
        return requestAudioFocus(l, STREAM_MUSIC, hint);
    }

    public int abandonAudioFocus(OnAudioFocusChangeListener l) {
        if (activeFocusListener == l) {
            activeFocusListener = null;
        }
        return AUDIOFOCUS_REQUEST_GRANTED;
    }

    public int abandonAudioFocusRequest(AudioFocusRequest focusRequest) {
        return abandonAudioFocus(focusRequest != null ? focusRequest.getListener() : null);
    }

    public void playSoundEffect(int effectType) {
        playSoundEffect(effectType, -1f);
    }

    public void playSoundEffect(int effectType, float volume) {
        if (effectType == FX_KEYPRESS_INVALID) {
            try {
                Toolkit.getDefaultToolkit().beep();
            } catch (Throwable ignored) {
            }
        }
    }

    public void loadSoundEffects() {
    }

    public void unloadSoundEffects() {
    }

    public String getProperty(String key) {
        if (PROPERTY_OUTPUT_SAMPLE_RATE.equals(key)) return "44100";
        if (PROPERTY_OUTPUT_FRAMES_PER_BUFFER.equals(key)) return "512";
        return null;
    }

    public String getParameters(String keys) {
        return "";
    }

    public void setParameters(String keyValuePairs) {
    }

    public void registerMediaButtonEventReceiver(ComponentName eventReceiver) {
    }

    public void registerMediaButtonEventReceiver(PendingIntent eventReceiver) {
    }

    public void unregisterMediaButtonEventReceiver(ComponentName eventReceiver) {
    }

    public void unregisterMediaButtonEventReceiver(PendingIntent eventReceiver) {
    }

    public void registerAudioDeviceCallback(Object callback, Handler handler) {
    }

    public void unregisterAudioDeviceCallback(Object callback) {
    }
}
