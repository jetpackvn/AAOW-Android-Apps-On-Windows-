package android.view;

public interface WindowManager extends ViewManager {
    class LayoutParams extends ViewGroup.LayoutParams {
        public static final int FLAG_KEEP_SCREEN_ON = 0x00000080;
        public static final int FLAG_FULLSCREEN = 0x00000400;

        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }
    }
}
