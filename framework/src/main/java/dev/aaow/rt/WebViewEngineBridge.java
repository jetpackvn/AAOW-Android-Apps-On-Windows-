package dev.aaow.rt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import dev.aaow.apk.ApkFile;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.Event;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import netscape.javascript.JSObject;

import javax.swing.JEditorPane;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cầu nối kết xuất HTML5/CSS3/JS thật cho android.webkit.WebView dựa trên JavaFX WebKit.
 * Tự động giải nén assets của APK để file:///android_asset/ hoạt động đầy đủ và chụp snapshot
 * đồng bộ vào Java2D Canvas của HostWindow (hỗ trợ cả cửa sổ tương tác và --screenshot).
 */
public final class WebViewEngineBridge {
    private static final String TAG = "AAOW-WebView";
    private static final Set<WebViewEngineBridge> ACTIVE_BRIDGES = ConcurrentHashMap.newKeySet();

    private static volatile boolean fxInitAttempted;
    private static volatile boolean fxAvailable;

    private final WebView androidWebView;
    private final WebSettings settings;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Object> jsInterfaces = new ConcurrentHashMap<>();
    private final List<HistoryEntry> history = new ArrayList<>();
    private int historyIndex = -1;

    private volatile javafx.scene.web.WebView fxWebView;
    private volatile javafx.scene.web.WebEngine fxEngine;
    private volatile Stage fxStage;
    private volatile BufferedImage snapshotImage;
    private volatile int viewWidth = 420;
    private volatile int viewHeight = 700;
    private volatile int contentHeightCss = 0;
    private volatile int progress = 0;
    private volatile String currentUrl = "about:blank";
    private volatile String originalUrl = "about:blank";
    private volatile String currentTitle = "";
    private volatile String lastHtmlContent = "";
    private volatile boolean loading;
    private volatile boolean destroyed;
    private volatile Path extractedAssetRoot;
    private volatile CountDownLatch currentLoadLatch;
    private volatile String lastSearchQuery;

    private record HistoryEntry(String url, String originalUrl, String title) {
    }

    public WebViewEngineBridge(WebView androidWebView, WebSettings settings) {
        this.androidWebView = androidWebView;
        this.settings = settings;
        ACTIVE_BRIDGES.add(this);
        ensureAssetRootPrepared();
        if (ensureFxInitialized()) {
            initFxNodeAsync();
        }
    }

    private static synchronized boolean ensureFxInitialized() {
        if (fxInitAttempted) return fxAvailable;
        fxInitAttempted = true;
        try {
            try {
                Platform.startup(() -> Platform.setImplicitExit(false));
            } catch (IllegalStateException alreadyStarted) {
                Platform.setImplicitExit(false);
            }
            fxAvailable = true;
        } catch (Throwable t) {
            Log.w(TAG, "JavaFX không khả dụng, dùng bộ kết xuất HTML dự phòng Swing: " + t.getMessage());
            fxAvailable = false;
        }
        return fxAvailable;
    }

    public static boolean isJavaFxAvailable() {
        return ensureFxInitialized();
    }

    public static void awaitAllIdle(long timeoutMs) {
        long deadline = System.currentTimeMillis() + Math.max(0L, timeoutMs);
        for (WebViewEngineBridge bridge : ACTIVE_BRIDGES) {
            long rem = deadline - System.currentTimeMillis();
            if (rem <= 0) break;
            bridge.awaitIdle(rem);
        }
    }

