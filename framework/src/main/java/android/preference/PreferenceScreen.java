package android.preference;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public final class PreferenceScreen extends PreferenceGroup
        implements AdapterView.OnItemClickListener, DialogInterface.OnDismissListener {
    private ListAdapter rootAdapter;
    private Dialog dialog;

    public PreferenceScreen(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ListAdapter getRootAdapter() {
        if (rootAdapter == null) {
            rootAdapter = onCreateRootAdapter();
        }
        return rootAdapter;
    }

    protected ListAdapter onCreateRootAdapter() {
        List<Preference> flat = new ArrayList<>();
        flatten(this, flat);
        return new BaseAdapter() {
            @Override
            public int getCount() {
                return flat.size();
            }

            @Override
            public Object getItem(int position) {
                return flat.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return flat.get(position).getView(convertView, parent);
            }
        };
    }

    private static void flatten(PreferenceGroup group, List<Preference> out) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference p = group.getPreference(i);
            out.add(p);
            if (p instanceof PreferenceGroup pg && pg.isOnSameScreenAsChildren()) {
                flatten(pg, out);
            }
        }
    }

    public void bind(ListView listView) {
        if (listView == null) return;
        listView.setOnItemClickListener(this);
        listView.setAdapter(getRootAdapter());
    }

    public Dialog getDialog() {
        return dialog;
    }

    @Override
    protected void onClick() {
        if (getIntent() != null || getFragment() != null || getPreferenceCount() == 0) {
            return;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object item = getRootAdapter().getItem(position);
        if (item instanceof Preference p) {
            p.performClick(this);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        this.dialog = null;
    }

    @Override
    protected boolean isOnSameScreenAsChildren() {
        return false;
    }
}
