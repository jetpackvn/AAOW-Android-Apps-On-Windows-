package dev.aaow.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import dev.aaow.apk.ApkFile;
import dev.aaow.rt.AndroidRuntimeContext;
import dev.aaow.rt.HostWindow;
import dev.aaow.rt.SimpleMenu;
import dev.aaow.rt.WindowsFilePickerBridge;
import dev.aaow.rt.WindowsShareBridge;
import dev.aaow.rt.compat.CompatDrawerLayout;
import dev.aaow.rt.compat.CompatRecyclerView;
import dev.aaow.rt.compat.CompatTextInputLayout;
import dev.aaow.rt.compat.CompatViewPager;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị và tương tác đầu-cuối (E2E) cho Sprint 10 (Phase B — Offline Apps):
 * 1. android.media.ExifInterface + Bitmap.compress(JPEG) đọc/ghi/xóa EXIF thật.
 * 2. ColorStateList theo trạng thái, PorterDuffColorFilter, Dialog overlay chung trên HostWindow,
 *    CompatViewPager / CompatRecyclerView / CompatDrawerLayout / CompatTextInputLayout & nạp thư mục APK.
 * 3. Tương tác E2E trên Editor & Notes (nhập liệu, lưu đĩa, mở file, chuyển sửa/xem Markdown WebView).
 * 4. Tương tác E2E trên Bubble Level (đóng dialog, nghiêng cảm biến, mở Settings) & Scrambled Exif
 *    (đóng MaterialAlertDialog, xóa EXIF thật qua HandleImageActivity + FileProvider + WindowsShareBridge).
 * 5. Tương tác E2E trên Privacy Friendly Notes (Splash -> Tutorial ViewPager Next/Skip -> MainActivity
 *    DrawerLayout -> FAB + -> TextNoteActivity -> lưu Room SQLite -> hiện RecyclerView -> bấm mở lại).
 */
class Sprint10FrameworkTest {

    @Test
    void exifInterfaceAndBitmapJpegRoundTripWorks(@TempDir Path tempDir) throws Exception {
        File jpg = tempDir.resolve("sample-exif.jpg").toFile();
        Bitmap bmp = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF1976D2);
        canvas.drawRect(0, 0, 64, 48, p);

        try (FileOutputStream fos = new FileOutputStream(jpg)) {
            assertTrue(bmp.compress(Bitmap.CompressFormat.JPEG, 92, fos));
        }
        assertTrue(jpg.length() > 100);
        Bitmap decoded = BitmapFactory.decodeFile(jpg.getAbsolutePath());
        assertNotNull(decoded);
        assertEquals(64, decoded.getWidth());
        assertEquals(48, decoded.getHeight());

