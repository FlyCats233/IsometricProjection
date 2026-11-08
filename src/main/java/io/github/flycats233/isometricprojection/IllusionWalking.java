package io.github.flycats233.isometricprojection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Server-authoritative network teleports with local prediction. */
public final class IllusionWalking {
    private static volatile boolean enabled;
    private static volatile long generation;
    private static Pending pending;
    private static int pendingTicks;
    private static int cooldown;
    private static BlockPos lastSource;
    private static BlockPos lastTarget;
    private static Vec3 lastMotion = Vec3.ZERO;
    private static int reverseGuard;
    private record Pending(long id, LocalPlayer player, ClientLevel level, Vec3 from, Vec3 to, Vec3 velocity,
            BlockPos source, BlockPos target) { }

    private IllusionWalking() { }
    public static boolean enabled() { return enabled; }
    public static boolean busy() { return pending != null; }

    public static void toggle() {
        Minecraft mc = Minecraft.getInstance();
        if (enabled) { disable(); message("illusion_off"); return; }
        if (!CameraController.active()) { message("illusion_requires_iso"); return; }
        if (!serverSupported()) { message("illusion_singleplayer"); return; }
        LocalCutaway.setEnabled(false);
        enabled = true;
        generation++;
        cooldown = 0;
        reverseGuard = 0;
        lastSource = lastTarget = null;
        message("illusion_on");
    }

    private static void disable() {
        if (!enabled) return;
        enabled = false;
        generation++;
        rollback(pending);
        pending = null;
        lastSource = lastTarget = null;
        reverseGuard = 0;
        LocalCutaway.setEnabled(ClientConfig.CUTAWAY_ENABLED.get());
    }

