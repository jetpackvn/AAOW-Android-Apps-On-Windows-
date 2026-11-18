package android.webkit;

public class JsResult {
    private boolean result;

    public JsResult() {
    }

    public final void cancel() {
        this.result = false;
    }

    public final void confirm() {
        this.result = true;
    }

    public final boolean getResult() {
        return result;
    }
}
