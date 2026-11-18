package android.text;

public class TextDirectionHeuristics {
    public static final TextDirectionHeuristic LTR = fixed(false);
    public static final TextDirectionHeuristic RTL = fixed(true);
    public static final TextDirectionHeuristic FIRSTSTRONG_LTR = fixed(false);
    public static final TextDirectionHeuristic FIRSTSTRONG_RTL = fixed(true);
    public static final TextDirectionHeuristic ANYRTL_LTR = fixed(false);
    public static final TextDirectionHeuristic LOCALE = fixed(false);

    private static TextDirectionHeuristic fixed(boolean rtl) {
        return new TextDirectionHeuristic() {
            @Override
            public boolean isRtl(char[] array, int start, int count) {
                return rtl;
            }

            @Override
            public boolean isRtl(CharSequence cs, int start, int count) {
                return rtl;
            }
        };
    }
}
