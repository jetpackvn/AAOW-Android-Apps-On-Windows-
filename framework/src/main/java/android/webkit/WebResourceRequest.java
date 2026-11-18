package android.webkit;

import android.net.Uri;

import java.util.Map;

public interface WebResourceRequest {
    Uri getUrl();

    boolean isForMainFrame();

    default boolean isRedirect() {
        return false;
    }

    boolean hasGesture();

    String getMethod();

    Map<String, String> getRequestHeaders();
}
