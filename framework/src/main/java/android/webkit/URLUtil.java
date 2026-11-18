package android.webkit;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class URLUtil {
    private static final String ASSET_BASE = "file:///android_asset/";
    private static final String RESOURCE_BASE = "file:///android_res/";
    private static final String FILE_BASE = "file:";
    private static final String PROXY_BASE = "file:///cookieless_proxy/";
    private static final String CONTENT_BASE = "content:";

    private static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern.compile(
            "attachment;\\s*filename\\s*=\\s*(\"?)([^\"]*)\\1\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    public URLUtil() {
    }

    public static String guessUrl(String inUrl) {
        if (inUrl == null || inUrl.isEmpty()) return inUrl;
        if (inUrl.startsWith("about:") || inUrl.startsWith("data:") || inUrl.startsWith("file:")
                || inUrl.startsWith("javascript:") || inUrl.startsWith("content:")) {
            return inUrl;
        }
        String lower = inUrl.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://") && !lower.startsWith("ftp://")) {
            return "http://" + inUrl;
        }
        return inUrl;
    }

    public static String composeSearchUrl(String inQuery, String template, String queryPlaceHolder) {
        if (template == null || queryPlaceHolder == null) return inQuery;
        String encoded = java.net.URLEncoder.encode(inQuery != null ? inQuery : "", StandardCharsets.UTF_8);
        return template.replace(queryPlaceHolder, encoded);
    }

    public static byte[] decode(byte[] url) throws IllegalArgumentException {
        if (url == null) return null;
        if (url.length == 0) return new byte[0];
        String s = new String(url, StandardCharsets.US_ASCII);
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static boolean isAssetUrl(String url) {
        return url != null && url.startsWith(ASSET_BASE);
    }

    public static boolean isResourceUrl(String url) {
        return url != null && url.startsWith(RESOURCE_BASE);
    }

    public static boolean isCookielessProxyUrl(String url) {
        return url != null && url.startsWith(PROXY_BASE);
    }

    public static boolean isFileUrl(String url) {
        return url != null && url.startsWith(FILE_BASE) && !url.startsWith(ASSET_BASE) && !url.startsWith(PROXY_BASE);
    }

    public static boolean isAboutUrl(String url) {
        return url != null && url.equals("about:blank");
    }

    public static boolean isDataUrl(String url) {
        return url != null && url.startsWith("data:");
    }

    public static boolean isJavaScriptUrl(String url) {
        return url != null && url.startsWith("javascript:");
    }

    public static boolean isHttpUrl(String url) {
        return url != null && url.length() > 6 && url.substring(0, 7).equalsIgnoreCase("http://");
    }

    public static boolean isHttpsUrl(String url) {
        return url != null && url.length() > 7 && url.substring(0, 8).equalsIgnoreCase("https://");
    }

    public static boolean isNetworkUrl(String url) {
        return isHttpUrl(url) || isHttpsUrl(url);
    }

    public static boolean isContentUrl(String url) {
        return url != null && url.startsWith(CONTENT_BASE);
    }

    public static boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        return isAssetUrl(url) || isResourceUrl(url) || isFileUrl(url) || isAboutUrl(url)
                || isHttpUrl(url) || isHttpsUrl(url) || isJavaScriptUrl(url) || isContentUrl(url)
                || isDataUrl(url);
    }

    public static String stripAnchor(String url) {
        if (url == null) return null;
        int idx = url.indexOf('#');
        return idx >= 0 ? url.substring(0, idx) : url;
    }

    public static String guessFileName(String url, String contentDisposition, String mimeType) {
        String filename = null;
        if (contentDisposition != null) {
            Matcher m = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                filename = m.group(2);
            }
        }
        if (filename == null && url != null) {
            String decoded = stripAnchor(url);
            int q = decoded.indexOf('?');
            if (q > 0) decoded = decoded.substring(0, q);
            int slash = decoded.lastIndexOf('/');
            if (slash >= 0 && slash < decoded.length() - 1) {
                filename = decoded.substring(slash + 1);
            }
        }
        if (filename == null || filename.isEmpty()) {
            filename = "downloadfile";
        }
        if (!filename.contains(".")) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (ext != null && !ext.isEmpty()) {
                filename += "." + ext;
            } else {
                filename += ".bin";
            }
        }
        return filename;
    }
}
