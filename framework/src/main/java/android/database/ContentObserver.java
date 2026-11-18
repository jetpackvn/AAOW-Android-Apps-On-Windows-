package android.database;

import android.net.Uri;
import android.os.Handler;

import java.util.Collection;

/** Theo dõi thay đổi dữ liệu trên ContentResolver hoặc Cursor. */
public abstract class ContentObserver {
    private final Handler handler;

    public ContentObserver(Handler handler) {
        this.handler = handler;
    }

    public boolean deliverSelfNotifications() {
        return false;
    }

    public void onChange(boolean selfChange) {
    }

    public void onChange(boolean selfChange, Uri uri) {
        onChange(selfChange);
    }

    public void onChange(boolean selfChange, Uri uri, int flags) {
        onChange(selfChange, uri);
    }

    public void onChange(boolean selfChange, Collection<Uri> uris, int flags) {
        if (uris == null || uris.isEmpty()) {
            onChange(selfChange, (Uri) null, flags);
            return;
        }
        for (Uri u : uris) {
            onChange(selfChange, u, flags);
        }
    }

    public final void dispatchChange(boolean selfChange) {
        dispatchChange(selfChange, null);
    }

    public final void dispatchChange(boolean selfChange, Uri uri) {
        dispatchChange(selfChange, uri, 0);
    }

    public final void dispatchChange(boolean selfChange, Uri uri, int flags) {
        if (handler == null) {
            onChange(selfChange, uri, flags);
        } else {
            handler.post(() -> onChange(selfChange, uri, flags));
        }
    }

    public final void dispatchChange(boolean selfChange, Collection<Uri> uris, int flags) {
        if (handler == null) {
            onChange(selfChange, uris, flags);
        } else {
            handler.post(() -> onChange(selfChange, uris, flags));
        }
    }
}
