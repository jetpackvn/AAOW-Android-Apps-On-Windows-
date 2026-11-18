package android.app.job;

import android.content.ClipData;
import android.content.ComponentName;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class JobInfo implements Parcelable {
    public static final int BACKOFF_POLICY_LINEAR = 0;
    public static final int BACKOFF_POLICY_EXPONENTIAL = 1;
    public static final long DEFAULT_INITIAL_BACKOFF_MILLIS = 30000L;
    public static final long MAX_BACKOFF_DELAY_MILLIS = 18000000L;
    public static final int NETWORK_BYTES_UNKNOWN = -1;
    public static final int NETWORK_TYPE_NONE = 0;
    public static final int NETWORK_TYPE_ANY = 1;
    public static final int NETWORK_TYPE_UNMETERED = 2;
    public static final int NETWORK_TYPE_NOT_ROAMING = 3;
    public static final int NETWORK_TYPE_CELLULAR = 4;
    public static final int NETWORK_TYPE_METERED = 4;
    public static final int PRIORITY_MIN = 100;
    public static final int PRIORITY_LOW = 200;
    public static final int PRIORITY_DEFAULT = 300;
    public static final int PRIORITY_HIGH = 400;
    public static final int PRIORITY_MAX = 500;

    public static final class TriggerContentUri implements Parcelable {
        public static final int FLAG_NOTIFY_FOR_DESCENDANTS = 1;
        private final Uri uri;
        private final int flags;

        public TriggerContentUri(Uri uri, int flags) {
            this.uri = uri;
            this.flags = flags;
        }

        public Uri getUri() {
            return uri;
        }

        public int getFlags() {
            return flags;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int parcelFlags) {
            dest.writeParcelable(uri, parcelFlags);
            dest.writeInt(flags);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TriggerContentUri other)) return false;
            return flags == other.flags && Objects.equals(uri, other.uri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri, flags);
        }

        public static final Creator<TriggerContentUri> CREATOR = new Creator<>() {
            @Override
            public TriggerContentUri createFromParcel(Parcel in) {
                Uri uri = in.readParcelable(Uri.class.getClassLoader());
                int flags = in.readInt();
                return new TriggerContentUri(uri, flags);
            }

            @Override
            public TriggerContentUri[] newArray(int size) {
                return new TriggerContentUri[size];
            }
        };
    }

    private final int jobId;
    private final ComponentName service;
    private final PersistableBundle extras;
    private final Bundle transientExtras;
    private final ClipData clipData;
    private final int clipGrantFlags;
    private final int networkType;
    private final NetworkRequest requiredNetwork;
    private final long minLatencyMillis;
    private final long maxExecutionDelayMillis;
    private final long intervalMillis;
    private final long flexMillis;
    private final boolean isPeriodic;
    private final boolean isPersisted;
    private final boolean isPrefetch;
    private final boolean isExpedited;
    private final boolean isImportantWhileForeground;
    private final boolean isUserInitiated;
    private final boolean requireCharging;
    private final boolean requireDeviceIdle;
    private final boolean requireBatteryNotLow;
    private final boolean requireStorageNotLow;
    private final long initialBackoffMillis;
    private final int backoffPolicy;
    private final int priority;
    private final String traceTag;
    private final Set<String> debugTags;
    private final long estimatedDownloadBytes;
    private final long estimatedUploadBytes;
    private final long minimumChunkBytes;
    private final long triggerContentUpdateDelay;
    private final long triggerContentMaxDelay;
    private final TriggerContentUri[] triggerContentUris;

    JobInfo() {
        this(new Builder(0, null));
    }

    private JobInfo(Builder b) {
        this.jobId = b.jobId;
        this.service = b.service;
        this.extras = b.extras != null ? b.extras : new PersistableBundle();
        this.transientExtras = b.transientExtras != null ? b.transientExtras : new Bundle();
        this.clipData = b.clipData;
        this.clipGrantFlags = b.clipGrantFlags;
        this.networkType = b.networkType;
        this.requiredNetwork = b.requiredNetwork;
        this.minLatencyMillis = b.minLatencyMillis;
        this.maxExecutionDelayMillis = b.maxExecutionDelayMillis;
        this.intervalMillis = b.intervalMillis;
        this.flexMillis = b.flexMillis;
        this.isPeriodic = b.isPeriodic;
        this.isPersisted = b.isPersisted;
        this.isPrefetch = b.isPrefetch;
        this.isExpedited = b.isExpedited;
        this.isImportantWhileForeground = b.isImportantWhileForeground;
        this.isUserInitiated = b.isUserInitiated;
        this.requireCharging = b.requireCharging;
        this.requireDeviceIdle = b.requireDeviceIdle;
        this.requireBatteryNotLow = b.requireBatteryNotLow;
        this.requireStorageNotLow = b.requireStorageNotLow;
        this.initialBackoffMillis = b.initialBackoffMillis;
        this.backoffPolicy = b.backoffPolicy;
        this.priority = b.priority;
        this.traceTag = b.traceTag;
        this.debugTags = Collections.unmodifiableSet(new LinkedHashSet<>(b.debugTags));
        this.estimatedDownloadBytes = b.estimatedDownloadBytes;
        this.estimatedUploadBytes = b.estimatedUploadBytes;
        this.minimumChunkBytes = b.minimumChunkBytes;
        this.triggerContentUpdateDelay = b.triggerContentUpdateDelay;
        this.triggerContentMaxDelay = b.triggerContentMaxDelay;
        this.triggerContentUris = b.triggerContentUris.isEmpty()
                ? null
                : b.triggerContentUris.toArray(new TriggerContentUri[0]);
    }

    public static long getMinPeriodMillis() {
        return 15 * 60 * 1000L;
    }

    public static long getMinFlexMillis() {
        return 5 * 60 * 1000L;
    }

    public int getId() { return jobId; }
    public ComponentName getService() { return service; }
    public PersistableBundle getExtras() { return extras; }
    public Bundle getTransientExtras() { return transientExtras; }
    public ClipData getClipData() { return clipData; }
    public int getClipGrantFlags() { return clipGrantFlags; }
    public int getNetworkType() { return networkType; }
    public NetworkRequest getRequiredNetwork() { return requiredNetwork; }
    public long getMinLatencyMillis() { return minLatencyMillis; }
    public long getMaxExecutionDelayMillis() { return maxExecutionDelayMillis; }
    public long getIntervalMillis() { return intervalMillis; }
    public long getFlexMillis() { return flexMillis; }
    public boolean isPeriodic() { return isPeriodic; }
    public boolean isPersisted() { return isPersisted; }
    public boolean isPrefetch() { return isPrefetch; }
    public boolean isExpedited() { return isExpedited; }
    public boolean isImportantWhileForeground() { return isImportantWhileForeground; }
    public boolean isUserInitiated() { return isUserInitiated; }
    public boolean isRequireCharging() { return requireCharging; }
    public boolean isRequireDeviceIdle() { return requireDeviceIdle; }
    public boolean isRequireBatteryNotLow() { return requireBatteryNotLow; }
    public boolean isRequireStorageNotLow() { return requireStorageNotLow; }
    public long getInitialBackoffMillis() { return initialBackoffMillis; }
    public int getBackoffPolicy() { return backoffPolicy; }
    public int getPriority() { return priority; }
    public String getTraceTag() { return traceTag; }
    public Set<String> getDebugTags() { return debugTags; }
    public long getEstimatedNetworkDownloadBytes() { return estimatedDownloadBytes; }
    public long getEstimatedNetworkUploadBytes() { return estimatedUploadBytes; }
    public long getMinimumNetworkChunkBytes() { return minimumChunkBytes; }
    public long getTriggerContentUpdateDelay() { return triggerContentUpdateDelay; }
    public long getTriggerContentMaxDelay() { return triggerContentMaxDelay; }
    public TriggerContentUri[] getTriggerContentUris() { return triggerContentUris; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(jobId);
        dest.writeParcelable(service, flags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobInfo other)) return false;
        return jobId == other.jobId && Objects.equals(service, other.service);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, service);
    }

    @Override
    public String toString() {
        return "(job:" + jobId + "/" + service + ")";
    }

    public static final Creator<JobInfo> CREATOR = new Creator<>() {
        @Override
        public JobInfo createFromParcel(Parcel in) {
            int id = in.readInt();
            ComponentName cmp = in.readParcelable(ComponentName.class.getClassLoader());
            return new Builder(id, cmp).build();
        }

        @Override
        public JobInfo[] newArray(int size) {
            return new JobInfo[size];
        }
    };

    public static final class Builder {
        private final int jobId;
        private final ComponentName service;
        private PersistableBundle extras = new PersistableBundle();
        private Bundle transientExtras = new Bundle();
        private ClipData clipData;
        private int clipGrantFlags;
        private int networkType = NETWORK_TYPE_NONE;
        private NetworkRequest requiredNetwork;
        private long minLatencyMillis;
        private long maxExecutionDelayMillis;
        private long intervalMillis;
        private long flexMillis;
        private boolean isPeriodic;
        private boolean isPersisted;
        private boolean isPrefetch;
        private boolean isExpedited;
        private boolean isImportantWhileForeground;
        private boolean isUserInitiated;
        private boolean requireCharging;
        private boolean requireDeviceIdle;
        private boolean requireBatteryNotLow;
        private boolean requireStorageNotLow;
        private long initialBackoffMillis = DEFAULT_INITIAL_BACKOFF_MILLIS;
        private int backoffPolicy = BACKOFF_POLICY_EXPONENTIAL;
        private int priority = PRIORITY_DEFAULT;
        private String traceTag;
        private final Set<String> debugTags = new LinkedHashSet<>();
        private long estimatedDownloadBytes = NETWORK_BYTES_UNKNOWN;
        private long estimatedUploadBytes = NETWORK_BYTES_UNKNOWN;
        private long minimumChunkBytes = NETWORK_BYTES_UNKNOWN;
        private long triggerContentUpdateDelay = -1L;
        private long triggerContentMaxDelay = -1L;
        private final List<TriggerContentUri> triggerContentUris = new ArrayList<>();

        public Builder(int jobId, ComponentName jobService) {
            this.jobId = jobId;
            this.service = jobService;
        }

        public Builder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder setExtras(PersistableBundle extras) {
            this.extras = extras != null ? extras : new PersistableBundle();
            return this;
        }

        public Builder setTransientExtras(Bundle transientExtras) {
            this.transientExtras = transientExtras != null ? transientExtras : new Bundle();
            return this;
        }

        public Builder setClipData(ClipData clip, int grantFlags) {
            this.clipData = clip;
            this.clipGrantFlags = grantFlags;
            return this;
        }

        public Builder setRequiredNetworkType(int networkType) {
            this.networkType = networkType;
            return this;
        }

        public Builder setRequiredNetwork(NetworkRequest networkRequest) {
            this.requiredNetwork = networkRequest;
            return this;
        }

        public Builder setEstimatedNetworkBytes(long downloadBytes, long uploadBytes) {
            this.estimatedDownloadBytes = downloadBytes;
            this.estimatedUploadBytes = uploadBytes;
            return this;
        }

        public Builder setMinimumNetworkChunkBytes(long chunkSizeBytes) {
            this.minimumChunkBytes = chunkSizeBytes;
            return this;
        }

        public Builder setRequiresCharging(boolean requiresCharging) {
            this.requireCharging = requiresCharging;
            return this;
        }

        public Builder setRequiresDeviceIdle(boolean requiresDeviceIdle) {
            this.requireDeviceIdle = requiresDeviceIdle;
            return this;
        }

        public Builder setRequiresBatteryNotLow(boolean batteryNotLow) {
            this.requireBatteryNotLow = batteryNotLow;
            return this;
        }

        public Builder setRequiresStorageNotLow(boolean storageNotLow) {
            this.requireStorageNotLow = storageNotLow;
            return this;
        }

        public Builder addTriggerContentUri(TriggerContentUri uri) {
            if (uri != null) triggerContentUris.add(uri);
            return this;
        }

        public Builder setTriggerContentUpdateDelay(long durationMs) {
            this.triggerContentUpdateDelay = durationMs;
            return this;
        }

        public Builder setTriggerContentMaxDelay(long durationMs) {
            this.triggerContentMaxDelay = durationMs;
            return this;
        }

        public Builder setPeriodic(long intervalMillis) {
            return setPeriodic(intervalMillis, intervalMillis);
        }

        public Builder setPeriodic(long intervalMillis, long flexMillis) {
            this.isPeriodic = true;
            this.intervalMillis = intervalMillis;
            this.flexMillis = flexMillis;
            return this;
        }

        public Builder setMinimumLatency(long minLatencyMillis) {
            this.minLatencyMillis = minLatencyMillis;
            return this;
        }

        public Builder setOverrideDeadline(long maxExecutionDelayMillis) {
            this.maxExecutionDelayMillis = maxExecutionDelayMillis;
            return this;
        }

        public Builder setBackoffCriteria(long initialBackoffMillis, int backoffPolicy) {
            this.initialBackoffMillis = initialBackoffMillis;
            this.backoffPolicy = backoffPolicy;
            return this;
        }

        public Builder setExpedited(boolean expedited) {
            this.isExpedited = expedited;
            return this;
        }

        public Builder setUserInitiated(boolean userInitiated) {
            this.isUserInitiated = userInitiated;
            return this;
        }

        public Builder setImportantWhileForeground(boolean importantWhileForeground) {
            this.isImportantWhileForeground = importantWhileForeground;
            return this;
        }

        public Builder setPrefetch(boolean prefetch) {
            this.isPrefetch = prefetch;
            return this;
        }

        public Builder setPersisted(boolean isPersisted) {
            this.isPersisted = isPersisted;
            return this;
        }

        public Builder setTraceTag(String traceTag) {
            this.traceTag = traceTag;
            return this;
        }

        public Builder addDebugTag(String debugTag) {
            if (debugTag != null) debugTags.add(debugTag);
            return this;
        }

        public Builder removeDebugTag(String debugTag) {
            debugTags.remove(debugTag);
            return this;
        }

        public JobInfo build() {
            return new JobInfo(this);
        }
    }
}
