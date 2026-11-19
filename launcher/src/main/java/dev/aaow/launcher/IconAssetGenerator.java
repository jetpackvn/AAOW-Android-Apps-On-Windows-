package dev.aaow.launcher;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tạo biểu tượng đa độ phân giải chuẩn Windows (.ico) cho AAOW.exe và file .apk.
 * Sử dụng BMP 32-bit BGRA cho 16/32/48/64px (tương thích 100% csc.exe/jpackage/Explorer)
 * và PNG cho 256px (chuẩn Vista/Win10/Win11).
 */
public final class IconAssetGenerator {
    private static final int[] ICON_SIZES = {16, 32, 48, 64, 256};

    private IconAssetGenerator() {
    }

    public static void main(String[] args) throws Exception {
        Path outDir = args.length > 0 ? Path.of(args[0]) : Path.of("packaging");
        Files.createDirectories(outDir);
        writeAppIcon(outDir.resolve("aaow.ico"));
        writeApkFileIcon(outDir.resolve("apk-file.ico"));
        System.out.println("Generated AAOW Windows icons in: " + outDir.toAbsolutePath());
    }

    public static void writeAppIcon(Path outFile) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        for (int sz : ICON_SIZES) {
            images.add(renderAaowAppLogo(sz));
        }
        writeIco(outFile, images);
    }

    public static void writeApkFileIcon(Path outFile) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        for (int sz : ICON_SIZES) {
            images.add(renderApkDocumentLogo(sz));
        }
        writeIco(outFile, images);
    }

    public static BufferedImage renderAaowAppLogo(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        float pad = size * 0.06f;
        float box = size - pad * 2f;
        float arc = size * 0.24f;
        g.setPaint(new GradientPaint(0, 0, new Color(0x4F5BD5), size, size, new Color(0x1E293B)));
        g.fill(new RoundRectangle2D.Float(pad, pad, box, box, arc, arc));

        // 4 ô biểu trưng cho Windows ở nền nhẹ phía trên
        g.setColor(new Color(255, 255, 255, 42));
        float wx = size * 0.56f;
        float wy = size * 0.17f;
        float ws = size * 0.11f;
        float wg = size * 0.03f;
        g.fill(new RoundRectangle2D.Float(wx, wy, ws, ws, size * 0.02f, size * 0.02f));
        g.fill(new RoundRectangle2D.Float(wx + ws + wg, wy, ws, ws, size * 0.02f, size * 0.02f));
        g.fill(new RoundRectangle2D.Float(wx, wy + ws + wg, ws, ws, size * 0.02f, size * 0.02f));
        g.fill(new RoundRectangle2D.Float(wx + ws + wg, wy + ws + wg, ws, ws, size * 0.02f, size * 0.02f));

        // Nửa đầu Android xanh lá đặc trưng
        g.setColor(new Color(0x3DDC84));
        float headW = size * 0.50f;
        float headH = size * 0.44f;
        float headX = (size - headW) / 2f;
        float headY = size * 0.31f;
        g.fill(new Arc2D.Float(headX, headY, headW, headH, 0, 180, Arc2D.PIE));

        // Râu ăng-ten Android
        g.setStroke(new BasicStroke(Math.max(1.2f, size * 0.055f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(Math.round(size * 0.36f), Math.round(size * 0.35f),
                Math.round(size * 0.29f), Math.round(size * 0.22f));
        g.drawLine(Math.round(size * 0.64f), Math.round(size * 0.35f),
                Math.round(size * 0.71f), Math.round(size * 0.22f));

        // Mắt Android
        g.setColor(new Color(0x1E293B));
        float eyeR = Math.max(1.5f, size * 0.055f);
        g.fillOval(Math.round(size * 0.38f - eyeR / 2f), Math.round(size * 0.42f - eyeR / 2f),
                Math.round(eyeR), Math.round(eyeR));
        g.fillOval(Math.round(size * 0.62f - eyeR / 2f), Math.round(size * 0.42f - eyeR / 2f),
                Math.round(eyeR), Math.round(eyeR));

        // Thanh thân chữ "AAOW"
        if (size >= 32) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Segoe UI", Font.BOLD, Math.max(8, Math.round(size * 0.21f))));
            FontMetrics fm = g.getFontMetrics();
            String txt = "AAOW";
            g.drawString(txt, (size - fm.stringWidth(txt)) / 2, Math.round(size * 0.78f));
        } else {
            g.setColor(new Color(0x3DDC84));
            g.fill(new RoundRectangle2D.Float(size * 0.26f, size * 0.60f, size * 0.48f, size * 0.20f, 3, 3));
        }
        g.dispose();
        return img;
    }

    public static BufferedImage renderApkDocumentLogo(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        float pad = size * 0.08f;
        float w = size - pad * 2f;
        float h = size - pad * 2f;
        g.setPaint(new GradientPaint(0, 0, new Color(0x2ECC71), size, size, new Color(0x1B7F4B)));
        g.fill(new RoundRectangle2D.Float(pad, pad, w, h, size * 0.2f, size * 0.2f));

        g.setColor(new Color(255, 255, 255, 210));
        g.setStroke(new BasicStroke(Math.max(1.2f, size * 0.04f)));
        g.draw(new RoundRectangle2D.Float(pad, pad, w, h, size * 0.2f, size * 0.2f));

        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, Math.max(7, Math.round(size * 0.30f))));
        FontMetrics fm = g.getFontMetrics();
        String txt = "APK";
        g.drawString(txt, (size - fm.stringWidth(txt)) / 2, (size + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
        return img;
    }

    static void writeIco(Path outFile, List<BufferedImage> images) throws IOException {
        if (outFile.getParent() != null) {
            Files.createDirectories(outFile.getParent());
        }
        List<byte[]> payloads = new ArrayList<>(images.size());
        for (BufferedImage img : images) {
            if (img.getWidth() >= 256 || img.getHeight() >= 256) {
                ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
                ImageIO.write(img, "png", pngOut);
                payloads.add(pngOut.toByteArray());
            } else {
                payloads.add(encodeBmpDib32(img));
            }
        }

        int count = images.size();
        int headerSize = 6 + count * 16;
        int totalSize = headerSize;
        for (byte[] p : payloads) totalSize += p.length;

        ByteBuffer buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort((short) 0);      // Reserved
        buf.putShort((short) 1);      // Type: ICO
        buf.putShort((short) count);  // Count

        int offset = headerSize;
        for (int i = 0; i < count; i++) {
            BufferedImage img = images.get(i);
            byte[] payload = payloads.get(i);
            int w = img.getWidth();
            int h = img.getHeight();
            buf.put((byte) (w >= 256 ? 0 : w));
            buf.put((byte) (h >= 256 ? 0 : h));
            buf.put((byte) 0);        // Color count
            buf.put((byte) 0);        // Reserved
            buf.putShort((short) 1);  // Planes
            buf.putShort((short) 32); // BitCount
            buf.putInt(payload.length);
            buf.putInt(offset);
            offset += payload.length;
        }
        for (byte[] payload : payloads) {
            buf.put(payload);
        }
        Files.write(outFile, buf.array());
    }

    private static byte[] encodeBmpDib32(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int andStride = ((w + 31) / 32) * 4;
        int xorSize = w * h * 4;
        int andSize = andStride * h;
        ByteBuffer buf = ByteBuffer.allocate(40 + xorSize + andSize).order(ByteOrder.LITTLE_ENDIAN);
        // BITMAPINFOHEADER (40 bytes)
        buf.putInt(40);
        buf.putInt(w);
        buf.putInt(h * 2); // XOR + AND mask height
        buf.putShort((short) 1);
        buf.putShort((short) 32);
        buf.putInt(0); // BI_RGB
        buf.putInt(xorSize + andSize);
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(0);

        // Bottom-up BGRA pixels
        for (int y = h - 1; y >= 0; y--) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                buf.put((byte) (argb & 0xFF));         // B
                buf.put((byte) ((argb >>> 8) & 0xFF)); // G
                buf.put((byte) ((argb >>> 16) & 0xFF));// R
                buf.put((byte) ((argb >>> 24) & 0xFF));// A
            }
        }
        // AND mask (all 0 since 32-bit alpha channel is used)
        for (int i = 0; i < andSize; i++) {
            buf.put((byte) 0);
        }
        return buf.array();
    }
}
