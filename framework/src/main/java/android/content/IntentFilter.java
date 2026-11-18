package android.content;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IntentFilter implements Parcelable {
    public static final int SYSTEM_HIGH_PRIORITY = 1000;
    public static final int SYSTEM_LOW_PRIORITY = -1000;

    private int priority;
    private final List<String> actions = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private final List<String> dataSchemes = new ArrayList<>();
    private final List<String> dataTypes = new ArrayList<>();

    public IntentFilter() {
    }

    public IntentFilter(String action) {
        addAction(action);
    }

    public IntentFilter(String action, String dataType) {
        addAction(action);
        addDataType(dataType);
    }

    public IntentFilter(IntentFilter o) {
        if (o != null) {
            this.priority = o.priority;
            this.actions.addAll(o.actions);
            this.categories.addAll(o.categories);
            this.dataSchemes.addAll(o.dataSchemes);
            this.dataTypes.addAll(o.dataTypes);
        }
    }

    public final void setPriority(int priority) {
        this.priority = priority;
    }

    public final int getPriority() {
        return priority;
    }

    public final void addAction(String action) {
        if (action != null && !actions.contains(action)) actions.add(action);
    }

    public final int countActions() {
        return actions.size();
    }

    public final String getAction(int index) {
        return actions.get(index);
    }

    public final boolean hasAction(String action) {
        return action != null && actions.contains(action);
    }

    public final Iterator<String> actionsIterator() {
        return actions.iterator();
    }

    public final void addCategory(String category) {
        if (category != null && !categories.contains(category)) categories.add(category);
    }

    public final boolean hasCategory(String category) {
        return category != null && categories.contains(category);
    }

    public final void addDataScheme(String scheme) {
        if (scheme != null && !dataSchemes.contains(scheme)) dataSchemes.add(scheme);
    }

    public final boolean hasDataScheme(String scheme) {
        return scheme != null && dataSchemes.contains(scheme);
    }

    public final void addDataType(String type) {
        if (type != null && !dataTypes.contains(type)) dataTypes.add(type);
    }

    public final boolean matchAction(String action) {
        return hasAction(action);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(priority);
        dest.writeStringList(actions);
        dest.writeStringList(categories);
    }

    public static final Creator<IntentFilter> CREATOR = new Creator<>() {
        @Override
        public IntentFilter createFromParcel(Parcel source) {
            IntentFilter f = new IntentFilter();
            f.setPriority(source.readInt());
            f.actions.addAll(source.createStringArrayList());
            f.categories.addAll(source.createStringArrayList());
            return f;
        }

        @Override
        public IntentFilter[] newArray(int size) {
            return new IntentFilter[size];
        }
    };
}
