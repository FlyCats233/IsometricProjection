package io.github.flycats233.isometricprojection;

import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Headless regression checks: screen-relative input and screen-ray inverse projection. */
public final class ControlMathCheck {
    private static int checks;
    private static void near(double expected, double actual) {
        checks++;
        if (!Double.isFinite(actual) || Math.abs(expected - actual) > 1.0E-5) throw new AssertionError(expected + " != " + actual);
    }

    public static void main(String[] args) {
        // At the classic isometric angle, integer offsets along (1,1,-1) have zero screen displacement.
        for (double angle : new double[]{45, 135, 225, 315, 405}) {
            Matrix4f view = new Matrix4f().rotationX((float)Math.toRadians(35.26439)).rotateY((float)Math.toRadians(angle + 180));
            for (int rise = -16; rise <= 16; rise++) {
                IllusionMath.Shift shift = IllusionMath.alignedShift(angle, 35.26439, rise);
                if (rise == 0) {
                    if (shift != null) throw new AssertionError("Zero shift is not an illusion transfer");
                    continue;
                }
                if (shift == null) throw new AssertionError("Missing aligned isometric connection");
                Vector4f projected = view.transform(new Vector4f(shift.x(), shift.y(), shift.z(), 0));
                near(0, projected.x);
                near(0, projected.y);
                near(rise, shift.y());
            }
        }
        if (IllusionMath.alignedShift(60, 35.26439, 3) != null) throw new AssertionError("Disconnected faces accepted");
        if (IllusionMath.alignedShift(45, 0, 3) != null) throw new AssertionError("Horizontal camera accepted");
        if (IllusionMath.crossing(0, 0, .5, .5, 1, 0, .2) != null) throw new AssertionError("Transfer away from edge");
        var east = IllusionMath.crossing(0, 0, .92, .5, 1, 0, .2);
        near(1, east.stepX()); near(0, east.stepZ()); near(1.12, east.x()); near(.5, east.z());
        var west = IllusionMath.crossing(-1, -1, -.92, -.5, -1, 0, .2);
        near(-1, west.stepX()); near(0, west.stepZ()); near(-1.12, west.x()); near(-.5, west.z());
        var north = IllusionMath.crossing(0, 0, .5, .08, 0, -1, .2);
        near(0, north.stepX()); near(-1, north.stepZ()); near(.5, north.x()); near(-.12, north.z());
        var overhang = IllusionMath.crossing(0, 0, 1.12, .5, 1, 0, .286);
        near(1.406, overhang.x());
        if (IllusionMath.crossing(0, 0, 1.5, .5, 1, 0, .286) != null) throw new AssertionError("Stale support accepted");
        // Sweep all tick phases, including sprinting and speed boosts exceeding the old .25 cap.
        for (double speed : new double[]{.05, .1, .215, .286, .42, .65}) {
            for (int phase = 1; phase < 100; phase++) {
                double x = phase / 100D;
                var crossing = IllusionMath.crossing(0, 0, x, .5, 1, 0, speed);
                if (x + speed < 1 - 1.0E-8) {
                    if (crossing != null) throw new AssertionError("Early crossing");
                } else {
                    if (crossing == null) throw new AssertionError("Missed fast crossing at " + x + " / " + speed);
                    near(Math.clamp(x + speed, 1.03, 1.97), crossing.x());
                }
            }
            int bx = 0, bz = 0, crossed = 0;
            double x = .5, z = .5;
            for (int tick = 0; tick < 400; tick++) {
                var crossing = IllusionMath.crossing(bx, bz, x, z, 1, 0, speed);
                if (crossing == null) x += speed;
                else {
                    // Every successive tile is another illusion seam; no global cooldown may skip it.
                    bx += 4; bz -= 3;
                    x = crossing.x() + 3; z = crossing.z() - 3;
                    near(bx, Math.floor(x)); near(bz, Math.floor(z)); crossed++;
                }
            }
            if (crossed < 10) throw new AssertionError("Chain did not advance");
        }
        CutawayVolume room = CutawayVolume.create(true, .5, 65.62, .5, 0, 35.26439, 4, 32);
        // Retain floor, rear buildings and side walls; cut the head-height foreground layer too.
        near(0, room.hides(0, 63, 0) ? 1 : 0);
        near(1, room.hides(0, 65, -1) ? 1 : 0);
        near(0, room.hides(0, 64, -1) ? 1 : 0);
        near(1, room.hides(0, 66, -1) ? 1 : 0);
        near(1, room.hides(0, 68, -4) ? 1 : 0);
        near(0, room.hides(0, 68, 4) ? 1 : 0);
        near(0, room.hides(6, 68, -4) ? 1 : 0);
        // A high foreground roof can occlude the player from farther than the old horizontal radius.
        near(1, room.hides(0, 73, -11) ? 1 : 0);
        near(0, room.hides(0, 90, -36) ? 1 : 0);
        near(0, CutawayVolume.DISABLED.hides(0, 1, 0) ? 1 : 0);
        near(0, room.hides(Integer.MAX_VALUE, 68, Integer.MIN_VALUE) ? 1 : 0);
        CutawayVolume reverse = CutawayVolume.create(true, .5, 65.62, .5, 180, 35.26439, 4, 32);
        near(1, reverse.hides(0, 68, 4) ? 1 : 0);
        near(0, reverse.hides(0, 68, -4) ? 1 : 0);
        CutawayVolume higherHead = CutawayVolume.create(true, .5, 66.62, .5, 0, 35.26439, 4, 32);
        near(1, higherHead.hides(0, 66, -1) ? 1 : 0);
        near(0, higherHead.hides(0, 65, -1) ? 1 : 0);
        for (double angle : new double[]{0, 45, 67.5, 90, 135, 180, 225, 270, 315}) {
            CutawayVolume volume = CutawayVolume.create(true, -10.5, 65.62, -20.5, angle, 35.26439, 4, 32);
            for (int x = -48; x <= 28; x++) for (int y = 64; y <= 92; y++) for (int z = -58; z <= 18; z++) {
                if (volume.hides(x, y, z)) {
                    if (x < volume.minX() || x > volume.maxX() || z < volume.minZ() || z > volume.maxZ()
                            || y < volume.firstHiddenY() || y > volume.maxY()) throw new AssertionError("Dirty bounds omit cut block");
                    checks++;
                }
            }
        }
        SmoothRotation rotation = new SmoothRotation(315, 250);
        rotation.rotateBy(90, 0);
        near(315, rotation.sample(0));
        near(360, rotation.sample(125));
        near(405, rotation.sample(250));
        near(405, rotation.sample(500));
        rotation.rotateBy(-90, 500);
        double interrupted = rotation.sample(600);
        rotation.rotateBy(90, 600);
        near(interrupted, rotation.sample(600));
        near(405, rotation.sample(850));
        rotation.rotateBy(90, 900);
        rotation.rotateBy(90, 950);
        near(585, rotation.sample(1200));
        near(585, rotation.finish());
        for (double yaw : new double[]{45, 67.5, 90, 135, 180, 225, 270, 315, 360, 405}) {
                for (double[] input : new double[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {.3, -.3}}) {
                    var world = ControlMath.worldMovement(yaw, input[0], input[1]);
                    double speed = Math.hypot(world.x(), world.z());
                    // The player faces travel and uses only positive local-forward movement.
                    var facing = ControlMath.worldMovement(ControlMath.facingYaw(world), 1, 0);
                    near(world.x() / speed, facing.x());
                    near(world.z() / speed, facing.z());
                    near(input[0]*input[0] + input[1]*input[1], speed*speed);
                }
            for (float span : new float[]{8, 24, 64}) {
                for (float aspect : new float[]{1, 16F/9, 21F/9}) {
                    Matrix4f matrix = new Matrix4f().setOrtho(-span/2*aspect, span/2*aspect, -span/2, span/2, .05F, 384)
                            .translate(0, 0, -32)
                            .mul(new Matrix4f().rotationX((float)Math.toRadians(35.26439)).rotateY((float)Math.toRadians(yaw+180)));
                    var forward = ControlMath.worldMovement(yaw, 1, 0);
                    var left = ControlMath.worldMovement(yaw, 0, 1);
                    Vector4f screenForward = matrix.transform(new Vector4f((float)forward.x(), 0, (float)forward.z(), 0));
                    Vector4f screenLeft = matrix.transform(new Vector4f((float)left.x(), 0, (float)left.z(), 0));
                    near(0, screenForward.x);
                    near(0, screenLeft.y);
                    if (screenForward.y <= 0 || screenLeft.x >= 0) throw new AssertionError("Screen movement reversed");
                    for (double x : new double[]{-.9, 0, .9}) {
                        for (double y : new double[]{-.9, 0, .9}) {
                            var offset = ControlMath.screenOffset(yaw, 35.26439, x*span*aspect/2, y*span/2);
                            Vector4f clip = matrix.transform(new Vector4f((float)offset.x(), (float)offset.y(), (float)offset.z(), 1));
                            near(x, clip.x / clip.w);
                            near(y, clip.y / clip.w);
                        }
                    }
                }
            }
        }
        System.out.println("PASS: " + checks + " control and mouse-ray checks");
    }
}
