package android.graphics;

import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * Cài đặt thật android.graphics.PathMeasure dựa trên FlatteningPathIterator của Java2D.
 * Tính độ dài thực (getLength), tọa độ & tiếp tuyến tại khoảng cách d (getPosTan),
 * và trích xuất đoạn con (getSegment) phục vụ các thư viện vẽ đồ thị (WilliamChart, MPAndroidChart...) và Vector/Lottie.
 */
public class PathMeasure {
    public static final int POSITION_MATRIX_FLAG = 0x01;
    public static final int TANGENT_MATRIX_FLAG = 0x02;

    private record Segment(float x0, float y0, float x1, float y1, float len, float cumStart, float cumEnd) {
    }

    private final List<List<Segment>> contours = new ArrayList<>();
    private final List<Float> contourLengths = new ArrayList<>();
    private int currentContour = 0;

    public PathMeasure() {
    }

    public PathMeasure(Path path, boolean forceClosed) {
        setPath(path, forceClosed);
    }

    public void setPath(Path path, boolean forceClosed) {
        contours.clear();
        contourLengths.clear();
        currentContour = 0;
        if (path == null) return;

        PathIterator it = path.awtPath.getPathIterator(null, 0.5);
        float[] coords = new float[6];
        float startX = 0f, startY = 0f;
        float lastX = 0f, lastY = 0f;
        List<Segment> current = null;
        float cumLen = 0f;

        while (!it.isDone()) {
            int type = it.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_MOVETO -> {
                    if (current != null && !current.isEmpty()) {
                        if (forceClosed && (lastX != startX || lastY != startY)) {
                            float dx = startX - lastX;
                            float dy = startY - lastY;
                            float l = (float) Math.hypot(dx, dy);
                            if (l > 1e-5f) {
                                current.add(new Segment(lastX, lastY, startX, startY, l, cumLen, cumLen + l));
                                cumLen += l;
                            }
                        }
                        contours.add(current);
                        contourLengths.add(cumLen);
                    }
                    current = new ArrayList<>();
                    cumLen = 0f;
                    startX = lastX = coords[0];
                    startY = lastY = coords[1];
                }
                case PathIterator.SEG_LINETO -> {
                    if (current == null) {
                        current = new ArrayList<>();
                        cumLen = 0f;
                        startX = lastX;
                        startY = lastY;
                    }
                    float nx = coords[0];
                    float ny = coords[1];
                    float dx = nx - lastX;
                    float dy = ny - lastY;
                    float l = (float) Math.hypot(dx, dy);
                    if (l > 1e-5f) {
                        current.add(new Segment(lastX, lastY, nx, ny, l, cumLen, cumLen + l));
                        cumLen += l;
                    }
                    lastX = nx;
                    lastY = ny;
                }
                case PathIterator.SEG_CLOSE -> {
                    if (current != null && (lastX != startX || lastY != startY)) {
                        float dx = startX - lastX;
                        float dy = startY - lastY;
                        float l = (float) Math.hypot(dx, dy);
                        if (l > 1e-5f) {
                            current.add(new Segment(lastX, lastY, startX, startY, l, cumLen, cumLen + l));
                            cumLen += l;
                        }
                        lastX = startX;
                        lastY = startY;
                    }
                }
            }
            it.next();
        }

        if (current != null && !current.isEmpty()) {
            if (forceClosed && (lastX != startX || lastY != startY)) {
                float dx = startX - lastX;
                float dy = startY - lastY;
                float l = (float) Math.hypot(dx, dy);
                if (l > 1e-5f) {
                    current.add(new Segment(lastX, lastY, startX, startY, l, cumLen, cumLen + l));
                    cumLen += l;
                }
            }
            contours.add(current);
            contourLengths.add(cumLen);
        }
    }

    public float getLength() {
        if (currentContour < 0 || currentContour >= contourLengths.size()) {
            return 0f;
        }
        return contourLengths.get(currentContour);
    }

    public boolean nextContour() {
        if (currentContour + 1 < contours.size()) {
            currentContour++;
            return true;
        }
        currentContour = contours.size();
        return false;
    }

    public boolean getPosTan(float distance, float[] pos, float[] tan) {
        if (currentContour < 0 || currentContour >= contours.size()) return false;
        List<Segment> segs = contours.get(currentContour);
        if (segs.isEmpty()) return false;

        float total = contourLengths.get(currentContour);
        float d = Math.max(0f, Math.min(total, distance));
        Segment target = segs.get(segs.size() - 1);
        for (Segment s : segs) {
            if (d <= s.cumEnd || s == target) {
                target = s;
                break;
            }
        }

        float t = target.len > 1e-5f ? (d - target.cumStart) / target.len : 0f;
        t = Math.max(0f, Math.min(1f, t));
        if (pos != null && pos.length >= 2) {
            pos[0] = target.x0 + (target.x1 - target.x0) * t;
            pos[1] = target.y0 + (target.y1 - target.y0) * t;
        }
        if (tan != null && tan.length >= 2) {
            if (target.len > 1e-5f) {
                tan[0] = (target.x1 - target.x0) / target.len;
                tan[1] = (target.y1 - target.y0) / target.len;
            } else {
                tan[0] = 1f;
                tan[1] = 0f;
            }
        }
        return true;
    }

    public boolean getSegment(float startD, float stopD, Path dst, boolean startWithMoveTo) {
        if (dst == null || currentContour < 0 || currentContour >= contours.size()) return false;
        float len = getLength();
        if (startD < 0f) startD = 0f;
        if (stopD > len) stopD = len;
        if (startD >= stopD) return false;

        List<Segment> segs = contours.get(currentContour);
        boolean started = false;
        for (Segment s : segs) {
            if (s.cumEnd <= startD) continue;
            if (s.cumStart >= stopD) break;

            float t0 = startD > s.cumStart ? (startD - s.cumStart) / s.len : 0f;
            float t1 = stopD < s.cumEnd ? (stopD - s.cumStart) / s.len : 1f;
            float sx = s.x0 + (s.x1 - s.x0) * t0;
            float sy = s.y0 + (s.y1 - s.y0) * t0;
            float ex = s.x0 + (s.x1 - s.x0) * t1;
            float ey = s.y0 + (s.y1 - s.y0) * t1;

            if (!started) {
                if (startWithMoveTo) {
                    dst.moveTo(sx, sy);
                } else {
                    dst.lineTo(sx, sy);
                }
                started = true;
            }
            dst.lineTo(ex, ey);
        }
        return started;
    }

    public boolean isClosed() {
        if (currentContour < 0 || currentContour >= contours.size()) return false;
        List<Segment> segs = contours.get(currentContour);
        if (segs.isEmpty()) return false;
        Segment first = segs.get(0);
        Segment last = segs.get(segs.size() - 1);
        return Math.hypot(first.x0 - last.x1, first.y0 - last.y1) < 1e-3;
    }
}
