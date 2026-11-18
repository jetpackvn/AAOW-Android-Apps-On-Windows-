package dev.aaow.rt;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import dev.aaow.apk.AndroidManifest;
import dev.aaow.apk.ResValue;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Cửa sổ Windows (Win32/Swing + Java2D) trực tiếp hiển thị và điều phối ứng dụng Android.
 * Không sử dụng máy ảo hay trình giả lập.
 */
public final class HostWindow {
    private static final int STATUS_BAR_H = 28;
    private static final int ACTION_BAR_H = 52;
    private static final int NAV_BAR_H = 44;
    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final AndroidRuntimeContext runtimeContext;
    private final Application application;
    private final Deque<Activity> activityStack = new ArrayDeque<>();

    private JFrame frame;
    private DeviceViewport viewport;
    private JPanel inspectorPanel;
    private JTextArea inspectorArea;
    private boolean inspectorVisible = false;
    private boolean exitProcessOnClose = true;
    private Runnable onWindowClosedCallback;

    private View focusedView;
    private AlertDialog activeDialog;
    private String toastMessage;
    private long toastExpireAt;
    private Timer toastTimer;

    private int themePrimaryColor = 0xFF3949AB;
    private int themeStatusBarColor = 0xFF283593;
    private boolean themeNoTitle = false;

    public HostWindow(AndroidRuntimeContext runtimeContext, Application application) {
        this.runtimeContext = runtimeContext;
        this.application = application;
        this.runtimeContext.setActivityStarter(this::startActivity);

        resolveThemeColors(runtimeContext.getManifest().themeRes);
        Toast.setHostToastBridge(this::showToast);
        AlertDialog.setHostDialogBridge(d -> {
            this.activeDialog = d;
            repaintViewport();
        });
    }

    private void resolveThemeColors(int themeRes) {
        if (themeRes == 0) return;
        var res = runtimeContext.getResources();
        ResValue cp = res.resolveStyleAttr(themeRes, 0x01010433); // android:colorPrimary
        if (cp != null && cp.isColor()) themePrimaryColor = cp.data();
        ResValue cpd = res.resolveStyleAttr(themeRes, 0x01010434); // android:colorPrimaryDark
        if (cpd != null && cpd.isColor()) themeStatusBarColor = cpd.data();
        else themeStatusBarColor = darken(themePrimaryColor, 0.82f);

        ResValue noTitle = res.resolveStyleAttr(themeRes, 0x01010056); // android:windowNoTitle
        if (noTitle != null && noTitle.data() != 0) themeNoTitle = true;
    }

    private static int darken(int color, float factor) {
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(Color.alpha(color), Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    public void setExitProcessOnClose(boolean exitProcessOnClose) {
        this.exitProcessOnClose = exitProcessOnClose;
    }

    public void setOnWindowClosedCallback(Runnable callback) {
        this.onWindowClosedCallback = callback;
    }

    public JFrame getFrame() {
        return frame;
    }

    public void bringToFront() {
        if (frame != null) {
            SwingUtilities.invokeLater(() -> {
                if (frame.getState() == JFrame.ICONIFIED) {
                    frame.setState(JFrame.NORMAL);
                }
                frame.toFront();
                frame.requestFocus();
            });
        }
    }

    public void forceStop() {
        if (frame != null) {
            SwingUtilities.invokeLater(() -> {
                cleanupAllActivities();
                frame.dispose();
            });
        }
    }

    private void cleanupAllActivities() {
        while (!activityStack.isEmpty()) {
            Activity act = activityStack.pop();
            try {
                act.performPause();
                act.performStop();
                act.performDestroy();
            } catch (Exception ignored) {
            }
        }
        try {
            application.onTerminate();
        } catch (Exception ignored) {
        }
        if (onWindowClosedCallback != null) {
            Runnable cb = onWindowClosedCallback;
            onWindowClosedCallback = null;
            cb.run();
        }
    }

    public void showWindow(String initialActivityClass) {
        SwingUtilities.invokeLater(() -> {
            String appLabel = runtimeContext.getManifest().label != null
                    ? runtimeContext.getManifest().label
                    : runtimeContext.getPackageName();
            frame = new JFrame(appLabel + " — AAOW");
            frame.setDefaultCloseOperation(exitProcessOnClose ? JFrame.EXIT_ON_CLOSE : JFrame.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    cleanupAllActivities();
                }
            });

            Bitmap iconBmp = runtimeContext.getManifest().iconRes != 0
                    ? runtimeContext.getResources().loadBitmap(runtimeContext.getManifest().iconRes)
                    : null;
            if (iconBmp != null) {
                frame.setIconImage(iconBmp.getAwtImage());
            }

            viewport = new DeviceViewport();
            viewport.setPreferredSize(new Dimension(420, 820));
            frame.add(viewport, BorderLayout.CENTER);

            inspectorPanel = buildInspectorPanel();
            inspectorPanel.setVisible(false);
            frame.add(inspectorPanel, BorderLayout.EAST);

            Intent mainIntent = new Intent(Intent.ACTION_MAIN);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            mainIntent.setClassName(runtimeContext.getPackageName(), initialActivityClass);
            startActivity(mainIntent);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            viewport.requestFocusInWindow();
        });
    }

