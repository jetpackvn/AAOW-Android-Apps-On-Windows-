package android.media;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.os.Handler;
import android.os.Looper;

import javax.sound.sampled.Clip;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cài đặt thật android.media.SoundPool phát hiệu ứng âm thanh qua Windows Audio.
 */
public class SoundPool {
    public interface OnLoadCompleteListener {
        void onLoadComplete(SoundPool soundPool, int sampleId, int status);
    }

    private record LoadedSample(int id, WindowsAudioHelper.DecodedPcm pcm, byte[] rawBytes) {
    }

    private static final class ActiveStream {
        final int streamId;
        final int sampleId;
        final Clip clip;
        volatile boolean paused;

        ActiveStream(int streamId, int sampleId, Clip clip) {
            this.streamId = streamId;
            this.sampleId = sampleId;
            this.clip = clip;
        }
    }

    private final int maxStreams;
    private final AtomicInteger nextSampleId = new AtomicInteger(1);
    private final AtomicInteger nextStreamId = new AtomicInteger(1);
    private final Map<Integer, LoadedSample> samples = new ConcurrentHashMap<>();
    private final Map<Integer, ActiveStream> activeStreams = new ConcurrentHashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile OnLoadCompleteListener listener;
    private volatile boolean released;

    public SoundPool(int maxStreams, int streamType, int srcQuality) {
        this.maxStreams = Math.max(1, maxStreams);
    }

    public int load(Context context, int resId, int priority) {
        byte[] bytes = null;
        if (context != null && context.getResources() != null) {
            try (InputStream in = context.getResources().openRawResource(resId)) {
                bytes = in.readAllBytes();
            } catch (Throwable ignored) {
            }
        }
        return registerSampleBytes(bytes);
    }

    public int load(String path, int priority) {
        byte[] bytes = null;
        if (path != null) {
            try {
                bytes = Files.readAllBytes(new File(path).toPath());
            } catch (Throwable ignored) {
            }
        }
        return registerSampleBytes(bytes);
    }

    public int load(AssetFileDescriptor afd, int priority) {
        byte[] bytes = null;
        if (afd != null) {
            try (InputStream in = afd.createInputStream()) {
                bytes = in.readAllBytes();
            } catch (Throwable ignored) {
            }
        }
        return registerSampleBytes(bytes);
    }

    public int load(FileDescriptor fd, long offset, long length, int priority) {
        byte[] bytes = null;
        if (fd != null) {
            try (FileInputStream fis = new FileInputStream(fd)) {
                if (offset > 0) fis.skipNBytes(offset);
                bytes = length > 0 ? fis.readNBytes((int) Math.min(Integer.MAX_VALUE, length)) : fis.readAllBytes();
            } catch (Throwable ignored) {
            }
        }
        return registerSampleBytes(bytes);
    }

    private int registerSampleBytes(byte[] bytes) {
        int id = nextSampleId.getAndIncrement();
        WindowsAudioHelper.DecodedPcm pcm = WindowsAudioHelper.decodeToPcm(bytes);
        int status = (bytes != null && bytes.length > 0) ? 0 : -1;
        samples.put(id, new LoadedSample(id, pcm, bytes));
        OnLoadCompleteListener l = listener;
        if (l != null) {
            mainHandler.post(() -> l.onLoadComplete(this, id, status));
        }
        return id;
    }

    public final int play(int soundID, float leftVolume, float rightVolume, int priority, int loop, float rate) {
        if (released) return 0;
        LoadedSample sample = samples.get(soundID);
        if (sample == null) return 0;

        int streamId = nextStreamId.getAndIncrement();
        if (activeStreams.size() >= maxStreams) {
            Integer oldest = activeStreams.keySet().stream().findFirst().orElse(null);
            if (oldest != null) stop(oldest);
        }

        Clip clip = WindowsAudioHelper.openClip(sample.pcm());
        ActiveStream stream = new ActiveStream(streamId, soundID, clip);
        activeStreams.put(streamId, stream);
        if (clip != null) {
            WindowsAudioHelper.applyGain(clip, leftVolume, rightVolume);
            clip.addLineListener(ev -> {
                if (ev.getType() == javax.sound.sampled.LineEvent.Type.STOP && !stream.paused) {
                    activeStreams.remove(streamId);
                    try {
                        clip.close();
                    } catch (Throwable ignored) {
                    }
                }
            });
            if (loop < 0) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } else if (loop > 0) {
                clip.loop(loop);
            } else {
                clip.start();
            }
        }
        return streamId;
    }

    public final void stop(int streamID) {
        ActiveStream s = activeStreams.remove(streamID);
        if (s != null && s.clip != null) {
            try {
                s.clip.stop();
                s.clip.close();
            } catch (Throwable ignored) {
            }
        }
    }

    public final void pause(int streamID) {
        ActiveStream s = activeStreams.get(streamID);
        if (s != null && s.clip != null) {
            s.paused = true;
            s.clip.stop();
        }
    }

    public final void resume(int streamID) {
        ActiveStream s = activeStreams.get(streamID);
        if (s != null && s.clip != null && s.paused) {
            s.paused = false;
            s.clip.start();
        }
    }

    public final void autoPause() {
        for (ActiveStream s : activeStreams.values()) {
            if (s.clip != null && s.clip.isRunning()) {
                s.paused = true;
                s.clip.stop();
            }
        }
    }

    public final void autoResume() {
        for (ActiveStream s : activeStreams.values()) {
            if (s.clip != null && s.paused) {
                s.paused = false;
                s.clip.start();
            }
        }
    }

    public final void setVolume(int streamID, float leftVolume, float rightVolume) {
        ActiveStream s = activeStreams.get(streamID);
        if (s != null && s.clip != null) {
            WindowsAudioHelper.applyGain(s.clip, leftVolume, rightVolume);
        }
    }

    public final void setLoop(int streamID, int loop) {
        ActiveStream s = activeStreams.get(streamID);
        if (s != null && s.clip != null) {
            s.clip.loop(loop < 0 ? Clip.LOOP_CONTINUOUSLY : loop);
        }
    }

    public final void setRate(int streamID, float rate) {
    }

    public final void setPriority(int streamID, int priority) {
    }

    public final boolean unload(int soundID) {
        return samples.remove(soundID) != null;
    }

    public final void release() {
        released = true;
        for (Integer id : activeStreams.keySet()) {
            stop(id);
        }
        activeStreams.clear();
        samples.clear();
    }

    public void setOnLoadCompleteListener(OnLoadCompleteListener listener) {
        this.listener = listener;
    }

    public static class Builder {
        private int maxStreams = 1;

        public Builder setMaxStreams(int maxStreams) {
            this.maxStreams = Math.max(1, maxStreams);
            return this;
        }

        public Builder setAudioAttributes(AudioAttributes attributes) {
            return this;
        }

        public SoundPool build() {
            return new SoundPool(maxStreams, 0, 0);
        }
    }
}
