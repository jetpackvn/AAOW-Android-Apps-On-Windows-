package android.media;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.SurfaceHolder;
import dev.aaow.rt.WebViewEngineBridge;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.util.Duration;

import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Cài đặt thật android.media.MediaPlayer phát âm thanh qua Windows Audio (javax.sound.sampled.Clip + JavaFX Media).
 */
public class MediaPlayer implements AudioRouting, VolumeAutomation {
    public static final int MEDIA_ERROR_UNKNOWN = 1;
    public static final int MEDIA_ERROR_SERVER_DIED = 100;
    public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;
    public static final int MEDIA_ERROR_IO = -1004;
    public static final int MEDIA_ERROR_MALFORMED = -1007;
    public static final int MEDIA_ERROR_UNSUPPORTED = -1010;
    public static final int MEDIA_ERROR_TIMED_OUT = -110;

    public interface OnPreparedListener {
        void onPrepared(MediaPlayer mp);
    }

    public interface OnCompletionListener {
        void onCompletion(MediaPlayer mp);
    }

    public interface OnErrorListener {
        boolean onError(MediaPlayer mp, int what, int extra);
    }

    public interface OnSeekCompleteListener {
        void onSeekComplete(MediaPlayer mp);
    }

    public interface OnBufferingUpdateListener {
        void onBufferingUpdate(MediaPlayer mp, int percent);
    }

    public interface OnInfoListener {
        boolean onInfo(MediaPlayer mp, int what, int extra);
    }

    public interface OnVideoSizeChangedListener {
        void onVideoSizeChanged(MediaPlayer mp, int width, int height);
    }

    private enum State {
        IDLE, INITIALIZED, PREPARING, PREPARED, STARTED, PAUSED, STOPPED, COMPLETED, END
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile State state = State.IDLE;
    private volatile boolean looping;
    private volatile float leftVolume = 1.0f;
    private volatile float rightVolume = 1.0f;
    private volatile int durationMs = 0;
    private volatile int pausePositionMs = 0;
    private volatile int audioSessionId = 1;
    private AudioAttributes audioAttributes = new AudioAttributes.Builder().build();

    private byte[] rawSourceBytes;
    private String sourceUriString;
    private Clip clip;
    private javafx.scene.media.MediaPlayer fxPlayer;
    private volatile boolean manualStop;

    private OnPreparedListener onPreparedListener;
    private OnCompletionListener onCompletionListener;
    private OnErrorListener onErrorListener;
    private OnSeekCompleteListener onSeekCompleteListener;
    private OnBufferingUpdateListener onBufferingUpdateListener;
    private OnInfoListener onInfoListener;
    private OnVideoSizeChangedListener onVideoSizeChangedListener;

    public MediaPlayer() {
    }

    public static MediaPlayer create(Context context, int resid) {
        return create(context, resid, null, 0);
    }

