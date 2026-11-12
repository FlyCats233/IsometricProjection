package io.github.flycats233.isometricprojection;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public final class IllusionNetwork {
    private IllusionNetwork() { }
    public static Consumer<Result> clientReceiver = result -> { };
    private static final WeakHashMap<ServerPlayer, Long> LAST_REQUEST = new WeakHashMap<>();

    public record Request(long id, BlockPos source, BlockPos target, Vec3 from, Vec3 to, Vec3 velocity, float cameraYaw) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("isometricprojection", "crossing"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of((b,p) -> {
            b.writeLong(p.id); b.writeBlockPos(p.source); b.writeBlockPos(p.target); b.writeVec3(p.from);
            b.writeVec3(p.to); b.writeVec3(p.velocity); b.writeFloat(p.cameraYaw);
        }, b -> new Request(b.readLong(), b.readBlockPos(), b.readBlockPos(), b.readVec3(), b.readVec3(), b.readVec3(), b.readFloat()));
        @Override public Type<Request> type() { return TYPE; }
    }

    public record Result(long id, boolean accepted) implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("isometricprojection", "crossing_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Result> CODEC = StreamCodec.of((b,p) -> {
            b.writeLong(p.id); b.writeBoolean(p.accepted);
        }, b -> new Result(b.readLong(), b.readBoolean()));
        @Override public Type<Result> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2").optional();
        registrar.playToServer(Request.TYPE, Request.CODEC, (p,c) -> c.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer)c.player();
            boolean accepted = validate(player, p);
            if (accepted) {
                player.connection.teleport(p.to.x, p.to.y, p.to.z, player.getYRot(), player.getXRot());
                player.setDeltaMovement(p.velocity);
                player.resetFallDistance(); player.setOnGround(true);
            } else {
                // Correct any client-side prediction before reporting rejection.
                player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            }
            c.reply(new Result(p.id, accepted));
        }));
        registrar.playToClient(Result.TYPE, Result.CODEC, (p,c) -> c.enqueueWork(() -> clientReceiver.accept(p)));
    }

    private static boolean finite(Vec3 v) { return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z); }

    private static boolean validate(ServerPlayer player, Request p) {
        long now = player.serverLevel().getGameTime();
        Long last = LAST_REQUEST.put(player, now);
        if (last != null && last == now) return false;
        if (!finite(p.from) || !finite(p.to) || !finite(p.velocity) || !Float.isFinite(p.cameraYaw)
                || !player.isAlive() || !player.onGround() || player.isPassenger() || player.getAbilities().flying
                || player.position().distanceToSqr(p.from) > 4 || p.velocity.horizontalDistance() > 1
                || Math.abs(p.velocity.y) > .001 || Math.abs(p.from.y - p.source.getY() - 1) > .08
                || Math.abs(p.to.y - p.target.getY() - 1) > .001) return false;
        var level = player.serverLevel();
        if (!level.hasChunkAt(p.source) || !level.hasChunkAt(p.target) || !level.getWorldBorder().isWithinBounds(p.target)
                || !level.getBlockState(p.source).isCollisionShapeFullBlock(level, p.source)
                || !level.getBlockState(p.target).isCollisionShapeFullBlock(level, p.target)) return false;
        if (p.from.x < p.source.getX() - .3 || p.from.x > p.source.getX() + 1.3
                || p.from.z < p.source.getZ() - .3 || p.from.z > p.source.getZ() + 1.3
                || p.to.x < p.target.getX() || p.to.x > p.target.getX() + 1
                || p.to.z < p.target.getZ() || p.to.z > p.target.getZ() + 1) return false;
        int rise = p.target.getY() - p.source.getY();
        if (Math.abs(rise) > 16) return false;
        var shift = IllusionMath.alignedShift(p.cameraYaw, 35.26439, rise);
        if (shift == null) return false;
        int edgeX = p.target.getX() - p.source.getX() - shift.x();
        int edgeZ = p.target.getZ() - p.source.getZ() - shift.z();
        if (Math.abs(edgeX) + Math.abs(edgeZ) != 1) return false;
        Vec3 step = p.to.subtract(new Vec3(shift.x(), shift.y(), shift.z())).subtract(p.from);
        if (step.horizontalDistance() > 1 || step.x * edgeX + step.z * edgeZ < -.001) return false;
        AABB box = player.getBoundingBox().move(p.to.subtract(player.position()));
        return level.noCollision(player, box) && IllusionVisibility.seamVisible(player, p.source, p.target,
                edgeX, edgeZ, p.cameraYaw, p.from.add(0, player.getEyeHeight(), 0));
    }
}
