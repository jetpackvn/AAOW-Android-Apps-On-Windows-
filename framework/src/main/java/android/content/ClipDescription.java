package android.content;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClipDescription implements Parcelable {
    public static final String MIMETYPE_TEXT_PLAIN = "text/plain";
    public static final String MIMETYPE_TEXT_HTML = "text/html";
    public static final String MIMETYPE_TEXT_URILIST = "text/uri-list";
    public static final String MIMETYPE_TEXT_INTENT = "text/vnd.android.intent";
    public static final String MIMETYPE_UNKNOWN = "application/octet-stream";

    private final CharSequence label;
    private final String[] mimeTypes;
    private PersistableBundle extras;
    private final long timeStamp = System.currentTimeMillis();

    public ClipDescription(CharSequence label, String[] mimeTypes) {
        this.label = label;
        this.mimeTypes = mimeTypes != null ? mimeTypes.clone() : new String[0];
    }

    public ClipDescription(ClipDescription o) {
        this.label = o != null ? o.label : null;
        this.mimeTypes = o != null && o.mimeTypes != null ? o.mimeTypes.clone() : new String[0];
        this.extras = o != null ? o.extras : null;
    }

    public static boolean compareMimeTypes(String concreteType, String desiredType) {
        if (concreteType == null || desiredType == null) return false;
        if ("*/*".equals(desiredType) || concreteType.equalsIgnoreCase(desiredType)) return true;
        int slashDesired = desiredType.indexOf('/');
        int slashConcrete = concreteType.indexOf('/');
        if (slashDesired > 0 && slashConcrete > 0 && desiredType.endsWith("/*")) {
            return concreteType.substring(0, slashConcrete)
                    .equalsIgnoreCase(desiredType.substring(0, slashDesired));
        }
        return false;
    }

    public CharSequence getLabel() {
        return label;
    }

    public boolean hasMimeType(String mimeType) {
        for (String mt : mimeTypes) {
            if (compareMimeTypes(mt, mimeType)) return true;
        }
        return false;
    }

    public String[] filterMimeTypes(String mimeType) {
        List<String> matched = new ArrayList<>();
        for (String mt : mimeTypes) {
            if (compareMimeTypes(mt, mimeType)) matched.add(mt);
        }
        return matched.isEmpty() ? null : matched.toArray(new String[0]);
    }

    public int getMimeTypeCount() {
        return mimeTypes.length;
    }

    public String getMimeType(int index) {
        return mimeTypes[index];
    }

    public long getTimestamp() {
        return timeStamp;
    }

    public PersistableBundle getExtras() {
        return extras;
    }

    public void setExtras(PersistableBundle extras) {
        this.extras = extras;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(label != null ? label.toString() : null);
        dest.writeStringArray(mimeTypes);
    }

    public static final Creator<ClipDescription> CREATOR = new Creator<>() {
        @Override
        public ClipDescription createFromParcel(Parcel in) {
            String lbl = in.readString();
            String[] mts = in.createStringArray();
            return new ClipDescription(lbl, mts);
        }

        @Override
        public ClipDescription[] newArray(int size) {
            return new ClipDescription[size];
        }
    };

    @Override
    public String toString() {
        return "ClipDescription{" + label + ", " + Arrays.toString(mimeTypes) + "}";
    }
}
