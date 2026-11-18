package android.media;

import javax.sound.sampled.SourceDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Phát PCM thô trực tiếp qua Windows Audio (javax.sound.sampled.SourceDataLine).
 */
public class AudioTrack implements AudioRouting, VolumeAutomation {
    public static final int MODE_STATIC = 0;
    public static final int MODE_STREAM = 1;

    public static final int PLAYSTATE_STOPPED = 1;
    public static final int PLAYSTATE_PAUSED = 2;
    public static final int PLAYSTATE_PLAYING = 3;

    public static final int STATE_UNINITIALIZED = 0;
    public static final int STATE_INITIALIZED = 1;
    public static final int STATE_NO_STATIC_DATA = 2;

    public static final int WRITE_BLOCKING = 0;
    public static final int WRITE_NON_BLOCKING = 1;

    public static final int SUCCESS = 0;
    public static final int ERROR = -1;
    public static final int ERROR_BAD_VALUE = -2;
    public static final int ERROR_INVALID_OPERATION = -3;

    private final AudioAttributes attributes;
    private final AudioFormat format;
    private final int bufferSizeInBytes;
    private final int mode;
    private final int sessionId;
    private final int streamType;
    private final javax.sound.sampled.AudioFormat javaFormat;

    private SourceDataLine line;
    private byte[] staticBuffer;
    private int staticBufferPos;
    private int playState = PLAYSTATE_STOPPED;
    private int state;
    private long writtenFrames;
    private float leftVolume = 1.0f;
    private float rightVolume = 1.0f;
    private int playbackRate;

