package android.webkit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsoluteLayout;
import dev.aaow.rt.WebViewEngineBridge;

import java.util.Map;

/**
 * Cài đặt android.webkit.WebView chạy trên JavaFX WebKit (HTML5 / CSS3 / JavaScript).
 */
public class WebView extends AbsoluteLayout
        implements ViewGroup.OnHierarchyChangeListener, ViewTreeObserver.OnGlobalFocusChangeListener {

    public interface FindListener {
        void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting);
    }

    private final WebSettings settings = new WebSettings();
    private final WebViewEngineBridge engineBridge;
    private WebViewClient webViewClient;
    private WebChromeClient webChromeClient;
    private DownloadListener downloadListener;
    private FindListener findListener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public WebView(Context context) {
        this(context, null, 0);
    }

    public WebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFocusable(true);
        setClickable(true);
        this.engineBridge = new WebViewEngineBridge(this, settings);
    }

    public WebView(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
        this(context, attrs, defStyleAttr);
    }

    public WebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this(context, attrs, defStyleAttr);
    }

    public WebViewEngineBridge getEngineBridge() {
        return engineBridge;
    }

    public WebSettings getSettings() {
        return settings;
    }

    public void setWebViewClient(WebViewClient client) {
        this.webViewClient = client;
    }

    public WebViewClient getWebViewClient() {
        return webViewClient;
    }

    public void setWebChromeClient(WebChromeClient client) {
        this.webChromeClient = client;
    }

    public WebChromeClient getWebChromeClient() {
        return webChromeClient;
    }

    public void setDownloadListener(DownloadListener listener) {
        this.downloadListener = listener;
    }

    public DownloadListener getDownloadListener() {
        return downloadListener;
    }

    public void setFindListener(FindListener listener) {
        this.findListener = listener;
    }

    public void loadUrl(String url) {
        engineBridge.loadUrl(url, null);
    }

    public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
        engineBridge.loadUrl(url, additionalHttpHeaders);
    }

    public void loadData(String data, String mimeType, String encoding) {
        engineBridge.loadData(data, mimeType, encoding);
    }

    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        engineBridge.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    public void evaluateJavascript(String script, ValueCallback<String> resultCallback) {
        engineBridge.evaluateJavascript(script, resultCallback);
    }

    public void addJavascriptInterface(Object object, String name) {
        engineBridge.addJavascriptInterface(object, name);
    }

    public void removeJavascriptInterface(String name) {
        engineBridge.removeJavascriptInterface(name);
    }

    public boolean canGoBack() {
        return engineBridge.canGoBack();
    }

    public void goBack() {
        engineBridge.goBack();
    }

    public boolean canGoForward() {
        return engineBridge.canGoForward();
    }

    public void goForward() {
        engineBridge.goForward();
    }

    public boolean canGoBackOrForward(int steps) {
        return engineBridge.canGoBackOrForward(steps);
    }

    public void goBackOrForward(int steps) {
        engineBridge.goBackOrForward(steps);
    }

    public WebBackForwardList copyBackForwardList() {
        return engineBridge.copyBackForwardList();
    }

    public void clearHistory() {
        engineBridge.clearHistory();
    }

    public void clearCache(boolean includeDiskFiles) {
        engineBridge.clearCache(includeDiskFiles);
    }

    public void clearFormData() {
    }

    public void clearSslPreferences() {
    }

    public void stopLoading() {
        engineBridge.stopLoading();
    }

    public void reload() {
        engineBridge.reload();
    }

    public void onPause() {
    }

    public void onResume() {
        engineBridge.syncSettings();
    }

    public void destroy() {
        engineBridge.destroy();
    }

    public String getUrl() {
        return engineBridge.getUrl();
    }

    public String getOriginalUrl() {
        return engineBridge.getOriginalUrl();
    }

    public String getTitle() {
        return engineBridge.getTitle();
    }

    public Bitmap getFavicon() {
        return null;
    }

    public int getProgress() {
        return engineBridge.getProgress();
    }

    public int getContentHeight() {
        return engineBridge.getContentHeight();
    }

    public int findAll(String find) {
        int count = engineBridge.findAll(find);
        if (findListener != null) {
            findListener.onFindResultReceived(0, count, true);
        }
        return count;
    }

    public void findAllAsync(String find) {
        findAll(find);
    }

    public void findNext(boolean forward) {
        engineBridge.findNext(forward);
    }

    public void clearMatches() {
    }

    public boolean zoomIn() {
        return engineBridge.zoomIn();
    }

    public boolean zoomOut() {
        return engineBridge.zoomOut();
    }

    public void zoomBy(float zoomFactor) {
        engineBridge.zoomBy(zoomFactor);
    }

    public WebBackForwardList saveState(Bundle outState) {
        if (outState != null) {
            outState.putString("aaow.webview.url", getUrl());
        }
        return copyBackForwardList();
    }

    public WebBackForwardList restoreState(Bundle inState) {
        if (inState != null) {
            String u = inState.getString("aaow.webview.url");
            if (u != null && !u.isEmpty() && !"about:blank".equals(u)) {
                loadUrl(u);
            }
        }
        return copyBackForwardList();
    }

    public PrintDocumentAdapter createPrintDocumentAdapter() {
        return createPrintDocumentAdapter("WebViewDocument");
    }

    public PrintDocumentAdapter createPrintDocumentAdapter(String documentName) {
        String name = documentName != null ? documentName : "Document";
        return new PrintDocumentAdapter() {
            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                                 android.os.CancellationSignal cancellationSignal,
                                 LayoutResultCallback callback, Bundle extras) {
                if (callback != null) {
                    callback.onLayoutFinished(new PrintDocumentInfo.Builder(name)
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(1)
                            .build(), true);
                }
            }

            @Override
            public void onWrite(android.print.PageRange[] pages, android.os.ParcelFileDescriptor destination,
                                android.os.CancellationSignal cancellationSignal,
                                WriteResultCallback callback) {
                if (callback != null) {
                    callback.onWriteFinished(new android.print.PageRange[]{android.print.PageRange.ALL_PAGES});
                }
            }
        };
    }

    public static void setWebContentsDebuggingEnabled(boolean enabled) {
    }

    public Looper getWebViewLooper() {
        return Looper.getMainLooper();
    }

    public Handler getHandler() {
        return handler;
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
    }

    @Override
    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            engineBridge.resize(w, h);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        engineBridge.dispatchTouchEvent(event);
        return true;
    }

    @Override
    public boolean onMouseWheel(int wheelRotation, float x, float y) {
        return engineBridge.dispatchMouseWheel(wheelRotation, x, y);
    }

    @Override
    public void onHostKeyTyped(char ch) {
        engineBridge.dispatchHostKeyTyped(ch);
    }

    @Override
    public void onHostKeyPressed(java.awt.event.KeyEvent e) {
        engineBridge.dispatchHostKeyPressed(e);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        engineBridge.draw(canvas, getWidth(), getHeight());
    }
}
