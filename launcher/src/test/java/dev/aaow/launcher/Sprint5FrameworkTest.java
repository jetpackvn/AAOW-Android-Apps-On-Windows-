package dev.aaow.launcher;

import android.app.Application;
import android.app.IntentService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.XmlResourceParser;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteTransactionListener;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.OperationCanceledException;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import dev.aaow.apk.ApkFile;
import dev.aaow.rt.AndroidRuntimeContext;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị toàn diện cho Sprint 5:
 * SQLiteDatabase/SQLiteOpenHelper/Cursor (SQLite JDBC), ContentResolver/ContentProvider/UriMatcher,
 * HandlerThread/Looper/AsyncTask, Service/IntentService/Binder/BroadcastReceiver,
 * ConnectivityManager và kiểm chứng Room trên APK thực tế.
 */
class Sprint5FrameworkTest {

    private static Path resolveRepoFile(String relative) {
        Path p = Path.of(relative);
        if (Files.exists(p)) return p;
        return Path.of("..").resolve(relative);
    }

    private static AndroidRuntimeContext createSampleRuntimeContext(Path tempDir) throws Exception {
        Path sampleApk = resolveRepoFile("samples/aaow-showcase.apk");
        try (ApkFile apk = new ApkFile(sampleApk)) {
            return new AndroidRuntimeContext(apk, Sprint5FrameworkTest.class.getClassLoader(), tempDir);
        }
    }

    @Test
    void sqliteOpenHelperCrudUpgradeTransactionsAndCursors(@TempDir Path tempDir) throws Exception {
        AndroidRuntimeContext ctx = createSampleRuntimeContext(tempDir);

        class NotesHelper extends SQLiteOpenHelper {
            boolean created;
            int upgradedFrom = -1;
            int upgradedTo = -1;

            NotesHelper(Context context, String name, int version) {
                super(context, name, null, version);
            }

            @Override
            public void onCreate(SQLiteDatabase db) {
                created = true;
                db.execSQL("CREATE TABLE notes (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, body TEXT, priority INTEGER, data BLOB)");
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                upgradedFrom = oldVersion;
                upgradedTo = newVersion;
                db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0");
            }
        }

        NotesHelper h1 = new NotesHelper(ctx, "aaow_test.db", 1);
        SQLiteDatabase db1 = h1.getWritableDatabase();
        assertTrue(h1.created);
        assertEquals(1, db1.getVersion());
        assertTrue(ctx.getDatabasePath("aaow_test.db").exists());
        assertTrue(Arrays.asList(ctx.databaseList()).contains("aaow_test.db"));

        ContentValues cv = new ContentValues();
        cv.put("title", "Sprint 5");
        cv.put("body", "SQLite JDBC");
        cv.put("priority", 10);
        cv.put("data", new byte[]{1, 2, 3, 4});
        long rowId = db1.insertOrThrow("notes", null, cv);
        assertEquals(1L, rowId);

        try (SQLiteStatement stmt = db1.compileStatement("INSERT INTO notes(title, body, priority) VALUES (?, ?, ?)")) {
            stmt.bindString(1, "Second Note");
            stmt.bindString(2, "With bind args");
            stmt.bindLong(3, 20L);
            assertEquals(2L, stmt.executeInsert());
        }

        assertEquals(2L, DatabaseUtils.queryNumEntries(db1, "notes"));

        // Kiểm tra transaction lồng nhau & listener rollback/commit
        AtomicInteger begins = new AtomicInteger();
        AtomicInteger commits = new AtomicInteger();
        AtomicInteger rollbacks = new AtomicInteger();
        SQLiteTransactionListener txListener = new SQLiteTransactionListener() {
            @Override public void onBegin() { begins.incrementAndGet(); }
            @Override public void onCommit() { commits.incrementAndGet(); }
            @Override public void onRollback() { rollbacks.incrementAndGet(); }
        };

        db1.beginTransactionWithListener(txListener);
        try {
            ContentValues tempRow = new ContentValues();
            tempRow.put("title", "Should Rollback");
            tempRow.put("priority", 99);
            db1.insertOrThrow("notes", null, tempRow);
            // Không gọi setTransactionSuccessful()
        } finally {
            db1.endTransaction();
        }
        assertEquals(1, rollbacks.get());
        assertEquals(2L, DatabaseUtils.queryNumEntries(db1, "notes"));

        db1.beginTransactionWithListener(txListener);
        try {
            ContentValues upd = new ContentValues();
            upd.put("priority", 30);
            int affected = db1.update("notes", upd, "title = ?", new String[]{"Second Note"});
            assertEquals(1, affected);
            db1.setTransactionSuccessful();
        } finally {
            db1.endTransaction();
        }
        assertEquals(1, commits.get());

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("notes");
        qb.appendWhere("priority >= 10");
        try (Cursor c = new CursorWrapper(qb.query(db1, new String[]{"_id", "title", "priority", "data"}, null, null, null, null, "_id ASC"))) {
            assertEquals(2, c.getCount());
            assertTrue(c.moveToFirst());
            assertEquals("Sprint 5", c.getString(c.getColumnIndexOrThrow("title")));
            assertEquals(10, c.getInt(c.getColumnIndexOrThrow("priority")));
            assertArrayEquals(new byte[]{1, 2, 3, 4}, c.getBlob(c.getColumnIndexOrThrow("data")));

            CharArrayBuffer cab = new CharArrayBuffer(8);
            c.copyStringToBuffer(1, cab);
            assertEquals("Sprint 5", new String(cab.data, 0, cab.sizeCopied));

            assertTrue(c.moveToNext());
            assertEquals("Second Note", c.getString(1));
            assertEquals(30, c.getInt(2));
            assertTrue(c.isNull(3));
        }

        CancellationSignal cancelled = new CancellationSignal();
        cancelled.cancel();
        assertThrows(OperationCanceledException.class,
                () -> db1.rawQuery("SELECT * FROM notes", null, cancelled));

        h1.close();

        // Kiểm tra nâng cấp schema v1 -> v2
        NotesHelper h2 = new NotesHelper(ctx, "aaow_test.db", 2);
        SQLiteDatabase db2 = h2.getWritableDatabase();
        assertFalse(h2.created);
        assertEquals(1, h2.upgradedFrom);
        assertEquals(2, h2.upgradedTo);
        assertEquals(2, db2.getVersion());
        h2.close();

        assertTrue(ctx.deleteDatabase("aaow_test.db"));
        assertFalse(ctx.getDatabasePath("aaow_test.db").exists());
    }

