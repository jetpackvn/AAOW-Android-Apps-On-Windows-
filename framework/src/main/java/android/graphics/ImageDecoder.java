package android.graphics;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Size;
import dev.aaow.rt.jni.WebpHelper;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * Cài đặt thật android.graphics.ImageDecoder (API 28+) phục vụ Glide, Coil, Picasso và ứng dụng hiện đại.
 * Hỗ trợ createSource từ byte[], ByteBuffer, File, ContentResolver/Uri, AssetManager, Resources, Callable<AssetFileDescriptor>,
 * OnHeaderDecodedListener (setTargetSize, setTargetSampleSize, setCrop, setPostProcessor) và xuất Bitmap / Drawable.
 */
public final class ImageDecoder implements AutoCloseable {
    public static final int ALLOCATOR_DEFAULT = 0;
    public static final int ALLOCATOR_SOFTWARE = 1;
    public static final int ALLOCATOR_SHARED_MEMORY = 2;
    public static final int ALLOCATOR_HARDWARE = 3;

    public static final int MEMORY_POLICY_LOW_RAM = 0;
    public static final int MEMORY_POLICY_DEFAULT = 1;

    public abstract static class Source {
        Source() {
        }

        abstract byte[] readBytes() throws IOException;

        Resources getResources() {
            return null;
        }
    }

    public static class ImageInfo {
        private final Size size;
        private final String mimeType;
        private final boolean animated;
        private final ColorSpace colorSpace;

        ImageInfo(Size size, String mimeType, boolean animated, ColorSpace colorSpace) {
            this.size = size;
            this.mimeType = mimeType != null ? mimeType : "image/png";
            this.animated = animated;
            this.colorSpace = colorSpace != null ? colorSpace : ColorSpace.get(ColorSpace.Named.SRGB);
        }

        public Size getSize() {
            return size;
        }

        public String getMimeType() {
            return mimeType;
        }

        public boolean isAnimated() {
            return animated;
        }

        public ColorSpace getColorSpace() {
            return colorSpace;
        }
    }

    public interface OnHeaderDecodedListener {
        void onHeaderDecoded(ImageDecoder decoder, ImageInfo info, Source source);
    }

    public interface OnPartialImageListener {
        boolean onPartialImage(DecodeException exception);
    }

    public static final class DecodeException extends IOException {
        public static final int SOURCE_EXCEPTION = 1;
        public static final int SOURCE_INCOMPLETE = 2;
        public static final int SOURCE_MALFORMED_DATA = 3;

        private final int error;
        private final Source source;

        public DecodeException(int error, String msg, Throwable cause, Source source) {
            super(msg, cause);
            this.error = error;
            this.source = source;
        }

        public int getError() {
            return error;
        }

        public Source getSource() {
            return source;
        }
    }

    private static volatile OnHeaderDecodedListener defaultProcessListener;
    private static final ThreadLocal<OnHeaderDecodedListener> DEFAULT_THREAD_LISTENER = new ThreadLocal<>();

    private int allocator = ALLOCATOR_DEFAULT;
    private int memorySizePolicy = MEMORY_POLICY_DEFAULT;
    private boolean mutableRequired = false;
    private boolean unpremultipliedRequired = false;
    private boolean decodeAsAlphaMask = false;
    private int targetWidth = -1;
    private int targetHeight = -1;
    private int targetSampleSize = 1;
    private Rect cropRect;
    private PostProcessor postProcessor;
    private OnPartialImageListener onPartialImageListener;
    private ColorSpace targetColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
    private boolean closed = false;

    ImageDecoder() {
    }

    public static boolean isMimeTypeSupported(String mimeType) {
        if (mimeType == null) return false;
        String lower = mimeType.toLowerCase(Locale.ROOT);
        return lower.equals("image/png")
                || lower.equals("image/jpeg")
                || lower.equals("image/jpg")
                || lower.equals("image/webp")
                || lower.equals("image/gif")
                || lower.equals("image/bmp")
                || lower.equals("image/x-ms-bmp")
                || lower.equals("image/ico")
                || lower.equals("image/x-icon");
    }

    public static Source createSource(Resources res, int resId) {
        if (res == null) throw new NullPointerException("Resources cannot be null");
        return new Source() {
            @Override
            byte[] readBytes() throws IOException {
                try (InputStream in = res.openRawResource(resId)) {
                    if (in != null) return in.readAllBytes();
                } catch (Exception ignored) {
                }
                Bitmap bmp = res.loadBitmap(resId);
                if (bmp != null) {
                    java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
                    return bos.toByteArray();
                }
                throw new IOException("Cannot load resource id 0x" + Integer.toHexString(resId));
            }

            @Override
            Resources getResources() {
                return res;
            }
        };
    }

