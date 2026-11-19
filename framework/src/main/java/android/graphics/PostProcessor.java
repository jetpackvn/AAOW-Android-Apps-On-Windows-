package android.graphics;

/** Bộ hậu xử lý Canvas sau khi ImageDecoder giải mã ảnh. */
public interface PostProcessor {
    int onPostProcess(Canvas canvas);
}
