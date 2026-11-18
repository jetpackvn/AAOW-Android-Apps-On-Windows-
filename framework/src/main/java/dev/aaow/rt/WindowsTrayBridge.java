package dev.aaow.rt;

import android.app.Notification;
import android.app.PendingIntent;
import android.util.Log;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;

/**
 * Cầu nối đẩy thông báo Android (NotificationManager) ra Windows System Tray / Action Center
 * và thông báo trực quan trên HostWindow.
 */
public final class WindowsTrayBridge {
    private static final String TAG = "AAOW-Tray";
    private static volatile TrayIcon trayIcon;
    private static volatile PendingIntent latestContentIntent;
    private static volatile BiConsumer<String, String> inWindowBannerListener;

    private WindowsTrayBridge() {
    }

    public static void setInWindowBannerListener(BiConsumer<String, String> listener) {
        inWindowBannerListener = listener;
    }

    public static boolean isTraySupported() {
        return !GraphicsEnvironment.isHeadless() && SystemTray.isSupported();
    }

    public static void postNotification(String appName, String title, String text,
                                        Image appIcon, Notification notification) {
        String safeTitle = (title != null && !title.isBlank()) ? title
                : (appName != null && !appName.isBlank() ? appName : "AAOW Notification");
        String safeText = text != null ? text : "";
        latestContentIntent = notification != null ? notification.contentIntent : null;

        Log.i(TAG, "[" + safeTitle + "] " + safeText);
        if (inWindowBannerListener != null) {
            inWindowBannerListener.accept(safeTitle, safeText);
        }

        if (!isTraySupported()) return;
        try {
            TrayIcon icon = ensureTrayIcon(appName, appIcon);
            if (icon != null) {
                TrayIcon.MessageType msgType = TrayIcon.MessageType.INFO;
                if (notification != null && notification.priority >= Notification.PRIORITY_HIGH) {
                    msgType = TrayIcon.MessageType.WARNING;
                }
                icon.displayMessage(safeTitle, safeText.isEmpty() ? " " : safeText, msgType);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Không hiển thị được Windows SystemTray notification: " + t.getMessage());
        }
    }

    private static synchronized TrayIcon ensureTrayIcon(String appName, Image appIcon) throws Exception {
        if (trayIcon != null) return trayIcon;
        SystemTray tray = SystemTray.getSystemTray();
        Image img = appIcon != null ? appIcon : createDefaultBadgeImage();
        TrayIcon ti = new TrayIcon(img, appName != null ? appName + " — AAOW" : "AAOW");
        ti.setImageAutoSize(true);
        ti.addActionListener(e -> {
            PendingIntent pi = latestContentIntent;
            if (pi != null) {
                try {
                    pi.send();
                } catch (Throwable ignored) {
                }
            }
        });
        tray.add(ti);
        trayIcon = ti;
        return ti;
    }

    private static Image createDefaultBadgeImage() {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(0x4F5BD5));
        g2.fillRoundRect(2, 2, 28, 28, 8, 8);
        g2.setColor(Color.WHITE);
        g2.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        g2.drawString("A", 10, 22);
        g2.dispose();
        return img;
    }
}
