package android.os;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thông số bộ nhớ & chẩn đoán runtime tương thích {@code android.os.Debug} trên JVM Windows.
 */
public final class Debug {
    public static final int TRACE_COUNT_ALLOCS = 1;
    public static final int SHOW_FULL_DETAIL = 1;
    public static final int SHOW_CLASSLOADER = 2;
    public static final int SHOW_INITIALIZED = 4;

    Debug() {
    }

    public static class MemoryInfo implements Parcelable {
        public int dalvikPss;
        public int dalvikPrivateDirty;
        public int dalvikSharedDirty;
        public int nativePss;
        public int nativePrivateDirty;
        public int nativeSharedDirty;
        public int otherPss;
        public int otherPrivateDirty;
        public int otherSharedDirty;

        public MemoryInfo() {
        }

        public int getTotalPss() {
            return dalvikPss + nativePss + otherPss;
        }

        public int getTotalSwappablePss() {
            return 0;
        }

        public int getTotalPrivateDirty() {
            return dalvikPrivateDirty + nativePrivateDirty + otherPrivateDirty;
        }

        public int getTotalSharedDirty() {
            return dalvikSharedDirty + nativeSharedDirty + otherSharedDirty;
        }

        public int getTotalPrivateClean() {
            return 0;
        }

        public int getTotalSharedClean() {
            return 0;
        }

        public String getMemoryStat(String statName) {
            return getMemoryStats().get(statName);
        }

        public Map<String, String> getMemoryStats() {
            Map<String, String> stats = new LinkedHashMap<>();
            stats.put("summary.java-heap", Integer.toString(dalvikPrivateDirty));
            stats.put("summary.native-heap", Integer.toString(nativePrivateDirty));
            stats.put("summary.code", "0");
            stats.put("summary.stack", "0");
            stats.put("summary.graphics", "0");
            stats.put("summary.private-other", Integer.toString(otherPrivateDirty));
            stats.put("summary.system", "0");
            stats.put("summary.total-pss", Integer.toString(getTotalPss()));
            stats.put("summary.total-swap", "0");
            return stats;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(dalvikPss);
            dest.writeInt(dalvikPrivateDirty);
            dest.writeInt(dalvikSharedDirty);
            dest.writeInt(nativePss);
            dest.writeInt(nativePrivateDirty);
            dest.writeInt(nativeSharedDirty);
            dest.writeInt(otherPss);
            dest.writeInt(otherPrivateDirty);
            dest.writeInt(otherSharedDirty);
        }

        public void readFromParcel(Parcel source) {
            dalvikPss = source.readInt();
            dalvikPrivateDirty = source.readInt();
            dalvikSharedDirty = source.readInt();
            nativePss = source.readInt();
            nativePrivateDirty = source.readInt();
            nativeSharedDirty = source.readInt();
            otherPss = source.readInt();
            otherPrivateDirty = source.readInt();
            otherSharedDirty = source.readInt();
        }

        public static final Creator<MemoryInfo> CREATOR = new Creator<>() {
            @Override
            public MemoryInfo createFromParcel(Parcel source) {
                MemoryInfo info = new MemoryInfo();
                info.readFromParcel(source);
                return info;
            }

            @Override
            public MemoryInfo[] newArray(int size) {
                return new MemoryInfo[size];
            }
        };
    }

    public static void waitForDebugger() {
    }

    public static boolean waitingForDebugger() {
        return false;
    }

    public static boolean isDebuggerConnected() {
        return false;
    }

    public static void changeDebugPort(int port) {
    }

    public static void startNativeTracing() {
    }

    public static void stopNativeTracing() {
    }

    public static void enableEmulatorTraceOutput() {
    }

    public static void startMethodTracing() {
    }

    public static void startMethodTracing(String tracePath) {
    }

    public static void startMethodTracing(String tracePath, int bufferSize) {
    }

    public static void startMethodTracing(String tracePath, int bufferSize, int flags) {
    }

    public static void startMethodTracingSampling(String tracePath, int bufferSize, int intervalUs) {
    }

    public static void stopMethodTracing() {
    }

    public static long threadCpuTimeNanos() {
        try {
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            if (bean != null && bean.isCurrentThreadCpuTimeSupported()) {
                return bean.getCurrentThreadCpuTime();
            }
        } catch (Throwable ignored) {
        }
        return System.nanoTime();
    }

    public static void startAllocCounting() {
    }

    public static void stopAllocCounting() {
    }

    public static int getGlobalAllocCount() {
        return 0;
    }

    public static void resetGlobalAllocCount() {
    }

    public static int getGlobalAllocSize() {
        return 0;
    }

    public static void resetGlobalAllocSize() {
    }

