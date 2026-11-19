package dev.aaow.rt;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ContentProvider hệ thống cho authority "media" (android.provider.MediaStore).
 * Ánh xạ các URI content://media/external/images/media, video/media, audio/media, downloads, file
 * trực tiếp sang các thư mục sdcard/Pictures, Movies, Music, Download, Documents trên Windows.
 */
public final class MediaStoreProvider extends ContentProvider {
    private static final String[] DEFAULT_PROJECTION = {
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.TITLE,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.RELATIVE_PATH
    };

    private record MediaEntry(long id, File file, String mimeType, String title, String relativePath) {
    }

    private final AtomicLong nextId = new AtomicLong(1);
    private final Map<Long, MediaEntry> entriesById = new ConcurrentHashMap<>();
    private final Map<String, Long> idByPath = new ConcurrentHashMap<>();

    @Override
    public boolean onCreate() {
        return true;
    }

    private File resolveBaseDirectory(Uri uri) {
        String path = uri != null && uri.getPath() != null ? uri.getPath().toLowerCase(Locale.ROOT) : "";
        String envType = Environment.DIRECTORY_PICTURES;
        if (path.contains("/video")) {
            envType = Environment.DIRECTORY_MOVIES;
        } else if (path.contains("/audio")) {
            envType = Environment.DIRECTORY_MUSIC;
        } else if (path.contains("/downloads")) {
            envType = Environment.DIRECTORY_DOWNLOADS;
        } else if (path.contains("/file")) {
            envType = Environment.DIRECTORY_DOCUMENTS;
        }
        File dir = Environment.getExternalStoragePublicDirectory(envType);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private void scanDirectoryIntoIndex(File dir, String relPrefix) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                scanDirectoryIntoIndex(f, relPrefix + f.getName() + "/");
            } else if (f.isFile()) {
                String abs = f.getAbsolutePath();
                idByPath.computeIfAbsent(abs, p -> {
                    long id = nextId.getAndIncrement();
                    String name = f.getName();
                    int dot = name.lastIndexOf('.');
                    String title = dot > 0 ? name.substring(0, dot) : name;
                    entriesById.put(id, new MediaEntry(id, f, guessMimeType(name), title, relPrefix));
                    return id;
                });
            }
        }
    }

    private static Long parseTrailingId(Uri uri) {
        if (uri == null) return null;
        String last = uri.getLastPathSegment();
        if (last == null) return null;
        try {
            return Long.parseLong(last);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        File baseDir = resolveBaseDirectory(uri);
        scanDirectoryIntoIndex(baseDir, baseDir.getName() + "/");

        String[] cols = (projection != null && projection.length > 0) ? projection : DEFAULT_PROJECTION;
        MatrixCursor cursor = new MatrixCursor(cols);

        Long targetId = parseTrailingId(uri);
        List<MediaEntry> list = new ArrayList<>();
        if (targetId != null) {
            MediaEntry single = entriesById.get(targetId);
            if (single != null && single.file.exists()) {
                list.add(single);
            }
        } else {
            for (MediaEntry e : entriesById.values()) {
                if (e.file.exists()) {
                    list.add(e);
                }
            }
        }

        for (MediaEntry entry : list) {
            Object[] row = new Object[cols.length];
            int width = 0;
            int height = 0;
            if (entry.mimeType != null && entry.mimeType.startsWith("image/")) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(entry.file.getAbsolutePath(), opts);
                width = Math.max(0, opts.outWidth);
                height = Math.max(0, opts.outHeight);
            }
            long modSec = entry.file.lastModified() / 1000L;
            for (int i = 0; i < cols.length; i++) {
                String c = cols[i];
                row[i] = switch (c) {
                    case MediaStore.MediaColumns._ID -> entry.id;
                    case MediaStore.MediaColumns.DISPLAY_NAME -> entry.file.getName();
                    case MediaStore.MediaColumns.TITLE -> entry.title;
                    case MediaStore.MediaColumns.DATA -> entry.file.getAbsolutePath().replace('\\', '/');
                    case MediaStore.MediaColumns.SIZE -> entry.file.length();
                    case MediaStore.MediaColumns.MIME_TYPE -> entry.mimeType;
                    case MediaStore.MediaColumns.WIDTH -> width;
                    case MediaStore.MediaColumns.HEIGHT -> height;
                    case MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.DATE_MODIFIED -> modSec;
                    case MediaStore.MediaColumns.DATE_TAKEN -> entry.file.lastModified();
                    case MediaStore.MediaColumns.RELATIVE_PATH -> entry.relativePath;
                    case MediaStore.MediaColumns.IS_PENDING, MediaStore.MediaColumns.IS_TRASHED -> 0;
                    default -> null;
                };
            }
            cursor.addRow(row);
        }
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        File baseDir = resolveBaseDirectory(uri);
        String relPath = values != null ? values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH) : null;
        File targetDir = baseDir;
        if (relPath != null && !relPath.isEmpty()) {
            String cleaned = relPath.replace('\\', '/');
            if (cleaned.startsWith(baseDir.getName() + "/")) {
                cleaned = cleaned.substring(baseDir.getName().length() + 1);
            }
            if (!cleaned.isEmpty()) {
                targetDir = new File(baseDir, cleaned);
            }
        }
        if (!targetDir.exists()) targetDir.mkdirs();

        String mime = values != null ? values.getAsString(MediaStore.MediaColumns.MIME_TYPE) : null;
        String displayName = values != null ? values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME) : null;
        String title = values != null ? values.getAsString(MediaStore.MediaColumns.TITLE) : null;

        if (displayName == null || displayName.isEmpty()) {
            String baseName = (title != null && !title.isEmpty()) ? title : ("MEDIA_" + System.currentTimeMillis());
            String ext = extensionForMime(mime);
            displayName = baseName.endsWith(ext) ? baseName : (baseName + ext);
        }
        displayName = displayName.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (title == null || title.isEmpty()) {
            int dot = displayName.lastIndexOf('.');
            title = dot > 0 ? displayName.substring(0, dot) : displayName;
        }
        if (mime == null || mime.isEmpty()) {
            mime = guessMimeType(displayName);
        }

        File targetFile = new File(targetDir, displayName);
        try {
            if (!targetFile.exists()) {
                Files.createFile(targetFile.toPath());
            }
        } catch (Exception ignored) {
        }

        long id = nextId.getAndIncrement();
        String rel = relPath != null ? relPath : (baseDir.getName() + "/");
        MediaEntry entry = new MediaEntry(id, targetFile, mime, title, rel);
        entriesById.put(id, entry);
        idByPath.put(targetFile.getAbsolutePath(), id);

        String baseUri = uri.toString();
        if (baseUri.endsWith("/")) baseUri = baseUri.substring(0, baseUri.length() - 1);
        return Uri.parse(baseUri + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Long id = parseTrailingId(uri);
        if (id != null) {
            MediaEntry removed = entriesById.remove(id);
            if (removed != null) {
                idByPath.remove(removed.file.getAbsolutePath());
                removed.file.delete();
                return 1;
            }
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Long id = parseTrailingId(uri);
        if (id != null && entriesById.containsKey(id)) {
            return 1;
        }
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        Long id = parseTrailingId(uri);
        if (id != null) {
            MediaEntry e = entriesById.get(id);
            if (e != null && e.mimeType != null) return e.mimeType;
        }
        String path = uri != null && uri.getPath() != null ? uri.getPath().toLowerCase(Locale.ROOT) : "";
        if (path.contains("/images")) return MediaStore.Images.Media.CONTENT_TYPE;
        if (path.contains("/video")) return MediaStore.Video.Media.CONTENT_TYPE;
        if (path.contains("/audio")) return MediaStore.Audio.Media.CONTENT_TYPE;
        if (path.contains("/downloads")) return MediaStore.Downloads.CONTENT_TYPE;
        return "application/octet-stream";
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        Long id = parseTrailingId(uri);
        if (id == null) {
            throw new FileNotFoundException("Missing media item ID in URI: " + uri);
        }
        MediaEntry entry = entriesById.get(id);
        if (entry == null) {
            throw new FileNotFoundException("Media item not found for URI: " + uri);
        }
        File parent = entry.file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        return ParcelFileDescriptor.open(entry.file, ParcelFileDescriptor.parseMode(mode));
    }

    private static String extensionForMime(String mime) {
        if (mime == null) return ".jpg";
        String lower = mime.toLowerCase(Locale.ROOT);
        if (lower.contains("png")) return ".png";
        if (lower.contains("webp")) return ".webp";
        if (lower.contains("gif")) return ".gif";
        if (lower.contains("mp4")) return ".mp4";
        if (lower.contains("mp3") || lower.contains("mpeg")) return ".mp3";
        if (lower.contains("wav")) return ".wav";
        if (lower.contains("txt") || lower.contains("plain")) return ".txt";
        if (lower.contains("json")) return ".json";
        return ".jpg";
    }

    private static String guessMimeType(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".txt") || lower.endsWith(".md")) return "text/plain";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }
}
