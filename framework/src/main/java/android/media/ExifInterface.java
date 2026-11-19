package android.media;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cài đặt thật android.media.ExifInterface đọc, sửa, xóa và lưu lại metadata EXIF (JPEG APP1 / TIFF IFD)
 * trực tiếp trên JVM Windows.
 */
public class ExifInterface {
    public static final int STREAM_TYPE_FULL_IMAGE_DATA = 0;
    public static final int STREAM_TYPE_EXIF_DATA_ONLY = 1;

    public static final int ORIENTATION_UNDEFINED = 0;
    public static final int ORIENTATION_NORMAL = 1;
    public static final int ORIENTATION_FLIP_HORIZONTAL = 2;
    public static final int ORIENTATION_ROTATE_180 = 3;
    public static final int ORIENTATION_FLIP_VERTICAL = 4;
    public static final int ORIENTATION_TRANSPOSE = 5;
    public static final int ORIENTATION_ROTATE_90 = 6;
    public static final int ORIENTATION_TRANSVERSE = 7;
    public static final int ORIENTATION_ROTATE_270 = 8;

    public static final String TAG_IMAGE_WIDTH = "ImageWidth";
    public static final String TAG_IMAGE_LENGTH = "ImageLength";
    public static final String TAG_MAKE = "Make";
    public static final String TAG_MODEL = "Model";
    public static final String TAG_ORIENTATION = "Orientation";
    public static final String TAG_SOFTWARE = "Software";
    public static final String TAG_DATETIME = "DateTime";
    public static final String TAG_ARTIST = "Artist";
    public static final String TAG_COPYRIGHT = "Copyright";
    public static final String TAG_IMAGE_DESCRIPTION = "ImageDescription";
    public static final String TAG_DATETIME_ORIGINAL = "DateTimeOriginal";
    public static final String TAG_DATETIME_DIGITIZED = "DateTimeDigitized";
    public static final String TAG_EXPOSURE_TIME = "ExposureTime";
    public static final String TAG_F_NUMBER = "FNumber";
    public static final String TAG_APERTURE_VALUE = "ApertureValue";
    public static final String TAG_ISO_SPEED_RATINGS = "ISOSpeedRatings";
    public static final String TAG_PHOTOGRAPHIC_SENSITIVITY = "PhotographicSensitivity";
    public static final String TAG_FOCAL_LENGTH = "FocalLength";
    public static final String TAG_FLASH = "Flash";
    public static final String TAG_WHITE_BALANCE = "WhiteBalance";
    public static final String TAG_USER_COMMENT = "UserComment";
    public static final String TAG_PIXEL_X_DIMENSION = "PixelXDimension";
    public static final String TAG_PIXEL_Y_DIMENSION = "PixelYDimension";
    public static final String TAG_GPS_LATITUDE_REF = "GPSLatitudeRef";
    public static final String TAG_GPS_LATITUDE = "GPSLatitude";
    public static final String TAG_GPS_LONGITUDE_REF = "GPSLongitudeRef";
    public static final String TAG_GPS_LONGITUDE = "GPSLongitude";
    public static final String TAG_GPS_ALTITUDE_REF = "GPSAltitudeRef";
    public static final String TAG_GPS_ALTITUDE = "GPSAltitude";
    public static final String TAG_GPS_TIMESTAMP = "GPSTimeStamp";
    public static final String TAG_GPS_DATESTAMP = "GPSDateStamp";
    public static final String TAG_GPS_PROCESSING_METHOD = "GPSProcessingMethod";

    private static final int TAG_EXIF_IFD_POINTER = 0x8769;
    private static final int TAG_GPS_IFD_POINTER = 0x8825;

    private static final int TYPE_BYTE = 1;
    private static final int TYPE_ASCII = 2;
    private static final int TYPE_SHORT = 3;
    private static final int TYPE_LONG = 4;
    private static final int TYPE_RATIONAL = 5;
    private static final int TYPE_SBYTE = 6;
    private static final int TYPE_UNDEFINED = 7;
    private static final int TYPE_SSHORT = 8;
    private static final int TYPE_SLONG = 9;
    private static final int TYPE_SRATIONAL = 10;

