package android.webkit;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

public class WebResourceResponse {
    private String mimeType;
    private String encoding;
    private int statusCode;
    private String reasonPhrase;
    private Map<String, String> responseHeaders;
    private InputStream data;

    public WebResourceResponse(String mimeType, String encoding, InputStream data) {
        this(mimeType, encoding, 200, "OK", Collections.emptyMap(), data);
    }

    public WebResourceResponse(String mimeType, String encoding, int statusCode, String reasonPhrase,
                               Map<String, String> responseHeaders, InputStream data) {
        this.mimeType = mimeType;
        this.encoding = encoding;
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
        this.responseHeaders = responseHeaders != null ? responseHeaders : Collections.emptyMap();
        this.data = data;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public void setStatusCodeAndReasonPhrase(int statusCode, String reasonPhrase) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> headers) {
        this.responseHeaders = headers != null ? headers : Collections.emptyMap();
    }

    public InputStream getData() {
        return data;
    }

    public void setData(InputStream data) {
        this.data = data;
    }
}
