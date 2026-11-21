package dev.aaow.rt;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import dev.aaow.apk.AndroidManifest;
import dev.aaow.apk.ResValue;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
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
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cửa sổ Windows (Win32/Swing + Java2D) trực tiếp hiển thị và điều phối ứng
 * dụng Android.
 * Không sử dụng máy ảo hay trình giả lập.
 */
public final class HostWindow {
    private static final int STATUS_BAR_H = 28;
    private static final int ACTION_BAR_H = 52;
    private static final int NAV_BAR_H = 44;
    private static final int DEFAULT_PORTRAIT_W = 420;
    private static final int DEFAULT_PORTRAIT_H = 820;
    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // Bộ đệm Font, Stroke & Color tái sử dụng giữa các khung hình (giảm cấp phát ở
    // 60fps)
    private static final Font FONT_STATUS_TIME = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font FONT_STATUS_BADGE = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_ACTION_TITLE = new Font("Segoe UI", Font.BOLD, 17);
    private static final Font FONT_ACTION_BTN = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font FONT_MENU_ITEM = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_DIALOG_TITLE = new Font("Segoe UI", Font.BOLD, 17);
    private static final Font FONT_DIALOG_BODY = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_DIALOG_BTN = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_TOAST = new Font("Segoe UI", Font.PLAIN, 13);
    private static final BasicStroke STROKE_AB_BACK = new BasicStroke(2.2f, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND);
    private static final BasicStroke STROKE_NAV = new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final java.awt.Color COL_VIEWPORT_BG = new java.awt.Color(0xFAFAFC);
    private static final java.awt.Color COL_NAV_BG = new java.awt.Color(0x14151B);
    private static final java.awt.Color COL_NAV_FG = new java.awt.Color(0xD8DAE8);
    private static final java.awt.Color COL_NAV_ACTIVE = new java.awt.Color(0x7C8CFF);
    private static final java.awt.Color COL_STATUS_DARK = new java.awt.Color(0x1C1B1F);
    private static final java.awt.Color COL_TOAST_BG = new java.awt.Color(0xE6303036, true);

    public record FrameStats(
            int fps,
            double lastFrameMs,
            long totalFrames,
            long layoutPasses,
            long layoutSkips,
            long heapUsedMb,
            double cacheHitPercent,
            int logicalWidth,
            int logicalHeight,
            boolean landscape,
            float zoomScale) {
    }

    private final AndroidRuntimeContext runtimeContext;
    private final Application application;
    private final Deque<Activity> activityStack = new ArrayDeque<>();
    private final Map<Activity, PendingResultCaller> pendingResults = new IdentityHashMap<>();

    private record PendingResultCaller(Activity caller, int requestCode) {
    }

    private JFrame frame;
    private DeviceViewport viewport;
    private JPanel inspectorPanel;
    private JTextArea inspectorArea;
    private JButton rotateButton;
    private JComboBox<String> zoomCombo;
    private JLabel perfLabel;
    private Timer perfStatsTimer;
    private Timer inspectorThrottleTimer;
    private long lastInspectorRefreshMs;
    private boolean inspectorVisible = false;
    private boolean exitProcessOnClose = true;
    private Runnable onWindowClosedCallback;

    private boolean landscape = false;
    private float viewportZoom = 0f; // 0f = Tự do (Responsive 1:1 theo kích thước cửa sổ)
    private int logicalWidth = DEFAULT_PORTRAIT_W;
    private int logicalHeight = DEFAULT_PORTRAIT_H;
    private boolean updatingZoomCombo = false;

    private Activity lastLayoutActivity;
    private int lastLayoutW = -1;
    private int lastLayoutH = -1;
    private boolean forceNextLayout = true;

    private long frameWindowStartNs = System.nanoTime();
    private int framesInWindow = 0;
    private volatile int currentFps = 0;
    private volatile double lastFrameMs = 0.0;
    private volatile long totalFrames = 0;
    private volatile long layoutPasses = 0;
    private volatile long layoutSkips = 0;

    private View focusedView;
    private final List<Dialog> activeDialogs = new ArrayList<>();
    private int lastCustomDialogX, lastCustomDialogY, lastCustomDialogW, lastCustomDialogH;
    private PopupWindow activePopup;
    private SimpleMenu activeOptionsMenu;
    private final List<SimpleMenu.SimpleMenuItem> activeOverflowItems = new ArrayList<>();
    private final List<ActionBarHit> actionBarHits = new ArrayList<>();
    private boolean showingDiagnosticFallback = false;
    private String toastMessage;
    private long toastExpireAt;
    private Timer toastTimer;

    private record ActionBarHit(int left, int right, boolean isHome, boolean isOverflow,
            SimpleMenu.SimpleMenuItem item) {
    }

    private int themePrimaryColor = 0xFF3949AB;
    private int themeStatusBarColor = 0xFF283593;
    private boolean themeNoTitle = false;
    private Drawable cachedAppIconDrawable;

    public HostWindow(AndroidRuntimeContext runtimeContext, Application application) {
        this.runtimeContext = runtimeContext;
        this.application = application;
        this.runtimeContext.setActivityStarter(this::startActivity);
        this.runtimeContext.setActivityForResultStarter(this::startActivityForResult);

        resolveThemeColors(runtimeContext.getManifest().themeRes);
        if (runtimeContext.getManifest().iconRes != 0) {
            this.cachedAppIconDrawable = runtimeContext.getResources()
                    .getDrawable(runtimeContext.getManifest().iconRes);
        }
        Toast.setHostToastBridge(this::showToast);
        Dialog.setHostDialogLifecycleBridge((dialog, shown) -> {
            if (dialog == null)
                return;
            if (Boolean.TRUE.equals(shown)) {
                activeDialogs.remove(dialog);
                activeDialogs.add(dialog);
                if (dialog.getWindow() != null && dialog.getWindow().getDecorView() != null) {
                    dialog.getWindow().getDecorView().setInvalidateCallback(this::repaintViewport);
                }
            } else {
                activeDialogs.remove(dialog);
                if (activeDialogs.isEmpty()) {
                    Activity top = activityStack.peek();
                    if (top != null && top.getWindow() != null && top.getWindow().getDecorView() != null) {
                        top.getWindow().getDecorView().dispatchWindowFocusChanged(true);
                    }
                }
            }
            repaintViewport();
        });
        AlertDialog.setHostDialogBridge(null);
        PopupWindow.setHostPopupBridge(pw -> {
            this.activePopup = pw;
            repaintViewport();
        });
    }

    public Dialog getActiveDialog() {
        return activeDialogs.isEmpty() ? null : activeDialogs.get(activeDialogs.size() - 1);
    }

    private static boolean usesNativeAlertOverlay(Dialog d) {
        if (!(d instanceof AlertDialog))
            return false;
        if (d.getWindow() == null || !(d.getWindow().getDecorView() instanceof ViewGroup vg))
            return true;
        return vg.getChildCount() == 0;
    }

