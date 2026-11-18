package android.view.animation;

public abstract class BaseInterpolator implements Interpolator {
    private int changingConfiguration;

    public int getChangingConfiguration() {
        return changingConfiguration;
    }

    void setChangingConfiguration(int changingConfiguration) {
        this.changingConfiguration = changingConfiguration;
    }
}
