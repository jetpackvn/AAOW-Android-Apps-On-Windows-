package android.view;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;

public class ContextThemeWrapper extends ContextWrapper {
    private int themeResource;
    private Resources.Theme theme;
    private Configuration overrideConfiguration;

    public ContextThemeWrapper() {
        super(null);
    }

    public ContextThemeWrapper(Context base, int themeResId) {
        super(base);
        this.themeResource = themeResId;
    }

    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
        if (overrideConfiguration != null) {
            this.overrideConfiguration = new Configuration(overrideConfiguration);
        }
    }

    public Configuration getOverrideConfiguration() {
        return overrideConfiguration;
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
            if (getBaseContext() != null && getBaseContext().getTheme() != null) {
                theme.setTo(getBaseContext().getTheme());
            }
            if (themeResource != 0) {
                theme.applyStyle(themeResource, true);
            }
        }
        return theme;
    }
}
