package io.github.flycats233.isometricprojection;

/** Monotonic-time rotation; angles stay unwrapped so 315 -> 405 takes a quarter turn. */
public final class SmoothRotation {
    private final long durationNanos;
    private double start;
    private double target;
    private long startedAt;
    private boolean moving;

    public SmoothRotation(double initial, long durationNanos) {
        if (durationNanos <= 0) throw new IllegalArgumentException("duration must be positive");
        this.start = this.target = initial;
        this.durationNanos = durationNanos;
    }

    public double sample(long now) {
        if (!moving) return target;
        double t = Math.clamp((double) (now - startedAt) / durationNanos, 0, 1);
        if (t >= 1) { moving = false; return target; }
        double eased = t * t * (3 - 2 * t);
        return start + (target - start) * eased;
    }

    public void rotateBy(double degrees, long now) {
        start = sample(now);
        target += degrees;
        startedAt = now;
        moving = true;
    }

    public double moveImmediately(double degrees, long now) {
        target = sample(now) + degrees;
        return finish();
    }

    public void alignToNearest(long now) {
        start = sample(now);
        target = 45 + 90 * Math.round((start - 45) / 90);
        startedAt = now;
        moving = start != target;
    }

    public void rotateTo(double degrees, long now) {
        double delta = net.minecraft.util.Mth.wrapDegrees(degrees - target);
        if (delta != 0) rotateBy(delta, now);
    }

    public double finish() {
        moving = false;
        return start = target;
    }
    public boolean isMoving() { return moving; }
}
