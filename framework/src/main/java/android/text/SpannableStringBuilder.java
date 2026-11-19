package android.text;

public class SpannableStringBuilder implements CharSequence, GetChars, Spannable, Editable, Appendable {
    private final StringBuilder sb;
    private final Editable.SpanStore spans = new Editable.SpanStore();

    public SpannableStringBuilder() {
        this.sb = new StringBuilder();
    }

    public SpannableStringBuilder(CharSequence text) {
        this.sb = new StringBuilder(text != null ? text : "");
    }

    public SpannableStringBuilder(CharSequence text, int start, int end) {
        this.sb = new StringBuilder(text != null ? text.subSequence(start, end) : "");
    }

    @Override
    public int length() {
        return sb.length();
    }

    @Override
    public char charAt(int index) {
        return sb.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return sb.subSequence(start, end);
    }

    @Override
    public void getChars(int start, int end, char[] dest, int destoff) {
        sb.getChars(start, end, dest, destoff);
    }

    @Override
    public SpannableStringBuilder replace(int st, int en, CharSequence source, int start, int end) {
        sb.replace(st, en, source.subSequence(start, end).toString());
        return this;
    }

    @Override
    public SpannableStringBuilder replace(int st, int en, CharSequence text) {
        sb.replace(st, en, text != null ? text.toString() : "");
        return this;
    }

    @Override
    public SpannableStringBuilder insert(int where, CharSequence text, int start, int end) {
        sb.insert(where, text, start, end);
        return this;
    }

    @Override
    public SpannableStringBuilder insert(int where, CharSequence text) {
        sb.insert(where, text);
        return this;
    }

    @Override
    public SpannableStringBuilder delete(int st, int en) {
        sb.delete(st, en);
        return this;
    }

    @Override
    public SpannableStringBuilder append(CharSequence text) {
        sb.append(text);
        return this;
    }

    @Override
    public SpannableStringBuilder append(CharSequence text, int start, int end) {
        sb.append(text, start, end);
        return this;
    }

    @Override
    public SpannableStringBuilder append(char text) {
        sb.append(text);
        return this;
    }

    @Override
    public void clear() {
        sb.setLength(0);
        spans.clear();
    }

    @Override
    public void clearSpans() {
        spans.clear();
    }

    @Override
    public void setSpan(Object what, int start, int end, int flags) {
        spans.setSpan(what, start, end, flags);
    }

    @Override
    public void removeSpan(Object what) {
        spans.removeSpan(what);
    }

    @Override
    public <T> T[] getSpans(int start, int end, Class<T> type) {
        return spans.getSpans(start, end, type);
    }

    @Override
    public int getSpanStart(Object tag) {
        return spans.getSpanStart(tag);
    }

    @Override
    public int getSpanEnd(Object tag) {
        return spans.getSpanEnd(tag);
    }

    @Override
    public int getSpanFlags(Object tag) {
        return spans.getSpanFlags(tag);
    }

    @Override
    public int nextSpanTransition(int start, int limit, Class type) {
        return spans.nextSpanTransition(start, limit, type);
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