    /** Chế độ kết xuất ảnh chụp màn hình tự động (kiểm thử trực tiếp từ CLI). */
    public void renderHeadlessScreenshot(String activityClass, File outFile, int width, int height) throws Exception {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mainIntent.setClassName(runtimeContext.getPackageName(), activityClass);
        viewport = new DeviceViewport();
        viewport.setSize(width, height);
        startActivity(mainIntent);

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        viewport.paintComponent(g2);
        g2.dispose();
        ImageIO.write(img, "png", outFile);
    }

    public void startActivity(Intent intent) {
        ComponentName cmp = intent != null ? intent.getComponent() : null;
        String rawCls = cmp != null ? cmp.getClassName() : runtimeContext.getManifest().launcherActivity;
        if (rawCls == null) {
            rawCls = "android.app.Activity";
        }
        final String clsName = rawCls.startsWith(".")
                ? runtimeContext.getPackageName() + rawCls
                : rawCls;

        Activity prev = activityStack.peek();
        if (prev != null) {
            try {
                prev.performPause();
            } catch (Throwable ignored) {
            }
        }

        Activity act = null;
        Throwable launchError = null;
        try {
            Class<?> cls = Class.forName(clsName, true, runtimeContext.getClassLoader());
            if (Activity.class.isAssignableFrom(cls)) {
                act = (Activity) cls.getDeclaredConstructor().newInstance();
            } else {
                act = new Activity();
                launchError = new IllegalStateException("Lớp " + clsName + " kế thừa từ nhánh Activity tuỳ chỉnh ("
                        + cls.getSuperclass().getName() + ") chưa ánh xạ trực tiếp sang android.app.Activity.");
            }
        } catch (Throwable t) {
            act = new Activity();
            launchError = t;
        }

        AndroidManifest.Activity actMeta = runtimeContext.getManifest().findActivity(clsName);
        String title = (actMeta != null && actMeta.label != null)
                ? actMeta.label
                : (runtimeContext.getManifest().label != null ? runtimeContext.getManifest().label : simpleName(clsName));
        if (actMeta != null && actMeta.themeRes != 0) {
            resolveThemeColors(actMeta.themeRes);
        }

        final Activity finalAct = act;
        try {
            finalAct.attach(runtimeContext, application, intent, title, () -> finishActivity(finalAct), this::repaintViewport);
        } catch (Throwable t) {
            if (launchError == null) launchError = t;
            Activity fallbackAct = new Activity();
            fallbackAct.attach(runtimeContext, application, intent, title, () -> finishActivity(fallbackAct), this::repaintViewport);
            act = fallbackAct;
        }
        final Activity activeAct = act;
        activeAct.getWindow().setStatusBarColor(themeStatusBarColor);
        activeAct.getWindow().getDecorView().setInvalidateCallback(this::repaintViewport);

        activityStack.push(activeAct);
        Log.i("AAOW-ActivityManager", "Starting Activity: " + clsName);

        if (launchError == null) {
            try {
                activeAct.performCreate(null);
                activeAct.performStart();
                activeAct.performResume();
            } catch (Throwable t) {
                launchError = t;
                Log.e("AAOW-ActivityManager", "Lỗi trong vòng đời Activity " + clsName + ": " + t, t);
            }
        } else {
            Log.e("AAOW-ActivityManager", "Lỗi nạp lớp Activity " + clsName + ": " + launchError, launchError);
        }

        FrameLayout decor = (FrameLayout) activeAct.getWindow().getDecorView();
        if (decor.getChildCount() == 0) {
            decor.addView(buildDiagnosticFallbackView(activeAct, clsName, launchError),
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        focusedView = null;
        repaintViewport();
    }

    private static String simpleName(String fqcn) {
        int dot = fqcn.lastIndexOf('.');
        return dot >= 0 ? fqcn.substring(dot + 1) : fqcn;
    }

    /**
     * Dựng màn hình chẩn đoán tương tác khi một Activity bên thứ 3 gặp lỗi JNI/SDK
     * hoặc dừng ở màn hình Splash rỗng.
     */
    private View buildDiagnosticFallbackView(Activity hostAct, String attemptedClass, Throwable error) {
        ScrollView scroll = new ScrollView(hostAct);
        scroll.setBackgroundColor(0xFFF4F6FB);
        scroll.setPadding(14, 14, 14, 14);

        LinearLayout root = new LinearLayout(hostAct);
        root.setOrientation(LinearLayout.VERTICAL);

        // Thẻ 1: Trạng thái khởi chạy Activity
        LinearLayout card1 = createDiagCard(hostAct);
        TextView tvHeader = new TextView(hostAct);
        tvHeader.setText("Báo cáo Tương thích AAOW");
        tvHeader.setTextSize(17f);
        tvHeader.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvHeader.setTextColor(0xFF283593);
        card1.addView(tvHeader);

        TextView tvTarget = new TextView(hostAct);
        tvTarget.setText("Activity: " + attemptedClass);
        tvTarget.setTextSize(12f);
        tvTarget.setTextColor(0xFF5A607F);
        tvTarget.setPadding(0, 4, 0, 8);
        card1.addView(tvTarget);

        TextView tvReason = new TextView(hostAct);
        tvReason.setBackgroundColor(error != null ? 0xFFFFEBEE : 0xFFFFF8E1);
        tvReason.setPadding(10, 8, 10, 8);
        tvReason.setTextSize(12f);
        tvReason.setTextColor(error != null ? 0xFFB71C1C : 0xFFE65100);
        if (error != null) {
            Throwable cause = error.getCause() != null ? error.getCause() : error;
            String where = cause.getStackTrace().length > 0 ? "\nTại: " + cause.getStackTrace()[0] : "";
            tvReason.setText("Dừng tại: " + cause.getClass().getSimpleName()
                    + "\nChi tiết: " + (cause.getMessage() != null ? cause.getMessage() : "(không có mô tả)")
                    + where);
        } else {
            tvReason.setText("Activity đã chạy xong onCreate() nhưng chưa gọi setContentView().\n"
                    + "Thường gặp ở SplashActivity đang chờ callback từ thư viện native (.so) hoặc mạng.");
        }
        card1.addView(tvReason);
        root.addView(card1, createCardLp());

        // Thẻ 2: Thống kê cầu nối JNI (.so) & De-Stubbed SDK Classes
        LinearLayout card2 = createDiagCard(hostAct);
        TextView tvJniTitle = new TextView(hostAct);
        tvJniTitle.setText("Cầu nối Native (.so) & Android SDK Tự động");
        tvJniTitle.setTextSize(14f);
        tvJniTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvJniTitle.setTextColor(0xFF1A1C29);
        card2.addView(tvJniTitle);

        var abis = runtimeContext.getApk().nativeAbis();
        var libs = NativeBridge.getInterceptedLibraries();
        var jniCalls = NativeBridge.getInterceptedNativeCalls();
        var stubs = NativeBridge.getStubbedClasses();

        StringBuilder diag = new StringBuilder();
        diag.append("• Kiến trúc .so trong APK : ").append(abis.isEmpty() ? "Không có" : abis).append('\n');
        diag.append("• Thư viện .so đã chặn   : ").append(libs.isEmpty() ? "0" : libs).append('\n');
        diag.append("• Lời gọi hàm JNI đã bọc : ").append(jniCalls.size()).append(" hàm\n");
        if (!jniCalls.isEmpty()) {
            int c = 0;
            for (String call : jniCalls) {
                diag.append("   - ").append(call).append('\n');
                if (++c >= 4) break;
            }
        }
        diag.append("• Class android.jar tự động gỡ Stub: ").append(stubs.size()).append(" class");

        TextView tvDiagBody = new TextView(hostAct);
        tvDiagBody.setText(diag.toString());
        tvDiagBody.setTextSize(12f);
        tvDiagBody.setTextColor(0xFF374151);
        tvDiagBody.setPadding(0, 6, 0, 0);
        card2.addView(tvDiagBody);
        root.addView(card2, createCardLp());

        // Thẻ 3: Bộ chọn thử nghiệm các Activity khác trong APK
        LinearLayout card3 = createDiagCard(hostAct);
        TextView tvSwitch = new TextView(hostAct);
        tvSwitch.setText("Thử mở trực tiếp các Activity khác trong APK ("
                + runtimeContext.getManifest().activities.size() + " Activity):");
        tvSwitch.setTextSize(14f);
        tvSwitch.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvSwitch.setTextColor(0xFF1A1C29);
        tvSwitch.setPadding(0, 0, 0, 8);
        card3.addView(tvSwitch);

        int shown = 0;
        for (AndroidManifest.Activity actItem : runtimeContext.getManifest().activities) {
            if (actItem.name == null || actItem.name.equals(attemptedClass)) continue;
            final String targetActName = actItem.name;
            Button btn = new Button(hostAct);
            btn.setText("Mở " + simpleName(targetActName) + " →");
            btn.setTextSize(12f);
            btn.setOnClickListener(v -> {
                Intent next = new Intent();
                next.setClassName(runtimeContext.getPackageName(), targetActName);
                startActivity(next);
            });
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 40);
            blp.setMargins(0, 0, 0, 6);
            card3.addView(btn, blp);
            if (++shown >= 12) break;
        }
        root.addView(card3, createCardLp());

        scroll.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private LinearLayout createDiagCard(Activity act) {
        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(14, 12, 14, 12);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(14f);
        bg.setStroke(1, 0xFFDCE1F0);
        card.setBackground(bg);
        return card;
    }

    private LinearLayout.LayoutParams createCardLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 12);
        return lp;
    }

