package android.content.res;

import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class AssetFileDescriptor implements Closeable, Parcelable {
    public static final long UNKNOWN_LENGTH = -1;

    private final ParcelFileDescriptor fd;
    private final long startOffset;
    private final long length;
    private final Bundle extras;

    public AssetFileDescriptor(ParcelFileDescriptor fd, long startOffset, long length) {
        this(fd, startOffset, length, null);
    }

    public AssetFileDescriptor(ParcelFileDescriptor fd, long startOffset, long length, Bundle extras) {
        this.fd = fd;
        this.startOffset = startOffset;
        this.length = length;
        this.extras = extras;
    }

    public ParcelFileDescriptor getParcelFileDescriptor() {
        return fd;
    }

    public FileDescriptor getFileDescriptor() {
        return fd != null ? fd.getFileDescriptor() : null;
    }

    public long getStartOffset() {
        return startOffset;
    }

    public long getLength() {
        if (length >= 0) return length;
        return fd != null ? fd.getStatSize() : UNKNOWN_LENGTH;
    }

    public long getDeclaredLength() {
        return length;
    }

    public Bundle getExtras() {
        return extras;
    }

    public FileInputStream createInputStream() throws IOException {
        return new ParcelFileDescriptor.AutoCloseInputStream(fd);
    }

    public FileOutputStream createOutputStream() throws IOException {
        return new ParcelFileDescriptor.AutoCloseOutputStream(fd);
    }

    @Override
    public void close() throws IOException {
        if (fd != null) fd.close();
    }

    @Override
    public int describeContents() {
        return fd != null ? fd.describeContents() : 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(fd, flags);
        out.writeLong(startOffset);
        out.writeLong(length);
        out.writeBundle(extras);
    }

    public static final Creator<AssetFileDescriptor> CREATOR = new Creator<>() {
        @Override
        public AssetFileDescriptor createFromParcel(Parcel in) {
            ParcelFileDescriptor fd = in.readParcelable(null);
            long start = in.readLong();
            long len = in.readLong();
            Bundle ex = in.readBundle();
            return new AssetFileDescriptor(fd, start, len, ex);
        }

        @Override
        public AssetFileDescriptor[] newArray(int size) {
            return new AssetFileDescriptor[size];
        }
    };
}
