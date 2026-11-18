package android.app;

import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public final class NotificationChannel implements Parcelable {
    public static final String DEFAULT_CHANNEL_ID = "miscellaneous";

    private final String id;
    private CharSequence name;
    private int importance;
    private String description;
    private String group;
    private boolean showBadge = true;
    private boolean lights;
    private int lightColor;
    private boolean vibration;
    private long[] vibrationPattern;
    private Uri sound;
    private AudioAttributes audioAttributes;
    private int lockscreenVisibility;
    private boolean bypassDnd;

    public NotificationChannel(String id, CharSequence name, int importance) {
        this.id = id;
        this.name = name;
        this.importance = importance;
    }

    public String getId() { return id; }
    public CharSequence getName() { return name; }
    public void setName(CharSequence name) { this.name = name; }
    public int getImportance() { return importance; }
    public void setImportance(int importance) { this.importance = importance; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getGroup() { return group; }
    public void setGroup(String groupId) { this.group = groupId; }
    public boolean canShowBadge() { return showBadge; }
    public void setShowBadge(boolean showBadge) { this.showBadge = showBadge; }
    public void enableLights(boolean lights) { this.lights = lights; }
    public boolean shouldShowLights() { return lights; }
    public void setLightColor(int argb) { this.lightColor = argb; }
    public int getLightColor() { return lightColor; }
    public void enableVibration(boolean vibration) { this.vibration = vibration; }
    public boolean shouldVibrate() { return vibration; }
    public void setVibrationPattern(long[] vibrationPattern) { this.vibrationPattern = vibrationPattern; }
    public long[] getVibrationPattern() { return vibrationPattern; }
    public void setSound(Uri sound, AudioAttributes audioAttributes) {
        this.sound = sound;
        this.audioAttributes = audioAttributes;
    }
    public Uri getSound() { return sound; }
    public AudioAttributes getAudioAttributes() { return audioAttributes; }
    public void setLockscreenVisibility(int lockscreenVisibility) { this.lockscreenVisibility = lockscreenVisibility; }
    public int getLockscreenVisibility() { return lockscreenVisibility; }
    public void setBypassDnd(boolean bypassDnd) { this.bypassDnd = bypassDnd; }
    public boolean canBypassDnd() { return bypassDnd; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name != null ? name.toString() : null);
        dest.writeInt(importance);
        dest.writeString(description);
        dest.writeString(group);
    }

    public static final Creator<NotificationChannel> CREATOR = new Creator<>() {
        @Override
        public NotificationChannel createFromParcel(Parcel in) {
            NotificationChannel ch = new NotificationChannel(in.readString(), in.readString(), in.readInt());
            ch.description = in.readString();
            ch.group = in.readString();
            return ch;
        }

        @Override
        public NotificationChannel[] newArray(int size) { return new NotificationChannel[size]; }
    };
}
