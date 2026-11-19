package android.appwidget;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.widget.RemoteViews;

import java.util.Collections;
import java.util.List;

/**
 * Cài đặt android.appwidget.AppWidgetManager an toàn cho các ứng dụng có kèm Home Screen Widget
 * (Currency, Forecastie, OMWeather, Privacy Friendly Notes...).
 */
public class AppWidgetManager {
    public static final String ACTION_APPWIDGET_PICK = "android.appwidget.action.APPWIDGET_PICK";
    public static final String ACTION_APPWIDGET_CONFIGURE = "android.appwidget.action.APPWIDGET_CONFIGURE";
    public static final String ACTION_APPWIDGET_UPDATE = "android.appwidget.action.APPWIDGET_UPDATE";
    public static final String ACTION_APPWIDGET_OPTIONS_CHANGED = "android.appwidget.action.APPWIDGET_UPDATE_OPTIONS";
    public static final String ACTION_APPWIDGET_DELETED = "android.appwidget.action.APPWIDGET_DELETED";
    public static final String ACTION_APPWIDGET_DISABLED = "android.appwidget.action.APPWIDGET_DISABLED";
    public static final String ACTION_APPWIDGET_ENABLED = "android.appwidget.action.APPWIDGET_ENABLED";

    public static final String EXTRA_APPWIDGET_ID = "appWidgetId";
    public static final String EXTRA_APPWIDGET_IDS = "appWidgetIds";
    public static final String EXTRA_APPWIDGET_OPTIONS = "appWidgetOptions";
    public static final String EXTRA_APPWIDGET_PROVIDER = "appWidgetProvider";

    public static final int INVALID_APPWIDGET_ID = 0;

    private static final AppWidgetManager INSTANCE = new AppWidgetManager();

    public AppWidgetManager() {
    }

    public static AppWidgetManager getInstance(Context context) {
        return INSTANCE;
    }

    public void updateAppWidget(int[] appWidgetIds, RemoteViews views) {
    }

    public void updateAppWidget(int appWidgetId, RemoteViews views) {
    }

    public void updateAppWidget(ComponentName provider, RemoteViews views) {
    }

    public void partiallyUpdateAppWidget(int[] appWidgetIds, RemoteViews views) {
    }

    public void partiallyUpdateAppWidget(int appWidgetId, RemoteViews views) {
    }

    public void notifyAppWidgetViewDataChanged(int[] appWidgetIds, int viewId) {
    }

    public void notifyAppWidgetViewDataChanged(int appWidgetId, int viewId) {
    }

    public int[] getAppWidgetIds(ComponentName provider) {
        return new int[0];
    }

    public Bundle getAppWidgetOptions(int appWidgetId) {
        return new Bundle();
    }

    public void updateAppWidgetOptions(int appWidgetId, Bundle options) {
    }

    public AppWidgetProviderInfo getAppWidgetInfo(int appWidgetId) {
        return null;
    }

    public List<AppWidgetProviderInfo> getInstalledProviders() {
        return Collections.emptyList();
    }

    public boolean isRequestPinAppWidgetSupported() {
        return false;
    }

    public boolean requestPinAppWidget(ComponentName provider, Bundle extras, PendingIntent successCallback) {
        return false;
    }
}
