package dev.aaow.rt;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import javax.swing.JFileChooser;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * Cầu nối hộp thoại chọn/lưu file gốc của Windows (Win32 FileDialog / JFileChooser)
 * cho các Intent hệ thống: ACTION_GET_CONTENT, ACTION_OPEN_DOCUMENT,
 * ACTION_CREATE_DOCUMENT, ACTION_OPEN_DOCUMENT_TREE.
 */
public final class WindowsFilePickerBridge {
    private static volatile Function<Intent, Intent> testPickerOverride;

    private WindowsFilePickerBridge() {
    }

    public static void setTestPickerOverride(Function<Intent, Intent> handler) {
        testPickerOverride = handler;
    }

    public static boolean isFilePickerIntent(Intent intent) {
        if (intent == null) return false;
        Intent unwrapped = unwrapChooser(intent);
        String action = unwrapped.getAction();
        return Intent.ACTION_GET_CONTENT.equals(action)
                || Intent.ACTION_OPEN_DOCUMENT.equals(action)
                || Intent.ACTION_CREATE_DOCUMENT.equals(action)
                || Intent.ACTION_OPEN_DOCUMENT_TREE.equals(action)
                || Intent.ACTION_PICK.equals(action);
    }

    public static Intent unwrapChooser(Intent intent) {
        if (intent != null && Intent.ACTION_CHOOSER.equals(intent.getAction())) {
            Intent inner = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (inner != null) return inner;
        }
        return intent;
    }

    public static Intent pickFiles(Activity caller, Intent rawIntent) {
        Intent intent = unwrapChooser(rawIntent);
        if (intent == null) return null;

        if (testPickerOverride != null) {
            return testPickerOverride.apply(intent);
        }
        if (GraphicsEnvironment.isHeadless()) {
            return null;
        }

        String action = intent.getAction();
        String title = rawIntent.getStringExtra(Intent.EXTRA_TITLE);
        if (title == null || title.isEmpty()) {
            title = intent.getStringExtra(Intent.EXTRA_TITLE);
        }

        if (Intent.ACTION_OPEN_DOCUMENT_TREE.equals(action)) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle(title != null && !title.isEmpty() ? title : "Chọn thư mục");
            int res = chooser.showOpenDialog(null);
            if (res == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                Intent out = new Intent();
                out.setData(Uri.fromFile(chooser.getSelectedFile()));
                out.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                return out;
            }
            return null;
        }

        boolean saveMode = Intent.ACTION_CREATE_DOCUMENT.equals(action);
        boolean allowMultiple = !saveMode && intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);

        FileDialog fd = new FileDialog((Frame) null,
                title != null && !title.isEmpty() ? title : (saveMode ? "Lưu tệp" : "Chọn tệp"),
                saveMode ? FileDialog.SAVE : FileDialog.LOAD);
        fd.setMultipleMode(allowMultiple);
        if (saveMode && title != null && !title.isEmpty()) {
            fd.setFile(title);
        }

        Set<String> allowedExts = resolveAllowedExtensions(intent);
        if (!allowedExts.isEmpty()) {
            FilenameFilter filter = (dir, name) -> {
                int dot = name.lastIndexOf('.');
                if (dot < 0) return false;
                return allowedExts.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
            };
            fd.setFilenameFilter(filter);
        }

        fd.setVisible(true);
        File[] selected = fd.getFiles();
        if (selected == null || selected.length == 0) {
            String dir = fd.getDirectory();
            String file = fd.getFile();
            if (dir != null && file != null) {
                selected = new File[]{new File(dir, file)};
            }
        }
        if (selected == null || selected.length == 0) {
            return null;
        }

        Intent out = new Intent();
        out.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (selected.length == 1) {
            out.setData(Uri.fromFile(selected[0]));
        } else {
            out.setData(Uri.fromFile(selected[0]));
            ClipData clip = ClipData.newRawUri("Selected Files", Uri.fromFile(selected[0]));
            for (int i = 1; i < selected.length; i++) {
                clip.addItem(new ClipData.Item(Uri.fromFile(selected[i])));
            }
            out.setClipData(clip);
        }
        return out;
    }

    private static Set<String> resolveAllowedExtensions(Intent intent) {
        Set<String> exts = new LinkedHashSet<>();
        List<String> mimes = new ArrayList<>();
        if (intent.getType() != null) mimes.add(intent.getType());
        String[] extraMimes = intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES);
        if (extraMimes != null) {
            java.util.Collections.addAll(mimes, extraMimes);
        }
        for (String mime : mimes) {
            if (mime == null || mime.isEmpty() || "*/*".equals(mime)) continue;
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
            if (ext != null) exts.add(ext);
            if ("text/*".equalsIgnoreCase(mime)) {
                exts.add("txt");
                exts.add("md");
                exts.add("markdown");
                exts.add("json");
                exts.add("xml");
                exts.add("html");
                exts.add("csv");
            } else if ("image/*".equalsIgnoreCase(mime)) {
                exts.add("png");
                exts.add("jpg");
                exts.add("jpeg");
                exts.add("gif");
                exts.add("webp");
            } else if ("audio/*".equalsIgnoreCase(mime)) {
                exts.add("wav");
                exts.add("mp3");
                exts.add("ogg");
                exts.add("m4a");
            }
        }
        return exts;
    }
}
