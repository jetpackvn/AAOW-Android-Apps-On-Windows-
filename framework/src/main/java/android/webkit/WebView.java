package android.webkit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;

public class WebView extends AbsoluteLayout implements ViewGroup.OnHierarchyChangeListener {
    private String currentUrl = "about:blank";
    private final WebSettings settings = new WebSettings();
    private WebViewClient webViewClient;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public WebView(Context context) {
        super(context);
    }

    public WebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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

    public boolean canGoBack() {
        return false;
    }

    public void goBack() {
    }

    public void reload() {
        invalidate();
    }

    public void clearCache(boolean includeDiskFiles) {
    }

    public void clearHistory() {
    }

    @Override
    public void onChildViewAdded(android.view.View parent, android.view.View child) {
    }

    @Override
    public void onChildViewRemoved(android.view.View parent, android.view.View child) {
    }

    public void loadUrl(String url) {
        this.currentUrl = url != null ? url : "";
        invalidate();
    }

    public void loadData(String data, String mimeType, String encoding) {
        this.currentUrl = "data:" + (mimeType != null ? mimeType : "text/html");
        invalidate();
    }

    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        this.currentUrl = baseUrl != null ? baseUrl : "data:text/html";
        invalidate();
    }

    public String getUrl() {
        return currentUrl;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFF3F4F8);
        canvas.drawRoundRect(0, 0, w, h, 8, 8, paint);
        paint.setColor(0xFF4F5BD5);
        paint.setTextSize(13f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("WebView: " + currentUrl, w / 2f, h / 2f, paint);
    }
}
