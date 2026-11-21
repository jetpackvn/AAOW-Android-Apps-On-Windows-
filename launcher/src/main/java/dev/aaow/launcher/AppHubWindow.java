package dev.aaow.launcher;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Giao diện trung tâm quản lý ứng dụng Android trên Windows (AAOW App Hub).
 * Hỗ trợ kéo-thả cài đặt APK, mở nhiều ứng dụng đồng thời, soi cấu trúc APK và
 * quản lý dữ liệu.
 */
public final class AppHubWindow {
    private static final Color BG_DARK = new Color(0x0F111A);
    private static final Color BG_SURFACE = new Color(0x181B28);
    private static final Color BG_CARD = new Color(0x1F2335);
    private static final Color BORDER_COLOR = new Color(0x2D334D);
    private static final Color ACCENT_BLUE = new Color(0x5865F2);
    private static final Color ACCENT_GREEN = new Color(0x2ECC71);
    private static final Color ACCENT_RED = new Color(0xE74C3C);
    private static final Color TEXT_PRIMARY = new Color(0xF2F4FA);
    private static final Color TEXT_MUTED = new Color(0x9AA2C2);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AppRepository repository;
    private JFrame frame;
    private JPanel cardsContainer;
    private JLabel statsLabel;
    private JTextField searchField;
    private JTextArea inspectorArea;
    private JTextArea logArea;
    private InstallLoadingBanner installBanner;
    private AppRepository.InstalledApp selectedApp;

    private record ProgressUpdate(float progress, String message) {
    }

    public AppHubWindow(AppRepository repository) {
        this.repository = repository;
        this.repository.setLogListener(this::appendLog);
        List<AppRepository.InstalledApp> initial = repository.listApps();
        if (!initial.isEmpty()) {
            selectedApp = initial.get(0);
        }
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("AAOW App Hub — Trình quản lý & chạy ứng dụng Android trên Windows");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1080, 720);
            frame.setMinimumSize(new Dimension(920, 600));
            frame.setIconImage(IconAssetGenerator.renderAaowAppLogo(64));
            frame.setContentPane(buildRootPanel());
            installDragAndDrop(frame);
            refreshCards();
            updateInspector(selectedApp);
            appendLog("[AAOW-Hub] Sẵn sàng. Kéo-thả file .apk vào cửa sổ hoặc bấm '+ Cài đặt APK' để thêm ứng dụng.");
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public void selectApp(AppRepository.InstalledApp app) {
        if (app != null) {
            this.selectedApp = app;
        }
    }

    /** Chụp ảnh giao diện App Hub phục vụ kiểm thử trực tiếp từ CLI. */
    public void renderScreenshot(File outFile, int width, int height) throws Exception {
        JPanel root = buildRootPanel();
        refreshCards();
        updateInspector(selectedApp);
        appendLog("[AAOW-Hub] Đã nạp " + repository.listApps().size() + " ứng dụng Android trong kho.");
        appendLog("[AAOW-Hub] Kéo-thả file .apk bất kỳ vào cửa sổ để cài đặt trực tiếp.");

        JFrame tempFrame = new JFrame();
        tempFrame.setUndecorated(true);
        tempFrame.setContentPane(root);
        tempFrame.setSize(width, height);
        tempFrame.addNotify();
        root.setSize(width, height);
        layoutRecursive(root);

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        root.paint(g2);
        g2.dispose();
        tempFrame.dispose();
        ImageIO.write(img, "png", outFile);
    }

    private static void layoutRecursive(Component c) {
        c.doLayout();
        if (c instanceof java.awt.Container cont) {
            for (Component child : cont.getComponents()) {
                layoutRecursive(child);
            }
        }
    }

