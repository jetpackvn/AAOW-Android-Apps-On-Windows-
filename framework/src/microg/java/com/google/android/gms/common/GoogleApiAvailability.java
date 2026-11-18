package com.google.android.gms.common;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

/** Cài đặt tương thích GoogleApiAvailability trên AAOW (microG-lite). */
public class GoogleApiAvailability extends GoogleApiAvailabilityLight {
    public static final int GOOGLE_PLAY_SERVICES_VERSION_CODE = GoogleApiAvailabilityLight.GOOGLE_PLAY_SERVICES_VERSION_CODE;
    private static final GoogleApiAvailability INSTANCE = new GoogleApiAvailability();

    public static GoogleApiAvailability getInstance() {
        return INSTANCE;
    }

    public Task<Void> makeGooglePlayServicesAvailable(Activity activity) {
        return Tasks.forResult(null);
    }

    public Dialog getErrorDialog(Activity activity, int errorCode, int requestCode) {
        return getErrorDialog(activity, errorCode, requestCode, null);
    }

    public Dialog getErrorDialog(Activity activity, int errorCode, int requestCode, DialogInterface.OnCancelListener cancelListener) {
        if (errorCode == ConnectionResult.SUCCESS || activity == null) return null;
        AlertDialog d = new AlertDialog.Builder(activity)
                .setTitle("AAOW microG-lite")
                .setMessage("Trạng thái dịch vụ Google Play: " + getErrorString(errorCode))
                .setPositiveButton("OK", null)
                .create();
        if (cancelListener != null) d.setOnCancelListener(cancelListener);
        return d;
    }

    public boolean showErrorDialogFragment(Activity activity, int errorCode, int requestCode) {
        return showErrorDialogFragment(activity, errorCode, requestCode, null);
    }

    public boolean showErrorDialogFragment(Activity activity, int errorCode, int requestCode, DialogInterface.OnCancelListener cancelListener) {
        Dialog d = getErrorDialog(activity, errorCode, requestCode, cancelListener);
        if (d == null) return false;
        d.show();
        return true;
    }

    public void showErrorNotification(Context context, int errorCode) {
    }
}