        ExifInterface exif = new ExifInterface(jpg);
        exif.setAttribute(ExifInterface.TAG_MAKE, "AAOW-Camera");
        exif.setAttribute(ExifInterface.TAG_MODEL, "AAOW-Win11");
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "21/1,1/1,42/1");
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "105/1,51/1,15/1");
        exif.saveAttributes();

        ExifInterface reloaded = new ExifInterface(jpg);
        assertEquals("AAOW-Camera", reloaded.getAttribute(ExifInterface.TAG_MAKE));
        assertEquals("AAOW-Win11", reloaded.getAttribute(ExifInterface.TAG_MODEL));
        assertEquals(ExifInterface.ORIENTATION_ROTATE_90,
                reloaded.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
        float[] latLong = new float[2];
        assertTrue(reloaded.getLatLong(latLong));
        assertEquals(21.0283f, latLong[0], 0.01f);
        assertEquals(105.8541f, latLong[1], 0.01f);

        // Xóa EXIF và kiểm tra ảnh JPEG vẫn đọc lại được nguyên vẹn
        reloaded.setAttribute(ExifInterface.TAG_MAKE, null);
        reloaded.setAttribute(ExifInterface.TAG_MODEL, null);
        reloaded.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null);
        reloaded.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, null);
        reloaded.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null);
        reloaded.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, null);
        reloaded.saveAttributes();

        try (InputStream in = Files.newInputStream(jpg.toPath())) {
            ExifInterface stripped = new ExifInterface(in);
            assertNull(stripped.getAttribute(ExifInterface.TAG_MAKE));
            assertNull(stripped.getAttribute(ExifInterface.TAG_MODEL));
            assertNull(stripped.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
        }
        Bitmap afterStrip = BitmapFactory.decodeFile(jpg.getAbsolutePath());
        assertNotNull(afterStrip);
        assertEquals(64, afterStrip.getWidth());
    }

    @Test
    void colorStateListDialogOverlayCompatWidgetsAndBatchImportWork(@TempDir Path tempDir) throws Exception {
        // 1. ColorStateList & PorterDuffColorFilter
        ColorStateList csl = new ColorStateList(
                new int[][]{
                        new int[]{0x010100a7}, // state_pressed
                        new int[]{-0x0101009e}, // -state_enabled
                        new int[0]
                },
                new int[]{0xFFFF0000, 0xFF9E9E9E, 0xFF1565C0}
        );
        assertTrue(csl.isStateful());
        assertEquals(0xFFFF0000, csl.getColorForState(new int[]{0x0101009e, 0x010100a7}, 0));
        assertEquals(0xFF9E9E9E, csl.getColorForState(new int[0], 0));
        assertEquals(0xFF1565C0, csl.getColorForState(new int[]{0x0101009e}, 0));

        int tinted = Canvas.applyColorFilter(0xFF000000, new PorterDuffColorFilter(0xFF024265, PorterDuff.Mode.SRC_IN));
        assertEquals(0xFF024265, tinted);

        // 2. Uri.Builder luôn chèn '/' giữa authority và path
        Uri builtUri = new Uri.Builder()
                .scheme("content")
                .authority("com.example.fileprovider")
                .encodedPath("shared_images/clean.jpg")
                .build();
        assertEquals("content://com.example.fileprovider/shared_images/clean.jpg", builtUri.toString());

        // 3. CompatViewPager, CompatRecyclerView, CompatDrawerLayout, CompatTextInputLayout
        Path showcaseApk = resolveRepoFile("samples/aaow-showcase.apk");
        try (ApkFile apk = new ApkFile(showcaseApk)) {
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, getClass().getClassLoader(), tempDir.resolve("ctx"));

            CompatViewPager pager = new CompatViewPager(ctx);
            pager.addView(new TextView(ctx));
            pager.addView(new TextView(ctx));
            AtomicInteger selectedPage = new AtomicInteger(-1);
            pager.addOnPageChangeListener(selectedPage::set);
            pager.setCurrentItem(1);
            assertEquals(1, pager.getCurrentItem());
            assertEquals(1, selectedPage.get());

            CompatRecyclerView rv = new CompatRecyclerView(ctx);
            List<String> items = new ArrayList<>(List.of("Note A", "Note B"));
            AtomicInteger clickedPos = new AtomicInteger(-1);
            rv.setOnItemClickListener((parent, view, pos) -> clickedPos.set(pos));
            CompatRecyclerView.Adapter<CompatRecyclerView.ViewHolder> adapter = new CompatRecyclerView.Adapter<>() {
                @Override
                public CompatRecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    return new CompatRecyclerView.ViewHolder(new TextView(ctx)) {};
                }

                @Override
                public void onBindViewHolder(CompatRecyclerView.ViewHolder holder, int position) {
                    ((TextView) holder.itemView).setText(items.get(position));
                }

                @Override
                public int getItemCount() {
                    return items.size();
                }
            };
            rv.setAdapter(adapter);
            assertEquals(2, rv.getChildCount());
            rv.getChildAt(1).performClick();
            assertEquals(1, clickedPos.get());
            items.add("Note C");
            adapter.notifyItemInserted(2);
            assertEquals(3, rv.getChildCount());

            CompatDrawerLayout drawerLayout = new CompatDrawerLayout(ctx);
            drawerLayout.addView(new LinearLayout(ctx));
            View navDrawer = new LinearLayout(ctx);
            drawerLayout.addView(navDrawer);
            AtomicBoolean openedCb = new AtomicBoolean();
            AtomicBoolean closedCb = new AtomicBoolean();
            drawerLayout.addDrawerListener(new CompatDrawerLayout.DrawerListener() {
                @Override
                public void onDrawerOpened(View drawerView) {
                    openedCb.set(true);
                }

                @Override
                public void onDrawerClosed(View drawerView) {
                    closedCb.set(true);
                }
            });
            assertFalse(drawerLayout.isDrawerOpen(navDrawer));
            drawerLayout.openDrawer(navDrawer);
            assertTrue(drawerLayout.isDrawerOpen(navDrawer));
            assertTrue(openedCb.get());
            drawerLayout.closeDrawers();
            assertFalse(drawerLayout.isDrawerOpen(navDrawer));
            assertTrue(closedCb.get());

            CompatTextInputLayout til = new CompatTextInputLayout(ctx);
            til.setHint("Title");
            EditText innerEt = new EditText(ctx);
            til.addView(innerEt);
            til.setError("Required");
            assertSame(innerEt, til.getEditText());
            assertEquals("Title", til.getHint().toString());
            assertEquals("Required", til.getError().toString());

            // 4. Universal Dialog overlay trên HostWindow (cả AlertDialog và Dialog tùy biến)
            Application app = new Application();
            app.attach(ctx);
            HostWindow host = new HostWindow(ctx, app);
            host.launchHeadless(apk.getManifest().launcherActivity, 420, 800);
            AtomicBoolean customClicked = new AtomicBoolean(false);
            SwingUtilities.invokeAndWait(() -> {
                Dialog customDlg = new Dialog(host.getCurrentActivity());
                Button ok = new Button(host.getCurrentActivity());
                ok.setText("Confirm");
                ok.setOnClickListener(v -> {
                    customClicked.set(true);
                    customDlg.dismiss();
                });
                customDlg.setContentView(ok);
                customDlg.show();
            });
            host.pumpEventsAndLayout(100);
            assertNotNull(host.getActiveDialog());
            View dlgBtn = findFirstByClass(host.getActiveDialog().getWindow().getDecorView(), Button.class);
            assertNotNull(dlgBtn);
            host.simulateClickView(dlgBtn);
            host.pumpEventsAndLayout(100);
            assertTrue(customClicked.get());
            assertNull(host.getActiveDialog());

            SwingUtilities.invokeAndWait(() -> new AlertDialog.Builder(host.getCurrentActivity())
                    .setTitle("Test Alert")
                    .setMessage("Hello")
                    .setPositiveButton("OK", null)
                    .show());
            host.pumpEventsAndLayout(100);
            assertNotNull(host.getActiveDialog());
            host.simulateBack();
            host.pumpEventsAndLayout(80);
            assertNull(host.getActiveDialog());
        }

        // 5. AppRepository.installApksFromDirectory nạp hàng loạt APK
        Path batchSrc = tempDir.resolve("batch-src");
        Files.createDirectories(batchSrc);
        Files.copy(showcaseApk, batchSrc.resolve("aaow-showcase.apk"));
        Path gameApk = resolveRepoFile("samples/aaow-2048.apk");
        if (Files.exists(gameApk)) {
            Files.copy(gameApk, batchSrc.resolve("aaow-2048.apk"));
        }
        AppRepository repo = new AppRepository(tempDir.resolve("repo-data"), null);
        List<AppRepository.InstalledApp> imported = repo.installApksFromDirectory(batchSrc);
        assertTrue(imported.size() >= 1);
        assertNotNull(repo.getApp("dev.aaow.showcase"));
    }

    @Test
    void editorAndNotesEndToEndInteractiveFlowsWork(@TempDir Path tempDir) throws Exception {
        Path editorApk = resolveRepoFile("build/benchmark-apks/org.billthefarmer.editor_198.apk");
        Path notesApk = resolveRepoFile("build/benchmark-apks/org.billthefarmer.notes_142.apk");
        Assumptions.assumeTrue(Files.exists(editorApk) && Files.exists(notesApk),
                "Bỏ qua E2E Editor/Notes vì chưa có bộ APK tại build/benchmark-apks/");
        Path dexCache = resolveSharedDexCache();

        // --- FLOW 1: Editor (gõ văn bản, lưu ra sdcard, mở file khác qua FilePickerBridge) ---
        Path editorData = tempDir.resolve("editor-data");
        ClassLoader editorCl = null;
        try (ApkFile apk = new ApkFile(editorApk)) {
            editorCl = DexLoader.loadApkDex(apk, dexCache, getClass().getClassLoader());
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, editorCl, editorData);
            Application app = AppRepository.createAndInitializeApplication(apk.getManifest(), editorCl, ctx, null);
            HostWindow host = new HostWindow(ctx, app);
            host.launchHeadless(apk.getManifest().launcherActivity, 420, 820);

            Activity act = host.getCurrentActivity();
            EditText et = (EditText) findFirstByClass(act.getWindow().getDecorView(), EditText.class);
            assertNotNull(et);
            assertTrue(et.getHeight() >= 400, "Editor EditText phải trải rộng fillViewport");

            host.simulateClickView(et);
            host.simulateTypeText("AAOW Sprint 10 Editor Test\nLine 2");
            host.pumpEventsAndLayout(120);
            assertTrue(et.getText().toString().contains("AAOW Sprint 10 Editor Test"));

            int saveId = findMenuIdByTitle(act.getOptionsMenu(), "Save");
            assertTrue(saveId != 0, "Phải có nút menu Save sau khi chỉnh sửa");
            host.simulateOptionsMenuItem(saveId);
            host.pumpEventsAndLayout(180);

            Path savedFile = editorData.resolve("sdcard/Documents/Editor.txt");
            assertTrue(Files.exists(savedFile), "File Editor.txt phải được lưu xuống sdcard");
            assertEquals("AAOW Sprint 10 Editor Test\nLine 2", Files.readString(savedFile, StandardCharsets.UTF_8));

            // Kiểm tra mở hộp thoại Open file nội bộ của Editor (openDialog dùng android.R.id.title_template)
            int openId = ctx.getResources().getIdentifier("open", "id", ctx.getPackageName());
            if (openId != 0) {
                host.simulateOptionsMenuItem(openId);
                host.pumpEventsAndLayout(200);
                assertNotNull(host.getActiveDialog(), "Bấm Open file trong Editor phải mở hộp thoại chọn tệp");
                host.simulateBack();
                host.pumpEventsAndLayout(120);
                assertNull(host.getActiveDialog());
            }

            // Mở trực tiếp một tệp văn bản khác trên đĩa qua ACTION_VIEW
            File externalTxt = tempDir.resolve("imported.txt").toFile();
            Files.writeString(externalTxt.toPath(), "Loaded external text file!", StandardCharsets.UTF_8);
            Intent viewFileIntent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(externalTxt));
            viewFileIntent.setClassName(ctx.getPackageName(), apk.getManifest().launcherActivity);
            SwingUtilities.invokeAndWait(() -> host.startActivity(viewFileIntent));
            host.pumpEventsAndLayout(250);
            EditText reopenedEt = (EditText) findFirstByClass(host.getCurrentActivity().getWindow().getDecorView(), EditText.class);
            assertNotNull(reopenedEt);
            assertTrue(reopenedEt.getText().toString().contains("Loaded external text file!"));
        } finally {
            closeQuietly(editorCl);
        }

        // --- FLOW 2: Notes (chuyển chế độ sửa, thêm Markdown, bấm Accept render WebView, lưu Notes.md) ---
        Path notesData = tempDir.resolve("notes-data");
        Path notesFile = notesData.resolve("sdcard/Notes/Notes.md");
        Files.createDirectories(notesFile.getParent());
        Files.writeString(notesFile, "# Initial Heading\n\nBody text.", StandardCharsets.UTF_8);
        ClassLoader notesCl = null;
        try (ApkFile apk = new ApkFile(notesApk)) {
            notesCl = DexLoader.loadApkDex(apk, dexCache, getClass().getClassLoader());
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, notesCl, notesData);
            Application app = AppRepository.createAndInitializeApplication(apk.getManifest(), notesCl, ctx, null);
            HostWindow host = new HostWindow(ctx, app);
            host.launchHeadless(apk.getManifest().launcherActivity, 420, 820);
            host.pumpEventsAndLayout(300);

            Activity act = host.getCurrentActivity();
            int editId = ctx.getResources().getIdentifier("edit", "id", ctx.getPackageName());
            int acceptId = ctx.getResources().getIdentifier("accept", "id", ctx.getPackageName());
            int textId = ctx.getResources().getIdentifier("text", "id", ctx.getPackageName());
            int saveNoteId = ctx.getResources().getIdentifier("saveNote", "id", ctx.getPackageName());

            View editBtn = act.findViewById(editId);
            assertNotNull(editBtn);
            host.simulateClickView(editBtn);
            host.pumpEventsAndLayout(200);

            EditText et = act.findViewById(textId);
            assertNotNull(et);
            assertTrue(et.getText().toString().contains("# Initial Heading"));

            host.simulateClickView(et);
            host.simulateTypeText("\n\n## Added by Sprint 10");
            host.pumpEventsAndLayout(150);

            View acceptBtn = act.findViewById(acceptId);
            assertNotNull(acceptBtn);
            host.simulateClickView(acceptBtn);
            host.pumpEventsAndLayout(400);

            assertTrue(saveNoteId != 0);
            host.simulateOptionsMenuItem(saveNoteId);
            host.pumpEventsAndLayout(300);

            String savedMd = Files.readString(notesFile, StandardCharsets.UTF_8);
            assertTrue(savedMd.contains("## Added by Sprint 10"), "Notes.md phải lưu nội dung Markdown mới");
        } finally {
            closeQuietly(notesCl);
        }
    }

    @Test
    void bubbleLevelAndScrambledExifEndToEndInteractiveFlowsWork(@TempDir Path tempDir) throws Exception {
        Path levelApk = resolveRepoFile("build/benchmark-apks/org.woheller69.level_35.apk");
        Path scrambledApk = resolveRepoFile("build/benchmark-apks/com.jarsilio.android.scrambledeggsif_88.apk");
        Assumptions.assumeTrue(Files.exists(levelApk) && Files.exists(scrambledApk),
                "Bỏ qua E2E Bubble Level / Scrambled Exif vì chưa có APK tại build/benchmark-apks/");
        Path dexCache = resolveSharedDexCache();

        // --- FLOW 3: Bubble Level (đóng FreeDroidWarn, cảm biến nghiêng, mở SettingsActivity) ---
        ClassLoader levelCl = null;
        try (ApkFile apk = new ApkFile(levelApk)) {
            levelCl = DexLoader.loadApkDex(apk, dexCache, getClass().getClassLoader());
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, levelCl, tempDir.resolve("level-data"));
            Application app = AppRepository.createAndInitializeApplication(apk.getManifest(), levelCl, ctx, null);
            HostWindow host = new HostWindow(ctx, app);
            host.launchHeadless(apk.getManifest().launcherActivity, 420, 820);

            assertNotNull(host.getActiveDialog(), "Bubble Level lần đầu phải hiện hộp thoại FreeDroidWarn");
            host.simulateClick(350, 515); // Bấm nút OK
            host.pumpEventsAndLayout(180);
            assertNull(host.getActiveDialog(), "Hộp thoại cảnh báo phải đóng sau khi bấm OK");

            SensorManager sm = (SensorManager) ctx.getSystemService(android.content.Context.SENSOR_SERVICE);
            assertNotNull(sm);
            sm.setVirtualGravity(1.8f, -1.2f, 9.5f);
            host.pumpEventsAndLayout(250);

            int settingsId = findMenuIdByTitle(host.getCurrentActivity().getOptionsMenu(), "Settings");
            assertTrue(settingsId != 0, "Phải có mục Settings trong menu của Bubble Level");
            host.simulateOptionsMenuItem(settingsId);
            host.pumpEventsAndLayout(300);
            assertEquals("org.woheller69.level.SettingsActivity", host.getCurrentActivity().getClass().getName());
        } finally {
            closeQuietly(levelCl);
        }

        // --- FLOW 4: Scrambled Exif (đóng MaterialAlertDialog, xóa EXIF thật qua HandleImageActivity) ---
        Path scrambledData = tempDir.resolve("scrambled-data");
        ClassLoader scrambledCl = null;
        try (ApkFile apk = new ApkFile(scrambledApk)) {
            scrambledCl = DexLoader.loadApkDex(apk, dexCache, getClass().getClassLoader());
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, scrambledCl, scrambledData);
            Application app = AppRepository.createAndInitializeApplication(apk.getManifest(), scrambledCl, ctx, null);
            HostWindow host = new HostWindow(ctx, app);
            host.launchHeadless(apk.getManifest().launcherActivity, 420, 820);

            Dialog dlg = host.getActiveDialog();
            assertNotNull(dlg, "Scrambled Exif phải hiển thị hộp thoại MaterialAlertDialog");
            View dlgButton = findVisibleButton(dlg.getWindow().getDecorView());
            assertNotNull(dlgButton, "Hộp thoại Material phải chứa MaterialButton bấm được");
            host.simulateClickView(dlgButton);
            host.pumpEventsAndLayout(200);
            assertNull(host.getActiveDialog(), "Hộp thoại Material phải đóng sau khi bấm nút");

            // Tạo ảnh JPEG thật có EXIF (Make, Model, GPS) và đẩy vào HandleImageActivity
            File inputJpg = scrambledData.resolve("photo-with-gps.jpg").toFile();
            inputJpg.getParentFile().mkdirs();
            Bitmap bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
            try (FileOutputStream fos = new FileOutputStream(inputJpg)) {
                assertTrue(bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos));
            }
            ExifInterface exifIn = new ExifInterface(inputJpg);
            exifIn.setAttribute(ExifInterface.TAG_MAKE, "AAOW-Cam");
            exifIn.setAttribute(ExifInterface.TAG_MODEL, "AAOW-Model");
            exifIn.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
            exifIn.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "21/1,1/1,42/1");
            exifIn.saveAttributes();

            AtomicReference<Intent> capturedShare = new AtomicReference<>();
            WindowsShareBridge.setTestShareHandler(capturedShare::set);
            try {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("image/jpeg");
                sendIntent.setClassName(ctx.getPackageName(), "com.jarsilio.android.scrambledeggsif.HandleImageActivity");
                sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(inputJpg));
                SwingUtilities.invokeAndWait(() -> host.startActivity(sendIntent));
                host.pumpEventsAndLayout(450);

                assertNotNull(capturedShare.get(), "HandleImageActivity phải gọi ACTION_SEND chia sẻ ảnh đã xóa EXIF");
                List<Uri> outUris = WindowsShareBridge.extractStreamUris(capturedShare.get());
                assertFalse(outUris.isEmpty(), "Share Intent phải chứa URI FileProvider của ảnh đầu ra");

                try (InputStream outIn = ctx.getContentResolver().openInputStream(outUris.get(0))) {
                    assertNotNull(outIn);
                    byte[] cleanBytes = outIn.readAllBytes();
                    assertTrue(cleanBytes.length > 100);
                    ExifInterface exifOut = new ExifInterface(new java.io.ByteArrayInputStream(cleanBytes));
                    assertNull(exifOut.getAttribute(ExifInterface.TAG_MAKE), "EXIF Make phải bị xóa sạch");
                    assertNull(exifOut.getAttribute(ExifInterface.TAG_MODEL), "EXIF Model phải bị xóa sạch");
                    assertNull(exifOut.getAttribute(ExifInterface.TAG_GPS_LATITUDE), "EXIF GPSLatitude phải bị xóa sạch");
                    Bitmap cleanBmp = BitmapFactory.decodeByteArray(cleanBytes, 0, cleanBytes.length);
                    assertNotNull(cleanBmp, "Ảnh JPEG đầu ra sau khi xóa EXIF phải giải mã lại được bình thường");
                }
            } finally {
                WindowsShareBridge.setTestShareHandler(null);
            }
        } finally {
            closeQuietly(scrambledCl);
        }
    }

    @Test
    void privacyFriendlyNotesEndToEndInteractiveFlowWorks(@TempDir Path tempDir) throws Exception {
        Path pfApk = resolveRepoFile("build/benchmark-apks/org.secuso.privacyfriendlynotes_105.apk");
        Assumptions.assumeTrue(Files.exists(pfApk),
                "Bỏ qua E2E Privacy Friendly Notes vì chưa có APK tại build/benchmark-apks/");
        Path dexCache = resolveSharedDexCache();

        ClassLoader cl = null;
        try (ApkFile apk = new ApkFile(pfApk)) {
            cl = DexLoader.loadApkDex(apk, dexCache, getClass().getClassLoader());
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, cl, tempDir.resolve("pfnotes-data"));
            Application app = AppRepository.createAndInitializeApplication(apk.getManifest(), cl, ctx, null);
            HostWindow host = new HostWindow(ctx, app);
            host.launchHeadless(apk.getManifest().launcherActivity, 420, 820);

            // 1. SplashActivity -> tự chuyển sang TutorialActivity
            assertEquals("org.secuso.privacyfriendlynotes.ui.TutorialActivity",
                    host.getCurrentActivity().getClass().getName());

            // 2. Bấm Next chuyển trang ViewPager, sau đó bấm Skip sang MainActivity
            int nextId = ctx.getResources().getIdentifier("btn_next", "id", ctx.getPackageName());
            int skipId = ctx.getResources().getIdentifier("btn_skip", "id", ctx.getPackageName());
            View btnNext = host.getCurrentActivity().findViewById(nextId);
            assertNotNull(btnNext);
            host.simulateClickView(btnNext);
            host.pumpEventsAndLayout(300);

            View btnSkip = host.getCurrentActivity().findViewById(skipId);
            assertNotNull(btnSkip);
            host.simulateClickView(btnSkip);
            host.pumpEventsAndLayout(450);
            assertEquals("org.secuso.privacyfriendlynotes.ui.main.MainActivity",
                    host.getCurrentActivity().getClass().getName());

            // 3. Bấm nút hamburger trên MaterialToolbar mở DrawerLayout -> bấm Back đóng Drawer
            int toolbarId = ctx.getResources().getIdentifier("toolbar", "id", ctx.getPackageName());
            View toolbar = host.getCurrentActivity().findViewById(toolbarId);
            View navBtn = findFirstByClass(toolbar, ImageButton.class);
            assertNotNull(navBtn);
            host.simulateClickView(navBtn);
            host.pumpEventsAndLayout(450);
            host.simulateBack();
            host.pumpEventsAndLayout(600);
            assertEquals("org.secuso.privacyfriendlynotes.ui.main.MainActivity",
                    host.getCurrentActivity().getClass().getName());

            // 4. Bấm FAB '+' -> bấm 'New text' mở TextNoteActivity
            int fabId = ctx.getResources().getIdentifier("fab", "id", ctx.getPackageName());
            int fabTextId = ctx.getResources().getIdentifier("fab_text", "id", ctx.getPackageName());
            View fab = host.getCurrentActivity().findViewById(fabId);
            assertNotNull(fab);
            host.simulateClickView(fab);
            host.pumpEventsAndLayout(300);
            assertEquals(View.GONE, fab.getVisibility(), "Bấm FAB '+' phải mở sheet menu tạo ghi chú");

            View fabText = host.getCurrentActivity().findViewById(fabTextId);
            assertNotNull(fabText);
            host.simulateClickView(fabText);
            host.pumpEventsAndLayout(400);
            assertEquals("org.secuso.privacyfriendlynotes.ui.notes.TextNoteActivity",
                    host.getCurrentActivity().getClass().getName());

            // 5. Nhập tiêu đề & nội dung ghi chú -> bấm Back lưu xuống Room SQLite
            int etNameId = ctx.getResources().getIdentifier("etName", "id", ctx.getPackageName());
            int etContentId = ctx.getResources().getIdentifier("etContent", "id", ctx.getPackageName());
            View etName = host.getCurrentActivity().findViewById(etNameId);
            View etContent = host.getCurrentActivity().findViewById(etContentId);
            assertNotNull(etName);
            assertNotNull(etContent);

            host.simulateClickView(etName);
            host.simulateTypeText("AAOW Sprint 10 Note");
            host.simulateClickView(etContent);
            host.simulateTypeText("Persisted in Room SQLite!");
            host.pumpEventsAndLayout(200);

            host.simulateBack();
            host.pumpEventsAndLayout(500);
            assertEquals("org.secuso.privacyfriendlynotes.ui.main.MainActivity",
                    host.getCurrentActivity().getClass().getName());

            // 6. Kiểm tra RecyclerView hiển thị ghi chú vừa lưu và bấm mở lại được
            int rvId = ctx.getResources().getIdentifier("recycler_view", "id", ctx.getPackageName());
            View rv = host.getCurrentActivity().findViewById(rvId);
            assertInstanceOf(ViewGroup.class, rv);
            ViewGroup rvGroup = (ViewGroup) rv;
            assertTrue(rvGroup.getChildCount() >= 1, "RecyclerView phải hiển thị ít nhất 1 ghi chú đã lưu trong Room");

            View firstCard = rvGroup.getChildAt(0);
            host.simulateClickView(firstCard);
            host.pumpEventsAndLayout(450);

            Activity reopened = host.getCurrentActivity();
            assertEquals("org.secuso.privacyfriendlynotes.ui.notes.TextNoteActivity", reopened.getClass().getName());
            TextView reName = reopened.findViewById(etNameId);
            TextView reContent = reopened.findViewById(etContentId);
            assertNotNull(reName);
            assertNotNull(reContent);
            assertEquals("AAOW Sprint 10 Note", reName.getText().toString());
            assertEquals("Persisted in Room SQLite!", reContent.getText().toString());
        } finally {
            closeQuietly(cl);
        }
    }

    private static Path resolveRepoFile(String relativePath) {
        Path direct = Path.of(relativePath);
        if (Files.exists(direct)) return direct;
        return Path.of("..").resolve(relativePath).normalize();
    }

    private static Path resolveSharedDexCache() {
        Path p = Path.of("build/aaow-data/dex-cache");
        if (Files.exists(p)) return p;
        return Path.of("../build/aaow-data/dex-cache").normalize();
    }

    private static int findMenuIdByTitle(SimpleMenu menu, String title) {
        if (menu == null || title == null) return 0;
        for (SimpleMenu.SimpleMenuItem mi : menu.getVisibleItems()) {
            if (mi.getTitle() != null && title.equalsIgnoreCase(mi.getTitle().toString())) {
                return mi.getItemId();
            }
        }
        return 0;
    }

    private static View findFirstByClass(View root, Class<?> cls) {
        if (root == null) return null;
        if (cls.isInstance(root)) return root;
        if (root instanceof ViewGroup vg) {
            for (int i = 0; i < vg.getChildCount(); i++) {
                View f = findFirstByClass(vg.getChildAt(i), cls);
                if (f != null) return f;
            }
        }
        return null;
    }

    private static View findVisibleButton(View root) {
        if (root == null || root.getVisibility() != View.VISIBLE) return null;
        if (root instanceof Button b && b.getWidth() > 0 && b.getHeight() > 0) return b;
        if (root instanceof ViewGroup vg) {
            for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                View f = findVisibleButton(vg.getChildAt(i));
                if (f != null) return f;
            }
        }
        return null;
    }

    private static void closeQuietly(ClassLoader cl) {
        android.database.sqlite.SQLiteDatabase.releaseMemory();
        if (cl instanceof Closeable c) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
    }
}
