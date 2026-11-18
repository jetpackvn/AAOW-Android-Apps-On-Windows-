package android.app;

import android.content.ComponentCallbacks;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Bundle;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Application extends ContextWrapper implements ComponentCallbacks2 {
    public interface ActivityLifecycleCallbacks {
        void onActivityCreated(Activity activity, Bundle savedInstanceState);

        void onActivityStarted(Activity activity);

        void onActivityResumed(Activity activity);

        void onActivityPaused(Activity activity);

        void onActivityStopped(Activity activity);

        void onActivitySaveInstanceState(Activity activity, Bundle outState);

        void onActivityDestroyed(Activity activity);

        default void onActivityPreCreated(Activity activity, Bundle savedInstanceState) {
        }

        default void onActivityPostCreated(Activity activity, Bundle savedInstanceState) {
        }

        default void onActivityPreStarted(Activity activity) {
        }

        default void onActivityPostStarted(Activity activity) {
        }

        default void onActivityPreResumed(Activity activity) {
        }

        default void onActivityPostResumed(Activity activity) {
        }

        default void onActivityPrePaused(Activity activity) {
        }

        default void onActivityPostPaused(Activity activity) {
        }

        default void onActivityPreStopped(Activity activity) {
        }

        default void onActivityPostStopped(Activity activity) {
        }

        default void onActivityPreSaveInstanceState(Activity activity, Bundle outState) {
        }

        default void onActivityPostSaveInstanceState(Activity activity, Bundle outState) {
        }

        default void onActivityPreDestroyed(Activity activity) {
        }

        default void onActivityPostDestroyed(Activity activity) {
        }
    }

    public interface OnProvideAssistDataListener {
        void onProvideAssistData(Activity activity, Bundle data);
    }

    private final List<ActivityLifecycleCallbacks> lifecycleCallbacks = new CopyOnWriteArrayList<>();
    private final List<ComponentCallbacks> componentCallbacks = new CopyOnWriteArrayList<>();

    public Application() {
        super(null);
    }

    public void attach(Context context) {
        ActivityThread.setPackageName(context != null ? context.getPackageName() : null);
        attachBaseContext(context);
        ActivityThread.setCurrentApplication(this);
    }

    public void onCreate() {
    }

    public void onTerminate() {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        for (ComponentCallbacks cb : componentCallbacks) {
            cb.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onLowMemory() {
        for (ComponentCallbacks cb : componentCallbacks) {
            cb.onLowMemory();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        for (ComponentCallbacks cb : componentCallbacks) {
            if (cb instanceof ComponentCallbacks2 cb2) {
                cb2.onTrimMemory(level);
            }
        }
    }

    public void registerComponentCallbacks(ComponentCallbacks callback) {
        if (callback != null)
            componentCallbacks.add(callback);
    }

    public void unregisterComponentCallbacks(ComponentCallbacks callback) {
        componentCallbacks.remove(callback);
    }

    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        if (callback != null)
            lifecycleCallbacks.add(callback);
    }

    public void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        lifecycleCallbacks.remove(callback);
    }

    public static String getProcessName() {
        return ActivityThread.currentProcessName();
    }

    public void dispatchActivityCreated(Activity activity, Bundle savedInstanceState) {
        for (ActivityLifecycleCallbacks cb : lifecycleCallbacks) {
            try {
                cb.onActivityCreated(activity, savedInstanceState);
            } catch (Throwable ignored) {
            }
        }
    }

    public void dispatchActivityStarted(Activity activity) {
        for (ActivityLifecycleCallbacks cb : lifecycleCallbacks) {
            try {
                cb.onActivityStarted(activity);
            } catch (Throwable ignored) {
            }
        }
    }

    public void dispatchActivityResumed(Activity activity) {
        for (ActivityLifecycleCallbacks cb : lifecycleCallbacks) {
            try {
                cb.onActivityResumed(activity);
            } catch (Throwable ignored) {
            }
        }
    }

    public void dispatchActivityPaused(Activity activity) {
        for (ActivityLifecycleCallbacks cb : lifecycleCallbacks) {
            try {
                cb.onActivityPaused(activity);
            } catch (Throwable ignored) {
            }
        }
    }

    public void dispatchActivityStopped(Activity activity) {
        for (ActivityLifecycleCallbacks cb : lifecycleCallbacks) {
            try {
                cb.onActivityStopped(activity);
            } catch (Throwable ignored) {
            }
        }
    }

    public void dispatchActivityDestroyed(Activity activity) {
        for (ActivityLifecycleCallbacks cb : lifecycleCallbacks) {
            try {
                cb.onActivityDestroyed(activity);
            } catch (Throwable ignored) {
            }
        }
    }
}
