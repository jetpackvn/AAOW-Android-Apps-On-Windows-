package dev.aaow.launcher;

import android.app.ActivityManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.content.res.AssetFileDescriptor;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.AbstractWindowedCursor;
import android.database.CursorWindow;
import android.database.sqlite.SQLiteClosable;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteProgram;
import android.database.sqlite.SQLiteQuery;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Icon;
import android.media.AudioFormat;
import android.media.AudioRouting;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.VolumeAutomation;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.BaseBundle;
import android.os.Bundle;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentInfo;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.text.Editable;
import android.text.Spannable;
import android.util.AndroidException;
import android.util.Base64;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.os.Debug;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.StrictMode;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.util.LogPrinter;
import android.util.LruCache;
import android.util.Printer;
import android.view.Choreographer;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.AbsSeekBar;
import android.widget.AbsoluteLayout;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.SpinnerAdapter;
import android.widget.TableLayout;
import android.widget.TableRow;
import dev.aaow.rt.HostWindow;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class FrameworkHierarchyTest {

    @Test
    void coreWidgetAndEventHierarchiesMatchAndroidSdk() {
        assertTrue(AbsSeekBar.class.isAssignableFrom(SeekBar.class));
        assertTrue(AbsListView.class.isAssignableFrom(ListView.class));
        assertTrue(AbsoluteLayout.class.isAssignableFrom(WebView.class));
        assertTrue(InputEvent.class.isAssignableFrom(KeyEvent.class));
        assertTrue(InputEvent.class.isAssignableFrom(MotionEvent.class));
        assertTrue(PackageItemInfo.class.isAssignableFrom(ApplicationInfo.class));
        assertTrue(ComponentInfo.class.isAssignableFrom(ActivityInfo.class));
        assertTrue(ComponentInfo.class.isAssignableFrom(ServiceInfo.class));
        assertTrue(AndroidException.class.isAssignableFrom(PackageManager.NameNotFoundException.class));
        assertTrue(Checkable.class.isAssignableFrom(CompoundButton.class));
        assertTrue(SpinnerAdapter.class.isAssignableFrom(BaseAdapter.class));
        assertTrue(Spannable.class.isAssignableFrom(Editable.class));
        assertTrue(SQLiteClosable.class.isAssignableFrom(SQLiteDatabase.class));
        assertTrue(SQLiteClosable.class.isAssignableFrom(SQLiteProgram.class));
        assertTrue(SQLiteClosable.class.isAssignableFrom(CursorWindow.class));
        assertTrue(SQLiteProgram.class.isAssignableFrom(SQLiteStatement.class));
        assertTrue(SQLiteProgram.class.isAssignableFrom(SQLiteQuery.class));
        assertTrue(AbstractWindowedCursor.class.isAssignableFrom(SQLiteCursor.class));
        assertTrue(ContextWrapper.class.isAssignableFrom(Service.class));
        assertTrue(Service.class.isAssignableFrom(IntentService.class));
        assertTrue(AndroidException.class.isAssignableFrom(RemoteException.class));
        assertTrue(ComponentInfo.class.isAssignableFrom(ProviderInfo.class));
        assertTrue(android.text.ClipboardManager.class.isAssignableFrom(ClipboardManager.class));
        assertTrue(BaseBundle.class.isAssignableFrom(PersistableBundle.class));
        assertTrue(AudioRouting.class.isAssignableFrom(MediaPlayer.class));
        assertTrue(VolumeAutomation.class.isAssignableFrom(MediaPlayer.class));
        assertTrue(AudioRouting.class.isAssignableFrom(AudioTrack.class));
        assertTrue(VolumeAutomation.class.isAssignableFrom(AudioTrack.class));
        assertTrue(java.io.Closeable.class.isAssignableFrom(JsonReader.class));
        assertTrue(java.io.Closeable.class.isAssignableFrom(JsonWriter.class));
        assertTrue(Enum.class.isAssignableFrom(JsonToken.class));
        assertNotNull(Base64.class);
        assertNotNull(StatFs.class);
        assertNotNull(PowerManager.class);
        assertNotNull(StrictMode.class);
        assertTrue(java.io.FilterInputStream.class.isAssignableFrom(Base64InputStream.class));
        assertTrue(java.io.FilterOutputStream.class.isAssignableFrom(Base64OutputStream.class));
        assertTrue(Printer.class.isAssignableFrom(LogPrinter.class));
        assertTrue(LinearLayout.class.isAssignableFrom(TableLayout.class));
        assertTrue(LinearLayout.class.isAssignableFrom(TableRow.class));
        assertTrue(Window.Callback.class.isAssignableFrom(android.app.Dialog.class));
        assertNotNull(LruCache.class);
        assertNotNull(Choreographer.class);
        assertNotNull(Debug.class);

        Class<?>[] parcelables = {
                Intent.class, IntentFilter.class, Bundle.class, PersistableBundle.class,
                ResultReceiver.class, Uri.class, ComponentName.class,
                ContentValues.class, Bitmap.class, Rect.class, RectF.class, Configuration.class,
                PackageInfo.class, ActivityInfo.class, ApplicationInfo.class, ProviderInfo.class,
                ServiceInfo.class, ResolveInfo.class, Signature.class,
                ActivityManager.RunningAppProcessInfo.class, ActivityManager.MemoryInfo.class,
                Debug.MemoryInfo.class,
                ColorStateList.class, KeyEvent.class, MotionEvent.class,
                Message.class, ParcelFileDescriptor.class, AssetFileDescriptor.class,
                Network.class, NetworkInfo.class, NetworkCapabilities.class, NetworkRequest.class,
                ClipData.class, ClipDescription.class, Notification.class, Notification.Action.class,
                NotificationChannel.class, NotificationChannelGroup.class, PendingIntent.class,
                Icon.class, RemoteViews.class, AudioFormat.class,
                PrintAttributes.class, PageRange.class, PrintDocumentInfo.class,
                PrintJobId.class, PrintJobInfo.class
        };
        for (Class<?> cls : parcelables) {
            assertTrue(Parcelable.class.isAssignableFrom(cls), cls.getName() + " must implement Parcelable");
        }
    }

    @Test
    void allFrameworkClassesMatchAndroidJarSuperclass() throws Exception {
        Path sdkJar = AndroidSdkFallbackClassLoader.findSystemAndroidJarPath();
        if (sdkJar == null || !Files.exists(sdkJar)) {
            return; // Bỏ qua phần đối chiếu JAR nếu môi trường CI không cài Android SDK
        }

        URL loc = HostWindow.class.getProtectionDomain().getCodeSource().getLocation();
        assertNotNull(loc);
        Path fwLocation = Path.of(loc.toURI());

        List<String> mismatches = new ArrayList<>();
        try (ZipFile sdk = new ZipFile(sdkJar.toFile())) {
            if (Files.isDirectory(fwLocation)) {
                try (Stream<Path> walk = Files.walk(fwLocation)) {
                    for (Path p : walk.filter(f -> f.toString().endsWith(".class")).toList()) {
                        String rel = fwLocation.relativize(p).toString().replace('\\', '/');
                        checkClassSuper(rel, Files.readAllBytes(p), sdk, mismatches);
                    }
                }
            } else {
                try (ZipFile fwZip = new ZipFile(fwLocation.toFile())) {
                    Enumeration<? extends ZipEntry> entries = fwZip.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry e = entries.nextElement();
                        if (!e.getName().endsWith(".class")) continue;
                        try (InputStream in = fwZip.getInputStream(e)) {
                            checkClassSuper(e.getName(), in.readAllBytes(), sdk, mismatches);
                        }
                    }
                }
            }
        }
        assertTrue(mismatches.isEmpty(), "Phát hiện superclass lệch chuẩn Android SDK:\n" + String.join("\n", mismatches));
    }

    private static void checkClassSuper(String entryName, byte[] fwBytes, ZipFile sdk, List<String> mismatches) throws Exception {
        if (entryName.startsWith("dev/aaow/") || entryName.contains("$")) return;
        ZipEntry sdkEntry = sdk.getEntry(entryName);
        if (sdkEntry == null) return;

        String fwSuper = readSuper(fwBytes);
        String sdkSuper;
        try (InputStream in = sdk.getInputStream(sdkEntry)) {
            sdkSuper = readSuper(in.readAllBytes());
        }
        if (!Objects.equals(fwSuper, sdkSuper)) {
            mismatches.add(entryName + ": framework=" + fwSuper + " vs sdk=" + sdkSuper);
        }
    }

    private static String readSuper(byte[] bytes) {
        final String[] sup = {null};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                sup[0] = superName;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return sup[0];
    }
}
