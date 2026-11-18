package com.google.firebase;

public class FirebaseException extends Exception {
    protected FirebaseException() {
    }

    public FirebaseException(String detailMessage) {
        super(detailMessage);
    }

    public FirebaseException(String detailMessage, Throwable cause) {
        super(detailMessage, cause);
    }
}
