package android.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

/**
 * Cài đặt thật android.provider.MediaStore ánh xạ trực tiếp kho Media (Images, Video, Audio, Files, Downloads)
 * sang thư mục dữ liệu Windows thông qua MediaStoreProvider (content://media/...).
 */
public final class MediaStore {
    public static final String AUTHORITY = "media";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    public static final String VOLUME_INTERNAL = "internal";
    public static final String VOLUME_EXTERNAL = "external";
    public static final String VOLUME_EXTERNAL_PRIMARY = "external_primary";

    public static final String ACTION_IMAGE_CAPTURE = "android.media.action.IMAGE_CAPTURE";
    public static final String ACTION_IMAGE_CAPTURE_SECURE = "android.media.action.IMAGE_CAPTURE_SECURE";
    public static final String ACTION_VIDEO_CAPTURE = "android.media.action.VIDEO_CAPTURE";
    public static final String ACTION_REVIEW = "android.provider.action.REVIEW";
    public static final String ACTION_PICK_IMAGES = "android.provider.action.PICK_IMAGES";

    public static final String EXTRA_OUTPUT = "output";
    public static final String EXTRA_VIDEO_QUALITY = "android.intent.extra.videoQuality";
    public static final String EXTRA_SIZE_LIMIT = "android.intent.extra.sizeLimit";
    public static final String EXTRA_DURATION_LIMIT = "android.intent.extra.durationLimit";
    public static final String EXTRA_PICK_IMAGES_MAX = "android.provider.extra.PICK_IMAGES_MAX";

    public static final String UNKNOWN_STRING = "<unknown>";

    public MediaStore() {
    }

    public static Set<String> getExternalVolumeNames(Context context) {
        return Collections.singleton(VOLUME_EXTERNAL_PRIMARY);
    }

    public static Set<String> getRecentExternalVolumeNames(Context context) {
        return Collections.singleton(VOLUME_EXTERNAL_PRIMARY);
    }

    public static String getVolumeName(Uri uri) {
        if (uri == null) return VOLUME_EXTERNAL_PRIMARY;
        var segs = uri.getPathSegments();
        return !segs.isEmpty() ? segs.get(0) : VOLUME_EXTERNAL_PRIMARY;
    }

    public static String getVersion(Context context) {
        return "aaow-media-1.0";
    }

    public static String getVersion(Context context, String volumeName) {
        return "aaow-media-1.0";
    }

    public static int getPickImagesMaxLimit() {
        return 100;
    }

    public static boolean canManageMedia(Context context) {
        return true;
    }

    public static Uri setIncludePending(Uri uri) {
        return uri;
    }

    public static Uri setRequireOriginal(Uri uri) {
        return uri;
    }

    public static boolean getRequireOriginal(Uri uri) {
        return false;
    }

    public interface MediaColumns extends BaseColumns {
        String DATA = "_data";
        String SIZE = "_size";
        String DISPLAY_NAME = "_display_name";
        String TITLE = "title";
        String DATE_ADDED = "date_added";
        String DATE_MODIFIED = "date_modified";
        String DATE_TAKEN = "datetaken";
        String MIME_TYPE = "mime_type";
        String WIDTH = "width";
        String HEIGHT = "height";
        String ORIENTATION = "orientation";
        String DURATION = "duration";
        String ALBUM = "album";
        String ARTIST = "artist";
        String AUTHOR = "author";
        String BUCKET_ID = "bucket_id";
        String BUCKET_DISPLAY_NAME = "bucket_display_name";
        String RELATIVE_PATH = "relative_path";
        String IS_PENDING = "is_pending";
        String IS_FAVORITE = "is_favorite";
        String IS_TRASHED = "is_trashed";
        String IS_DOWNLOAD = "is_download";
        String VOLUME_NAME = "volume_name";
        String OWNER_PACKAGE_NAME = "owner_package_name";
    }

    public static final class Images {
        public Images() {
        }

