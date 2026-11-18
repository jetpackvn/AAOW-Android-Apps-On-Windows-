package android.widget;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;

/**
 * EditText của Android tích hợp bàn phím và Clipboard của Windows.
 */
public class EditText extends TextView {
    private static final int TYPE_TEXT_FLAG_MULTI_LINE = 0x20000;

    private int cursorIndex;
    private int selectionStart;
    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public EditText(Context context) {
        this(context, null, 0);
    }

    public EditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFocusable(true);
        setClickable(true);
        boolean multiLine = (inputType & TYPE_TEXT_FLAG_MULTI_LINE) != 0
                || (attrs instanceof AttributeSet.XmlAttributeSet xas && xas.element.attr("singleLine") == null
                && ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.TOP || maxLines > 1));
        if (!multiLine && !(attrs instanceof AttributeSet.XmlAttributeSet xas2 && xas2.element.attr("singleLine") != null)) {
            if ((inputType & TYPE_TEXT_FLAG_MULTI_LINE) == 0 && (gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.TOP) {
                // Keep XML top gravity as multiline editor
                multiLine = true;
            } else {
                setSingleLine(true);
            }
        }
        if (paddingLeft == 0 && paddingRight == 0) {
            setPadding(12, 10, 12, 10);
        }
        if (getMinimumHeight() == 0) {
            setMinimumHeight(44);
        }
        if (!multiLine && (gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.TOP
                && !(attrs instanceof AttributeSet.XmlAttributeSet xas && xas.element.attr("gravity") != null)) {
            setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        }
        this.cursorIndex = text.length();
        this.selectionStart = this.cursorIndex;
    }

    @Override
    public Editable getText() {
        return getEditableText();
    }

    @Override
    public void setText(CharSequence text) {
        super.setText(text);
        cursorIndex = Math.min(cursorIndex, this.text.length());
        selectionStart = Math.min(selectionStart, this.text.length());
    }

    public void setSelection(int index) {
        int clamped = Math.max(0, Math.min(index, text.length()));
        this.selectionStart = clamped;
        this.cursorIndex = clamped;
        invalidate();
    }

    public void setSelection(int start, int stop) {
        int len = text.length();
        this.selectionStart = Math.max(0, Math.min(start, len));
        this.cursorIndex = Math.max(0, Math.min(stop, len));
        invalidate();
    }

    public void selectAll() {
        setSelection(0, text.length());
    }

    public void extendSelection(int index) {
        this.cursorIndex = Math.max(0, Math.min(index, text.length()));
        invalidate();
    }

    public int getSelectionStart() {
        return Math.min(selectionStart, cursorIndex);
    }

    public int getSelectionEnd() {
        return Math.max(selectionStart, cursorIndex);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            cursorIndex = text.length();
            selectionStart = cursorIndex;
            invalidate();
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onHostKeyTyped(char ch) {
        if (!isEnabled()) return;
        if (ch >= 32 && ch != 127) {
            insertAtCursor(String.valueOf(ch));
        }
    }

    private void insertAtCursor(String ins) {
        String cur = text.toString();
        int selA = Math.max(0, Math.min(getSelectionStart(), cur.length()));
        int selB = Math.max(selA, Math.min(getSelectionEnd(), cur.length()));
        String next = cur.substring(0, selA) + ins + cur.substring(selB);
        cursorIndex = selA + ins.length();
        selectionStart = cursorIndex;
        super.setText(next);
    }

    private ClipboardManager getAndroidClipboard() {
        Context ctx = getContext();
        return ctx != null ? ctx.getSystemService(ClipboardManager.class) : null;
    }

    @Override
    public void onHostKeyPressed(KeyEvent e) {
        if (!isEnabled()) return;
        String cur = text.toString();
        cursorIndex = Math.max(0, Math.min(cursorIndex, cur.length()));
        int code = e.getKeyCode();

        if (e.isControlDown()) {
            if (code == KeyEvent.VK_V) {
                String clip = null;
                ClipboardManager cm = getAndroidClipboard();
                if (cm != null && cm.hasPrimaryClip()) {
                    CharSequence cs = cm.getText();
                    if (cs != null) clip = cs.toString();
                } else {
                    try {
                        clip = (String) Toolkit.getDefaultToolkit()
                                .getSystemClipboard().getData(DataFlavor.stringFlavor);
                    } catch (Exception ignored) {
                    }
                }
                if (clip != null && !clip.isEmpty()) {
                    clip = clip.replace("\r", "");
                    if (singleLine) clip = clip.replace("\n", " ");
                    insertAtCursor(clip);
                }
                return;
            } else if (code == KeyEvent.VK_C || code == KeyEvent.VK_X) {
                int selA = getSelectionStart();
                int selB = getSelectionEnd();
                String toCopy = selB > selA ? cur.substring(selA, selB) : cur;
                ClipboardManager cm = getAndroidClipboard();
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("EditText", toCopy));
                } else {
                    try {
                        Toolkit.getDefaultToolkit().getSystemClipboard()
                                .setContents(new StringSelection(toCopy), null);
                    } catch (Exception ignored) {
                    }
                }
                if (code == KeyEvent.VK_X && selB > selA) {
                    insertAtCursor("");
                }
                return;
            } else if (code == KeyEvent.VK_A) {
                selectAll();
                return;
            }
        }

        switch (code) {
            case KeyEvent.VK_ENTER -> {
                if (!singleLine) {
                    insertAtCursor("\n");
                }
            }
            case KeyEvent.VK_BACK_SPACE -> {
                if (cursorIndex > 0) {
                    String next = cur.substring(0, cursorIndex - 1) + cur.substring(cursorIndex);
                    cursorIndex--;
                    selectionStart = cursorIndex;
                    super.setText(next);
                }
            }
            case KeyEvent.VK_DELETE -> {
                if (cursorIndex < cur.length()) {
                    String next = cur.substring(0, cursorIndex) + cur.substring(cursorIndex + 1);
                    super.setText(next);
                }
            }
            case KeyEvent.VK_LEFT -> {
                if (cursorIndex > 0) {
                    cursorIndex--;
                    selectionStart = cursorIndex;
                    invalidate();
                }
            }
            case KeyEvent.VK_RIGHT -> {
                if (cursorIndex < cur.length()) {
                    cursorIndex++;
                    selectionStart = cursorIndex;
                    invalidate();
                }
            }
            case KeyEvent.VK_HOME -> {
                cursorIndex = 0;
                selectionStart = 0;
                invalidate();
            }
            case KeyEvent.VK_END -> {
                cursorIndex = cur.length();
                selectionStart = cursorIndex;
                invalidate();
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (getVisibility() != VISIBLE) return;
        int w = getWidth();
        int h = getHeight();
        if (getBackground() == null) {
            if (singleLine || h <= 72) {
                boxPaint.setStyle(Paint.Style.FILL);
                boxPaint.setColor(isFocused() ? 0xFFF5F6FF : 0xFFF3F4F6);
                canvas.drawRoundRect(0, 2, w, h - 2, 10, 10, boxPaint);

                boxPaint.setStyle(Paint.Style.STROKE);
                boxPaint.setStrokeWidth(isFocused() ? 2f : 1f);
                boxPaint.setColor(isFocused() ? 0xFF4F5BD5 : 0xFFC5C6D0);
                canvas.drawRoundRect(1, 3, w - 1, h - 3, 10, 10, boxPaint);
            } else {
                boxPaint.setStyle(Paint.Style.FILL);
                boxPaint.setColor(0xFFFFFFFF);
                canvas.drawRect(0, 0, w, h, boxPaint);
            }
        }
        super.draw(canvas);

        if (isFocused()) {
            Graphics2D g = canvas.getAwtGraphics();
            if (g != null) {
                Font font = typeface.toAwtFont(textSizePx);
                FontMetrics fm = g.getFontMetrics(font);
                String before = text.subSequence(0, Math.min(cursorIndex, text.length())).toString();
                int lastNl = before.lastIndexOf('\n');
                String linePart = lastNl >= 0 ? before.substring(lastNl + 1) : before;
                int lineIdx = 0;
                for (int i = 0; i < before.length(); i++) {
                    if (before.charAt(i) == '\n') lineIdx++;
                }
                int lineHeight = Math.max((int) (textSizePx * 1.28f), fm.getHeight());
                int cx = paddingLeft + fm.stringWidth(linePart);
                int topY = (singleLine || h <= 72)
                        ? (h - fm.getHeight()) / 2
                        : paddingTop + lineIdx * lineHeight;
                boxPaint.setStyle(Paint.Style.FILL);
                boxPaint.setColor(0xFF4F5BD5);
                canvas.drawRect(cx, topY, cx + 2, topY + fm.getHeight(), boxPaint);
            }
        }
    }
}
