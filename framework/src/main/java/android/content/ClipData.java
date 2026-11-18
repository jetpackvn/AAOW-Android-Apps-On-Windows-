package android.content;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;

import java.util.ArrayList;
import java.util.List;

public class ClipData implements Parcelable {
    public static class Item {
        private final CharSequence text;
        private final String htmlText;
        private final Intent intent;
        private final Uri uri;

        public Item(CharSequence text) {
            this(text, null, null, null);
        }

        public Item(CharSequence text, String htmlText) {
            this(text, htmlText, null, null);
        }

        public Item(Intent intent) {
            this(null, null, intent, null);
        }

        public Item(Uri uri) {
            this(null, null, null, uri);
        }

        public Item(CharSequence text, Intent intent, Uri uri) {
            this(text, null, intent, uri);
        }

        public Item(CharSequence text, String htmlText, Intent intent, Uri uri) {
            this.text = text;
            this.htmlText = htmlText;
            this.intent = intent;
            this.uri = uri;
        }

        public CharSequence getText() { return text; }
        public String getHtmlText() { return htmlText; }
        public Intent getIntent() { return intent; }
        public Uri getUri() { return uri; }

        public CharSequence coerceToText(Context context) {
            if (text != null) return text;
            if (uri != null) return uri.toString();
            if (intent != null) return intent.toUri();
            return "";
        }

        public CharSequence coerceToStyledText(Context context) {
            if (htmlText != null) return Html.fromHtml(htmlText);
            return coerceToText(context);
        }

        public String coerceToHtmlText(Context context) {
            if (htmlText != null) return htmlText;
            CharSequence t = coerceToText(context);
            return t != null ? Html.escapeHtml(t) : "";
        }
    }

    private final ClipDescription clipDescription;
    private final List<Item> items = new ArrayList<>();

    public ClipData(CharSequence label, String[] mimeTypes, Item item) {
        this.clipDescription = new ClipDescription(label, mimeTypes);
        if (item != null) this.items.add(item);
    }

    public ClipData(ClipDescription description, Item item) {
        this.clipDescription = description != null ? description : new ClipDescription(null, new String[0]);
        if (item != null) this.items.add(item);
    }

    public ClipData(ClipData other) {
        this.clipDescription = other != null ? new ClipDescription(other.clipDescription) : new ClipDescription(null, new String[0]);
        if (other != null) this.items.addAll(other.items);
    }

    public static ClipData newPlainText(CharSequence label, CharSequence text) {
        return new ClipData(label, new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, new Item(text));
    }

    public static ClipData newHtmlText(CharSequence label, CharSequence text, String htmlText) {
        return new ClipData(label,
                new String[]{ClipDescription.MIMETYPE_TEXT_HTML, ClipDescription.MIMETYPE_TEXT_PLAIN},
                new Item(text, htmlText));
    }

    public static ClipData newIntent(CharSequence label, Intent intent) {
        return new ClipData(label, new String[]{ClipDescription.MIMETYPE_TEXT_INTENT}, new Item(intent));
    }

    public static ClipData newUri(ContentResolver resolver, CharSequence label, Uri uri) {
        return newRawUri(label, uri);
    }

    public static ClipData newRawUri(CharSequence label, Uri uri) {
        return new ClipData(label, new String[]{ClipDescription.MIMETYPE_TEXT_URILIST}, new Item(uri));
    }

    public ClipDescription getDescription() {
        return clipDescription;
    }

    public void addItem(Item item) {
        if (item != null) items.add(item);
    }

    public void addItem(ContentResolver resolver, Item item) {
        addItem(item);
    }

    public int getItemCount() {
        return items.size();
    }

    public Item getItemAt(int index) {
        return items.get(index);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(clipDescription, flags);
        dest.writeInt(items.size());
        for (Item item : items) {
            dest.writeString(item.text != null ? item.text.toString() : null);
            dest.writeString(item.htmlText);
            dest.writeParcelable(item.intent, flags);
            dest.writeParcelable(item.uri, flags);
        }
    }

    public static final Creator<ClipData> CREATOR = new Creator<>() {
        @Override
        public ClipData createFromParcel(Parcel in) {
            ClipDescription desc = in.readParcelable(null);
            int count = in.readInt();
            ClipData cd = null;
            for (int i = 0; i < count; i++) {
                String txt = in.readString();
                String html = in.readString();
                Intent it = in.readParcelable(null);
                Uri u = in.readParcelable(null);
                Item item = new Item(txt, html, it, u);
                if (cd == null) cd = new ClipData(desc, item);
                else cd.addItem(item);
            }
            return cd != null ? cd : new ClipData(desc, new Item(""));
        }

        @Override
        public ClipData[] newArray(int size) {
            return new ClipData[size];
        }
    };
}