    private static void message(String key) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.translatable("message.isometricprojection." + key), true);
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (enabled && (!CameraController.active() || !serverSupported())) disable();
        if (cooldown > 0) cooldown--;
        if (reverseGuard > 0) reverseGuard--;
        if (pending != null && ++pendingTicks > 40) {
            generation++;
            rollback(pending);
            pending = null;
            cooldown = 10;
            message("illusion_failed");
        }
    }

    /** Swept test immediately before LocalPlayer.move applies its actual physics displacement. */
    public static boolean beforeMove(Vec3 movement) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled || !IsometricControls.controlling() || !serverSupported()) return false;
        LocalPlayer player = mc.player;
        if (pending != null) {
            // No drift/fall while waiting for the authoritative packet; momentum resumes at acknowledgement.
            player.setDeltaMovement(Vec3.ZERO);
            player.setOnGround(true);
            return true;
        }
        double length = movement.horizontalDistance();
        if (cooldown > 0 || length < 1.0E-5 || player.input.jumping || player.input.forwardImpulse <= 0
                || CameraController.isRotating()
                || LocalCutaway.snapshot().enabled() || player.isPassenger() || !player.onGround()
                || player.getAbilities().flying || player.isSwimming()) return false;
        BlockPos support = supportingBlock(player);
        if (support == null) return false;
        IllusionMath.Crossing crossing = IllusionMath.crossing(support.getX(), support.getZ(), player.getX(), player.getZ(),
                movement.x / length, movement.z / length, length);
        if (crossing == null) return false;
        BlockPos adjacent = support.offset(crossing.stepX(), 0, crossing.stepZ());
        if (fullTop(mc.level, adjacent) || !visibleTop(support)) return false;
        for (int distance = 1; distance <= 16; distance++) {
            for (int sign : new int[]{1, -1}) {
                IllusionMath.Shift shift = IllusionMath.alignedShift(CameraController.yaw(), CameraController.PITCH, distance * sign);
                if (shift == null) continue;
                BlockPos target = adjacent.offset(shift.x(), shift.y(), shift.z());
                if (reverseGuard > 0 && support.equals(lastTarget) && target.equals(lastSource)
                        && movement.x * lastMotion.x + movement.z * lastMotion.z >= 0) continue;
                if (!mc.level.hasChunkAt(target) || !mc.level.getWorldBorder().isWithinBounds(target)
                        || !fullTop(mc.level, target) || !visibleTop(target)) continue;
                Vec3 to = new Vec3(crossing.x() + shift.x(), target.getY() + 1, crossing.z() + shift.z());
                AABB landing = player.getBoundingBox().move(to.subtract(player.position()));
                if (!mc.level.noCollision(player, landing)) continue;
                transfer(support, target, to, landing);
                return true;
            }
        }
        return false;
    }

    private static BlockPos supportingBlock(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        int y = net.minecraft.util.Mth.floor(player.getY() - .05);
        if (Math.abs(player.getY() - y - 1) > .08) return null;
        BlockPos center = BlockPos.containing(player.getX(), y, player.getZ());
        if (fullTop(mc.level, center)) return center;
        // The center can already be past an edge while the player's footprint is still supported.
        AABB bounds = player.getBoundingBox();
        BlockPos best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int x = net.minecraft.util.Mth.floor(bounds.minX + .001); x <= net.minecraft.util.Mth.floor(bounds.maxX - .001); x++) {
            for (int z = net.minecraft.util.Mth.floor(bounds.minZ + .001); z <= net.minecraft.util.Mth.floor(bounds.maxZ - .001); z++) {
                BlockPos pos = new BlockPos(x, y, z);
                double distance = Math.pow(x + .5 - player.getX(), 2) + Math.pow(z + .5 - player.getZ(), 2);
                if (distance < bestDistance && fullTop(mc.level, pos)) { best = pos; bestDistance = distance; }
            }
        }
        return best;
    }

    private static boolean fullTop(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).isCollisionShapeFullBlock(level, pos);
    }

    private static boolean visibleTop(BlockPos support) {
        Minecraft mc = Minecraft.getInstance();
        double a = Math.toRadians(CameraController.yaw()), b = Math.toRadians(CameraController.PITCH);
        Vec3 toward = new Vec3(Math.sin(a) * Math.cos(b), Math.sin(b), -Math.cos(a) * Math.cos(b));
        Vec3 top = new Vec3(support.getX() + .5, support.getY() + 1.002, support.getZ() + .5);
        double depth = top.subtract(mc.player.getEyePosition()).dot(toward);
        if (depth >= CameraController.DISTANCE - .05) return false;
        Vec3 start = top.add(toward.scale(CameraController.DISTANCE - .05 - depth));
        return mc.level.clip(new ClipContext(start, top, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player))
                .getType() == HitResult.Type.MISS;
    }

    private static void transfer(BlockPos support, BlockPos target, Vec3 to, AABB landing) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Vec3 from = player.position();
        float drag = mc.level.getBlockState(support).getFriction(mc.level, support, player) * .91F;
        Vec3 velocity = player.getDeltaMovement().multiply(drag, 0, drag);
        float yaw = player.getYRot(), pitch = player.getXRot();
        var uuid = player.getUUID();
        var dimension = player.level().dimension();
        long id = ++generation;
        Pending request = new Pending(id, player, mc.level, from, to, velocity, support, target);
        pending = request;
        pendingTicks = 0;
        // The major position change is parallel to the camera ray, so it does not shift the scene sideways.
        player.setPos(to);
        player.setOldPosAndRot();
        player.resetFallDistance();
        player.setOnGround(true);
        player.setDeltaMovement(Vec3.ZERO);
        mc.getConnection().send(new IllusionNetwork.Request(id, support, target, from, to, velocity, CameraController.yaw()));
    }

    public static boolean serverSupported() {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(IllusionNetwork.Request.TYPE)
                && connection.hasChannel(IllusionNetwork.Result.TYPE);
    }

    public static void onResult(IllusionNetwork.Result result) {
        Pending request = pending;
        if (request != null && request.id == result.id() && !result.accepted()) {
            rollback(request);
            pending = null;
            cooldown = 10;
            message("illusion_failed");
        }
    }

    public static void onTeleportApplied() {
        Minecraft mc = Minecraft.getInstance();
        Pending request = pending;
        if (request != null && mc.player == request.player && mc.level == request.level
                && mc.player.position().distanceToSqr(request.to) < .01) {
            mc.player.setOldPosAndRot();
            mc.player.setDeltaMovement(request.velocity);
            mc.player.setOnGround(true);
            mc.player.resetFallDistance();
            lastSource = request.source;
            lastTarget = request.target;
            lastMotion = request.velocity;
            reverseGuard = 4;
            pending = null;
            cooldown = 0;
        }
    }

    private static void rollback(Pending request) {
        if (request == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == request.player && mc.level == request.level && mc.player.position().distanceToSqr(request.to) < 4) {
            mc.player.setPos(request.from);
            mc.player.setOldPosAndRot();
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
    }
}
