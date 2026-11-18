package android.media;

import android.os.Parcel;
import android.os.Parcelable;

public final class AudioAttributes implements Parcelable {
    public static final int CONTENT_TYPE_UNKNOWN = 0;
    public static final int CONTENT_TYPE_SPEECH = 1;
    public static final int CONTENT_TYPE_MUSIC = 2;
    public static final int CONTENT_TYPE_MOVIE = 3;
    public static final int CONTENT_TYPE_SONIFICATION = 4;

    public static final int USAGE_UNKNOWN = 0;
    public static final int USAGE_MEDIA = 1;
    public static final int USAGE_VOICE_COMMUNICATION = 2;
    public static final int USAGE_ALARM = 4;
    public static final int USAGE_NOTIFICATION = 5;
    public static final int USAGE_GAME = 14;
    public static final int USAGE_ASSISTANCE_SONIFICATION = 13;

    public static final int FLAG_AUDIBILITY_ENFORCED = 0x1;
    public static final int FLAG_HW_AV_SYNC = 0x10;
    public static final int FLAG_LOW_LATENCY = 0x100;

    private int usage;
    private int contentType;
    private int flags;

    private AudioAttributes() {
    }

    public int getUsage() {
        return usage;
    }

    public int getContentType() {
        return contentType;
    }

    public int getFlags() {
        return flags;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(usage);
        dest.writeInt(contentType);
        dest.writeInt(this.flags);
    }

    public static final Creator<AudioAttributes> CREATOR = new Creator<>() {
        @Override
        public AudioAttributes createFromParcel(Parcel source) {
            AudioAttributes aa = new AudioAttributes();
            aa.usage = source.readInt();
            aa.contentType = source.readInt();
            aa.flags = source.readInt();
            return aa;
        }

        @Override
        public AudioAttributes[] newArray(int size) {
            return new AudioAttributes[size];
        }
    };

    public static class Builder {
        private final AudioAttributes attrs = new AudioAttributes();

        public Builder() {
        }

        public Builder(AudioAttributes aa) {
            if (aa != null) {
                attrs.usage = aa.usage;
                attrs.contentType = aa.contentType;
                attrs.flags = aa.flags;
            }
        }

        public Builder setUsage(int usage) {
            attrs.usage = usage;
            return this;
        }

        public Builder setContentType(int contentType) {
            attrs.contentType = contentType;
            return this;
        }

        public Builder setFlags(int flags) {
            attrs.flags = flags;
            return this;
        }

        public Builder setLegacyStreamType(int streamType) {
            return this;
        }

        public AudioAttributes build() {
            return attrs;
        }
    }
}
