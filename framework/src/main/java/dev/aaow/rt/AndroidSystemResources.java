package dev.aaow.rt;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Bảng tra cứu thuộc tính hệ thống Android (0x0101xxxx <-> tên thuộc tính)
 * và các hằng số resource hệ thống (android.R.*).
 */
public final class AndroidSystemResources {
    private static final Map<Integer, String> ATTR_ID_TO_NAME = new HashMap<>();
    private static final Map<String, Integer> ATTR_NAME_TO_ID = new HashMap<>();

    static {
        try (InputStream in = AndroidSystemResources.class.getResourceAsStream("/dev/aaow/rt/android_attrs.txt")) {
            if (in != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    int sp = line.indexOf(' ');
                    if (sp > 0) {
                        int id = (int) Long.parseUnsignedLong(line.substring(0, sp), 16);
                        String name = line.substring(sp + 1).trim();
                        ATTR_ID_TO_NAME.put(id, name);
                        ATTR_NAME_TO_ID.put(name, id);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private AndroidSystemResources() {
    }

    public static String attrName(int resId) {
        return ATTR_ID_TO_NAME.get(resId);
    }

    public static int attrId(String name) {
        return ATTR_NAME_TO_ID.getOrDefault(name, 0);
    }

    /** Giải quyết màu hệ thống android.R.color.* phổ biến. */
    public static Integer systemColor(int resId) {
        return switch (resId) {
            case 0x0106000b -> 0xFFFFFFFF; // white
            case 0x0106000c -> 0xFF000000; // black
            case 0x0106000d -> 0x00000000; // transparent
            case 0x01060000 -> 0xFFAAAAAA; // darker_gray
            case 0x01060012 -> 0xFF33B5E5; // holo_blue_light
            case 0x01060013 -> 0xFF0099CC; // holo_blue_dark
            case 0x01060014 -> 0xFF99CC00; // holo_green_light
            case 0x01060016 -> 0xFFFF4444; // holo_red_light
            case 0x01060018 -> 0xFFFFBB33; // holo_orange_light
            default -> null;
        };
    }

    /** Giải quyết chuỗi hệ thống android.R.string.* phổ biến. */
    public static String systemString(int resId) {
        return switch (resId) {
            case 0x0104000a -> "OK";
            case 0x01040000 -> "Cancel";
            case 0x01040013 -> "Yes";
            case 0x01040009 -> "No";
            case 0x01040001 -> "Copy";
            case 0x0104000b -> "Paste";
            default -> null;
        };
    }
}
