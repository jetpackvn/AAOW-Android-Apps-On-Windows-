package android.app;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;

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
    private CharSequence[] items;
    private OnClickListener itemClickListener;
    private ListAdapter listAdapter;
    private int checkedItem = -1;
    private Button positiveButton;
    private Button negativeButton;
    private Button neutralButton;
    private android.widget.LinearLayout titleTemplateContainer;

    public AlertDialog(Context context) {
        super(context);
    }

    public AlertDialog(Context context, int themeResId) {
        super(context, themeResId);
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

    public CharSequence[] getItems() {
        return items;
    }

    public ListAdapter getListAdapter() {
        return listAdapter;
    }

    public OnClickListener getItemClickListener() {
        return itemClickListener;
    }

    public int getCheckedItem() {
        return checkedItem;
    }

    public void setMessage(CharSequence message) {
        this.message = message != null ? message : "";
    }

    public void setView(View view) {
        this.customView = view;
    }

    public void setIcon(Drawable icon) {
    }

    public void setIcon(int resId) {
    }

    public void setButton(int whichButton, CharSequence text, OnClickListener listener) {
        switch (whichButton) {
            case BUTTON_POSITIVE -> {
                positiveText = text;
                positiveListener = listener;
                if (positiveButton != null && text != null) positiveButton.setText(text);
            }
            case BUTTON_NEGATIVE -> {
                negativeText = text;
                negativeListener = listener;
                if (negativeButton != null && text != null) negativeButton.setText(text);
            }
            case BUTTON_NEUTRAL -> {
                neutralText = text;
                neutralListener = listener;
                if (neutralButton != null && text != null) neutralButton.setText(text);
            }
        }
    }

    public Button getButton(int whichButton) {
        return switch (whichButton) {
            case BUTTON_POSITIVE -> {
                if (positiveButton == null) {
                    positiveButton = createDialogButton(whichButton, positiveText);
                }
                yield positiveButton;
            }
            case BUTTON_NEGATIVE -> {
                if (negativeButton == null) {
                    negativeButton = createDialogButton(whichButton, negativeText);
                }
                yield negativeButton;
            }
            case BUTTON_NEUTRAL -> {
                if (neutralButton == null) {
                    neutralButton = createDialogButton(whichButton, neutralText);
                }
                yield neutralButton;
            }
            default -> null;
        };
    }

    private Button createDialogButton(int whichButton, CharSequence label) {
        Button b = new Button(context);
        if (label != null) b.setText(label);
        b.setOnClickListener(v -> {
            OnClickListener l = switch (whichButton) {
                case BUTTON_POSITIVE -> positiveListener;
                case BUTTON_NEGATIVE -> negativeListener;
                case BUTTON_NEUTRAL -> neutralListener;
                default -> null;
            };
            if (l != null) l.onClick(this, whichButton);
            dismiss();
        });
        return b;
    }

    @Override
    public <T extends View> T findViewById(int id) {
        T found = super.findViewById(id);
        if (found != null) return found;
        if (id == 0x01020901) { // android.R.id.title_template
            if (titleTemplateContainer == null) {
                titleTemplateContainer = new android.widget.LinearLayout(context);
                titleTemplateContainer.setId(0x01020901);
            }
            @SuppressWarnings("unchecked")
            T cast = (T) titleTemplateContainer;
            return cast;
        }
        return null;
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
        private CharSequence[] items;
        private OnClickListener itemClickListener;
        private ListAdapter listAdapter;
        private int checkedItem = -1;

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

        public Builder setIcon(int iconId) {
            return this;
        }

        public Builder setIcon(Drawable icon) {
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

        public Builder setItems(int itemsId, OnClickListener listener) {
            return setItems(context.getResources().getStringArray(itemsId), listener);
        }

        public Builder setItems(CharSequence[] items, OnClickListener listener) {
            this.items = items;
            this.itemClickListener = listener;
            return this;
        }

        public Builder setAdapter(ListAdapter adapter, OnClickListener listener) {
            this.listAdapter = adapter;
            this.itemClickListener = listener;
            return this;
        }

        public Builder setSingleChoiceItems(CharSequence[] items, int checkedItem, OnClickListener listener) {
            this.items = items;
            this.checkedItem = checkedItem;
            this.itemClickListener = listener;
            return this;
        }

        public Builder setSingleChoiceItems(int itemsId, int checkedItem, OnClickListener listener) {
            return setSingleChoiceItems(context.getResources().getStringArray(itemsId), checkedItem, listener);
        }

        public Builder setSingleChoiceItems(ListAdapter adapter, int checkedItem, OnClickListener listener) {
            this.listAdapter = adapter;
            this.checkedItem = checkedItem;
            this.itemClickListener = listener;
            return this;
        }

        public Builder setPositiveButton(int textId, OnClickListener listener) {
            this.posText = textId != 0 ? context.getText(textId) : null;
            this.posListener = listener;
            return this;
        }

        public Builder setPositiveButton(CharSequence text, OnClickListener listener) {
            this.posText = text;
            this.posListener = listener;
            return this;
        }

        public Builder setNegativeButton(int textId, OnClickListener listener) {
            this.negText = textId != 0 ? context.getText(textId) : null;
            this.negListener = listener;
            return this;
        }

        public Builder setNegativeButton(CharSequence text, OnClickListener listener) {
            this.negText = text;
            this.negListener = listener;
            return this;
        }

        public Builder setNeutralButton(int textId, OnClickListener listener) {
            this.neuText = textId != 0 ? context.getText(textId) : null;
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

        public Builder setView(int layoutResId) {
            this.customView = android.view.LayoutInflater.from(context).inflate(layoutResId, null, false);
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
            d.items = items;
            d.listAdapter = listAdapter;
            d.itemClickListener = itemClickListener;
            d.checkedItem = checkedItem;
            return d;
        }

        public AlertDialog show() {
            AlertDialog d = create();
            d.show();
            return d;
        }
    }
}
