package io.github.flycats233.isometricprojection;

import org.joml.Matrix4f;

/** Reversible blend from the original camera to the isometric camera. */
public final class ViewTransition {
    private float start, target;
    private long startedAt;
    private long duration;

    public float sample(long now) {
        if (duration == 0) return target;
        double t = Math.clamp((double) (now - startedAt) / duration, 0, 1);
        return (float) (start + (target - start) * t * t * (3 - 2 * t));
    }

    public void retarget(boolean isometric, boolean animate, long now) {
        start = sample(now);
        target = isometric ? 1 : 0;
        startedAt = now;
        duration = animate ? Math.round((isometric ? 400_000_000D : 250_000_000D) * Math.abs(target - start)) : 0;
    }

    public static Matrix4f projection(Matrix4f vanilla, float progress, float halfWidth, float halfHeight,
            float far, float standoff) {
        if (progress <= 0) return vanilla;
        float distance = progress * standoff;
        var orthographic = new Matrix4f().setOrtho(-halfWidth, halfWidth, -halfHeight, halfHeight, .05f, far)
                .translate(0, 0, -distance);
        if (progress >= 1) return orthographic;
        var perspective = new Matrix4f(vanilla).translate(0, 0, -distance);
        // Normalize homogeneous W near the player so the blend changes scale smoothly.
        return new Matrix4f().zero().lerp(perspective, 1 / (1 + distance)).lerp(orthographic, progress);
    }
}
