package android.preference;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;

public abstract class DialogPreference extends Preference
        implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener,
        PreferenceManager.OnActivityDestroyListener {
    private CharSequence dialogTitle;
    private CharSequence dialogMessage;
    private Drawable dialogIcon;
    private CharSequence positiveButtonText = "OK";
    private CharSequence negativeButtonText = "Cancel";
    private int dialogLayoutResId;
    private Dialog dialog;
    private int whichButtonClicked;

    public DialogPreference(Context context) {
        this(context, null, 0, 0);
    }

    public DialogPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public DialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.dialogTitle = getTitle();
    }

    public void setDialogTitle(CharSequence dialogTitle) {
        this.dialogTitle = dialogTitle;
    }

    public void setDialogTitle(int dialogTitleResId) {
        if (getContext() != null) setDialogTitle(getContext().getString(dialogTitleResId));
    }

    public CharSequence getDialogTitle() {
        return dialogTitle;
    }

    public void setDialogMessage(CharSequence dialogMessage) {
        this.dialogMessage = dialogMessage;
    }

    public void setDialogMessage(int dialogMessageResId) {
        if (getContext() != null) setDialogMessage(getContext().getString(dialogMessageResId));
    }

    public CharSequence getDialogMessage() {
        return dialogMessage;
    }

    public void setDialogIcon(Drawable dialogIcon) {
        this.dialogIcon = dialogIcon;
    }

    public void setDialogIcon(int dialogIconRes) {
        if (getContext() != null) setDialogIcon(getContext().getDrawable(dialogIconRes));
    }

    public Drawable getDialogIcon() {
        return dialogIcon;
    }

    public void setPositiveButtonText(CharSequence positiveButtonText) {
        this.positiveButtonText = positiveButtonText;
    }

    public void setPositiveButtonText(int positiveButtonTextResId) {
        if (getContext() != null) setPositiveButtonText(getContext().getString(positiveButtonTextResId));
    }

    public CharSequence getPositiveButtonText() {
        return positiveButtonText;
    }

    public void setNegativeButtonText(CharSequence negativeButtonText) {
        this.negativeButtonText = negativeButtonText;
    }

    public void setNegativeButtonText(int negativeButtonTextResId) {
        if (getContext() != null) setNegativeButtonText(getContext().getString(negativeButtonTextResId));
    }

    public CharSequence getNegativeButtonText() {
        return negativeButtonText;
    }

    public void setDialogLayoutResource(int dialogLayoutResId) {
        this.dialogLayoutResId = dialogLayoutResId;
    }

    public int getDialogLayoutResource() {
        return dialogLayoutResId;
    }

    public Dialog getDialog() {
        return dialog;
    }

    @Override
    protected void onClick() {
        if (dialog != null && dialog.isShowing()) return;
        showDialog(null);
    }

    protected void showDialog(Bundle state) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(dialogTitle)
                .setIcon(dialogIcon)
                .setPositiveButton(positiveButtonText, this)
                .setNegativeButton(negativeButtonText, this);
        View contentView = onCreateDialogView();
        if (contentView != null) {
            onBindDialogView(contentView);
            builder.setView(contentView);
        } else {
            builder.setMessage(dialogMessage);
        }
        onPrepareDialogBuilder(builder);
        whichButtonClicked = DialogInterface.BUTTON_NEGATIVE;
        AlertDialog d = builder.create();
        d.setOnDismissListener(this);
        this.dialog = d;
        d.show();
    }

    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
    }

    protected View onCreateDialogView() {
        return null;
    }

    protected void onBindDialogView(View view) {
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        this.whichButtonClicked = which;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        this.dialog = null;
        onDialogClosed(whichButtonClicked == DialogInterface.BUTTON_POSITIVE);
    }

    protected void onDialogClosed(boolean positiveResult) {
    }

    @Override
    public void onActivityDestroy() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
