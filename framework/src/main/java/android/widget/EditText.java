package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
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
    private int cursorIndex;
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
        setSingleLine(true);
        if (paddingLeft == 0 && paddingRight == 0) {
            setPadding(12, 10, 12, 10);
        }
        setMinimumHeight(44);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        this.cursorIndex = text.length();
    }

    @Override
    public Editable getText() {
        return getEditableText();
    }

    @Override
    public void setText(CharSequence text) {
        super.setText(text);
        cursorIndex = Math.min(cursorIndex, this.text.length());
    }

    public void setSelection(int index) {
        cursorIndex = Math.max(0, Math.min(index, text.length()));
        invalidate();
    }

    public int getSelectionStart() {
        return cursorIndex;
    }

    public int getSelectionEnd() {
        return cursorIndex;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            cursorIndex = text.length();
            invalidate();
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onHostKeyTyped(char ch) {
        if (!isEnabled()) return;
        if (ch >= 32 && ch != 127) {
            String cur = text.toString();
            cursorIndex = Math.max(0, Math.min(cursorIndex, cur.length()));
            String next = cur.substring(0, cursorIndex) + ch + cur.substring(cursorIndex);
            cursorIndex++;
            super.setText(next);
        }
    }

    @Override
    public void onHostKeyPressed(KeyEvent e) {
        if (!isEnabled()) return;
        String cur = text.toString();
        cursorIndex = Math.max(0, Math.min(cursorIndex, cur.length()));
        int code = e.getKeyCode();

        if (e.isControlDown()) {
            if (code == KeyEvent.VK_V) {
                try {
                    String clip = (String) Toolkit.getDefaultToolkit()
                            .getSystemClipboard().getData(DataFlavor.stringFlavor);
                    if (clip != null && !clip.isEmpty()) {
                        clip = clip.replace("\r", "").replace("\n", " ");
                        String next = cur.substring(0, cursorIndex) + clip + cur.substring(cursorIndex);
                        cursorIndex += clip.length();
                        super.setText(next);
                    }
                } catch (Exception ignored) {
                }
                return;
            } else if (code == KeyEvent.VK_C) {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(cur), null);
                return;
            }
        }

        switch (code) {
            case KeyEvent.VK_BACK_SPACE -> {
                if (cursorIndex > 0) {
                    String next = cur.substring(0, cursorIndex - 1) + cur.substring(cursorIndex);
                    cursorIndex--;
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
                    invalidate();
                }
            }
            case KeyEvent.VK_RIGHT -> {
                if (cursorIndex < cur.length()) {
                    cursorIndex++;
                    invalidate();
                }
            }
            case KeyEvent.VK_HOME -> {
                cursorIndex = 0;
                invalidate();
            }
            case KeyEvent.VK_END -> {
                cursorIndex = cur.length();
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
            boxPaint.setStyle(Paint.Style.FILL);
            boxPaint.setColor(isFocused() ? 0xFFF5F6FF : 0xFFF3F4F6);
            canvas.drawRoundRect(0, 2, w, h - 2, 10, 10, boxPaint);

            boxPaint.setStyle(Paint.Style.STROKE);
            boxPaint.setStrokeWidth(isFocused() ? 2f : 1f);
            boxPaint.setColor(isFocused() ? 0xFF4F5BD5 : 0xFFC5C6D0);
            canvas.drawRoundRect(1, 3, w - 1, h - 3, 10, 10, boxPaint);
        }
        super.draw(canvas);

        if (isFocused()) {
            Graphics2D g = canvas.getAwtGraphics();
            if (g != null) {
                Font font = typeface.toAwtFont(textSizePx);
                FontMetrics fm = g.getFontMetrics(font);
                String sub = text.subSequence(0, Math.min(cursorIndex, text.length())).toString();
                int cx = paddingLeft + fm.stringWidth(sub);
                int topY = (h - fm.getHeight()) / 2;
                boxPaint.setStyle(Paint.Style.FILL);
                boxPaint.setColor(0xFF4F5BD5);
                canvas.drawRect(cx, topY, cx + 2, topY + fm.getHeight(), boxPaint);
            }
        }
    }
}