    public void awaitIdle(long timeoutMs) {
        if (destroyed || !fxAvailable) return;
        CountDownLatch latch = currentLoadLatch;
        if (latch != null && loading) {
            try {
                latch.await(Math.min(timeoutMs, 2500L), TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        captureSnapshotSync(Math.max(100, viewWidth), Math.max(100, viewHeight), Math.min(timeoutMs, 1000L));
    }

    private void initFxNodeAsync() {
        CountDownLatch created = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                if (destroyed) return;
                createFxObjects();
            } finally {
                created.countDown();
            }
        });
        if (!Platform.isFxApplicationThread()) {
            try {
                created.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void createFxObjects() {
        if (fxWebView != null) return;
        javafx.scene.web.WebView wv = new javafx.scene.web.WebView();
        wv.setPrefSize(viewWidth, viewHeight);
        wv.setMinSize(1, 1);
        wv.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        javafx.scene.web.WebEngine engine = wv.getEngine();
        applySettingsOnFxThread(wv, engine);

        StackPane root = new StackPane(wv);
        Scene scene = new Scene(root, viewWidth, viewHeight);

        Stage stage = new Stage(StageStyle.UTILITY);
        stage.setScene(scene);
        stage.setOpacity(0.0);
        stage.setX(-32000);
        stage.setY(-32000);
        stage.setWidth(viewWidth);
        stage.setHeight(viewHeight);
        stage.show();

        this.fxWebView = wv;
        this.fxEngine = engine;
        this.fxStage = stage;

        engine.titleProperty().addListener((obs, oldT, newT) -> {
            currentTitle = newT != null ? newT : "";
            WebChromeClient cc = androidWebView.getWebChromeClient();
            if (cc != null) {
                mainHandler.post(() -> cc.onReceivedTitle(androidWebView, currentTitle));
            }
        });

        engine.getLoadWorker().progressProperty().addListener((obs, oldP, newP) -> {
            attachJsInterfacesOnFxThread();
            int pct = Math.max(0, Math.min(100, (int) Math.round(newP.doubleValue() * 100.0)));
            progress = pct;
            WebChromeClient cc = androidWebView.getWebChromeClient();
            if (cc != null) {
                mainHandler.post(() -> cc.onProgressChanged(androidWebView, pct));
            }
        });

        engine.documentProperty().addListener((obs, oldDoc, newDoc) -> {
            if (newDoc != null) {
                attachJsInterfacesOnFxThread();
            }
        });

        engine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
            if (newLoc == null || newLoc.isEmpty() || "about:blank".equals(newLoc)) return;
            String reported = unmapAssetUrl(newLoc);
            WebViewClient client = androidWebView.getWebViewClient();
            if (client != null && !reported.equals(currentUrl)) {
                WebResourceRequest req = simpleRequest(reported, true);
                if (client.shouldOverrideUrlLoading(androidWebView, req)) {
                    Platform.runLater(() -> engine.getLoadWorker().cancel());
                    return;
                }
            }
            if (!reported.startsWith("data:")) {
                currentUrl = reported;
            }
        });

        engine.getLoadWorker().stateProperty().addListener((obs, oldS, newS) -> {
            if (newS == Worker.State.RUNNING || newS == Worker.State.SCHEDULED) {
                loading = true;
                attachJsInterfacesOnFxThread();
                String url = currentUrl;
                WebViewClient client = androidWebView.getWebViewClient();
                if (client != null) {
                    mainHandler.post(() -> client.onPageStarted(androidWebView, url, null));
                }
            } else if (newS == Worker.State.SUCCEEDED) {
                loading = false;
                progress = 100;
                attachJsInterfacesOnFxThread();
                updateMetricsOnFxThread();
                takeSnapshotOnFxThread();
                CountDownLatch latch = currentLoadLatch;
                if (latch != null) latch.countDown();
                WebViewClient client = androidWebView.getWebViewClient();
                String url = currentUrl;
                if (client != null) {
                    mainHandler.post(() -> {
                        client.onPageCommitVisible(androidWebView, url);
                        client.onPageFinished(androidWebView, url);
                    });
                }
            } else if (newS == Worker.State.FAILED || newS == Worker.State.CANCELLED) {
                loading = false;
                CountDownLatch latch = currentLoadLatch;
                if (latch != null) latch.countDown();
                if (newS == Worker.State.FAILED) {
                    Throwable ex = engine.getLoadWorker().getException();
                    String desc = ex != null && ex.getMessage() != null ? ex.getMessage() : "Load failed";
                    WebViewClient client = androidWebView.getWebViewClient();
                    String url = currentUrl;
                    if (client != null) {
                        WebResourceRequest req = simpleRequest(url, true);
                        WebResourceError err = new WebResourceError() {
                            @Override public int getErrorCode() { return WebViewClient.ERROR_UNKNOWN; }
                            @Override public CharSequence getDescription() { return desc; }
                        };
                        mainHandler.post(() -> client.onReceivedError(androidWebView, req, err));
                    }
                }
            }
        });

        engine.setOnAlert(ev -> {
            WebChromeClient cc = androidWebView.getWebChromeClient();
            if (cc != null) {
                JsResult res = new JsResult();
                cc.onJsAlert(androidWebView, currentUrl, ev.getData(), res);
            }
        });

        engine.setConfirmHandler(msg -> {
            WebChromeClient cc = androidWebView.getWebChromeClient();
            if (cc != null) {
                JsResult res = new JsResult();
                if (cc.onJsConfirm(androidWebView, currentUrl, msg, res)) {
                    return res.getResult();
                }
            }
            return true;
        });

        engine.setPromptHandler(param -> {
            WebChromeClient cc = androidWebView.getWebChromeClient();
            if (cc != null) {
                JsPromptResult res = new JsPromptResult();
                if (cc.onJsPrompt(androidWebView, currentUrl, param.getMessage(), param.getDefaultValue(), res)) {
                    return res.getStringResult();
                }
            }
            return param.getDefaultValue();
        });

        engine.setOnError(ev -> Log.w(TAG, "WebEngine error: " + ev.getMessage()));
    }

    private void applySettingsOnFxThread(javafx.scene.web.WebView wv, javafx.scene.web.WebEngine engine) {
        engine.setJavaScriptEnabled(settings.getJavaScriptEnabled());
        engine.setUserAgent(settings.getUserAgentString());
        double fontScale = (settings.getDefaultFontSize() / 16.0) * (settings.getTextZoom() / 100.0);
        wv.setFontScale(Math.max(0.5, Math.min(3.0, fontScale)));
    }

    public void syncSettings() {
        if (!fxAvailable || fxWebView == null) return;
        Platform.runLater(() -> {
            if (fxWebView != null && fxEngine != null) {
                applySettingsOnFxThread(fxWebView, fxEngine);
            }
        });
    }

    public void resize(int w, int h) {
        if (w <= 0 || h <= 0) return;
        boolean changed = (w != viewWidth || h != viewHeight);
        this.viewWidth = w;
        this.viewHeight = h;
        if (changed && fxAvailable && fxWebView != null) {
            Platform.runLater(() -> {
                if (fxWebView == null) return;
                fxWebView.setPrefSize(w, h);
                fxWebView.resize(w, h);
                if (fxStage != null) {
                    fxStage.setWidth(w);
                    fxStage.setHeight(h);
                }
                fxWebView.applyCss();
                fxWebView.layout();
                updateMetricsOnFxThread();
                takeSnapshotOnFxThread();
            });
        } else if (changed && !fxAvailable && lastHtmlContent != null && !lastHtmlContent.isEmpty()) {
            renderSwingFallbackImage(lastHtmlContent, w, h);
        }
    }

    public void loadUrl(String url, Map<String, String> headers) {
        if (destroyed || url == null) return;
        if (url.startsWith("javascript:")) {
            evaluateJavascript(url.substring("javascript:".length()), null);
            return;
        }

        WebViewClient client = androidWebView.getWebViewClient();
        if (client != null && !url.equals("about:blank")) {
            WebResourceRequest req = simpleRequest(url, true, headers);
            if (client.shouldOverrideUrlLoading(androidWebView, req)) {
                return;
            }
        }

        this.originalUrl = url;
        this.currentUrl = url;
        recordHistory(url, url, "");
        this.progress = 10;

        String mappedUrl = mapAndroidUrl(url);
        if (client != null) {
            WebResourceResponse intercepted = client.shouldInterceptRequest(androidWebView, simpleRequest(url, true, headers));
            if (intercepted != null && intercepted.getData() != null) {
                try {
                    String html = new String(intercepted.getData().readAllBytes(),
                            intercepted.getEncoding() != null ? intercepted.getEncoding() : "UTF-8");
                    loadHtmlInternal(url, html, intercepted.getMimeType());
                    return;
                } catch (IOException ignored) {
                }
            }
        }

        if (!fxAvailable) {
            String html = tryReadLocalHtml(mappedUrl);
            if (html == null) {
                html = "<html><body style='font-family:sans-serif;padding:16px'><h3>" + escapeHtml(url) + "</h3></body></html>";
            }
            loadHtmlInternal(url, html, "text/html");
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        this.currentLoadLatch = latch;
        this.loading = true;
        Platform.runLater(() -> {
            if (destroyed) {
                latch.countDown();
                return;
            }
            createFxObjects();
            applySettingsOnFxThread(fxWebView, fxEngine);
            fxEngine.load(mappedUrl);
        });
    }

    public void loadData(String data, String mimeType, String encoding) {
        loadDataWithBaseURL(null, data, mimeType, encoding, null);
    }

    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        if (destroyed) return;
        String decoded = decodeDataPayload(data, encoding);
        String reportedUrl = historyUrl != null ? historyUrl : (baseUrl != null ? baseUrl : "data:" + (mimeType != null ? mimeType : "text/html"));
        this.originalUrl = reportedUrl;
        this.currentUrl = reportedUrl;
        recordHistory(reportedUrl, reportedUrl, "");
        this.progress = 15;

        String effectiveHtml = rewriteHtmlWithBaseAndAssets(decoded, baseUrl);
        loadHtmlInternal(reportedUrl, effectiveHtml, mimeType != null ? mimeType : "text/html");
    }

    private void loadHtmlInternal(String reportedUrl, String html, String mimeType) {
        this.lastHtmlContent = html != null ? html : "";
        String contentType = (mimeType != null && !mimeType.isBlank()) ? mimeType : "text/html";
        if (!fxAvailable) {
            renderSwingFallbackImage(lastHtmlContent, viewWidth, viewHeight);
            this.loading = false;
            this.progress = 100;
            WebViewClient client = androidWebView.getWebViewClient();
            if (client != null) {
                mainHandler.post(() -> {
                    client.onPageStarted(androidWebView, reportedUrl, null);
                    client.onPageFinished(androidWebView, reportedUrl);
                });
            }
            androidWebView.postInvalidate();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        this.currentLoadLatch = latch;
        this.loading = true;
        Platform.runLater(() -> {
            if (destroyed) {
                latch.countDown();
                return;
            }
            createFxObjects();
            applySettingsOnFxThread(fxWebView, fxEngine);
            fxEngine.loadContent(lastHtmlContent, contentType);
        });
    }

    public void reload() {
        if (destroyed) return;
        if (fxAvailable && fxEngine != null) {
            Platform.runLater(() -> {
                if (fxEngine != null) fxEngine.reload();
            });
        } else if (lastHtmlContent != null && !lastHtmlContent.isEmpty()) {
            renderSwingFallbackImage(lastHtmlContent, viewWidth, viewHeight);
            androidWebView.postInvalidate();
        }
    }

    public void stopLoading() {
        loading = false;
        if (fxAvailable && fxEngine != null) {
            Platform.runLater(() -> {
                if (fxEngine != null) fxEngine.getLoadWorker().cancel();
            });
        }
    }

    public boolean canGoBack() {
        if (fxAvailable && fxEngine != null) {
            return fxEngine.getHistory().getCurrentIndex() > 0 || historyIndex > 0;
        }
        return historyIndex > 0;
    }

    public void goBack() {
        goBackOrForward(-1);
    }

    public boolean canGoForward() {
        if (fxAvailable && fxEngine != null) {
            var h = fxEngine.getHistory();
            return h.getCurrentIndex() < h.getEntries().size() - 1 || historyIndex < history.size() - 1;
        }
        return historyIndex < history.size() - 1;
    }

    public void goForward() {
        goBackOrForward(1);
    }

    public boolean canGoBackOrForward(int steps) {
        int next = historyIndex + steps;
        return next >= 0 && next < history.size();
    }

    public void goBackOrForward(int steps) {
        if (steps == 0) return;
        if (fxAvailable && fxEngine != null) {
            Platform.runLater(() -> {
                try {
                    var h = fxEngine.getHistory();
                    int idx = h.getCurrentIndex() + steps;
                    if (idx >= 0 && idx < h.getEntries().size()) {
                        h.go(steps);
                    }
                } catch (Exception ignored) {
                }
            });
        }
        int next = historyIndex + steps;
        if (next >= 0 && next < history.size()) {
            historyIndex = next;
            HistoryEntry he = history.get(historyIndex);
            this.currentUrl = he.url();
            this.originalUrl = he.originalUrl();
            if (!fxAvailable) {
                loadUrl(he.url(), Collections.emptyMap());
            }
        }
    }

    public WebBackForwardList copyBackForwardList() {
        List<HistoryEntry> snap = new ArrayList<>(history);
        int idx = historyIndex;
        return new WebBackForwardList() {
            @Override public WebHistoryItem getCurrentItem() {
                return idx >= 0 && idx < snap.size() ? toHistoryItem(snap.get(idx)) : null;
            }
            @Override public int getCurrentIndex() { return idx; }
            @Override public WebHistoryItem getItemAtIndex(int index) {
                return index >= 0 && index < snap.size() ? toHistoryItem(snap.get(index)) : null;
            }
            @Override public int getSize() { return snap.size(); }
            @Override protected WebBackForwardList clone() { return this; }
        };
    }

    private static WebHistoryItem toHistoryItem(HistoryEntry he) {
        return new WebHistoryItem() {
            @Override public String getUrl() { return he.url(); }
            @Override public String getOriginalUrl() { return he.originalUrl(); }
            @Override public String getTitle() { return he.title(); }
            @Override public Bitmap getFavicon() { return null; }
            @Override protected WebHistoryItem clone() { return this; }
        };
    }

    public void clearHistory() {
        history.clear();
        historyIndex = -1;
    }

    public void clearCache(boolean includeDiskFiles) {
    }

    public void evaluateJavascript(String script, ValueCallback<String> resultCallback) {
        if (destroyed || script == null) {
            if (resultCallback != null) mainHandler.post(() -> resultCallback.onReceiveValue("null"));
            return;
        }
        if (!fxAvailable) {
            if (resultCallback != null) mainHandler.post(() -> resultCallback.onReceiveValue("null"));
            return;
        }
        Platform.runLater(() -> {
            String json = "null";
            try {
                if (fxEngine != null) {
                    Object res = fxEngine.executeScript(script);
                    json = toJsJsonString(res);
                    takeSnapshotOnFxThread();
                }
            } catch (Throwable t) {
                Log.w(TAG, "JS eval error: " + t.getMessage());
            }
            String finalJson = json;
            if (resultCallback != null) {
                mainHandler.post(() -> resultCallback.onReceiveValue(finalJson));
            }
        });
    }

    public void addJavascriptInterface(Object obj, String name) {
        if (obj == null || name == null || name.isEmpty()) return;
        jsInterfaces.put(name, obj);
        if (fxAvailable && fxEngine != null) {
            Platform.runLater(this::attachJsInterfacesOnFxThread);
        }
    }

    public void removeJavascriptInterface(String name) {
        if (name == null) return;
        jsInterfaces.remove(name);
        if (fxAvailable && fxEngine != null) {
            Platform.runLater(() -> {
                try {
                    if (fxEngine != null) {
                        JSObject win = (JSObject) fxEngine.executeScript("window");
                        if (win != null) win.removeMember(name);
                    }
                } catch (Throwable ignored) {
                }
            });
        }
    }

    private void attachJsInterfacesOnFxThread() {
        if (fxEngine == null || jsInterfaces.isEmpty()) return;
        try {
            JSObject win = (JSObject) fxEngine.executeScript("window");
            if (win != null) {
                for (Map.Entry<String, Object> e : jsInterfaces.entrySet()) {
                    win.setMember(e.getKey(), e.getValue());
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public int findAll(String find) {
        this.lastSearchQuery = find;
        if (find == null || find.isEmpty()) return 0;
        if (fxAvailable && fxEngine != null) {
            CompletableFuture<Integer> fut = new CompletableFuture<>();
            Runnable r = () -> {
                try {
                    String escaped = jsEscape(find);
                    Object countObj = fxEngine.executeScript(
                            "(function(){var t=document.body?document.body.innerText:'';if(!t)return 0;"
                                    + "var q='" + escaped + "'.toLowerCase();t=t.toLowerCase();var c=0,i=0;"
                                    + "while((i=t.indexOf(q,i))>=0){c++;i+=q.length;}window.find('" + escaped + "');return c;})()");
                    takeSnapshotOnFxThread();
                    fut.complete(countObj instanceof Number n ? n.intValue() : 0);
                } catch (Throwable t) {
                    fut.complete(0);
                }
            };
            if (Platform.isFxApplicationThread()) r.run();
            else Platform.runLater(r);
            try {
                return fut.get(1, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    public void findNext(boolean forward) {
        if (lastSearchQuery == null || lastSearchQuery.isEmpty() || !fxAvailable || fxEngine == null) return;
        String escaped = jsEscape(lastSearchQuery);
        Platform.runLater(() -> {
            try {
                fxEngine.executeScript("window.find('" + escaped + "', false, " + (!forward) + ", true)");
                takeSnapshotOnFxThread();
            } catch (Throwable ignored) {
            }
        });
    }

    public void zoomBy(float zoomFactor) {
        if (fxAvailable && fxWebView != null) {
            Platform.runLater(() -> {
                if (fxWebView != null) {
                    fxWebView.setZoom(Math.max(0.25, Math.min(4.0, fxWebView.getZoom() * zoomFactor)));
                    takeSnapshotOnFxThread();
                }
            });
        }
    }

    public boolean zoomIn() {
        zoomBy(1.2f);
        return true;
    }

    public boolean zoomOut() {
        zoomBy(0.833f);
        return true;
    }

    public void dispatchTouchEvent(MotionEvent ev) {
        if (destroyed || ev == null || !fxAvailable || fxWebView == null) return;
        float x = ev.getX();
        float y = ev.getY();
        int action = ev.getActionMasked();
        Platform.runLater(() -> {
            if (fxWebView == null) return;
            javafx.event.EventType<javafx.scene.input.MouseEvent> type = switch (action) {
                case MotionEvent.ACTION_DOWN -> javafx.scene.input.MouseEvent.MOUSE_PRESSED;
                case MotionEvent.ACTION_MOVE -> javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
                case MotionEvent.ACTION_UP -> javafx.scene.input.MouseEvent.MOUSE_RELEASED;
                default -> null;
            };
            if (type != null) {
                Point2D screenPt = new Point2D(x, y);
                javafx.scene.input.MouseEvent me = new javafx.scene.input.MouseEvent(
                        type, x, y, screenPt.getX(), screenPt.getY(), MouseButton.PRIMARY, 1,
                        false, false, false, false,
                        action != MotionEvent.ACTION_UP, false, false, true, false, false, null);
                Event.fireEvent(fxWebView, me);
                if (action == MotionEvent.ACTION_UP) {
                    javafx.scene.input.MouseEvent click = new javafx.scene.input.MouseEvent(
                            javafx.scene.input.MouseEvent.MOUSE_CLICKED, x, y, screenPt.getX(), screenPt.getY(),
                            MouseButton.PRIMARY, 1, false, false, false, false,
                            false, false, false, true, false, false, null);
                    Event.fireEvent(fxWebView, click);
                }
                scheduleSnapshotRefresh();
            }
        });
    }

    public boolean dispatchMouseWheel(int wheelRotation, float x, float y) {
        if (destroyed || !fxAvailable || fxWebView == null) return false;
        double deltaY = -wheelRotation * 48.0;
        Platform.runLater(() -> {
            if (fxWebView == null) return;
            ScrollEvent se = new ScrollEvent(
                    ScrollEvent.SCROLL, x, y, x, y,
                    false, false, false, false, false, false,
                    0.0, deltaY, 0.0, deltaY,
                    ScrollEvent.HorizontalTextScrollUnits.NONE, 0.0,
                    ScrollEvent.VerticalTextScrollUnits.NONE, 0.0,
                    0, null);
            Event.fireEvent(fxWebView, se);
            if (fxEngine != null) {
                try {
                    fxEngine.executeScript("window.scrollBy(0, " + (-deltaY) + ")");
                } catch (Throwable ignored) {
                }
            }
            scheduleSnapshotRefresh();
        });
        return true;
    }

    public void dispatchHostKeyTyped(char ch) {
        if (destroyed || !fxAvailable || fxWebView == null) return;
        Platform.runLater(() -> {
            if (fxWebView == null) return;
            KeyEvent ke = new KeyEvent(KeyEvent.KEY_TYPED, String.valueOf(ch), "", KeyCode.UNDEFINED,
                    false, false, false, false);
            Event.fireEvent(fxWebView, ke);
            scheduleSnapshotRefresh();
        });
    }

    public void dispatchHostKeyPressed(java.awt.event.KeyEvent e) {
        if (destroyed || e == null || !fxAvailable || fxWebView == null) return;
        KeyCode kc = mapAwtKeyCode(e.getKeyCode());
        if (kc == KeyCode.UNDEFINED) return;
        Platform.runLater(() -> {
            if (fxWebView == null) return;
            KeyEvent pressed = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", kc,
                    e.isShiftDown(), e.isControlDown(), e.isAltDown(), false);
            Event.fireEvent(fxWebView, pressed);
            KeyEvent released = new KeyEvent(KeyEvent.KEY_RELEASED, "", "", kc,
                    e.isShiftDown(), e.isControlDown(), e.isAltDown(), false);
            Event.fireEvent(fxWebView, released);
            scheduleSnapshotRefresh();
        });
    }

    public void draw(Canvas canvas, int w, int h) {
        if (w <= 0 || h <= 0) return;
        resize(w, h);
        BufferedImage img = snapshotImage;
        if (img == null && fxAvailable && fxWebView != null && !loading) {
            captureSnapshotSync(w, h, 300);
            img = snapshotImage;
        }
        Graphics2D g2 = canvas.getAwtGraphics();
        if (g2 == null) return;
        if (img != null) {
            g2.drawImage(img, 0, 0, w, h, null);
        } else {
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(0x6B7280));
            g2.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
            String status = loading ? "Đang tải " + currentUrl + "..." : "WebView: " + currentUrl;
            g2.drawString(status, 16, 28);
        }
    }

    public String getUrl() {
        return currentUrl;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getTitle() {
        return currentTitle != null && !currentTitle.isEmpty() ? currentTitle : currentUrl;
    }

    public int getProgress() {
        return progress;
    }

    public int getContentHeight() {
        if (contentHeightCss > 0) return contentHeightCss;
        return viewHeight;
    }

    public BufferedImage getSnapshotImage() {
        return snapshotImage;
    }

    public void destroy() {
        destroyed = true;
        ACTIVE_BRIDGES.remove(this);
        CountDownLatch latch = currentLoadLatch;
        if (latch != null) latch.countDown();
        if (fxAvailable && (fxWebView != null || fxStage != null)) {
            Platform.runLater(() -> {
                if (fxEngine != null) {
                    fxEngine.load(null);
                }
                if (fxStage != null) {
                    fxStage.close();
                    fxStage = null;
                }
                fxWebView = null;
                fxEngine = null;
            });
        }
    }

    private void scheduleSnapshotRefresh() {
        mainHandler.postDelayed(() -> {
            if (!destroyed && fxAvailable && fxWebView != null) {
                Platform.runLater(() -> {
                    updateMetricsOnFxThread();
                    takeSnapshotOnFxThread();
                });
            }
        }, 50);
    }

    private void captureSnapshotSync(int w, int h, long timeoutMs) {
        if (!fxAvailable || fxWebView == null || destroyed) return;
        if (Platform.isFxApplicationThread()) {
            fxWebView.setPrefSize(w, h);
            fxWebView.resize(w, h);
            fxWebView.applyCss();
            fxWebView.layout();
            updateMetricsOnFxThread();
            takeSnapshotOnFxThread();
            return;
        }
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                if (!destroyed && fxWebView != null) {
                    fxWebView.setPrefSize(w, h);
                    fxWebView.resize(w, h);
                    fxWebView.applyCss();
                    fxWebView.layout();
                    updateMetricsOnFxThread();
                    takeSnapshotOnFxThread();
                }
            } finally {
                done.countDown();
            }
        });
        try {
            done.await(Math.max(100L, timeoutMs), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void updateMetricsOnFxThread() {
        if (fxEngine == null) return;
        try {
            Object h = fxEngine.executeScript(
                    "Math.max(document.body?document.body.scrollHeight:0, document.documentElement?document.documentElement.scrollHeight:0)");
            if (h instanceof Number n) {
                contentHeightCss = Math.max(0, n.intValue());
            }
        } catch (Throwable ignored) {
        }
    }

    private void takeSnapshotOnFxThread() {
        if (destroyed || fxWebView == null) return;
        try {
            int w = Math.max(1, viewWidth);
            int h = Math.max(1, viewHeight);
            WritableImage wi = new WritableImage(w, h);
            fxWebView.snapshot(new SnapshotParameters(), wi);
            this.snapshotImage = SwingFXUtils.fromFXImage(wi, null);
            androidWebView.postInvalidate();
        } catch (Throwable t) {
            Log.w(TAG, "Lỗi snapshot WebView: " + t.getMessage());
        }
    }

    private void renderSwingFallbackImage(String html, int w, int h) {
        int iw = Math.max(1, w);
        int ih = Math.max(1, h);
        BufferedImage img = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            JEditorPane pane = new JEditorPane("text/html", html != null ? html : "");
            pane.setEditable(false);
            pane.setBackground(Color.WHITE);
            pane.setSize(iw, ih);
            pane.doLayout();
            pane.paint(g2);
        } finally {
            g2.dispose();
        }
        this.snapshotImage = img;
    }

    private void ensureAssetRootPrepared() {
        Context ctx = androidWebView.getContext();
        if (ctx == null) return;
        ApkFile apk = null;
        if (ctx.getApplicationContext() instanceof android.app.Application app && app.getBaseContext() instanceof AndroidRuntimeContext arc) {
            apk = arc.getApk();
        } else if (ctx instanceof AndroidRuntimeContext arc) {
            apk = arc.getApk();
        } else if (ctx instanceof android.content.ContextWrapper cw) {
            Context base = cw.getBaseContext();
            while (base instanceof android.content.ContextWrapper inner) {
                base = inner.getBaseContext();
            }
            if (base instanceof AndroidRuntimeContext arc) {
                apk = arc.getApk();
            }
        }
        if (apk == null) return;
        try {
            File cacheDir = ctx.getCacheDir();
            Path base = cacheDir != null
                    ? cacheDir.toPath().resolve("aaow-webview-assets")
                    : Files.createTempDirectory("aaow-webview-assets");
            List<String> assets = apk.listEntries("assets/");
            if (!assets.isEmpty()) {
                Files.createDirectories(base);
                for (String entry : assets) {
                    String rel = entry.substring("assets/".length());
                    if (rel.isEmpty() || rel.startsWith("dexopt/")) continue;
                    Path target = base.resolve(rel).normalize();
                    if (!target.startsWith(base)) continue;
                    if (!Files.exists(target)) {
                        Files.createDirectories(target.getParent());
                        byte[] bytes = apk.read(entry);
                        if (bytes != null) {
                            Files.write(target, bytes);
                        }
                    }
                }
            }
            this.extractedAssetRoot = base;
        } catch (Exception e) {
            Log.w(TAG, "Không giải nén được assets cho WebView: " + e.getMessage());
        }
    }

    private String mapAndroidUrl(String url) {
        if (url != null && url.startsWith("file:///android_asset/") && extractedAssetRoot != null) {
            String rel = url.substring("file:///android_asset/".length());
            Path resolved = extractedAssetRoot.resolve(rel).normalize();
            return resolved.toUri().toString();
        }
        return url;
    }

    private String unmapAssetUrl(String url) {
        if (url != null && extractedAssetRoot != null) {
            String rootUri = extractedAssetRoot.toUri().toString();
            if (url.startsWith(rootUri)) {
                return "file:///android_asset/" + url.substring(rootUri.length());
            }
        }
        return url;
    }

    private String rewriteHtmlWithBaseAndAssets(String html, String baseUrl) {
        if (html == null) return "";
        String out = html;
        if (extractedAssetRoot != null) {
            String rootUri = extractedAssetRoot.toUri().toString();
            if (!rootUri.endsWith("/")) rootUri += "/";
            out = out.replace("href='file:///android_asset/", "href='" + rootUri)
                    .replace("href=\"file:///android_asset/", "href=\"" + rootUri)
                    .replace("src='file:///android_asset/", "src='" + rootUri)
                    .replace("src=\"file:///android_asset/", "src=\"" + rootUri)
                    .replace("url(file:///android_asset/", "url(" + rootUri);
        }
        String mappedBase = mapAndroidUrl(baseUrl);
        if (mappedBase != null && !mappedBase.isEmpty() && !"about:blank".equals(mappedBase)) {
            if (!out.toLowerCase().contains("<base ")) {
                int headEnd = out.toLowerCase().indexOf("<head>");
                if (headEnd >= 0) {
                    out = out.substring(0, headEnd + 6) + "<base href=\"" + escapeHtmlAttr(mappedBase) + "\">"
                            + out.substring(headEnd + 6);
                } else {
                    out = "<head><base href=\"" + escapeHtmlAttr(mappedBase) + "\"></head>" + out;
                }
            }
        }
        return out;
    }

    private static String decodeDataPayload(String data, String encoding) {
        if (data == null) return "";
        if ("base64".equalsIgnoreCase(encoding)) {
            try {
                return new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (!data.contains("<") && data.contains("%")) {
            try {
                return URLDecoder.decode(data, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }
        return data;
    }

    private static String tryReadLocalHtml(String url) {
        if (url != null && url.startsWith("file:")) {
            try {
                Path p = Path.of(java.net.URI.create(url));
                if (Files.isRegularFile(p)) {
                    return Files.readString(p, StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private void recordHistory(String url, String origUrl, String title) {
        while (history.size() > historyIndex + 1) {
            history.remove(history.size() - 1);
        }
        history.add(new HistoryEntry(url, origUrl, title));
        historyIndex = history.size() - 1;
    }

    private static WebResourceRequest simpleRequest(String url, boolean mainFrame) {
        return simpleRequest(url, mainFrame, Collections.emptyMap());
    }

    private static WebResourceRequest simpleRequest(String url, boolean mainFrame, Map<String, String> headers) {
        Uri parsed = Uri.parse(url != null ? url : "");
        Map<String, String> safeHeaders = headers != null ? new LinkedHashMap<>(headers) : Collections.emptyMap();
        return new WebResourceRequest() {
            @Override public Uri getUrl() { return parsed; }
            @Override public boolean isForMainFrame() { return mainFrame; }
            @Override public boolean hasGesture() { return false; }
            @Override public String getMethod() { return "GET"; }
            @Override public Map<String, String> getRequestHeaders() { return safeHeaders; }
        };
    }

    private static String toJsJsonString(Object res) {
        if (res == null) return "null";
        if (res instanceof Boolean || res instanceof Number) {
            return String.valueOf(res);
        }
        String s = String.valueOf(res);
        if ("undefined".equals(s)) return "null";
        return "\"" + jsEscape(s) + "\"";
    }

    private static String jsEscape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String escapeHtmlAttr(String s) {
        return escapeHtml(s).replace("\"", "&quot;");
    }

    private static KeyCode mapAwtKeyCode(int awtCode) {
        return switch (awtCode) {
            case java.awt.event.KeyEvent.VK_ENTER -> KeyCode.ENTER;
            case java.awt.event.KeyEvent.VK_BACK_SPACE -> KeyCode.BACK_SPACE;
            case java.awt.event.KeyEvent.VK_TAB -> KeyCode.TAB;
            case java.awt.event.KeyEvent.VK_UP -> KeyCode.UP;
            case java.awt.event.KeyEvent.VK_DOWN -> KeyCode.DOWN;
            case java.awt.event.KeyEvent.VK_LEFT -> KeyCode.LEFT;
            case java.awt.event.KeyEvent.VK_RIGHT -> KeyCode.RIGHT;
            case java.awt.event.KeyEvent.VK_HOME -> KeyCode.HOME;
            case java.awt.event.KeyEvent.VK_END -> KeyCode.END;
            case java.awt.event.KeyEvent.VK_PAGE_UP -> KeyCode.PAGE_UP;
            case java.awt.event.KeyEvent.VK_PAGE_DOWN -> KeyCode.PAGE_DOWN;
            case java.awt.event.KeyEvent.VK_DELETE -> KeyCode.DELETE;
            default -> KeyCode.UNDEFINED;
        };
    }
}
