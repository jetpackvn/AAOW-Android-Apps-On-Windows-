package android.media;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class AudioFormat implements Parcelable {
    public static final int ENCODING_INVALID = 0;
    public static final int ENCODING_DEFAULT = 1;
    public static final int ENCODING_PCM_16BIT = 2;
    public static final int ENCODING_PCM_8BIT = 3;
    public static final int ENCODING_PCM_FLOAT = 4;
    public static final int ENCODING_MP3 = 9;

    public static final int CHANNEL_INVALID = 0;
    public static final int CHANNEL_CONFIGURATION_DEFAULT = 1;
    public static final int CHANNEL_CONFIGURATION_MONO = 2;
    public static final int CHANNEL_CONFIGURATION_STEREO = 3;
    public static final int CHANNEL_OUT_DEFAULT = 1;
    public static final int CHANNEL_OUT_MONO = 4;
    public static final int CHANNEL_OUT_STEREO = 12;
    public static final int CHANNEL_IN_DEFAULT = 1;
    public static final int CHANNEL_IN_MONO = 16;
    public static final int CHANNEL_IN_STEREO = 12;
    public static final int SAMPLE_RATE_UNSPECIFIED = 0;

    private int encoding = ENCODING_PCM_16BIT;
    private int sampleRate = 44100;
    private int channelMask = CHANNEL_OUT_STEREO;

    AudioFormat() {
    }

    public int getEncoding() {
        return encoding;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getChannelMask() {
        return channelMask;
    }

    public int getChannelCount() {
        if (channelMask == CHANNEL_OUT_MONO || channelMask == CHANNEL_CONFIGURATION_MONO || channelMask == CHANNEL_IN_MONO) {
            return 1;
        }
        int bits = Integer.bitCount(channelMask);
        return bits > 0 ? bits : 2;
    }

    public int getFrameSizeInBytes() {
        int bytesPerSample = switch (encoding) {
            case ENCODING_PCM_8BIT -> 1;
            case ENCODING_PCM_FLOAT -> 4;
            default -> 2;
        };
        return bytesPerSample * getChannelCount();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(encoding);
        dest.writeInt(sampleRate);
        dest.writeInt(channelMask);
    }

    public static final Creator<AudioFormat> CREATOR = new Creator<>() {
        @Override
        public AudioFormat createFromParcel(Parcel in) {
            AudioFormat af = new AudioFormat();
            af.encoding = in.readInt();
            af.sampleRate = in.readInt();
            af.channelMask = in.readInt();
            return af;
        }

        @Override
        public AudioFormat[] newArray(int size) {
            return new AudioFormat[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AudioFormat other)) return false;
        return encoding == other.encoding && sampleRate == other.sampleRate && channelMask == other.channelMask;
    }

    @Override
    public int hashCode() {
        return Objects.hash(encoding, sampleRate, channelMask);
    }

    public static class Builder {
        private final AudioFormat format = new AudioFormat();

        public Builder() {
        }

        public Builder(AudioFormat af) {
            if (af != null) {
                format.encoding = af.encoding;
                format.sampleRate = af.sampleRate;
                format.channelMask = af.channelMask;
            }
        }

        public Builder setEncoding(int encoding) {
            format.encoding = encoding;
            return this;
        }

        public Builder setSampleRate(int sampleRate) {
            format.sampleRate = sampleRate;
            return this;
        }

        public Builder setChannelMask(int channelMask) {
            format.channelMask = channelMask;
            return this;
        }

        public Builder setChannelIndexMask(int channelIndexMask) {
            format.channelMask = channelIndexMask;
            return this;
        }

        public AudioFormat build() {
            return format;
        }
    }
}
