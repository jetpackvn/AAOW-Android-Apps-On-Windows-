package android.print;

import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;

public final class PrintAttributes implements Parcelable {
    public static final int COLOR_MODE_MONOCHROME = 1;
    public static final int COLOR_MODE_COLOR = 2;
    public static final int DUPLEX_MODE_NONE = 1;
    public static final int DUPLEX_MODE_LONG_EDGE = 2;
    public static final int DUPLEX_MODE_SHORT_EDGE = 4;

    private MediaSize mediaSize = MediaSize.ISO_A4;
    private Resolution resolution = new Resolution("default", "300dpi", 300, 300);
    private Margins minMargins = Margins.NO_MARGINS;
    private int colorMode = COLOR_MODE_COLOR;
    private int duplexMode = DUPLEX_MODE_NONE;

    PrintAttributes() {
    }

    public MediaSize getMediaSize() {
        return mediaSize;
    }

    public Resolution getResolution() {
        return resolution;
    }

    public Margins getMinMargins() {
        return minMargins;
    }

    public int getColorMode() {
        return colorMode;
    }

    public int getDuplexMode() {
        return duplexMode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(colorMode);
        dest.writeInt(duplexMode);
    }

    public static final Creator<PrintAttributes> CREATOR = new Creator<>() {
        @Override
        public PrintAttributes createFromParcel(Parcel in) {
            PrintAttributes pa = new PrintAttributes();
            pa.colorMode = in.readInt();
            pa.duplexMode = in.readInt();
            return pa;
        }

        @Override
        public PrintAttributes[] newArray(int size) {
            return new PrintAttributes[size];
        }
    };

    public static final class Builder {
        private final PrintAttributes attrs = new PrintAttributes();

        public Builder setMediaSize(MediaSize mediaSize) {
            attrs.mediaSize = mediaSize;
            return this;
        }

        public Builder setResolution(Resolution resolution) {
            attrs.resolution = resolution;
            return this;
        }

        public Builder setMinMargins(Margins margins) {
            attrs.minMargins = margins;
            return this;
        }

        public Builder setColorMode(int colorMode) {
            attrs.colorMode = colorMode;
            return this;
        }

        public Builder setDuplexMode(int duplexMode) {
            attrs.duplexMode = duplexMode;
            return this;
        }

        public PrintAttributes build() {
            return attrs;
        }
    }

    public static final class MediaSize {
        public static final MediaSize ISO_A4 = new MediaSize("ISO_A4", "ISO A4", 8270, 11690);
        public static final MediaSize ISO_A5 = new MediaSize("ISO_A5", "ISO A5", 5830, 8270);
        public static final MediaSize NA_LETTER = new MediaSize("NA_LETTER", "US Letter", 8500, 11000);
        public static final MediaSize UNKNOWN_PORTRAIT = new MediaSize("UNKNOWN_PORTRAIT", "Portrait", 8500, 11000);
        public static final MediaSize UNKNOWN_LANDSCAPE = new MediaSize("UNKNOWN_LANDSCAPE", "Landscape", 11000, 8500);

        private final String id;
        private final String label;
        private final int widthMils;
        private final int heightMils;

        public MediaSize(String id, String label, int widthMils, int heightMils) {
            this.id = id;
            this.label = label;
            this.widthMils = widthMils;
            this.heightMils = heightMils;
        }

        public String getId() {
            return id;
        }

        public String getLabel(PackageManager pm) {
            return label;
        }

        public int getWidthMils() {
            return widthMils;
        }

        public int getHeightMils() {
            return heightMils;
        }

        public boolean isPortrait() {
            return heightMils >= widthMils;
        }

        public MediaSize asPortrait() {
            return isPortrait() ? this : new MediaSize(id, label, heightMils, widthMils);
        }

        public MediaSize asLandscape() {
            return !isPortrait() ? this : new MediaSize(id, label, heightMils, widthMils);
        }
    }

    public static final class Margins {
        public static final Margins NO_MARGINS = new Margins(0, 0, 0, 0);

        private final int leftMils;
        private final int topMils;
        private final int rightMils;
        private final int bottomMils;

        public Margins(int leftMils, int topMils, int rightMils, int bottomMils) {
            this.leftMils = leftMils;
            this.topMils = topMils;
            this.rightMils = rightMils;
            this.bottomMils = bottomMils;
        }

        public int getLeftMils() { return leftMils; }
        public int getTopMils() { return topMils; }
        public int getRightMils() { return rightMils; }
        public int getBottomMils() { return bottomMils; }
    }

    public static final class Resolution {
        private final String id;
        private final String label;
        private final int horizontalDpi;
        private final int verticalDpi;

        public Resolution(String id, String label, int horizontalDpi, int verticalDpi) {
            this.id = id;
            this.label = label;
            this.horizontalDpi = horizontalDpi;
            this.verticalDpi = verticalDpi;
        }

        public String getId() { return id; }
        public String getLabel() { return label; }
        public int getHorizontalDpi() { return horizontalDpi; }
        public int getVerticalDpi() { return verticalDpi; }
    }
}
