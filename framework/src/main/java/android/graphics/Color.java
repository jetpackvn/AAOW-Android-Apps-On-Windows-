package android.graphics;

public class Color {
    public static final int BLACK = 0xFF000000;
    public static final int DKGRAY = 0xFF444444;
    public static final int GRAY = 0xFF888888;
    public static final int LTGRAY = 0xFFCCCCCC;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int RED = 0xFFFF0000;
    public static final int GREEN = 0xFF00FF00;
    public static final int BLUE = 0xFF0000FF;
    public static final int YELLOW = 0xFFFFFF00;
    public static final int CYAN = 0xFF00FFFF;
    public static final int MAGENTA = 0xFFFF00FF;
    public static final int TRANSPARENT = 0;

    public static int alpha(int color) {
        return color >>> 24;
    }

    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    public static int rgb(int red, int green, int blue) {
        return 0xFF000000 | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    public static int argb(int alpha, int red, int green, int blue) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    public static int HSVToColor(float[] hsv) {
        return HSVToColor(0xFF, hsv);
    }

    public static int HSVToColor(int alpha, float[] hsv) {
        int rgb = java.awt.Color.HSBtoRGB(hsv[0] / 360f, hsv[1], hsv[2]);
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    public static void RGBToHSV(int red, int green, int blue, float[] hsv) {
        if (hsv == null || hsv.length < 3) {
            throw new RuntimeException("3 components required for hsv");
        }
        java.awt.Color.RGBtoHSB(red & 0xFF, green & 0xFF, blue & 0xFF, hsv);
        hsv[0] *= 360f;
    }

    public static void colorToHSV(int color, float[] hsv) {
        RGBToHSV(red(color), green(color), blue(color), hsv);
    }

    public static int parseColor(String colorString) {
        if (colorString == null || colorString.isEmpty()) {
            throw new IllegalArgumentException("Unknown color");
        }
        if (colorString.charAt(0) == '#') {
            String hex = colorString.substring(1);
            if (hex.length() == 3) {
                int r = Integer.parseInt(hex.substring(0, 1), 16);
                int g = Integer.parseInt(hex.substring(1, 2), 16);
                int b = Integer.parseInt(hex.substring(2, 3), 16);
                return rgb(r * 17, g * 17, b * 17);
            } else if (hex.length() == 4) {
                int a = Integer.parseInt(hex.substring(0, 1), 16);
                int r = Integer.parseInt(hex.substring(1, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 3), 16);
                int b = Integer.parseInt(hex.substring(3, 4), 16);
                return argb(a * 17, r * 17, g * 17, b * 17);
            } else if (hex.length() == 6) {
                return 0xFF000000 | Integer.parseUnsignedInt(hex, 16);
            } else if (hex.length() == 8) {
                return Integer.parseUnsignedInt(hex, 16);
            }
            throw new IllegalArgumentException("Unknown color: " + colorString);
        }
        return switch (colorString.toLowerCase()) {
            case "black" -> BLACK;
            case "darkgray", "darkgrey" -> DKGRAY;
            case "gray", "grey" -> GRAY;
            case "lightgray", "lightgrey" -> LTGRAY;
            case "white" -> WHITE;
            case "red" -> RED;
            case "green" -> GREEN;
            case "blue" -> BLUE;
            case "yellow" -> YELLOW;
            case "cyan", "aqua" -> CYAN;
            case "magenta", "fuchsia" -> MAGENTA;
            case "transparent" -> TRANSPARENT;
            default -> throw new IllegalArgumentException("Unknown color: " + colorString);
        };
    }

    public static java.awt.Color toAwt(int argb) {
        return new java.awt.Color(argb, true);
    }
}
