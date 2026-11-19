package dev.aaow.launcher;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Parcel;
import android.provider.MediaStore;
import android.security.NetworkSecurityPolicy;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import com.sun.net.httpserver.HttpServer;
import dev.aaow.apk.ApkFile;
import dev.aaow.rt.AndroidRuntimeContext;
import dev.aaow.rt.HostWindow;
import dev.aaow.rt.SimpleMenu;
import dev.aaow.rt.WindowsBrowserBridge;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị và tương tác đầu-cuối (E2E) cho Sprint 11 (Phase C — Non-GMS Online & Media Apps):
 * 1. Tầng mạng hiện đại: ConnectivityManager callbacks (onAvailable/onCapabilitiesChanged/
 *    onLinkPropertiesChanged/onLost), LinkProperties/RouteInfo/IpPrefix/ProxyInfo,
 *    WifiManager/WifiInfo/DhcpInfo, TrafficStats, NetworkSecurityPolicy.
 * 2. ImageDecoder, BitmapFactory.Options (Glide/Picasso/Coil), MediaStore + MediaStoreProvider,
 *    android.resource:// AssetFileDescriptor và WindowsBrowserBridge (ACTION_VIEW http/https).
 * 3. Tải JSON + ảnh PNG qua HttpURLConnection nội bộ (HttpServer) và giải mã qua ImageDecoder/BitmapFactory.
 * 4. Tương tác E2E trên 3 APK Non-GMS Online thực tế: Currency, Forecastie (OkHttp3 + WilliamChart + Leaflet)
 *    và OMWeather / Cirrus (Volley + Open-Meteo + OSMDroid RainViewer).
 */
class Sprint11FrameworkTest {

    @Test
    void modernNetworkingCallbacksWifiTrafficStatsAndSecurityPolicyWork(@TempDir Path tempDir) throws Exception {
        Path showcaseApk = resolveRepoFile("samples/aaow-showcase.apk");
        try (ApkFile apk = new ApkFile(showcaseApk)) {
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, getClass().getClassLoader(),
                    tempDir.resolve("net-ctx"));

            // 1. ConnectivityManager callbacks + LinkProperties + Simulated offline/online
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            assertNotNull(cm);
            assertFalse(cm.isSimulatedOffline());
            Network active = cm.getActiveNetwork();
            assertNotNull(active);

            NetworkCapabilities caps = cm.getNetworkCapabilities(active);
            assertNotNull(caps);
            assertTrue(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
            assertTrue(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
            assertTrue(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
            assertInstanceOf(WifiInfo.class, caps.getTransportInfo());

            LinkProperties lp = cm.getLinkProperties(active);
            assertNotNull(lp);
            assertNotNull(lp.getInterfaceName());
            assertFalse(lp.getLinkAddresses().isEmpty());
            assertFalse(lp.getDnsServers().isEmpty());
            assertFalse(lp.getRoutes().isEmpty());

            AtomicInteger availableCount = new AtomicInteger();
            AtomicInteger capsCount = new AtomicInteger();
            AtomicInteger lpCount = new AtomicInteger();
            AtomicInteger lostCount = new AtomicInteger();
            CountDownLatch initialLatch = new CountDownLatch(3);
            CountDownLatch lostLatch = new CountDownLatch(1);

            ConnectivityManager.NetworkCallback cb = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    availableCount.incrementAndGet();
                    initialLatch.countDown();
                }

                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                    capsCount.incrementAndGet();
                    initialLatch.countDown();
                }

                @Override
                public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                    lpCount.incrementAndGet();
                    initialLatch.countDown();
                }

                @Override
                public void onLost(Network network) {
                    lostCount.incrementAndGet();
                    lostLatch.countDown();
                }
            };

            cm.registerDefaultNetworkCallback(cb);
            assertTrue(initialLatch.await(3, TimeUnit.SECONDS),
                    "registerDefaultNetworkCallback phải chủ động gọi onAvailable/onCapabilitiesChanged/onLinkPropertiesChanged");
            assertEquals(1, availableCount.get());
            assertEquals(1, capsCount.get());
            assertEquals(1, lpCount.get());

