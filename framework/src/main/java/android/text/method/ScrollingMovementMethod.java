package android.text.method;

public class ScrollingMovementMethod extends BaseMovementMethod implements MovementMethod {
    private static ScrollingMovementMethod instance;

    public static MovementMethod getInstance() {
        if (instance == null) {
            instance = new ScrollingMovementMethod();
        }
        return instance;
    }
}
