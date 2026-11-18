package android.widget;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;

public abstract class BaseAdapter implements ListAdapter, SpinnerAdapter {
    private final DataSetObservable dataSetObservable = new DataSetObservable();
    private final List<Runnable> observers = new CopyOnWriteArrayList<>();

    public void registerObserver(Runnable r) {
        if (r != null) observers.add(r);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        dataSetObservable.registerObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        dataSetObservable.unregisterObserver(observer);
    }

    public void notifyDataSetChanged() {
        dataSetObservable.notifyChanged();
        for (Runnable r : observers) r.run();
    }

    public void notifyDataSetInvalidated() {
        dataSetObservable.notifyInvalidated();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
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