    @Test
    void contentResolverProviderUriMatcherAndEditorFileProvider(@TempDir Path tempDir) throws Exception {
        AndroidRuntimeContext ctx = createSampleRuntimeContext(tempDir);
        ContentResolver cr = ctx.getContentResolver();

        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI("dev.aaow.notes", "items", 1);
        matcher.addURI("dev.aaow.notes", "items/#", 2);
        assertEquals(1, matcher.match(Uri.parse("content://dev.aaow.notes/items")));
        assertEquals(2, matcher.match(ContentUris.withAppendedId(Uri.parse("content://dev.aaow.notes/items"), 42L)));
        assertEquals(42L, ContentUris.parseId(ContentUris.withAppendedId(Uri.parse("content://dev.aaow.notes/items"), 42L)));

        List<String> items = new ArrayList<>(List.of("Alpha"));
        cr.registerProvider("dev.aaow.notes", new ContentProvider() {
            @Override public boolean onCreate() { return true; }

            @Override
            public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
                MatrixCursor mc = new MatrixCursor(new String[]{"_id", "name"});
                for (int i = 0; i < items.size(); i++) {
                    mc.newRow().add((long) (i + 1)).add(items.get(i));
                }
                mc.setNotificationUri(getContext().getContentResolver(), uri);
                return mc;
            }

            @Override public String getType(Uri uri) { return "vnd.android.cursor.dir/items"; }

            @Override
            public Uri insert(Uri uri, ContentValues values) {
                items.add(values.getAsString("name"));
                Uri res = ContentUris.withAppendedId(uri, items.size());
                getContext().getContentResolver().notifyChange(uri, null);
                return res;
            }

            @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
            @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
        });

