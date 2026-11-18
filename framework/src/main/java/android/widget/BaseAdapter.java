package android.widget;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.view.View;
import android.view.ViewGroup;

public abstract class BaseAdapter implements ListAdapter, SpinnerAdapter {
    private final List<Runnable> observers = new CopyOnWriteArrayList<>();

    public void registerObserver(Runnable r) {
        if (r != null) observers.add(r);
    }

    public void notifyDataSetChanged() {
        for (Runnable r : observers) r.run();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return getCount() == 0;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }
}
