package com.google.android.gms.appset;

public class AppSetIdInfo {
    public static final int SCOPE_APP = 1;
    public static final int SCOPE_DEVELOPER = 2;

    private final String id;
    private final int scope;

    public AppSetIdInfo(String id, int scope) {
        this.id = id;
        this.scope = scope;
    }

    public String getId() {
        return id;
    }

    public int getScope() {
        return scope;
    }
}
