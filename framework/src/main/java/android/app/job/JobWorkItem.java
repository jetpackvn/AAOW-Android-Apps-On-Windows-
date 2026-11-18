package android.app.job;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;

public final class JobWorkItem implements Parcelable {
    private final Intent intent;
    private final long networkDownloadBytes;
    private final long networkUploadBytes;
    private final long minimumChunkBytes;
    private final PersistableBundle extras = new PersistableBundle();
    private int deliveryCount = 1;

    public JobWorkItem(Intent intent) {
        this(intent, JobInfo.NETWORK_BYTES_UNKNOWN, JobInfo.NETWORK_BYTES_UNKNOWN, JobInfo.NETWORK_BYTES_UNKNOWN);
    }

    public JobWorkItem(Intent intent, long downloadBytes, long uploadBytes) {
        this(intent, downloadBytes, uploadBytes, JobInfo.NETWORK_BYTES_UNKNOWN);
    }

    public JobWorkItem(Intent intent, long downloadBytes, long uploadBytes, long minimumChunkBytes) {
        this.intent = intent;
        this.networkDownloadBytes = downloadBytes;
        this.networkUploadBytes = uploadBytes;
        this.minimumChunkBytes = minimumChunkBytes;
    }

    public Intent getIntent() {
        return intent;
    }

    public long getEstimatedNetworkDownloadBytes() {
        return networkDownloadBytes;
    }

    public long getEstimatedNetworkUploadBytes() {
        return networkUploadBytes;
    }

    public long getMinimumNetworkChunkBytes() {
        return minimumChunkBytes;
    }

    public PersistableBundle getExtras() {
        return extras;
    }

    public int getDeliveryCount() {
        return deliveryCount;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(intent, flags);
        dest.writeLong(networkDownloadBytes);
        dest.writeLong(networkUploadBytes);
        dest.writeLong(minimumChunkBytes);
    }

    @Override
    public String toString() {
        return "JobWorkItem{intent=" + intent + ", deliveryCount=" + deliveryCount + '}';
    }

    public static final Creator<JobWorkItem> CREATOR = new Creator<>() {
        @Override
        public JobWorkItem createFromParcel(Parcel in) {
            Intent intent = in.readParcelable(Intent.class.getClassLoader());
            long dl = in.readLong();
            long ul = in.readLong();
            long chunk = in.readLong();
            return new JobWorkItem(intent, dl, ul, chunk);
        }

        @Override
        public JobWorkItem[] newArray(int size) {
            return new JobWorkItem[size];
        }
    };
}
