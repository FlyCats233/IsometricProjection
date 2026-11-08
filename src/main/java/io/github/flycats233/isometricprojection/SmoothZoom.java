package io.github.flycats233.isometricprojection;

/** Bounded, retargetable zoom interpolation in visible world units. */
public final class SmoothZoom {
    private final double minimum, maximum;
    private final long duration;
    private double start, target;
    private long startedAt;
    private boolean moving;

    public SmoothZoom(double initial, double minimum, double maximum, long duration) {
        if (minimum > maximum || duration <= 0) throw new IllegalArgumentException();
        this.minimum = minimum; this.maximum = maximum; this.duration = duration;
        start = target = Math.clamp(initial, minimum, maximum);
    }

    public double sample(long now) {
        if (!moving) return target;
        double t = Math.clamp((double)(now - startedAt) / duration, 0, 1);
        if (t >= 1) { moving = false; return target; }
        return start + (target - start) * t * t * (3 - 2 * t);
    }

    public void adjust(double delta, long now) {
        double next = Math.clamp(target + delta, minimum, maximum);
        if (next == target) return;
        start = sample(now);
        target = next;
        startedAt = now;
        moving = true;
    }

    public double finish() { moving = false; return start = target; }
}
