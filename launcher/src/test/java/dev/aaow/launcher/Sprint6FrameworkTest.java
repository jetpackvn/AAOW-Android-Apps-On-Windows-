package dev.aaow.launcher;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.provider.DocumentsContract;
import android.text.Html;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;
import dev.aaow.apk.ApkFile;
import dev.aaow.rt.AndroidRuntimeContext;
import dev.aaow.rt.WebViewEngineBridge;
import dev.aaow.rt.WindowsFilePickerBridge;
import dev.aaow.rt.WindowsTrayBridge;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị toàn diện cho Sprint 6:
 * WebView (JavaFX WebKit, HTML/CSS/JS, file:///android_asset/), Audio (MediaPlayer, SoundPool, AudioTrack, AudioManager),
 * NotificationManager + SystemTray + PendingIntent, ClipboardManager 2 chiều & Windows File Picker.
 */
class Sprint6FrameworkTest {

    private static Path resolveRepoFile(String relative) {
        Path p = Path.of(relative);
        if (Files.exists(p)) return p;
        return Path.of("..").resolve(relative);
    }

    private static AndroidRuntimeContext createSampleContext(Path tempDir) throws Exception {
        Path sampleApk = resolveRepoFile("samples/aaow-showcase.apk");
        try (ApkFile apk = new ApkFile(sampleApk)) {
            return new AndroidRuntimeContext(apk, Sprint6FrameworkTest.class.getClassLoader(), tempDir);
        }
    }

    public static final class JsBridgeSample {
        @JavascriptInterface
        public String greet(String name) {
            return "Hello " + name + " from AAOW";
        }
    }

    @Test
    void webViewRendersRealHtmlCssJsAndApkAssets(@TempDir Path tempDir) throws Exception {
        assertTrue(URLUtil.isAssetUrl("file:///android_asset/help.md"));
        assertTrue(URLUtil.isNetworkUrl("https://example.org/test"));
        assertTrue(URLUtil.isFileUrl("file:///sdcard/note.md"));
        assertFalse(URLUtil.isFileUrl("file:///android_asset/help.md"));
        assertEquals("text/css", MimeTypeMap.getSingleton().getMimeTypeFromExtension("css"));
        assertEquals("help.md", URLUtil.guessFileName("file:///android_asset/help.md", null, null));

        CookieManager cm = CookieManager.getInstance();
        cm.setCookie("https://aaow.dev/", "session=sprint6");
        assertTrue(cm.hasCookies());
        assertTrue(cm.getCookie("https://aaow.dev/").contains("session=sprint6"));

        Path notesApkPath = resolveRepoFile("build/benchmark-apks/org.billthefarmer.notes_142.apk");
        Assumptions.assumeTrue(Files.exists(notesApkPath));

        try (ApkFile notesApk = new ApkFile(notesApkPath)) {
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(notesApk, getClass().getClassLoader(), tempDir.resolve("notes-wv"));
            assertTrue(notesApk.getResources() != null);
            assertTrue(ctx.getAssets().list("").length >= 2);

            WebView wv = new WebView(ctx);
            wv.getSettings().setJavaScriptEnabled(true);
            wv.addJavascriptInterface(new JsBridgeSample(), "aaowBridge");

            CountDownLatch loaded = new CountDownLatch(1);
            AtomicReference<String> finishedUrl = new AtomicReference<>();
            AtomicReference<String> receivedTitle = new AtomicReference<>();

            wv.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    finishedUrl.set(url);
                    loaded.countDown();
                }
            });
            wv.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onReceivedTitle(WebView view, String title) {
                    receivedTitle.set(title);
                }
            });

            wv.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY));
            wv.layout(0, 0, 400, 300);

            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                      <title>AAOW WebView Test</title>
                      <link rel='stylesheet' type='text/css' href='file:///android_asset/styles.css' />
                    </head>
                    <body style='background:#f0f4ff;color:#1b2559;padding:16px'>
                      <h1 id='heading'>Sprint 6 WebKit</h1>
                      <p id='msg'>Initial</p>
                      <script>
                        if (window.aaowBridge) {
                          document.getElementById('msg').innerText = window.aaowBridge.greet('Windows');
                        }
                      </script>
                    </body>
                    </html>
                    """;
            wv.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
            assertTrue(loaded.await(5, TimeUnit.SECONDS), "WebView phải phát sự kiện onPageFinished");
            assertEquals("file:///android_asset/", finishedUrl.get());

            if (WebViewEngineBridge.isJavaFxAvailable()) {
                assertEquals("AAOW WebView Test", receivedTitle.get());

                CountDownLatch jsLatch = new CountDownLatch(1);
                AtomicReference<String> jsResult = new AtomicReference<>();
                wv.evaluateJavascript("document.getElementById('msg').innerText", val -> {
                    jsResult.set(val);
                    jsLatch.countDown();
                });
                assertTrue(jsLatch.await(3, TimeUnit.SECONDS));
                assertEquals("\"Hello Windows from AAOW\"", jsResult.get());
                assertTrue(wv.findAll("Sprint 6") >= 1);
            }

            wv.getEngineBridge().awaitIdle(2000);
            Bitmap bmp = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888);
            wv.draw(new Canvas(bmp));

            int nonWhitePixels = 0;
            for (int y = 0; y < 300; y += 4) {
                for (int x = 0; x < 400; x += 4) {
                    int px = bmp.getPixel(x, y);
                    if ((px & 0x00FFFFFF) != 0x00FFFFFF) {
                        nonWhitePixels++;
                    }
                }
            }
            assertTrue(nonWhitePixels > 50, "WebView phải vẽ nội dung HTML/CSS thật lên Canvas, nonWhite=" + nonWhitePixels);

            // Kiểm tra PrintDocumentAdapter + PrintManager
            PrintManager pm = ctx.getSystemService(PrintManager.class);
            assertNotNull(pm);
            var job = pm.print("NotesDoc", wv.createPrintDocumentAdapter("Notes"), new PrintAttributes.Builder().build());
            assertTrue(job.isCompleted());
            assertEquals(1, pm.getPrintJobs().size());

            wv.destroy();
        }
    }

    @Test
    void windowsAudioMediaPlayerSoundPoolAudioTrackAndManagerWork(@TempDir Path tempDir) throws Exception {
        AndroidRuntimeContext ctx = createSampleContext(tempDir);
        AudioManager am = ctx.getSystemService(AudioManager.class);
        assertNotNull(am);
        assertSame(am, ctx.getSystemService(Context.AUDIO_SERVICE));
        assertEquals("44100", am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));

        int maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol - 2, 0);
        assertEquals(maxVol - 2, am.getStreamVolume(AudioManager.STREAM_MUSIC));
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
        assertEquals(maxVol - 1, am.getStreamVolume(AudioManager.STREAM_MUSIC));
        assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED,
                am.requestAudioFocus(fc -> {}, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN));

        // Tạo file WAV PCM 16-bit 44100Hz 250ms thật (tông nhẹ gần như im lặng để không làm ồn khi test)
        byte[] wavBytes = buildSineWavBytes(44100, 250, 440.0, 0.01);
        Path wavPath = tempDir.resolve("test_tone.wav");
        Files.write(wavPath, wavBytes);

        // 1. MediaPlayer
        MediaPlayer mp = new MediaPlayer();
        mp.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        mp.setDataSource(wavPath.toAbsolutePath().toString());
        mp.prepare();
        assertTrue(mp.getDuration() >= 200 && mp.getDuration() <= 350, "Duration=" + mp.getDuration());
        mp.setVolume(0.1f, 0.1f);
        mp.start();
        assertTrue(mp.isPlaying());
        mp.seekTo(100);
        assertTrue(mp.getCurrentPosition() >= 80);
        mp.pause();
        assertFalse(mp.isPlaying());
        mp.stop();
        mp.reset();
        mp.release();

        // 2. SoundPool
        SoundPool sp = new SoundPool.Builder().setMaxStreams(2).build();
        CountDownLatch spLatch = new CountDownLatch(1);
        AtomicBoolean loadOk = new AtomicBoolean(false);
        sp.setOnLoadCompleteListener((pool, sampleId, status) -> {
            loadOk.set(status == 0);
            spLatch.countDown();
        });
        int sampleId = sp.load(wavPath.toAbsolutePath().toString(), 1);
        assertTrue(spLatch.await(2, TimeUnit.SECONDS));
        assertTrue(loadOk.get());
        int streamId = sp.play(sampleId, 0.05f, 0.05f, 1, 0, 1.0f);
        assertTrue(streamId > 0);
        sp.pause(streamId);
        sp.resume(streamId);
        sp.stop(streamId);
        assertTrue(sp.unload(sampleId));
        sp.release();

        // 3. AudioTrack (MODE_STATIC + MODE_STREAM)
        int minBuf = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        assertTrue(minBuf > 0);
        AudioTrack track = new AudioTrack(
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build(),
                new AudioFormat.Builder()
                        .setSampleRate(44100)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build(),
                minBuf, AudioTrack.MODE_STATIC, am.generateAudioSessionId());
        short[] samples = new short[2048];
        int written = track.write(samples, 0, samples.length);
        assertEquals(samples.length, written);
        assertTrue(track.getWrittenFramesCount() > 0);
        track.setVolume(0.05f);
        track.play();
        assertEquals(AudioTrack.PLAYSTATE_PLAYING, track.getPlayState());
        track.pause();
        assertEquals(AudioTrack.PLAYSTATE_PAUSED, track.getPlayState());
        track.stop();
        track.release();
    }

    @Test
    void notificationManagerPendingIntentAndTrayBridgeWork(@TempDir Path tempDir) throws Exception {
        AndroidRuntimeContext ctx = createSampleContext(tempDir);
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        assertNotNull(nm);
        assertSame(nm, ctx.getSystemService(Context.NOTIFICATION_SERVICE));

        NotificationChannel ch = new NotificationChannel("alerts", "AAOW Alerts", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Sprint 6 channel");
        nm.createNotificationChannel(ch);
        assertNotNull(nm.getNotificationChannel("alerts"));

        CountDownLatch broadcastLatch = new CountDownLatch(1);
        AtomicReference<String> receivedExtra = new AtomicReference<>();
        ctx.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                receivedExtra.set(intent.getStringExtra("note"));
                broadcastLatch.countDown();
            }
        }, new IntentFilter("dev.aaow.NOTIF_ACTION"));

        PendingIntent pi = PendingIntent.getBroadcast(
                ctx, 10, new Intent("dev.aaow.NOTIF_ACTION").putExtra("note", "opened"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AtomicReference<String> bannerTitle = new AtomicReference<>();
        AtomicReference<String> bannerText = new AtomicReference<>();
        WindowsTrayBridge.setInWindowBannerListener((t, body) -> {
            bannerTitle.set(t);
            bannerText.set(body);
        });
        try {
            Notification n = new Notification.Builder(ctx, "alerts")
                    .setContentTitle("AAOW Ready")
                    .setContentText("Thông báo chạy trên Windows")
                    .setStyle(new Notification.BigTextStyle().bigText("Chi tiết thông báo Sprint 6"))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();

            nm.notify("sync", 101, n);
            assertEquals(1, nm.getActivePostedNotifications().size());
            assertEquals("AAOW Ready", bannerTitle.get());
            assertEquals("Chi tiết thông báo Sprint 6", bannerText.get());

            pi.send();
            assertTrue(broadcastLatch.await(2, TimeUnit.SECONDS));
            assertEquals("opened", receivedExtra.get());

            nm.cancel("sync", 101);
            assertTrue(nm.getActivePostedNotifications().isEmpty());
        } finally {
            WindowsTrayBridge.setInWindowBannerListener(null);
        }
    }

    @Test
    void clipboardManagerWindowsSyncFilePickerDocumentsAndUiHelpersWork(@TempDir Path tempDir) throws Exception {
        AndroidRuntimeContext ctx = createSampleContext(tempDir);
        ClipboardManager cb = ctx.getSystemService(ClipboardManager.class);
        assertNotNull(cb);
        assertSame(cb, ctx.getSystemService(Context.CLIPBOARD_SERVICE));

        CountDownLatch clipChanged = new CountDownLatch(1);
        cb.addPrimaryClipChangedListener(clipChanged::countDown);
        cb.setPrimaryClip(ClipData.newPlainText("AAOW", "Sprint6-Clipboard-Text"));
        assertTrue(clipChanged.await(2, TimeUnit.SECONDS));
        assertTrue(cb.hasPrimaryClip());
        assertTrue(cb.hasText());
        assertEquals("Sprint6-Clipboard-Text", cb.getText().toString());
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                Object sysStr = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                assertEquals("Sprint6-Clipboard-Text", sysStr);
            } catch (Exception ignored) {
            }
        }

        // Kiểm tra Windows File Picker bridge qua Activity.startActivityForResult
        Path chosenFile = tempDir.resolve("picked_note.md");
        Files.writeString(chosenFile, "# Picked from Windows", StandardCharsets.UTF_8);

        WindowsFilePickerBridge.setTestPickerOverride(intent -> {
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, intent.getAction());
            return new Intent().setData(Uri.fromFile(chosenFile.toFile()));
        });
        try {
            AtomicReference<Uri> resultUri = new AtomicReference<>();
            Activity act = new Activity() {
                @Override
                protected void onActivityResult(int requestCode, int resultCode, Intent data) {
                    if (requestCode == 77 && resultCode == RESULT_OK && data != null) {
                        resultUri.set(data.getData());
                    }
                }
            };
            act.attach(ctx, null, new Intent(), "Picker", () -> {}, () -> {});
            Intent openDoc = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("text/*");
            act.startActivityForResult(Intent.createChooser(openDoc, "Open Note"), 77);

            assertNotNull(resultUri.get());
            try (InputStream in = ctx.getContentResolver().openInputStream(resultUri.get())) {
                assertEquals("# Picked from Windows", new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        } finally {
            WindowsFilePickerBridge.setTestPickerOverride(null);
        }

        // DocumentsContract + Html.toHtml + HorizontalScrollView.fullScroll + Toast.getView + StaticLayout.Builder
        Uri docUri = DocumentsContract.buildDocumentUri("dev.aaow.docs", "doc42");
        assertTrue(DocumentsContract.isDocumentUri(ctx, docUri));
        assertEquals("doc42", DocumentsContract.getDocumentId(docUri));

        Spanned spanned = Html.fromHtml("Hello<br>World");
        assertTrue(Html.toHtml(spanned).contains("Hello<br>World"));

        Toast toast = Toast.makeText(ctx, "ToastView", Toast.LENGTH_SHORT);
        assertInstanceOf(TextView.class, toast.getView());
        toast.setGravity(android.view.Gravity.CENTER, 0, 0);

        HorizontalScrollView hsv = new HorizontalScrollView(ctx);
        TextView child = new TextView(ctx);
        child.setText("Wide content");
        hsv.addView(child);
        hsv.measure(View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY));
        hsv.layout(0, 0, 100, 50);
        hsv.fullScroll(View.FOCUS_LEFT);

        StaticLayout sl = StaticLayout.Builder.obtain("Line1\nLine2", 0, 11, new TextPaint(), 200).build();
        assertEquals(2, sl.getLineCount());
    }

    private static byte[] buildSineWavBytes(int sampleRate, int durationMs, double freqHz, double amplitude) {
        int numSamples = (sampleRate * durationMs) / 1000;
        int dataSize = numSamples * 2;
        ByteBuffer buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        buf.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        buf.putInt(36 + dataSize);
        buf.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        buf.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        buf.putInt(16);
        buf.putShort((short) 1); // PCM
        buf.putShort((short) 1); // Mono
        buf.putInt(sampleRate);
        buf.putInt(sampleRate * 2);
        buf.putShort((short) 2);
        buf.putShort((short) 16);
        buf.put("data".getBytes(StandardCharsets.US_ASCII));
        buf.putInt(dataSize);
        for (int i = 0; i < numSamples; i++) {
            double angle = 2.0 * Math.PI * freqHz * i / sampleRate;
            short sample = (short) Math.round(Math.sin(angle) * amplitude * 32767.0);
            buf.putShort(sample);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(buf.array());
        return out.toByteArray();
    }
}
