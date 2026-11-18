package android.widget;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ArrayAdapter<T> extends BaseAdapter {
    private final Context context;
    private final List<T> objects;

    public ArrayAdapter(Context context, int resource) {
        this(context, resource, new ArrayList<>());
    }

    public ArrayAdapter(Context context, int resource, T[] objects) {
        this(context, resource, new ArrayList<>(Arrays.asList(objects)));
    }

    public ArrayAdapter(Context context, int resource, List<T> objects) {
        this.context = context;
        this.objects = objects != null ? objects : new ArrayList<>();
    }

    public Context getContext() {
        return context;
    }

    public void add(T object) {
        objects.add(object);
        notifyDataSetChanged();
    }

    public void addAll(Collection<? extends T> collection) {
        objects.addAll(collection);
        notifyDataSetChanged();
    }

    public void insert(T object, int index) {
        objects.add(index, object);
        notifyDataSetChanged();
    }

    public void remove(T object) {
        objects.remove(object);
        notifyDataSetChanged();
    }

    public void clear() {
        objects.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return objects.size();
    }

    @Override
    public T getItem(int position) {
        return objects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv = convertView instanceof TextView t ? t : new TextView(context);
        tv.setPadding(16, 12, 16, 12);
        tv.setTextSize(14f);
        tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        T item = getItem(position);
        tv.setText(item != null ? item.toString() : "");
        return tv;
    }
}
