package android.webkit;

public class JsPromptResult extends JsResult {
    private String stringResult;

    public JsPromptResult() {
    }

    public void confirm(String result) {
        this.stringResult = result;
        confirm();
    }

    public String getStringResult() {
        return stringResult;
    }
}