    public static Source createSource(ContentResolver cr, Uri uri) {
        if (cr == null || uri == null) throw new NullPointerException("ContentResolver and Uri cannot be null");
        return new Source() {
            @Override
            byte[] readBytes() throws IOException {
                try (InputStream in = cr.openInputStream(uri)) {
                    if (in == null) throw new IOException("Null stream for URI: " + uri);
                    return in.readAllBytes();
                }
            }
        };
    }

    public static Source createSource(AssetManager assets, String fileName) {
        if (assets == null || fileName == null) throw new NullPointerException("AssetManager and fileName cannot be null");
        return new Source() {
            @Override
            byte[] readBytes() throws IOException {
                try (InputStream in = assets.open(fileName)) {
                    if (in == null) throw new IOException("Asset not found: " + fileName);
                    return in.readAllBytes();
                }
            }
        };
    }

    public static Source createSource(byte[] data, int offset, int length) throws ArrayIndexOutOfBoundsException {
        if (data == null) throw new NullPointerException("data cannot be null");
        if (offset < 0 || length < 0 || offset >= data.length || offset + length > data.length) {
            throw new ArrayIndexOutOfBoundsException("Invalid offset/length");
        }
        final byte[] copy = Arrays.copyOfRange(data, offset, offset + length);
        return new Source() {
            @Override
            byte[] readBytes() {
                return copy;
            }
        };
    }

    public static Source createSource(byte[] data) {
        if (data == null) throw new NullPointerException("data cannot be null");
        return createSource(data, 0, data.length);
    }

    public static Source createSource(ByteBuffer buffer) {
        if (buffer == null) throw new NullPointerException("buffer cannot be null");
        ByteBuffer dup = buffer.slice();
        byte[] bytes = new byte[dup.remaining()];
        dup.get(bytes);
        return new Source() {
            @Override
            byte[] readBytes() {
                return bytes;
            }
        };
    }

    public static Source createSource(File file) {
        if (file == null) throw new NullPointerException("file cannot be null");
        return new Source() {
            @Override
            byte[] readBytes() throws IOException {
                return Files.readAllBytes(file.toPath());
            }
        };
    }

    public static Source createSource(Callable<AssetFileDescriptor> callable) {
        if (callable == null) throw new NullPointerException("callable cannot be null");
        return new Source() {
            @Override
            byte[] readBytes() throws IOException {
                try (AssetFileDescriptor afd = callable.call()) {
                    if (afd == null) throw new IOException("Callable returned null AssetFileDescriptor");
                    try (InputStream in = afd.createInputStream()) {
                        return in.readAllBytes();
                    }
                } catch (IOException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IOException("Failed to open AssetFileDescriptor", e);
                }
            }
        };
    }

    public static Bitmap decodeBitmap(Source src) throws IOException {
        return decodeBitmap(src, null);
    }

    public static Bitmap decodeBitmap(Source src, OnHeaderDecodedListener listener) throws IOException {
        if (src == null) throw new NullPointerException("Source cannot be null");
        byte[] bytes;
        try {
            bytes = src.readBytes();
        } catch (IOException e) {
            throw new DecodeException(DecodeException.SOURCE_EXCEPTION, e.getMessage(), e, src);
        }
        if (bytes == null || bytes.length == 0) {
            throw new DecodeException(DecodeException.SOURCE_INCOMPLETE, "Empty image data", null, src);
        }

        BitmapFactory.Options boundsOpts = new BitmapFactory.Options();
        boundsOpts.inJustDecodeBounds = true;
        WebpHelper.decodeWithOptions(bytes, 0, bytes.length, boundsOpts);
        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) {
            throw new DecodeException(DecodeException.SOURCE_MALFORMED_DATA, "Unable to decode image header", null, src);
        }

