package com.google.android.gms.common.internal;

import android.os.Looper;

public final class Preconditions {
    private Preconditions() {
    }

    public static <T> T checkNotNull(T reference) {
        if (reference == null) throw new NullPointerException("null reference");
        return reference;
    }

    public static <T> T checkNotNull(T reference, Object errorMessage) {
        if (reference == null) throw new NullPointerException(String.valueOf(errorMessage));
        return reference;
    }

    public static String checkNotEmpty(String string) {
        if (string == null || string.isEmpty()) throw new IllegalArgumentException("Given String is empty or null");
        return string;
    }

    public static String checkNotEmpty(String string, Object errorMessage) {
        if (string == null || string.isEmpty()) throw new IllegalArgumentException(String.valueOf(errorMessage));
        return string;
    }

    public static void checkArgument(boolean expression) {
        if (!expression) throw new IllegalArgumentException();
    }

    public static void checkArgument(boolean expression, Object errorMessage) {
        if (!expression) throw new IllegalArgumentException(String.valueOf(errorMessage));
    }

    public static void checkState(boolean expression) {
        if (!expression) throw new IllegalStateException();
    }

    public static void checkState(boolean expression, Object errorMessage) {
        if (!expression) throw new IllegalStateException(String.valueOf(errorMessage));
    }

    public static void checkMainThread(String errorMessage) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalStateException(errorMessage);
        }
    }

    public static void checkNotMainThread() {
        checkNotMainThread("Must not be called on the main application thread");
    }

    public static void checkNotMainThread(String errorMessage) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw new IllegalStateException(errorMessage);
        }
    }
}
