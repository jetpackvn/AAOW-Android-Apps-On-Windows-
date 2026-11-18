package dev.aaow.showcase;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity thứ hai kiểm chứng khả năng điều hướng Intent, vẽ đồ họa 2D trực tiếp
 * qua android.graphics.Canvas và tương tác kéo chuột vẽ tự do.
 */
public class CanvasLabActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int power = getIntent().getIntExtra("power_level", 75);
        String note = getIntent().getStringExtra("note");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 16);
        root.setBackgroundColor(0xFFF4F6FB);

        TextView header = new TextView(this);
        header.setText("Phòng thí nghiệm android.graphics.Canvas (Mức: " + power + "%)");
        header.setTextSize(16f);
        header.setTextColor(0xFF283593);
        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView sub = new TextView(this);
        sub.setText("Dùng chuột vẽ trực tiếp lên vùng Canvas bên dưới • Ghi chú: " + (note != null ? note : ""));
        sub.setTextSize(12f);
        sub.setTextColor(0xFF5A607F);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.setMargins(0, 4, 0, 12);
        root.addView(sub, subLp);

        InteractiveCanvasView canvasView = new InteractiveCanvasView(this, power);
        LinearLayout.LayoutParams cvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        cvLp.setMargins(0, 0, 0, 12);
        root.addView(canvasView, cvLp);

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnClear = new Button(this);
        btnClear.setText("Xóa nét vẽ");
        btnClear.setOnClickListener(v -> {
            canvasView.clearStrokes();
            Toast.makeText(this, "Đã làm mới vùng vẽ Canvas", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(0, 44, 1f);
        b1.setMargins(0, 0, 6, 0);
        bottomRow.addView(btnClear, b1);

        Button btnBack = new Button(this);
        btnBack.setText("← Quay lại màn hình chính");
        btnBack.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(0, 44, 1f);
        b2.setMargins(6, 0, 0, 0);
        bottomRow.addView(btnBack, b2);

        root.addView(bottomRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    static final class InteractiveCanvasView extends View {
        private final int powerLevel;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final List<Path> paths = new ArrayList<>();
        private final List<Integer> pathColors = new ArrayList<>();
        private Path currentPath;
        private int paletteIndex = 0;
        private static final int[] PALETTE = {
                0xFF3949AB, 0xFF00897B, 0xFFE53935, 0xFF8E24AA, 0xFFF4511E
        };

        InteractiveCanvasView(Context context, int powerLevel) {
            super(context);
            this.powerLevel = powerLevel;
            setClickable(true);
        }

        void clearStrokes() {
            paths.clear();
            pathColors.clear();
            currentPath = null;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();

            // Nền thẻ trắng bo góc
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawRoundRect(0, 0, w, h, 16, 16, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f);
            paint.setColor(0xFFDCE1F0);
            canvas.drawRoundRect(1, 1, w - 1, h - 1, 16, 16, paint);

            // Biểu đồ đồng hồ đo hiệu năng (Arc + Circle)
            float cx = w / 2f;
            float cy = 115f;
            float r = 68f;
            RectF arcRect = new RectF(cx - r, cy - r, cx + r, cy + r);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(14f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(0xFFE8EAF6);
            canvas.drawArc(arcRect, 135, 270, false, paint);

            paint.setColor(0xFF4F5BD5);
            canvas.drawArc(arcRect, 135, 270f * powerLevel / 100f, false, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF1A1C29);
            paint.setTextSize(22f);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(powerLevel + "%", cx, cy + 8, paint);

            paint.setTextSize(12f);
            paint.setColor(0xFF5A607F);
            canvas.drawText("ANDROID CANVAS → JAVA2D DIRECT", cx, cy + 84, paint);

            // Vẽ sóng minh họa bằng Path2D
            Path wave = new Path();
            float waveTop = 260f;
            float leftPad = 28f;
            float usableW = Math.max(10f, w - 56f);
            wave.moveTo(leftPad, waveTop);
            for (int i = 0; i <= 60; i++) {
                float t = i / 60f;
                float px = leftPad + t * usableW;
                float py = waveTop + (float) Math.sin(t * Math.PI * 4) * (12f + powerLevel * 0.22f);
                wave.lineTo(px, py);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            paint.setColor(0xFF00897B);
            canvas.drawPath(wave, paint);

            // Vẽ các nét vẽ tay của người dùng
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            for (int i = 0; i < paths.size(); i++) {
                paint.setColor(pathColors.get(i));
                canvas.drawPath(paths.get(i), paint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN -> {
                    currentPath = new Path();
                    currentPath.moveTo(x, y);
                    paths.add(currentPath);
                    pathColors.add(PALETTE[paletteIndex % PALETTE.length]);
                    paletteIndex++;
                    invalidate();
                    return true;
                }
                case MotionEvent.ACTION_MOVE -> {
                    if (currentPath != null) {
                        currentPath.lineTo(x, y);
                        invalidate();
                    }
                    return true;
                }
                case MotionEvent.ACTION_UP -> {
                    currentPath = null;
                    invalidate();
                    return true;
                }
            }
            return true;
        }
    }
}
