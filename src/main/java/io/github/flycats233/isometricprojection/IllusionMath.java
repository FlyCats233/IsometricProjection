package io.github.flycats233.isometricprojection;

/** Geometry for two unit top faces sharing a projected edge. No world access. */
public final class IllusionMath {
    private IllusionMath() { }
    public record Shift(int x, int y, int z) { }
    public record Crossing(int stepX, int stepZ, double x, double z) { }

    public static Shift alignedShift(double yaw, double pitch, int rise) {
        if (rise == 0) return null;
        double a = Math.toRadians(yaw), b = Math.toRadians(pitch);
        if (Math.sin(b) < .1) return null;
        double x = Math.sin(a) * rise / Math.tan(b);
        double z = -Math.cos(a) * rise / Math.tan(b);
        int ix = (int) Math.round(x), iz = (int) Math.round(z);
        double depth = ix * Math.sin(a) * Math.cos(b) + rise * Math.sin(b) - iz * Math.cos(a) * Math.cos(b);
        double errorSquared = Math.max(0, (double)ix * ix + rise * rise + iz * iz - depth * depth);
        return errorSquared <= .015 * .015 ? new Shift(ix, rise, iz) : null;
    }

    public static Crossing crossing(int blockX, int blockZ, double x, double z, double dx, double dz, double probe) {
        double tx = Math.abs(dx) < 1.0E-8 ? Double.POSITIVE_INFINITY : ((dx > 0 ? blockX + 1 : blockX) - x) / dx;
        double tz = Math.abs(dz) < 1.0E-8 ? Double.POSITIVE_INFINITY : ((dz > 0 ? blockZ + 1 : blockZ) - z) / dz;
        double t = Math.min(tx, tz);
        // A player's center may overhang while the 0.6-block-wide footprint still has support.
        if (t < -.3 || t > probe + 1.0E-8) return null;
        int stepX = tx <= tz ? (dx > 0 ? 1 : -1) : 0;
        int stepZ = tx <= tz ? 0 : (dz > 0 ? 1 : -1);
        // Preserve this move's remaining displacement instead of dropping it at the seam.
        double nx = Math.clamp(x + dx * probe, blockX + stepX + .03, blockX + stepX + .97);
        double nz = Math.clamp(z + dz * probe, blockZ + stepZ + .03, blockZ + stepZ + .97);
        return new Crossing(stepX, stepZ, nx, nz);
    }
}
