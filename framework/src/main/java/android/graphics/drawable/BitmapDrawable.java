package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class BitmapDrawable extends Drawable {
    private final Bitmap bitmap;
    private Paint drawPaint;

    public BitmapDrawable(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public BitmapDrawable(Resources res, Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public final Bitmap getBitmap() {
        return bitmap;
    }

    @Override
    public int getIntrinsicWidth() {
        return bitmap != null ? bitmap.getWidth() : -1;
    }

    @Override
    public int getIntrinsicHeight() {
        return bitmap != null ? bitmap.getHeight() : -1;
    }

    @Override
    public void draw(Canvas canvas) {
        if (bitmap != null && !bitmap.isRecycled()) {
            Paint p = null;
            int a = getAlpha();
            if (a < 255) {
                if (drawPaint == null) drawPaint = new Paint();
                drawPaint.setAlpha(a);
                p = drawPaint;
            }
            canvas.drawBitmap(bitmap, null, getBounds(), p);
        }
    }

    @Override
    public ConstantState getConstantState() {
        return new ConstantState() {
            @Override
            public Drawable newDrawable() {
                return new BitmapDrawable(bitmap);
            }

            @Override
            public Drawable newDrawable(Resources res) {
                return new BitmapDrawable(res, bitmap);
            }
        };
    }
}