    private void resolveThemeColors(int themeRes) {
        if (themeRes == 0)
            return;
        var res = runtimeContext.getResources();
        ResValue cp = res.resolveStyleAttr(themeRes, 0x01010433); // android:colorPrimary
        if (cp != null && cp.isColor())
            themePrimaryColor = cp.data();
        ResValue cpd = res.resolveStyleAttr(themeRes, 0x01010434); // android:colorPrimaryDark
        if (cpd != null && cpd.isColor())
            themeStatusBarColor = cpd.data();
        else
            themeStatusBarColor = darken(themePrimaryColor, 0.82f);

        ResValue noTitle = res.resolveStyleAttr(themeRes, 0x01010056); // android:windowNoTitle
        if (noTitle != null && noTitle.data() != 0)
            themeNoTitle = true;
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
        if (perfStatsTimer != null) {
            perfStatsTimer.stop();
            perfStatsTimer = null;
        }
        if (inspectorThrottleTimer != null) {
            inspectorThrottleTimer.stop();
            inspectorThrottleTimer = null;
        }
        if (toastTimer != null) {
            toastTimer.stop();
            toastTimer = null;
        }
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
            runtimeContext.stopAllServices();
        } catch (Exception ignored) {
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

    public Activity getCurrentActivity() {
        return activityStack.peek();
    }

    public boolean isLandscape() {
        return landscape;
    }

    public float getViewportZoom() {
        return viewportZoom;
    }

    public FrameStats getFrameStats() {
        Runtime rt = Runtime.getRuntime();
        long usedMb = Math.max(0L, (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L));
        double cacheHit = runtimeContext.getResources().getCacheStats().hitRatioPercent();
        return new FrameStats(
                currentFps,
                lastFrameMs,
                totalFrames,
                layoutPasses,
                layoutSkips,
                usedMb,
                cacheHit,
                logicalWidth,
                logicalHeight,
                landscape,
                viewportZoom <= 0f ? 1.0f : viewportZoom);
    }

    public void forceRelayout() {
        forceNextLayout = true;
        Activity top = activityStack.peek();
        if (top != null) {
            top.getWindow().getDecorView().requestLayout();
        }
        repaintViewport();
    }

    public void setOrientationLandscape(boolean targetLandscape) {
        if (this.landscape == targetLandscape)
            return;
        this.landscape = targetLandscape;
        if (viewportZoom <= 0f) {
            int curW = viewport != null && viewport.getWidth() > 0 ? viewport.getWidth() : logicalWidth;
            int curH = viewport != null && viewport.getHeight() > 0 ? viewport.getHeight() : logicalHeight;
            int newW = targetLandscape ? Math.max(curW, curH) : Math.min(curW, curH);
            int newH = targetLandscape ? Math.min(curW, curH) : Math.max(curW, curH);
            if (newW == newH) {
                newW = targetLandscape ? DEFAULT_PORTRAIT_H : DEFAULT_PORTRAIT_W;
                newH = targetLandscape ? DEFAULT_PORTRAIT_W : DEFAULT_PORTRAIT_H;
            }
            logicalWidth = newW;
            logicalHeight = newH;
        } else {
            logicalWidth = targetLandscape ? DEFAULT_PORTRAIT_H : DEFAULT_PORTRAIT_W;
            logicalHeight = targetLandscape ? DEFAULT_PORTRAIT_W : DEFAULT_PORTRAIT_H;
        }
        applyWindowViewportSize();
        syncConfigurationAndRelayout(targetLandscape, true);
        updateControlBarLabels();
    }

    public void setViewportZoom(float zoom) {
        float cleanZoom = zoom <= 0f ? 0f : Math.max(0.5f, Math.min(2.5f, zoom));
        if (Float.compare(this.viewportZoom, cleanZoom) == 0)
            return;
        this.viewportZoom = cleanZoom;
        if (cleanZoom > 0f) {
            logicalWidth = landscape ? DEFAULT_PORTRAIT_H : DEFAULT_PORTRAIT_W;
            logicalHeight = landscape ? DEFAULT_PORTRAIT_W : DEFAULT_PORTRAIT_H;
        }
        applyWindowViewportSize();
        syncConfigurationAndRelayout(landscape, true);
        updateControlBarLabels();
    }

    public void resizeLogicalViewport(int width, int height) {
        this.logicalWidth = Math.max(240, width);
        this.logicalHeight = Math.max(240, height);
        boolean isLand = this.logicalWidth > this.logicalHeight;
        if (viewport != null && viewportZoom <= 0f) {
            viewport.setSize(this.logicalWidth, this.logicalHeight);
            viewport.setPreferredSize(new Dimension(this.logicalWidth, this.logicalHeight));
        }
        syncConfigurationAndRelayout(isLand, true);
        updateControlBarLabels();
    }

    private void applyWindowViewportSize() {
        if (viewport == null)
            return;
        float scale = viewportZoom <= 0f ? 1.0f : viewportZoom;
        int prefW = Math.max(240, Math.round(logicalWidth * scale));
        int prefH = Math.max(240, Math.round(logicalHeight * scale));
        viewport.setPreferredSize(new Dimension(prefW, prefH));
        if (frame != null) {
            boolean wasMaximized = (frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
            if (!wasMaximized) {
                frame.pack();
                frame.setLocationRelativeTo(null);
            } else {
                frame.revalidate();
            }
        } else {
            viewport.setSize(prefW, prefH);
        }
    }

    private void syncConfigurationAndRelayout(boolean targetLandscape, boolean forceLayout) {
        this.landscape = targetLandscape;
        Activity top = activityStack.peek();
        int topOff = viewport != null ? viewport.contentTopOffset(top) : (STATUS_BAR_H + ACTION_BAR_H);
        int ch = Math.max(0, logicalHeight - topOff - NAV_BAR_H);

        var res = runtimeContext.getResources();
        boolean orientationChanged = res.updateViewportMetrics(logicalWidth, ch, targetLandscape);
        if (runtimeContext.getDisplay() != null) {
            runtimeContext.getDisplay().setRotation(
                    targetLandscape ? Surface.ROTATION_90 : Surface.ROTATION_0);
        }
        if (orientationChanged) {
            Configuration cfg = new Configuration(res.getConfiguration());
            try {
                application.onConfigurationChanged(cfg);
            } catch (Throwable ignored) {
            }
            if (top != null) {
                try {
                    top.onConfigurationChanged(cfg);
                } catch (Throwable ignored) {
                }
            }
        }
        if (forceLayout) {
            forceNextLayout = true;
            if (top != null) {
                top.getWindow().getDecorView().requestLayout();
            }
        }
        repaintViewport();
    }

    private void updateControlBarLabels() {
        if (rotateButton != null) {
            rotateButton.setText(landscape ? "↻ Dọc" : "↻ Ngang");
        }
        if (zoomCombo != null) {
            updatingZoomCombo = true;
            try {
                String target = viewportZoom <= 0f ? "Tự do (1:1)" : Math.round(viewportZoom * 100) + "%";
                zoomCombo.setSelectedItem(target);
            } finally {
                updatingZoomCombo = false;
            }
        }
    }

    private JPanel buildControlBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(new java.awt.Color(0x161822));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(0x2D334D)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.setOpaque(false);

        JButton btnBack = createToolbarButton("◀", "Quay lại (Esc)");
        btnBack.addActionListener(e -> {
            Activity act = activityStack.peek();
            if (act != null)
                act.onBackPressed();
            if (viewport != null)
                viewport.requestFocusInWindow();
        });

        rotateButton = createToolbarButton(landscape ? "↻ Ngang" : "↻ Dọc", "Xoay dọc / ngang (Ctrl+R)");
        rotateButton.addActionListener(e -> {
            setOrientationLandscape(!landscape);
            if (viewport != null)
                viewport.requestFocusInWindow();
        });

        zoomCombo = new JComboBox<>(new String[] { "Tự do (1:1)", "75%", "100%", "125%", "150%" });
        zoomCombo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        zoomCombo.setToolTipText("Phóng to / thu nhỏ hoặc kéo giãn tự do");
        zoomCombo.setFocusable(false);
        zoomCombo.addActionListener(e -> {
            if (updatingZoomCombo)
                return;
            Object sel = zoomCombo.getSelectedItem();
            if (sel == null)
                return;
            String s = sel.toString();
            switch (s) {
                case "75%" -> setViewportZoom(0.75f);
                case "100%" -> setViewportZoom(1.0f);
                case "125%" -> setViewportZoom(1.25f);
                case "150%" -> setViewportZoom(1.5f);
                default -> setViewportZoom(0f);
            }
            if (viewport != null)
                viewport.requestFocusInWindow();
        });

        JButton btnInspector = createToolbarButton("Soi View", "Mở/đóng cây View & Chẩn đoán (F12)");
        btnInspector.addActionListener(e -> {
            toggleInspector();
            if (viewport != null)
                viewport.requestFocusInWindow();
        });

        left.add(btnBack);
        left.add(rotateButton);
        left.add(zoomCombo);
        left.add(btnInspector);
        bar.add(left, BorderLayout.WEST);

        perfLabel = new JLabel("60 fps");
        perfLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        perfLabel.setForeground(new java.awt.Color(0x9CE7B8));
        bar.add(perfLabel, BorderLayout.EAST);
        return bar;
    }

    private static JButton createToolbarButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(new java.awt.Color(0xE8EAF6));
        btn.setBackground(new java.awt.Color(0x252A3E));
        btn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        btn.setFocusPainted(false);
        btn.setFocusable(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(tooltip);
        return btn;
    }

    public void showWindow(String initialActivityClass) {
        SwingUtilities.invokeLater(() -> {
            String appLabel = runtimeContext.getManifest().label != null
                    ? runtimeContext.getManifest().label
                    : runtimeContext.getPackageName();
            frame = new JFrame(appLabel + " — AAOW");
            frame.setDefaultCloseOperation(exitProcessOnClose ? JFrame.EXIT_ON_CLOSE : JFrame.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.setMinimumSize(new Dimension(340, 420));
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    cleanupAllActivities();
                }
            });

            Bitmap iconBmp = runtimeContext.getManifest().iconRes != 0
                    ? runtimeContext.getResources().loadBitmap(runtimeContext.getManifest().iconRes)
                    : null;
            if (iconBmp != null && !iconBmp.isRecycled()) {
                frame.setIconImage(iconBmp.getAwtImage());
            }

            frame.add(buildControlBar(), BorderLayout.NORTH);

            viewport = new DeviceViewport();
            viewport.setPreferredSize(new Dimension(logicalWidth, logicalHeight));
            viewport.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    if (viewportZoom <= 0f && viewport.getWidth() > 0 && viewport.getHeight() > 0) {
                        int nw = viewport.getWidth();
                        int nh = viewport.getHeight();
                        if (nw != logicalWidth || nh != logicalHeight) {
                            logicalWidth = nw;
                            logicalHeight = nh;
                            syncConfigurationAndRelayout(nw > nh, true);
                            updateControlBarLabels();
                        }
                    }
                }
            });
            frame.add(viewport, BorderLayout.CENTER);

            inspectorPanel = buildInspectorPanel();
            inspectorPanel.setVisible(false);
            frame.add(inspectorPanel, BorderLayout.EAST);

            Intent mainIntent = new Intent(Intent.ACTION_MAIN);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            mainIntent.setClassName(runtimeContext.getPackageName(), initialActivityClass);
            startActivity(mainIntent);

            perfStatsTimer = new Timer(500, e -> {
                if (perfLabel != null && frame != null && frame.isDisplayable()) {
                    FrameStats st = getFrameStats();
                    perfLabel.setText(String.format("%d fps • %.1f ms • %d MB",
                            st.fps(), st.lastFrameMs(), st.heapUsedMb()));
                }
            });
            perfStatsTimer.start();

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            viewport.requestFocusInWindow();
        });
    }

    /** Chế độ kết xuất ảnh chụp màn hình tự động (kiểm thử trực tiếp từ CLI). */
    public void renderHeadlessScreenshot(String activityClass, File outFile, int width, int height) throws Exception {
        WindowsBrowserBridge.setSuppressExternalBrowser(true);
        Runnable launch = () -> {
            this.viewportZoom = 0f;
            this.logicalWidth = width;
            this.logicalHeight = height;
            this.landscape = width > height;
            this.forceNextLayout = true;

            Intent mainIntent = new Intent(Intent.ACTION_MAIN);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            mainIntent.setClassName(runtimeContext.getPackageName(), activityClass);
            viewport = new DeviceViewport();
            viewport.setSize(width, height);
            startActivity(mainIntent);
            Activity top = activityStack.peek();
            if (top != null) {
                View decor = top.getWindow().getDecorView();
                int ch = Math.max(0, height - viewport.contentTopOffset(top) - NAV_BAR_H);
                runtimeContext.getResources().updateViewportMetrics(width, ch, landscape);
                if (!decor.isAttachedToWindow()) {
                    decor.dispatchAttachedToWindow();
                    decor.dispatchWindowFocusChanged(activeDialogs.isEmpty());
                }
                decor.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(ch, View.MeasureSpec.EXACTLY));
                decor.layout(0, 0, width, ch);
                decor.dispatchWindowFocusChanged(activeDialogs.isEmpty());
                lastLayoutW = width;
                lastLayoutH = ch;
                lastLayoutActivity = top;
                forceNextLayout = false;
                layoutPasses++;
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            launch.run();
        } else {
            SwingUtilities.invokeAndWait(launch);
        }

        // Cho phép luồng nền (Room/AsyncTask/MarkdownView) và WebView tải xong nội dung
        // trước khi chụp:
        try {
            Thread.sleep(300);
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeAndWait(() -> {
                });
            }
            // Nếu ứng dụng đang hiển thị ProgressDialog tải dữ liệu trực tuyến hoặc đang ở
            // SplashActivity
            // chờ tự chuyển tiếp sang MainActivity, chờ tối đa 5.0s cho chuyển xong:
            boolean startedOnSplash = activityClass != null && activityClass.toLowerCase().contains("splash");
            boolean leftSplash = false;
            long deadline = System.currentTimeMillis() + 5000L;
            while (System.currentTimeMillis() < deadline) {
                Activity curTop = activityStack.peek();
                boolean onSplash = startedOnSplash && curTop != null
                        && curTop.getClass().getSimpleName().toLowerCase().contains("splash");
                if (startedOnSplash && !onSplash && !leftSplash) {
                    leftSplash = true;
                    // Sau khi rời SplashActivity sang MainActivity, chờ thêm 1.2s để UI & mạng tải
                    // ổn định
                    Thread.sleep(1200);
                    if (!SwingUtilities.isEventDispatchThread()) {
                        SwingUtilities.invokeAndWait(() -> {
                        });
                    }
                    break;
                }
                if (getActiveDialog() instanceof android.app.ProgressDialog || onSplash) {
                    Thread.sleep(120);
                    if (!SwingUtilities.isEventDispatchThread()) {
                        SwingUtilities.invokeAndWait(() -> {
                        });
                    }
                    continue;
                }
                Thread.sleep(180);
                if (!SwingUtilities.isEventDispatchThread()) {
                    SwingUtilities.invokeAndWait(() -> {
                    });
                }
                if (!(getActiveDialog() instanceof android.app.ProgressDialog)) {
                    break;
                }
            }
            WebViewEngineBridge.awaitAllIdle(2500);
            Thread.sleep(100);
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeAndWait(() -> {
                    forceNextLayout = true;
                });
            }
            WebViewEngineBridge.awaitAllIdle(1000);
        } catch (Exception ignored) {
        }

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Runnable paint = () -> {
            Graphics2D g2 = img.createGraphics();
            viewport.paintComponent(g2);
            g2.dispose();
        };
        if (SwingUtilities.isEventDispatchThread()) {
            paint.run();
        } else {
            SwingUtilities.invokeAndWait(paint);
        }
        if (Boolean.getBoolean("aaow.dumpTree")) {
            Activity top = activityStack.peek();
            if (top != null) {
                StringBuilder sb = new StringBuilder();
                dumpViewTree(top.getWindow().getDecorView(), 0, sb);
                System.out.println(sb);
            }
        }
        File parentDir = outFile.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
        }
        ImageIO.write(img, "png", outFile);
    }

    /**
     * Chụp lại khung hình hiện tại của viewport mà không khởi tạo lại Activity hay
     * ép re-layout.
     */
    public void renderCurrentViewportSnapshot(File outFile) throws Exception {
        if (viewport == null) {
            throw new IllegalStateException("Viewport chưa được khởi tạo");
        }
        float scale = viewportZoom > 0f ? viewportZoom : 1.0f;
        int w = Math.max(1, viewport.getWidth() > 0 ? viewport.getWidth() : Math.round(logicalWidth * scale));
        int h = Math.max(1, viewport.getHeight() > 0 ? viewport.getHeight() : Math.round(logicalHeight * scale));
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Runnable paint = () -> {
            if (viewport.getWidth() != w || viewport.getHeight() != h) {
                viewport.setSize(w, h);
            }
            Graphics2D g2 = img.createGraphics();
            viewport.paintComponent(g2);
            g2.dispose();
        };
        if (SwingUtilities.isEventDispatchThread()) {
            paint.run();
        } else {
            SwingUtilities.invokeAndWait(paint);
        }
        if (Boolean.getBoolean("aaow.dumpTree")) {
            Activity top = activityStack.peek();
            if (top != null) {
                StringBuilder sb = new StringBuilder();
                dumpViewTree(top.getWindow().getDecorView(), 0, sb);
                System.out.println(sb);
            }
        }
        File parentDir = outFile.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
        }
        ImageIO.write(img, "png", outFile);
    }

    // --------------------------------------------------------- API tương tác tự
    // động (Headless / End-to-End Test)

    /**
     * Bắt đầu Activity trên viewport headless mà chưa chụp ảnh, sẵn sàng nhận sự
     * kiện tương tác.
     */
    public void launchHeadless(String activityClass, int width, int height) throws Exception {
        WindowsBrowserBridge.setSuppressExternalBrowser(true);
        Runnable launch = () -> {
            this.viewportZoom = 0f;
            this.logicalWidth = width;
            this.logicalHeight = height;
            this.landscape = width > height;
            this.forceNextLayout = true;

            Intent mainIntent = new Intent(Intent.ACTION_MAIN);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            mainIntent.setClassName(runtimeContext.getPackageName(), activityClass);
            viewport = new DeviceViewport();
            viewport.setSize(width, height);
            startActivity(mainIntent);
            layoutCurrentIfNeeded();
        };
        if (SwingUtilities.isEventDispatchThread()) {
            launch.run();
        } else {
            SwingUtilities.invokeAndWait(launch);
        }
        pumpEventsAndLayout(250);
    }

    /**
     * Chờ các tác vụ bất đồng bộ (Main Looper, Room, WebView, Drawer/ViewPager
     * animations) và đo/vẽ lại khung hình.
     */
    public void pumpEventsAndLayout(long waitMs) throws Exception {
        Runnable pass = () -> {
            if (viewport == null)
                return;
            layoutCurrentIfNeeded();
            BufferedImage tmp = new BufferedImage(Math.max(1, logicalWidth), Math.max(1, logicalHeight),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = tmp.createGraphics();
            viewport.paintComponent(g2);
            g2.dispose();
        };
        if (SwingUtilities.isEventDispatchThread()) {
            pass.run();
            return;
        }
        long deadline = System.currentTimeMillis() + Math.max(0L, waitMs);
        do {
            SwingUtilities.invokeAndWait(pass);
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0)
                break;
            Thread.sleep(Math.min(32L, remaining));
        } while (true);
        WebViewEngineBridge.awaitAllIdle(1500);
        SwingUtilities.invokeAndWait(pass);
    }

    private void layoutCurrentIfNeeded() {
        Activity top = activityStack.peek();
        if (top == null || viewport == null)
            return;
        View decor = top.getWindow().getDecorView();
        int w = Math.max(1, logicalWidth);
        int ch = Math.max(0, logicalHeight - viewport.contentTopOffset(top) - NAV_BAR_H);
        runtimeContext.getResources().updateViewportMetrics(w, ch, landscape);
        if (!decor.isAttachedToWindow()) {
            decor.dispatchAttachedToWindow();
        }
        decor.measure(
                View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(ch, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, w, ch);
        decor.dispatchWindowFocusChanged(activeDialogs.isEmpty());
        lastLayoutW = w;
        lastLayoutH = ch;
        lastLayoutActivity = top;
        forceNextLayout = false;
        layoutPasses++;
    }

    public void simulateClick(int logicalX, int logicalY) throws Exception {
        Runnable r = () -> {
            if (viewport == null)
                return;
            viewport.handleMouse(MotionEvent.ACTION_DOWN, logicalX, logicalY);
            viewport.handleMouse(MotionEvent.ACTION_UP, logicalX, logicalY);
        };
        if (SwingUtilities.isEventDispatchThread())
            r.run();
        else
            SwingUtilities.invokeAndWait(r);
    }

    public void simulateSwipe(int x1, int y1, int x2, int y2, int steps) throws Exception {
        int safeSteps = Math.max(2, steps);
        Runnable r = () -> {
            if (viewport == null)
                return;
            viewport.handleMouse(MotionEvent.ACTION_DOWN, x1, y1);
            for (int i = 1; i <= safeSteps; i++) {
                int x = x1 + (x2 - x1) * i / safeSteps;
                int y = y1 + (y2 - y1) * i / safeSteps;
                viewport.handleMouse(MotionEvent.ACTION_MOVE, x, y);
            }
            viewport.handleMouse(MotionEvent.ACTION_UP, x2, y2);
        };
        if (SwingUtilities.isEventDispatchThread())
            r.run();
        else
            SwingUtilities.invokeAndWait(r);
    }

    /**
     * Tính tọa độ tâm của một View (trong Activity hoặc trong Dialog đang mở) và
     * bắn click chuột thật qua HostWindow.
     */
    public void simulateClickView(View target) throws Exception {
        if (target == null)
            throw new IllegalArgumentException("Target view is null");
        pumpEventsAndLayout(50);
        int[] coords = new int[2];
        Runnable calcAndClick = () -> {
            int[] loc = new int[2];
            target.getLocationInWindow(loc);
            int cx = loc[0] + Math.max(1, target.getWidth()) / 2;
            int cy = loc[1] + Math.max(1, target.getHeight()) / 2;
            Dialog topDialog = getActiveDialog();
            boolean inDialog = false;
            if (topDialog != null && !usesNativeAlertOverlay(topDialog)) {
                View cur = target;
                View dialogDecor = topDialog.getWindow().getDecorView();
                while (cur != null) {
                    if (cur == dialogDecor) {
                        inDialog = true;
                        break;
                    }
                    cur = cur.getParent() instanceof View pv ? pv : null;
                }
            }
            if (inDialog) {
                int visLeft = Math.max(0, loc[0]);
                int visRight = Math.min(loc[0] + Math.max(1, target.getWidth()), Math.max(1, lastCustomDialogW));
                int visTop = Math.max(0, loc[1]);
                int visBottom = Math.min(loc[1] + Math.max(1, target.getHeight()), Math.max(1, lastCustomDialogH));
                int clampedCx = visRight > visLeft ? (visLeft + visRight) / 2
                        : Math.max(2, Math.min(cx, lastCustomDialogW - 2));
                int clampedCy = visBottom > visTop ? (visTop + visBottom) / 2
                        : Math.max(2, Math.min(cy, lastCustomDialogH - 2));
                coords[0] = lastCustomDialogX + clampedCx;
                coords[1] = lastCustomDialogY + clampedCy;
            } else {
                Activity top = activityStack.peek();
                int topOff = viewport != null ? viewport.contentTopOffset(top) : (STATUS_BAR_H + ACTION_BAR_H);
                coords[0] = cx;
                coords[1] = topOff + cy;
            }
            if (viewport != null) {
                viewport.handleMouse(MotionEvent.ACTION_DOWN, coords[0], coords[1]);
                viewport.handleMouse(MotionEvent.ACTION_UP, coords[0], coords[1]);
            }
        };
        if (SwingUtilities.isEventDispatchThread())
            calcAndClick.run();
        else
            SwingUtilities.invokeAndWait(calcAndClick);
    }

    public void simulateTypeText(String text) throws Exception {
        if (text == null)
            return;
        Runnable r = () -> {
            if (focusedView == null)
                return;
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (ch == '\n') {
                    KeyEvent ke = new KeyEvent(viewport, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
                            KeyEvent.VK_ENTER, '\n');
                    focusedView.onHostKeyPressed(ke);
                } else {
                    focusedView.onHostKeyTyped(ch);
                }
            }
            repaintViewport();
        };
        if (SwingUtilities.isEventDispatchThread())
            r.run();
        else
            SwingUtilities.invokeAndWait(r);
    }

    public void simulateKeyPressed(int awtKeyCode, boolean ctrl) throws Exception {
        Runnable r = () -> {
            if (viewport == null)
                return;
            int mods = ctrl ? InputEvent.CTRL_DOWN_MASK : 0;
            KeyEvent ke = new KeyEvent(viewport, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), mods, awtKeyCode,
                    KeyEvent.CHAR_UNDEFINED);
            if (focusedView != null) {
                focusedView.onHostKeyPressed(ke);
            } else if (awtKeyCode == KeyEvent.VK_ESCAPE) {
                Dialog topDialog = getActiveDialog();
                if (topDialog != null)
                    topDialog.onBackPressed();
                else if (activityStack.peek() != null)
                    activityStack.peek().onBackPressed();
            }
            repaintViewport();
        };
        if (SwingUtilities.isEventDispatchThread())
            r.run();
        else
            SwingUtilities.invokeAndWait(r);
    }

    public void simulateBack() throws Exception {
        Runnable r = () -> {
            Dialog topDialog = getActiveDialog();
            if (topDialog != null) {
                topDialog.onBackPressed();
            } else {
                Activity top = activityStack.peek();
                if (top != null)
                    top.onBackPressed();
            }
            repaintViewport();
        };
        if (SwingUtilities.isEventDispatchThread())
            r.run();
        else
            SwingUtilities.invokeAndWait(r);
    }

    public boolean simulateOptionsMenuItem(int itemId) throws Exception {
        final boolean[] handled = { false };
        Runnable r = () -> {
            Activity top = activityStack.peek();
            if (top == null)
                return;
            SimpleMenu menu = top.getOptionsMenu();
            if (menu != null) {
                MenuItem item = menu.findItem(itemId);
                if (item != null) {
                    handled[0] = top.dispatchOptionsItemSelected(item);
                    repaintViewport();
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread())
            r.run();
        else
            SwingUtilities.invokeAndWait(r);
        return handled[0];
    }

    public void startActivity(Intent intent) {
        startActivityForResult(null, intent, -1);
    }

    public void startActivityForResult(Activity caller, Intent intent, int requestCode) {
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
                : (runtimeContext.getManifest().label != null ? runtimeContext.getManifest().label
                        : simpleName(clsName));
        if (actMeta != null && actMeta.themeRes != 0) {
            resolveThemeColors(actMeta.themeRes);
        }

        final Activity finalAct = act;
        try {
            finalAct.attach(runtimeContext, application, intent, title, () -> finishActivity(finalAct),
                    this::repaintViewport);
        } catch (Throwable t) {
            if (launchError == null)
                launchError = t;
            Activity fallbackAct = new Activity();
            fallbackAct.attach(runtimeContext, application, intent, title, () -> finishActivity(fallbackAct),
                    this::repaintViewport);
            act = fallbackAct;
        }
        final Activity activeAct = act;
        if (actMeta != null && actMeta.themeRes != 0) {
            activeAct.setTheme(actMeta.themeRes);
        }
        activeAct.setOpenOptionsMenuBridge(() -> openOptionsMenuOverlay(activeAct, activeAct.getOptionsMenu(), null));
        activeAct.getWindow().setStatusBarColor(themeStatusBarColor);
        activeAct.getWindow().getDecorView().setInvalidateCallback(this::repaintViewport);

        activityStack.push(activeAct);
        if (caller != null && requestCode >= 0) {
            pendingResults.put(activeAct, new PendingResultCaller(caller, requestCode));
        }
        Log.i("AAOW-ActivityManager", "Starting Activity: " + clsName);

        if (launchError == null) {
            try {
                activeAct.performCreate(null);
                if (!activeAct.isFinishing() && !activeAct.isDestroyed()) {
                    activeAct.performStart();
                    activeAct.performResume();
                }
            } catch (Throwable t) {
                launchError = t;
                Log.e("AAOW-ActivityManager", "Lỗi trong vòng đời Activity " + clsName + ": " + t, t);
            }
        } else {
            Log.e("AAOW-ActivityManager", "Lỗi nạp lớp Activity " + clsName + ": " + launchError, launchError);
        }

        if (!activeAct.isFinishing() && !activeAct.isDestroyed()) {
            FrameLayout decor = (FrameLayout) activeAct.getWindow().getDecorView();
            boolean needFallback = (decor.getChildCount() == 0);
            if (needFallback) {
                decor.addView(buildDiagnosticFallbackView(activeAct, clsName, launchError),
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));
            }
            if (activeAct == activityStack.peek()) {
                showingDiagnosticFallback = needFallback;
            }
        }

        focusedView = null;
        repaintViewport();
    }

    private static String simpleName(String fqcn) {
        int dot = fqcn.lastIndexOf('.');
        return dot >= 0 ? fqcn.substring(dot + 1) : fqcn;
    }

    private static String shortenSignature(String sig) {
        int paren = sig.indexOf('(');
        if (paren <= 0)
            return sig;
        String prefix = sig.substring(0, paren);
        int dot = prefix.lastIndexOf('.');
        if (dot > 0) {
            String owner = prefix.substring(0, dot);
            String m = prefix.substring(dot + 1);
            return simpleName(owner) + "." + m + "()";
        }
        return prefix + "()";
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
        var loadedNativeLibs = NativeLibraryManager.getLoadedLibraries();
        var emulatedJni = NativeBridge.getEmulatedNativeCallCounts();
        var jniCalls = NativeBridge.getInterceptedNativeCalls();
        var stubs = NativeBridge.getStubbedClasses();
        var pluginStubs = NativeBridge.getStubbedPluginClasses();
        var stubMethodCalls = NativeBridge.getStubMethodCalls();
        var topStubMethods = NativeBridge.getTopStubMethodCalls(5);

        StringBuilder diag = new StringBuilder();
        diag.append("• Kiến trúc .so trong APK : ").append(abis.isEmpty() ? "Không có" : abis).append('\n');
        diag.append("• Thư viện native đã nạp : ").append(loadedNativeLibs.size())
                .append(" | đã chặn: ").append(libs.isEmpty() ? "0" : libs).append('\n');
        diag.append("• Hàm JNI giả lập/ARM64  : ").append(emulatedJni.size())
                .append(" hàm | fallback: ").append(jniCalls.size()).append(" hàm\n");
        if (!emulatedJni.isEmpty()) {
            int c = 0;
            for (var entry : emulatedJni.entrySet()) {
                diag.append("   + ").append(entry.getKey()).append(" (×").append(entry.getValue()).append(")\n");
                if (++c >= 3)
                    break;
            }
        }
        if (!jniCalls.isEmpty()) {
            int c = 0;
            for (String call : jniCalls) {
                diag.append("   - ").append(call).append('\n');
                if (++c >= 3)
                    break;
            }
        }
        diag.append("• Class android.jar gỡ Stub: ").append(stubs.size())
                .append(" | Plugin stub: ").append(pluginStubs.size()).append('\n');
        diag.append("• Hàm Stub đã gọi lúc chạy : ").append(stubMethodCalls.size()).append(" hàm");
        if (!topStubMethods.isEmpty()) {
            diag.append('\n');
            for (var entry : topStubMethods) {
                diag.append("   - ").append(shortenSignature(entry.getKey()))
                        .append(" (×").append(entry.getValue()).append(")\n");
            }
            if (diag.charAt(diag.length() - 1) == '\n') {
                diag.deleteCharAt(diag.length() - 1);
            }
        }

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
            if (actItem.name == null || actItem.name.equals(attemptedClass))
                continue;
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
            if (++shown >= 12)
                break;
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
        if (!activityStack.contains(act))
            return;
        act.performPause();
        act.performStop();
        act.performDestroy();
        activityStack.remove(act);
        PendingResultCaller prc = pendingResults.remove(act);

        if (activityStack.isEmpty()) {
            Log.i("AAOW-ActivityManager", "Activity gốc đã đóng, kết thúc ứng dụng.");
            if (frame != null) {
                frame.dispose();
                if (exitProcessOnClose) {
                    System.exit(0);
                }
            }
        } else {
            if (prc != null && !prc.caller().isDestroyed()) {
                prc.caller().dispatchActivityResult(prc.requestCode(), act.getResultCode(), act.getResultData());
            }
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
        if (toastTimer != null)
            toastTimer.stop();
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
        if (frame == null || inspectorPanel == null)
            return;
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
        sp.setBorder(null);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private void refreshInspectorText() {
        if (!inspectorVisible || inspectorArea == null)
            return;
        StringBuilder sb = new StringBuilder();
        AndroidManifest mf = runtimeContext.getManifest();
        sb.append("=== AAOW LIVE INSPECTOR ===\n");
        sb.append("Package   : ").append(mf.packageName).append("\n");
        sb.append("Version   : ").append(mf.versionName).append(" (").append(mf.versionCode).append(")\n");
        sb.append("SDK       : min=").append(mf.minSdk).append(", target=").append(mf.targetSdk).append("\n");
        sb.append("Resources : ").append(runtimeContext.getResources().getTable().size()).append(" entries\n");
        Activity top = activityStack.peek();
        sb.append("Activity  : ").append(top != null ? top.getClass().getName() : "<none>").append("\n");
        sb.append("Stack     : ").append(activityStack.size()).append(" activity\n");
        sb.append("Stubs     : ").append(NativeBridge.getStubbedClasses().size())
                .append(" class, ").append(NativeBridge.getStubMethodCalls().size()).append(" method đã gọi\n\n");
        if (!NativeBridge.getTopStubMethodCalls(5).isEmpty()) {
            sb.append("=== TOP HÀM STUB ĐƯỢC GỌI ===\n");
            for (var entry : NativeBridge.getTopStubMethodCalls(5)) {
                sb.append(" • ").append(entry.getKey()).append(" (×").append(entry.getValue()).append(")\n");
            }
            sb.append('\n');
        }
        sb.append("=== CÂY GIAO DIỆN ANDROID (VIEW TREE) ===\n");
        if (top != null) {
            dumpViewTree(top.getWindow().getDecorView(), 0, sb);
        }
        inspectorArea.setText(sb.toString());
    }

    private void dumpViewTree(View v, int depth, StringBuilder sb) {
        if (v == null)
            return;
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
            if (t.length() > 24)
                t = t.substring(0, 24) + "…";
            sb.append(" \"").append(t).append("\"");
        }
        sb.append('\n');
        if (v instanceof ViewGroup vg) {
            for (int i = 0; i < vg.getChildCount(); i++) {
                dumpViewTree(vg.getChildAt(i), depth + 1, sb);
            }
        }
    }

    private void openOptionsMenuOverlay(Activity act, SimpleMenu menu, List<SimpleMenu.SimpleMenuItem> customItems) {
        if (act == null || menu == null)
            return;
        this.activeOptionsMenu = menu;
        this.activeOverflowItems.clear();
        if (customItems != null) {
            this.activeOverflowItems.addAll(customItems);
        } else {
            this.activeOverflowItems.addAll(menu.getVisibleItems());
        }
        if (this.activeOverflowItems.isEmpty()) {
            this.activeOptionsMenu = null;
        }
        repaintViewport();
    }

    public void repaintViewport() {
        if (viewport == null)
            return;
        if (inspectorVisible) {
            long now = System.currentTimeMillis();
            if (now - lastInspectorRefreshMs >= 250L) {
                lastInspectorRefreshMs = now;
                refreshInspectorText();
            } else if (inspectorThrottleTimer == null || !inspectorThrottleTimer.isRunning()) {
                inspectorThrottleTimer = new Timer(250, e -> {
                    if (inspectorVisible) {
                        lastInspectorRefreshMs = System.currentTimeMillis();
                        refreshInspectorText();
                    }
                });
                inspectorThrottleTimer.setRepeats(false);
                inspectorThrottleTimer.start();
            }
        }
        viewport.repaint();
    }

    // --------------------------------------------------------- Khung vẽ chính của
    // thiết bị

    private final class DeviceViewport extends JPanel {
        private long touchDownTime;

        DeviceViewport() {
            setFocusable(true);
            setDoubleBuffered(true);
            setBackground(java.awt.Color.WHITE);

            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    handleMouse(MotionEvent.ACTION_DOWN, toLogicalCoord(e.getX()), toLogicalCoord(e.getY()));
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    handleMouse(MotionEvent.ACTION_MOVE, toLogicalCoord(e.getX()), toLogicalCoord(e.getY()));
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    handleMouse(MotionEvent.ACTION_UP, toLogicalCoord(e.getX()), toLogicalCoord(e.getY()));
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    int lx = toLogicalCoord(e.getX());
                    int ly = toLogicalCoord(e.getY());
                    Dialog topDialog = getActiveDialog();
                    if (topDialog != null) {
                        if (!usesNativeAlertOverlay(topDialog) && topDialog.getWindow() != null
                                && lx >= lastCustomDialogX && lx <= lastCustomDialogX + lastCustomDialogW
                                && ly >= lastCustomDialogY && ly <= lastCustomDialogY + lastCustomDialogH) {
                            topDialog.getWindow().getDecorView().onMouseWheel(
                                    e.getWheelRotation(), lx - lastCustomDialogX, ly - lastCustomDialogY);
                            repaintViewport();
                        }
                        return;
                    }
                    Activity act = activityStack.peek();
                    if (act == null)
                        return;
                    int contentTop = contentTopOffset(act);
                    float cy = ly - contentTop;
                    if (cy >= 0 && cy < contentHeight(act)) {
                        act.getWindow().getDecorView().onMouseWheel(e.getWheelRotation(), lx, cy);
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
                    boolean ctrl = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
                    if (ctrl && e.getKeyCode() == KeyEvent.VK_R) {
                        setOrientationLandscape(!landscape);
                        return;
                    }
                    if (ctrl && (e.getKeyCode() == KeyEvent.VK_EQUALS || e.getKeyCode() == KeyEvent.VK_ADD)) {
                        float next = viewportZoom <= 0f ? 1.25f : Math.min(2.0f, viewportZoom + 0.25f);
                        setViewportZoom(next);
                        return;
                    }
                    if (ctrl && (e.getKeyCode() == KeyEvent.VK_MINUS || e.getKeyCode() == KeyEvent.VK_SUBTRACT)) {
                        float next = viewportZoom <= 0f ? 0.75f : Math.max(0.5f, viewportZoom - 0.25f);
                        setViewportZoom(next);
                        return;
                    }
                    if (ctrl && e.getKeyCode() == KeyEvent.VK_0) {
                        setViewportZoom(0f);
                        return;
                    }
                    if (e.getKeyCode() == KeyEvent.VK_F12) {
                        toggleInspector();
                        return;
                    }
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        Dialog topDialog = getActiveDialog();
                        if (topDialog != null) {
                            topDialog.onBackPressed();
                            repaint();
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
                    if (act != null && activeDialogs.isEmpty()) {
                        int androidCode = switch (e.getKeyCode()) {
                            case KeyEvent.VK_UP, KeyEvent.VK_W -> android.view.KeyEvent.KEYCODE_DPAD_UP;
                            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> android.view.KeyEvent.KEYCODE_DPAD_DOWN;
                            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> android.view.KeyEvent.KEYCODE_DPAD_LEFT;
                            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
                            case KeyEvent.VK_ENTER -> android.view.KeyEvent.KEYCODE_ENTER;
                            default -> 0;
                        };
                        if (androidCode != 0) {
                            act.onKeyDown(androidCode,
                                    new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, androidCode));
                            repaint();
                        }
                    }
                }
            });
        }

        private float effectiveScale() {
            return viewportZoom > 0f ? viewportZoom : 1.0f;
        }

        private int toLogicalCoord(int screenCoord) {
            float s = effectiveScale();
            return s == 1.0f ? screenCoord : Math.round(screenCoord / s);
        }

        private int logicalW() {
            return effectiveScale() == 1.0f ? Math.max(1, getWidth()) : logicalWidth;
        }

        private int logicalH() {
            return effectiveScale() == 1.0f ? Math.max(1, getHeight()) : logicalHeight;
        }

        private int contentTopOffset(Activity act) {
            boolean showAb = isActionBarVisible(act);
            return STATUS_BAR_H + (showAb ? ACTION_BAR_H : 0);
        }

        private int contentHeight(Activity act) {
            return Math.max(0, logicalH() - contentTopOffset(act) - NAV_BAR_H);
        }

        private boolean isActionBarVisible(Activity act) {
            if (act == null || themeNoTitle)
                return false;
            if (!showingDiagnosticFallback && hasAppCompatDecorBar(act.getWindow().getDecorView())) {
                return false;
            }
            ActionBar ab = act.getActionBar();
            return ab != null && ab.isShowing();
        }

        private static boolean hasAppCompatDecorBar(View root) {
            if (!(root instanceof ViewGroup vg))
                return false;
            String name = vg.getClass().getName();
            if ("androidx.appcompat.widget.ActionBarOverlayLayout".equals(name)
                    || "androidx.appcompat.widget.Toolbar".equals(name)) {
                return true;
            }
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                String cn = child.getClass().getName();
                if ("androidx.appcompat.widget.ActionBarOverlayLayout".equals(cn)
                        || "androidx.appcompat.widget.Toolbar".equals(cn)) {
                    return true;
                }
            }
            return false;
        }

        private void handleMouse(int action, int mx, int my) {
            Activity act = activityStack.peek();
            if (act == null)
                return;

            int w = logicalW();
            int h = logicalH();

            // 1. Nếu đang mở menu dropdown (ActionBar Overflow / SubMenu)
            if (activeOptionsMenu != null) {
                if (action == MotionEvent.ACTION_UP) {
                    handleOptionsMenuClick(act, mx, my, w, h);
                }
                return;
            }

            // 2. Nếu đang mở PopupWindow (PopupMenu / Dropdown)
            if (activePopup != null && activePopup.isShowing()) {
                handlePopupTouch(action, mx, my, w, h);
                return;
            }

            // 3. Nếu đang mở Dialog (AlertDialog, AppCompat AlertDialog,
            // MaterialAlertDialogBuilder, ComponentDialog...)
            Dialog topDialog = getActiveDialog();
            if (topDialog != null) {
                if (usesNativeAlertOverlay(topDialog)) {
                    if (action == MotionEvent.ACTION_UP) {
                        handleAlertDialogClick((AlertDialog) topDialog, mx, my, w, h);
                    }
                } else {
                    handleCustomDialogTouch(topDialog, action, mx, my, w, h);
                }
                return;
            }

            // 4. Thanh điều hướng dưới cùng (Back / Home / Inspector)
            if (my >= h - NAV_BAR_H) {
                if (action == MotionEvent.ACTION_UP) {
                    int third = w / 3;
                    if (mx < third) {
                        act.onBackPressed();
                    } else if (mx < third * 2) {
                        // Nút Home: quay về Activity gốc
                        while (activityStack.size() > 1) {
                            Activity top = activityStack.peek();
                            if (top != null)
                                top.finish();
                        }
                        showToast("Đã về màn hình chính của ứng dụng", 1500);
                    } else {
                        toggleInspector();
                    }
                }
                return;
            }

            // 5. Thanh ActionBar phía trên (HomeAsUp / Action Items / Overflow)
            int contentTop = contentTopOffset(act);
            if (my >= STATUS_BAR_H && my < contentTop && isActionBarVisible(act)) {
                if (action == MotionEvent.ACTION_UP) {
                    handleActionBarClick(act, mx);
                }
                return;
            }
            if (my < contentTop)
                return;

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
            if (focusedView == v)
                return;
            if (focusedView != null)
                focusedView.setFocusedInternal(false);
            focusedView = v;
            if (focusedView != null)
                focusedView.setFocusedInternal(true);
        }

        private View findFocusableAt(View root, float x, float y) {
            if (root == null || root.getVisibility() != View.VISIBLE || !root.isEnabled())
                return null;
            if (root instanceof ViewGroup vg) {
                float sx = x + vg.getScrollX();
                float sy = y + vg.getScrollY();
                for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                    View c = vg.getChildAt(i);
                    float cx = sx - c.getLeft();
                    float cy = sy - c.getTop();
                    if (cx >= 0 && cx < c.getWidth() && cy >= 0 && cy < c.getHeight()) {
                        View found = findFocusableAt(c, cx, cy);
                        if (found != null)
                            return found;
                    }
                }
            }
            return root instanceof EditText || root.isFocusable() ? root : null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            long frameStartNs = System.nanoTime();
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            float scale = effectiveScale();
            int swingW = getWidth();
            int swingH = getHeight();
            int w = logicalW();
            int h = logicalH();
            if (scale != 1.0f) {
                g2.setColor(COL_NAV_BG);
                g2.fillRect(0, 0, swingW, swingH);
                g2.scale(scale, scale);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            }

            Activity act = activityStack.peek();

            // Nền mặc định sáng sạch của Android
            g2.setColor(COL_VIEWPORT_BG);
            g2.fillRect(0, 0, w, h);

            if (act != null) {
                int topY = STATUS_BAR_H;
                boolean showAb = isActionBarVisible(act);
                int contentTop = topY + (showAb ? ACTION_BAR_H : 0);
                int ch = Math.max(0, h - contentTop - NAV_BAR_H);
                runtimeContext.getResources().updateViewportMetrics(w, ch, landscape);

                drawStatusBar(g2, w, act);
                if (showAb) {
                    drawActionBar(g2, w, topY, act);
                }

                // Chỉ đo & dàn trang lại khi cây View yêu cầu hoặc kích thước/Activity thay đổi
                View decor = act.getWindow().getDecorView();
                if (!decor.isAttachedToWindow()) {
                    decor.dispatchAttachedToWindow();
                    decor.dispatchWindowFocusChanged(activeDialogs.isEmpty());
                }
                if (forceNextLayout || decor.isLayoutRequested() || !decor.isLaidOut()
                        || w != lastLayoutW || ch != lastLayoutH || act != lastLayoutActivity) {
                    int wSpec = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY);
                    int hSpec = View.MeasureSpec.makeMeasureSpec(ch, View.MeasureSpec.EXACTLY);
                    decor.measure(wSpec, hSpec);
                    decor.layout(0, 0, w, ch);
                    if (decor.isLayoutRequested()) {
                        decor.measure(wSpec, hSpec);
                        decor.layout(0, 0, w, ch);
                    }
                    lastLayoutW = w;
                    lastLayoutH = ch;
                    lastLayoutActivity = act;
                    forceNextLayout = false;
                    layoutPasses++;
                } else {
                    layoutSkips++;
                }

                Graphics2D contentG = (Graphics2D) g2.create(0, contentTop, w, ch);
                Canvas androidCanvas = new Canvas(contentG);
                decor.draw(androidCanvas);
                contentG.dispose();

                if (activePopup != null && activePopup.isShowing()) {
                    drawPopupOverlay(g2, w, h, activePopup);
                }
                if (activeOptionsMenu != null) {
                    drawOptionsMenuOverlay(g2, w, h, act);
                }
                Dialog topDialog = getActiveDialog();
                if (topDialog != null) {
                    if (usesNativeAlertOverlay(topDialog)) {
                        drawAlertDialog(g2, w, h, (AlertDialog) topDialog);
                    } else {
                        drawCustomDialogOverlay(g2, w, h, topDialog);
                    }
                }
                if (toastMessage != null) {
                    drawToastOverlay(g2, w, h - NAV_BAR_H, toastMessage);
                }
            }

            drawNavigationBar(g2, w, h);
            g2.dispose();

            long nowNs = System.nanoTime();
            lastFrameMs = (nowNs - frameStartNs) / 1_000_000.0;
            totalFrames++;
            framesInWindow++;
            long elapsedNs = nowNs - frameWindowStartNs;
            if (elapsedNs >= 500_000_000L) {
                currentFps = (int) Math.round(framesInWindow * 1_000_000_000.0 / elapsedNs);
                framesInWindow = 0;
                frameWindowStartNs = nowNs;
            }
        }

        private void drawStatusBar(Graphics2D g2, int w, Activity act) {
            int sbColor = act.getWindow().getStatusBarColor();
            if (((sbColor >>> 24) & 0xFF) < 20)
                sbColor = 0xFF283593;
            g2.setColor(Color.toAwt(sbColor));
            g2.fillRect(0, 0, w, STATUS_BAR_H);

            int r = (sbColor >> 16) & 0xFF;
            int g = (sbColor >> 8) & 0xFF;
            int b = sbColor & 0xFF;
            boolean lightBar = (0.299 * r + 0.587 * g + 0.114 * b) > 180;
            g2.setColor(lightBar ? COL_STATUS_DARK : java.awt.Color.WHITE);
            g2.setFont(FONT_STATUS_TIME);
            g2.drawString(LocalTime.now().format(CLOCK_FMT), 14, 19);

            g2.setFont(FONT_STATUS_BADGE);
            String badge = "AAOW Direct • No VM";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(badge, w - fm.stringWidth(badge) - 14, 19);
        }

        private void drawActionBar(Graphics2D g2, int w, int topY, Activity act) {
            actionBarHits.clear();
            g2.setColor(Color.toAwt(themePrimaryColor));
            g2.fillRect(0, topY, w, ACTION_BAR_H);

            ActionBar ab = act.getActionBar();
            int textX = 16;
            if (ab != null && ab.isDisplayHomeAsUp()) {
                g2.setColor(java.awt.Color.WHITE);
                g2.setStroke(STROKE_AB_BACK);
                int cy = topY + ACTION_BAR_H / 2;
                g2.drawLine(16, cy, 30, cy);
                g2.drawLine(16, cy, 22, cy - 6);
                g2.drawLine(16, cy, 22, cy + 6);
                actionBarHits.add(new ActionBarHit(0, 44, true, false, null));
                textX = 44;
            } else if (cachedAppIconDrawable != null) {
                Canvas c = new Canvas(g2);
                int iy = topY + (ACTION_BAR_H - 28) / 2;
                cachedAppIconDrawable.setBounds(14, iy, 14 + 28, iy + 28);
                cachedAppIconDrawable.draw(c);
                actionBarHits.add(new ActionBarHit(0, 48, true, false, null));
                textX = 52;
            }

            // Phía phải: vẽ tối đa 3 nút Action + nút Overflow (⋮) nếu có
            SimpleMenu menu = showingDiagnosticFallback ? null : act.getOptionsMenu();
            List<SimpleMenu.SimpleMenuItem> visible = menu != null ? menu.getVisibleItems() : List.of();
            List<SimpleMenu.SimpleMenuItem> barItems = new ArrayList<>();
            List<SimpleMenu.SimpleMenuItem> overflowItems = new ArrayList<>();
            for (SimpleMenu.SimpleMenuItem item : visible) {
                if (item.requestsActionButton() && barItems.size() < 3) {
                    barItems.add(item);
                } else {
                    overflowItems.add(item);
                }
            }

            int rightCursor = w - 8;
            if (!overflowItems.isEmpty()) {
                int left = rightCursor - 34;
                int cx = left + 17;
                int cy = topY + ACTION_BAR_H / 2;
                g2.setColor(java.awt.Color.WHITE);
                g2.fillOval(cx - 2, cy - 8, 4, 4);
                g2.fillOval(cx - 2, cy - 2, 4, 4);
                g2.fillOval(cx - 2, cy + 4, 4, 4);
                actionBarHits.add(new ActionBarHit(left, rightCursor, false, true, null));
                rightCursor = left - 4;
            }

            for (int i = barItems.size() - 1; i >= 0; i--) {
                SimpleMenu.SimpleMenuItem item = barItems.get(i);
                Drawable icon = item.getIcon();
                if (icon != null) {
                    int left = rightCursor - 36;
                    int iy = topY + (ACTION_BAR_H - 24) / 2;
                    Canvas c = new Canvas(g2);
                    icon.setBounds(left + 6, iy, left + 30, iy + 24);
                    icon.draw(c);
                    actionBarHits.add(new ActionBarHit(left, rightCursor, false, false, item));
                    rightCursor = left - 4;
                } else {
                    String label = item.getTitleCondensed().toString();
                    if (label.length() > 10)
                        label = label.substring(0, 9) + "…";
                    g2.setFont(FONT_ACTION_BTN);
                    int tw = g2.getFontMetrics().stringWidth(label) + 14;
                    int left = rightCursor - tw;
                    g2.setColor(new java.awt.Color(255, 255, 255, 36));
                    g2.fill(new RoundRectangle2D.Float(left, topY + 12, tw, 28, 8, 8));
                    g2.setColor(java.awt.Color.WHITE);
                    g2.drawString(label, left + 7, topY + 30);
                    actionBarHits.add(new ActionBarHit(left, rightCursor, false, false, item));
                    rightCursor = left - 6;
                }
            }

            String title = ab != null && ab.getTitle().length() > 0
                    ? ab.getTitle().toString()
                    : act.getTitle().toString();
            int maxTitleW = Math.max(40, rightCursor - textX - 8);
            g2.setColor(java.awt.Color.WHITE);
            g2.setFont(FONT_ACTION_TITLE);
            FontMetrics fm = g2.getFontMetrics();
            while (title.length() > 1 && fm.stringWidth(title) > maxTitleW) {
                title = title.substring(0, title.length() - 2) + "…";
            }
            g2.drawString(title, textX, topY + 32);
        }

        private void handleActionBarClick(Activity act, int mx) {
            SimpleMenu menu = showingDiagnosticFallback ? null : act.getOptionsMenu();
            for (ActionBarHit hit : actionBarHits) {
                if (mx >= hit.left() && mx <= hit.right()) {
                    if (hit.isHome()) {
                        SimpleMenu.SimpleMenuItem homeItem = new SimpleMenu(act)
                                .add(Menu.NONE, 0x0102002c, Menu.NONE, "Home") instanceof SimpleMenu.SimpleMenuItem smi
                                        ? smi
                                        : null;
                        if (homeItem != null)
                            act.dispatchOptionsItemSelected(homeItem);
                        repaintViewport();
                        return;
                    }
                    if (hit.isOverflow() && menu != null) {
                        List<SimpleMenu.SimpleMenuItem> overflow = new ArrayList<>();
                        int barCount = 0;
                        for (SimpleMenu.SimpleMenuItem item : menu.getVisibleItems()) {
                            if (item.requestsActionButton() && barCount < 3) {
                                barCount++;
                            } else {
                                overflow.add(item);
                            }
                        }
                        openOptionsMenuOverlay(act, menu, overflow);
                        return;
                    }
                    if (hit.item() != null) {
                        if (hit.item().hasSubMenu() && hit.item().getSubMenu() instanceof SimpleMenu sub) {
                            openOptionsMenuOverlay(act, sub, null);
                        } else {
                            act.dispatchOptionsItemSelected(hit.item());
                            repaintViewport();
                        }
                        return;
                    }
                }
            }
        }

        private void drawOptionsMenuOverlay(Graphics2D g2, int w, int h, Activity act) {
            int itemCount = activeOverflowItems.size();
            if (itemCount == 0)
                return;
            int rowH = 40;
            int menuW = 216;
            int menuH = Math.min(h - 120, itemCount * rowH + 12);
            int mx = w - menuW - 10;
            int my = STATUS_BAR_H + (isActionBarVisible(act) ? ACTION_BAR_H : 0) + 4;

            g2.setColor(new java.awt.Color(0, 0, 0, 50));
            g2.fill(new RoundRectangle2D.Float(mx + 2, my + 3, menuW, menuH, 12, 12));
            g2.setColor(java.awt.Color.WHITE);
            g2.fill(new RoundRectangle2D.Float(mx, my, menuW, menuH, 12, 12));
            g2.setColor(new java.awt.Color(0xD8DCE8));
            g2.draw(new RoundRectangle2D.Float(mx, my, menuW, menuH, 12, 12));

            int y = my + 6;
            g2.setFont(FONT_MENU_ITEM);
            for (SimpleMenu.SimpleMenuItem item : activeOverflowItems) {
                if (y + rowH > my + menuH)
                    break;
                g2.setColor(item.isEnabled() ? new java.awt.Color(0x1C1B1F) : new java.awt.Color(0x9CA3AF));
                String text = item.getTitle().toString();
                if (item.isCheckable()) {
                    text = (item.isChecked() ? "☑ " : "☐ ") + text;
                }
                if (item.hasSubMenu()) {
                    text = text + "  ▸";
                }
                g2.drawString(text, mx + 14, y + 25);
                y += rowH;
            }
        }

        private void handleOptionsMenuClick(Activity act, int mx, int my, int w, int h) {
            int itemCount = activeOverflowItems.size();
            int rowH = 40;
            int menuW = 216;
            int menuH = Math.min(h - 120, itemCount * rowH + 12);
            int boxX = w - menuW - 10;
            int boxY = STATUS_BAR_H + (isActionBarVisible(act) ? ACTION_BAR_H : 0) + 4;

            if (mx < boxX || mx > boxX + menuW || my < boxY || my > boxY + menuH) {
                activeOptionsMenu = null;
                activeOverflowItems.clear();
                repaintViewport();
                return;
            }
            int idx = (my - (boxY + 6)) / rowH;
            if (idx >= 0 && idx < activeOverflowItems.size()) {
                SimpleMenu.SimpleMenuItem chosen = activeOverflowItems.get(idx);
                if (chosen.hasSubMenu() && chosen.getSubMenu() instanceof SimpleMenu sub) {
                    openOptionsMenuOverlay(act, sub, null);
                    return;
                }
                activeOptionsMenu = null;
                activeOverflowItems.clear();
                act.dispatchOptionsItemSelected(chosen);
                repaintViewport();
            }
        }

        private void drawPopupOverlay(Graphics2D g2, int w, int h, PopupWindow pw) {
            View cv = pw.getContentView();
            if (cv == null)
                return;
            int maxW = w - 24;
            int maxH = h - 100;
            int pwW = pw.getWidth() > 0 ? Math.min(pw.getWidth(), maxW) : maxW;
            int pwH = pw.getHeight() > 0 ? Math.min(pw.getHeight(), maxH) : maxH;
            cv.measure(
                    View.MeasureSpec.makeMeasureSpec(pwW,
                            pw.getWidth() > 0 ? View.MeasureSpec.EXACTLY : View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(pwH,
                            pw.getHeight() > 0 ? View.MeasureSpec.EXACTLY : View.MeasureSpec.AT_MOST));
            int mw = Math.max(80, cv.getMeasuredWidth());
            int mh = Math.max(40, cv.getMeasuredHeight());
            int px = Math.max(8, Math.min(pw.getAnchorX(), w - mw - 8));
            int py = Math.max(STATUS_BAR_H + 8, Math.min(pw.getAnchorY(), h - NAV_BAR_H - mh - 8));
            cv.layout(0, 0, mw, mh);

            Graphics2D pg = (Graphics2D) g2.create(px, py, mw, mh);
            cv.draw(new Canvas(pg));
            pg.dispose();
        }

        private void handlePopupTouch(int action, int mx, int my, int w, int h) {
            View cv = activePopup.getContentView();
            if (cv == null) {
                activePopup.dismiss();
                return;
            }
            int mw = Math.max(80, cv.getWidth());
            int mh = Math.max(40, cv.getHeight());
            int px = Math.max(8, Math.min(activePopup.getAnchorX(), w - mw - 8));
            int py = Math.max(STATUS_BAR_H + 8, Math.min(activePopup.getAnchorY(), h - NAV_BAR_H - mh - 8));
            if (mx < px || mx > px + mw || my < py || my > py + mh) {
                if (action == MotionEvent.ACTION_UP && activePopup.isOutsideTouchable()) {
                    activePopup.dismiss();
                }
                return;
            }
            long now = SystemClock.uptimeMillis();
            MotionEvent ev = new MotionEvent(action, mx - px, my - py, mx, my, now, now);
            cv.dispatchTouchEvent(ev);
            repaintViewport();
        }

        private int computeDialogHeight(AlertDialog d, int dw) {
            if (d.getCustomView() != null) {
                d.getCustomView().measure(
                        View.MeasureSpec.makeMeasureSpec(dw - 44, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.AT_MOST));
                return Math.min(460, Math.max(180, 110 + d.getCustomView().getMeasuredHeight()));
            }
            int itemCount = getDialogItemCount(d);
            if (itemCount > 0) {
                return Math.min(460, 110 + Math.min(itemCount, 8) * 38);
            }
            java.awt.image.BufferedImage dummy = new java.awt.image.BufferedImage(1, 1,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D dg = dummy.createGraphics();
            dg.setFont(FONT_DIALOG_BODY);
            List<String> lines = wrapDialogLines(d.getMessageText().toString(), dw - 44, dg.getFontMetrics());
            dg.dispose();
            return Math.min(460, Math.max(170, 110 + lines.size() * 20));
        }

        private static List<String> wrapDialogLines(String msg, int maxWidth, FontMetrics fm) {
            List<String> out = new ArrayList<>();
            if (msg == null || msg.isEmpty())
                return out;
            for (String para : msg.split("\n", -1)) {
                if (para.isEmpty() || fm.stringWidth(para) <= maxWidth) {
                    out.add(para);
                    continue;
                }
                StringBuilder cur = new StringBuilder();
                for (String word : para.split(" ")) {
                    String cand = cur.isEmpty() ? word : cur + " " + word;
                    if (!cur.isEmpty() && fm.stringWidth(cand) > maxWidth) {
                        out.add(cur.toString());
                        cur = new StringBuilder(word);
                    } else {
                        cur = new StringBuilder(cand);
                    }
                }
                if (!cur.isEmpty())
                    out.add(cur.toString());
            }
            return out;
        }

        private static int getDialogItemCount(AlertDialog d) {
            if (d.getItems() != null)
                return d.getItems().length;
            if (d.getListAdapter() != null)
                return d.getListAdapter().getCount();
            return 0;
        }

        private static String getDialogItemLabel(AlertDialog d, int index) {
            if (d.getItems() != null && index < d.getItems().length) {
                return d.getItems()[index] != null ? d.getItems()[index].toString() : "";
            }
            ListAdapter la = d.getListAdapter();
            if (la != null && index < la.getCount()) {
                Object item = la.getItem(index);
                return item != null ? item.toString() : "";
            }
            return "";
        }

        private void drawAlertDialog(Graphics2D g2, int w, int h, AlertDialog d) {
            g2.setColor(new java.awt.Color(0, 0, 0, 135));
            g2.fillRect(0, 0, w, h);

            int dw = Math.min(w - 48, 340);
            int dh = computeDialogHeight(d, dw);
            int dx = (w - dw) / 2;
            int dy = (h - dh) / 2;

            g2.setColor(java.awt.Color.WHITE);
            g2.fill(new RoundRectangle2D.Float(dx, dy, dw, dh, 24, 24));

            g2.setColor(COL_STATUS_DARK);
            g2.setFont(FONT_DIALOG_TITLE);
            g2.drawString(d.getTitleText().toString(), dx + 22, dy + 36);

            int itemCount = getDialogItemCount(d);
            if (d.getCustomView() != null) {
                View cv = d.getCustomView();
                int cw = dw - 44;
                int ch = Math.max(40, dh - 110);
                cv.layout(0, 0, cw, ch);
                Graphics2D cg = (Graphics2D) g2.create(dx + 22, dy + 52, cw, ch);
                cv.draw(new Canvas(cg));
                cg.dispose();
            } else if (itemCount > 0) {
                g2.setFont(FONT_DIALOG_BODY);
                int maxShow = Math.min(itemCount, 8);
                int ly = dy + 58;
                for (int i = 0; i < maxShow; i++) {
                    String prefix = (d.getCheckedItem() == i) ? "◉ " : "";
                    g2.setColor(COL_STATUS_DARK);
                    g2.drawString(prefix + getDialogItemLabel(d, i), dx + 22, ly + 22);
                    g2.setColor(new java.awt.Color(0xECEFF5));
                    g2.drawLine(dx + 22, ly + 36, dx + dw - 22, ly + 36);
                    ly += 38;
                }
            } else {
                g2.setColor(new java.awt.Color(0x49454F));
                g2.setFont(FONT_DIALOG_BODY);
                List<String> lines = wrapDialogLines(d.getMessageText().toString(), dw - 44, g2.getFontMetrics());
                int ly = dy + 68;
                for (String line : lines) {
                    g2.drawString(line, dx + 22, ly);
                    ly += 20;
                }
            }

            g2.setFont(FONT_DIALOG_BTN);
            g2.setColor(new java.awt.Color(0x4F5BD5));
            int btnY = dy + dh - 22;
            if (d.getNeutralText() != null) {
                String neu = d.getNeutralText().toString();
                g2.drawString(neu, dx + 24, btnY);
            }
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

        private void drawCustomDialogOverlay(Graphics2D g2, int w, int h, Dialog d) {
            if (d == null || d.getWindow() == null)
                return;
            View decor = d.getWindow().getDecorView();
            if (decor == null)
                return;

            g2.setColor(new java.awt.Color(0, 0, 0, 135));
            g2.fillRect(0, 0, w, h);

            int maxW = Math.min(w - 36, 368);
            int maxH = Math.max(140, h - STATUS_BAR_H - NAV_BAR_H - 48);
            if (!decor.isAttachedToWindow()) {
                decor.dispatchAttachedToWindow();
            }
            if (decor.getPaddingLeft() == 0 && decor.getPaddingTop() == 0
                    && decor.getPaddingRight() == 0 && decor.getPaddingBottom() == 0) {
                decor.setPadding(16, 16, 16, 10);
            }
            decor.measure(
                    View.MeasureSpec.makeMeasureSpec(maxW, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(maxH, View.MeasureSpec.AT_MOST));
            int dw = Math.max(240, Math.min(maxW, decor.getMeasuredWidth() > 0 ? decor.getMeasuredWidth() : maxW));
            int dh = Math.max(120, Math.min(maxH, decor.getMeasuredHeight() > 0 ? decor.getMeasuredHeight() : 220));
            int dx = (w - dw) / 2;
            int dy = Math.max(STATUS_BAR_H + 16, (h - NAV_BAR_H - dh) / 2);

            lastCustomDialogX = dx;
            lastCustomDialogY = dy;
            lastCustomDialogW = dw;
            lastCustomDialogH = dh;

            decor.layout(0, 0, dw, dh);

            // Vẽ thẻ nền Material trắng sạch phía sau decorView của Dialog
            // (AppCompat/MaterialAlertDialog)
            g2.setColor(new java.awt.Color(0, 0, 0, 48));
            g2.fill(new RoundRectangle2D.Float(dx + 2, dy + 4, dw, dh, 20, 20));
            g2.setColor(java.awt.Color.WHITE);
            g2.fill(new RoundRectangle2D.Float(dx, dy, dw, dh, 20, 20));

            Graphics2D dg = (Graphics2D) g2.create(dx, dy, dw, dh);
            dg.setClip(new RoundRectangle2D.Float(0, 0, dw, dh, 20, 20));
            decor.draw(new Canvas(dg));
            dg.dispose();
        }

        private void handleCustomDialogTouch(Dialog d, int action, int mx, int my, int w, int h) {
            if (d == null || d.getWindow() == null || d.getWindow().getDecorView() == null) {
                if (d != null)
                    d.dismiss();
                return;
            }
            View decor = d.getWindow().getDecorView();
            if (lastCustomDialogW <= 0 || lastCustomDialogH <= 0) {
                int maxW = Math.min(w - 36, 368);
                int maxH = Math.max(140, h - STATUS_BAR_H - NAV_BAR_H - 48);
                decor.measure(
                        View.MeasureSpec.makeMeasureSpec(maxW, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(maxH, View.MeasureSpec.AT_MOST));
                lastCustomDialogW = Math.max(240,
                        Math.min(maxW, decor.getMeasuredWidth() > 0 ? decor.getMeasuredWidth() : maxW));
                lastCustomDialogH = Math.max(120,
                        Math.min(maxH, decor.getMeasuredHeight() > 0 ? decor.getMeasuredHeight() : 220));
                lastCustomDialogX = (w - lastCustomDialogW) / 2;
                lastCustomDialogY = Math.max(STATUS_BAR_H + 16, (h - NAV_BAR_H - lastCustomDialogH) / 2);
                decor.layout(0, 0, lastCustomDialogW, lastCustomDialogH);
            }

            if (mx < lastCustomDialogX || mx > lastCustomDialogX + lastCustomDialogW
                    || my < lastCustomDialogY || my > lastCustomDialogY + lastCustomDialogH) {
                if (action == MotionEvent.ACTION_UP && d.isCanceledOnTouchOutside()) {
                    d.cancel();
                }
                repaintViewport();
                return;
            }

            float vx = mx - lastCustomDialogX;
            float vy = my - lastCustomDialogY;
            long now = SystemClock.uptimeMillis();
            if (action == MotionEvent.ACTION_DOWN) {
                touchDownTime = now;
                View hit = findFocusableAt(decor, vx, vy);
                setFocusedView(hit);
            }
            MotionEvent ev = new MotionEvent(action, vx, vy, mx, my, touchDownTime, now);
            d.dispatchTouchEvent(ev);
            repaintViewport();
        }

        private void handleAlertDialogClick(AlertDialog d, int mx, int my, int w, int h) {
            if (d == null)
                return;
            int dw = Math.min(w - 48, 340);
            int dh = computeDialogHeight(d, dw);
            int dx = (w - dw) / 2;
            int dy = (h - dh) / 2;

            if (mx < dx || mx > dx + dw || my < dy || my > dy + dh) {
                if (d.isCanceledOnTouchOutside()) {
                    d.cancel();
                }
                return;
            }
            int itemCount = getDialogItemCount(d);
            if (itemCount > 0 && my >= dy + 54 && my < dy + dh - 52) {
                int idx = (my - (dy + 54)) / 38;
                if (idx >= 0 && idx < Math.min(itemCount, 8)) {
                    if (d.getItemClickListener() != null) {
                        d.getItemClickListener().onClick(d, idx);
                    }
                    d.dismiss();
                    return;
                }
            }
            if (d.getCustomView() != null && my >= dy + 52 && my < dy + dh - 52) {
                long now = SystemClock.uptimeMillis();
                float vx = mx - (dx + 22);
                float vy = my - (dy + 52);
                View hit = findFocusableAt(d.getCustomView(), vx, vy);
                setFocusedView(hit);
                MotionEvent ev = new MotionEvent(MotionEvent.ACTION_DOWN, vx, vy, mx, my, now, now);
                d.getCustomView().dispatchTouchEvent(ev);
                MotionEvent evUp = new MotionEvent(MotionEvent.ACTION_UP, vx, vy, mx, my, now, now);
                d.getCustomView().dispatchTouchEvent(evUp);
                repaintViewport();
                return;
            }
            if (my >= dy + dh - 52) {
                if (mx >= dx + dw - 100 && d.getPositiveText() != null) {
                    if (d.getPositiveListener() != null) {
                        d.getPositiveListener().onClick(d, DialogInterface.BUTTON_POSITIVE);
                    }
                    d.dismiss();
                } else if (mx >= dx + dw - 200 && d.getNegativeText() != null) {
                    if (d.getNegativeListener() != null) {
                        d.getNegativeListener().onClick(d, DialogInterface.BUTTON_NEGATIVE);
                    }
                    d.dismiss();
                } else if (d.getNeutralText() != null) {
                    if (d.getNeutralListener() != null) {
                        d.getNeutralListener().onClick(d, DialogInterface.BUTTON_NEUTRAL);
                    }
                    d.dismiss();
                }
            }
        }

        private void drawToastOverlay(Graphics2D g2, int w, int bottomY, String msg) {
            g2.setFont(FONT_TOAST);
            FontMetrics fm = g2.getFontMetrics();
            int tw = Math.min(w - 40, fm.stringWidth(msg) + 36);
            int th = 38;
            int tx = (w - tw) / 2;
            int ty = bottomY - th - 20;

            g2.setColor(COL_TOAST_BG);
            g2.fill(new RoundRectangle2D.Float(tx, ty, tw, th, 19, 19));

            g2.setColor(java.awt.Color.WHITE);
            g2.drawString(msg, tx + (tw - fm.stringWidth(msg)) / 2, ty + 24);
        }

        private void drawNavigationBar(Graphics2D g2, int w, int h) {
            int top = h - NAV_BAR_H;
            g2.setColor(COL_NAV_BG);
            g2.fillRect(0, top, w, NAV_BAR_H);

            g2.setColor(COL_NAV_FG);
            g2.setStroke(STROKE_NAV);
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
                g2.setColor(COL_NAV_ACTIVE);
            }
            g2.drawRoundRect(rx - 8, cy - 8, 16, 16, 4, 4);
        }
    }
}
