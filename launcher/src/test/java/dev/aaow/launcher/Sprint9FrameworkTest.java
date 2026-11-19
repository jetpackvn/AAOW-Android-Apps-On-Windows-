package dev.aaow.launcher;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.Parcel;
import android.os.SystemProperties;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.util.Log;
import android.util.LogPrinter;
import android.util.LruCache;
import android.view.Choreographer;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import com.googlecode.d2j.DexConstants;
import com.googlecode.d2j.Field;
import com.googlecode.d2j.Method;
import com.googlecode.d2j.dex.Dex2jar;
import com.googlecode.d2j.node.DexClassNode;
import com.googlecode.d2j.node.DexCodeNode;
import com.googlecode.d2j.node.DexFileNode;
import com.googlecode.d2j.node.DexMethodNode;
import com.googlecode.d2j.node.insn.ConstStmtNode;
import com.googlecode.d2j.node.insn.FieldStmtNode;
import com.googlecode.d2j.node.insn.MethodStmtNode;
import com.googlecode.d2j.node.insn.Stmt0RNode;
import com.googlecode.d2j.node.insn.Stmt1RNode;
import com.googlecode.d2j.node.insn.TypeStmtNode;
import com.googlecode.d2j.reader.BaseDexFileReader;
import com.googlecode.d2j.reader.Op;
import com.googlecode.d2j.visitors.DexFileVisitor;
import dev.aaow.apk.ApkFile;
import dev.aaow.rt.AndroidRuntimeContext;
import dev.aaow.rt.HostWindow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị toàn diện cho Sprint 9 (Phase A — Phá rào VerifyError & Core Runtime):
 * 1. DexR8Sanitizer sửa R8 Constructor Inlining (cả lớp không có ctor, chuỗi nhiều tầng, và trùng chữ ký bằng R8CtorMarker).
 * 2. Khởi chạy sạch hoàn toàn APK thực tế Kotlin/R8/AndroidX (com.jarsilio.android.scrambledeggsif_88.apk) không còn VerifyError.
 * 3. LruCache, Base64InputStream/Base64OutputStream, LogPrinter.
 * 4. Choreographer (60fps FrameCallback & VsyncCallback thật).
 * 5. SystemProperties, Debug, Bundle/Intent/Parcel API 33, PackageManager API 33 Flags, TableLayout/TableRow & Async requestPermissions.
 */
class Sprint9FrameworkTest {

