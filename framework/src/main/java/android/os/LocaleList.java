package android.os;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class LocaleList implements Parcelable {
    private static final LocaleList EMPTY = new LocaleList();
    private static LocaleList defaultList = new LocaleList(Locale.getDefault());

    private final Locale[] list;

    public LocaleList(Locale... list) {
        if (list == null || list.length == 0) {
            this.list = new Locale[0];
        } else {
            List<Locale> clean = new ArrayList<>();
            for (Locale l : list) {
                if (l != null && !clean.contains(l)) {
                    clean.add(l);
                }
            }
            this.list = clean.toArray(new Locale[0]);
        }
    }

    public static LocaleList getEmptyLocaleList() {
        return EMPTY;
    }

    public static LocaleList getDefault() {
        return defaultList;
    }

    public static void setDefault(LocaleList locales) {
        if (locales != null && !locales.isEmpty()) {
            defaultList = locales;
            Locale.setDefault(locales.get(0));
        }
    }

    public static LocaleList forLanguageTags(String list) {
        if (list == null || list.isBlank()) return EMPTY;
        String[] parts = list.split(",");
        Locale[] locales = new Locale[parts.length];
        for (int i = 0; i < parts.length; i++) {
            locales[i] = Locale.forLanguageTag(parts[i].trim());
        }
        return new LocaleList(locales);
    }

    public Locale get(int index) {
        return (index >= 0 && index < list.length) ? list[index] : null;
    }

    public boolean isEmpty() {
        return list.length == 0;
    }

    public int size() {
        return list.length;
    }

    public int indexOf(Locale locale) {
        for (int i = 0; i < list.length; i++) {
            if (Objects.equals(list[i], locale)) return i;
        }
        return -1;
    }

    public String toLanguageTags() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(list[i].toLanguageTag());
        }
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(toLanguageTags());
    }

    public static final Creator<LocaleList> CREATOR = new Creator<>() {
        @Override
        public LocaleList createFromParcel(Parcel source) {
            return forLanguageTags(source.readString());
        }

        @Override
        public LocaleList[] newArray(int size) {
            return new LocaleList[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        return o instanceof LocaleList other && Arrays.equals(list, other.list);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(list);
    }

    @Override
    public String toString() {
        return Arrays.toString(list);
    }
}