    private JPanel buildRootPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_DARK);

        root.add(buildHeaderBar(), BorderLayout.NORTH);

        // Cột trái: Danh sách thẻ ứng dụng + thanh hướng dẫn kéo thả
        JPanel leftPane = new JPanel(new BorderLayout(0, 10));
        leftPane.setBackground(BG_DARK);
        leftPane.setBorder(new EmptyBorder(16, 18, 16, 10));

        cardsContainer = new JPanel();
        cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));
        cardsContainer.setBackground(BG_DARK);

        JScrollPane cardsScroll = new JScrollPane(cardsContainer);
        cardsScroll.setBorder(BorderFactory.createEmptyBorder());
        cardsScroll.getViewport().setBackground(BG_DARK);
        cardsScroll.getVerticalScrollBar().setUnitIncrement(18);

        JPanel leftCenter = new JPanel(new BorderLayout(0, 10));
        leftCenter.setOpaque(false);
        installBanner = new InstallLoadingBanner();
        installBanner.setVisible(false);
        leftCenter.add(installBanner, BorderLayout.NORTH);
        leftCenter.add(cardsScroll, BorderLayout.CENTER);

        leftPane.add(leftCenter, BorderLayout.CENTER);
        leftPane.add(buildDropHintBanner(), BorderLayout.SOUTH);

        // Cột phải: Inspector + Console log
        JPanel rightPane = buildRightInspectorAndConsole();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPane, rightPane);
        split.setResizeWeight(0.62);
        split.setDividerLocation(655);
        split.setDividerSize(4);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setBackground(BG_DARK);

        root.add(split, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildHeaderBar() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(BG_SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(14, 20, 14, 20)));

        // Cụm tiêu đề bên trái
        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);

        JLabel title = new JLabel("AAOW App Hub");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT_PRIMARY);

        statsLabel = new JLabel();
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statsLabel.setForeground(TEXT_MUTED);
        updateStatsLabel();

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(statsLabel);
        header.add(titleBox, BorderLayout.WEST);

        // Cụm tìm kiếm + nút cài đặt bên phải
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        actions.setOpaque(false);

        searchField = new JTextField(15);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBackground(BG_CARD);
        searchField.setForeground(TEXT_PRIMARY);
        searchField.setCaretColor(TEXT_PRIMARY);
        searchField.setToolTipText("Lọc theo tên ứng dụng hoặc tên gói (package)");
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(6, 10, 6, 10)));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshCards();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshCards();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshCards();
            }
        });

        JLabel searchLbl = new JLabel("Tìm kiếm:");
        searchLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchLbl.setForeground(TEXT_MUTED);

        JButton btnInstall = createStyledButton("+ Cài đặt APK...", ACCENT_BLUE, Color.WHITE);
        btnInstall.addActionListener(e -> promptInstallApk());

        JButton btnBatch = createStyledButton("Nạp bộ APK", new Color(0x1E3A34), new Color(0x7CE8B4));
        btnBatch.setToolTipText(
                "Nạp nhanh bộ ứng dụng Android (Offline & Online) có sẵn hoặc chọn một thư mục chứa nhiều file .apk");
        btnBatch.addActionListener(e -> promptImportOfflineBundle());

        JButton btnAssoc = createStyledButton("", new Color(0x252A3E), new Color(0xD8DAE8));
        updateAssocButtonLabel(btnAssoc);
        btnAssoc.addActionListener(e -> toggleApkAssociation(btnAssoc));

        actions.add(searchLbl);
        actions.add(searchField);
        actions.add(btnAssoc);
        actions.add(btnBatch);
        actions.add(btnInstall);
        header.add(actions, BorderLayout.EAST);

        return header;
    }

    private void updateAssocButtonLabel(JButton btnAssoc) {
        boolean linked = WindowsIntegration.isApkAssociationRegistered();
        btnAssoc.setText(linked ? "✓ Đã liên kết .apk" : "Liên kết .apk");
        btnAssoc.setToolTipText(linked
                ? "Đang liên kết: nhấp đúp file .apk trong Windows Explorer sẽ mở bằng AAOW (bấm để gỡ)"
                : "Đăng ký mở file .apk bằng cách nhấp đúp trong Windows Explorer (HKCU)");
    }

    private void toggleApkAssociation(JButton btnAssoc) {
        try {
            if (WindowsIntegration.isApkAssociationRegistered()) {
                WindowsIntegration.unregisterApkAssociation();
                appendLog("[AAOW-Win] Đã gỡ liên kết đuôi file .apk khỏi Windows Explorer.");
                JOptionPane.showMessageDialog(frame,
                        "Đã gỡ liên kết mở file .apk bằng AAOW.",
                        "Liên kết file .apk", JOptionPane.INFORMATION_MESSAGE);
            } else {
                Path home = WindowsIntegration.resolveAaowHome();
                Path launcher = WindowsIntegration.resolveSelfLauncher(home);
                Path icon = WindowsIntegration.resolveApkIconPath(home);
                WindowsIntegration.registerApkAssociation(launcher, icon);
                appendLog("[AAOW-Win] Đã liên kết đuôi .apk -> " + launcher.toAbsolutePath());
                JOptionPane.showMessageDialog(frame,
                        "Đã đăng ký liên kết thành công!\nTừ giờ bạn có thể nhấp đúp bất kỳ file .apk nào trong Windows Explorer để mở bằng AAOW.\nLauncher: "
                                + launcher.toAbsolutePath(),
                        "Liên kết file .apk", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            appendLog("[AAOW-ERROR] Lỗi cấu hình liên kết .apk: " + ex.getMessage());
            JOptionPane.showMessageDialog(frame,
                    "Không thể cập nhật liên kết .apk:\n" + ex.getMessage(),
                    "Lỗi Windows Registry", JOptionPane.ERROR_MESSAGE);
        } finally {
            updateAssocButtonLabel(btnAssoc);
        }
    }

    private JPanel buildDropHintBanner() {
        JPanel banner = new RoundedPanel(14, new Color(0x161926), BORDER_COLOR);
        banner.setLayout(new BorderLayout());
        banner.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel lbl = new JLabel(
                "Mẹo: Kéo-thả bất kỳ file .apk nào từ Windows Explorer vào cửa sổ này để cài đặt và biên dịch DEX tức thì.");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_MUTED);
        banner.add(lbl, BorderLayout.CENTER);
        return banner;
    }

    private JPanel buildRightInspectorAndConsole() {
        JPanel right = new JPanel(new BorderLayout(0, 12));
        right.setBackground(BG_DARK);
        right.setBorder(new EmptyBorder(16, 6, 16, 18));

        // Khung trên: Chi tiết APK
        JPanel inspectorBox = new RoundedPanel(14, BG_SURFACE, BORDER_COLOR);
        inspectorBox.setLayout(new BorderLayout());
        inspectorBox.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel insTitle = new JLabel("Chi tiết cấu trúc APK & Manifest (AXML)");
        insTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        insTitle.setForeground(TEXT_PRIMARY);
        insTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
        inspectorBox.add(insTitle, BorderLayout.NORTH);

        inspectorArea = new JTextArea();
        inspectorArea.setEditable(false);
        inspectorArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        inspectorArea.setBackground(new Color(0x12141F));
        inspectorArea.setForeground(new Color(0xDCE0F5));
        inspectorArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane insScroll = new JScrollPane(inspectorArea);
        insScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        inspectorBox.add(insScroll, BorderLayout.CENTER);

        // Khung dưới: Nhật ký hệ thống
        JPanel logBox = new RoundedPanel(14, BG_SURFACE, BORDER_COLOR);
        logBox.setLayout(new BorderLayout());
        logBox.setBorder(new EmptyBorder(10, 14, 10, 14));
        logBox.setPreferredSize(new Dimension(380, 190));

        JLabel logTitle = new JLabel("Nhật ký AAOW Runtime & Package Manager");
        logTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logTitle.setForeground(TEXT_PRIMARY);
        logTitle.setBorder(new EmptyBorder(0, 0, 6, 0));
        logBox.add(logTitle, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setBackground(new Color(0x12141F));
        logArea.setForeground(new Color(0x9CE7B8));
        logArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        logBox.add(logScroll, BorderLayout.CENTER);

        right.add(inspectorBox, BorderLayout.CENTER);
        right.add(logBox, BorderLayout.SOUTH);
        return right;
    }

    private void updateStatsLabel() {
        if (statsLabel == null)
            return;
        int total = repository.listApps().size();
        int running = repository.getRunningCount();
        statsLabel.setText(String.format(
                "Kho ứng dụng: %d APK đã cài • Đang chạy: %d cửa sổ • Chế độ: Direct JVM / Java2D (Không máy ảo)",
                total, running));
    }

    public void refreshCards() {
        if (cardsContainer == null)
            return;
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshCards);
            return;
        }
        cardsContainer.removeAll();
        updateStatsLabel();

        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        List<AppRepository.InstalledApp> all = repository.listApps();
        int shown = 0;

        for (AppRepository.InstalledApp app : all) {
            if (!query.isEmpty()
                    && !app.label().toLowerCase().contains(query)
                    && !app.packageName().toLowerCase().contains(query)) {
                continue;
            }
            cardsContainer.add(buildAppCard(app));
            cardsContainer.add(Box.createVerticalStrut(12));
            shown++;
        }

        if (shown == 0) {
            JLabel empty = new JLabel(
                    "Chưa có ứng dụng nào khớp tìm kiếm. Hãy bấm '+ Cài đặt APK...' hoặc kéo-thả file .apk vào đây.");
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            empty.setForeground(TEXT_MUTED);
            empty.setBorder(new EmptyBorder(30, 10, 30, 10));
            cardsContainer.add(empty);
        }

        cardsContainer.revalidate();
        cardsContainer.repaint();
    }

    private JPanel buildAppCard(AppRepository.InstalledApp app) {
        boolean launching = repository.isLaunching(app.packageName());
        boolean running = repository.isRunning(app.packageName());
        boolean isSelected = selectedApp != null && selectedApp.packageName().equals(app.packageName());
        Color borderCol = launching
                ? ACCENT_BLUE
                : (running ? ACCENT_GREEN : (isSelected ? ACCENT_BLUE : BORDER_COLOR));

        RoundedPanel card = new RoundedPanel(16, BG_CARD, borderCol);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 128));

        // Icon ứng dụng bên trái
        JLabel iconLabel = new JLabel(new ImageIcon(app.icon()));
        iconLabel.setVerticalAlignment(JLabel.TOP);
        card.add(iconLabel, BorderLayout.WEST);

        // Thông tin ở giữa + hàng nút hành động
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        // Dòng 1: Tên ứng dụng + nhãn phiên bản + trạng thái chạy
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nameLbl = new JLabel(app.label());
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        nameLbl.setForeground(TEXT_PRIMARY);
        titleRow.add(nameLbl);

        JLabel verBadge = createBadge("v" + app.versionName() + " (" + app.versionCode() + ")", new Color(0x2B3048),
                TEXT_MUTED);
        titleRow.add(verBadge);

        if (launching) {
            JLabel loadBadge = createBadge("◌ ĐANG TẢI...", new Color(0x1B2B4B), new Color(0x7AA2F7));
            titleRow.add(loadBadge);
        } else if (running) {
            JLabel runBadge = createBadge("● ĐANG CHẠY", new Color(0x193D29), ACCENT_GREEN);
            titleRow.add(runBadge);
        }
        center.add(titleRow);
        center.add(Box.createVerticalStrut(4));

        // Dòng 2: Thông số kỹ thuật của APK
        String abiInfo = app.nativeAbis().isEmpty() ? "Pure DEX (Đa nền tảng)" : "Native: " + app.nativeAbis();
        String meta = String.format("%s  •  API %d→%d  •  %d resources  •  %.1f KB  •  %s",
                app.packageName(), app.minSdk(), app.targetSdk(),
                app.resourceCount(), app.apkSizeBytes() / 1024.0, abiInfo);
        JLabel metaLbl = new JLabel(meta);
        metaLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        metaLbl.setForeground(TEXT_MUTED);
        metaLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(metaLbl);
        center.add(Box.createVerticalStrut(10));

        // Dòng 3: Các nút điều khiển
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (launching) {
            JButton btnFocus = createStyledButton("Xem cửa sổ tải", ACCENT_BLUE, Color.WHITE);
            btnFocus.addActionListener(e -> repository.launchApp(app, this::refreshCards));
            btnRow.add(btnFocus);

            JButton btnCancel = createStyledButton("Hủy", ACCENT_RED, Color.WHITE);
            btnCancel.addActionListener(e -> {
                repository.stopApp(app.packageName());
                refreshCards();
            });
            btnRow.add(btnCancel);
        } else if (running) {
            JButton btnFocus = createStyledButton("Đưa lên trước", ACCENT_GREEN, new Color(0x091A10));
            btnFocus.addActionListener(e -> repository.launchApp(app, this::refreshCards));
            btnRow.add(btnFocus);

            JButton btnStop = createStyledButton("Dừng", ACCENT_RED, Color.WHITE);
            btnStop.addActionListener(e -> {
                repository.stopApp(app.packageName());
                refreshCards();
            });
            btnRow.add(btnStop);
        } else {
            JButton btnLaunch = createStyledButton("Chạy ứng dụng →", ACCENT_BLUE, Color.WHITE);
            btnLaunch.addActionListener(e -> {
                selectedApp = app;
                updateInspector(app);
                repository.launchApp(app, this::refreshCards);
            });
            btnRow.add(btnLaunch);
        }

        JButton btnInspect = createStyledButton("Soi APK", new Color(0x2B314B), TEXT_PRIMARY);
        btnInspect.addActionListener(e -> {
            selectedApp = app;
            updateInspector(app);
            refreshCards();
        });
        btnRow.add(btnInspect);

        JButton btnScan = createStyledButton("Quét API", new Color(0x243B55), new Color(0x90CAF9));
        btnScan.addActionListener(e -> scanCompatibility(app));
        btnRow.add(btnScan);

        JButton btnReset = createStyledButton("Xóa dữ liệu", new Color(0x2B314B), TEXT_MUTED);
        btnReset.addActionListener(e -> {
            repository.clearAppData(app.packageName());
            JOptionPane.showMessageDialog(frame,
                    "Đã xóa SharedPreferences và bộ nhớ riêng của " + app.label(),
                    "Xóa dữ liệu ứng dụng", JOptionPane.INFORMATION_MESSAGE);
        });
        btnRow.add(btnReset);

        JButton btnUninstall = createStyledButton("Gỡ cài đặt", new Color(0x362229), new Color(0xFF8A80));
        btnUninstall.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(frame,
                    "Bạn có chắc muốn gỡ cài đặt \"" + app.label() + "\" (" + app.packageName() + ")?",
                    "Xác nhận gỡ APK", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                repository.uninstallApp(app.packageName());
                if (selectedApp != null && selectedApp.packageName().equals(app.packageName())) {
                    List<AppRepository.InstalledApp> rem = repository.listApps();
                    selectedApp = rem.isEmpty() ? null : rem.get(0);
                    updateInspector(selectedApp);
                }
                refreshCards();
            }
        });
        btnRow.add(btnUninstall);

        center.add(btnRow);
        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private void updateInspector(AppRepository.InstalledApp app) {
        if (inspectorArea == null)
            return;
        if (app == null) {
            inspectorArea.setText("Chưa chọn ứng dụng nào.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Tên ứng dụng : ").append(app.label()).append('\n');
        sb.append("Package      : ").append(app.packageName()).append('\n');
        sb.append("Phiên bản    : ").append(app.versionName()).append(" (code ").append(app.versionCode())
                .append(")\n");
        sb.append("SDK          : minSdk=").append(app.minSdk()).append(", targetSdk=").append(app.targetSdk())
                .append('\n');
        sb.append("Launcher     : ").append(app.launcherActivity()).append('\n');
        sb.append("Tài nguyên   : ").append(app.resourceCount()).append(" entries trong resources.arsc\n");
        sb.append("Native ABIs  : ").append(app.nativeAbis().isEmpty() ? "Không (Thuần DEX/JVM)" : app.nativeAbis())
                .append('\n');
        sb.append("Đường dẫn    : ").append(app.apkPath().toAbsolutePath()).append("\n\n");

        sb.append("--- DANH SÁCH ACTIVITY (").append(app.activities().size()).append(") ---\n");
        for (String a : app.activities()) {
            sb.append(" • ").append(a).append('\n');
        }

        if (!app.permissions().isEmpty()) {
            sb.append("\n--- QUYỀN TRUY CẬP (PERMISSIONS) ---\n");
            for (String p : app.permissions()) {
                sb.append(" • ").append(p).append('\n');
            }
        }

        sb.append("\n--- ANDROIDMANIFEST.XML (ĐÃ GIẢI MÃ TỪ AXML) ---\n");
        sb.append(app.manifestXmlDump());
        inspectorArea.setText(sb.toString());
        inspectorArea.setCaretPosition(0);
    }

    private void scanCompatibility(AppRepository.InstalledApp app) {
        selectedApp = app;
        refreshCards();
        appendLog("[AAOW-Scan] Đang quét tương thích tĩnh cho " + app.label() + " (" + app.packageName() + ")...");
        if (inspectorArea != null) {
            inspectorArea
                    .setText("Đang phân tích bytecode DEX của " + app.label() + "...\nVui lòng chờ trong giây lát.");
        }
        new javax.swing.SwingWorker<ApkCompatibilityScanner.ScanReport, Void>() {
            @Override
            protected ApkCompatibilityScanner.ScanReport doInBackground() throws Exception {
                return ApkCompatibilityScanner.scan(app.apkPath(), repository.getDexCacheDir());
            }

            @Override
            protected void done() {
                try {
                    ApkCompatibilityScanner.ScanReport report = get();
                    if (inspectorArea != null) {
                        inspectorArea.setText(report.formatReport());
                        inspectorArea.setCaretPosition(0);
                    }
                    appendLog(String.format("[AAOW-Scan] Hoàn tất %s: Class %.1f%% | Method %.1f%% | Call-sites %.1f%%",
                            app.packageName(),
                            report.classCoveragePercent(),
                            report.uniqueMethodCoveragePercent(),
                            report.callSiteCoveragePercent()));
                } catch (Exception ex) {
                    appendLog("[AAOW-ERROR] Lỗi quét " + app.packageName() + ": " + ex.getMessage());
                    if (inspectorArea != null) {
                        inspectorArea.setText("Lỗi khi quét APK:\n" + ex);
                    }
                }
            }
        }.execute();
    }

    private void promptInstallApk() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("Chọn một hoặc nhiều file Android APK để cài vào AAOW App Hub");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Android Package (*.apk)", "apk"));
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            List<Path> toInstall = new ArrayList<>();
            File[] files = chooser.getSelectedFiles();
            if (files != null && files.length > 0) {
                for (File f : files) {
                    toInstall.add(f.toPath());
                }
            } else if (chooser.getSelectedFile() != null) {
                toInstall.add(chooser.getSelectedFile().toPath());
            }
            if (!toInstall.isEmpty()) {
                installApksAsync(toInstall);
            }
        }
    }

    private void promptImportOfflineBundle() {
        Path defaultDir = AppRepository.findDefaultOfflineApkDir();
        Path targetDir = null;
        if (defaultDir != null) {
            Object[] options = { "Nạp từ " + defaultDir.getFileName(), "Chọn thư mục khác...", "Hủy" };
            int choice = JOptionPane.showOptionDialog(frame,
                    "Tìm thấy thư mục chứa bộ APK Offline:\n" + defaultDir
                            + "\nBạn muốn nạp toàn bộ APK trong thư mục này vào AAOW App Hub?",
                    "Nạp bộ ứng dụng Offline",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);
            if (choice == 0) {
                targetDir = defaultDir;
            } else if (choice == 1) {
                targetDir = chooseDirectory("Chọn thư mục chứa các file .apk");
            }
        } else {
            targetDir = chooseDirectory("Chọn thư mục chứa các file .apk");
        }
        if (targetDir == null)
            return;

        final Path dirToImport = targetDir;
        appendLog("[AAOW-Hub] Đang nạp hàng loạt APK từ " + dirToImport + "...");
        if (installBanner != null) {
            installBanner.start("Đang nạp bộ APK từ " + dirToImport.getFileName() + "...",
                    "Đang quét danh sách file .apk...");
        }
        new SwingWorker<List<AppRepository.InstalledApp>, ProgressUpdate>() {
            @Override
            protected List<AppRepository.InstalledApp> doInBackground() throws Exception {
                return repository.installApksFromDirectory(dirToImport,
                        (p, msg) -> publish(new ProgressUpdate(p, msg)));
            }

            @Override
            protected void process(List<ProgressUpdate> chunks) {
                if (installBanner != null && !chunks.isEmpty()) {
                    ProgressUpdate last = chunks.get(chunks.size() - 1);
                    installBanner.updateProgress(last.progress(), last.message());
                }
            }

            @Override
            protected void done() {
                if (installBanner != null) {
                    installBanner.finishAndHide();
                }
                try {
                    List<AppRepository.InstalledApp> list = get();
                    if (!list.isEmpty()) {
                        selectedApp = list.get(0);
                        updateInspector(selectedApp);
                    }
                    refreshCards();
                    if (frame != null) {
                        JOptionPane.showMessageDialog(frame,
                                "Đã nạp thành công " + list.size() + " ứng dụng Android vào AAOW App Hub.",
                                "Hoàn tất nạp APK", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    appendLog("[AAOW-ERROR] Lỗi nạp thư mục APK: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private Path chooseDirectory(String title) {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            return chooser.getSelectedFile().toPath();
        }
        return null;
    }

    private void installApksAsync(List<Path> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        final List<Path> batch = new ArrayList<>(files);
        String headline = batch.size() == 1
                ? "Đang cài đặt & biên dịch " + batch.get(0).getFileName() + "..."
                : "Đang cài đặt & biên dịch " + batch.size() + " file APK...";
        appendLog("[AAOW-Hub] " + headline);
        if (installBanner != null) {
            installBanner.start(headline, "Đang đọc cấu trúc APK...");
        }

        new SwingWorker<List<AppRepository.InstalledApp>, ProgressUpdate>() {
            private final List<String> errors = new ArrayList<>();

            @Override
            protected List<AppRepository.InstalledApp> doInBackground() {
                List<AppRepository.InstalledApp> installed = new ArrayList<>();
                int total = batch.size();
                for (int i = 0; i < total; i++) {
                    Path file = batch.get(i);
                    final int idx = i;
                    String prefix = total > 1
                            ? "(" + (i + 1) + "/" + total + ") " + file.getFileName() + ": "
                            : "";
                    try {
                        installed.add(repository.installApk(file,
                                (p, msg) -> publish(new ProgressUpdate((idx + p) / total, prefix + msg))));
                    } catch (Exception e) {
                        errors.add(file.getFileName() + ": " + e.getMessage());
                        appendLog("[AAOW-ERROR] Không cài đặt được " + file.getFileName() + ": " + e.getMessage());
                    }
                }
                return installed;
            }

            @Override
            protected void process(List<ProgressUpdate> chunks) {
                if (installBanner != null && !chunks.isEmpty()) {
                    ProgressUpdate last = chunks.get(chunks.size() - 1);
                    installBanner.updateProgress(last.progress(), last.message());
                }
            }

            @Override
            protected void done() {
                if (installBanner != null) {
                    installBanner.finishAndHide();
                }
                try {
                    List<AppRepository.InstalledApp> list = get();
                    if (!list.isEmpty()) {
                        selectedApp = list.get(list.size() - 1);
                        updateInspector(selectedApp);
                    }
                    refreshCards();
                    if (!errors.isEmpty() && frame != null) {
                        JOptionPane.showMessageDialog(frame,
                                "Không thể cài đặt một số file APK:\n" + String.join("\n", errors),
                                "Lỗi cài đặt APK", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    appendLog("[AAOW-ERROR] Lỗi cài đặt APK: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void installDragAndDrop(JFrame targetFrame) {
        new DropTarget(targetFrame, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            @SuppressWarnings("unchecked")
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>) dtde.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    List<Path> apks = new ArrayList<>();
                    for (File f : droppedFiles) {
                        if (f.getName().toLowerCase().endsWith(".apk")) {
                            apks.add(f.toPath());
                        } else {
                            appendLog("[AAOW-Hub] Bỏ qua file không phải .apk: " + f.getName());
                        }
                    }
                    if (!apks.isEmpty()) {
                        installApksAsync(apks);
                    }
                    dtde.dropComplete(true);
                } catch (Exception ex) {
                    dtde.dropComplete(false);
                }
            }
        }, true);
    }

    private void appendLog(String msg) {
        if (logArea == null)
            return;
        SwingUtilities.invokeLater(() -> {
            String line = LocalTime.now().format(TIME_FMT) + "  " + msg + "\n";
            logArea.append(line);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private static JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = getModel().isPressed() ? bg.darker() : (getModel().isRollover() ? bg.brighter() : bg);
                g2.setColor(fill);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 10, 6, 10));
        return btn;
    }

    private static JLabel createBadge(String text, Color bg, Color fg) {
        JLabel lbl = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(fg);
        lbl.setBorder(new EmptyBorder(2, 8, 2, 8));
        return lbl;
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;
        private final Color stroke;

        RoundedPanel(int radius, Color fill, Color stroke) {
            this.radius = radius;
            this.fill = fill;
            this.stroke = stroke;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, radius, radius));
            if (stroke != null) {
                g2.setColor(stroke);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, radius, radius));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Thanh tiến trình cài đặt & biên dịch DEX có vòng xoay 60fps và hiệu ứng
     * shimmer.
     */
    private static final class InstallLoadingBanner extends RoundedPanel {
        private final Timer animTimer;
        private String title = "Đang cài đặt APK...";
        private String detail = "";
        private float targetProgress = 0.05f;
        private float displayedProgress = 0.03f;
        private float phase = 0f;

        InstallLoadingBanner() {
            super(14, new Color(0x161D33), ACCENT_BLUE);
            setPreferredSize(new Dimension(0, 74));
            setMinimumSize(new Dimension(0, 74));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
            this.animTimer = new Timer(16, e -> {
                phase += 0.10f;
                displayedProgress += (targetProgress - displayedProgress) * 0.22f;
                if (Math.abs(targetProgress - displayedProgress) < 0.002f) {
                    displayedProgress = targetProgress;
                }
                repaint();
            });
        }

        void start(String titleText, String detailText) {
            this.title = titleText != null ? titleText : "Đang cài đặt APK...";
            this.detail = detailText != null ? detailText : "";
            this.targetProgress = 0.05f;
            this.displayedProgress = 0.03f;
            this.phase = 0f;
            setVisible(true);
            if (!animTimer.isRunning()) {
                animTimer.start();
            }
            revalidate();
            repaint();
        }

        void updateProgress(float p, String detailText) {
            this.targetProgress = Math.max(0.03f, Math.min(1.0f, p));
            if (detailText != null && !detailText.isBlank()) {
                this.detail = detailText;
            }
            repaint();
        }

        void finishAndHide() {
            this.targetProgress = 1.0f;
            this.displayedProgress = 1.0f;
            animTimer.stop();
            setVisible(false);
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            // Vòng xoay kép bên trái
            int cx = 28;
            int cy = h / 2;
            int r1 = 13;
            g2.setColor(new Color(255, 255, 255, 24));
            g2.setStroke(new BasicStroke(2.4f));
            g2.drawOval(cx - r1, cy - r1, r1 * 2, r1 * 2);

            g2.setColor(new Color(0x7C8CFF));
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Arc2D.Float(cx - r1, cy - r1, r1 * 2, r1 * 2, (phase * 57.3f) % 360f, 125f, Arc2D.OPEN));

            int r2 = 8;
            g2.setColor(ACCENT_GREEN);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Arc2D.Float(cx - r2, cy - r2, r2 * 2, r2 * 2, (-phase * 42f + 180f) % 360f, 95f, Arc2D.OPEN));

            int textX = 54;
            int rightPad = 16;
            int pct = Math.round(displayedProgress * 100);
            String pctStr = pct + "%";
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.setColor(new Color(0x8CE9B0));
            FontMetrics pfm = g2.getFontMetrics();
            int pctW = pfm.stringWidth(pctStr);
            g2.drawString(pctStr, w - rightPad - pctW, 25);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.setColor(TEXT_PRIMARY);
            FontMetrics tfm = g2.getFontMetrics();
            int maxTitleW = Math.max(80, w - textX - rightPad - pctW - 12);
            String t = title;
            while (t.length() > 1 && tfm.stringWidth(t) > maxTitleW) {
                t = t.substring(0, t.length() - 2) + "…";
            }
            g2.drawString(t, textX, 25);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(TEXT_MUTED);
            FontMetrics dfm = g2.getFontMetrics();
            int maxDetW = Math.max(80, w - textX - rightPad);
            String d = detail;
            while (d.length() > 1 && dfm.stringWidth(d) > maxDetW) {
                d = d.substring(0, d.length() - 2) + "…";
            }
            g2.drawString(d, textX, 44);

            // Thanh tiến trình + shimmer
            int barX = textX;
            int barY = 54;
            int barW = Math.max(60, w - textX - rightPad);
            g2.setColor(new Color(0x232B47));
            g2.fill(new RoundRectangle2D.Float(barX, barY, barW, 7, 7, 7));

            int fillW = Math.max(10, Math.round(barW * Math.max(0.03f, Math.min(1f, displayedProgress))));
            Shape fillShape = new RoundRectangle2D.Float(barX, barY, fillW, 7, 7, 7);
            g2.setPaint(new GradientPaint(barX, barY, ACCENT_BLUE, barX + barW, barY, ACCENT_GREEN));
            g2.fill(fillShape);

            Shape oldClip = g2.getClip();
            g2.setClip(fillShape);
            float sx = barX + ((phase * 36f) % (barW + 70)) - 35f;
            g2.setPaint(new GradientPaint(sx, barY, new Color(255, 255, 255, 0),
                    sx + 20f, barY, new Color(255, 255, 255, 130), true));
            g2.fillRect(Math.round(sx), barY, 40, 7);
            g2.setClip(oldClip);

            g2.dispose();
        }
    }
}
