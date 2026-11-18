package com.google.android.gms.tasks;

import android.app.Activity;
import java.util.concurrent.Executor;

public interface OnCompleteListener<TResult> {
    void onComplete(Task<TResult> task);
}
