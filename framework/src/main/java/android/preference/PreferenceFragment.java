package android.preference;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public abstract class PreferenceFragment extends Fragment {
    private PreferenceManager preferenceManager;
    private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        listView = new ListView(getActivity());
        listView.setId(0x0102000a); // android.R.id.list
        bindPreferences();
        return listView;
    }

    public PreferenceManager getPreferenceManager() {
        if (preferenceManager == null) {
            preferenceManager = new PreferenceManager(getActivity());
        }
        return preferenceManager;
    }

    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        if (getPreferenceManager().setPreferences(preferenceScreen) && preferenceScreen != null) {
            bindPreferences();
        }
    }

    public PreferenceScreen getPreferenceScreen() {
        return getPreferenceManager().getPreferenceScreen();
    }

    public void addPreferencesFromIntent(Intent intent) {
    }

    public void addPreferencesFromResource(int preferencesResId) {
        PreferenceScreen screen = getPreferenceManager().inflateFromResource(
                getActivity(), preferencesResId, getPreferenceScreen());
        setPreferenceScreen(screen);
    }

    public Preference findPreference(CharSequence key) {
        PreferenceScreen screen = getPreferenceScreen();
        return screen != null ? screen.findPreference(key) : null;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return false;
    }

    private void bindPreferences() {
        PreferenceScreen screen = getPreferenceScreen();
        if (screen != null && listView != null) {
            screen.bind(listView);
        }
    }
}
