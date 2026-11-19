package dev.aaow.rt;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Cầu nối chia sẻ hệ thống trên Windows cho các Intent ACTION_SEND / ACTION_SEND_MULTIPLE / ACTION_CHOOSER.
 * Khi ứng dụng (ví dụ Scrambled Exif, Notes, PFNotes) chia sẻ ảnh hoặc văn bản, cầu nối này sẽ:
 * - Gọi testShareHandler nếu đang chạy kiểm thử tự động, HOẶC
 * - Sao chép văn bản sang Windows Clipboard / bật hộp thoại lưu ảnh ra ổ cứng Windows.
 */
public final class WindowsShareBridge {
    private static final String TAG = "AAOW-Share";
    private static volatile Consumer<Intent> testShareHandler;

    private WindowsShareBridge() {
    }

    public static void setTestShareHandler(Consumer<Intent> handler) {
        testShareHandler = handler;
    }

    public static boolean isShareIntent(Intent intent) {
        if (intent == null) return false;
        Intent inner = WindowsFilePickerBridge.unwrapChooser(intent);
        if (inner == null) return false;
        String act = inner.getAction();
        return Intent.ACTION_SEND.equals(act) || Intent.ACTION_SEND_MULTIPLE.equals(act);
    }

    public static boolean handleShare(Context ctx, Intent rawIntent) {
        if (!isShareIntent(rawIntent)) return false;
        Intent inner = WindowsFilePickerBridge.unwrapChooser(rawIntent);
        Consumer<Intent> hook = testShareHandler;
        if (hook != null) {
            hook.accept(inner);
            return true;
        }

        List<Uri> streams = extractStreamUris(inner);
        String text = inner.getStringExtra(Intent.EXTRA_TEXT);

        if (!streams.isEmpty() && ctx != null) {
            if (GraphicsEnvironment.isHeadless()) {
                Log.i(TAG, "Chia sẻ " + streams.size() + " tệp (headless): " + streams);
                return true;
            }
            for (Uri uri : streams) {
                try {
                    String suggested = suggestFilename(uri, inner.getType());
                    FileDialog fd = new FileDialog((Frame) null, "Lưu tệp chia sẻ từ Android", FileDialog.SAVE);
                    fd.setFile(suggested);
                    fd.setVisible(true);
                    if (fd.getDirectory() != null && fd.getFile() != null) {
                        File dst = new File(fd.getDirectory(), fd.getFile());
                        try (InputStream in = ctx.getContentResolver().openInputStream(uri);
                             FileOutputStream fos = new FileOutputStream(dst)) {
                            if (in != null) in.transferTo(fos);
                        }
                        Toast.makeText(ctx, "Đã lưu: " + dst.getName(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Không thể lưu tệp chia sẻ " + uri + ": " + e);
                }
            }
            return true;
        }

        if (text != null && !text.isEmpty() && ctx != null) {
            ClipboardManager cm = ctx.getSystemService(ClipboardManager.class);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Shared Text", text));
            }
            Toast.makeText(ctx, "Đã sao chép nội dung chia sẻ vào Clipboard Windows", Toast.LENGTH_SHORT).show();
            return true;
        }
        return true;
    }

    public static List<Uri> extractStreamUris(Intent intent) {
        List<Uri> uris = new ArrayList<>();
        if (intent == null) return uris;
        Object single = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (single instanceof Uri u) {
            uris.add(u);
        }
        ArrayList<?> multi = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (multi != null) {
            for (Object o : multi) {
                if (o instanceof Uri u && !uris.contains(u)) uris.add(u);
            }
        }
        ClipData clip = intent.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri u = clip.getItemAt(i).getUri();
                if (u != null && !uris.contains(u)) uris.add(u);
            }
        }
        return uris;
    }

    private static String suggestFilename(Uri uri, String mimeType) {
        String name = uri != null ? uri.getLastPathSegment() : null;
        if (name != null && !name.isEmpty()) {
            int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
            if (slash >= 0) name = name.substring(slash + 1);
            if (!name.isEmpty()) return name;
        }
        if (mimeType != null && mimeType.contains("png")) return "shared-image.png";
        if (mimeType != null && mimeType.contains("jpeg")) return "shared-image.jpg";
        return "shared-file";
    }
}
