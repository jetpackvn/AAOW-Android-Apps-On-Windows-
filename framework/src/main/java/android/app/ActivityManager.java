package android.app;

import android.content.ComponentName;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;

import java.util.ArrayList;
import java.util.List;

/** Dịch vụ quản lý tiến trình và bộ nhớ đang chạy android.app.ActivityManager. */
public class ActivityManager {
    private final Context context;

    public ActivityManager() {
        this(null);
    }

    public ActivityManager(Context context) {
        this.context = context;
    }

    public static class RunningAppProcessInfo implements Parcelable {
        public static final int IMPORTANCE_FOREGROUND = 100;
        public static final int IMPORTANCE_FOREGROUND_SERVICE = 125;
        public static final int IMPORTANCE_VISIBLE = 200;
        public static final int IMPORTANCE_PERCEPTIBLE = 230;
        public static final int IMPORTANCE_PERCEPTIBLE_PRE_26 = 130;
        public static final int IMPORTANCE_CANT_SAVE_STATE = 350;
        public static final int IMPORTANCE_SERVICE = 300;
        public static final int IMPORTANCE_TOP_SLEEPING = 325;
        public static final int IMPORTANCE_TOP_SLEEPING_PRE_28 = 150;
        public static final int IMPORTANCE_BACKGROUND = 400;
        public static final int IMPORTANCE_CACHED = 400;
        public static final int IMPORTANCE_EMPTY = 500;
        public static final int IMPORTANCE_GONE = 1000;

        public static final int REASON_UNKNOWN = 0;
        public static final int REASON_PROVIDER_IN_USE = 1;
        public static final int REASON_SERVICE_IN_USE = 2;

        public String processName;
        public int pid;
        public int uid;
        public String[] pkgList;
        public int importance;
        public int lru;
        public int importanceReasonCode;
        public int importanceReasonPid;
        public ComponentName importanceReasonComponent;
        public int lastTrimLevel;

        public RunningAppProcessInfo() {
        }

        public RunningAppProcessInfo(String pName, int pPid, String[] pkgs) {
            this.processName = pName;
            this.pid = pPid;
            this.pkgList = pkgs;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public void readFromParcel(Parcel source) {
            processName = source.readString();
            pid = source.readInt();
            uid = source.readInt();
            pkgList = source.createStringArray();
            importance = source.readInt();
            lru = source.readInt();
            importanceReasonCode = source.readInt();
            importanceReasonPid = source.readInt();
            importanceReasonComponent = source.readParcelable(null);
            lastTrimLevel = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(processName);
            dest.writeInt(pid);
            dest.writeInt(uid);
            dest.writeStringArray(pkgList);
            dest.writeInt(importance);
            dest.writeInt(lru);
            dest.writeInt(importanceReasonCode);
            dest.writeInt(importanceReasonPid);
            dest.writeParcelable(importanceReasonComponent, flags);
            dest.writeInt(lastTrimLevel);
        }

        public static final Creator<RunningAppProcessInfo> CREATOR = new Creator<>() {
            @Override
            public RunningAppProcessInfo createFromParcel(Parcel source) {
                RunningAppProcessInfo info = new RunningAppProcessInfo();
                info.readFromParcel(source);
                return info;
            }

            @Override
            public RunningAppProcessInfo[] newArray(int size) {
                return new RunningAppProcessInfo[size];
            }
        };
    }

    public static class MemoryInfo implements Parcelable {
        public long availMem;
        public long totalMem;
        public long threshold;
        public boolean lowMemory;
        public long freeMem;
        public long advertisedMem;

        @Override
        public int describeContents() {
            return 0;
        }

        public void readFromParcel(Parcel source) {
            availMem = source.readLong();
            totalMem = source.readLong();
            threshold = source.readLong();
            lowMemory = source.readInt() != 0;
            freeMem = source.readLong();
            advertisedMem = source.readLong();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(availMem);
            dest.writeLong(totalMem);
            dest.writeLong(threshold);
            dest.writeInt(lowMemory ? 1 : 0);
            dest.writeLong(freeMem);
            dest.writeLong(advertisedMem);
        }

        public static final Creator<MemoryInfo> CREATOR = new Creator<>() {
            @Override
            public MemoryInfo createFromParcel(Parcel source) {
                MemoryInfo mi = new MemoryInfo();
                mi.readFromParcel(source);
                return mi;
            }

            @Override
            public MemoryInfo[] newArray(int size) {
                return new MemoryInfo[size];
            }
        };
    }

    public List<RunningAppProcessInfo> getRunningAppProcesses() {
        String pkg = context != null && context.getPackageName() != null
                ? context.getPackageName()
                : ActivityThread.currentPackageName();
        RunningAppProcessInfo self = new RunningAppProcessInfo(pkg, Process.myPid(), new String[]{pkg});
        self.uid = Process.myUid();
        self.importance = RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
        List<RunningAppProcessInfo> list = new ArrayList<>(1);
        list.add(self);
        return list;
    }

    public void getMemoryInfo(MemoryInfo outInfo) {
        if (outInfo == null) return;
        Runtime rt = Runtime.getRuntime();
        long max = rt.maxMemory();
        long free = rt.freeMemory();
        long total = rt.totalMemory();
        long avail = Math.max(free, max - (total - free));
        outInfo.totalMem = Math.max(max, 4L * 1024 * 1024 * 1024);
        outInfo.availMem = Math.max(avail, 2L * 1024 * 1024 * 1024);
        outInfo.freeMem = outInfo.availMem;
        outInfo.advertisedMem = outInfo.totalMem;
        outInfo.threshold = 128L * 1024 * 1024;
        outInfo.lowMemory = false;
    }

    public int getMemoryClass() {
        return 256;
    }

    public int getLargeMemoryClass() {
        return 512;
    }

    public boolean isLowRamDevice() {
        return false;
    }

    public static boolean isUserAMonkey() {
        return false;
    }

    public static void getMyMemoryState(RunningAppProcessInfo outState) {
        if (outState == null) return;
        outState.pid = Process.myPid();
        outState.uid = Process.myUid();
        outState.processName = ActivityThread.currentProcessName();
        outState.importance = RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
    }

    public <T> List<T> getHistoricalProcessExitReasons(String packageName, int pid, int maxNum) {
        return java.util.Collections.emptyList();
    }
}