    private static final int IFD_PRIMARY = 0;
    private static final int IFD_EXIF = 1;
    private static final int IFD_GPS = 2;

    private record TagDef(int id, String name, int format, int ifd) {
    }

    private static final List<TagDef> TAG_DEFS = List.of(
            new TagDef(0x0100, TAG_IMAGE_WIDTH, TYPE_LONG, IFD_PRIMARY),
            new TagDef(0x0101, TAG_IMAGE_LENGTH, TYPE_LONG, IFD_PRIMARY),
            new TagDef(0x010E, TAG_IMAGE_DESCRIPTION, TYPE_ASCII, IFD_PRIMARY),
            new TagDef(0x010F, TAG_MAKE, TYPE_ASCII, IFD_PRIMARY),
            new TagDef(0x0110, TAG_MODEL, TYPE_ASCII, IFD_PRIMARY),
            new TagDef(0x0112, TAG_ORIENTATION, TYPE_SHORT, IFD_PRIMARY),
            new TagDef(0x0131, TAG_SOFTWARE, TYPE_ASCII, IFD_PRIMARY),
            new TagDef(0x0132, TAG_DATETIME, TYPE_ASCII, IFD_PRIMARY),
            new TagDef(0x013B, TAG_ARTIST, TYPE_ASCII, IFD_PRIMARY),
            new TagDef(0x8298, TAG_COPYRIGHT, TYPE_ASCII, IFD_PRIMARY),

            new TagDef(0x829A, TAG_EXPOSURE_TIME, TYPE_RATIONAL, IFD_EXIF),
            new TagDef(0x829D, TAG_F_NUMBER, TYPE_RATIONAL, IFD_EXIF),
            new TagDef(0x8827, TAG_ISO_SPEED_RATINGS, TYPE_SHORT, IFD_EXIF),
            new TagDef(0x9003, TAG_DATETIME_ORIGINAL, TYPE_ASCII, IFD_EXIF),
            new TagDef(0x9004, TAG_DATETIME_DIGITIZED, TYPE_ASCII, IFD_EXIF),
            new TagDef(0x9202, TAG_APERTURE_VALUE, TYPE_RATIONAL, IFD_EXIF),
            new TagDef(0x9209, TAG_FLASH, TYPE_SHORT, IFD_EXIF),
            new TagDef(0x920A, TAG_FOCAL_LENGTH, TYPE_RATIONAL, IFD_EXIF),
            new TagDef(0x9286, TAG_USER_COMMENT, TYPE_ASCII, IFD_EXIF),
            new TagDef(0xA002, TAG_PIXEL_X_DIMENSION, TYPE_LONG, IFD_EXIF),
            new TagDef(0xA003, TAG_PIXEL_Y_DIMENSION, TYPE_LONG, IFD_EXIF),
            new TagDef(0xA403, TAG_WHITE_BALANCE, TYPE_SHORT, IFD_EXIF),

            new TagDef(0x0001, TAG_GPS_LATITUDE_REF, TYPE_ASCII, IFD_GPS),
            new TagDef(0x0002, TAG_GPS_LATITUDE, TYPE_RATIONAL, IFD_GPS),
            new TagDef(0x0003, TAG_GPS_LONGITUDE_REF, TYPE_ASCII, IFD_GPS),
            new TagDef(0x0004, TAG_GPS_LONGITUDE, TYPE_RATIONAL, IFD_GPS),
            new TagDef(0x0005, TAG_GPS_ALTITUDE_REF, TYPE_BYTE, IFD_GPS),
            new TagDef(0x0006, TAG_GPS_ALTITUDE, TYPE_RATIONAL, IFD_GPS),
            new TagDef(0x0007, TAG_GPS_TIMESTAMP, TYPE_RATIONAL, IFD_GPS),
            new TagDef(0x001B, TAG_GPS_PROCESSING_METHOD, TYPE_ASCII, IFD_GPS),
            new TagDef(0x001D, TAG_GPS_DATESTAMP, TYPE_ASCII, IFD_GPS)
    );