    @Test
    void dexR8SanitizerFixesInlinedConstructorsAndMultiLevelChain() throws Exception {
        // Dựng DexFileNode mô phỏng R8 Constructor Inlining:
        // - SubNoCtor extends java/lang/Object (0 constructor, có field value)
        // - ChildNoCtor extends SubNoCtor (0 constructor)
        // - SubWithExistingCtor extends java/lang/Object (đã có <init>()V riêng đặt tag="existing", nhưng caller gọi thẳng Object.<init>())
        // - Factory tạo cả 3 bằng lệnh new-instance + invoke-direct Object.<init>()V
        DexFileNode fileNode = new DexFileNode();
        fileNode.clzs = new ArrayList<>();

        DexClassNode subNoCtor = new DexClassNode(DexConstants.ACC_PUBLIC, "Ltest/r8/SubNoCtor;", "Ljava/lang/Object;", null);
        subNoCtor.fields = new ArrayList<>();
        subNoCtor.visitField(DexConstants.ACC_PUBLIC, new Field("Ltest/r8/SubNoCtor;", "tag", "Ljava/lang/String;"), null);
        subNoCtor.methods = new ArrayList<>();
        fileNode.clzs.add(subNoCtor);

        DexClassNode childNoCtor = new DexClassNode(DexConstants.ACC_PUBLIC, "Ltest/r8/ChildNoCtor;", "Ltest/r8/SubNoCtor;", null);
        childNoCtor.methods = new ArrayList<>();
        fileNode.clzs.add(childNoCtor);

        DexClassNode subExisting = new DexClassNode(DexConstants.ACC_PUBLIC, "Ltest/r8/SubExisting;", "Ljava/lang/Object;", null);
        subExisting.fields = new ArrayList<>();
        subExisting.visitField(DexConstants.ACC_PUBLIC, new Field("Ltest/r8/SubExisting;", "mode", "Ljava/lang/String;"), null);
        subExisting.methods = new ArrayList<>();
        DexMethodNode existingInit = new DexMethodNode(DexConstants.ACC_PUBLIC | DexConstants.ACC_CONSTRUCTOR,
                new Method("Ltest/r8/SubExisting;", "<init>", new String[0], "V"));
        existingInit.codeNode = new DexCodeNode();
        existingInit.codeNode.totalRegister = 2; // v0=local, v1=this
        existingInit.codeNode.stmts = new ArrayList<>(List.of(
                new MethodStmtNode(Op.INVOKE_DIRECT, new int[]{1}, new Method("Ljava/lang/Object;", "<init>", new String[0], "V")),
                new ConstStmtNode(Op.CONST_STRING, 0, "from-existing"),
                new FieldStmtNode(Op.IPUT_OBJECT, 0, 1, new Field("Ltest/r8/SubExisting;", "mode", "Ljava/lang/String;")),
                new Stmt0RNode(Op.RETURN_VOID)
        ));
        subExisting.methods.add(existingInit);
        fileNode.clzs.add(subExisting);

        DexClassNode factory = new DexClassNode(DexConstants.ACC_PUBLIC, "Ltest/r8/Factory;", "Ljava/lang/Object;", null);
        factory.methods = new ArrayList<>();

        // Method 1: static SubNoCtor makeChild() { ChildNoCtor c = new ChildNoCtor(); [R8 gọi Object.<init>]; c.tag = "ok"; return c; }
        DexMethodNode makeChild = new DexMethodNode(DexConstants.ACC_PUBLIC | DexConstants.ACC_STATIC,
                new Method("Ltest/r8/Factory;", "makeChild", new String[0], "Ltest/r8/SubNoCtor;"));
        makeChild.codeNode = new DexCodeNode();
        makeChild.codeNode.totalRegister = 2;
        makeChild.codeNode.stmts = new ArrayList<>(List.of(
                new TypeStmtNode(Op.NEW_INSTANCE, 0, 0, "Ltest/r8/ChildNoCtor;"),
                new MethodStmtNode(Op.INVOKE_DIRECT, new int[]{0}, new Method("Ljava/lang/Object;", "<init>", new String[0], "V")),
                new ConstStmtNode(Op.CONST_STRING, 1, "child-ok"),
                new FieldStmtNode(Op.IPUT_OBJECT, 1, 0, new Field("Ltest/r8/SubNoCtor;", "tag", "Ljava/lang/String;")),
                new Stmt1RNode(Op.RETURN_OBJECT, 0)
        ));
        factory.methods.add(makeChild);

        // Method 2: static SubExisting makeBypassed() { SubExisting s = new SubExisting(); [R8 gọi Object.<init>]; return s; }
        DexMethodNode makeBypassed = new DexMethodNode(DexConstants.ACC_PUBLIC | DexConstants.ACC_STATIC,
                new Method("Ltest/r8/Factory;", "makeBypassed", new String[0], "Ltest/r8/SubExisting;"));
        makeBypassed.codeNode = new DexCodeNode();
        makeBypassed.codeNode.totalRegister = 1;
        makeBypassed.codeNode.stmts = new ArrayList<>(List.of(
                new TypeStmtNode(Op.NEW_INSTANCE, 0, 0, "Ltest/r8/SubExisting;"),
                new MethodStmtNode(Op.INVOKE_DIRECT, new int[]{0}, new Method("Ljava/lang/Object;", "<init>", new String[0], "V")),
                new Stmt1RNode(Op.RETURN_OBJECT, 0)
        ));
        factory.methods.add(makeBypassed);
        fileNode.clzs.add(factory);

        // Chạy DexR8Sanitizer
        DexR8Sanitizer.SanitizeStats stats = DexR8Sanitizer.sanitizeInPlace(fileNode);
        assertEquals(2, stats.rewrittenCallSites(), "Phải sửa cả 2 điểm gọi constructor bị R8 inline");
        assertEquals(3, stats.synthesizedConstructors(), "Phải tổng hợp bridge ctor cho SubNoCtor, ChildNoCtor và SubExisting(R8CtorMarker)");

        // Dịch qua Dex2jar -> bytecode JVM thật -> nạp vào JVM & chạy thực thi để kiểm chứng 0 VerifyError và đúng kiểu!
        BaseDexFileReader reader = new BaseDexFileReader() {
            @Override public int getDexVersion() { return DexConstants.DEX_035; }
            @Override public List<String> getClassNames() { return Collections.emptyList(); }
            @Override public void accept(DexFileVisitor dv) { fileNode.accept(dv); }
            @Override public void accept(DexFileVisitor dv, int config) { fileNode.accept(dv); }
            @Override public void accept(DexFileVisitor dv, int classIdx, int config) { fileNode.clzs.get(classIdx).accept(dv); }
        };
        ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        Dex2jar.from(reader).reUseReg(false).topoLogicalSort().skipDebug(false).optimizeSynchronized(false).doTranslate(rawOut);

        Map<String, byte[]> classes = new HashMap<>();
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray()))) {
            while (dis.available() > 0) {
                int nameLen = dis.readInt();
                String internal = new String(dis.readNBytes(nameLen), StandardCharsets.UTF_8);
                if (internal.endsWith(".class")) internal = internal.substring(0, internal.length() - 6);
                int bodyLen = dis.readInt();
                byte[] bytes = DexLoader.rewriteApkClassForWindows(dis.readNBytes(bodyLen));
                classes.put(internal.replace('/', '.'), bytes);
            }
        }

        ClassLoader memLoader = new ClassLoader(getClass().getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] b = classes.get(name);
                if (b == null) throw new ClassNotFoundException(name);
                return defineClass(name, b, 0, b.length);
            }
        };

        Class<?> factoryCls = Class.forName("test.r8.Factory", true, memLoader);
        Object childObj = factoryCls.getMethod("makeChild").invoke(null);
        assertEquals("test.r8.ChildNoCtor", childObj.getClass().getName());
        assertEquals("child-ok", childObj.getClass().getField("tag").get(childObj));

        Object bypassedObj = factoryCls.getMethod("makeBypassed").invoke(null);
        assertEquals("test.r8.SubExisting", bypassedObj.getClass().getName());
        assertNull(bypassedObj.getClass().getField("mode").get(bypassedObj),
                "Bridge constructor dùng R8CtorMarker phải đi thẳng lên Object.<init> thay vì chạy ctor cũ");

        // Constructor gốc của SubExisting vẫn giữ nguyên hành vi
        Object normalObj = Class.forName("test.r8.SubExisting", true, memLoader).getDeclaredConstructor().newInstance();
        assertEquals("from-existing", normalObj.getClass().getField("mode").get(normalObj));
    }

    @Test
    void scrambledExifBootsCleanlyWithoutVerifyError(@TempDir Path tempDir) throws Exception {
        Path apkPath = resolveRepoFile("build/benchmark-apks/com.jarsilio.android.scrambledeggsif_88.apk");
        if (!Files.exists(apkPath)) {
            return; // Bỏ qua nếu không có bộ APK benchmark ngoài
        }

        Path outPng = tempDir.resolve("scrambled-s9.png");
        ClassLoader cl = null;
        try (ApkFile apk = new ApkFile(apkPath)) {
            cl = DexLoader.loadApkDex(apk, tempDir.resolve("dex-cache"), getClass().getClassLoader());

            // Kiểm chứng trực tiếp 2 class trước đây bị VerifyError nay nạp sạch hoàn toàn
            assertNotNull(Class.forName("androidx.startup.InitializationProvider", false, cl));
            assertNotNull(Class.forName("androidx.activity.ComponentActivity", false, cl));
            assertNotNull(Class.forName("com.jarsilio.android.scrambledeggsif.MainActivity", false, cl));

            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, cl, tempDir.resolve("data"));
            List<String> appWarnings = new ArrayList<>();
            Application app = AppRepository.createAndInitializeApplication(apk.getManifest(), cl, ctx, appWarnings::add);
            assertNotNull(app);

            HostWindow host = new HostWindow(ctx, app);
            host.renderHeadlessScreenshot(apk.getManifest().launcherActivity, outPng.toFile(), 420, 820);

            assertTrue(Files.isRegularFile(outPng));
            assertTrue(Files.size(outPng) > 10_000, "Ảnh chụp giao diện thật Scrambled Exif phải có nội dung đầy đủ");
            assertNotNull(host.getCurrentActivity());
            assertEquals("com.jarsilio.android.scrambledeggsif.MainActivity",
                    host.getCurrentActivity().getClass().getName());
        } finally {
            if (cl instanceof Closeable c) c.close();
        }
    }

    @Test
    void lruCacheBase64StreamsAndLogPrinterWork() throws Exception {
        // 1. LruCache
        AtomicInteger evictedCount = new AtomicInteger();
        LruCache<String, String> cache = new LruCache<>(10) {
            @Override
            protected int sizeOf(String key, String value) {
                return value.length();
            }

            @Override
            protected String create(String key) {
                return "auto".equals(key) ? "1234" : null;
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, String oldValue, String newValue) {
                if (evicted) evictedCount.incrementAndGet();
            }
        };

        cache.put("a", "12345"); // size=5
        cache.put("b", "67890"); // size=10
        assertEquals("12345", cache.get("a")); // touch 'a' -> 'b' trở thành LRU
        cache.put("c", "xy"); // size=12 -> evict 'b' -> size=7
        assertNull(cache.get("b"));
        assertEquals("12345", cache.get("a"));
        assertEquals("xy", cache.get("c"));
        assertEquals(1, evictedCount.get());
        assertEquals("1234", cache.get("auto")); // create() + evict 'c'
        assertTrue(cache.hitCount() >= 2);
        assertTrue(cache.createCount() == 1);
        assertFalse(cache.snapshot().isEmpty());
        cache.evictAll();
        assertEquals(0, cache.size());

        // 2. Base64OutputStream & Base64InputStream (kể cả NO_CLOSE)
        String secret = "Xin chào AAOW Sprint 9 — Direct JVM on Windows!";
        AtomicBoolean underlyingClosed = new AtomicBoolean(false);
        ByteArrayOutputStream encodedOut = new ByteArrayOutputStream() {
            @Override
            public void close() {
                underlyingClosed.set(true);
            }
        };
        try (Base64OutputStream b64Out = new Base64OutputStream(encodedOut, Base64.NO_WRAP | Base64.URL_SAFE | Base64.NO_CLOSE)) {
            b64Out.write(secret.getBytes(StandardCharsets.UTF_8));
        }
        assertFalse(underlyingClosed.get(), "Cờ NO_CLOSE không được đóng OutputStream nền");
        byte[] encodedBytes = encodedOut.toByteArray();
        assertTrue(encodedBytes.length > 0);

        ByteArrayOutputStream decodedOut = new ByteArrayOutputStream();
        try (Base64InputStream b64In = new Base64InputStream(new ByteArrayInputStream(encodedBytes), Base64.URL_SAFE)) {
            byte[] buf = new byte[16];
            int r;
            while ((r = b64In.read(buf)) != -1) {
                decodedOut.write(buf, 0, r);
            }
        }
        assertEquals(secret, decodedOut.toString(StandardCharsets.UTF_8));

        // 3. LogPrinter
        LogPrinter lp = new LogPrinter(Log.DEBUG, "Sprint9Test");
        assertDoesNotThrow(() -> lp.println("Kiểm thử LogPrinter"));
    }

    @Test
    void choreographerDispatchesFrameAndVsyncCallbacks() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        AtomicLong frameTime = new AtomicLong(0);
        AtomicReference<Choreographer.FrameData> vsyncData = new AtomicReference<>();
        AtomicBoolean cancelledRan = new AtomicBoolean(false);

        Choreographer.FrameCallback cancelledCb = t -> cancelledRan.set(true);
        Choreographer choreographer = Choreographer.getInstance();
        choreographer.postFrameCallback(cancelledCb);
        choreographer.removeFrameCallback(cancelledCb);

        choreographer.postFrameCallback(t -> {
            frameTime.set(t);
            latch.countDown();
        });
        choreographer.postVsyncCallback(data -> {
            vsyncData.set(data);
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Choreographer phải phát nhịp FrameCallback & VsyncCallback thật");
        assertTrue(frameTime.get() > 0L);
        assertNotNull(vsyncData.get());
        assertNotNull(vsyncData.get().getPreferredFrameTimeline());
        assertTrue(vsyncData.get().getPreferredFrameTimeline().getVsyncId() > 0L);
        assertFalse(cancelledRan.get(), "Callback đã removeFrameCallback không được chạy");
    }

    @Test
    void systemPropertiesDebugApi33BundlePackageManagerTableLayoutAndAsyncPermissionsWork(@TempDir Path tempDir) throws Exception {
        // 1. SystemProperties
        assertEquals(35, SystemProperties.getInt("ro.build.version.sdk", 0));
        assertFalse(SystemProperties.get("ro.product.cpu.abi", "").isEmpty());
        SystemProperties.set("aaow.test.flag", "true");
        assertTrue(SystemProperties.getBoolean("aaow.test.flag", false));
        SystemProperties.set("aaow.test.num", "42");
        assertEquals(42L, SystemProperties.getLong("aaow.test.num", 0L));

        // 2. Debug & Debug.MemoryInfo
        assertFalse(Debug.isDebuggerConnected());
        assertFalse(Debug.waitingForDebugger());
        Debug.MemoryInfo mi = new Debug.MemoryInfo();
        Debug.getMemoryInfo(mi);
        assertTrue(mi.getTotalPss() >= 0);
        Parcel p = Parcel.obtain();
        mi.writeToParcel(p, 0);
        p.setDataPosition(0);
        Debug.MemoryInfo restored = Debug.MemoryInfo.CREATOR.createFromParcel(p);
        assertEquals(mi.dalvikPss, restored.dalvikPss);
        p.recycle();

        // 3. Bundle & Intent API 33 generic getters
        ComponentName cn = new ComponentName("dev.aaow.test", "dev.aaow.test.Main");
        Bundle b = new Bundle();
        b.putParcelable("cmp", cn);
        b.putSerializable("ser", "hello-api33");
        assertEquals(cn, b.getParcelable("cmp", ComponentName.class));
        assertNull(b.getParcelable("cmp", Intent.class));
        assertEquals("hello-api33", b.getSerializable("ser", String.class));

        Intent intent = new Intent("dev.aaow.ACTION");
        intent.putExtra("cmp", cn);
        intent.putExtra("ser", 12345);
        assertEquals(cn, intent.getParcelableExtra("cmp", ComponentName.class));
        assertEquals(12345, intent.getSerializableExtra("ser", Integer.class));

        // 4. PackageManager API 33 Flags + TableLayout/TableRow + Async requestPermissions
        Path showcaseApk = resolveRepoFile("samples/aaow-showcase.apk");
        try (ApkFile apk = new ApkFile(showcaseApk)) {
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, getClass().getClassLoader(), tempDir);
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(apk.getManifest().packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES));
            assertNotNull(pi);
            assertEquals(apk.getManifest().packageName, pi.packageName);
            assertNotNull(pm.getApplicationInfo(apk.getManifest().packageName,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA)));

            TableLayout table = new TableLayout(ctx);
            TableRow row = new TableRow(ctx);
            View cell = new View(ctx);
            cell.setId(9901);
            row.addView(cell);
            table.addView(row);
            assertSame(cell, table.findViewById(9901), "TableLayout/TableRow thật phải giữ cây View con cho findViewById");

            CountDownLatch permLatch = new CountDownLatch(1);
            AtomicBoolean invokedSync = new AtomicBoolean(false);
            AtomicInteger grantedResult = new AtomicInteger(-99);
            Activity act = new Activity() {
                @Override
                public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
                    if (requestCode == 77 && grantResults.length == 1) {
                        grantedResult.set(grantResults[0]);
                        permLatch.countDown();
                    }
                }
            };
            act.attach(ctx, new Application(), new Intent(), "Test", () -> {}, () -> {});
            act.requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, 77);
            invokedSync.set(permLatch.getCount() == 0);
            assertFalse(invokedSync.get(), "requestPermissions phải gửi kết quả bất đồng bộ qua Main Looper");
            assertTrue(permLatch.await(2, TimeUnit.SECONDS));
            assertEquals(PackageManager.PERMISSION_GRANTED, grantedResult.get());
            assertFalse(act.shouldShowRequestPermissionRationale("android.permission.READ_EXTERNAL_STORAGE"));
        }
    }

    private static Path resolveRepoFile(String relativePath) {
        Path direct = Path.of(relativePath);
        if (Files.exists(direct)) return direct;
        Path parent = Path.of("..", relativePath);
        if (Files.exists(parent)) return parent;
        return direct;
    }
}
