package android.graphics;

public class Shader {
    public enum TileMode {
        CLAMP,
        REPEAT,
        MIRROR,
        DECAL
    }

    private Matrix localMatrix;

    public boolean getLocalMatrix(Matrix localM) {
        if (localMatrix != null && localM != null) {
            localM.set(localMatrix);
            return true;
        }
        return false;
    }

    public void setLocalMatrix(Matrix localM) {
        if (localM == null) {
            this.localMatrix = null;
        } else {
            if (this.localMatrix == null) this.localMatrix = new Matrix();
            this.localMatrix.set(localM);
        }
    }
}
