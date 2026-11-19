package dev.aaow.rt;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import dev.aaow.apk.XmlElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bộ nhớ đệm LRU có giới hạn dung lượng (byte-bounded) cho tài nguyên APK:
 * - Bitmap đã giải mã (mặc định tối đa 24 MB)
 * - Cây XML nhị phân đã giải mã (layout/drawable/menu)
 * - Drawable.ConstantState (tránh đọc lại ZIP và dựng lại VectorDrawable/GradientDrawable mỗi khung hình)
 */
public final class ResourceCache {
    public static final long DEFAULT_MAX_BITMAP_BYTES = 24L * 1024L * 1024L;
    public static final int DEFAULT_MAX_XML_ENTRIES = 512;
    public static final int DEFAULT_MAX_DRAWABLE_ENTRIES = 512;

    public record CacheStats(
            long bitmapHits,
            long bitmapMisses,
            long xmlHits,
            long xmlMisses,
            long drawableHits,
            long drawableMisses,
            long evictions,
            long currentBitmapBytes,
            long maxBitmapBytes,
            int bitmapEntries,
            int xmlEntries,
            int drawableEntries
    ) {
        public double hitRatioPercent() {
            long totalHits = bitmapHits + xmlHits + drawableHits;
            long totalReq = totalHits + bitmapMisses + xmlMisses + drawableMisses;
            return totalReq == 0 ? 100.0 : (totalHits * 100.0 / totalReq);
        }
    }

    private final long maxBitmapBytes;
    private final int maxXmlEntries;
    private final int maxDrawableEntries;

    private final LinkedHashMap<Integer, BitmapEntry> bitmapCache = new LinkedHashMap<>(64, 0.75f, true);
    private final LinkedHashMap<Integer, XmlElement> xmlCache = new LinkedHashMap<>(64, 0.75f, true);
    private final LinkedHashMap<Integer, Drawable.ConstantState> drawableStateCache = new LinkedHashMap<>(64, 0.75f, true);

    private long currentBitmapBytes;
    private long bitmapHits;
    private long bitmapMisses;
    private long xmlHits;
    private long xmlMisses;
    private long drawableHits;
    private long drawableMisses;
    private long evictions;

    private record BitmapEntry(Bitmap bitmap, long bytes) {
    }

    public ResourceCache() {
        this(DEFAULT_MAX_BITMAP_BYTES, DEFAULT_MAX_XML_ENTRIES, DEFAULT_MAX_DRAWABLE_ENTRIES);
    }

    public ResourceCache(long maxBitmapBytes, int maxXmlEntries, int maxDrawableEntries) {
        this.maxBitmapBytes = Math.max(1024L, maxBitmapBytes);
        this.maxXmlEntries = Math.max(8, maxXmlEntries);
        this.maxDrawableEntries = Math.max(8, maxDrawableEntries);
    }

    public synchronized Bitmap getBitmap(int resId) {
        BitmapEntry entry = bitmapCache.get(resId);
        if (entry != null) {
            if (entry.bitmap().isRecycled()) {
                bitmapCache.remove(resId);
                currentBitmapBytes = Math.max(0L, currentBitmapBytes - entry.bytes());
                bitmapMisses++;
                return null;
            }
            bitmapHits++;
            return entry.bitmap();
        }
        bitmapMisses++;
        return null;
    }

    public synchronized void putBitmap(int resId, Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return;
        long bytes = Math.max(64L, bitmap.getAllocationByteCount());
        if (bytes > maxBitmapBytes) {
            return;
        }
        BitmapEntry prev = bitmapCache.remove(resId);
        if (prev != null) {
            currentBitmapBytes = Math.max(0L, currentBitmapBytes - prev.bytes());
        }
        bitmapCache.put(resId, new BitmapEntry(bitmap, bytes));
        currentBitmapBytes += bytes;
        trimBitmapsTo(maxBitmapBytes);
    }

    public synchronized XmlElement getXml(int resId) {
        XmlElement el = xmlCache.get(resId);
        if (el != null) {
            xmlHits++;
            return el;
        }
        xmlMisses++;
        return null;
    }

    public synchronized void putXml(int resId, XmlElement element) {
        if (element == null) return;
        xmlCache.put(resId, element);
        while (xmlCache.size() > maxXmlEntries) {
            var it = xmlCache.entrySet().iterator();
            if (!it.hasNext()) break;
            it.next();
            it.remove();
            evictions++;
        }
    }

    public synchronized Drawable.ConstantState getDrawableState(int resId) {
        Drawable.ConstantState state = drawableStateCache.get(resId);
        if (state != null) {
            drawableHits++;
            return state;
        }
        drawableHitsOrMiss(false);
        return null;
    }

    private void drawableHitsOrMiss(boolean hit) {
        if (hit) drawableHits++;
        else drawableMisses++;
    }

    public synchronized void putDrawableState(int resId, Drawable.ConstantState state) {
        if (state == null) return;
        drawableStateCache.put(resId, state);
        while (drawableStateCache.size() > maxDrawableEntries) {
            var it = drawableStateCache.entrySet().iterator();
            if (!it.hasNext()) break;
            it.next();
            it.remove();
            evictions++;
        }
    }

    public synchronized void trimMemory(int level) {
        if (level >= 60) { // TRIM_MEMORY_MODERATE / COMPLETE
            clear();
        } else if (level >= 15) { // TRIM_MEMORY_RUNNING_CRITICAL / UI_HIDDEN
            trimBitmapsTo(maxBitmapBytes / 2);
        }
    }

    public synchronized void clear() {
        bitmapCache.clear();
        xmlCache.clear();
        drawableStateCache.clear();
        currentBitmapBytes = 0L;
    }

    private void trimBitmapsTo(long targetMaxBytes) {
        while (currentBitmapBytes > targetMaxBytes && !bitmapCache.isEmpty()) {
            var it = bitmapCache.entrySet().iterator();
            if (!it.hasNext()) break;
            Map.Entry<Integer, BitmapEntry> eldest = it.next();
            currentBitmapBytes = Math.max(0L, currentBitmapBytes - eldest.getValue().bytes());
            it.remove();
            evictions++;
        }
    }

    public synchronized CacheStats getStats() {
        return new CacheStats(
                bitmapHits, bitmapMisses,
                xmlHits, xmlMisses,
                drawableHits, drawableMisses,
                evictions,
                currentBitmapBytes,
                maxBitmapBytes,
                bitmapCache.size(),
                xmlCache.size(),
                drawableStateCache.size()
        );
    }
}
