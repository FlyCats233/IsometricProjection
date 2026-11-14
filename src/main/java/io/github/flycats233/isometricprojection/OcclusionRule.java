package io.github.flycats233.isometricprojection;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** A directed block-pair relation, applied where any of their visible faces overlap. */
public record OcclusionRule(long id, BlockPos hidden, Direction hiddenFace, BlockPos covering, Direction coveringFace) {
    public static boolean hasTopSeam(BlockPos a, BlockPos b) {
        int rise = b.getY() - a.getY();
        if (rise == 0 || a.distSqr(b) > 64 * 64) return false;
        for (double yaw : new double[]{45, 135, 225, 315}) {
            var shift = IllusionMath.alignedShift(yaw, 35.26439, rise);
            if (shift != null && Math.abs(b.getX() - a.getX() - shift.x())
                    + Math.abs(b.getZ() - a.getZ() - shift.z()) == 1) return true;
        }
        return false;
    }

    public record FacePair(Direction hidden, Direction covering) {}
    public static List<FacePair> visibleFacePairs(Vec3 toward) {
        var pairs = new java.util.ArrayList<FacePair>();
        for (Direction a : Direction.values()) {
            if (toward.dot(Vec3.atLowerCornerOf(a.getNormal())) <= 1.0E-6) continue;
            for (Direction b : Direction.values()) {
                if (toward.dot(Vec3.atLowerCornerOf(b.getNormal())) > 1.0E-6) pairs.add(new FacePair(a, b));
            }
        }
        return pairs;
    }
    public static boolean validBlock(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        var state = level.getBlockState(pos);
        return state.getRenderShape() == RenderShape.MODEL && state.canOcclude()
                && state.isCollisionShapeFullBlock(level, pos) && !state.hasBlockEntity();
    }

    public boolean valid(Level level) {
        return hasTopSeam(hidden, covering) && validBlock(level, hidden) && validBlock(level, covering);
    }

    public static Vec3 faceCenter(BlockPos pos, Direction face) {
        return Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5));
    }

    public static Vec3 intersect(BlockPos pos, Direction face, Vec3 start, Vec3 end) {
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 delta = end.subtract(start);
        double denominator = delta.dot(normal);
        // Only outward-facing surfaces can participate in a visible overlap.
        if (denominator >= -1.0E-8) return null;
        double t = faceCenter(pos, face).subtract(start).dot(normal) / denominator;
        if (t < 0 || t > 1) return null;
        Vec3 point = start.add(delta.scale(t));
        double epsilon = 1.0E-5;
        return point.x >= pos.getX() - epsilon && point.x <= pos.getX() + 1 + epsilon
                && point.y >= pos.getY() - epsilon && point.y <= pos.getY() + 1 + epsilon
                && point.z >= pos.getZ() - epsilon && point.z <= pos.getZ() + 1 + epsilon ? point : null;
    }

    public static BlockHitResult resolve(Level level, List<OcclusionRule> rules, Vec3 start, Vec3 end,
            BlockHitResult original) {
        if (original.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return original;
        for (OcclusionRule rule : rules) {
            if (!rule.hidden.equals(original.getBlockPos()) || !rule.valid(level)) continue;
            Vec3 front = intersect(rule.hidden, original.getDirection(), start, end);
            for (Direction face : Direction.values()) {
                Vec3 back = intersect(rule.covering, face, start, end);
                if (front != null && back != null && start.distanceToSqr(back) > start.distanceToSqr(front)) {
                    return new BlockHitResult(back, face, rule.covering, false);
                }
            }
        }
        return original;
    }

    static boolean visible(Player player, Vec3 start, Vec3 point) {
        Level level = player.level();
        List<OcclusionRule> rules = level instanceof ServerLevel server
                ? OcclusionData.get(server).rules() : OcclusionNetwork.clientRules.get();
        var hit = level.clip(new ClipContext(start, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) return true;
        for (var rule : rules) {
            if (!rule.hidden.equals(hit.getBlockPos()) || !rule.valid(level)
                    || !new AABB(rule.covering).inflate(.01).intersects(new AABB(point, point).inflate(.02))) continue;
            var second = level.clip(new ClipContext(start, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player) {
                @Override public VoxelShape getBlockShape(BlockState state, BlockGetter world, BlockPos pos) {
                    return pos.equals(rule.hidden) ? Shapes.empty() : super.getBlockShape(state, world, pos);
                }
            });
            if (second.getType() == HitResult.Type.MISS) return true;
        }
        return false;
    }
}
