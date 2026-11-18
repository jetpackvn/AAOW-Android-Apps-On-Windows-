package android.app;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import dev.aaow.rt.WindowsTrayBridge;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý kênh và thông báo Android, đồng thời đẩy thông báo thật ra Windows System Tray.
 */
public class NotificationManager {
    public static final int IMPORTANCE_UNSPECIFIED = -1000;
    public static final int IMPORTANCE_NONE = 0;
    public static final int IMPORTANCE_MIN = 1;
    public static final int IMPORTANCE_LOW = 2;
    public static final int IMPORTANCE_DEFAULT = 3;
    public static final int IMPORTANCE_HIGH = 4;
    public static final int IMPORTANCE_MAX = 5;

    public static final int INTERRUPTION_FILTER_ALL = 1;
    public static final int INTERRUPTION_FILTER_PRIORITY = 2;
    public static final int INTERRUPTION_FILTER_NONE = 3;
    public static final int INTERRUPTION_FILTER_ALARMS = 4;
    public static final int INTERRUPTION_FILTER_UNKNOWN = 0;

    public record PostedNotification(String tag, int id, Notification notification, long postTime) {
    }

    private final Context context;
    private final Map<String, NotificationChannel> channels = new ConcurrentHashMap<>();
    private final Map<String, NotificationChannelGroup> channelGroups = new ConcurrentHashMap<>();
    private final Map<String, PostedNotification> activeNotifications = new ConcurrentHashMap<>();

    public NotificationManager(Context context) {
        this.context = context;
    }

    private static String notifKey(String tag, int id) {
        return (tag != null ? tag : "") + "#" + id;
    }

    public void createNotificationChannel(NotificationChannel channel) {
        if (channel != null && channel.getId() != null) {
            channels.put(channel.getId(), channel);
        }
    }

    public void createNotificationChannels(List<NotificationChannel> channels) {
        if (channels != null) {
            for (NotificationChannel ch : channels) createNotificationChannel(ch);
        }
    }

    public NotificationChannel getNotificationChannel(String channelId) {
        return channelId != null ? channels.get(channelId) : null;
    }

    public NotificationChannel getNotificationChannel(String channelId, String conversationId) {
        return getNotificationChannel(channelId);
    }

    public List<NotificationChannel> getNotificationChannels() {
        return new ArrayList<>(channels.values());
    }

    public void deleteNotificationChannel(String channelId) {
        if (channelId != null) channels.remove(channelId);
    }

    public void createNotificationChannelGroup(NotificationChannelGroup group) {
        if (group != null && group.getId() != null) {
            channelGroups.put(group.getId(), group);
        }
    }

    public void createNotificationChannelGroups(List<NotificationChannelGroup> groups) {
        if (groups != null) {
            for (NotificationChannelGroup g : groups) createNotificationChannelGroup(g);
        }
    }

    public NotificationChannelGroup getNotificationChannelGroup(String channelGroupId) {
        return channelGroupId != null ? channelGroups.get(channelGroupId) : null;
    }

    public List<NotificationChannelGroup> getNotificationChannelGroups() {
        return new ArrayList<>(channelGroups.values());
    }

    public void deleteNotificationChannelGroup(String groupId) {
        if (groupId != null) channelGroups.remove(groupId);
    }

    public void notify(int id, Notification notification) {
        notify(null, id, notification);
    }

    public void notify(String tag, int id, Notification notification) {
        if (notification == null) return;
        String chId = notification.getChannelId();
        if (chId != null) {
            NotificationChannel ch = channels.get(chId);
            if (ch != null && ch.getImportance() == IMPORTANCE_NONE) {
                return;
            }
        }
        activeNotifications.put(notifKey(tag, id),
                new PostedNotification(tag, id, notification, System.currentTimeMillis()));

        String appName = context != null && context.getApplicationInfo() != null
                && context.getApplicationInfo().nonLocalizedLabel != null
                ? context.getApplicationInfo().nonLocalizedLabel.toString()
                : (context != null ? context.getPackageName() : "AAOW");
        CharSequence titleCs = notification.getResolvedTitle();
        CharSequence textCs = notification.getResolvedText();
        String title = titleCs != null ? titleCs.toString() : appName;
        String text = textCs != null ? textCs.toString() : "";

        Image icon = null;
        if (notification.largeIcon != null) {
            icon = notification.largeIcon.getAwtImage();
        } else if (context != null && context.getApplicationInfo() != null && context.getApplicationInfo().icon != 0) {
            Bitmap bmp = context.getResources().loadBitmap(context.getApplicationInfo().icon);
            if (bmp != null) icon = bmp.getAwtImage();
        }

        WindowsTrayBridge.postNotification(appName, title, text, icon, notification);
    }

    public void notifyAsPackage(String targetPackage, String tag, int id, Notification notification) {
        notify(tag, id, notification);
    }

    public void cancel(int id) {
        cancel(null, id);
    }

    public void cancel(String tag, int id) {
        activeNotifications.remove(notifKey(tag, id));
    }

    public void cancelAll() {
        activeNotifications.clear();
    }

    public List<PostedNotification> getActivePostedNotifications() {
        return new ArrayList<>(activeNotifications.values());
    }

    public boolean areNotificationsEnabled() {
        return true;
    }

    public int getImportance() {
        return IMPORTANCE_DEFAULT;
    }

    public int getCurrentInterruptionFilter() {
        return INTERRUPTION_FILTER_ALL;
    }

    public boolean isNotificationPolicyAccessGranted() {
        return true;
    }

    public boolean isNotificationListenerAccessGranted(ComponentName listener) {
        return true;
    }
}