        try (ImageDecoder decoder = new ImageDecoder()) {
            ImageInfo info = new ImageInfo(
                    new Size(boundsOpts.outWidth, boundsOpts.outHeight),
                    boundsOpts.outMimeType != null ? boundsOpts.outMimeType : "image/png",
                    "image/gif".equalsIgnoreCase(boundsOpts.outMimeType),
                    ColorSpace.get(ColorSpace.Named.SRGB)
            );
            OnHeaderDecodedListener proc = defaultProcessListener;
            if (proc != null) proc.onHeaderDecoded(decoder, info, src);
            OnHeaderDecodedListener th = DEFAULT_THREAD_LISTENER.get();
            if (th != null) th.onHeaderDecoded(decoder, info, src);
            if (listener != null) {
                listener.onHeaderDecoded(decoder, info, src);
            }

            BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
            decodeOpts.inSampleSize = Math.max(1, decoder.targetSampleSize);
            decodeOpts.inMutable = decoder.mutableRequired;
            decodeOpts.inPreferredColorSpace = decoder.targetColorSpace;
            Bitmap bmp = WebpHelper.decodeWithOptions(bytes, 0, bytes.length, decodeOpts);
            if (bmp == null) {
                throw new DecodeException(DecodeException.SOURCE_MALFORMED_DATA, "Failed to decode bitmap", null, src);
            }

            // Áp dụng targetWidth / targetHeight nếu listener gọi setTargetSize()
            if (decoder.targetWidth > 0 && decoder.targetHeight > 0
                    && (bmp.getWidth() != decoder.targetWidth || bmp.getHeight() != decoder.targetHeight)) {
                bmp = Bitmap.createScaledBitmap(bmp, decoder.targetWidth, decoder.targetHeight, true);
            }

            // Áp dụng cropRect nếu listener gọi setCrop()
            if (decoder.cropRect != null) {
                Rect r = decoder.cropRect;
                int x = Math.max(0, Math.min(bmp.getWidth() - 1, r.left));
                int y = Math.max(0, Math.min(bmp.getHeight() - 1, r.top));
                int w = Math.max(1, Math.min(bmp.getWidth() - x, r.width()));
                int h = Math.max(1, Math.min(bmp.getHeight() - y, r.height()));
                bmp = Bitmap.createBitmap(bmp, x, y, w, h);
            }

            // Áp dụng PostProcessor nếu có
            if (decoder.postProcessor != null) {
                Canvas canvas = new Canvas(bmp);
                decoder.postProcessor.onPostProcess(canvas);
            }
            return bmp;
        }
    }

    public static Drawable decodeDrawable(Source src) throws IOException {
        return decodeDrawable(src, null);
    }

    public static Drawable decodeDrawable(Source src, OnHeaderDecodedListener listener) throws IOException {
        Bitmap bmp = decodeBitmap(src, listener);
        return new BitmapDrawable(src != null ? src.getResources() : null, bmp);
    }

    public void setTargetSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Dimensions must be positive: " + width + "x" + height);
        }
        this.targetWidth = width;
        this.targetHeight = height;
    }

    public void setTargetSampleSize(int sampleSize) {
        if (sampleSize < 1) {
            throw new IllegalArgumentException("sampleSize must be >= 1");
        }
        this.targetSampleSize = sampleSize;
    }

    public void setAllocator(int allocator) {
        this.allocator = allocator;
    }

    public int getAllocator() {
        return allocator;
    }

    public void setMemorySizePolicy(int policy) {
        this.memorySizePolicy = policy;
    }

    public int getMemorySizePolicy() {
        return memorySizePolicy;
    }

    public void setMutableRequired(boolean mutable) {
        this.mutableRequired = mutable;
    }

    public boolean isMutableRequired() {
        return mutableRequired;
    }

    public void setUnpremultipliedRequired(boolean unpremultipliedRequired) {
        this.unpremultipliedRequired = unpremultipliedRequired;
    }

    public boolean isUnpremultipliedRequired() {
        return unpremultipliedRequired;
    }

    public void setDecodeAsAlphaMaskEnabled(boolean enabled) {
        this.decodeAsAlphaMask = enabled;
    }

    public boolean isDecodeAsAlphaMaskEnabled() {
        return decodeAsAlphaMask;
    }

    public void setCrop(Rect subset) {
        this.cropRect = subset != null ? new Rect(subset) : null;
    }

    public Rect getCrop() {
        return cropRect;
    }

    public void setPostProcessor(PostProcessor postProcessor) {
        this.postProcessor = postProcessor;
    }

    public PostProcessor getPostProcessor() {
        return postProcessor;
    }

    public void setOnPartialImageListener(OnPartialImageListener listener) {
        this.onPartialImageListener = listener;
    }

    public OnPartialImageListener getOnPartialImageListener() {
        return onPartialImageListener;
    }

    public void setTargetColorSpace(ColorSpace colorSpace) {
        this.targetColorSpace = colorSpace;
    }

    public static void setDefaultProcessListener(OnHeaderDecodedListener listener) {
        defaultProcessListener = listener;
    }

    public static OnHeaderDecodedListener getDefaultProcessListener() {
        return defaultProcessListener;
    }

    public static void setDefaultThreadListener(OnHeaderDecodedListener listener) {
        DEFAULT_THREAD_LISTENER.set(listener);
    }

    public static OnHeaderDecodedListener getDefaultThreadListener() {
        return DEFAULT_THREAD_LISTENER.get();
    }

    @Override
    public void close() {
        closed = true;
    }
}
