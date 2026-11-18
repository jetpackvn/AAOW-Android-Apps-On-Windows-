package android.content;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Đồng bộ Clipboard hai chiều giữa ứng dụng Android và Windows System Clipboard.
 */
public class ClipboardManager extends android.text.ClipboardManager {
    public interface OnPrimaryClipChangedListener {
        void onPrimaryClipChanged();
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<OnPrimaryClipChangedListener> listeners = new CopyOnWriteArrayList<>();
    private volatile ClipData lastLocalClip;

    public ClipboardManager(Context context) {
        this.context = context;
    }

    private static Clipboard getSystemClipboard() {
        if (GraphicsEnvironment.isHeadless()) return null;
        try {
            return Toolkit.getDefaultToolkit().getSystemClipboard();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public void setPrimaryClip(ClipData clip) {
        this.lastLocalClip = clip;
        Clipboard sys = getSystemClipboard();
        if (sys != null) {
            if (clip == null || clip.getItemCount() == 0) {
                try {
                    sys.setContents(new StringSelection(""), null);
                } catch (Throwable ignored) {
                }
            } else {
                ClipData.Item first = clip.getItemAt(0);
                CharSequence coerced = first.coerceToText(context);
                String plain = coerced != null ? coerced.toString() : "";
                String html = first.getHtmlText();
                try {
                    if (html != null && !html.isEmpty()) {
                        sys.setContents(new HtmlAndTextTransferable(plain, html), null);
                    } else {
                        sys.setContents(new StringSelection(plain), null);
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        notifyListeners();
    }

    public void clearPrimaryClip() {
        setPrimaryClip(null);
    }

    public ClipData getPrimaryClip() {
        Clipboard sys = getSystemClipboard();
        if (sys != null) {
            try {
                Transferable t = sys.getContents(null);
                if (t != null) {
                    if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                        if (files != null && !files.isEmpty()) {
                            ClipData cd = ClipData.newRawUri("Windows Files", Uri.fromFile(files.get(0)));
                            for (int i = 1; i < files.size(); i++) {
                                cd.addItem(new ClipData.Item(Uri.fromFile(files.get(i))));
                            }
                            return cd;
                        }
                    }
                    String text = null;
                    String html = null;
                    if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        text = (String) t.getTransferData(DataFlavor.stringFlavor);
                    }
                    if (t.isDataFlavorSupported(DataFlavor.allHtmlFlavor)) {
                        html = (String) t.getTransferData(DataFlavor.allHtmlFlavor);
                    }
                    if (lastLocalClip != null && lastLocalClip.getItemCount() > 0) {
                        CharSequence localTxt = lastLocalClip.getItemAt(0).coerceToText(context);
                        if (text != null && text.equals(localTxt != null ? localTxt.toString() : null)) {
                            return lastLocalClip;
                        }
                    }
                    if (html != null && text != null) {
                        return ClipData.newHtmlText("Windows Clipboard", text, html);
                    }
                    if (text != null) {
                        return ClipData.newPlainText("Windows Clipboard", text);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return lastLocalClip;
    }

    public ClipDescription getPrimaryClipDescription() {
        ClipData cd = getPrimaryClip();
        return cd != null ? cd.getDescription() : null;
    }

    public boolean hasPrimaryClip() {
        ClipData cd = getPrimaryClip();
        return cd != null && cd.getItemCount() > 0;
    }

    @Override
    @Deprecated
    public CharSequence getText() {
        ClipData cd = getPrimaryClip();
        if (cd != null && cd.getItemCount() > 0) {
            return cd.getItemAt(0).coerceToText(context);
        }
        return null;
    }

    @Override
    @Deprecated
    public void setText(CharSequence text) {
        setPrimaryClip(ClipData.newPlainText(null, text != null ? text : ""));
    }

    @Override
    @Deprecated
    public boolean hasText() {
        CharSequence t = getText();
        return t != null && t.length() > 0;
    }

    public void addPrimaryClipChangedListener(OnPrimaryClipChangedListener what) {
        if (what != null && !listeners.contains(what)) {
            listeners.add(what);
        }
    }

    public void removePrimaryClipChangedListener(OnPrimaryClipChangedListener what) {
        listeners.remove(what);
    }

    private void notifyListeners() {
        for (OnPrimaryClipChangedListener l : listeners) {
            mainHandler.post(l::onPrimaryClipChanged);
        }
    }

    private static final class HtmlAndTextTransferable implements Transferable {
        private final String plain;
        private final String html;
        private final DataFlavor[] flavors = {
                DataFlavor.stringFlavor,
                DataFlavor.allHtmlFlavor
        };

        HtmlAndTextTransferable(String plain, String html) {
            this.plain = plain;
            this.html = html;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.stringFlavor.equals(flavor) || DataFlavor.allHtmlFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (DataFlavor.stringFlavor.equals(flavor)) return plain;
            if (DataFlavor.allHtmlFlavor.equals(flavor)) return html;
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
