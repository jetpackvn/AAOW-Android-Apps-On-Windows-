package android.webkit;

import android.graphics.Bitmap;
import android.view.KeyEvent;

public class WebViewClient {
    public static final int ERROR_UNKNOWN = -1;
    public static final int ERROR_HOST_LOOKUP = -2;
    public static final int ERROR_UNSUPPORTED_AUTH_SCHEME = -3;
    public static final int ERROR_AUTHENTICATION = -4;
    public static final int ERROR_PROXY_AUTHENTICATION = -5;
    public static final int ERROR_CONNECT = -6;
    public static final int ERROR_IO = -7;
    public static final int ERROR_TIMEOUT = -8;
    public static final int ERROR_REDIRECT_LOOP = -9;
    public static final int ERROR_UNSUPPORTED_SCHEME = -10;
    public static final int ERROR_FAILED_SSL_HANDSHAKE = -11;
    public static final int ERROR_BAD_URL = -12;
    public static final int ERROR_FILE = -13;
    public static final int ERROR_FILE_NOT_FOUND = -14;
    public static final int ERROR_TOO_MANY_REQUESTS = -15;

    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return false;
    }

    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return shouldOverrideUrlLoading(view, request != null && request.getUrl() != null ? request.getUrl().toString() : null);
    }

    public void onPageStarted(WebView view, String url, Bitmap favicon) {
    }

    public void onPageFinished(WebView view, String url) {
    }

    public void onPageCommitVisible(WebView view, String url) {
    }

    public void onLoadResource(WebView view, String url) {
    }

    @Deprecated
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        return null;
    }

    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return shouldInterceptRequest(view, request != null && request.getUrl() != null ? request.getUrl().toString() : null);
    }

    @Deprecated
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
    }

    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        if (request != null && error != null) {
            onReceivedError(view, error.getErrorCode(),
                    error.getDescription() != null ? error.getDescription().toString() : "",
                    request.getUrl() != null ? request.getUrl().toString() : "");
        }
    }

    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
    }

    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
    }

    public void onScaleChanged(WebView view, float oldScale, float newScale) {
    }

    public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
    }
}
