package dev.aaow.rt;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Cầu nối mở liên kết Web (Intent.ACTION_VIEW với http:// hoặc https://) ra trình duyệt mặc định của Windows.
 * Hỗ trợ chế độ kiểm thử (testBrowserHandler / suppressExternalBrowser) để không bật cửa sổ trình duyệt ngoài
 * khi chạy JUnit 5 hoặc chụp màn hình headless.
 */
public final class WindowsBrowserBridge {
    private static final String TAG = "AAOW-Browser";

    private static volatile Consumer<Uri> testBrowserHandler;
    private static volatile boolean suppressExternalBrowser = false;
    private static final List<Uri> openedUrls = Collections.synchronizedList(new ArrayList<>());

    private WindowsBrowserBridge() {
    }

    public static void setTestBrowserHandler(Consumer<Uri> handler) {
        testBrowserHandler = handler;
    }

    public static void setSuppressExternalBrowser(boolean suppress) {
        suppressExternalBrowser = suppress;
    }

    public static List<Uri> getOpenedUrls() {
        synchronized (openedUrls) {
            return new ArrayList<>(openedUrls);
        }
    }

    public static void clearOpenedUrls() {
        openedUrls.clear();
    }

    public static boolean isWebBrowserIntent(Intent rawIntent) {
        if (rawIntent == null) return false;
        Intent intent = WindowsFilePickerBridge.unwrapChooser(rawIntent);
        if (intent == null || intent.getComponent() != null) return false;
        if (!Intent.ACTION_VIEW.equals(intent.getAction())) return false;
        Uri data = intent.getData();
        if (data == null) return false;
        String scheme = data.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    public static boolean handleBrowserIntent(Context ctx, Intent rawIntent) {
        if (!isWebBrowserIntent(rawIntent)) return false;
        Intent intent = WindowsFilePickerBridge.unwrapChooser(rawIntent);
        Uri uri = intent.getData();
        if (uri == null) return false;

        openedUrls.add(uri);
        Consumer<Uri> handler = testBrowserHandler;
        if (handler != null) {
            handler.accept(uri);
            return true;
        }
        if (suppressExternalBrowser || GraphicsEnvironment.isHeadless()) {
            Log.i(TAG, "Mở URL ngoài (headless/suppressed): " + uri);
            return true;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(uri.toString()));
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "Không mở được trình duyệt Windows cho URL " + uri + ": " + e);
        }
        return true;
    }
}
