package dev.aaow.rt.jni;

import dev.aaow.rt.NativeBridge;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bộ giả lập cầu nối JNI cho các wrapper SQLite native đóng gói trong APK (WCDB, Requery, sqlite-android),
 * ánh xạ trực tiếp sang sqlite-jdbc chạy trên Windows.
 */
public final class SqliteJniHandler implements NativeJniHandler {
    private final AtomicLong nextConn = new AtomicLong(0x80_0000L);
    private final Map<Long, Connection> connections = new ConcurrentHashMap<>();

    @Override
    public boolean supports(String className) {
        return className.endsWith(".SQLiteConnection")
                && (className.startsWith("com.tencent.wcdb.")
                || className.startsWith("io.requery.android.database.sqlite.")
                || className.startsWith("org.sqlite.database.sqlite."));
    }

    @Override
    public Object invoke(String methodSig, String className, String methodName, String descriptor, Object self, Object[] args) {
        return switch (methodName) {
            case "nativeOpen" -> {
                String path = (args != null && args.length >= 1 && args[0] instanceof String s) ? s : ":memory:";
                String jdbc = ":memory:".equals(path) ? "jdbc:sqlite::memory:" : "jdbc:sqlite:" + path;
                try {
                    Connection c = DriverManager.getConnection(jdbc);
                    long h = nextConn.getAndIncrement();
                    connections.put(h, c);
                    yield h;
                } catch (Exception e) {
                    yield 0L;
                }
            }
            case "nativeClose" -> {
                if (args != null && args.length >= 1 && args[0] instanceof Number n) {
                    Connection c = connections.remove(n.longValue());
                    if (c != null) {
                        try {
                            c.close();
                        } catch (Exception ignored) {
                        }
                    }
                }
                yield null;
            }
            case "nativeExecute" -> {
                if (args != null && args.length >= 3 && args[0] instanceof Number n && args[2] instanceof String sql) {
                    Connection c = connections.get(n.longValue());
                    if (c != null) {
                        try (Statement st = c.createStatement()) {
                            st.execute(sql);
                        } catch (Exception ignored) {
                        }
                    }
                }
                yield null;
            }
            default -> NativeBridge.UNHANDLED;
        };
    }
}
