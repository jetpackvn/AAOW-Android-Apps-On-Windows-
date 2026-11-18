package android.view;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;

public class ContextThemeWrapper extends ContextWrapper {
    private int themeResource;
    private Resources.Theme theme;

    public ContextThemeWrapper() {
        super(null);
    }

    public ContextThemeWrapper(Context base, int themeResId) {
        super(base);
        this.themeResource = themeResId;
    }

    @Override
    public void setTheme(int resid) {
        themeResource = resid;
        if (theme != null) {
            theme.applyStyle(resid, true);
        }
    }

    public int getThemeResId() {
        return themeResource;
    }

    @Override
    public Resources.Theme getTheme() {
        if (theme == null) {
            theme = getResources().newTheme();
            if (themeResource != 0) {
                theme.applyStyle(themeResource, true);
            } else if (getBaseContext() != null && getBaseContext().getTheme() != null) {
                theme.setTo(getBaseContext().getTheme());
            }
        }
        return theme;
    }
}
