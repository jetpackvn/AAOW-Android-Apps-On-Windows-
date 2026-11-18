package android.text;

public interface Editable extends CharSequence {
    Editable replace(int st, int en, CharSequence source, int start, int end);
    Editable replace(int st, int en, CharSequence text);
    Editable insert(int where, CharSequence text, int start, int end);
    Editable insert(int where, CharSequence text);
    Editable delete(int st, int en);
    Editable append(CharSequence text);
    Editable append(CharSequence text, int start, int end);
    Editable append(char text);
    void clear();

    final class SimpleEditable implements Editable {
        private final StringBuilder sb;

        public SimpleEditable(CharSequence cs) {
            this.sb = new StringBuilder(cs != null ? cs : "");
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
        public Editable replace(int st, int en, CharSequence source, int start, int end) {
            sb.replace(st, en, source.subSequence(start, end).toString());
            return this;
        }

        @Override
        public Editable replace(int st, int en, CharSequence text) {
            sb.replace(st, en, text != null ? text.toString() : "");
            return this;
        }

        @Override
        public Editable insert(int where, CharSequence text, int start, int end) {
            sb.insert(where, text, start, end);
            return this;
        }

        @Override
        public Editable insert(int where, CharSequence text) {
            sb.insert(where, text);
            return this;
        }

        @Override
        public Editable delete(int st, int en) {
            sb.delete(st, en);
            return this;
        }

        @Override
        public Editable append(CharSequence text) {
            sb.append(text);
            return this;
        }

        @Override
        public Editable append(CharSequence text, int start, int end) {
            sb.append(text, start, end);
            return this;
        }

        @Override
        public Editable append(char text) {
            sb.append(text);
            return this;
        }

        @Override
        public void clear() {
            sb.setLength(0);
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }
}
