package dev.aaow.apk;

/**
 * Tương đương struct Res_value của Android: một giá trị đã được aapt2 biên dịch
 * (chuỗi, số nguyên, màu, kích thước, tham chiếu tới resource khác...).
 */
public record ResValue(int type, int data, String string) {
    public static final int TYPE_NULL = 0x00;
    public static final int TYPE_REFERENCE = 0x01;
    public static final int TYPE_ATTRIBUTE = 0x02;
    public static final int TYPE_STRING = 0x03;
    public static final int TYPE_FLOAT = 0x04;
    public static final int TYPE_DIMENSION = 0x05;
    public static final int TYPE_FRACTION = 0x06;
    public static final int TYPE_DYNAMIC_REFERENCE = 0x07;
    public static final int TYPE_INT_DEC = 0x10;
    public static final int TYPE_INT_HEX = 0x11;
    public static final int TYPE_INT_BOOLEAN = 0x12;
    public static final int TYPE_INT_COLOR_ARGB8 = 0x1c;
    public static final int TYPE_INT_COLOR_RGB8 = 0x1d;
    public static final int TYPE_INT_COLOR_ARGB4 = 0x1e;
    public static final int TYPE_INT_COLOR_RGB4 = 0x1f;

    public static final int COMPLEX_UNIT_PX = 0;
    public static final int COMPLEX_UNIT_DIP = 1;
    public static final int COMPLEX_UNIT_SP = 2;
    public static final int COMPLEX_UNIT_PT = 3;
    public static final int COMPLEX_UNIT_IN = 4;
    public static final int COMPLEX_UNIT_MM = 5;

    private static final float MANTISSA_MULT = 1.0f / (1 << 8);
    private static final float[] RADIX_MULTS = {
            1.0f * MANTISSA_MULT, 1.0f / (1 << 7) * MANTISSA_MULT,
            1.0f / (1 << 15) * MANTISSA_MULT, 1.0f / (1 << 23) * MANTISSA_MULT
    };

    public boolean isReference() {
        return type == TYPE_REFERENCE || type == TYPE_DYNAMIC_REFERENCE;
    }

    public boolean isString() {
        return type == TYPE_STRING;
    }

    public boolean isColor() {
        return type >= TYPE_INT_COLOR_ARGB8 && type <= TYPE_INT_COLOR_RGB4;
    }

    public boolean isInt() {
        return type >= TYPE_INT_DEC && type <= TYPE_INT_COLOR_RGB4;
    }

    public boolean isDimension() {
        return type == TYPE_DIMENSION;
    }

    public float getFloat() {
        return Float.intBitsToFloat(data);
    }

    /** Giá trị số của TYPE_DIMENSION (chưa đổi đơn vị). */
    public float complexValue() {
        return (data & 0xffffff00) * RADIX_MULTS[(data >> 4) & 3];
    }

    public int complexUnit() {
        return data & 0xf;
    }

    /** Biểu diễn dạng chuỗi giống cách Android hiển thị giá trị thô. */
    public String coerceToString() {
        switch (type) {
            case TYPE_STRING:
                return string;
            case TYPE_NULL:
                return null;
            case TYPE_REFERENCE:
            case TYPE_DYNAMIC_REFERENCE:
                return String.format("@0x%08x", data);
            case TYPE_ATTRIBUTE:
                return String.format("?0x%08x", data);
            case TYPE_FLOAT:
                return Float.toString(getFloat());
            case TYPE_DIMENSION: {
                String[] units = {"px", "dip", "sp", "pt", "in", "mm"};
                int u = complexUnit();
                return complexValue() + (u < units.length ? units[u] : "");
            }
            case TYPE_INT_BOOLEAN:
                return data != 0 ? "true" : "false";
            case TYPE_INT_HEX:
                return String.format("0x%08x", data);
            default:
                if (isColor()) return String.format("#%08x", data);
                return Integer.toString(data);
        }
    }

    @Override
    public String toString() {
        return "ResValue{type=0x" + Integer.toHexString(type) + ", " + coerceToString() + "}";
    }
}