            try {
                cm.setSimulatedOffline(true);
                assertNull(cm.getActiveNetwork());
                assertTrue(lostLatch.await(3, TimeUnit.SECONDS), "Mất mạng giả lập phải kích hoạt onLost");
                assertEquals(1, lostCount.get());

                cm.setSimulatedOffline(false);
                assertNotNull(cm.getActiveNetwork());
            } finally {
                cm.setSimulatedOffline(false);
                cm.unregisterNetworkCallback(cb);
            }

            // NetworkRequest + registerNetworkCallback
            CountDownLatch reqLatch = new CountDownLatch(1);
            ConnectivityManager.NetworkCallback reqCb = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    reqLatch.countDown();
                }
            };
            NetworkRequest req = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build();
            cm.registerNetworkCallback(req, reqCb);
            assertTrue(reqLatch.await(3, TimeUnit.SECONDS));
            cm.unregisterNetworkCallback(reqCb);

            // 2. LinkAddress / IpPrefix / RouteInfo / ProxyInfo / DhcpInfo
            InetAddress localAddr = InetAddress.getByName("192.168.1.42");
            InetAddress gwAddr = InetAddress.getByName("192.168.1.1");
            IpPrefix prefix = new IpPrefix(InetAddress.getByName("192.168.1.0"), 24);
            assertTrue(prefix.contains(localAddr));
            assertEquals(24, prefix.getPrefixLength());
            LinkAddress la = new LinkAddress(localAddr, 24);
            assertEquals(localAddr, la.getAddress());
            RouteInfo route = new RouteInfo(prefix, gwAddr, "wlan0");
            assertEquals("wlan0", route.getInterface());
            assertFalse(route.isDefaultRoute());
            ProxyInfo proxy = ProxyInfo.buildDirectProxy("127.0.0.1", 8080, List.of("localhost"));
            assertEquals("127.0.0.1", proxy.getHost());
            assertEquals(8080, proxy.getPort());

            // 3. WifiManager + WifiInfo + DhcpInfo + Locks
            WifiManager wm = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            assertNotNull(wm);
            assertTrue(wm.isWifiEnabled());
            assertEquals(WifiManager.WIFI_STATE_ENABLED, wm.getWifiState());
            WifiInfo wi = wm.getConnectionInfo();
            assertNotNull(wi);
            assertEquals(SupplicantState.COMPLETED, wi.getSupplicantState());
            assertTrue(wi.getLinkSpeed() > 0);
            assertTrue(wi.getFrequency() > 2000);
            DhcpInfo dhcp = wm.getDhcpInfo();
            assertNotNull(dhcp);
            assertTrue(dhcp.dns1 != 0);

            WifiManager.WifiLock wLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "test-lock");
            assertFalse(wLock.isHeld());
            wLock.acquire();
            assertTrue(wLock.isHeld());
            wLock.release();
            assertFalse(wLock.isHeld());

            WifiManager.MulticastLock mLock = wm.createMulticastLock("mdns-lock");
            mLock.acquire();
            assertTrue(mLock.isHeld());
            mLock.release();
            assertFalse(mLock.isHeld());
            assertTrue(WifiManager.calculateSignalLevel(-50, 5) >= 3);

            // 4. TrafficStats + NetworkSecurityPolicy
            TrafficStats.clearThreadStatsTag();
            assertEquals(-1, TrafficStats.getThreadStatsTag());
            TrafficStats.setThreadStatsTag(0x11AA);
            assertEquals(0x11AA, TrafficStats.getThreadStatsTag());
            assertEquals(0x11AA, TrafficStats.getAndSetThreadStatsTag(0x22BB));
            assertEquals(0x22BB, TrafficStats.getThreadStatsTag());
            TrafficStats.clearThreadStatsTag();
            assertTrue(TrafficStats.getTotalRxBytes() >= 0);

            NetworkSecurityPolicy nsp = NetworkSecurityPolicy.getInstance();
            assertNotNull(nsp);
            assertTrue(nsp.isCleartextTrafficPermitted());
            assertTrue(nsp.isCleartextTrafficPermitted("api.open-meteo.com"));

            // 5. Rect/PathMeasure geometry additions
            Rect outer = new Rect(0, 0, 100, 100);
            Rect inner = new Rect(10, 10, 50, 50);
            assertTrue(outer.contains(inner));
            assertTrue(Rect.intersects(outer, inner));
            Parcel p = Parcel.obtain();
            outer.writeToParcel(p, 0);
            p.setDataPosition(0);
            assertEquals(outer, Rect.CREATOR.createFromParcel(p));

            android.graphics.Path line = new android.graphics.Path();
            line.moveTo(0, 0);
            line.lineTo(30, 40);
            PathMeasure pm = new PathMeasure(line, false);
            assertEquals(50f, pm.getLength(), 0.5f);
            float[] pos = new float[2];
            assertTrue(pm.getPosTan(25f, pos, null));
            assertEquals(15f, pos[0], 0.5f);
            assertEquals(20f, pos[1], 0.5f);
        }
    }

    @Test
    void imageDecoderBitmapFactoryMediaStoreAndWindowsBrowserBridgeWork(@TempDir Path tempDir) throws Exception {
        Path prevDataRoot = Environment.getDataRoot();
        Environment.setDataRoot(tempDir.resolve("env-root"));
        Path showcaseApk = resolveRepoFile("samples/aaow-showcase.apk");
        try (ApkFile apk = new ApkFile(showcaseApk)) {
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, getClass().getClassLoader(),
                    tempDir.resolve("media-ctx"));

            // Tạo ảnh PNG mẫu 64x48 (nửa trái đỏ, nửa phải xanh dương)
            Bitmap src = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(src);
            Paint paint = new Paint();
            paint.setColor(0xFFE53935);
            c.drawRect(0, 0, 32, 48, paint);
            paint.setColor(0xFF1E88E5);
            c.drawRect(32, 0, 64, 48, paint);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            assertTrue(src.compress(Bitmap.CompressFormat.PNG, 100, baos));
            byte[] pngBytes = baos.toByteArray();
            File pngFile = tempDir.resolve("sample.png").toFile();
            Files.write(pngFile.toPath(), pngBytes);

            // 1. ImageDecoder: byte[], ByteBuffer, File + OnHeaderDecodedListener (targetSize + PostProcessor)
            AtomicReference<Size> headerSize = new AtomicReference<>();
            AtomicReference<String> headerMime = new AtomicReference<>();
            Bitmap scaled = ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(ByteBuffer.wrap(pngBytes)),
                    (decoder, info, source) -> {
                        headerSize.set(info.getSize());
                        headerMime.set(info.getMimeType());
                        decoder.setTargetSize(32, 24);
                        decoder.setPostProcessor(canvas -> {
                            Paint dot = new Paint();
                            dot.setColor(0xFF00E676);
                            canvas.drawRect(0, 0, 4, 4, dot);
                            return 0;
                        });
                    });
            assertEquals(new Size(64, 48), headerSize.get());
            assertEquals("image/png", headerMime.get());
            assertEquals(32, scaled.getWidth());
            assertEquals(24, scaled.getHeight());
            assertEquals(0xFF00E676, scaled.getPixel(1, 1));

            Drawable drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(pngFile));
            assertNotNull(drawable);
            assertEquals(64, drawable.getIntrinsicWidth());
            assertEquals(48, drawable.getIntrinsicHeight());

            // 2. BitmapFactory.Options cho Glide/Picasso/Coil
            BitmapFactory.Options boundsOpts = new BitmapFactory.Options();
            boundsOpts.inJustDecodeBounds = true;
            assertNull(BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.length, boundsOpts));
            assertEquals(64, boundsOpts.outWidth);
            assertEquals(48, boundsOpts.outHeight);
            assertEquals("image/png", boundsOpts.outMimeType);
            assertEquals(Bitmap.Config.ARGB_8888, boundsOpts.outConfig);
            assertEquals(ColorSpace.get(ColorSpace.Named.SRGB), boundsOpts.outColorSpace);

            BitmapFactory.Options sampleOpts = new BitmapFactory.Options();
            sampleOpts.inSampleSize = 2;
            Bitmap half = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.length, sampleOpts);
            assertNotNull(half);
            assertEquals(32, half.getWidth());
            assertEquals(24, half.getHeight());

            // 3. MediaStore + MediaStoreProvider ánh xạ vào sdcard/Pictures
            ContentResolver cr = ctx.getContentResolver();
            String inserted = MediaStore.Images.Media.insertImage(cr, src, "sprint11_test_photo", "AAOW test");
            assertNotNull(inserted);
            Uri mediaUri = Uri.parse(inserted);
            assertEquals("content", mediaUri.getScheme());
            assertEquals(MediaStore.AUTHORITY, mediaUri.getAuthority());

            try (Cursor cur = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null)) {
                assertNotNull(cur);
                assertTrue(cur.getCount() >= 1);
                boolean found = false;
                while (cur.moveToNext()) {
                    int nameIdx = cur.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    if (nameIdx >= 0 && cur.getString(nameIdx).contains("sprint11_test_photo")) {
                        found = true;
                        int mimeIdx = cur.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE);
                        assertEquals("image/jpeg", cur.getString(mimeIdx));
                    }
                }
                assertTrue(found, "MediaStoreProvider phải trả về ảnh vừa chèn trong Pictures/");
            }

            Bitmap fromMedia = MediaStore.Images.Media.getBitmap(cr, mediaUri);
            assertNotNull(fromMedia);
            assertEquals(64, fromMedia.getWidth());
            assertEquals(48, fromMedia.getHeight());

            Bitmap viaDecoder = ImageDecoder.decodeBitmap(ImageDecoder.createSource(cr, mediaUri));
            assertNotNull(viaDecoder);
            assertEquals(64, viaDecoder.getWidth());

            assertEquals(1, cr.delete(mediaUri, null, null));

            // 4. ContentResolver.openAssetFileDescriptor cho android.resource://
            int iconResId = apk.getManifest().iconRes;
            if (iconResId != 0) {
                Uri resUri = Uri.parse("android.resource://" + ctx.getPackageName() + "/" + iconResId);
                try (AssetFileDescriptor afd = cr.openAssetFileDescriptor(resUri, "r")) {
                    assertNotNull(afd);
                    assertTrue(afd.getLength() > 0);
                }
            }

            // 5. WindowsBrowserBridge bắt Intent.ACTION_VIEW http/https
            AtomicReference<Uri> openedUrl = new AtomicReference<>();
            WindowsBrowserBridge.setTestBrowserHandler(openedUrl::set);
            try {
                Intent webIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://f-droid.org/packages/cz.martykan.forecastie/"));
                assertTrue(WindowsBrowserBridge.isWebBrowserIntent(webIntent));
                ctx.startActivity(webIntent);
                assertEquals("https://f-droid.org/packages/cz.martykan.forecastie/",
                        String.valueOf(openedUrl.get()));

                Intent nonWeb = new Intent(Intent.ACTION_VIEW, Uri.parse("content://media/external/images/media/1"));
                assertFalse(WindowsBrowserBridge.isWebBrowserIntent(nonWeb));
            } finally {
                WindowsBrowserBridge.setTestBrowserHandler(null);
            }
        } finally {
            Environment.setDataRoot(prevDataRoot);
        }
    }

    @Test
    void localHttpServerJsonAndImageDownloadPipelineWorks() throws Exception {
        // Tạo PNG 40x40 màu xanh ngọc
        Bitmap icon = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(icon);
        c.drawColor(0xFF00897B);
        ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
        assertTrue(icon.compress(Bitmap.CompressFormat.PNG, 100, pngOut));
        byte[] iconBytes = pngOut.toByteArray();
        byte[] jsonBytes = "{\"city\":\"Hanoi\",\"temp\":26.5,\"condition\":\"Clear\"}".getBytes(StandardCharsets.UTF_8);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/weather.json", ex -> {
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            ex.sendResponseHeaders(200, jsonBytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(jsonBytes);
            }
        });
        server.createContext("/img/icon.png", ex -> {
            ex.getResponseHeaders().add("Content-Type", "image/png");
            ex.sendResponseHeaders(200, iconBytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(iconBytes);
            }
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            TrafficStats.setThreadStatsTag(0x1101);

            HttpURLConnection jsonConn = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/api/weather.json")
                    .openConnection();
            assertEquals(200, jsonConn.getResponseCode());
            try (InputStream in = jsonConn.getInputStream()) {
                String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(body.contains("\"city\":\"Hanoi\""));
                assertTrue(body.contains("26.5"));
            }

            HttpURLConnection imgConn = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/img/icon.png")
                    .openConnection();
            assertEquals(200, imgConn.getResponseCode());
            byte[] downloaded;
            try (InputStream in = imgConn.getInputStream()) {
                downloaded = in.readAllBytes();
            }

            Bitmap decodedA = ImageDecoder.decodeBitmap(ImageDecoder.createSource(downloaded));
            Bitmap decodedB = BitmapFactory.decodeByteArray(downloaded, 0, downloaded.length);
            assertEquals(40, decodedA.getWidth());
            assertEquals(40, decodedA.getHeight());
            assertEquals(0xFF00897B, decodedA.getPixel(20, 20));
            assertEquals(40, decodedB.getWidth());
            assertEquals(0xFF00897B, decodedB.getPixel(20, 20));
        } finally {
            TrafficStats.clearThreadStatsTag();
            server.stop(0);
        }
    }

    @Test
    void currencyAndForecastieOnlineAppsEndToEndWork(@TempDir Path tempDir) throws Exception {
        Path currencyApk = resolveRepoFile("build/benchmark-apks/org.billthefarmer.currency_158.apk");
        Path forecastieApk = resolveRepoFile("build/benchmark-apks/cz.martykan.forecastie_75.apk");
        Assumptions.assumeTrue(Files.exists(currencyApk) && Files.exists(forecastieApk),
                "Bỏ qua E2E Currency/Forecastie vì chưa có APK tại build/benchmark-apks/");
        Path dexCache = resolveSharedDexCache();

        // --- FLOW 1: Currency v1.58 (ECB HTTPS XML + Vector Flags + Converter) ---
        ClassLoader currencyCl = null;
        try (ApkFile apk = new ApkFile(currencyApk)) {
            currencyCl = DexLoader.loadApkDex(apk, dexCache, getClass().getClassLoader());
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, currencyCl, tempDir.resolve("currency-data"));
            Application app = AppRepository.createAndInitializeApplication(apk.getManifest(), currencyCl, ctx, null);
            HostWindow host = new HostWindow(ctx, app);
            host.launchHeadless(apk.getManifest().launcherActivity, 420, 820);
            host.pumpEventsAndLayout(900);

            Activity main = host.getCurrentActivity();
            assertEquals("org.billthefarmer.currency.Main", main.getClass().getName());
            ListView lv = findFirstByClass(main.getWindow().getDecorView(), ListView.class);
            assertNotNull(lv, "Currency phải hiển thị ListView danh sách tỷ giá ngoại tệ");
            assertTrue(lv.getChildCount() >= 3, "ListView tỷ giá phải có ít nhất 3 đồng tiền (USD, GBP, ...)");

            // Kích hoạt menu Refresh tỷ giá thật qua HTTPS ECB và bấm chọn 1 dòng tiền tệ
            int refreshId = findMenuIdByTitle(main.getOptionsMenu(), "Refresh");
            if (refreshId != 0) {
                host.simulateOptionsMenuItem(refreshId);
                host.pumpEventsAndLayout(1200);
            }
            host.simulateClickView(lv.getChildAt(0));
            host.pumpEventsAndLayout(300);
            assertEquals("org.billthefarmer.currency.Main", host.getCurrentActivity().getClass().getName());
        } finally {
            closeQuietly(currencyCl);
        }

        // --- FLOW 2: Forecastie v1.23 (OkHttp3 HTTPS + TabLayout/ViewPager + WilliamChart + Leaflet WebView) ---
        ClassLoader forecastieCl = null;
        try (ApkFile apk = new ApkFile(forecastieApk)) {
            forecastieCl = DexLoader.loadApkDex(apk, dexCache, getClass().getClassLoader());
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, forecastieCl, tempDir.resolve("forecastie-data"));
            Application app = AppRepository.createAndInitializeApplication(apk.getManifest(), forecastieCl, ctx, null);
            HostWindow host = new HostWindow(ctx, app);
            host.launchHeadless(apk.getManifest().launcherActivity, 420, 820);

            // Chờ SplashActivity -> MainActivity và cả TodayWeatherTask + LongTermWeatherTask nạp TabLayout xong
            long deadline = System.currentTimeMillis() + 8000;
            View tomorrowTab = null;
            while (System.currentTimeMillis() < deadline) {
                host.pumpEventsAndLayout(300);
                if (host.getCurrentActivity() != null
                        && "cz.martykan.forecastie.activities.MainActivity".equals(host.getCurrentActivity().getClass().getName())
                        && host.getActiveDialog() == null) {
                    tomorrowTab = findViewWithText(host.getCurrentActivity().getWindow().getDecorView(), "TOMORROW");
                    if (tomorrowTab != null) {
                        break;
                    }
                }
            }
            Activity mainAct = host.getCurrentActivity();
            assertEquals("cz.martykan.forecastie.activities.MainActivity", mainAct.getClass().getName());

            int tempId = ctx.getResources().getIdentifier("todayTemperature", "id", ctx.getPackageName());
            TextView todayTemp = mainAct.findViewById(tempId);
            assertNotNull(todayTemp, "Forecastie MainActivity phải hiển thị nhiệt độ hiện tại");
            assertFalse(todayTemp.getText().toString().isBlank(),
                    "todayTemperature phải chứa giá trị thời tiết từ OpenWeatherMap");

            // Bấm chuyển tab TOMORROW trong TabLayout/ViewPager
            assertNotNull(tomorrowTab, "TabLayout phải có tab TOMORROW");
            host.simulateClickView(tomorrowTab);
            host.pumpEventsAndLayout(350);

            // Mở GraphActivity (WilliamChart LineChartView + BarChartView với DashPathEffect + PathMeasure)
            SwingUtilities.invokeAndWait(() -> {
                Intent graphIntent = new Intent();
                graphIntent.setClassName(ctx.getPackageName(), "cz.martykan.forecastie.activities.GraphActivity");
                host.startActivity(graphIntent);
            });
            host.pumpEventsAndLayout(450);
            assertEquals("cz.martykan.forecastie.activities.GraphActivity",
                    host.getCurrentActivity().getClass().getName());

            int tempChartId = ctx.getResources().getIdentifier("graph_temperature", "id", ctx.getPackageName());
            View tempChart = host.getCurrentActivity().findViewById(tempChartId);
            assertNotNull(tempChart, "GraphActivity phải có biểu đồ graph_temperature");
            assertTrue(tempChart.getWidth() > 100 && tempChart.getHeight() > 80);

            // Mở MapActivity (Leaflet OpenStreetMap WebView)
            SwingUtilities.invokeAndWait(() -> {
                Intent mapIntent = new Intent();
                mapIntent.setClassName(ctx.getPackageName(), "cz.martykan.forecastie.activities.MapActivity");
                host.startActivity(mapIntent);
            });
            host.pumpEventsAndLayout(600);
            assertEquals("cz.martykan.forecastie.activities.MapActivity",
                    host.getCurrentActivity().getClass().getName());
            android.webkit.WebView webView = findFirstByClass(
                    host.getCurrentActivity().getWindow().getDecorView(), android.webkit.WebView.class);
            assertNotNull(webView, "MapActivity phải khởi tạo WebView Leaflet");
            assertNotNull(webView.getUrl());
            assertTrue(webView.getUrl().contains("map.html"), "WebView phải nạp trang bản đồ map.html");
        } finally {
            closeQuietly(forecastieCl);
        }
    }

    @Test
    void omWeatherCirrusOnlineAppEndToEndWork(@TempDir Path tempDir) throws Exception {
        Path omApk = resolveRepoFile("build/benchmark-apks/org.woheller69.omweather_50.apk");
        Assumptions.assumeTrue(Files.exists(omApk),
                "Bỏ qua E2E OMWeather vì chưa có APK tại build/benchmark-apks/");
        Path dexCache = resolveSharedDexCache();

        ClassLoader cl = null;
        try (ApkFile apk = new ApkFile(omApk)) {
            cl = DexLoader.loadApkDex(apk, dexCache, getClass().getClassLoader());
            AndroidRuntimeContext ctx = new AndroidRuntimeContext(apk, cl, tempDir.resolve("omweather-data"));
            Application app = AppRepository.createAndInitializeApplication(apk.getManifest(), cl, ctx, null);
            HostWindow host = new HostWindow(ctx, app);
            host.launchHeadless(apk.getManifest().launcherActivity, 420, 820);
            host.pumpEventsAndLayout(350);

            // 1. SplashActivity -> TutorialActivity
            assertEquals("org.woheller69.weather.firststart.TutorialActivity",
                    host.getCurrentActivity().getClass().getName());

            // Bấm Next qua các trang hướng dẫn (mở SettingsActivity hoặc ForecastCityActivity)
            int nextId = ctx.getResources().getIdentifier("btn_next", "id", ctx.getPackageName());
            for (int step = 0; step < 6; step++) {
                if (!"org.woheller69.weather.firststart.TutorialActivity"
                        .equals(host.getCurrentActivity().getClass().getName())) {
                    break;
                }
                View btnNext = host.getCurrentActivity().findViewById(nextId);
                if (btnNext != null) {
                    host.simulateClickView(btnNext);
                    host.pumpEventsAndLayout(300);
                }
            }
            String afterTutorial = host.getCurrentActivity().getClass().getName();
            assertTrue(
                    "org.woheller69.weather.activities.SettingsActivity".equals(afterTutorial)
                            || "org.woheller69.weather.activities.ForecastCityActivity".equals(afterTutorial),
                    "Sau TutorialActivity phải mở SettingsActivity hoặc ForecastCityActivity, thực tế: " + afterTutorial);

            // Chuyển sang ForecastCityActivity nếu đang ở SettingsActivity
            if (!"org.woheller69.weather.activities.ForecastCityActivity".equals(afterTutorial)) {
                SwingUtilities.invokeAndWait(() -> {
                    Intent fcIntent = new Intent();
                    fcIntent.setClassName(ctx.getPackageName(),
                            "org.woheller69.weather.activities.ForecastCityActivity");
                    host.startActivity(fcIntent);
                });
                host.pumpEventsAndLayout(350);
            }
            assertEquals("org.woheller69.weather.activities.ForecastCityActivity",
                    host.getCurrentActivity().getClass().getName());

            // Đóng các hộp thoại FreeDroidWarn nếu xuất hiện
            for (int i = 0; i < 4 && host.getActiveDialog() != null; i++) {
                Dialog dlg = host.getActiveDialog();
                View okBtn = findViewWithText(dlg.getWindow().getDecorView(), "OK");
                if (okBtn != null) {
                    host.simulateClickView(okBtn);
                } else if (dlg instanceof android.app.AlertDialog ad) {
                    SwingUtilities.invokeAndWait(() ->
                            ad.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick());
                } else {
                    SwingUtilities.invokeAndWait(dlg::dismiss);
                }
                host.pumpEventsAndLayout(200);
            }
            assertNull(host.getActiveDialog(), "Hộp thoại FreeDroidWarn phải đóng sau khi bấm OK");

            // 2. Mở ManageLocationsActivity -> bấm FAB '+' mở hộp thoại tìm thành phố (Volley + Open-Meteo)
            SwingUtilities.invokeAndWait(() -> {
                Intent manageIntent = new Intent();
                manageIntent.setClassName(ctx.getPackageName(),
                        "org.woheller69.weather.activities.ManageLocationsActivity");
                host.startActivity(manageIntent);
            });
            host.pumpEventsAndLayout(350);
            assertEquals("org.woheller69.weather.activities.ManageLocationsActivity",
                    host.getCurrentActivity().getClass().getName());
            if (host.getActiveDialog() != null) {
                Dialog dlg = host.getActiveDialog();
                View okBtn = findViewWithText(dlg.getWindow().getDecorView(), "OK");
                if (okBtn != null) {
                    host.simulateClickView(okBtn);
                } else if (dlg instanceof android.app.AlertDialog ad) {
                    SwingUtilities.invokeAndWait(() ->
                            ad.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick());
                } else {
                    SwingUtilities.invokeAndWait(dlg::dismiss);
                }
                host.pumpEventsAndLayout(200);
            }

            View fab = findFirstBySimpleClassName(
                    host.getCurrentActivity().getWindow().getDecorView(), "FloatingActionButton");
            assertNotNull(fab, "ManageLocationsActivity phải có nút FloatingActionButton thêm địa điểm");
            host.simulateClickView(fab);
            host.pumpEventsAndLayout(350);

            // 3. Mở RainViewerActivity (Volley HTTPS weather-maps.json + OSMDroid MapView)
            SwingUtilities.invokeAndWait(() -> {
                Intent radarIntent = new Intent();
                radarIntent.setClassName(ctx.getPackageName(),
                        "org.woheller69.weather.activities.RainViewerActivity");
                host.startActivity(radarIntent);
            });
            host.pumpEventsAndLayout(900);
            assertEquals("org.woheller69.weather.activities.RainViewerActivity",
                    host.getCurrentActivity().getClass().getName());
            View mapView = findFirstBySimpleClassName(
                    host.getCurrentActivity().getWindow().getDecorView(), "MapView");
            assertNotNull(mapView, "RainViewerActivity phải khởi tạo OSMDroid MapView");
            assertTrue(mapView.getWidth() > 100 && mapView.getHeight() > 100);
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

    private static <T extends View> T findFirstByClass(View root, Class<T> cls) {
        if (root == null) return null;
        if (cls.isInstance(root)) return cls.cast(root);
        if (root instanceof ViewGroup vg) {
            for (int i = 0; i < vg.getChildCount(); i++) {
                T f = findFirstByClass(vg.getChildAt(i), cls);
                if (f != null) return f;
            }
        }
        return null;
    }

    private static View findFirstBySimpleClassName(View root, String simpleName) {
        if (root == null || simpleName == null) return null;
        if (simpleName.equals(root.getClass().getSimpleName())) return root;
        if (root instanceof ViewGroup vg) {
            for (int i = 0; i < vg.getChildCount(); i++) {
                View f = findFirstBySimpleClassName(vg.getChildAt(i), simpleName);
                if (f != null) return f;
            }
        }
        return null;
    }

    private static View findViewWithText(View root, String text) {
        if (root == null || text == null) return null;
        if (root instanceof TextView tv && tv.getText() != null
                && text.equalsIgnoreCase(tv.getText().toString().trim())) {
            return tv;
        }
        if (root instanceof ViewGroup vg) {
            for (int i = 0; i < vg.getChildCount(); i++) {
                View f = findViewWithText(vg.getChildAt(i), text);
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