        public interface ImageColumns extends MediaColumns {
            String DESCRIPTION = "description";
            String PICASA_ID = "picasa_id";
            String IS_PRIVATE = "isprivate";
            String LATITUDE = "latitude";
            String LONGITUDE = "longitude";
            String MINI_THUMB_MAGIC = "mini_thumb_magic";
            String EXPOSURE_TIME = "exposure_time";
            String F_NUMBER = "f_number";
            String ISO = "iso";
            String SCENE_CAPTURE_TYPE = "scene_capture_type";
        }

        public static final class Media implements ImageColumns {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/image";
            public static final String DEFAULT_SORT_ORDER = ImageColumns.BUCKET_DISPLAY_NAME;
            public static final Uri INTERNAL_CONTENT_URI = getContentUri(VOLUME_INTERNAL);
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri(VOLUME_EXTERNAL);

            public Media() {
            }

            public static Uri getContentUri(String volumeName) {
                return Uri.parse("content://" + AUTHORITY + "/" + (volumeName != null ? volumeName : VOLUME_EXTERNAL) + "/images/media");
            }

            public static Uri getContentUri(String volumeName, long id) {
                return Uri.parse(getContentUri(volumeName).toString() + "/" + id);
            }

            public static Bitmap getBitmap(ContentResolver cr, Uri url) throws FileNotFoundException, IOException {
                if (cr == null || url == null) throw new FileNotFoundException("Null ContentResolver or Uri");
                try (InputStream input = cr.openInputStream(url)) {
                    if (input == null) throw new FileNotFoundException("Cannot open stream for " + url);
                    return BitmapFactory.decodeStream(input);
                }
            }

            public static String insertImage(ContentResolver cr, String imagePath, String name, String description)
                    throws FileNotFoundException {
                if (imagePath == null) throw new FileNotFoundException("imagePath is null");
                File f = new File(imagePath);
                if (!f.exists()) throw new FileNotFoundException("File not found: " + imagePath);
                try (FileInputStream fis = new FileInputStream(f)) {
                    Bitmap bmp = BitmapFactory.decodeStream(fis);
                    if (bmp == null) throw new FileNotFoundException("Failed to decode bitmap: " + imagePath);
                    return insertImage(cr, bmp, name != null ? name : f.getName(), description);
                } catch (IOException e) {
                    throw new FileNotFoundException(e.getMessage());
                }
            }

            public static String insertImage(ContentResolver cr, Bitmap source, String title, String description) {
                if (cr == null || source == null) return null;
                ContentValues values = new ContentValues();
                String safeTitle = (title != null && !title.isEmpty()) ? title : ("IMG_" + System.currentTimeMillis());
                String displayName = safeTitle.toLowerCase().endsWith(".jpg") || safeTitle.toLowerCase().endsWith(".jpeg")
                        || safeTitle.toLowerCase().endsWith(".png")
                        ? safeTitle
                        : (safeTitle + ".jpg");
                values.put(DISPLAY_NAME, displayName);
                values.put(TITLE, safeTitle);
                if (description != null) {
                    values.put(DESCRIPTION, description);
                }
                values.put(MIME_TYPE, displayName.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg");
                values.put(WIDTH, source.getWidth());
                values.put(HEIGHT, source.getHeight());

                Uri uri = cr.insert(EXTERNAL_CONTENT_URI, values);
                if (uri == null) return null;
                try (OutputStream out = cr.openOutputStream(uri)) {
                    if (out == null) return null;
                    Bitmap.CompressFormat fmt = displayName.toLowerCase().endsWith(".png")
                            ? Bitmap.CompressFormat.PNG
                            : Bitmap.CompressFormat.JPEG;
                    source.compress(fmt, 92, out);
                    return uri.toString();
                } catch (Exception e) {
                    return null;
                }
            }

            public static Cursor query(ContentResolver cr, Uri uri, String[] projection) {
                return cr != null ? cr.query(uri, projection, null, null, DEFAULT_SORT_ORDER) : null;
            }

            public static Cursor query(ContentResolver cr, Uri uri, String[] projection, String where, String orderBy) {
                return cr != null ? cr.query(uri, projection, where, null, orderBy != null ? orderBy : DEFAULT_SORT_ORDER) : null;
            }

            public static Cursor query(ContentResolver cr, Uri uri, String[] projection,
                                       String selection, String[] selectionArgs, String orderBy) {
                return cr != null ? cr.query(uri, projection, selection, selectionArgs, orderBy != null ? orderBy : DEFAULT_SORT_ORDER) : null;
            }
        }
    }

