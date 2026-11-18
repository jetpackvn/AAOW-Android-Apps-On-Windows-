package android.webkit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Message;
import android.view.View;

public class WebChromeClient {
    public interface CustomViewCallback {
        void onCustomViewHidden();
    }

    public static abstract class FileChooserParams {
        public static final int MODE_OPEN = 0;
        public static final int MODE_OPEN_MULTIPLE = 1;
        public static final int MODE_SAVE = 3;

        public abstract int getMode();

        public abstract String[] getAcceptTypes();

        public abstract boolean isCaptureEnabled();

        public abstract CharSequence getTitle();

        public abstract String getFilenameHint();

        public abstract Intent createIntent();

        public static Uri[] parseResult(int resultCode, Intent data) {
            if (resultCode != android.app.Activity.RESULT_OK || data == null) return null;
            if (data.getData() != null) {
                return new Uri[]{data.getData()};
            }
            return null;
        }
    }

    public void onProgressChanged(WebView view, int newProgress) {
    }

    public void onReceivedTitle(WebView view, String title) {
    }

    public void onReceivedIcon(WebView view, Bitmap icon) {
    }

    public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed) {
    }

    public void onShowCustomView(View view, CustomViewCallback callback) {
    }

    public void onHideCustomView() {
    }

    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
        return false;
    }

    public void onRequestFocus(WebView view) {
    }

    public void onCloseWindow(WebView window) {
    }

    public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
        return false;
    }

    public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
        return false;
    }

    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
        return false;
    }

    public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
        return false;
    }

    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        return false;
    }

    @Deprecated
    public void onConsoleMessage(String message, int lineNumber, String sourceID) {
    }

    public Bitmap getDefaultVideoPoster() {
        return null;
    }

    public View getVideoLoadingProgressView() {
        return null;
    }

    public void getVisitedHistory(ValueCallback<String[]> callback) {
    }

    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        return false;
    }
}
