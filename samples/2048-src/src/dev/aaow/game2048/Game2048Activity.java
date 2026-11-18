package dev.aaow.game2048;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

/**
 * Game 2048 viết bằng Android SDK chuẩn, chạy trực tiếp trên Windows qua AAOW.
 * Hỗ trợ kéo chuột (swipe), phím mũi tên / WASD trên bàn phím PC, và nút điều khiển.
 */
public class Game2048Activity extends Activity {
    private final int[][] grid = new int[4][4];
    private final Random random = new Random();
    private int score = 0;
    private int bestScore = 0;
    private SharedPreferences prefs;
    private TextView tvScore;
    private TextView tvBest;
    private BoardView boardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("game2048_prefs", MODE_PRIVATE);
        bestScore = prefs.getInt("best_score", 512);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 14, 16, 14);
        root.setBackgroundColor(0xFFFAF8EF);

        // Hàng điểm số
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        tvScore = new TextView(this);
        tvScore.setTextSize(16f);
        tvScore.setTypeface(Typeface.DEFAULT_BOLD);
        tvScore.setTextColor(0xFF776E65);
        topRow.addView(tvScore, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        tvBest = new TextView(this);
        tvBest.setTextSize(15f);
        tvBest.setTypeface(Typeface.DEFAULT_BOLD);
        tvBest.setTextColor(0xFFD97706);
        tvBest.setGravity(Gravity.RIGHT);
        topRow.addView(tvBest, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(topRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView guide = new TextView(this);
        guide.setText("Dùng phím mũi tên / WASD, vuốt chuột trên bàn cờ hoặc bấm nút bên dưới:");
        guide.setTextSize(12f);
        guide.setTextColor(0xFF8F7A66);
        LinearLayout.LayoutParams guideLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        guideLp.setMargins(0, 4, 0, 10);
        root.addView(guide, guideLp);

        boardView = new BoardView(this);
        LinearLayout.LayoutParams boardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        boardLp.setMargins(0, 0, 0, 12);
        root.addView(boardView, boardLp);

        // Cụm nút điều hướng nhanh
        LinearLayout dpadRow = new LinearLayout(this);
        dpadRow.setOrientation(LinearLayout.HORIZONTAL);
        addMoveButton(dpadRow, "← Trái", () -> move(0));
        addMoveButton(dpadRow, "▲ Lên", () -> move(1));
        addMoveButton(dpadRow, "▼ Xuống", () -> move(2));
        addMoveButton(dpadRow, "Phải →", () -> move(3));
        LinearLayout.LayoutParams dpadLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dpadLp.setMargins(0, 0, 0, 8);
        root.addView(dpadRow, dpadLp);

        Button btnNew = new Button(this);
        btnNew.setText("Ván mới (New Game)");
        btnNew.setOnClickListener(v -> {
            startNewGame(false);
            Toast.makeText(this, "Đã bắt đầu ván 2048 mới!", Toast.LENGTH_SHORT).show();
        });
        root.addView(btnNew, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 44));

        setContentView(root);
        startNewGame(true);
    }

    private void addMoveButton(LinearLayout row, String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12f);
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 42, 1f);
        lp.setMargins(3, 0, 3, 0);
        row.addView(b, lp);
    }

    private void startNewGame(boolean seedShowcaseTiles) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                grid[r][c] = 0;
            }
        }
        if (seedShowcaseTiles) {
            grid[0][0] = 64;
            grid[0][1] = 32;
            grid[0][2] = 16;
            grid[1][0] = 8;
            grid[1][1] = 8;
            grid[2][2] = 4;
            grid[3][3] = 2;
            score = 280;
        } else {
            score = 0;
            spawnTile();
            spawnTile();
        }
        updateHeader();
        boardView.invalidate();
    }

    private void updateHeader() {
        if (score > bestScore) {
            bestScore = score;
            prefs.edit().putInt("best_score", bestScore).apply();
        }
        tvScore.setText("Điểm: " + score);
        tvBest.setText("Kỷ lục: " + bestScore);
    }

    private void spawnTile() {
        int emptyCount = 0;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (grid[r][c] == 0) emptyCount++;
            }
        }
        if (emptyCount == 0) return;
        int pick = random.nextInt(emptyCount);
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (grid[r][c] == 0) {
                    if (pick-- == 0) {
                        grid[r][c] = random.nextInt(10) == 0 ? 4 : 2;
                        return;
                    }
                }
            }
        }
    }

    /** dir: 0=Left, 1=Up, 2=Down, 3=Right */
    void move(int dir) {
        boolean moved = false;
        for (int i = 0; i < 4; i++) {
            int[] line = new int[4];
            for (int j = 0; j < 4; j++) {
                line[j] = switch (dir) {
                    case 0 -> grid[i][j];
                    case 3 -> grid[i][3 - j];
                    case 1 -> grid[j][i];
                    default -> grid[3 - j][i];
                };
            }
            int[] merged = compressAndMerge(line);
            for (int j = 0; j < 4; j++) {
                int old = switch (dir) {
                    case 0 -> grid[i][j];
                    case 3 -> grid[i][3 - j];
                    case 1 -> grid[j][i];
                    default -> grid[3 - j][i];
                };
                if (old != merged[j]) moved = true;
                switch (dir) {
                    case 0 -> grid[i][j] = merged[j];
                    case 3 -> grid[i][3 - j] = merged[j];
                    case 1 -> grid[j][i] = merged[j];
                    default -> grid[3 - j][i] = merged[j];
                }
            }
        }
        if (moved) {
            spawnTile();
            updateHeader();
            boardView.invalidate();
        }
    }

    private int[] compressAndMerge(int[] line) {
        int[] compact = new int[4];
        int p = 0;
        for (int v : line) if (v != 0) compact[p++] = v;

        int[] res = new int[4];
        int out = 0;
        for (int i = 0; i < p; i++) {
            if (i + 1 < p && compact[i] == compact[i + 1]) {
                int m = compact[i] * 2;
                res[out++] = m;
                score += m;
                i++;
            } else {
                res[out++] = compact[i];
            }
        }
        return res;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT -> {
                move(0);
                return true;
            }
            case KeyEvent.KEYCODE_DPAD_UP -> {
                move(1);
                return true;
            }
            case KeyEvent.KEYCODE_DPAD_DOWN -> {
                move(2);
                return true;
            }
            case KeyEvent.KEYCODE_DPAD_RIGHT -> {
                move(3);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    final class BoardView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float downX;
        private float downY;

        BoardView(Context context) {
            super(context);
            setClickable(true);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN -> {
                    downX = event.getX();
                    downY = event.getY();
                    return true;
                }
                case MotionEvent.ACTION_UP -> {
                    float dx = event.getX() - downX;
                    float dy = event.getY() - downY;
                    if (Math.abs(dx) > 20 || Math.abs(dy) > 20) {
                        if (Math.abs(dx) > Math.abs(dy)) {
                            move(dx > 0 ? 3 : 0);
                        } else {
                            move(dy > 0 ? 2 : 1);
                        }
                    }
                    return true;
                }
            }
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            int boardSize = Math.min(w, h);
            int startX = (w - boardSize) / 2;
            int startY = (h - boardSize) / 2;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFBBADA0);
            canvas.drawRoundRect(startX, startY, startX + boardSize, startY + boardSize, 16, 16, paint);

            int gap = 10;
            int cellSize = (boardSize - gap * 5) / 4;

            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    int val = grid[r][c];
                    int cx = startX + gap + c * (cellSize + gap);
                    int cy = startY + gap + r * (cellSize + gap);

                    paint.setColor(tileBgColor(val));
                    canvas.drawRoundRect(cx, cy, cx + cellSize, cy + cellSize, 10, 10, paint);

                    if (val > 0) {
                        paint.setColor(val <= 4 ? 0xFF776E65 : Color.WHITE);
                        paint.setTypeface(Typeface.DEFAULT_BOLD);
                        paint.setTextSize(val < 100 ? 26f : (val < 1000 ? 22f : 18f));
                        paint.setTextAlign(Paint.Align.CENTER);
                        canvas.drawText(String.valueOf(val), cx + cellSize / 2f, cy + cellSize / 2f + 9f, paint);
                    }
                }
            }
        }

        private int tileBgColor(int v) {
            return switch (v) {
                case 0 -> 0xFFCDC1B4;
                case 2 -> 0xFFEEE4DA;
                case 4 -> 0xFFEDE0C8;
                case 8 -> 0xFFF2B179;
                case 16 -> 0xFFF59563;
                case 32 -> 0xFFF67C5F;
                case 64 -> 0xFFF65E3B;
                case 128 -> 0xFFEDCF72;
                case 256 -> 0xFFEDCC61;
                case 512 -> 0xFFEDC850;
                case 1024 -> 0xFFEDC53F;
                default -> 0xFFEDC22E;
            };
        }
    }
}
