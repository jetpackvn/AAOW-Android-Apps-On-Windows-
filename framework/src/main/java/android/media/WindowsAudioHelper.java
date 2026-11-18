package android.media;

import android.util.Log;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Tiện ích dùng chung cho MediaPlayer, SoundPool và AudioTrack kết nối tới Windows Audio (javax.sound.sampled).
 */
public final class WindowsAudioHelper {
    private static final String TAG = "AAOW-Audio";

    public record DecodedPcm(AudioFormat format, byte[] pcmBytes, int durationMs) {
    }

    private WindowsAudioHelper() {
    }

    public static DecodedPcm decodeToPcm(byte[] rawBytes) {
        if (rawBytes == null || rawBytes.length == 0) return null;
        return decodeToPcm(new ByteArrayInputStream(rawBytes));
    }

    public static DecodedPcm decodeToPcm(InputStream stream) {
        if (stream == null) return null;
        try (BufferedInputStream bis = new BufferedInputStream(stream);
             AudioInputStream source = AudioSystem.getAudioInputStream(bis)) {
            AudioFormat base = source.getFormat();
            int channels = base.getChannels() > 0 ? base.getChannels() : 2;
            float rate = base.getSampleRate() > 0 ? base.getSampleRate() : 44100f;
            AudioFormat pcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    rate,
                    16,
                    channels,
                    channels * 2,
                    rate,
                    false
            );
            try (AudioInputStream decoded = base.matches(pcmFormat)
                    ? source
                    : AudioSystem.getAudioInputStream(pcmFormat, source)) {
                byte[] pcm = decoded.readAllBytes();
                int bytesPerSec = Math.max(1, (int) (rate * channels * 2));
                int durationMs = (int) Math.round((pcm.length * 1000.0) / bytesPerSec);
                return new DecodedPcm(pcmFormat, pcm, durationMs);
            }
        } catch (Throwable t) {
            Log.d(TAG, "Không giải mã được qua javax.sound.sampled: " + t.getMessage());
            return null;
        }
    }

    public static Clip openClip(DecodedPcm pcm) {
        if (pcm == null || pcm.pcmBytes().length == 0) return null;
        try {
            DataLine.Info info = new DataLine.Info(Clip.class, pcm.format());
            if (!AudioSystem.isLineSupported(info)) {
                return null;
            }
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(pcm.format(), pcm.pcmBytes(), 0, pcm.pcmBytes().length);
            return clip;
        } catch (Throwable t) {
            Log.d(TAG, "Không mở được Clip trên Windows Audio: " + t.getMessage());
            return null;
        }
    }

    public static SourceDataLine openSourceDataLine(AudioFormat format, int bufferSizeBytes) {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                return null;
            }
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            if (bufferSizeBytes > 0) {
                line.open(format, bufferSizeBytes);
            } else {
                line.open(format);
            }
            return line;
        } catch (Throwable t) {
            Log.d(TAG, "Không mở được SourceDataLine trên Windows Audio: " + t.getMessage());
            return null;
        }
    }

    public static void applyGain(DataLine line, float leftVol, float rightVol) {
        if (line == null) return;
        float avg = Math.max(0f, Math.min(1f, (leftVol + rightVol) * 0.5f));
        try {
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = avg <= 0.0001f ? gain.getMinimum() : (float) (20.0 * Math.log10(avg));
                dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
                gain.setValue(dB);
            }
            if (line.isControlSupported(FloatControl.Type.PAN) && Math.abs(leftVol - rightVol) > 0.001f) {
                FloatControl pan = (FloatControl) line.getControl(FloatControl.Type.PAN);
                float sum = leftVol + rightVol;
                float val = sum > 0f ? (rightVol - leftVol) / sum : 0f;
                pan.setValue(Math.max(pan.getMinimum(), Math.min(pan.getMaximum(), val)));
            }
        } catch (Throwable ignored) {
        }
    }
}