    private static final Map<String, TagDef> NAME_TO_DEF = new LinkedHashMap<>();
    private static final Map<Long, TagDef> IFD_ID_TO_DEF = new LinkedHashMap<>();

    static {
        for (TagDef td : TAG_DEFS) {
            NAME_TO_DEF.put(td.name, td);
            IFD_ID_TO_DEF.put((((long) td.ifd) << 32) | (td.id & 0xFFFFL), td);
        }
        NAME_TO_DEF.put(TAG_PHOTOGRAPHIC_SENSITIVITY, NAME_TO_DEF.get(TAG_ISO_SPEED_RATINGS));
    }

    private final File file;
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private byte[] cachedBytes;

    public ExifInterface(String filename) throws IOException {
        if (filename == null) throw new NullPointerException("filename cannot be null");
        this.file = new File(filename);
        loadFromFile(this.file);
    }

    public ExifInterface(File file) throws IOException {
        if (file == null) throw new NullPointerException("file cannot be null");
        this.file = file;
        loadFromFile(this.file);
    }

    public ExifInterface(FileDescriptor fileDescriptor) throws IOException {
        if (fileDescriptor == null) throw new NullPointerException("fileDescriptor cannot be null");
        this.file = null;
        try (FileInputStream fis = new FileInputStream(fileDescriptor)) {
            loadFromBytes(fis.readAllBytes());
        }
    }

    public ExifInterface(InputStream inputStream) throws IOException {
        this(inputStream, STREAM_TYPE_FULL_IMAGE_DATA);
    }

    public ExifInterface(InputStream inputStream, int streamType) throws IOException {
        if (inputStream == null) throw new NullPointerException("inputStream cannot be null");
        this.file = null;
        byte[] data = inputStream.readAllBytes();
        if (streamType == STREAM_TYPE_EXIF_DATA_ONLY) {
            parseTiffHeader(data, 0, data.length);
        } else {
            loadFromBytes(data);
        }
    }

    private void loadFromFile(File f) throws IOException {
        if (!f.exists() || !f.isFile()) {
            throw new IOException("File does not exist: " + f);
        }
        try (FileInputStream fis = new FileInputStream(f)) {
            loadFromBytes(fis.readAllBytes());
        }
    }

