package android.widget;

import android.content.Context;
import android.util.AttributeSet;

public class ImageButton extends ImageView {
    public ImageButton(Context context) {
        this(context, null);
    }

    public ImageButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ImageButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setClickable(true);
    }
}
