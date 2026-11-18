package android.os;

import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/** Cầu nối FileDescriptor của Android tới tệp thật trên Windows. */
public class ParcelFileDescriptor implements Closeable, Parcelable {
    public static final int MODE_WORLD_READABLE = 0x00000001;
    public static final int MODE_WORLD_WRITEABLE = 0x00000002;
    public static final int MODE_READ_ONLY = 0x10000000;
    public static final int MODE_WRITE_ONLY = 0x20000000;
    public static final int MODE_READ_WRITE = 0x30000000;
    public static final int MODE_CREATE = 0x08000000;
    public static final int MODE_TRUNCATE = 0x04000000;
    public static final int MODE_APPEND = 0x02000000;

    private final File file;
    private final int mode;
    private final FileDescriptor fd;
    private final RandomAccessFile raf;
    private volatile boolean closed;

    private ParcelFileDescriptor(File file, int mode, RandomAccessFile raf, FileDescriptor fd) {
        this.file = file;
        this.mode = mode;
        this.raf = raf;
        this.fd = fd;
    }

    public ParcelFileDescriptor(ParcelFileDescriptor wrapped) {
        this.file = wrapped != null ? wrapped.file : null;
        this.mode = wrapped != null ? wrapped.mode : MODE_READ_ONLY;
        this.raf = wrapped != null ? wrapped.raf : null;
        this.fd = wrapped != null ? wrapped.fd : new FileDescriptor();
    }

    public static ParcelFileDescriptor open(File file, int mode) throws FileNotFoundException {
        if (file == null) throw new FileNotFoundException("Null file");
        boolean writable = (mode & MODE_WRITE_ONLY) != 0 || (mode & MODE_READ_WRITE) == MODE_READ_WRITE;
        if ((mode & MODE_CREATE) != 0 || writable) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
        }
        if (!writable && !file.exists()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }
        try {
            if ((mode & MODE_CREATE) != 0 && !file.exists()) {
                file.createNewFile();
            }
            if ((mode & MODE_TRUNCATE) != 0 && file.exists()) {
                try (FileOutputStream fos = new FileOutputStream(file, false)) {
                    // truncate
                }
            }
            RandomAccessFile r = new RandomAccessFile(file, writable ? "rw" : "r");
            if ((mode & MODE_APPEND) != 0) {
                r.seek(r.length());
            }
            return new ParcelFileDescriptor(file, mode, r, r.getFD());
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            FileNotFoundException fnf = new FileNotFoundException(e.getMessage());
            fnf.initCause(e);
            throw fnf;
        }
    }

    public static int parseMode(String mode) {
        if (mode == null) return MODE_READ_ONLY;
        return switch (mode) {
            case "r" -> MODE_READ_ONLY;
            case "w", "wt" -> MODE_WRITE_ONLY | MODE_CREATE | MODE_TRUNCATE;
            case "wa" -> MODE_WRITE_ONLY | MODE_CREATE | MODE_APPEND;
            case "rw" -> MODE_READ_WRITE | MODE_CREATE;
            case "rwt" -> MODE_READ_WRITE | MODE_CREATE | MODE_TRUNCATE;
            default -> throw new IllegalArgumentException("Bad mode '" + mode + "'");
        };
    }

    public static ParcelFileDescriptor dup(FileDescriptor orig) throws IOException {
        return new ParcelFileDescriptor(null, MODE_READ_WRITE, null, orig);
    }

    public FileDescriptor getFileDescriptor() {
        return fd != null ? fd : new FileDescriptor();
    }

    public File getFile() {
        return file;
    }

    public int getFd() {
        return -1;
    }

    public int detachFd() {
        return -1;
    }

    public long getStatSize() {
        if (file != null && file.exists()) {
            return file.length();
        }
        try {
            return raf != null ? raf.length() : -1;
        } catch (IOException e) {
            return -1;
        }
    }

    public ParcelFileDescriptor dup() throws IOException {
        if (file != null) {
            return open(file, mode & ~(MODE_TRUNCATE));
        }
        return new ParcelFileDescriptor(this);
    }

    public boolean canDetectErrors() {
        return true;
    }

    public void checkError() throws IOException {
    }

    public void closeWithError(String msg) throws IOException {
        close();
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        if (raf != null) {
            raf.close();
        }
    }

    @Override
    public int describeContents() {
        return Parcelable.CONTENTS_FILE_DESCRIPTOR;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(file != null ? file.getAbsolutePath() : null);
        out.writeInt(mode);
    }

    public static final Creator<ParcelFileDescriptor> CREATOR = new Creator<>() {
        @Override
        public ParcelFileDescriptor createFromParcel(Parcel in) {
            String path = in.readString();
            int mode = in.readInt();
            if (path == null) return new ParcelFileDescriptor((ParcelFileDescriptor) null);
            try {
                return open(new File(path), mode);
            } catch (FileNotFoundException e) {
                return new ParcelFileDescriptor(new File(path), mode, null, new FileDescriptor());
            }
        }

        @Override
        public ParcelFileDescriptor[] newArray(int size) {
            return new ParcelFileDescriptor[size];
        }
    };

    public static class AutoCloseInputStream extends FileInputStream {
        private final ParcelFileDescriptor pfd;

        public AutoCloseInputStream(ParcelFileDescriptor pfd) {
            super(pfd.getFileDescriptor());
            this.pfd = pfd;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                if (pfd != null) pfd.close();
            }
        }
    }

    public static class AutoCloseOutputStream extends FileOutputStream {
        private final ParcelFileDescriptor pfd;

        public AutoCloseOutputStream(ParcelFileDescriptor pfd) {
            super(pfd.getFileDescriptor());
            this.pfd = pfd;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                if (pfd != null) pfd.close();
            }
        }
    }
}