    private void loadFromBytes(byte[] data) {
        this.cachedBytes = data;
        if (data == null || data.length < 4) return;
        // JPEG SOI: FF D8
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) {
            parseJpegExif(data);
        }
    }

    private void parseJpegExif(byte[] data) {
        int pos = 2;
        int n = data.length;
        while (pos + 4 <= n) {
            if ((data[pos] & 0xFF) != 0xFF) break;
            while (pos < n && (data[pos] & 0xFF) == 0xFF) pos++;
            if (pos >= n) break;
            int marker = data[pos++] & 0xFF;
            if (marker == 0xD9 || marker == 0xDA) break; // EOI or SOS
            if (marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) continue;
            if (pos + 2 > n) break;
            int segLen = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
            if (segLen < 2 || pos + segLen > n) break;
            int bodyStart = pos + 2;
            int bodyLen = segLen - 2;
            if (marker == 0xE1 && bodyLen > 6
                    && data[bodyStart] == 'E' && data[bodyStart + 1] == 'x'
                    && data[bodyStart + 2] == 'i' && data[bodyStart + 3] == 'f'
                    && data[bodyStart + 4] == 0 && data[bodyStart + 5] == 0) {
                parseTiffHeader(data, bodyStart + 6, bodyLen - 6);
                return;
            }
            pos += segLen;
        }
    }

    private void parseTiffHeader(byte[] data, int tiffStart, int tiffLen) {
        if (tiffLen < 8 || tiffStart + tiffLen > data.length) return;
        boolean littleEndian;
        if (data[tiffStart] == 'I' && data[tiffStart + 1] == 'I') {
            littleEndian = true;
        } else if (data[tiffStart] == 'M' && data[tiffStart + 1] == 'M') {
            littleEndian = false;
        } else {
            return;
        }
        int magic = readU16(data, tiffStart + 2, littleEndian);
        if (magic != 42) return;
        int ifd0Offset = readU32(data, tiffStart + 4, littleEndian);
        parseIfd(data, tiffStart, tiffLen, ifd0Offset, IFD_PRIMARY, littleEndian);
    }

    private void parseIfd(byte[] data, int tiffStart, int tiffLen, int relOffset, int ifdType, boolean le) {
        if (relOffset < 0 || relOffset + 2 > tiffLen) return;
        int pos = tiffStart + relOffset;
        int count = readU16(data, pos, le);
        pos += 2;
        for (int i = 0; i < count; i++) {
            int entryPos = pos + i * 12;
            if (entryPos + 12 > tiffStart + tiffLen) break;
            int tagId = readU16(data, entryPos, le);
            int format = readU16(data, entryPos + 2, le);
            int numComponents = readU32(data, entryPos + 4, le);
            int unitSize = formatByteSize(format);
            if (unitSize == 0 || numComponents < 0) continue;
            long byteLen = (long) numComponents * unitSize;
            int valPos;
            if (byteLen <= 4) {
                valPos = entryPos + 8;
            } else {
                int dataOffset = readU32(data, entryPos + 8, le);
                if (dataOffset < 0 || dataOffset + byteLen > tiffLen) continue;
                valPos = tiffStart + dataOffset;
            }

            if (ifdType == IFD_PRIMARY && tagId == TAG_EXIF_IFD_POINTER) {
                int subOff = readU32(data, valPos, le);
                parseIfd(data, tiffStart, tiffLen, subOff, IFD_EXIF, le);
                continue;
            }
            if (ifdType == IFD_PRIMARY && tagId == TAG_GPS_IFD_POINTER) {
                int subOff = readU32(data, valPos, le);
                parseIfd(data, tiffStart, tiffLen, subOff, IFD_GPS, le);
                continue;
            }

            TagDef def = IFD_ID_TO_DEF.get((((long) ifdType) << 32) | (tagId & 0xFFFFL));
            if (def == null) continue;
            String val = decodeValue(data, valPos, format, numComponents, le);
            if (val != null) {
                attributes.put(def.name, val);
            }
        }
    }

    private static String decodeValue(byte[] data, int pos, int format, int count, boolean le) {
        if (count <= 0) return "";
        switch (format) {
            case TYPE_ASCII -> {
                int end = pos;
                int limit = Math.min(data.length, pos + count);
                while (end < limit && data[end] != 0) end++;
                return new String(data, pos, end - pos, StandardCharsets.UTF_8).trim();
            }
            case TYPE_UNDEFINED -> {
                int limit = Math.min(data.length, pos + count);
                if (limit - pos >= 8 && data[pos] == 'A' && data[pos + 1] == 'S' && data[pos + 2] == 'C') {
                    return new String(data, pos + 8, limit - (pos + 8), StandardCharsets.UTF_8).replace("\0", "").trim();
                }
                return new String(data, pos, limit - pos, StandardCharsets.UTF_8).replace("\0", "").trim();
            }
            case TYPE_BYTE, TYPE_SBYTE -> {
                if (count == 1) return Integer.toString(data[pos] & 0xFF);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < count && pos + i < data.length; i++) {
                    if (i > 0) sb.append(',');
                    sb.append(data[pos + i] & 0xFF);
                }
                return sb.toString();
            }
            case TYPE_SHORT, TYPE_SSHORT -> {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < count && pos + i * 2 + 2 <= data.length; i++) {
                    if (i > 0) sb.append(',');
                    int v = readU16(data, pos + i * 2, le);
                    sb.append(format == TYPE_SSHORT ? (short) v : v);
                }
                return sb.toString();
            }
            case TYPE_LONG, TYPE_SLONG -> {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < count && pos + i * 4 + 4 <= data.length; i++) {
                    if (i > 0) sb.append(',');
                    int v = readU32(data, pos + i * 4, le);
                    sb.append(format == TYPE_SLONG ? v : (v & 0xFFFFFFFFL));
                }
                return sb.toString();
            }
            case TYPE_RATIONAL, TYPE_SRATIONAL -> {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < count && pos + i * 8 + 8 <= data.length; i++) {
                    if (i > 0) sb.append(',');
                    int num = readU32(data, pos + i * 8, le);
                    int den = readU32(data, pos + i * 8 + 4, le);
                    if (format == TYPE_SRATIONAL) {
                        sb.append(num).append('/').append(den);
                    } else {
                        sb.append(num & 0xFFFFFFFFL).append('/').append(den & 0xFFFFFFFFL);
                    }
                }
                return sb.toString();
            }
            default -> {
                return null;
            }
        }
    }

    private static int formatByteSize(int format) {
        return switch (format) {
            case TYPE_BYTE, TYPE_ASCII, TYPE_SBYTE, TYPE_UNDEFINED -> 1;
            case TYPE_SHORT, TYPE_SSHORT -> 2;
            case TYPE_LONG, TYPE_SLONG -> 4;
            case TYPE_RATIONAL, TYPE_SRATIONAL -> 8;
            default -> 0;
        };
    }

    private static int readU16(byte[] b, int off, boolean le) {
        if (le) return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
        return ((b[off] & 0xFF) << 8) | (b[off + 1] & 0xFF);
    }

    private static int readU32(byte[] b, int off, boolean le) {
        if (le) {
            return (b[off] & 0xFF)
                    | ((b[off + 1] & 0xFF) << 8)
                    | ((b[off + 2] & 0xFF) << 16)
                    | ((b[off + 3] & 0xFF) << 24);
        }
        return ((b[off] & 0xFF) << 24)
                | ((b[off + 1] & 0xFF) << 16)
                | ((b[off + 2] & 0xFF) << 8)
                | (b[off + 3] & 0xFF);
    }

    // --------------------------------------------------------- Public API

    public String getAttribute(String tag) {
        if (tag == null) return null;
        if (TAG_PHOTOGRAPHIC_SENSITIVITY.equals(tag) && !attributes.containsKey(tag)) {
            return attributes.get(TAG_ISO_SPEED_RATINGS);
        }
        return attributes.get(tag);
    }

    public boolean hasAttribute(String tag) {
        return getAttribute(tag) != null;
    }

    public int getAttributeInt(String tag, int defaultValue) {
        String val = getAttribute(tag);
        if (val == null || val.isEmpty()) return defaultValue;
        try {
            int comma = val.indexOf(',');
            String first = comma > 0 ? val.substring(0, comma) : val;
            return Integer.parseInt(first.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getAttributeDouble(String tag, double defaultValue) {
        String val = getAttribute(tag);
        if (val == null || val.isEmpty()) return defaultValue;
        try {
            int slash = val.indexOf('/');
            if (slash > 0) {
                double num = Double.parseDouble(val.substring(0, slash).trim());
                double den = Double.parseDouble(val.substring(slash + 1).trim());
                return den != 0.0 ? num / den : defaultValue;
            }
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void setAttribute(String tag, String value) {
        if (tag == null) return;
        if (value == null) {
            attributes.remove(tag);
        } else {
            attributes.put(tag, value);
        }
    }

    public void clearAllAttributes() {
        attributes.clear();
    }

    public boolean getLatLong(float[] output) {
        if (output == null || output.length < 2) return false;
        String latStr = getAttribute(TAG_GPS_LATITUDE);
        String latRef = getAttribute(TAG_GPS_LATITUDE_REF);
        String lonStr = getAttribute(TAG_GPS_LONGITUDE);
        String lonRef = getAttribute(TAG_GPS_LONGITUDE_REF);
        if (latStr == null || latRef == null || lonStr == null || lonRef == null) return false;
        try {
            float lat = convertRationalLatLonToFloat(latStr, latRef);
            float lon = convertRationalLatLonToFloat(lonStr, lonRef);
            output[0] = lat;
            output[1] = lon;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static float convertRationalLatLonToFloat(String rationalString, String ref) {
        String[] parts = rationalString.split(",");
        double deg = parseRational(parts[0]);
        double min = parts.length > 1 ? parseRational(parts[1]) : 0.0;
        double sec = parts.length > 2 ? parseRational(parts[2]) : 0.0;
        double res = deg + (min / 60.0) + (sec / 3600.0);
        if ("S".equalsIgnoreCase(ref.trim()) || "W".equalsIgnoreCase(ref.trim())) {
            res = -res;
        }
        return (float) res;
    }

    private static double parseRational(String part) {
        String s = part.trim();
        int slash = s.indexOf('/');
        if (slash > 0) {
            double n = Double.parseDouble(s.substring(0, slash).trim());
            double d = Double.parseDouble(s.substring(slash + 1).trim());
            return d != 0.0 ? n / d : 0.0;
        }
        return Double.parseDouble(s);
    }

    public double getAltitude(double defaultValue) {
        double alt = getAttributeDouble(TAG_GPS_ALTITUDE, -1.0);
        int ref = getAttributeInt(TAG_GPS_ALTITUDE_REF, -1);
        if (alt >= 0.0 && ref >= 0) {
            return ref == 1 ? -alt : alt;
        }
        return defaultValue;
    }

    public int getRotationDegrees() {
        return switch (getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL)) {
            case ORIENTATION_ROTATE_90, ORIENTATION_TRANSPOSE -> 90;
            case ORIENTATION_ROTATE_180, ORIENTATION_FLIP_VERTICAL -> 180;
            case ORIENTATION_ROTATE_270, ORIENTATION_TRANSVERSE -> 270;
            default -> 0;
        };
    }

    public boolean isFlipped() {
        return switch (getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL)) {
            case ORIENTATION_FLIP_HORIZONTAL, ORIENTATION_FLIP_VERTICAL,
                 ORIENTATION_TRANSPOSE, ORIENTATION_TRANSVERSE -> true;
            default -> false;
        };
    }

    public void resetOrientation() {
        setAttribute(TAG_ORIENTATION, Integer.toString(ORIENTATION_NORMAL));
    }

    public void rotate(int degree) {
        if (degree % 90 != 0) throw new IllegalArgumentException("degree should be a multiple of 90");
        int cur = getRotationDegrees();
        int next = ((cur + degree) % 360 + 360) % 360;
        int ori = switch (next) {
            case 90 -> ORIENTATION_ROTATE_90;
            case 180 -> ORIENTATION_ROTATE_180;
            case 270 -> ORIENTATION_ROTATE_270;
            default -> ORIENTATION_NORMAL;
        };
        setAttribute(TAG_ORIENTATION, Integer.toString(ori));
    }

    public boolean hasThumbnail() {
        return false;
    }

    public byte[] getThumbnail() {
        return null;
    }

    public byte[] getThumbnailBytes() {
        return null;
    }

    /** Ghi lại các thuộc tính EXIF vào tệp JPEG trên đĩa (hoặc xóa sạch APP1 EXIF nếu không còn thuộc tính). */
    public void saveAttributes() throws IOException {
        if (file == null) {
            throw new IOException("Cannot save ExifInterface opened from InputStream");
        }
        byte[] src = cachedBytes != null ? cachedBytes : new FileInputStream(file).readAllBytes();
        if (src.length < 4 || (src[0] & 0xFF) != 0xFF || (src[1] & 0xFF) != 0xD8) {
            throw new IOException("ExifInterface only supports saving JPEG files");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(src.length + 512);
        out.write(0xFF);
        out.write(0xD8);

        byte[] app1Payload = buildExifApp1Payload();
        if (app1Payload != null && app1Payload.length > 0) {
            int segLen = app1Payload.length + 2;
            out.write(0xFF);
            out.write(0xE1);
            out.write((segLen >> 8) & 0xFF);
            out.write(segLen & 0xFF);
            out.write(app1Payload);
        }

        int pos = 2;
        int n = src.length;
        while (pos < n) {
            if ((src[pos] & 0xFF) != 0xFF) {
                out.write(src, pos, n - pos);
                break;
            }
            int markerStart = pos;
            while (pos < n && (src[pos] & 0xFF) == 0xFF) pos++;
            if (pos >= n) break;
            int marker = src[pos++] & 0xFF;
            if (marker == 0xDA || marker == 0xD9) {
                out.write(src, markerStart, n - markerStart);
                break;
            }
            if (marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) {
                out.write(src, markerStart, pos - markerStart);
                continue;
            }
            if (pos + 2 > n) {
                out.write(src, markerStart, n - markerStart);
                break;
            }
            int segLen = ((src[pos] & 0xFF) << 8) | (src[pos + 1] & 0xFF);
            int nextPos = pos + segLen;
            if (segLen < 2 || nextPos > n) {
                out.write(src, markerStart, n - markerStart);
                break;
            }
            boolean isExifApp1 = marker == 0xE1 && segLen >= 8
                    && src[pos + 2] == 'E' && src[pos + 3] == 'x'
                    && src[pos + 4] == 'i' && src[pos + 5] == 'f'
                    && src[pos + 6] == 0 && src[pos + 7] == 0;
            if (!isExifApp1) {
                out.write(src, markerStart, nextPos - markerStart);
            }
            pos = nextPos;
        }

        byte[] updated = out.toByteArray();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(updated);
        }
        this.cachedBytes = updated;
    }

    private record EncodedTag(int id, int format, int count, byte[] raw) {
    }

    private byte[] buildExifApp1Payload() throws IOException {
        List<EncodedTag> primaryTags = new ArrayList<>();
        List<EncodedTag> exifTags = new ArrayList<>();
        List<EncodedTag> gpsTags = new ArrayList<>();

        for (Map.Entry<String, String> e : attributes.entrySet()) {
            TagDef def = NAME_TO_DEF.get(e.getKey());
            if (def == null || e.getValue() == null) continue;
            EncodedTag enc = encodeTag(def, e.getValue());
            if (enc == null) continue;
            switch (def.ifd) {
                case IFD_PRIMARY -> primaryTags.add(enc);
                case IFD_EXIF -> exifTags.add(enc);
                case IFD_GPS -> gpsTags.add(enc);
            }
        }
        if (primaryTags.isEmpty() && exifTags.isEmpty() && gpsTags.isEmpty()) {
            return null;
        }

        int primaryCount = primaryTags.size() + (exifTags.isEmpty() ? 0 : 1) + (gpsTags.isEmpty() ? 0 : 1);
        int primaryIfdSize = 2 + primaryCount * 12 + 4;
        int primaryExtraStart = 8 + primaryIfdSize;
        int primaryExtraSize = extraBytesSize(primaryTags);

        int exifIfdOffset = primaryExtraStart + primaryExtraSize;
        int exifIfdSize = exifTags.isEmpty() ? 0 : (2 + exifTags.size() * 12 + 4);
        int exifExtraSize = extraBytesSize(exifTags);

        int gpsIfdOffset = exifIfdOffset + exifIfdSize + exifExtraSize;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        // Exif\0\0 header
        dos.writeBytes("Exif");
        dos.writeShort(0);
        // TIFF Big-Endian header ("MM", 42, offset=8)
        dos.writeBytes("MM");
        dos.writeShort(42);
        dos.writeInt(8);

        List<EncodedTag> primaryAll = new ArrayList<>(primaryTags);
        if (!exifTags.isEmpty()) {
            primaryAll.add(new EncodedTag(TAG_EXIF_IFD_POINTER, TYPE_LONG, 1, intToBe4(exifIfdOffset)));
        }
        if (!gpsTags.isEmpty()) {
            primaryAll.add(new EncodedTag(TAG_GPS_IFD_POINTER, TYPE_LONG, 1, intToBe4(gpsIfdOffset)));
        }
        writeIfd(dos, primaryAll, primaryExtraStart);
        if (!exifTags.isEmpty()) {
            writeIfd(dos, exifTags, exifIfdOffset + exifIfdSize);
        }
        if (!gpsTags.isEmpty()) {
            int gpsIfdSize = 2 + gpsTags.size() * 12 + 4;
            writeIfd(dos, gpsTags, gpsIfdOffset + gpsIfdSize);
        }
        dos.flush();
        return baos.toByteArray();
    }

    private static int extraBytesSize(List<EncodedTag> tags) {
        int sum = 0;
        for (EncodedTag t : tags) {
            if (t.raw.length > 4) sum += t.raw.length;
        }
        return sum;
    }

    private static void writeIfd(DataOutputStream dos, List<EncodedTag> tags, int extraStartOffset) throws IOException {
        dos.writeShort(tags.size());
        int curExtra = extraStartOffset;
        for (EncodedTag t : tags) {
            dos.writeShort(t.id);
            dos.writeShort(t.format);
            dos.writeInt(t.count);
            if (t.raw.length <= 4) {
                dos.write(t.raw);
                for (int pad = t.raw.length; pad < 4; pad++) dos.writeByte(0);
            } else {
                dos.writeInt(curExtra);
                curExtra += t.raw.length;
            }
        }
        dos.writeInt(0); // next IFD offset = 0
        for (EncodedTag t : tags) {
            if (t.raw.length > 4) {
                dos.write(t.raw);
            }
        }
    }

    private static EncodedTag encodeTag(TagDef def, String value) {
        try {
            switch (def.format) {
                case TYPE_ASCII -> {
                    byte[] str = value.getBytes(StandardCharsets.UTF_8);
                    byte[] raw = Arrays.copyOf(str, str.length + 1);
                    return new EncodedTag(def.id, TYPE_ASCII, raw.length, raw);
                }
                case TYPE_BYTE -> {
                    int v = Integer.parseInt(value.trim());
                    return new EncodedTag(def.id, TYPE_BYTE, 1, new byte[] { (byte) (v & 0xFF) });
                }
                case TYPE_SHORT -> {
                    int v = Integer.parseInt(value.trim());
                    return new EncodedTag(def.id, TYPE_SHORT, 1, new byte[] { (byte) ((v >> 8) & 0xFF), (byte) (v & 0xFF) });
                }
                case TYPE_LONG -> {
                    long v = Long.parseLong(value.trim());
                    return new EncodedTag(def.id, TYPE_LONG, 1, intToBe4((int) v));
                }
                case TYPE_RATIONAL -> {
                    String[] parts = value.split(",");
                    ByteArrayOutputStream bs = new ByteArrayOutputStream();
                    DataOutputStream d = new DataOutputStream(bs);
                    for (String p : parts) {
                        String s = p.trim();
                        int slash = s.indexOf('/');
                        int num, den;
                        if (slash > 0) {
                            num = (int) Long.parseLong(s.substring(0, slash).trim());
                            den = (int) Long.parseLong(s.substring(slash + 1).trim());
                        } else {
                            double dv = Double.parseDouble(s);
                            num = (int) Math.round(dv * 10000.0);
                            den = 10000;
                        }
                        d.writeInt(num);
                        d.writeInt(den);
                    }
                    return new EncodedTag(def.id, TYPE_RATIONAL, parts.length, bs.toByteArray());
                }
                default -> {
                    return null;
                }
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] intToBe4(int v) {
        return new byte[] {
                (byte) ((v >>> 24) & 0xFF),
                (byte) ((v >>> 16) & 0xFF),
                (byte) ((v >>> 8) & 0xFF),
                (byte) (v & 0xFF)
        };
    }
}
