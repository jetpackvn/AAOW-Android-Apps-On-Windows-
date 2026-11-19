package android.graphics;

/** Không gian màu tương thích android.graphics.ColorSpace dùng trong BitmapFactory và ImageDecoder. */
public abstract class ColorSpace {
    public static final int MIN_ID = -1;
    public static final int MAX_ID = 63;

    public enum Named {
        SRGB,
        LINEAR_SRGB,
        EXTENDED_SRGB,
        LINEAR_EXTENDED_SRGB,
        BT709,
        BT2020,
        DCI_P3,
        DISPLAY_P3,
        NTSC_1953,
        SMPTE_C,
        ADOBE_RGB,
        PRO_PHOTO_RGB,
        ACES,
        ACESCG,
        CIE_XYZ,
        CIE_LAB,
        BT2020_HLG,
        BT2020_PQ,
        OK_LAB,
        DISPLAY_BT2020
    }

    public enum Model {
        RGB(3),
        XYZ(3),
        LAB(3),
        CMYK(4);

        private final int componentCount;

        Model(int componentCount) {
            this.componentCount = componentCount;
        }

        public int getComponentCount() {
            return componentCount;
        }
    }

    private final String name;
    private final Model model;
    private final int id;

    ColorSpace(String name, Model model, int id) {
        this.name = name;
        this.model = model;
        this.id = id;
    }

    private static final ColorSpace SRGB_INSTANCE = new SimpleRgbColorSpace("sRGB IEC61966-2.1", 0, false);
    private static final ColorSpace DISPLAY_P3_INSTANCE = new SimpleRgbColorSpace("Display P3", Named.DISPLAY_P3.ordinal(), true);

    public static ColorSpace get(Named name) {
        if (name == Named.DISPLAY_P3 || name == Named.DCI_P3 || name == Named.BT2020) {
            return DISPLAY_P3_INSTANCE;
        }
        return SRGB_INSTANCE;
    }

    public static ColorSpace getFromDataSpace(int dataSpace) {
        return SRGB_INSTANCE;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public Model getModel() {
        return model;
    }

    public int getComponentCount() {
        return model.getComponentCount();
    }

    public boolean isSrgb() {
        return id == 0;
    }

    public abstract boolean isWideGamut();

    public abstract float getMinValue(int component);

    public abstract float getMaxValue(int component);

    public float[] toXyz(float r, float g, float b) {
        return toXyz(new float[]{r, g, b});
    }

    public abstract float[] toXyz(float[] v);

    public float[] fromXyz(float x, float y, float z) {
        return fromXyz(new float[]{x, y, z});
    }

    public abstract float[] fromXyz(float[] v);

    private static final class SimpleRgbColorSpace extends ColorSpace {
        private final boolean wideGamut;

        SimpleRgbColorSpace(String name, int id, boolean wideGamut) {
            super(name, Model.RGB, id);
            this.wideGamut = wideGamut;
        }

        @Override
        public boolean isWideGamut() {
            return wideGamut;
        }

        @Override
        public float getMinValue(int component) {
            return 0.0f;
        }

        @Override
        public float getMaxValue(int component) {
            return 1.0f;
        }

        @Override
        public float[] toXyz(float[] v) {
            return v != null ? v.clone() : new float[]{0f, 0f, 0f};
        }

        @Override
        public float[] fromXyz(float[] v) {
            return v != null ? v.clone() : new float[]{0f, 0f, 0f};
        }
    }
}
