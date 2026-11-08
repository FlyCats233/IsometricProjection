package io.github.flycats233.isometricprojection;

/** A circular screen-space opening extruded from the player's head towards the camera. */
public record CutawayVolume(boolean enabled, double headX, double headY, double headZ,
        double towardX, double towardY, double towardZ, double radius, double depth, int firstHiddenY) {
    public static final CutawayVolume DISABLED = create(false, 0, 0, 0, 45, 35.26439, 4, 32);

    public static CutawayVolume create(boolean enabled, double x, double eyeY, double z,
            double yaw, double pitch, double radius, double depth) {
        double a = Math.toRadians(yaw), b = Math.toRadians(pitch);
        return new CutawayVolume(enabled, x, eyeY, z,
                Math.sin(a) * Math.cos(b), Math.sin(b), -Math.cos(a) * Math.cos(b),
                radius, depth, (int) Math.floor(eyeY));
    }

    public boolean hides(int x, int y, int z) {
        // Include the block layer containing the player's head, preserving all lower layers.
        if (!enabled || y < firstHiddenY) return false;
        double dx = x + .5 - headX;
        double dy = y + .5 - headY;
        double dz = z + .5 - headZ;
        double alongView = dx * towardX + dy * towardY + dz * towardZ;
        if (alongView <= 0 || alongView > depth) return false;
        double screenDistanceSquared = Math.max(0, dx * dx + dy * dy + dz * dz - alongView * alongView);
        return screenDistanceSquared <= radius * radius;
    }

    // Conservative world bounds for rebuilding old and new cut surfaces, including rotation.
    public int minX() { return (int) Math.floor(Math.min(headX, headX + towardX * depth) - radius); }
    public int maxX() { return (int) Math.ceil(Math.max(headX, headX + towardX * depth) + radius); }
    public int minZ() { return (int) Math.floor(Math.min(headZ, headZ + towardZ * depth) - radius); }
    public int maxZ() { return (int) Math.ceil(Math.max(headZ, headZ + towardZ * depth) + radius); }
    public int maxY() { return (int) Math.ceil(Math.max(headY, headY + towardY * depth) + radius); }
}
