package android.webkit;

public class WebSettings {
    public static final int LOAD_DEFAULT = -1;
    public static final int LOAD_NORMAL = 0;
    public static final int LOAD_CACHE_ELSE_NETWORK = 1;
    public static final int LOAD_NO_CACHE = 2;
    public static final int LOAD_CACHE_ONLY = 3;

    public static final int MIXED_CONTENT_ALWAYS_ALLOW = 0;
    public static final int MIXED_CONTENT_NEVER_ALLOW = 1;
    public static final int MIXED_CONTENT_COMPATIBILITY_MODE = 2;

    private boolean javaScriptEnabled = true;
    private boolean builtInZoomControls;
    private boolean displayZoomControls = true;
    private boolean domStorageEnabled;
    private boolean databaseEnabled;
    private boolean supportZoom = true;
    private boolean useWideViewPort;
    private boolean loadWithOverviewMode;
    private boolean allowFileAccess = true;
    private boolean allowContentAccess = true;
    private boolean allowFileAccessFromFileURLs;
    private boolean allowUniversalAccessFromFileURLs;
    private boolean javaScriptCanOpenWindowsAutomatically;
    private boolean supportMultipleWindows;
    private boolean mediaPlaybackRequiresUserGesture = true;
    private boolean blockNetworkImage;
    private boolean loadsImagesAutomatically = true;
    private int cacheMode = LOAD_DEFAULT;
    private int mixedContentMode = MIXED_CONTENT_NEVER_ALLOW;
    private int defaultFontSize = 16;
    private int textZoom = 100;
    private String userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AAOW-WebView/1.0";
    private String defaultTextEncodingName = "UTF-8";

    public void setJavaScriptEnabled(boolean flag) {
        this.javaScriptEnabled = flag;
    }

    public boolean getJavaScriptEnabled() {
        return javaScriptEnabled;
    }

    public void setBuiltInZoomControls(boolean enabled) {
        this.builtInZoomControls = enabled;
    }

    public boolean getBuiltInZoomControls() {
        return builtInZoomControls;
    }

    public void setDisplayZoomControls(boolean enabled) {
        this.displayZoomControls = enabled;
    }

    public boolean getDisplayZoomControls() {
        return displayZoomControls;
    }

    public void setDomStorageEnabled(boolean flag) {
        this.domStorageEnabled = flag;
    }

    public boolean getDomStorageEnabled() {
        return domStorageEnabled;
    }

    public void setDatabaseEnabled(boolean flag) {
        this.databaseEnabled = flag;
    }

    public boolean getDatabaseEnabled() {
        return databaseEnabled;
    }

    public void setSupportZoom(boolean support) {
        this.supportZoom = support;
    }

    public boolean supportZoom() {
        return supportZoom;
    }

    public void setUseWideViewPort(boolean use) {
        this.useWideViewPort = use;
    }

    public boolean getUseWideViewPort() {
        return useWideViewPort;
    }

    public void setLoadWithOverviewMode(boolean overview) {
        this.loadWithOverviewMode = overview;
    }

    public boolean getLoadWithOverviewMode() {
        return loadWithOverviewMode;
    }

    public void setDefaultFontSize(int size) {
        this.defaultFontSize = size;
    }

    public int getDefaultFontSize() {
        return defaultFontSize;
    }

    public void setTextZoom(int textZoom) {
        this.textZoom = Math.max(25, textZoom);
    }

    public int getTextZoom() {
        return textZoom;
    }

    public void setAllowFileAccess(boolean allow) {
        this.allowFileAccess = allow;
    }

    public boolean getAllowFileAccess() {
        return allowFileAccess;
    }

    public void setAllowContentAccess(boolean allow) {
        this.allowContentAccess = allow;
    }

    public boolean getAllowContentAccess() {
        return allowContentAccess;
    }

    public void setAllowFileAccessFromFileURLs(boolean flag) {
        this.allowFileAccessFromFileURLs = flag;
    }

    public boolean getAllowFileAccessFromFileURLs() {
        return allowFileAccessFromFileURLs;
    }

    public void setAllowUniversalAccessFromFileURLs(boolean flag) {
        this.allowUniversalAccessFromFileURLs = flag;
    }

    public boolean getAllowUniversalAccessFromFileURLs() {
        return allowUniversalAccessFromFileURLs;
    }

    public void setJavaScriptCanOpenWindowsAutomatically(boolean flag) {
        this.javaScriptCanOpenWindowsAutomatically = flag;
    }

    public boolean getJavaScriptCanOpenWindowsAutomatically() {
        return javaScriptCanOpenWindowsAutomatically;
    }

    public void setSupportMultipleWindows(boolean support) {
        this.supportMultipleWindows = support;
    }

    public boolean supportMultipleWindows() {
        return supportMultipleWindows;
    }

    public void setMediaPlaybackRequiresUserGesture(boolean require) {
        this.mediaPlaybackRequiresUserGesture = require;
    }

    public boolean getMediaPlaybackRequiresUserGesture() {
        return mediaPlaybackRequiresUserGesture;
    }

    public void setBlockNetworkImage(boolean flag) {
        this.blockNetworkImage = flag;
    }

    public boolean getBlockNetworkImage() {
        return blockNetworkImage;
    }

    public void setLoadsImagesAutomatically(boolean flag) {
        this.loadsImagesAutomatically = flag;
    }

    public boolean getLoadsImagesAutomatically() {
        return loadsImagesAutomatically;
    }

    public void setCacheMode(int mode) {
        this.cacheMode = mode;
    }

    public int getCacheMode() {
        return cacheMode;
    }

    public void setMixedContentMode(int mode) {
        this.mixedContentMode = mode;
    }

    public int getMixedContentMode() {
        return mixedContentMode;
    }

    public static String getDefaultUserAgent(android.content.Context context) {
        return "Mozilla/5.0 (Linux; Android 15; AAOW Windows Desktop) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36";
    }

    public void setUserAgentString(String ua) {
        if (ua != null && !ua.isEmpty()) {
            this.userAgentString = ua;
        }
    }

    public String getUserAgentString() {
        return userAgentString;
    }

    public void setDefaultTextEncodingName(String encoding) {
        if (encoding != null) this.defaultTextEncodingName = encoding;
    }

    public String getDefaultTextEncodingName() {
        return defaultTextEncodingName;
    }
}