    public AudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat,
                      int bufferSizeInBytes, int mode) throws IllegalArgumentException {
        this(streamType, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, mode, 1);
    }

    public AudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat,
                      int bufferSizeInBytes, int mode, int sessionId) throws IllegalArgumentException {
        this(new AudioAttributes.Builder().setLegacyStreamType(streamType).build(),
                new AudioFormat.Builder()
                        .setSampleRate(sampleRateInHz)
                        .setChannelMask(channelConfig)
                        .setEncoding(audioFormat)
                        .build(),
                bufferSizeInBytes, mode, sessionId, streamType);
    }

    public AudioTrack(AudioAttributes attributes, AudioFormat format, int bufferSizeInBytes,
                      int mode, int sessionId) throws IllegalArgumentException {
        this(attributes, format, bufferSizeInBytes, mode, sessionId, AudioManager.STREAM_MUSIC);
    }

    private AudioTrack(AudioAttributes attributes, AudioFormat format, int bufferSizeInBytes,
                       int mode, int sessionId, int streamType) {
        if (format == null || format.getSampleRate() <= 0 || bufferSizeInBytes <= 0) {
            throw new IllegalArgumentException("Invalid AudioTrack configuration");
        }
        this.attributes = attributes != null ? attributes : new AudioAttributes.Builder().build();
        this.format = format;
        this.bufferSizeInBytes = bufferSizeInBytes;
        this.mode = mode;
        this.sessionId = sessionId > 0 ? sessionId : 1;
        this.streamType = streamType;
        this.playbackRate = format.getSampleRate();

        int channels = format.getChannelCount();
        int sampleBits = format.getEncoding() == AudioFormat.ENCODING_PCM_8BIT ? 8 : 16;
        int frameSize = Math.max(1, (sampleBits / 8) * channels);
        this.javaFormat = new javax.sound.sampled.AudioFormat(
                format.getEncoding() == AudioFormat.ENCODING_PCM_8BIT
                        ? javax.sound.sampled.AudioFormat.Encoding.PCM_UNSIGNED
                        : javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
                format.getSampleRate(),
                sampleBits,
                channels,
                frameSize,
                format.getSampleRate(),
                false
        );

        this.line = WindowsAudioHelper.openSourceDataLine(javaFormat, Math.max(4096, bufferSizeInBytes));
        if (mode == MODE_STATIC) {
            this.staticBuffer = new byte[bufferSizeInBytes];
            this.state = STATE_NO_STATIC_DATA;
        } else {
            this.state = STATE_INITIALIZED;
        }
    }

    public static int getMinBufferSize(int sampleRateInHz, int channelConfig, int audioFormat) {
        int channels = (channelConfig == AudioFormat.CHANNEL_OUT_MONO || channelConfig == AudioFormat.CHANNEL_CONFIGURATION_MONO) ? 1 : 2;
        int bytesPerSample = audioFormat == AudioFormat.ENCODING_PCM_8BIT ? 1 : (audioFormat == AudioFormat.ENCODING_PCM_FLOAT ? 4 : 2);
        return Math.max(2048, (sampleRateInHz / 10) * channels * bytesPerSample);
    }

    public static int getNativeOutputSampleRate(int streamType) {
        return 44100;
    }

    public static float getMinVolume() {
        return 0.0f;
    }

    public static float getMaxVolume() {
        return 1.0f;
    }

    public AudioAttributes getAudioAttributes() { return attributes; }
    public AudioFormat getFormat() { return format; }
    public int getSampleRate() { return format.getSampleRate(); }
    public int getPlaybackRate() { return playbackRate; }
    public int getAudioFormat() { return format.getEncoding(); }
    public int getChannelConfiguration() { return format.getChannelMask(); }
    public int getChannelCount() { return format.getChannelCount(); }
    public int getStreamType() { return streamType; }
    public int getAudioSessionId() { return sessionId; }
    public int getState() { return state; }
    public int getPlayState() { return playState; }
    public int getBufferSizeInFrames() { return bufferSizeInBytes / Math.max(1, format.getFrameSizeInBytes()); }
    public int getBufferCapacityInFrames() { return getBufferSizeInFrames(); }
    public long getWrittenFramesCount() { return writtenFrames; }

    public int getPlaybackHeadPosition() {
        if (line != null && line.isOpen()) {
            return (int) Math.max(line.getLongFramePosition(), writtenFrames);
        }
        return (int) writtenFrames;
    }

    public int setPlaybackRate(int sampleRateInHz) {
        if (sampleRateInHz <= 0) return ERROR_BAD_VALUE;
        this.playbackRate = sampleRateInHz;
        return SUCCESS;
    }

    public int setVolume(float gain) {
        return setStereoVolume(gain, gain);
    }

    public int setStereoVolume(float leftGain, float rightGain) {
        this.leftVolume = Math.max(0f, Math.min(1f, leftGain));
        this.rightVolume = Math.max(0f, Math.min(1f, rightGain));
        WindowsAudioHelper.applyGain(line, leftVolume, rightVolume);
        return SUCCESS;
    }

    public void play() throws IllegalStateException {
        if (state == STATE_UNINITIALIZED) throw new IllegalStateException("AudioTrack uninitialized");
        this.playState = PLAYSTATE_PLAYING;
        if (line != null) {
            WindowsAudioHelper.applyGain(line, leftVolume, rightVolume);
            line.start();
            if (mode == MODE_STATIC && staticBuffer != null && staticBufferPos > 0) {
                byte[] copy = java.util.Arrays.copyOf(staticBuffer, staticBufferPos);
                Thread t = new Thread(() -> {
                    try {
                        line.write(copy, 0, copy.length);
                        line.drain();
                    } catch (Throwable ignored) {
                    }
                }, "AAOW-AudioTrack-Static");
                t.setDaemon(true);
                t.start();
            }
        }
    }

    public void pause() throws IllegalStateException {
        if (state == STATE_UNINITIALIZED) throw new IllegalStateException("AudioTrack uninitialized");
        this.playState = PLAYSTATE_PAUSED;
        if (line != null) line.stop();
    }

    public void stop() throws IllegalStateException {
        if (state == STATE_UNINITIALIZED) throw new IllegalStateException("AudioTrack uninitialized");
        this.playState = PLAYSTATE_STOPPED;
        if (line != null) {
            line.stop();
            line.flush();
        }
    }

    public void flush() {
        if (line != null) line.flush();
        staticBufferPos = 0;
    }

    public int reloadStaticData() {
        return SUCCESS;
    }

    public int write(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        return write(audioData, offsetInBytes, sizeInBytes, WRITE_BLOCKING);
    }

    public int write(byte[] audioData, int offsetInBytes, int sizeInBytes, int writeMode) {
        if (audioData == null || offsetInBytes < 0 || sizeInBytes < 0 || offsetInBytes + sizeInBytes > audioData.length) {
            return ERROR_BAD_VALUE;
        }
        if (state == STATE_UNINITIALIZED) return ERROR_INVALID_OPERATION;
        int frameBytes = Math.max(1, format.getFrameSizeInBytes());
        if (mode == MODE_STATIC && staticBuffer != null) {
            int copyLen = Math.min(sizeInBytes, staticBuffer.length - staticBufferPos);
            System.arraycopy(audioData, offsetInBytes, staticBuffer, staticBufferPos, copyLen);
            staticBufferPos += copyLen;
            state = STATE_INITIALIZED;
            writtenFrames += copyLen / frameBytes;
            return copyLen;
        }
        if (line != null && line.isOpen()) {
            int written = line.write(audioData, offsetInBytes, sizeInBytes);
            writtenFrames += Math.max(0, written) / frameBytes;
            return written;
        }
        writtenFrames += sizeInBytes / frameBytes;
        return sizeInBytes;
    }

    public int write(short[] audioData, int offsetInShorts, int sizeInShorts) {
        return write(audioData, offsetInShorts, sizeInShorts, WRITE_BLOCKING);
    }

    public int write(short[] audioData, int offsetInShorts, int sizeInShorts, int writeMode) {
        if (audioData == null || offsetInShorts < 0 || sizeInShorts < 0 || offsetInShorts + sizeInShorts > audioData.length) {
            return ERROR_BAD_VALUE;
        }
        ByteBuffer bb = ByteBuffer.allocate(sizeInShorts * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < sizeInShorts; i++) {
            bb.putShort(audioData[offsetInShorts + i]);
        }
        int bytesWritten = write(bb.array(), 0, bb.capacity(), writeMode);
        return bytesWritten < 0 ? bytesWritten : bytesWritten / 2;
    }

    public int write(float[] audioData, int offsetInFloats, int sizeInFloats, int writeMode) {
        if (audioData == null || offsetInFloats < 0 || sizeInFloats < 0 || offsetInFloats + sizeInFloats > audioData.length) {
            return ERROR_BAD_VALUE;
        }
        ByteBuffer bb = ByteBuffer.allocate(sizeInFloats * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < sizeInFloats; i++) {
            float clamped = Math.max(-1f, Math.min(1f, audioData[offsetInFloats + i]));
            bb.putShort((short) Math.round(clamped * 32767f));
        }
        int bytesWritten = write(bb.array(), 0, bb.capacity(), writeMode);
        return bytesWritten < 0 ? bytesWritten : bytesWritten / 2;
    }

    public int write(ByteBuffer audioData, int sizeInBytes, int writeMode) {
        if (audioData == null || sizeInBytes < 0 || sizeInBytes > audioData.remaining()) {
            return ERROR_BAD_VALUE;
        }
        byte[] buf = new byte[sizeInBytes];
        audioData.get(buf);
        return write(buf, 0, sizeInBytes, writeMode);
    }

    public int write(ByteBuffer audioData, int sizeInBytes, int writeMode, long timestamp) {
        return write(audioData, sizeInBytes, writeMode);
    }

    public void release() {
        playState = PLAYSTATE_STOPPED;
        state = STATE_UNINITIALIZED;
        if (line != null) {
            try {
                line.stop();
                line.close();
            } catch (Throwable ignored) {
            }
            line = null;
        }
    }
}
