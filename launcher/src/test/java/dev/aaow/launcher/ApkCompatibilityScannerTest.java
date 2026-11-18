package dev.aaow.launcher;

import android.net.Uri;
import android.os.Parcel;
import android.text.Editable;
import android.text.Spanned;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ApkCompatibilityScannerTest {

    @Test
    void sampleApksHaveFullFrameworkCompatibility() throws Exception {
        Path cacheDir = Path.of("build", "aaow-data", "dex-cache");

        ApkCompatibilityScanner.ScanReport showcase =
                ApkCompatibilityScanner.scan(Path.of("samples", "aaow-showcase.apk"), cacheDir);
        assertEquals("dev.aaow.showcase", showcase.packageName());
        assertTrue(showcase.launcherExtendsActivity());
        assertEquals(100.0, showcase.classCoveragePercent(), 0.01);
        assertEquals(100.0, showcase.uniqueMethodCoveragePercent(), 0.01);
        assertEquals(100.0, showcase.callSiteCoveragePercent(), 0.01);
        assertEquals(0, showcase.missingOnFrameworkUniqueMethods());

        ApkCompatibilityScanner.ScanReport game2048 =
                ApkCompatibilityScanner.scan(Path.of("samples", "aaow-2048.apk"), cacheDir);
        assertEquals("dev.aaow.game2048", game2048.packageName());
        assertTrue(game2048.launcherExtendsActivity());
        assertEquals(100.0, game2048.classCoveragePercent(), 0.01);
        assertEquals(100.0, game2048.uniqueMethodCoveragePercent(), 0.01);
        assertEquals(100.0, game2048.callSiteCoveragePercent(), 0.01);
    }

    @Test
    void newFrameworkApisBehaveAccurately() throws Exception {
        JSONObject obj = new JSONObject("{\"name\":\"AAOW\",\"version\":3,\"tags\":[\"win\",\"apk\"]}");
        assertEquals("AAOW", obj.getString("name"));
        assertEquals(3, obj.getInt("version"));
        JSONArray tags = obj.getJSONArray("tags");
        assertEquals(2, tags.length());
        assertEquals("apk", tags.getString(1));

        Uri uri = Uri.parse("https://aaow.dev/launch?pkg=dev.aaow.showcase&mode=direct");
        assertEquals("dev.aaow.showcase", uri.getQueryParameter("pkg"));
        assertEquals("direct", uri.getQueryParameter("mode"));
        assertEquals("launch", uri.getLastPathSegment());

        Parcel p = Parcel.obtain();
        p.writeInt(2026);
        p.writeString("hello");
        p.setDataPosition(0);
        assertEquals(2026, p.readInt());
        assertEquals("hello", p.readString());
        p.recycle();

        Editable ed = new Editable.SimpleEditable("hello world");
        Object marker = new Object();
        ed.setSpan(marker, 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        assertEquals(1, ed.getSpans(0, 5, Object.class).length);
        assertEquals(0, ed.getSpanStart(marker));
        assertEquals(5, ed.getSpanEnd(marker));
    }
}
