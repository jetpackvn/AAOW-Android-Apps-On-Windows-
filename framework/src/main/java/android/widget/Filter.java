package android.widget;

public abstract class Filter {
    public interface FilterListener {
        void onFilterComplete(int count);
    }

    protected static class FilterResults {
        public Object values;
        public int count;
    }

    public final void filter(CharSequence constraint) {
        filter(constraint, null);
    }

    public final void filter(CharSequence constraint, FilterListener listener) {
        FilterResults results = performFiltering(constraint);
        publishResults(constraint, results);
        if (listener != null) {
            listener.onFilterComplete(results != null ? results.count : 0);
        }
    }

    protected abstract FilterResults performFiltering(CharSequence constraint);

    protected abstract void publishResults(CharSequence constraint, FilterResults results);

    public CharSequence convertResultToString(Object resultValue) {
        return resultValue == null ? "" : resultValue.toString();
    }
}