    public static MediaPlayer create(Context context, int resid, AudioAttributes audioAttributes, int audioSessionId) {
        if (context == null) return null;
        try (AssetFileDescriptor afd = context.getResources().openRawResourceFd(resid)) {
            MediaPlayer mp = new MediaPlayer();
            if (audioAttributes != null) mp.setAudioAttributes(audioAttributes);
            if (audioSessionId > 0) mp.setAudioSessionId(audioSessionId);
            mp.setDataSource(afd);
            mp.prepare();
            return mp;
        } catch (Throwable t) {
            try {
                byte[] bytes = context.getResources().openRawResource(resid).readAllBytes();
                MediaPlayer mp = new MediaPlayer();
                if (audioAttributes != null) mp.setAudioAttributes(audioAttributes);
                mp.rawSourceBytes = bytes;
                mp.state = State.INITIALIZED;
                mp.prepare();
                return mp;
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    public static MediaPlayer create(Context context, Uri uri) {
        return create(context, uri, null);
    }

    public static MediaPlayer create(Context context, Uri uri, SurfaceHolder holder) {
        return create(context, uri, holder, null, 0);
    }

    public static MediaPlayer create(Context context, Uri uri, SurfaceHolder holder,
                                     AudioAttributes audioAttributes, int audioSessionId) {
        if (context == null || uri == null) return null;
        try {
            MediaPlayer mp = new MediaPlayer();
            if (holder != null) mp.setDisplay(holder);
            if (audioAttributes != null) mp.setAudioAttributes(audioAttributes);
            if (audioSessionId > 0) mp.setAudioSessionId(audioSessionId);
            mp.setDataSource(context, uri);
            mp.prepare();
            return mp;
        } catch (Throwable t) {
            return null;
        }
    }

    public void setDataSource(String path) throws IOException {
        if (path == null || path.isEmpty()) throw new IllegalArgumentException("Empty media path");
        this.rawSourceBytes = null;
        this.sourceUriString = null;
        if (path.startsWith("http://") || path.startsWith("https://")) {
            this.sourceUriString = path;
        } else if (path.startsWith("file://")) {
            Path p = Path.of(URI.create(path));
            this.rawSourceBytes = Files.readAllBytes(p);
            this.sourceUriString = p.toUri().toString();
        } else {
            File f = new File(path);
            if (!f.exists()) throw new IOException("File not found: " + path);
            this.rawSourceBytes = Files.readAllBytes(f.toPath());
            this.sourceUriString = f.toURI().toString();
        }
        this.state = State.INITIALIZED;
    }

    public void setDataSource(Context context, Uri uri) throws IOException {
        setDataSource(context, uri, null);
    }

    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException {
        if (uri == null) throw new IllegalArgumentException("Null URI");
        String scheme = uri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            this.sourceUriString = uri.toString();
            this.rawSourceBytes = null;
            this.state = State.INITIALIZED;
            return;
        }
        if ("file".equalsIgnoreCase(scheme)) {
            setDataSource(uri.getPath());
            return;
        }
        if ("android.resource".equalsIgnoreCase(scheme) && context != null) {
            String last = uri.getLastPathSegment();
            int resId = 0;
            try {
                resId = Integer.parseInt(last);
            } catch (Exception ignored) {
                resId = context.getResources().getIdentifier(last, "raw", context.getPackageName());
            }
            if (resId != 0) {
                try (InputStream in = context.getResources().openRawResource(resId)) {
                    this.rawSourceBytes = in.readAllBytes();
                    this.state = State.INITIALIZED;
                    return;
                }
            }
        }
        if ("content".equalsIgnoreCase(scheme) && context != null) {
            ContentResolver cr = context.getContentResolver();
            try (InputStream in = cr.openInputStream(uri)) {
                if (in != null) {
                    this.rawSourceBytes = in.readAllBytes();
                    this.state = State.INITIALIZED;
                    return;
                }
            }
        }
        setDataSource(uri.toString());
    }

    public void setDataSource(AssetFileDescriptor afd) throws IOException {
        if (afd == null) throw new IllegalArgumentException("Null AssetFileDescriptor");
        try (InputStream in = afd.createInputStream()) {
            this.rawSourceBytes = in.readAllBytes();
            if (afd.getParcelFileDescriptor() != null && afd.getParcelFileDescriptor().getFile() != null) {
                this.sourceUriString = afd.getParcelFileDescriptor().getFile().toURI().toString();
            }
            this.state = State.INITIALIZED;
        }
    }

    public void setDataSource(FileDescriptor fd) throws IOException {
        setDataSource(fd, 0L, -1L);
    }

    public void setDataSource(FileDescriptor fd, long offset, long length) throws IOException {
        if (fd == null) throw new IllegalArgumentException("Null FileDescriptor");
        try (FileInputStream fis = new FileInputStream(fd)) {
            if (offset > 0) fis.skipNBytes(offset);
            this.rawSourceBytes = length > 0
                    ? fis.readNBytes((int) Math.min(Integer.MAX_VALUE, length))
                    : fis.readAllBytes();
            this.state = State.INITIALIZED;
        }
    }

    public void setDataSource(MediaDataSource dataSource) throws IllegalArgumentException {
        if (dataSource == null) throw new IllegalArgumentException("Null MediaDataSource");
        try {
            long total = dataSource.getSize();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            long pos = 0;
            while (total < 0 || pos < total) {
                int r = dataSource.readAt(pos, buf, 0, buf.length);
                if (r <= 0) break;
                baos.write(buf, 0, r);
                pos += r;
            }
            this.rawSourceBytes = baos.toByteArray();
            this.state = State.INITIALIZED;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void prepare() throws IOException, IllegalStateException {
        if (state != State.INITIALIZED && state != State.STOPPED) {
            throw new IllegalStateException("prepare() called in state " + state);
        }
        closeBackends();
        this.state = State.PREPARING;
        prepareInternal();
        this.state = State.PREPARED;
    }

    public void prepareAsync() throws IllegalStateException {
        if (state != State.INITIALIZED && state != State.STOPPED) {
            throw new IllegalStateException("prepareAsync() called in state " + state);
        }
        this.state = State.PREPARING;
        Thread t = new Thread(() -> {
            try {
                prepareInternal();
                this.state = State.PREPARED;
                OnPreparedListener pl = onPreparedListener;
                if (pl != null) mainHandler.post(() -> pl.onPrepared(this));
            } catch (Throwable ex) {
                OnErrorListener el = onErrorListener;
                if (el != null) mainHandler.post(() -> el.onError(this, MEDIA_ERROR_IO, 0));
            }
        }, "AAOW-MediaPlayer-Prepare");
        t.setDaemon(true);
        t.start();
    }

    private void prepareInternal() throws IOException {
        if (rawSourceBytes == null && sourceUriString != null
                && (sourceUriString.startsWith("http://") || sourceUriString.startsWith("https://"))) {
            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create(sourceUriString).toURL().openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);
                try (InputStream in = conn.getInputStream()) {
                    rawSourceBytes = in.readAllBytes();
                }
            } catch (Exception ignored) {
            }
        }

        if (rawSourceBytes != null && rawSourceBytes.length > 0) {
            WindowsAudioHelper.DecodedPcm pcm = WindowsAudioHelper.decodeToPcm(rawSourceBytes);
            if (pcm != null) {
                this.durationMs = pcm.durationMs();
                this.clip = WindowsAudioHelper.openClip(pcm);
                if (this.clip != null) {
                    WindowsAudioHelper.applyGain(clip, leftVolume, rightVolume);
                    clip.addLineListener(ev -> {
                        if (ev.getType() == LineEvent.Type.STOP && !manualStop && state == State.STARTED && !looping) {
                            state = State.COMPLETED;
                            OnCompletionListener cl = onCompletionListener;
                            if (cl != null) mainHandler.post(() -> cl.onCompletion(this));
                        }
                    });
                }
                return;
            }
        }

        // Fallback cho MP3/AAC/M4A qua JavaFX Media nếu có URI hoặc ghi ra file tạm
        if (WebViewEngineBridge.isJavaFxAvailable()) {
            String mediaUrl = sourceUriString;
            if (mediaUrl == null && rawSourceBytes != null) {
                Path tmp = Files.createTempFile("aaow-media-", ".bin");
                tmp.toFile().deleteOnExit();
                Files.write(tmp, rawSourceBytes);
                mediaUrl = tmp.toUri().toString();
            }
            if (mediaUrl != null) {
                CountDownLatch ready = new CountDownLatch(1);
                String finalUrl = mediaUrl;
                Platform.runLater(() -> {
                    try {
                        Media media = new Media(finalUrl);
                        javafx.scene.media.MediaPlayer mp = new javafx.scene.media.MediaPlayer(media);
                        mp.setVolume((leftVolume + rightVolume) * 0.5);
                        mp.setCycleCount(looping ? javafx.scene.media.MediaPlayer.INDEFINITE : 1);
                        mp.setOnReady(() -> {
                            Duration d = media.getDuration();
                            if (d != null && !d.isUnknown()) {
                                durationMs = (int) Math.round(d.toMillis());
                            }
                            ready.countDown();
                        });
                        mp.setOnEndOfMedia(() -> {
                            if (!looping) {
                                state = State.COMPLETED;
                                OnCompletionListener cl = onCompletionListener;
                                if (cl != null) mainHandler.post(() -> cl.onCompletion(this));
                            }
                        });
                        mp.setOnError(ready::countDown);
                        this.fxPlayer = mp;
                    } catch (Throwable t) {
                        ready.countDown();
                    }
                });
                try {
                    ready.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void start() throws IllegalStateException {
        if (state != State.PREPARED && state != State.STARTED && state != State.PAUSED && state != State.COMPLETED) {
            throw new IllegalStateException("start() called in state " + state);
        }
        manualStop = false;
        state = State.STARTED;
        if (clip != null) {
            if (pausePositionMs > 0) {
                clip.setMicrosecondPosition(pausePositionMs * 1000L);
            }
            WindowsAudioHelper.applyGain(clip, leftVolume, rightVolume);
            if (looping) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                clip.start();
            }
        } else if (fxPlayer != null) {
            Platform.runLater(() -> {
                if (fxPlayer != null) fxPlayer.play();
            });
        }
    }

    public void pause() throws IllegalStateException {
        if (state != State.STARTED && state != State.PAUSED) {
            throw new IllegalStateException("pause() called in state " + state);
        }
        manualStop = true;
        pausePositionMs = getCurrentPosition();
        state = State.PAUSED;
        if (clip != null) {
            clip.stop();
        } else if (fxPlayer != null) {
            Platform.runLater(() -> {
                if (fxPlayer != null) fxPlayer.pause();
            });
        }
    }

    public void stop() throws IllegalStateException {
        if (state == State.IDLE || state == State.END) {
            throw new IllegalStateException("stop() called in state " + state);
        }
        manualStop = true;
        pausePositionMs = 0;
        state = State.STOPPED;
        if (clip != null) {
            clip.stop();
            clip.setMicrosecondPosition(0);
        } else if (fxPlayer != null) {
            Platform.runLater(() -> {
                if (fxPlayer != null) fxPlayer.stop();
            });
        }
    }

    public void seekTo(int msec) throws IllegalStateException {
        seekTo((long) msec, 0);
    }

    public void seekTo(long msec, int mode) {
        int clamped = Math.max(0, durationMs > 0 ? (int) Math.min(msec, durationMs) : (int) msec);
        pausePositionMs = clamped;
        if (clip != null) {
            clip.setMicrosecondPosition(clamped * 1000L);
        } else if (fxPlayer != null) {
            Platform.runLater(() -> {
                if (fxPlayer != null) fxPlayer.seek(Duration.millis(clamped));
            });
        }
        OnSeekCompleteListener sl = onSeekCompleteListener;
        if (sl != null) mainHandler.post(() -> sl.onSeekComplete(this));
    }

    public boolean isPlaying() {
        if (clip != null) return clip.isRunning() || state == State.STARTED;
        return state == State.STARTED;
    }

    public int getCurrentPosition() {
        if (clip != null && state == State.STARTED) {
            return (int) (clip.getMicrosecondPosition() / 1000L);
        }
        if (fxPlayer != null) {
            Duration d = fxPlayer.getCurrentTime();
            if (d != null && !d.isUnknown()) return (int) Math.round(d.toMillis());
        }
        return pausePositionMs;
    }

    public int getDuration() {
        return durationMs;
    }

    public int getVideoWidth() { return 0; }
    public int getVideoHeight() { return 0; }

    public void setLooping(boolean looping) {
        this.looping = looping;
        if (clip != null && state == State.STARTED) {
            if (looping) clip.loop(Clip.LOOP_CONTINUOUSLY);
        } else if (fxPlayer != null) {
            Platform.runLater(() -> {
                if (fxPlayer != null) {
                    fxPlayer.setCycleCount(looping ? javafx.scene.media.MediaPlayer.INDEFINITE : 1);
                }
            });
        }
    }

    public boolean isLooping() {
        return looping;
    }

    public void setVolume(float leftVolume, float rightVolume) {
        this.leftVolume = Math.max(0f, Math.min(1f, leftVolume));
        this.rightVolume = Math.max(0f, Math.min(1f, rightVolume));
        WindowsAudioHelper.applyGain(clip, this.leftVolume, this.rightVolume);
        if (fxPlayer != null) {
            double v = (this.leftVolume + this.rightVolume) * 0.5;
            Platform.runLater(() -> {
                if (fxPlayer != null) fxPlayer.setVolume(v);
            });
        }
    }

    public void setAudioAttributes(AudioAttributes attributes) {
        if (attributes != null) this.audioAttributes = attributes;
    }

    public AudioAttributes getAudioAttributes() { return audioAttributes; }

    public void setAudioStreamType(int streamtype) {
    }

    public void setAudioSessionId(int sessionId) {
        this.audioSessionId = sessionId;
    }

    public int getAudioSessionId() { return audioSessionId; }

    public void setDisplay(SurfaceHolder sh) {
    }

    public void setSurface(Surface surface) {
    }

    public void setScreenOnWhilePlaying(boolean screenOn) {
    }

    public void setWakeMode(Context context, int mode) {
    }

    public void reset() {
        closeBackends();
        rawSourceBytes = null;
        sourceUriString = null;
        durationMs = 0;
        pausePositionMs = 0;
        state = State.IDLE;
    }

    public void release() {
        closeBackends();
        state = State.END;
    }

    private void closeBackends() {
        manualStop = true;
        if (clip != null) {
            try {
                clip.stop();
                clip.close();
            } catch (Throwable ignored) {
            }
            clip = null;
        }
        if (fxPlayer != null) {
            javafx.scene.media.MediaPlayer p = fxPlayer;
            fxPlayer = null;
            Platform.runLater(() -> {
                try {
                    p.stop();
                    p.dispose();
                } catch (Throwable ignored) {
                }
            });
        }
    }

    public void setOnPreparedListener(OnPreparedListener listener) { this.onPreparedListener = listener; }
    public void setOnCompletionListener(OnCompletionListener listener) { this.onCompletionListener = listener; }
    public void setOnErrorListener(OnErrorListener listener) { this.onErrorListener = listener; }
    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) { this.onSeekCompleteListener = listener; }
    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) { this.onBufferingUpdateListener = listener; }
    public void setOnInfoListener(OnInfoListener listener) { this.onInfoListener = listener; }
    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) { this.onVideoSizeChangedListener = listener; }
}
