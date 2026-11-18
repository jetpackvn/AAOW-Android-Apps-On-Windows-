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
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Giao diện trung tâm quản lý ứng dụng Android trên Windows (AAOW App Hub).
 * Hỗ trợ kéo-thả cài đặt APK, mở nhiều ứng dụng đồng thời, soi cấu trúc APK và quản lý dữ liệu.
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
    private AppRepository.InstalledApp selectedApp;

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
            frame.setContentPane(buildRootPanel());
            installDragAndDrop(frame);
            refreshCards();
            updateInspector(selectedApp);
            appendLog("[AAOW-Hub] Sẵn sàng. Kéo-thả file .apk vào cửa sổ hoặc bấm '+ Cài đặt APK' để thêm ứng dụng.");
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
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
        leftPane.add(cardsScroll, BorderLayout.CENTER);
        leftPane.add(buildDropHintBanner(), BorderLayout.SOUTH);

        // Cột phải: Inspector + Console log
        JPanel rightPane = buildRightInspectorAndConsole();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPane, rightPane);
        split.setResizeWeight(0.62);
        split.setDividerLocation(640);
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
                new EmptyBorder(14, 20, 14, 20)
        ));

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

        searchField = new JTextField(18);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBackground(BG_CARD);
        searchField.setForeground(TEXT_PRIMARY);
        searchField.setCaretColor(TEXT_PRIMARY);
        searchField.setToolTipText("Lọc theo tên ứng dụng hoặc tên gói (package)");
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
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

        actions.add(searchLbl);
        actions.add(searchField);
        actions.add(btnInstall);
        header.add(actions, BorderLayout.EAST);

        return header;
    }

    private JPanel buildDropHintBanner() {
        JPanel banner = new RoundedPanel(14, new Color(0x161926), BORDER_COLOR);
        banner.setLayout(new BorderLayout());
        banner.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel lbl = new JLabel("Mẹo: Kéo-thả bất kỳ file .apk nào từ Windows Explorer vào cửa sổ này để cài đặt và biên dịch DEX tức thì.");
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
        if (statsLabel == null) return;
        int total = repository.listApps().size();
        int running = repository.getRunningCount();
        statsLabel.setText(String.format(
                "Kho ứng dụng: %d APK đã cài • Đang chạy: %d cửa sổ • Chế độ: Direct JVM / Java2D (Không máy ảo)",
                total, running));
    }

    public void refreshCards() {
        if (cardsContainer == null) return;
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
            JLabel empty = new JLabel("Chưa có ứng dụng nào khớp tìm kiếm. Hãy bấm '+ Cài đặt APK...' hoặc kéo-thả file .apk vào đây.");
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            empty.setForeground(TEXT_MUTED);
            empty.setBorder(new EmptyBorder(30, 10, 30, 10));
            cardsContainer.add(empty);
        }

        cardsContainer.revalidate();
        cardsContainer.repaint();
    }

    private JPanel buildAppCard(AppRepository.InstalledApp app) {
        boolean running = repository.isRunning(app.packageName());
        boolean isSelected = selectedApp != null && selectedApp.packageName().equals(app.packageName());
        Color borderCol = running ? ACCENT_GREEN : (isSelected ? ACCENT_BLUE : BORDER_COLOR);

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

        JLabel verBadge = createBadge("v" + app.versionName() + " (" + app.versionCode() + ")", new Color(0x2B3048), TEXT_MUTED);
        titleRow.add(verBadge);

        if (running) {
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
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (running) {
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
                repository.launchApp(app, () -> SwingUtilities.invokeLater(this::refreshCards));
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
        if (inspectorArea == null) return;
        if (app == null) {
            inspectorArea.setText("Chưa chọn ứng dụng nào.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Tên ứng dụng : ").append(app.label()).append('\n');
        sb.append("Package      : ").append(app.packageName()).append('\n');
        sb.append("Phiên bản    : ").append(app.versionName()).append(" (code ").append(app.versionCode()).append(")\n");
        sb.append("SDK          : minSdk=").append(app.minSdk()).append(", targetSdk=").append(app.targetSdk()).append('\n');
        sb.append("Launcher     : ").append(app.launcherActivity()).append('\n');
        sb.append("Tài nguyên   : ").append(app.resourceCount()).append(" entries trong resources.arsc\n");
        sb.append("Native ABIs  : ").append(app.nativeAbis().isEmpty() ? "Không (Thuần DEX/JVM)" : app.nativeAbis()).append('\n');
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

    private void promptInstallApk() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("Chọn file Android APK để cài vào AAOW App Hub");
        chooser.setFileFilter(new FileNameExtensionFilter("Android Package (*.apk)", "apk"));
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            installApkFromFile(chooser.getSelectedFile().toPath());
        }
    }

    private void installApkFromFile(Path file) {
        try {
            AppRepository.InstalledApp installed = repository.installApk(file);
            selectedApp = installed;
            updateInspector(installed);
            refreshCards();
        } catch (Exception e) {
            appendLog("[AAOW-ERROR] Không cài đặt được " + file.getFileName() + ": " + e.getMessage());
            if (frame != null) {
                JOptionPane.showMessageDialog(frame,
                        "Không thể cài đặt file APK:\n" + e.getMessage(),
                        "Lỗi cài đặt APK", JOptionPane.ERROR_MESSAGE);
            }
        }
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
                    for (File f : droppedFiles) {
                        if (f.getName().toLowerCase().endsWith(".apk")) {
                            installApkFromFile(f.toPath());
                        } else {
                            appendLog("[AAOW-Hub] Bỏ qua file không phải .apk: " + f.getName());
                        }
                    }
                    dtde.dropComplete(true);
                } catch (Exception ex) {
                    dtde.dropComplete(false);
                }
            }
        }, true);
    }

    private void appendLog(String msg) {
        if (logArea == null) return;
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
        btn.setBorder(new EmptyBorder(7, 13, 7, 13));
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

    private static final class RoundedPanel extends JPanel {
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
}
