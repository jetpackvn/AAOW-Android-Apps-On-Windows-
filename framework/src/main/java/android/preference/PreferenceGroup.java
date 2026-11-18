package android.preference;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

public abstract class PreferenceGroup extends Preference {
    private final List<Preference> preferenceList = new ArrayList<>();
    private boolean orderingAsAdded = true;

    public PreferenceGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public PreferenceGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PreferenceGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setOrderingAsAdded(boolean orderingAsAdded) {
        this.orderingAsAdded = orderingAsAdded;
    }

    public boolean isOrderingAsAdded() {
        return orderingAsAdded;
    }

    public void addItemFromInflater(Preference preference) {
        addPreference(preference);
    }

    public int getPreferenceCount() {
        return preferenceList.size();
    }

    public Preference getPreference(int index) {
        return preferenceList.get(index);
    }

    public boolean addPreference(Preference preference) {
        if (preference == null || preferenceList.contains(preference)) return false;
        preference.setParent(this);
        if (getPreferenceManager() != null) {
            preference.onAttachedToHierarchy(getPreferenceManager());
        }
        preferenceList.add(preference);
        notifyHierarchyChanged();
        return true;
    }

    public boolean removePreference(Preference preference) {
        boolean removed = preferenceList.remove(preference);
        if (removed) {
            preference.setParent(null);
            notifyHierarchyChanged();
        }
        return removed;
    }

    public void removeAll() {
        for (Preference p : preferenceList) {
            p.setParent(null);
        }
        preferenceList.clear();
        notifyHierarchyChanged();
    }

    public Preference findPreference(CharSequence key) {
        if (TextUtils.equals(getKey(), key)) return this;
        for (Preference p : preferenceList) {
            if (TextUtils.equals(p.getKey(), key)) return p;
            if (p instanceof PreferenceGroup pg) {
                Preference nested = pg.findPreference(key);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    protected boolean isOnSameScreenAsChildren() {
        return true;
    }
}