        Uri itemsUri = Uri.parse("content://dev.aaow.notes/items");
        CountDownLatch observerLatch = new CountDownLatch(1);
        ContentObserver observer = new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                if (itemsUri.equals(uri)) observerLatch.countDown();
            }
        };
        cr.registerContentObserver(itemsUri, true, observer);

        ContentValues insertVals = new ContentValues();
        insertVals.put("name", "Beta");
        Uri inserted = cr.insert(itemsUri, insertVals);
        assertEquals("content://dev.aaow.notes/items/2", inserted.toString());
        assertTrue(observerLatch.await(2, TimeUnit.SECONDS));

        try (Cursor c = cr.query(itemsUri, null, null, null, null)) {
            assertNotNull(c);
            assertEquals(2, c.getCount());
            assertTrue(c.moveToLast());
            assertEquals("Beta", c.getString(1));
        }

        // Kiểm tra đọc/ghi file:// + ParcelFileDescriptor
        File f = new File(ctx.getFilesDir(), "note.txt");
        Uri fileUri = Uri.fromFile(f);
        try (OutputStream out = cr.openOutputStream(fileUri, "w")) {
            out.write("Hello Windows".getBytes(StandardCharsets.UTF_8));
        }
        try (InputStream in = cr.openInputStream(fileUri)) {
            assertEquals("Hello Windows", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (ParcelFileDescriptor pfd = cr.openFileDescriptor(fileUri, "r")) {
            assertEquals("Hello Windows".length(), pfd.getStatSize());
        }

        // Kiểm tra resolveContentProvider + loadXmlMetaData trên APK thực tế (Editor FileProvider)
        Path editorApkPath = resolveRepoFile("build/benchmark-apks/org.billthefarmer.editor_198.apk");
        Assumptions.assumeTrue(Files.exists(editorApkPath));
        Path dexCache = resolveRepoFile("build/aaow-data/dex-cache");
        try (ApkFile editorApk = new ApkFile(editorApkPath)) {
            ClassLoader cl = DexLoader.loadApkDex(editorApk, dexCache, getClass().getClassLoader());
            try {
                AndroidRuntimeContext editorCtx = new AndroidRuntimeContext(editorApk, cl, tempDir.resolve("ed-fp"));
                ProviderInfo pi = editorCtx.getPackageManager().resolveContentProvider(
                        "org.billthefarmer.editor.fileprovider", PackageManager.GET_META_DATA);
                assertNotNull(pi);
                assertEquals("android.support.v4.content.FileProvider", pi.name);
                try (XmlResourceParser xml = pi.loadXmlMetaData(editorCtx.getPackageManager(), "android.support.FILE_PROVIDER_PATHS")) {
                    assertNotNull(xml);
                    int ev = xml.getEventType();
                    while (ev != XmlPullParser.START_TAG && ev != XmlPullParser.END_DOCUMENT) {
                        ev = xml.next();
                    }
                    assertEquals("paths", xml.getName());
                }
                ContentProvider fp = editorCtx.getContentResolver().acquireProvider("org.billthefarmer.editor.fileprovider");
                assertNotNull(fp);
            } finally {
                if (cl instanceof AutoCloseable ac) ac.close();
            }
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    void handlerThreadLooperAndAsyncTaskExecuteProperly() throws Exception {
        HandlerThread ht = new HandlerThread("Sprint5-Worker");
        ht.start();
        Looper bgLooper = ht.getLooper();
        assertNotNull(bgLooper);
        assertSame(ht, bgLooper.getThread());

        Handler bgHandler = new Handler(bgLooper);
        CountDownLatch workLatch = new CountDownLatch(2);
        AtomicReference<Thread> execThread = new AtomicReference<>();
        AtomicInteger msgWhat = new AtomicInteger();

        bgHandler.post(() -> {
            execThread.set(Thread.currentThread());
            workLatch.countDown();
        });
        AtomicBoolean cancelledRan = new AtomicBoolean(false);
        Runnable cancelledTask = () -> cancelledRan.set(true);
        bgHandler.postDelayed(cancelledTask, 50);
        bgHandler.removeCallbacks(cancelledTask);

        Handler msgHandler = new Handler(bgLooper) {
            @Override
            public void handleMessage(Message msg) {
                msgWhat.set(msg.what);
                workLatch.countDown();
            }
        };
        msgHandler.sendEmptyMessageDelayed(77, 15);

        assertTrue(workLatch.await(3, TimeUnit.SECONDS));
        assertSame(ht, execThread.get());
        assertEquals(77, msgWhat.get());
        assertFalse(cancelledRan.get());
        assertTrue(ht.quitSafely());
        ht.join(2000);

        // Kiểm tra AsyncTask
        CountDownLatch taskLatch = new CountDownLatch(1);
        AtomicBoolean bgRanOffMain = new AtomicBoolean();
        AtomicBoolean postRanOnMain = new AtomicBoolean();
        List<Integer> progressLog = new CopyOnWriteArrayList<>();

        AsyncTask<String, Integer, String> task = new AsyncTask<>() {
            @Override
            protected String doInBackground(String... params) {
                bgRanOffMain.set(!Looper.getMainLooper().isCurrentThread());
                publishProgress(50, 100);
                return "done:" + params[0];
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                progressLog.add(values[0]);
            }

            @Override
            protected void onPostExecute(String result) {
                postRanOnMain.set(Looper.getMainLooper().isCurrentThread());
                taskLatch.countDown();
            }
        };
        task.execute("aaow");
        assertEquals("done:aaow", task.get(3, TimeUnit.SECONDS));
        assertTrue(taskLatch.await(3, TimeUnit.SECONDS));
        assertTrue(bgRanOffMain.get());
        assertTrue(postRanOnMain.get());
        assertEquals(List.of(50), progressLog);
    }

    public static class EchoBinderService extends Service {
        static volatile boolean destroyed;
        static final List<String> commands = new CopyOnWriteArrayList<>();

        private final IBinder binder = new Binder("dev.aaow.IEcho") {
            @Override
            protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
                if (code == FIRST_CALL_TRANSACTION) {
                    String s = data.readString();
                    reply.writeNoException();
                    reply.writeString("echo:" + s);
                    return true;
                }
                return false;
            }
        };

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            if (intent != null && intent.getStringExtra("cmd") != null) {
                commands.add(intent.getStringExtra("cmd"));
            }
            return START_NOT_STICKY;
        }

        @Override
        public IBinder onBind(Intent intent) {
            return binder;
        }

        @Override
        public void onDestroy() {
            destroyed = true;
        }
    }

    public static class SampleIntentService extends IntentService {
        static final CountDownLatch handled = new CountDownLatch(1);
        static volatile String handledPayload;
        static volatile boolean ranOnBgThread;

        public SampleIntentService() {
            super("SampleIntentService");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            handledPayload = intent != null ? intent.getStringExtra("payload") : null;
            ranOnBgThread = !Looper.getMainLooper().isCurrentThread();
            handled.countDown();
        }
    }

    @Test
    void serviceIntentServiceBinderAndBroadcastReceiver(@TempDir Path tempDir) throws Exception {
        AndroidRuntimeContext ctx = createSampleRuntimeContext(tempDir);
        Application app = new Application();
        app.attach(ctx);
        ctx.setApplicationContext(app);

        EchoBinderService.destroyed = false;
        EchoBinderService.commands.clear();

        // startService + bindService + Binder.transact + unbindService + stopService
        Intent svcIntent = new Intent(ctx, EchoBinderService.class).putExtra("cmd", "boot");
        ComponentName cmp = ctx.startService(svcIntent);
        assertNotNull(cmp);
        assertEquals(List.of("boot"), EchoBinderService.commands);

        CountDownLatch boundLatch = new CountDownLatch(1);
        AtomicReference<IBinder> boundBinder = new AtomicReference<>();
        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                boundBinder.set(service);
                boundLatch.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        assertTrue(ctx.bindService(svcIntent, conn, Context.BIND_AUTO_CREATE));
        assertTrue(boundLatch.await(3, TimeUnit.SECONDS));
        assertNotNull(boundBinder.get());

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeString("sprint5");
        assertTrue(boundBinder.get().transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0));
        reply.readException();
        assertEquals("echo:sprint5", reply.readString());
        data.recycle();
        reply.recycle();

        ctx.unbindService(conn);
        assertFalse(EchoBinderService.destroyed, "Service đang started chưa bị destroy khi chỉ unbind");
        assertTrue(ctx.stopService(svcIntent));
        assertTrue(EchoBinderService.destroyed);

        // IntentService xử lý Intent trên HandlerThread riêng
        Intent isIntent = new Intent(ctx, SampleIntentService.class).putExtra("payload", "job42");
        ctx.startService(isIntent);
        assertTrue(SampleIntentService.handled.await(3, TimeUnit.SECONDS));
        assertEquals("job42", SampleIntentService.handledPayload);
        assertTrue(SampleIntentService.ranOnBgThread);

        // BroadcastReceiver + IntentFilter + Sticky + Ordered
        List<String> received = new ArrayList<>();
        BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                received.add(intent.getAction() + ":" + intent.getStringExtra("msg"));
            }
        };
        IntentFilter filter = new IntentFilter("dev.aaow.ACTION_PING");
        filter.addCategory("dev.aaow.CAT_TEST");
        ctx.registerReceiver(br, filter);

        Intent ping = new Intent("dev.aaow.ACTION_PING").addCategory("dev.aaow.CAT_TEST").putExtra("msg", "ok");
        ctx.sendStickyBroadcast(ping);
        assertEquals(List.of("dev.aaow.ACTION_PING:ok"), received);

        Intent sticky = ctx.registerReceiver(new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {}
        }, new IntentFilter("dev.aaow.ACTION_PING"));
        assertNotNull(sticky);
        assertEquals("ok", sticky.getStringExtra("msg"));
        ctx.unregisterReceiver(br);
    }

    @Test
    void connectivityManagerAndRealPfNotesRoomDatabase(@TempDir Path tempDir) throws Exception {
        AndroidRuntimeContext ctx = createSampleRuntimeContext(tempDir);
        ConnectivityManager cm = ctx.getSystemService(ConnectivityManager.class);
        assertNotNull(cm);
        assertSame(cm, ctx.getSystemService(Context.CONNECTIVITY_SERVICE));
        assertFalse(cm.isActiveNetworkMetered());
        assertEquals(ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED, cm.getRestrictBackgroundStatus());

        Network active = cm.getActiveNetwork();
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (active != null) {
            assertNotNull(info);
            assertTrue(info.isConnected());
            NetworkCapabilities caps = cm.getNetworkCapabilities(active);
            assertNotNull(caps);
            assertTrue(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
            assertTrue(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));

            AtomicBoolean cbAvailable = new AtomicBoolean(false);
            ConnectivityManager.NetworkCallback cb = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    cbAvailable.set(true);
                }
            };
            cm.registerDefaultNetworkCallback(cb);
            assertTrue(cbAvailable.get());
            cm.unregisterNetworkCallback(cb);
        }

        // Kiểm chứng Room + SQLiteDatabase chạy thật trên APK org.secuso.privacyfriendlynotes_105.apk
        Path pfNotesPath = resolveRepoFile("build/benchmark-apks/org.secuso.privacyfriendlynotes_105.apk");
        Assumptions.assumeTrue(Files.exists(pfNotesPath));
        Path dexCache = resolveRepoFile("build/aaow-data/dex-cache");
        try (ApkFile apk = new ApkFile(pfNotesPath)) {
            ClassLoader cl = DexLoader.loadApkDex(apk, dexCache, getClass().getClassLoader());
            Object roomDb = null;
            try {
                AndroidRuntimeContext pfCtx = new AndroidRuntimeContext(apk, cl, tempDir.resolve("pf-data"));
                Class<?> dbCls = Class.forName("org.secuso.privacyfriendlynotes.room.NoteDatabase", true, cl);
                roomDb = dbCls.getMethod("getInstance", Context.class).invoke(null, pfCtx);
                assertNotNull(roomDb);

                Object categoryDao = dbCls.getMethod("categoryDao").invoke(roomDb);
                Object noteDao = dbCls.getMethod("noteDao").invoke(roomDb);

                Class<?> catCls = Class.forName("org.secuso.privacyfriendlynotes.room.model.Category", true, cl);
                Object cat = catCls.getDeclaredConstructor(String.class, String.class).newInstance("AAOW", "#1976D2");
                Method insertCat = Arrays.stream(categoryDao.getClass().getMethods())
                        .filter(m -> m.getName().equals("insert") && m.getParameterCount() == 1)
                        .findFirst().orElseThrow();
                insertCat.invoke(categoryDao, cat);

                File dbFile = pfCtx.getDatabasePath("allthenotes");
                assertTrue(dbFile.exists() && dbFile.length() > 0, "Room phải tạo file allthenotes thật trên đĩa");

                // Đọc kiểm tra trực tiếp bằng SQLiteDatabase của AAOW
                try (SQLiteDatabase verifyDb = SQLiteDatabase.openDatabase(
                        dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
                     Cursor c = verifyDb.rawQuery("SELECT name, color FROM categories", null)) {
                    assertEquals(1, c.getCount());
                    assertTrue(c.moveToFirst());
                    assertEquals("AAOW", c.getString(0));
                    assertEquals("#1976D2", c.getString(1));
                }
                assertNotNull(noteDao);
            } finally {
                if (roomDb != null) {
                    try {
                        roomDb.getClass().getMethod("close").invoke(roomDb);
                    } catch (Throwable ignored) {
                    }
                }
                if (cl instanceof AutoCloseable ac) ac.close();
            }
        }
    }
}
