package android.app;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

import java.util.function.Consumer;

/**
 * Hộp thoại AlertDialog của Android hiển thị trực tiếp dưới dạng lớp phủ Material trên cửa sổ Windows.
 */
public class AlertDialog extends Dialog {
    private static Consumer<AlertDialog> hostDialogBridge;

    private CharSequence message = "";
    private CharSequence positiveText;
    private OnClickListener positiveListener;
    private CharSequence negativeText;
    private OnClickListener negativeListener;
    private CharSequence neutralText;
    private OnClickListener neutralListener;
    private View customView;

    public AlertDialog(Context context) {
        super(context);
    }

    public static void setHostDialogBridge(Consumer<AlertDialog> bridge) {
        hostDialogBridge = bridge;
    }

    public CharSequence getTitleText() {
        return title;
    }

    public CharSequence getMessageText() {
        return message;
    }

    public CharSequence getPositiveText() {
        return positiveText;
    }

    public OnClickListener getPositiveListener() {
        return positiveListener;
    }

    public CharSequence getNegativeText() {
        return negativeText;
    }

    public OnClickListener getNegativeListener() {
        return negativeListener;
    }

    public CharSequence getNeutralText() {
        return neutralText;
    }

    public OnClickListener getNeutralListener() {
        return neutralListener;
    }

    public View getCustomView() {
        return customView;
    }

    public void setMessage(CharSequence message) {
        this.message = message != null ? message : "";
    }

    public void setView(View view) {
        this.customView = view;
    }

    public void setButton(int whichButton, CharSequence text, OnClickListener listener) {
        switch (whichButton) {
            case BUTTON_POSITIVE -> {
                positiveText = text;
                positiveListener = listener;
            }
            case BUTTON_NEGATIVE -> {
                negativeText = text;
                negativeListener = listener;
            }
            case BUTTON_NEUTRAL -> {
                neutralText = text;
                neutralListener = listener;
            }
        }
    }

    @Override
    public void show() {
        super.show();
        if (hostDialogBridge != null) {
            hostDialogBridge.accept(this);
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (hostDialogBridge != null) {
            hostDialogBridge.accept(null);
        }
    }

    public static class Builder {
        private final Context context;
        private CharSequence title;
        private CharSequence message;
        private CharSequence posText;
        private OnClickListener posListener;
        private CharSequence negText;
        private OnClickListener negListener;
        private CharSequence neuText;
        private OnClickListener neuListener;
        private boolean cancelable = true;
        private View customView;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder(Context context, int themeResId) {
            this.context = context;
        }

        public Context getContext() {
            return context;
        }

        public Builder setTitle(int titleId) {
            this.title = context.getText(titleId);
            return this;
        }

        public Builder setTitle(CharSequence title) {
            this.title = title;
            return this;
        }

        public Builder setMessage(int messageId) {
            this.message = context.getText(messageId);
            return this;
        }

        public Builder setMessage(CharSequence message) {
            this.message = message;
            return this;
        }

        public Builder setPositiveButton(int textId, OnClickListener listener) {
            this.posText = context.getText(textId);
            this.posListener = listener;
            return this;
        }

        public Builder setPositiveButton(CharSequence text, OnClickListener listener) {
            this.posText = text;
            this.posListener = listener;
            return this;
        }

        public Builder setNegativeButton(int textId, OnClickListener listener) {
            this.negText = context.getText(textId);
            this.negListener = listener;
            return this;
        }

        public Builder setNegativeButton(CharSequence text, OnClickListener listener) {
            this.negText = text;
            this.negListener = listener;
            return this;
        }

        public Builder setNeutralButton(int textId, OnClickListener listener) {
            this.neuText = context.getText(textId);
            this.neuListener = listener;
            return this;
        }

        public Builder setNeutralButton(CharSequence text, OnClickListener listener) {
            this.neuText = text;
            this.neuListener = listener;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        public Builder setView(View view) {
            this.customView = view;
            return this;
        }

        public AlertDialog create() {
            AlertDialog d = new AlertDialog(context);
            if (title != null) d.setTitle(title);
            if (message != null) d.setMessage(message);
            d.setButton(BUTTON_POSITIVE, posText, posListener);
            d.setButton(BUTTON_NEGATIVE, negText, negListener);
            d.setButton(BUTTON_NEUTRAL, neuText, neuListener);
            d.setCancelable(cancelable);
            d.setView(customView);
            return d;
        }

        public AlertDialog show() {
            AlertDialog d = create();
            d.show();
            return d;
        }
    }
}
