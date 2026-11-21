package android.os;

import javax.swing.SwingUtilities;
import java.io.FileDescriptor;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hàng đợi thông điệp android.os.MessageQueue hỗ trợ IdleHandler thực thi trên Main Looper (Swing EDT)
 * và các Looper nền của AAOW.
 */
public final class MessageQueue {
    public interface IdleHandler {
        boolean queueIdle();
    }

    public interface OnFileDescriptorEventListener {
        int EVENT_INPUT = 1 << 0;
        int EVENT_OUTPUT = 1 << 1;
        int EVENT_ERROR = 1 << 2;

        int onFileDescriptorEvents(FileDescriptor fd, int events);
    }

    private final boolean mainQueue;
    private final List<IdleHandler> idleHandlers = new CopyOnWriteArrayList<>();

    MessageQueue(boolean mainQueue) {
        this.mainQueue = mainQueue;
    }

    public boolean isIdle() {
        return true;
    }

    public void addIdleHandler(IdleHandler handler) {
        if (handler == null) {
            throw new NullPointerException("Can't add a null IdleHandler");
        }
        if (!idleHandlers.contains(handler)) {
            idleHandlers.add(handler);
        }
        if (mainQueue) {
            SwingUtilities.invokeLater(this::dispatchIdleHandlersOnce);
        }
    }

    public void removeIdleHandler(IdleHandler handler) {
        if (handler != null) {
            idleHandlers.remove(handler);
        }
    }

    public void addOnFileDescriptorEventListener(FileDescriptor fd, int events,
                                                 OnFileDescriptorEventListener listener) {
    }

    public void removeOnFileDescriptorEventListener(FileDescriptor fd) {
    }

    void dispatchIdleHandlersOnce() {
        if (idleHandlers.isEmpty()) return;
        for (IdleHandler h : List.copyOf(idleHandlers)) {
            if (!idleHandlers.contains(h)) continue;
            try {
                boolean keep = h.queueIdle();
                if (!keep) {
                    idleHandlers.remove(h);
                }
            } catch (Throwable ignored) {
                idleHandlers.remove(h);
            }
        }
    }
}
