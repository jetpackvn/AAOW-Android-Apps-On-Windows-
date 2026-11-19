package android.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cài đặt android.net.TrafficStats an toàn cho OkHttp, Retrofit, Glide, Coil, Volley và ExoPlayer.
 * Hỗ trợ gắn thẻ (tag) và UID theo luồng (ThreadLocal) cùng bộ đếm lưu lượng mạng.
 */
public class TrafficStats {
    public static final int UNSUPPORTED = -1;

    private static final ThreadLocal<Integer> THREAD_TAG = ThreadLocal.withInitial(() -> UNSUPPORTED);
    private static final ThreadLocal<Integer> THREAD_UID = ThreadLocal.withInitial(() -> UNSUPPORTED);
    private static final AtomicLong TOTAL_RX_BYTES = new AtomicLong(1024L * 1024L);
    private static final AtomicLong TOTAL_TX_BYTES = new AtomicLong(256L * 1024L);
    private static final AtomicLong TOTAL_RX_PACKETS = new AtomicLong(1024L);
    private static final AtomicLong TOTAL_TX_PACKETS = new AtomicLong(512L);
    private static final AtomicLong OPERATION_COUNT = new AtomicLong(0L);

    public TrafficStats() {
    }

    public static void setThreadStatsTag(int tag) {
        THREAD_TAG.set(tag);
    }

    public static int getAndSetThreadStatsTag(int tag) {
        int prev = THREAD_TAG.get();
        THREAD_TAG.set(tag);
        return prev;
    }

    public static int getThreadStatsTag() {
        return THREAD_TAG.get();
    }

    public static void clearThreadStatsTag() {
        THREAD_TAG.set(UNSUPPORTED);
    }

    public static void setThreadStatsUid(int uid) {
        THREAD_UID.set(uid);
    }

    public static int getThreadStatsUid() {
        return THREAD_UID.get();
    }

    public static void clearThreadStatsUid() {
        THREAD_UID.set(UNSUPPORTED);
    }

    public static void tagSocket(Socket socket) throws SocketException {
        TOTAL_TX_PACKETS.incrementAndGet();
    }

    public static void untagSocket(Socket socket) throws SocketException {
    }

    public static void tagDatagramSocket(DatagramSocket socket) throws SocketException {
        TOTAL_TX_PACKETS.incrementAndGet();
    }

    public static void untagDatagramSocket(DatagramSocket socket) throws SocketException {
    }

    public static void tagFileDescriptor(FileDescriptor fd) throws IOException {
    }

    public static void untagFileDescriptor(FileDescriptor fd) throws IOException {
    }

    public static void incrementOperationCount(int operationCount) {
        OPERATION_COUNT.addAndGet(Math.max(0, operationCount));
    }

    public static void incrementOperationCount(int tag, int operationCount) {
        OPERATION_COUNT.addAndGet(Math.max(0, operationCount));
    }

    public static void recordTransferredBytes(long rxBytes, long txBytes) {
        if (rxBytes > 0) {
            TOTAL_RX_BYTES.addAndGet(rxBytes);
            TOTAL_RX_PACKETS.incrementAndGet();
        }
        if (txBytes > 0) {
            TOTAL_TX_BYTES.addAndGet(txBytes);
            TOTAL_TX_PACKETS.incrementAndGet();
        }
    }

    public static long getMobileTxPackets() {
        return 0L;
    }

    public static long getMobileRxPackets() {
        return 0L;
    }

    public static long getMobileTxBytes() {
        return 0L;
    }

    public static long getMobileRxBytes() {
        return 0L;
    }

    public static long getTxPackets(String iface) {
        return TOTAL_TX_PACKETS.get();
    }

    public static long getRxPackets(String iface) {
        return TOTAL_RX_PACKETS.get();
    }

    public static long getTxBytes(String iface) {
        return TOTAL_TX_BYTES.get();
    }

    public static long getRxBytes(String iface) {
        return TOTAL_RX_BYTES.get();
    }

    public static long getTotalTxPackets() {
        return TOTAL_TX_PACKETS.get();
    }

    public static long getTotalRxPackets() {
        return TOTAL_RX_PACKETS.get();
    }

    public static long getTotalTxBytes() {
        return TOTAL_TX_BYTES.get();
    }

    public static long getTotalRxBytes() {
        return TOTAL_RX_BYTES.get();
    }

    public static long getUidTxBytes(int uid) {
        return TOTAL_TX_BYTES.get();
    }

    public static long getUidRxBytes(int uid) {
        return TOTAL_RX_BYTES.get();
    }

    public static long getUidTxPackets(int uid) {
        return TOTAL_TX_PACKETS.get();
    }

    public static long getUidRxPackets(int uid) {
        return TOTAL_RX_PACKETS.get();
    }

    public static long getUidTcpTxBytes(int uid) {
        return TOTAL_TX_BYTES.get();
    }

    public static long getUidTcpRxBytes(int uid) {
        return TOTAL_RX_BYTES.get();
    }

    public static long getUidUdpTxBytes(int uid) {
        return 0L;
    }

    public static long getUidUdpRxBytes(int uid) {
        return 0L;
    }

    public static long getUidTcpTxSegments(int uid) {
        return TOTAL_TX_PACKETS.get();
    }

    public static long getUidTcpRxSegments(int uid) {
        return TOTAL_RX_PACKETS.get();
    }

    public static long getUidUdpTxPackets(int uid) {
        return 0L;
    }

    public static long getUidUdpRxPackets(int uid) {
        return 0L;
    }
}
