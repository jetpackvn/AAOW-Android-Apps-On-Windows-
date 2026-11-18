package com.google.android.gms.appset;

import android.content.Context;
import com.google.android.gms.tasks.Tasks;
import dev.aaow.rt.microg.MicroGServiceRegistry;

public final class AppSet {
    private AppSet() {
    }

    public static AppSetIdClient getClient(Context context) {
        return () -> Tasks.forResult(new AppSetIdInfo(MicroGServiceRegistry.getAppSetId(), AppSetIdInfo.SCOPE_APP));
    }
}
