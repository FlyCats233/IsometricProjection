package io.github.flycats233.isometricprojection;

/** Horizontal basis shared by keyboard movement and orthographic mouse rays. */
public final class ControlMath {
    private ControlMath() { }
    public record Horizontal(double x, double z) { }
    public record Offset(double x, double y, double z) { }

    public static Horizontal worldMovement(double yaw, double forward, double left) {
        double angle = Math.toRadians(yaw);
        return new Horizontal(-Math.sin(angle) * forward + Math.cos(angle) * left,
                Math.cos(angle) * forward + Math.sin(angle) * left);
    }

    public static double facingYaw(Horizontal movement) {
        return Math.toDegrees(Math.atan2(-movement.x(), movement.z()));
    }

    public static Offset screenOffset(double yaw, double pitch, double right, double up) {
        double a = Math.toRadians(yaw), b = Math.toRadians(pitch);
        return new Offset(-Math.cos(a) * right - Math.sin(a) * Math.sin(b) * up,
                Math.cos(b) * up, -Math.sin(a) * right + Math.cos(a) * Math.sin(b) * up);
    }
}