    public static final class Video {
        public Video() {
        }

        public interface VideoColumns extends MediaColumns {
            String DESCRIPTION = "description";
            String IS_PRIVATE = "isprivate";
            String TAGS = "tags";
            String CATEGORY = "category";
            String LANGUAGE = "language";
            String LATITUDE = "latitude";
            String LONGITUDE = "longitude";
        }

        public static final class Media implements VideoColumns {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/video";
            public static final String DEFAULT_SORT_ORDER = VideoColumns.TITLE;
            public static final Uri INTERNAL_CONTENT_URI = getContentUri(VOLUME_INTERNAL);
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri(VOLUME_EXTERNAL);

            public Media() {
            }

            public static Uri getContentUri(String volumeName) {
                return Uri.parse("content://" + AUTHORITY + "/" + (volumeName != null ? volumeName : VOLUME_EXTERNAL) + "/video/media");
            }

            public static Uri getContentUri(String volumeName, long id) {
                return Uri.parse(getContentUri(volumeName).toString() + "/" + id);
            }
        }
    }

    public static final class Audio {
        public Audio() {
        }

        public interface AudioColumns extends MediaColumns {
            String IS_MUSIC = "is_music";
            String IS_PODCAST = "is_podcast";
            String IS_RINGTONE = "is_ringtone";
            String IS_ALARM = "is_alarm";
            String IS_NOTIFICATION = "is_notification";
            String TRACK = "track";
            String YEAR = "year";
        }

        public static final class Media implements AudioColumns {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/audio";
            public static final String DEFAULT_SORT_ORDER = AudioColumns.TITLE;
            public static final Uri INTERNAL_CONTENT_URI = getContentUri(VOLUME_INTERNAL);
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri(VOLUME_EXTERNAL);

            public Media() {
            }

            public static Uri getContentUri(String volumeName) {
                return Uri.parse("content://" + AUTHORITY + "/" + (volumeName != null ? volumeName : VOLUME_EXTERNAL) + "/audio/media");
            }

            public static Uri getContentUri(String volumeName, long id) {
                return Uri.parse(getContentUri(volumeName).toString() + "/" + id);
            }
        }
    }

    public static final class Files {
        public Files() {
        }

        public interface FileColumns extends MediaColumns {
            String PARENT = "parent";
            String MEDIA_TYPE = "media_type";
            int MEDIA_TYPE_NONE = 0;
            int MEDIA_TYPE_IMAGE = 1;
            int MEDIA_TYPE_AUDIO = 2;
            int MEDIA_TYPE_VIDEO = 3;
            int MEDIA_TYPE_PLAYLIST = 4;
            int MEDIA_TYPE_SUBTITLE = 5;
            int MEDIA_TYPE_DOCUMENT = 6;
        }

        public static Uri getContentUri(String volumeName) {
            return Uri.parse("content://" + AUTHORITY + "/" + (volumeName != null ? volumeName : VOLUME_EXTERNAL) + "/file");
        }

        public static Uri getContentUri(String volumeName, long rowId) {
            return Uri.parse(getContentUri(volumeName).toString() + "/" + rowId);
        }
    }

    public static final class Downloads implements MediaColumns {
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/download";
        public static final Uri INTERNAL_CONTENT_URI = getContentUri(VOLUME_INTERNAL);
        public static final Uri EXTERNAL_CONTENT_URI = getContentUri(VOLUME_EXTERNAL);
        public static final String DOWNLOAD_URI = "download_uri";
        public static final String REFERER_URI = "referer_uri";

        private Downloads() {
        }

        public static Uri getContentUri(String volumeName) {
            return Uri.parse("content://" + AUTHORITY + "/" + (volumeName != null ? volumeName : VOLUME_EXTERNAL) + "/downloads");
        }

        public static Uri getContentUri(String volumeName, long id) {
            return Uri.parse(getContentUri(volumeName).toString() + "/" + id);
        }
    }
}
