package android.util;

import java.util.Arrays;

/** Tiện ích so khớp và rút gọn mảng trạng thái View (state_pressed, state_focused, v.v.). */
public class StateSet {
    public static final int[] NOTHING = new int[] { 0 };
    public static final int[] WILD_CARD = new int[0];

    StateSet() {
    }

    public static boolean isWildCard(int[] stateSetOrSpec) {
        return stateSetOrSpec == null || stateSetOrSpec.length == 0 || stateSetOrSpec[0] == 0;
    }

    public static boolean stateSetMatches(int[] stateSpec, int[] stateSet) {
        if (stateSet == null) {
            return stateSpec == null || isWildCard(stateSpec);
        }
        int stateSpecSize = stateSpec.length;
        int stateSetSize = stateSet.length;
        for (int i = 0; i < stateSpecSize; i++) {
            int stateSpecState = stateSpec[i];
            if (stateSpecState == 0) {
                return true;
            }
            boolean mustMatch;
            if (stateSpecState > 0) {
                mustMatch = true;
            } else {
                mustMatch = false;
                stateSpecState = -stateSpecState;
            }
            boolean found = false;
            for (int j = 0; j < stateSetSize; j++) {
                int state = stateSet[j];
                if (state == 0) {
                    if (mustMatch) {
                        return false;
                    } else {
                        break;
                    }
                }
                if (state == stateSpecState) {
                    if (mustMatch) {
                        found = true;
                        break;
                    } else {
                        return false;
                    }
                }
            }
            if (mustMatch && !found) {
                return false;
            }
        }
        return true;
    }

    public static boolean stateSetMatches(int[] stateSpec, int state) {
        int stateSpecSize = stateSpec.length;
        for (int i = 0; i < stateSpecSize; i++) {
            int stateSpecState = stateSpec[i];
            if (stateSpecState == 0) {
                return true;
            }
            if (stateSpecState > 0) {
                if (state != stateSpecState) {
                    return false;
                }
            } else {
                if (state == -stateSpecState) {
                    return false;
                }
            }
        }
        return true;
    }

    public static int[] trimStateSet(int[] states, int newSize) {
        if (states == null || states.length == newSize) {
            return states;
        }
        return Arrays.copyOf(states, newSize);
    }

    public static String dump(int[] states) {
        if (states == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int state : states) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Integer.toHexString(state));
        }
        return sb.toString();
    }
}