    private void finishActivity(Activity act) {
        if (!activityStack.contains(act)) return;
        act.performPause();
        act.performStop();
        act.performDestroy();
        activityStack.remove(act);

        if (activityStack.isEmpty()) {
            Log.i("AAOW-ActivityManager", "Activity gốc đã đóng, kết thúc ứng dụng.");
            if (frame != null) {
                frame.dispose();
                if (exitProcessOnClose) {
                    System.exit(0);
                }
            }
        } else {
            Activity top = activityStack.peek();
            if (top != null) {
                top.performResume();
            }
            repaintViewport();
        }
    }

    private void showToast(String msg, int durationMs) {
        this.toastMessage = msg;
        this.toastExpireAt = System.currentTimeMillis() + durationMs;
        if (toastTimer != null) toastTimer.stop();
        toastTimer = new Timer(durationMs, e -> {
            if (System.currentTimeMillis() >= toastExpireAt) {
                toastMessage = null;
                repaintViewport();
            }
        });
        toastTimer.setRepeats(false);
        toastTimer.start();
        repaintViewport();
    }

    private void toggleInspector() {
        if (frame == null || inspectorPanel == null) return;
        inspectorVisible = !inspectorVisible;
        inspectorPanel.setVisible(inspectorVisible);
        if (inspectorVisible) {
            refreshInspectorText();
        }
        frame.pack();
        repaintViewport();
    }

