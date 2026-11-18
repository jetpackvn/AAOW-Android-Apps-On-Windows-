package dev.aaow.launcher;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.Spannable;
import android.util.AndroidException;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.AbsSeekBar;
import android.widget.AbsoluteLayout;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SpinnerAdapter;
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

        Class<?>[] parcelables = {
                Intent.class, Bundle.class, Uri.class, ComponentName.class,
                Bitmap.class, Rect.class, RectF.class, Configuration.class,
                PackageInfo.class, ActivityInfo.class, ApplicationInfo.class,
                ColorStateList.class, KeyEvent.class, MotionEvent.class
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
