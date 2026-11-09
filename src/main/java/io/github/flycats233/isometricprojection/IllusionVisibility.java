package io.github.flycats233.isometricprojection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class IllusionVisibility {
    private static final double[] EDGE_SAMPLES = {.125, .5, .875};
    private static final double EDGE_INSET = .06;
    private static final double NEAR_PLANE = 31.95;

    private IllusionVisibility() {}

    static boolean seamVisible(Player player, BlockPos source, BlockPos target, int edgeX, int edgeZ,
            float yaw, Vec3 eye) {
        double a = Math.toRadians(yaw), b = Math.toRadians(35.26439);
        Vec3 toward = new Vec3(Math.sin(a) * Math.cos(b), Math.sin(b), -Math.cos(a) * Math.cos(b));
        if (!visible(player, top(source, .5, .5), toward, eye)
                || !visible(player, top(target, .5, .5), toward, eye)) return false;
        for (double along : EDGE_SAMPLES) {
            if (!visible(player, edgePoint(source, edgeX, edgeZ, along), toward, eye)
                    || !visible(player, edgePoint(target, -edgeX, -edgeZ, along), toward, eye)) return false;
        }
        return true;
    }

    static Vec3 edgePoint(BlockPos pos, int edgeX, int edgeZ, double along) {
        double edge = .5 - EDGE_INSET;
        return top(pos, edgeX == 0 ? along : .5 + edgeX * edge,
                edgeZ == 0 ? along : .5 + edgeZ * edge);
    }

    private static Vec3 top(BlockPos pos, double x, double z) {
        return new Vec3(pos.getX() + x, pos.getY() + 1.002, pos.getZ() + z);
    }

    private static boolean visible(Player player, Vec3 point, Vec3 toward, Vec3 eye) {
        double distance = NEAR_PLANE - point.subtract(eye).dot(toward);
        if (distance <= 0) return false;
        Vec3 start = point.add(toward.scale(distance));
        var level = player.level();
        if (!level.isClientSide() && !level.hasChunksAt(BlockPos.containing(start), BlockPos.containing(point))) {
            return false;
        }
        return level.clip(new ClipContext(start, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                .getType() == HitResult.Type.MISS;
    }
}
