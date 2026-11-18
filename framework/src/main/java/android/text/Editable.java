package android.text;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public interface Editable extends CharSequence, GetChars, Spannable, Appendable {
    Editable replace(int st, int en, CharSequence source, int start, int end);

    Editable replace(int st, int en, CharSequence text);

    Editable insert(int where, CharSequence text, int start, int end);

    Editable insert(int where, CharSequence text);

    Editable delete(int st, int en);

    @Override
    Editable append(CharSequence text);

    @Override
    Editable append(CharSequence text, int start, int end);

    @Override
    Editable append(char text);

    void clear();

    void clearSpans();

    final class SpanStore {
        private static final class Entry {
            final Object what;
            int start;
            int end;
            int flags;

            Entry(Object what, int start, int end, int flags) {
                this.what = what;
                this.start = start;
                this.end = end;
                this.flags = flags;
            }
        }

        private final List<Entry> entries = new ArrayList<>();

        public void setSpan(Object what, int start, int end, int flags) {
            if (what == null) return;
            removeSpan(what);
            entries.add(new Entry(what, start, end, flags));
        }

        public void removeSpan(Object what) {
            entries.removeIf(e -> e.what == what || (what != null && what.equals(e.what)));
        }

        public void clear() {
            entries.clear();
        }

        @SuppressWarnings("unchecked")
        public <T> T[] getSpans(int start, int end, Class<T> type) {
            List<T> matched = new ArrayList<>();
            for (Entry e : entries) {
                if (e.end >= start && e.start <= end) {
                    if (type == null || type.isInstance(e.what)) {
                        matched.add((T) e.what);
                    }
                }
            }
            Class<?> elemType = type != null ? type : Object.class;
            T[] out = (T[]) Array.newInstance(elemType, matched.size());
            return matched.toArray(out);
        }

        public int getSpanStart(Object tag) {
            for (Entry e : entries) {
                if (e.what == tag) return e.start;
            }
            return -1;
        }

        public int getSpanEnd(Object tag) {
            for (Entry e : entries) {
                if (e.what == tag) return e.end;
            }
            return -1;
        }

        public int getSpanFlags(Object tag) {
            for (Entry e : entries) {
                if (e.what == tag) return e.flags;
            }
            return 0;
        }

        public int nextSpanTransition(int start, int limit, Class<?> type) {
            int next = limit;
            for (Entry e : entries) {
                if (type == null || type.isInstance(e.what)) {
                    if (e.start > start && e.start < next) next = e.start;
                    if (e.end > start && e.end < next) next = e.end;
                }
            }
            return next;
        }
    }

    class SimpleEditable extends SpannableStringBuilder {
        public SimpleEditable(CharSequence cs) {
            super(cs);
        }
    }
}
