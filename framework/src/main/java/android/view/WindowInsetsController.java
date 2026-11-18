package android.view;

public interface WindowInsetsController {
    int APPEARANCE_LIGHT_STATUS_BARS = 1 << 3;
    int APPEARANCE_LIGHT_NAVIGATION_BARS = 1 << 4;
    int APPEARANCE_LIGHT_CAPTION_BARS = 1 << 8;
    int APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND = 1 << 7;

    int BEHAVIOR_DEFAULT = 1;
    int BEHAVIOR_SHOW_BARS_BY_TOUCH = 0;
    int BEHAVIOR_SHOW_BARS_BY_SWIPE = 1;
    int BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE = 2;

    void show(int types);

    void hide(int types);

    void setSystemBarsAppearance(int appearance, int mask);

    int getSystemBarsAppearance();

    void setSystemBarsBehavior(int behavior);

    int getSystemBarsBehavior();
}
