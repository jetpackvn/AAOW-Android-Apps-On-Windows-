package android.os;

import java.io.File;

/** Truy vấn dung lượng ổ đĩa Windows qua API android.os.StatFs. */
public class StatFs {
    private static final long BLOCK_SIZE = 4096L;
    private File target;

    public StatFs(String path) {
        restat(path);
    }

    public void restat(String path) {
        File f = path != null && !path.isEmpty() ? new File(path) : new File(".");
        while (f != null && !f.exists()) {
            f = f.getParentFile();
        }
        this.target = f != null ? f : new File(".");
    }

    public long getBlockSizeLong() {
        return BLOCK_SIZE;
    }

    @Deprecated
    public int getBlockSize() {
        return (int) BLOCK_SIZE;
    }

    public long getTotalBytes() {
        long total = target.getTotalSpace();
        return total > 0 ? total : 64L * 1024 * 1024 * 1024;
    }

    public long getFreeBytes() {
        long free = target.getFreeSpace();
        return free > 0 ? free : 16L * 1024 * 1024 * 1024;
    }

    public long getAvailableBytes() {
        long usable = target.getUsableSpace();
        return usable > 0 ? usable : getFreeBytes();
    }

    public long getBlockCountLong() {
        return getTotalBytes() / BLOCK_SIZE;
    }

    @Deprecated
    public int getBlockCount() {
        return (int) Math.min(Integer.MAX_VALUE, getBlockCountLong());
    }

    public long getFreeBlocksLong() {
        return getFreeBytes() / BLOCK_SIZE;
    }

    @Deprecated
    public int getFreeBlocks() {
        return (int) Math.min(Integer.MAX_VALUE, getFreeBlocksLong());
    }

    public long getAvailableBlocksLong() {
        return getAvailableBytes() / BLOCK_SIZE;
    }

    @Deprecated
    public int getAvailableBlocks() {
        return (int) Math.min(Integer.MAX_VALUE, getAvailableBlocksLong());
    }
}
