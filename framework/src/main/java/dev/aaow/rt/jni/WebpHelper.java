package dev.aaow.rt.jni;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;

/**
 * Tiện ích giải mã ảnh dùng chung giữa {@link BitmapFactory} và {@link WebpJniHandler}.
 * Hỗ trợ đọc kích thước thực từ header RIFF WebP (VP8 / VP8L / VP8X) và giải mã ảnh qua Java ImageIO.
 */
public final class WebpHelper {
    public record ImageHeader(int width, int height, boolean hasAlpha, String mimeType) {
    }

    private WebpHelper() {
    }

    public static boolean isWebp(byte[] data, int offset, int length) {
        if (data == null || offset < 0 || length < 12 || offset + length > data.length) return false;
        return data[offset] == 'R' && data[offset + 1] == 'I' && data[offset + 2] == 'F' && data[offset + 3] == 'F'
                && data[offset + 8] == 'W' && data[offset + 9] == 'E' && data[offset + 10] == 'B' && data[offset + 11] == 'P';
    }

    public static ImageHeader parseWebpHeader(byte[] data, int offset, int length) {
        if (!isWebp(data, offset, length) || length < 30) return null;
        int p = offset + 12;
        int end = offset + length;
        while (p + 8 <= end) {
            String fourCc = new String(data, p, 4, java.nio.charset.StandardCharsets.US_ASCII);
            int chunkSize = ByteBuffer.wrap(data, p + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int payload = p + 8;
            if (payload + 10 <= end && "VP8X".equals(fourCc)) {
                boolean hasAlpha = (data[payload] & 0x10) != 0;
                int w = 1 + ((data[payload + 4] & 0xFF) | ((data[payload + 5] & 0xFF) << 8) | ((data[payload + 6] & 0xFF) << 16));
                int h = 1 + ((data[payload + 7] & 0xFF) | ((data[payload + 8] & 0xFF) << 8) | ((data[payload + 9] & 0xFF) << 16));
                return new ImageHeader(w, h, hasAlpha, "image/webp");
            }
            if (payload + 10 <= end && "VP8 ".equals(fourCc)) {
                if ((data[payload + 3] & 0xFF) == 0x9D
                        && (data[payload + 4] & 0xFF) == 0x01
                        && (data[payload + 5] & 0xFF) == 0x2A) {
                    int w = ((data[payload + 6] & 0xFF) | ((data[payload + 7] & 0xFF) << 8)) & 0x3FFF;
                    int h = ((data[payload + 8] & 0xFF) | ((data[payload + 9] & 0xFF) << 8)) & 0x3FFF;
                    return new ImageHeader(w, h, false, "image/webp");
                }
            }
            if (payload + 5 <= end && "VP8L".equals(fourCc) && (data[payload] & 0xFF) == 0x2F) {
                int bits = ByteBuffer.wrap(data, payload + 1, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
                int w = (bits & 0x3FFF) + 1;
                int h = ((bits >>> 14) & 0x3FFF) + 1;
                boolean hasAlpha = ((bits >>> 28) & 1) != 0;
                return new ImageHeader(w, h, hasAlpha, "image/webp");
            }
            int padded = chunkSize + (chunkSize & 1);
            if (padded < 0) break;
            p = payload + padded;
        }
        return null;
    }

    public static Bitmap decodeWithOptions(byte[] data, int offset, int length, BitmapFactory.Options opts) {
        if (data == null || offset < 0 || length <= 0 || offset + length > data.length) {
            return null;
        }
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(data, offset, length))) {
            if (iis != null) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(iis, true, true);
                        int srcW = reader.getWidth(0);
                        int srcH = reader.getHeight(0);
                        int sample = opts != null ? Math.max(1, opts.inSampleSize) : 1;
                        if (opts != null) {
                            opts.outWidth = srcW;
                            opts.outHeight = srcH;
                            String fmt = reader.getFormatName();
                            if (fmt != null && !fmt.isEmpty()) {
                                opts.outMimeType = "image/" + fmt.toLowerCase(java.util.Locale.ROOT);
                            }
                            if (opts.inJustDecodeBounds) return null;
                        }
                        ImageReadParam param = reader.getDefaultReadParam();
                        if (sample > 1) {
                            param.setSourceSubsampling(sample, sample, 0, 0);
                        }
                        BufferedImage img = reader.read(0, param);
                        if (img != null) {
                            if (opts != null) {
                                opts.outWidth = img.getWidth();
                                opts.outHeight = img.getHeight();
                            }
                            return new Bitmap(img);
                        }
                    } finally {
                        reader.dispose();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        ImageHeader webp = parseWebpHeader(data, offset, length);
        if (webp != null && webp.width() > 0 && webp.height() > 0) {
            int sample = opts != null ? Math.max(1, opts.inSampleSize) : 1;
            int outW = Math.max(1, webp.width() / sample);
            int outH = Math.max(1, webp.height() / sample);
            if (opts != null) {
                opts.outWidth = outW;
                opts.outHeight = outH;
                opts.outMimeType = webp.mimeType();
                if (opts.inJustDecodeBounds) return null;
            }
            int w = Math.min(2048, outW);
            int h = Math.min(2048, outH);
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bi.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, w, h);
            g.dispose();
            return new Bitmap(bi);
        }
        return null;
    }
}
