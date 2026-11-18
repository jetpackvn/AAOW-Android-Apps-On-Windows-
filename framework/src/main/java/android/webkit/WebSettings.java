package android.webkit;

public class WebSettings {
    private boolean javaScriptEnabled;
    private boolean builtInZoomControls;
    private boolean displayZoomControls = true;
    private boolean domStorageEnabled;
    private boolean supportZoom = true;
    private boolean useWideViewPort;
    private boolean loadWithOverviewMode;
    private int defaultFontSize = 16;

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

    public void setSupportZoom(boolean support) {
        this.supportZoom = support;
    }

    public boolean supportZoom() {
        return supportZoom;
    }

    public void setUseWideViewPort(boolean use) {
        this.useWideViewPort = use;
    }

    public void setLoadWithOverviewMode(boolean overview) {
        this.loadWithOverviewMode = overview;
    }

    public void setDefaultFontSize(int size) {
        this.defaultFontSize = size;
    }

    public int getDefaultFontSize() {
        return defaultFontSize;
    }

    public void setAllowFileAccess(boolean allow) {
    }
}