    private JPanel buildInspectorPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setPreferredSize(new Dimension(360, 820));
        p.setBackground(new java.awt.Color(0x161822));
        p.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new java.awt.Color(0x2E3244)));

        inspectorArea = new JTextArea();
        inspectorArea.setEditable(false);
        inspectorArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        inspectorArea.setBackground(new java.awt.Color(0x161822));
        inspectorArea.setForeground(new java.awt.Color(0xE2E4F0));
        inspectorArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JScrollPane sp = new JScrollPane(inspectorArea);
        sp.setBorder( null);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private void refreshInspectorText() {
        if (!inspectorVisible || inspectorArea == null) return;
        StringBuilder sb = new StringBuilder();
        AndroidManifest mf = runtimeContext.getManifest();
        sb.append("=== AAOW LIVE INSPECTOR ===\n");
        sb.append("Package   : ").append(mf.packageName).append("\n");
        sb.append("Version   : ").append(mf.versionName).append(" (").append(mf.versionCode).append(")\n");
        sb.append("SDK       : min=").append(mf.minSdk).append(", target=").append(mf.targetSdk).append("\n");
        sb.append("Resources : ").append(runtimeContext.getResources().getTable().size()).append(" entries\n");
        Activity top = activityStack.peek();
        sb.append("Activity  : ").append(top != null ? top.getClass().getName() : "<none>").append("\n");
        sb.append("Stack     : ").append(activityStack.size()).append(" activity\n\n");
        sb.append("=== CÂY GIAO DIỆN ANDROID (VIEW TREE) ===\n");
        if (top != null) {
            dumpViewTree(top.getWindow().getDecorView(), 0, sb);
        }
        inspectorArea.setText(sb.toString());
    }

    private void dumpViewTree(View v, int depth, StringBuilder sb) {
        if (v == null) return;
        sb.append("  ".repeat(depth))
                .append(v.getClass().getSimpleName());
        if (v.getId() != View.NO_ID) {
            try {
                String rn = runtimeContext.getResources().getResourceEntryName(v.getId());
                sb.append(" @id/").append(rn);
            } catch (Exception e) {
                sb.append(String.format(" #0x%08x", v.getId()));
            }
        }
        sb.append(String.format(" [%d,%d %dx%d]", v.getLeft(), v.getTop(), v.getWidth(), v.getHeight()));
        if (v instanceof TextView tv && tv.getText() != null && tv.getText().length() > 0) {
            String t = tv.getText().toString().replace("\n", " ");
            if (t.length() > 24) t = t.substring(0, 24) + "…";
            sb.append(" \"").append(t).append("\"");
        }
        sb.append('\n');
        if (v instanceof ViewGroup vg) {
            for (int i = 0; i < vg.getChildCount(); i++) {
                dumpViewTree(vg.getChildAt(i), depth + 1, sb);
            }
        }
    }

    public void repaintViewport() {
        if (viewport != null) {
            if (inspectorVisible) refreshInspectorText();
            viewport.repaint();
        }
    }

    // --------------------------------------------------------- Khung vẽ chính của thiết bị

    private final class DeviceViewport extends JPanel {
        private long touchDownTime;

        DeviceViewport() {
            setFocusable(true);
            setBackground(java.awt.Color.WHITE);

            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    handleMouse(MotionEvent.ACTION_DOWN, e.getX(), e.getY());
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    handleMouse(MotionEvent.ACTION_MOVE, e.getX(), e.getY());
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    handleMouse(MotionEvent.ACTION_UP, e.getX(), e.getY());
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    Activity act = activityStack.peek();
                    if (act == null || activeDialog != null) return;
                    int contentTop = contentTopOffset(act);
                    float cy = e.getY() - contentTop;
                    if (cy >= 0 && cy < contentHeight(act)) {
                        act.getWindow().getDecorView().onMouseWheel(e.getWheelRotation(), e.getX(), cy);
                    }
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    if (focusedView != null) {
                        focusedView.onHostKeyTyped(e.getKeyChar());
                    }
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_F12) {
                        toggleInspector();
                        return;
                    }
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        if (activeDialog != null) {
                            activeDialog.cancel();
                            return;
                        }
                        Activity act = activityStack.peek();
                        if (act != null) {
                            act.onBackPressed();
                        }
                        return;
                    }
                    if (focusedView != null) {
                        focusedView.onHostKeyPressed(e);
                        return;
                    }
                    Activity act = activityStack.peek();
                    if (act != null && activeDialog == null) {
                        int androidCode = switch (e.getKeyCode()) {
                            case KeyEvent.VK_UP, KeyEvent.VK_W -> android.view.KeyEvent.KEYCODE_DPAD_UP;
                            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> android.view.KeyEvent.KEYCODE_DPAD_DOWN;
                            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> android.view.KeyEvent.KEYCODE_DPAD_LEFT;
                            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
                            case KeyEvent.VK_ENTER -> android.view.KeyEvent.KEYCODE_ENTER;
                            default -> 0;
                        };
                        if (androidCode != 0) {
                            act.onKeyDown(androidCode, new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, androidCode));
                            repaint();
                        }
                    }
                }
            });
        }

        private int contentTopOffset(Activity act) {
            boolean showAb = isActionBarVisible(act);
            return STATUS_BAR_H + (showAb ? ACTION_BAR_H : 0);
        }

        private int contentHeight(Activity act) {
            return Math.max(0, getHeight() - contentTopOffset(act) - NAV_BAR_H);
        }

        private boolean isActionBarVisible(Activity act) {
            if (act == null || themeNoTitle) return false;
            ActionBar ab = act.getActionBar();
            return ab != null && ab.isShowing();
        }

        private void handleMouse(int action, int mx, int my) {
            Activity act = activityStack.peek();
            if (act == null) return;

            int w = getWidth();
            int h = getHeight();

            // 1. Nếu đang mở AlertDialog thì xử lý click nút trên hộp thoại
            if (activeDialog != null) {
                if (action == MotionEvent.ACTION_UP) {
                    handleDialogClick(mx, my, w, h);
                }
                return;
            }

            // 2. Thanh điều hướng dưới cùng (Back / Home / Inspector)
            if (my >= h - NAV_BAR_H) {
                if (action == MotionEvent.ACTION_UP) {
                    int third = w / 3;
                    if (mx < third) {
                        act.onBackPressed();
                    } else if (mx < third * 2) {
                        // Nút Home: quay về Activity gốc
                        while (activityStack.size() > 1) {
                            Activity top = activityStack.peek();
                            if (top != null) top.finish();
                        }
                        showToast("Đã về màn hình chính của ứng dụng", 1500);
                    } else {
                        toggleInspector();
                    }
                }
                return;
            }

            // 3. Sự kiện chạm trong vùng decorView của Activity
            int contentTop = contentTopOffset(act);
            if (my < contentTop) return;

            float vx = mx;
            float vy = my - contentTop;
            long now = SystemClock.uptimeMillis();
            if (action == MotionEvent.ACTION_DOWN) {
                touchDownTime = now;
                View hit = findFocusableAt(act.getWindow().getDecorView(), vx, vy);
                setFocusedView(hit);
            }

            MotionEvent ev = new MotionEvent(action, vx, vy, mx, my, touchDownTime, now);
            act.getWindow().getDecorView().dispatchTouchEvent(ev);
            repaint();
        }

        private void setFocusedView(View v) {
            if (focusedView == v) return;
            if (focusedView != null) focusedView.setFocusedInternal(false);
            focusedView = v;
            if (focusedView != null) focusedView.setFocusedInternal(true);
        }

        private View findFocusableAt(View root, float x, float y) {
            if (root == null || root.getVisibility() != View.VISIBLE || !root.isEnabled()) return null;
            if (root instanceof ViewGroup vg) {
                float sx = x + vg.getScrollX();
                float sy = y + vg.getScrollY();
                for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                    View c = vg.getChildAt(i);
                    float cx = sx - c.getLeft();
                    float cy = sy - c.getTop();
                    if (cx >= 0 && cx < c.getWidth() && cy >= 0 && cy < c.getHeight()) {
                        View found = findFocusableAt(c, cx, cy);
                        if (found != null) return found;
                    }
                }
            }
            return root instanceof EditText || root.isFocusable() ? root : null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int w = getWidth();
            int h = getHeight();
            Activity act = activityStack.peek();

            // Nền mặc định sáng sạch của Android
            g2.setColor(new java.awt.Color(0xFAFAFC));
            g2.fillRect(0, 0, w, h);

            if (act != null) {
                runtimeContext.getResources().getDisplayMetrics().widthPixels = w;
                runtimeContext.getResources().getDisplayMetrics().heightPixels = contentHeight(act);

                drawStatusBar(g2, w, act);
                int topY = STATUS_BAR_H;
                if (isActionBarVisible(act)) {
                    drawActionBar(g2, w, topY, act);
                    topY += ACTION_BAR_H;
                }

                // Đo, dàn trang và vẽ decorView của Activity
                int ch = Math.max(0, h - topY - NAV_BAR_H);
                View decor = act.getWindow().getDecorView();
                int wSpec = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY);
                int hSpec = View.MeasureSpec.makeMeasureSpec(ch, View.MeasureSpec.EXACTLY);
                decor.measure(wSpec, hSpec);
                decor.layout(0, 0, w, ch);

                Graphics2D contentG = (Graphics2D) g2.create(0, topY, w, ch);
                Canvas androidCanvas = new Canvas(contentG);
                decor.draw(androidCanvas);
                contentG.dispose();

                if (activeDialog != null) {
                    drawAlertDialog(g2, w, h, activeDialog);
                }
                if (toastMessage != null) {
                    drawToastOverlay(g2, w, h - NAV_BAR_H, toastMessage);
                }
            }

            drawNavigationBar(g2, w, h);
            g2.dispose();
        }

        private void drawStatusBar(Graphics2D g2, int w, Activity act) {
            int sbColor = act.getWindow().getStatusBarColor();
            g2.setColor(Color.toAwt(sbColor));
            g2.fillRect(0, 0, w, STATUS_BAR_H);

            g2.setColor(java.awt.Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.drawString(LocalTime.now().format(CLOCK_FMT), 14, 19);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            String badge = "AAOW Direct • No VM";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(badge, w - fm.stringWidth(badge) - 14, 19);
        }

        private void drawActionBar(Graphics2D g2, int w, int topY, Activity act) {
            g2.setColor(Color.toAwt(themePrimaryColor));
            g2.fillRect(0, topY, w, ACTION_BAR_H);

            int textX = 18;
            if (runtimeContext.getManifest().iconRes != 0) {
                Drawable icon = runtimeContext.getResources().getDrawable(runtimeContext.getManifest().iconRes);
                if (icon != null) {
                    Canvas c = new Canvas(g2);
                    int iy = topY + (ACTION_BAR_H - 28) / 2;
                    icon.setBounds(14, iy, 14 + 28, iy + 28);
                    icon.draw(c);
                    textX = 52;
                }
            }

            ActionBar ab = act.getActionBar();
            String title = ab != null && ab.getTitle().length() > 0
                    ? ab.getTitle().toString()
                    : act.getTitle().toString();
            g2.setColor(java.awt.Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 17));
            g2.drawString(title, textX, topY + 32);
        }

        private void drawAlertDialog(Graphics2D g2, int w, int h, AlertDialog d) {
            g2.setColor(new java.awt.Color(0, 0, 0, 135));
            g2.fillRect(0, 0, w, h);

            int dw = Math.min(w - 48, 340);
            int dh = 180;
            int dx = (w - dw) / 2;
            int dy = (h - dh) / 2;

            g2.setColor(java.awt.Color.WHITE);
            g2.fill(new RoundRectangle2D.Float(dx, dy, dw, dh, 24, 24));

            g2.setColor(new java.awt.Color(0x1C1B1F));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 17));
            g2.drawString(d.getTitleText().toString(), dx + 22, dy + 36);

            g2.setColor(new java.awt.Color(0x49454F));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            String msg = d.getMessageText().toString();
            String[] lines = msg.split("\n");
            int ly = dy + 68;
            for (String line : lines) {
                g2.drawString(line, dx + 22, ly);
                ly += 20;
            }

            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.setColor(new java.awt.Color(0x4F5BD5));
            int btnY = dy + dh - 22;
            if (d.getPositiveText() != null) {
                String p = d.getPositiveText().toString();
                int pw = g2.getFontMetrics().stringWidth(p);
                g2.drawString(p, dx + dw - 24 - pw, btnY);
            }
            if (d.getNegativeText() != null) {
                String n = d.getNegativeText().toString();
                int nw = g2.getFontMetrics().stringWidth(n);
                g2.drawString(n, dx + dw - 110 - nw, btnY);
            }
        }

        private void handleDialogClick(int mx, int my, int w, int h) {
            if (activeDialog == null) return;
            int dw = Math.min(w - 48, 340);
            int dh = 180;
            int dx = (w - dw) / 2;
            int dy = (h - dh) / 2;

            if (mx < dx || mx > dx + dw || my < dy || my > dy + dh) {
                activeDialog.cancel();
                return;
            }
            if (my >= dy + dh - 52) {
                AlertDialog d = activeDialog;
                if (mx >= dx + dw - 100 && d.getPositiveText() != null) {
                    if (d.getPositiveListener() != null) {
                        d.getPositiveListener().onClick(d, DialogInterface.BUTTON_POSITIVE);
                    }
                    d.dismiss();
                } else if (d.getNegativeText() != null) {
                    if (d.getNegativeListener() != null) {
                        d.getNegativeListener().onClick(d, DialogInterface.BUTTON_NEGATIVE);
                    }
                    d.dismiss();
                }
            }
        }

        private void drawToastOverlay(Graphics2D g2, int w, int bottomY, String msg) {
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            FontMetrics fm = g2.getFontMetrics();
            int tw = Math.min(w - 40, fm.stringWidth(msg) + 36);
            int th = 38;
            int tx = (w - tw) / 2;
            int ty = bottomY - th - 20;

            g2.setColor(new java.awt.Color(0xE6303036, true));
            g2.fill(new RoundRectangle2D.Float(tx, ty, tw, th, 19, 19));

            g2.setColor(java.awt.Color.WHITE);
            g2.drawString(msg, tx + (tw - fm.stringWidth(msg)) / 2, ty + 24);
        }

        private void drawNavigationBar(Graphics2D g2, int w, int h) {
            int top = h - NAV_BAR_H;
            g2.setColor(new java.awt.Color(0x14151B));
            g2.fillRect(0, top, w, NAV_BAR_H);

            g2.setColor(new java.awt.Color(0xD8DAE8));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cy = top + NAV_BAR_H / 2;

            // Nút Back (tam giác trái)
            int bx = w / 6;
            g2.drawLine(bx + 6, cy - 7, bx - 6, cy);
            g2.drawLine(bx - 6, cy, bx + 6, cy + 7);
            g2.drawLine(bx + 6, cy - 7, bx + 6, cy + 7);

            // Nút Home (hình tròn giữa)
            int hx = w / 2;
            g2.drawOval(hx - 8, cy - 8, 16, 16);

            // Nút Inspector / Overview (hình vuông phải)
            int rx = w * 5 / 6;
            if (inspectorVisible) {
                g2.setColor(new java.awt.Color(0x7C8CFF));
            }
            g2.drawRoundRect(rx - 8, cy - 8, 16, 16, 4, 4);
        }
    }
}
