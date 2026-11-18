package android.app;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public final class NotificationChannelGroup implements Parcelable {
    private final String id;
    private final CharSequence name;
    private String description;
    private final List<NotificationChannel> channels = new ArrayList<>();

    public NotificationChannelGroup(String id, CharSequence name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public CharSequence getName() { return name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<NotificationChannel> getChannels() { return channels; }
    public boolean isBlocked() { return false; }

    @Override
    public NotificationChannelGroup clone() {
        NotificationChannelGroup g = new NotificationChannelGroup(id, name);
        g.description = description;
        g.channels.addAll(channels);
        return g;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name != null ? name.toString() : null);
        dest.writeString(description);
    }

    public static final Creator<NotificationChannelGroup> CREATOR = new Creator<>() {
        @Override
        public NotificationChannelGroup createFromParcel(Parcel in) {
            NotificationChannelGroup g = new NotificationChannelGroup(in.readString(), in.readString());
            g.description = in.readString();
            return g;
        }

        @Override
        public NotificationChannelGroup[] newArray(int size) { return new NotificationChannelGroup[size]; }
    };
}
