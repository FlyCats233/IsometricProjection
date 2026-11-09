package io.github.flycats233.isometricprojection;

/** Bounded, retargetable zoom interpolation in visible world units. */
public final class SmoothZoom {
    private final double minimum, maximum;
    private final long duration;
    private double start, target;
    private double manualTarget;
    private boolean covered;
    private long startedAt;
    private boolean moving;

    public SmoothZoom(double initial, double minimum, double maximum, long duration) {
        if (minimum > maximum || duration <= 0) throw new IllegalArgumentException();
        this.minimum = minimum; this.maximum = maximum; this.duration = duration;
        start = target = manualTarget = Math.clamp(initial, minimum, maximum);
    }

    public double sample(long now) {
        if (!moving) return target;
        double t = Math.clamp((double)(now - startedAt) / duration, 0, 1);
        if (t >= 1) { moving = false; return target; }
        return start + (target - start) * t * t * (3 - 2 * t);
    }

    public void adjust(double delta, long now) {
        if (covered) return;
        manualTarget = Math.clamp(manualTarget + delta, minimum, maximum);
        updateTarget(now);
    }

    public void setCovered(boolean covered, long now) {
        this.covered = covered;
        updateTarget(now);
    }

    public double manualTarget() { return manualTarget; }

    private void updateTarget(long now) {
        double next = covered ? minimum : manualTarget;
        if (next == target) return;
        start = sample(now);
        target = next;
        startedAt = now;
        moving = true;
    }

    public double finish() { moving = false; return start = target; }
}
