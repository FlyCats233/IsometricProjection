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

    private static void checkReverseOcclusion() {
        var a = net.minecraft.core.BlockPos.ZERO;
        var b = a.offset(4, 3, -3);
        var c = b.offset(4, 3, -3);
        var up = net.minecraft.core.Direction.UP;
        var side = net.minecraft.core.Direction.EAST;
        var data = new OcclusionData();
        if (!data.add(a, up, b, up) || !data.add(b, up, c, up))
            throw new AssertionError("Non-conflicting chain rejected");
        var before = data.save(new net.minecraft.nbt.CompoundTag(), null);
        data.setDirty(false);
        if (!data.hasReverse(b, a) || data.add(b, side, a, side))
            throw new AssertionError("Reverse relation accepted with different clicked faces");
        if (!before.equals(data.save(new net.minecraft.nbt.CompoundTag(), null)) || data.isDirty())
            throw new AssertionError("Rejected reverse changed existing rules or consumed an ID");
        near(2, data.rules().size());
        if (!data.add(a, side, b, side)) throw new AssertionError("Same direction update rejected");
        near(2, data.rules().size());

        var legacy = new OcclusionData();
        legacy.add(a, up, b, up);
        var tag = legacy.save(new net.minecraft.nbt.CompoundTag(), null);
        var entries = tag.getList("rules", net.minecraft.nbt.Tag.TAG_COMPOUND);
        var reverse = entries.getCompound(0).copy();
        reverse.putLong("id", 2);
        reverse.putLong("a", b.asLong());
        reverse.putLong("b", a.asLong());
        entries.add(reverse);
        tag.putLong("next_id", 20);
        var loaded = OcclusionData.load(tag, null);
        near(1, loaded.rules().size());
        if (!loaded.rules().getFirst().hidden().equals(a) || !loaded.rules().getFirst().covering().equals(b)
                || !loaded.isDirty()) throw new AssertionError("Legacy reverse cleanup did not preserve first rule");
        var roundtrip = OcclusionData.load(loaded.save(new net.minecraft.nbt.CompoundTag(), null), null);
        if (!roundtrip.rules().equals(loaded.rules()) || roundtrip.isDirty())
            throw new AssertionError("Cleaned rules did not survive save and reload");
        if (!roundtrip.add(b, up, c, up)) throw new AssertionError("Independent relation rejected after migration");
        near(20, roundtrip.rules().getLast().id());
        if (!data.remove(a, up) || !data.add(b, up, a, up))
            throw new AssertionError("Cannot reverse after removing the old relation");
        near(1, data.rules().size());
    }

    public static void main(String[] args) {
        var facePos = net.minecraft.core.BlockPos.ZERO;
        var face = net.minecraft.core.Direction.SOUTH;
        var rayStart = new net.minecraft.world.phys.Vec3(.5, .5, 3);
        var rayEnd = new net.minecraft.world.phys.Vec3(.5, .5, -3);
        var faceHit = OcclusionRule.intersect(facePos, face, rayStart, rayEnd);
        if (faceHit == null) throw new AssertionError("Missing authored face intersection");
        near(1, faceHit.z);
        if (OcclusionRule.intersect(facePos, face, rayEnd, rayStart) != null)
            throw new AssertionError("Back-facing authored side was selected");
        if (OcclusionRule.intersect(facePos, face, rayStart.add(1, 0, 0), rayEnd.add(1, 0, 0)) != null)
            throw new AssertionError("Authored side extends beyond block bounds");
        OcclusionData savedRules = new OcclusionData();
        for (double yaw : new double[]{45, 135, 225, 315}) {
            for (int rise : new int[]{-32, -16, -3, 1, 16, 32}) {
                var shift = IllusionMath.alignedShift(yaw, 35.26439, rise);
                var aligned = facePos.offset(shift.x(), rise, shift.z());
                if (OcclusionRule.hasTopSeam(facePos, aligned))
                    throw new AssertionError("Coincident tops are not an edge seam");
                for (var side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    if (!OcclusionRule.hasTopSeam(facePos, aligned.relative(side))
                            || !OcclusionRule.hasTopSeam(aligned.relative(side), facePos))
                        throw new AssertionError("Valid top seam rejected: " + yaw + "/" + rise);
                    checks++;
                }
            }
        }
        if (savedRules.add(facePos, face, facePos.offset(3, 3, -3), face)
                || savedRules.add(facePos, face, facePos.offset(1, 0, 0), face)
                || savedRules.add(facePos, face, facePos.offset(8, 3, -3), face))
            throw new AssertionError("Non-seam pair accepted");
        var facePairs = OcclusionRule.visibleFacePairs(new net.minecraft.world.phys.Vec3(1, 1, -1));
        near(9, facePairs.size());
        if (!facePairs.contains(new OcclusionRule.FacePair(net.minecraft.core.Direction.NORTH,
                net.minecraft.core.Direction.EAST))) throw new AssertionError("Cross-direction side overlap missing");
        if (!facePairs.contains(new OcclusionRule.FacePair(net.minecraft.core.Direction.EAST,
                net.minecraft.core.Direction.UP)) || !facePairs.contains(new OcclusionRule.FacePair(
                net.minecraft.core.Direction.UP, net.minecraft.core.Direction.NORTH)))
            throw new AssertionError("Top/side overlap missing");
        savedRules.add(facePos, face, facePos.offset(4, 3, -3), net.minecraft.core.Direction.UP);
        var restoredRules = OcclusionData.load(savedRules.save(new net.minecraft.nbt.CompoundTag(), null), null);
        near(1, restoredRules.rules().size());
        if (!restoredRules.rules().equals(savedRules.rules())) throw new AssertionError("Occlusion save roundtrip");
        restoredRules.add(facePos, net.minecraft.core.Direction.EAST, facePos.offset(5, 4, -4), face);
        near(1, restoredRules.rules().size());
        if (!restoredRules.remove(facePos.offset(5, 4, -4), face)) throw new AssertionError("Covering face cannot remove its rule");
        near(0, restoredRules.rules().size());
        checkReverseOcclusion();
        ViewTransition transition = new ViewTransition();
        transition.retarget(true, true, 0);
        near(.5, transition.sample(200_000_000));
        transition.retarget(false, true, 200_000_000);
        near(.5, transition.sample(200_000_000));
        near(0, transition.sample(325_000_000));
        transition.retarget(true, false, 400_000_000);
        near(1, transition.sample(400_000_000));
        Matrix4f vanillaProjection = new Matrix4f().perspective(1, 1.5f, .05f, 256);
        near(vanillaProjection.m00(), ViewTransition.projection(vanillaProjection, 0, 18, 12, 256, 32).m00());
        near(1.0 / 18, ViewTransition.projection(vanillaProjection, 1, 18, 12, 256, 32).m00());
        SmoothZoom zoom = new SmoothZoom(24, 8, 64, 200);
        zoom.adjust(-2, 0);
        near(24, zoom.sample(0)); near(23, zoom.sample(100)); near(22, zoom.sample(200));
        zoom.adjust(-2, 200);
        double interruptedZoom = zoom.sample(250);
        zoom.adjust(2, 250);
        near(interruptedZoom, zoom.sample(250)); near(22, zoom.sample(450));
        zoom.adjust(-100, 500); near(8, zoom.sample(700));
        zoom.adjust(-2, 700); near(8, zoom.sample(800));
        zoom.adjust(100, 800); near(64, zoom.sample(1000));
        zoom.adjust(2, 1000); near(64, zoom.finish());
        zoom.adjust(-2, 1100); zoom.adjust(-2, 1150); near(60, zoom.sample(1350));
        SmoothZoom indoor = new SmoothZoom(24, 8, 64, 200);
        indoor.adjust(4, 0);
        indoor.setCovered(true, 100);
        near(26, indoor.sample(100));
        indoor.setCovered(true, 200);
        indoor.adjust(-2, 200);
        near(8, indoor.sample(300));
        indoor.setCovered(false, 300);
        near(18, indoor.sample(400));
        near(28, indoor.sample(500));
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
        SmoothRotation fine = new SmoothRotation(350, 200);
        near(370, fine.moveImmediately(20, 0));
        near(370, fine.sample(100));
        fine.alignToNearest(100);
        near(387.5, fine.sample(200));
        near(405, fine.sample(300));
        near(365, fine.moveImmediately(-40, 400));
        fine.alignToNearest(400);
        near(405, fine.sample(600));
        near(355, fine.moveImmediately(-50, 700));
        fine.alignToNearest(700);
        near(315, fine.sample(900));
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
