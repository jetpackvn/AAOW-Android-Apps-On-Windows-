package android.text;

import android.graphics.drawable.Drawable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Html {
    public static final int FROM_HTML_MODE_LEGACY = 0x00000000;
    public static final int FROM_HTML_MODE_COMPACT = 0x0000003f;

    public interface ImageGetter {
        Drawable getDrawable(String source);
    }

    public interface TagHandler {
        void handleTag(boolean opening, String tag, Editable output, Object xmlReader);
    }

    private static final Pattern NUM_ENTITY = Pattern.compile("&#(x?)([0-9a-fA-F]+);");

    private Html() {
    }

    public static Spanned fromHtml(String source) {
        return fromHtml(source, FROM_HTML_MODE_LEGACY, null, null);
    }

    public static Spanned fromHtml(String source, int flags) {
        return fromHtml(source, flags, null, null);
    }

    public static Spanned fromHtml(String source, ImageGetter imageGetter, TagHandler tagHandler) {
        return fromHtml(source, FROM_HTML_MODE_LEGACY, imageGetter, tagHandler);
    }

    public static Spanned fromHtml(String source, int flags, ImageGetter imageGetter, TagHandler tagHandler) {
        if (source == null) {
            return new SpannableStringBuilder("");
        }
        String text = source
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("<[^>]+>", "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ")
                .replace("&bull;", "•");

        Matcher m = NUM_ENTITY.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            int cp;
            try {
                cp = Integer.parseInt(m.group(2), m.group(1).isEmpty() ? 10 : 16);
                m.appendReplacement(out, Matcher.quoteReplacement(new String(Character.toChars(cp))));
            } catch (Exception e) {
                m.appendReplacement(out, Matcher.quoteReplacement(m.group(0)));
            }
        }
        m.appendTail(out);
        return new SpannableStringBuilder(out.toString());
    }

    public static String toHtml(Spanned text) {
        return toHtml(text, 0);
    }

    public static String toHtml(Spanned text, int option) {
        if (text == null) return "";
        return "<p dir=\"ltr\">" + escapeHtml(text).replace("\n", "<br>") + "</p>";
    }

    public static String escapeHtml(CharSequence text) {
        if (text == null) return "";
        return text.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
