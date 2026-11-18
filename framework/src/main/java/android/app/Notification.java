package android.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Notification implements Parcelable {
    public static final int DEFAULT_ALL = -1;
    public static final int DEFAULT_SOUND = 1;
    public static final int DEFAULT_VIBRATE = 2;
    public static final int DEFAULT_LIGHTS = 4;

    public static final int FLAG_SHOW_LIGHTS = 0x00000001;
    public static final int FLAG_ONGOING_EVENT = 0x00000002;
    public static final int FLAG_INSISTENT = 0x00000004;
    public static final int FLAG_ONLY_ALERT_ONCE = 0x00000008;
    public static final int FLAG_AUTO_CANCEL = 0x00000010;
    public static final int FLAG_NO_CLEAR = 0x00000020;
    public static final int FLAG_FOREGROUND_SERVICE = 0x00000040;
    public static final int FLAG_HIGH_PRIORITY = 0x00000080;
    public static final int FLAG_GROUP_SUMMARY = 0x00000200;

    public static final int PRIORITY_DEFAULT = 0;
    public static final int PRIORITY_LOW = -1;
    public static final int PRIORITY_MIN = -2;
    public static final int PRIORITY_HIGH = 1;
    public static final int PRIORITY_MAX = 2;

    public static final int VISIBILITY_PUBLIC = 1;
    public static final int VISIBILITY_PRIVATE = 0;
    public static final int VISIBILITY_SECRET = -1;

    public static final String EXTRA_TITLE = "android.title";
    public static final String EXTRA_TITLE_BIG = "android.title.big";
    public static final String EXTRA_TEXT = "android.text";
    public static final String EXTRA_SUB_TEXT = "android.subText";
    public static final String EXTRA_BIG_TEXT = "android.bigText";
    public static final String EXTRA_SUMMARY_TEXT = "android.summaryText";
    public static final String EXTRA_INFO_TEXT = "android.infoText";
    public static final String EXTRA_TEXT_LINES = "android.textLines";
    public static final String EXTRA_PROGRESS = "android.progress";
    public static final String EXTRA_PROGRESS_MAX = "android.progressMax";
    public static final String EXTRA_PROGRESS_INDETERMINATE = "android.progressIndeterminate";
    public static final String EXTRA_SHOW_WHEN = "android.showWhen";

    public static final String CATEGORY_MESSAGE = "msg";
    public static final String CATEGORY_PROGRESS = "progress";
    public static final String CATEGORY_SERVICE = "service";
    public static final String CATEGORY_ALARM = "alarm";
    public static final String CATEGORY_REMINDER = "reminder";

    public static final AudioAttributes AUDIO_ATTRIBUTES_DEFAULT = new AudioAttributes.Builder().build();

    public long when;
    public int icon;
    public int iconLevel;
    public int number;
    public PendingIntent contentIntent;
    public PendingIntent deleteIntent;
    public PendingIntent fullScreenIntent;
    public CharSequence tickerText;
    public RemoteViews contentView;
    public RemoteViews bigContentView;
    public RemoteViews headsUpContentView;
    public Bitmap largeIcon;
    public Uri sound;
    public AudioAttributes audioAttributes = AUDIO_ATTRIBUTES_DEFAULT;
    public int audioStreamType = -1;
    public long[] vibrate;
    public int ledARGB;
    public int ledOnMS;
    public int ledOffMS;
    public int defaults;
    public int flags;
    public int priority;
    public int color;
    public int visibility;
    public String category;
    public Bundle extras = new Bundle();
    public Action[] actions;

    private String channelId;
    private String groupKey;
    private String sortKey;
    private Icon smallIconObj;
    private Icon largeIconObj;

    public Notification() {
        this.when = System.currentTimeMillis();
    }

    public Notification(int icon, CharSequence tickerText, long when) {
        this.icon = icon;
        this.tickerText = tickerText;
        this.when = when;
    }

    public Notification(Parcel parcel) {
        this();
        if (parcel != null) {
            this.when = parcel.readLong();
            this.icon = parcel.readInt();
            this.channelId = parcel.readString();
            Bundle b = parcel.readBundle();
            if (b != null) this.extras.putAll(b);
        }
    }

    public String getChannelId() { return channelId; }
    public String getGroup() { return groupKey; }
    public String getSortKey() { return sortKey; }
    public Icon getSmallIcon() { return smallIconObj; }
    public Icon getLargeIcon() { return largeIconObj; }

    public CharSequence getResolvedTitle() {
        CharSequence t = extras.getCharSequence(EXTRA_TITLE_BIG);
        if (t == null) t = extras.getCharSequence(EXTRA_TITLE);
        return t != null ? t : tickerText;
    }

    public CharSequence getResolvedText() {
        CharSequence t = extras.getCharSequence(EXTRA_BIG_TEXT);
        if (t == null) t = extras.getCharSequence(EXTRA_TEXT);
        return t != null ? t : extras.getCharSequence(EXTRA_SUB_TEXT);
    }

    @Override
    public Notification clone() {
        Notification n = new Notification();
        n.when = when;
        n.icon = icon;
        n.tickerText = tickerText;
        n.contentIntent = contentIntent;
        n.deleteIntent = deleteIntent;
        n.largeIcon = largeIcon;
        n.flags = flags;
        n.priority = priority;
        n.color = color;
        n.category = category;
        n.channelId = channelId;
        n.groupKey = groupKey;
        n.sortKey = sortKey;
        n.extras.putAll(extras);
        n.actions = actions != null ? actions.clone() : null;
        return n;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(when);
        dest.writeInt(icon);
        dest.writeString(channelId);
        dest.writeBundle(extras);
    }

    public static final Creator<Notification> CREATOR = new Creator<>() {
        @Override public Notification createFromParcel(Parcel in) { return new Notification(in); }
        @Override public Notification[] newArray(int size) { return new Notification[size]; }
    };

    public interface Extender {
        Builder extend(Builder builder);
    }

    public static class Action implements Parcelable {
        public interface Extender {
            Builder extend(Builder builder);
        }

        public int icon;
        public CharSequence title;
        public PendingIntent actionIntent;
        private Icon iconObj;
        private final Bundle extras = new Bundle();

        public Action(int icon, CharSequence title, PendingIntent intent) {
            this.icon = icon;
            this.title = title;
            this.actionIntent = intent;
        }

        private Action(Icon iconObj, CharSequence title, PendingIntent intent, Bundle extras) {
            this.iconObj = iconObj;
            this.icon = iconObj != null ? iconObj.getResId() : 0;
            this.title = title;
            this.actionIntent = intent;
            if (extras != null) this.extras.putAll(extras);
        }

        public Icon getIcon() { return iconObj; }
        public Bundle getExtras() { return extras; }

        @Override
        public Action clone() {
            return new Action(iconObj, title, actionIntent, extras);
        }

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(icon);
            dest.writeString(title != null ? title.toString() : null);
            dest.writeParcelable(actionIntent, flags);
        }

        public static final Creator<Action> CREATOR = new Creator<>() {
            @Override
            public Action createFromParcel(Parcel in) {
                return new Action(in.readInt(), in.readString(), in.readParcelable(null));
            }

            @Override
            public Action[] newArray(int size) { return new Action[size]; }
        };

        public static final class Builder {
            private final Icon iconObj;
            private final CharSequence title;
            private final PendingIntent intent;
            private final Bundle extras = new Bundle();

            public Builder(int icon, CharSequence title, PendingIntent intent) {
                this(Icon.createWithResource("", icon), title, intent);
            }

            public Builder(Icon icon, CharSequence title, PendingIntent intent) {
                this.iconObj = icon;
                this.title = title;
                this.intent = intent;
            }

            public Builder(Action action) {
                this(action.iconObj, action.title, action.actionIntent);
                this.extras.putAll(action.extras);
            }

            public Builder addExtras(Bundle extras) {
                if (extras != null) this.extras.putAll(extras);
                return this;
            }

            public Builder extend(Extender extender) {
                if (extender != null) extender.extend(this);
                return this;
            }

            public Bundle getExtras() { return extras; }
            public Builder setAllowGeneratedReplies(boolean allow) { return this; }
            public Builder setContextual(boolean isContextual) { return this; }
            public Builder setSemanticAction(int semanticAction) { return this; }
            public Builder setAuthenticationRequired(boolean authRequired) { return this; }

            public Action build() {
                return new Action(iconObj, title, intent, extras);
            }
        }
    }

    public static abstract class Style {
        protected Builder mBuilder;
        protected CharSequence bigContentTitle;
        protected CharSequence summaryText;

        public void setBuilder(Builder builder) {
            if (this.mBuilder != builder) {
                this.mBuilder = builder;
                if (builder != null) builder.setStyle(this);
            }
        }

        protected void checkBuilder() {
            if (mBuilder == null) throw new IllegalArgumentException("Style requires a valid Builder");
        }

        protected void internalSetBigContentTitle(CharSequence title) {
            this.bigContentTitle = title;
        }

        protected void internalSetSummaryText(CharSequence cs) {
            this.summaryText = cs;
        }

        protected void addExtras(Bundle extras) {
            if (bigContentTitle != null) extras.putCharSequence(EXTRA_TITLE_BIG, bigContentTitle);
            if (summaryText != null) extras.putCharSequence(EXTRA_SUMMARY_TEXT, summaryText);
        }

        public Notification build() {
            checkBuilder();
            return mBuilder.build();
        }
    }

    public static class BigTextStyle extends Style {
        private CharSequence bigText;

        public BigTextStyle() {}
        public BigTextStyle(Builder builder) { setBuilder(builder); }

        public BigTextStyle setBigContentTitle(CharSequence title) {
            internalSetBigContentTitle(title);
            return this;
        }

        public BigTextStyle setSummaryText(CharSequence cs) {
            internalSetSummaryText(cs);
            return this;
        }

        public BigTextStyle bigText(CharSequence cs) {
            this.bigText = cs;
            return this;
        }

        @Override
        protected void addExtras(Bundle extras) {
            super.addExtras(extras);
            if (bigText != null) extras.putCharSequence(EXTRA_BIG_TEXT, bigText);
        }
    }

    public static class InboxStyle extends Style {
        private final List<CharSequence> texts = new ArrayList<>();

        public InboxStyle() {}
        public InboxStyle(Builder builder) { setBuilder(builder); }

        public InboxStyle setBigContentTitle(CharSequence title) {
            internalSetBigContentTitle(title);
            return this;
        }

        public InboxStyle setSummaryText(CharSequence cs) {
            internalSetSummaryText(cs);
            return this;
        }

        public InboxStyle addLine(CharSequence cs) {
            if (cs != null) texts.add(cs);
            return this;
        }

        @Override
        protected void addExtras(Bundle extras) {
            super.addExtras(extras);
            if (!texts.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (CharSequence line : texts) {
                    if (!sb.isEmpty()) sb.append('\n');
                    sb.append(line);
                }
                extras.putCharSequence(EXTRA_BIG_TEXT, sb.toString());
            }
        }
    }

    public static class BigPictureStyle extends Style {
        private Bitmap picture;
        private Bitmap largeIcon;

        public BigPictureStyle() {}
        public BigPictureStyle(Builder builder) { setBuilder(builder); }

        public BigPictureStyle setBigContentTitle(CharSequence title) {
            internalSetBigContentTitle(title);
            return this;
        }

        public BigPictureStyle setSummaryText(CharSequence cs) {
            internalSetSummaryText(cs);
            return this;
        }

        public BigPictureStyle bigPicture(Bitmap b) {
            this.picture = b;
            return this;
        }

        public BigPictureStyle bigLargeIcon(Bitmap b) {
            this.largeIcon = b;
            return this;
        }

        public BigPictureStyle bigLargeIcon(Icon icon) {
            return this;
        }
    }

    public static class Builder {
        private final Context context;
        private final Notification n = new Notification();
        private final List<Action> actions = new ArrayList<>();
        private Style style;

        public Builder(Context context) {
            this(context, null);
        }

        public Builder(Context context, String channelId) {
            this.context = context;
            n.channelId = channelId;
        }

        public Builder setChannelId(String channelId) {
            n.channelId = channelId;
            return this;
        }

        public Builder setWhen(long when) {
            n.when = when;
            return this;
        }

        public Builder setShowWhen(boolean show) {
            n.extras.putBoolean(EXTRA_SHOW_WHEN, show);
            return this;
        }

        public Builder setSmallIcon(int icon) {
            n.icon = icon;
            n.smallIconObj = context != null ? Icon.createWithResource(context, icon) : null;
            return this;
        }

        public Builder setSmallIcon(int icon, int level) {
            n.iconLevel = level;
            return setSmallIcon(icon);
        }

        public Builder setSmallIcon(Icon icon) {
            n.smallIconObj = icon;
            if (icon != null) n.icon = icon.getResId();
            return this;
        }

        public Builder setLargeIcon(Bitmap b) {
            n.largeIcon = b;
            return this;
        }

        public Builder setLargeIcon(Icon icon) {
            n.largeIconObj = icon;
            if (icon != null) n.largeIcon = icon.getBitmap();
            return this;
        }

        public Builder setContentTitle(CharSequence title) {
            n.extras.putCharSequence(EXTRA_TITLE, title);
            return this;
        }

        public Builder setContentText(CharSequence text) {
            n.extras.putCharSequence(EXTRA_TEXT, text);
            return this;
        }

        public Builder setSubText(CharSequence text) {
            n.extras.putCharSequence(EXTRA_SUB_TEXT, text);
            return this;
        }

        public Builder setContentInfo(CharSequence info) {
            n.extras.putCharSequence(EXTRA_INFO_TEXT, info);
            return this;
        }

        public Builder setTicker(CharSequence tickerText) {
            n.tickerText = tickerText;
            return this;
        }

        public Builder setTicker(CharSequence tickerText, RemoteViews views) {
            n.tickerText = tickerText;
            return this;
        }

        public Builder setNumber(int number) {
            n.number = number;
            return this;
        }

        public Builder setContentIntent(PendingIntent intent) {
            n.contentIntent = intent;
            return this;
        }

        public Builder setDeleteIntent(PendingIntent intent) {
            n.deleteIntent = intent;
            return this;
        }

        public Builder setFullScreenIntent(PendingIntent intent, boolean highPriority) {
            n.fullScreenIntent = intent;
            return this;
        }

        public Builder setAutoCancel(boolean autoCancel) {
            return setFlag(FLAG_AUTO_CANCEL, autoCancel);
        }

        public Builder setOngoing(boolean ongoing) {
            return setFlag(FLAG_ONGOING_EVENT, ongoing);
        }

        public Builder setOnlyAlertOnce(boolean onlyAlertOnce) {
            return setFlag(FLAG_ONLY_ALERT_ONCE, onlyAlertOnce);
        }

        public Builder setFlag(int mask, boolean value) {
            if (value) n.flags |= mask;
            else n.flags &= ~mask;
            return this;
        }

        public Builder setDefaults(int defaults) {
            n.defaults = defaults;
            return this;
        }

        public Builder setPriority(int pri) {
            n.priority = pri;
            return this;
        }

        public Builder setColor(int argb) {
            n.color = argb;
            return this;
        }

        public Builder setColorized(boolean colorize) {
            return this;
        }

        public Builder setVisibility(int visibility) {
            n.visibility = visibility;
            return this;
        }

        public Builder setCategory(String category) {
            n.category = category;
            return this;
        }

        public Builder setGroup(String groupKey) {
            n.groupKey = groupKey;
            return this;
        }

        public Builder setGroupSummary(boolean isGroupSummary) {
            return setFlag(FLAG_GROUP_SUMMARY, isGroupSummary);
        }

        public Builder setSortKey(String sortKey) {
            n.sortKey = sortKey;
            return this;
        }

        public Builder setProgress(int max, int progress, boolean indeterminate) {
            n.extras.putInt(EXTRA_PROGRESS_MAX, max);
            n.extras.putInt(EXTRA_PROGRESS, progress);
            n.extras.putBoolean(EXTRA_PROGRESS_INDETERMINATE, indeterminate);
            return this;
        }

        public Builder setSound(Uri sound) {
            n.sound = sound;
            return this;
        }

        public Builder setSound(Uri sound, int streamType) {
            n.sound = sound;
            n.audioStreamType = streamType;
            return this;
        }

        public Builder setSound(Uri sound, AudioAttributes audioAttributes) {
            n.sound = sound;
            n.audioAttributes = audioAttributes;
            return this;
        }

        public Builder setVibrate(long[] pattern) {
            n.vibrate = pattern;
            return this;
        }

        public Builder setLights(int argb, int onMs, int offMs) {
            n.ledARGB = argb;
            n.ledOnMS = onMs;
            n.ledOffMS = offMs;
            return this;
        }

        public Builder setContent(RemoteViews views) {
            n.contentView = views;
            return this;
        }

        public Builder setCustomContentView(RemoteViews contentView) {
            n.contentView = contentView;
            return this;
        }

        public Builder setCustomBigContentView(RemoteViews contentView) {
            n.bigContentView = contentView;
            return this;
        }

        public Builder setCustomHeadsUpContentView(RemoteViews contentView) {
            n.headsUpContentView = contentView;
            return this;
        }

        public Builder addAction(int icon, CharSequence title, PendingIntent intent) {
            actions.add(new Action(icon, title, intent));
            return this;
        }

        public Builder addAction(Action action) {
            if (action != null) actions.add(action);
            return this;
        }

        public Builder setActions(Action... actions) {
            this.actions.clear();
            if (actions != null) Collections.addAll(this.actions, actions);
            return this;
        }

        public Builder setStyle(Style style) {
            this.style = style;
            if (style != null) style.setBuilder(this);
            return this;
        }

        public Style getStyle() { return style; }

        public Builder addExtras(Bundle extras) {
            if (extras != null) n.extras.putAll(extras);
            return this;
        }

        public Builder setExtras(Bundle extras) {
            n.extras.clear();
            if (extras != null) n.extras.putAll(extras);
            return this;
        }

        public Bundle getExtras() { return n.extras; }

        public Builder extend(Extender extender) {
            if (extender != null) extender.extend(this);
            return this;
        }

        public Builder setTimeoutAfter(long durationMs) { return this; }
        public Builder setLocalOnly(boolean localOnly) { return this; }
        public Builder setUsesChronometer(boolean b) { return this; }
        public Builder setBadgeIconType(int icon) { return this; }
        public Builder setShortcutId(String shortcutId) { return this; }
        public Builder setGroupAlertBehavior(int groupAlertBehavior) { return this; }
        public Builder setPublicVersion(Notification n) { return this; }
        public Builder addPerson(String uri) { return this; }

        @Deprecated
        public Notification getNotification() {
            return build();
        }

        public Notification build() {
            Notification built = n.clone();
            if (!actions.isEmpty()) {
                built.actions = actions.toArray(new Action[0]);
            }
            if (style != null) {
                style.addExtras(built.extras);
            }
            return built;
        }
    }
}