    public static int getGlobalFreedCount() {
        return 0;
    }

    public static void resetGlobalFreedCount() {
    }

    public static int getGlobalFreedSize() {
        return 0;
    }

    public static void resetGlobalFreedSize() {
    }

    public static int getGlobalGcInvocationCount() {
        return 0;
    }

    public static void resetGlobalGcInvocationCount() {
    }

    public static int getGlobalClassInitCount() {
        return 0;
    }

    public static void resetGlobalClassInitCount() {
    }

    public static int getGlobalClassInitTime() {
        return 0;
    }

    public static void resetGlobalClassInitTime() {
    }

    public static int getGlobalExternalAllocCount() {
        return 0;
    }

    public static void resetGlobalExternalAllocCount() {
    }

    public static int getGlobalExternalAllocSize() {
        return 0;
    }

    public static void resetGlobalExternalAllocSize() {
    }

    public static int getGlobalExternalFreedCount() {
        return 0;
    }

    public static void resetGlobalExternalFreedCount() {
    }

    public static int getGlobalExternalFreedSize() {
        return 0;
    }

    public static void resetGlobalExternalFreedSize() {
    }

    public static int getThreadAllocCount() {
        return 0;
    }

    public static void resetThreadAllocCount() {
    }

    public static int getThreadAllocSize() {
        return 0;
    }

    public static void resetThreadAllocSize() {
    }

    public static int getThreadExternalAllocCount() {
        return 0;
    }

    public static void resetThreadExternalAllocCount() {
    }

    public static int getThreadExternalAllocSize() {
        return 0;
    }

    public static void resetThreadExternalAllocSize() {
    }

    public static int getThreadGcInvocationCount() {
        return 0;
    }

    public static void resetThreadGcInvocationCount() {
    }

    public static void resetAllCounts() {
    }

    public static long getNativeHeapSize() {
        return Runtime.getRuntime().totalMemory();
    }

    public static long getNativeHeapAllocatedSize() {
        Runtime rt = Runtime.getRuntime();
        return Math.max(0L, rt.totalMemory() - rt.freeMemory());
    }

    public static long getNativeHeapFreeSize() {
        return Runtime.getRuntime().freeMemory();
    }

    public static void getMemoryInfo(MemoryInfo memoryInfo) {
        if (memoryInfo == null) {
            return;
        }
        int usedKb = (int) Math.min(Integer.MAX_VALUE, getNativeHeapAllocatedSize() / 1024L);
        memoryInfo.dalvikPss = usedKb;
        memoryInfo.dalvikPrivateDirty = usedKb;
        memoryInfo.dalvikSharedDirty = 0;
        memoryInfo.nativePss = 0;
        memoryInfo.nativePrivateDirty = 0;
        memoryInfo.nativeSharedDirty = 0;
        memoryInfo.otherPss = 0;
        memoryInfo.otherPrivateDirty = 0;
        memoryInfo.otherSharedDirty = 0;
    }

    public static long getPss() {
        return getNativeHeapAllocatedSize() / 1024L;
    }

    public static long getRss() {
        return Runtime.getRuntime().totalMemory() / 1024L;
    }

    public static int setAllocationLimit(int limit) {
        return -1;
    }

    public static int setGlobalAllocationLimit(int limit) {
        return -1;
    }

    public static void printLoadedClasses(int flags) {
    }

    public static int getLoadedClassCount() {
        try {
            return ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static void dumpHprofData(String fileName) throws IOException {
    }

    public static int getBinderSentTransactions() {
        return 0;
    }

    public static int getBinderReceivedTransactions() {
        return 0;
    }

    public static int getBinderLocalObjectCount() {
        return 0;
    }

    public static int getBinderProxyObjectCount() {
        return 0;
    }

    public static int getBinderDeathObjectCount() {
        return 0;
    }

    public static boolean dumpService(String name, FileDescriptor fd, String[] args) {
        return false;
    }

    public static void attachJvmtiAgent(String library, String options, ClassLoader classLoader) throws IOException {
    }

    public static String getRuntimeStat(String statName) {
        return getRuntimeStats().get(statName);
    }

    public static Map<String, String> getRuntimeStats() {
        return Collections.emptyMap();
    }

    public static long getInstanceCount(Class<?> cls) {
        return 0L;
    }

    public static long getInstanceCount(Class<?> cls, boolean includeSubclasses) {
        return 0L;
    }

    public static long[] getInstanceCounts(List<Class<?>> classes) {
        return classes == null ? new long[0] : new long[classes.size()];
    }

    public static long[] getInstanceCounts(List<Class<?>> classes, boolean includeSubclasses) {
        return classes == null ? new long[0] : new long[classes.size()];
    }
}
