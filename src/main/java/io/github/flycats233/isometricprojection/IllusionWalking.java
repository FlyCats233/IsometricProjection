package io.github.flycats233.isometricprojection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Server-authoritative network teleports with local prediction. */
public final class IllusionWalking {
    private static volatile boolean enabled;
    private static volatile long generation;
    private static final Deque<Pending> pending = new ArrayDeque<>();
    private static Prediction teleportPrediction;
    private static int cooldown;
    private static BlockPos lastSource;
    private static BlockPos lastTarget;
    private static Vec3 lastMotion = Vec3.ZERO;
    private static int reverseGuard;
    private record Pending(long id, LocalPlayer player, ClientLevel level, Vec3 from, Vec3 to, long sentAt) { }
    private record Prediction(Vec3 position, Vec3 velocity, double xo, double yo, double zo,
            double xOld, double yOld, double zOld, float yaw, float pitch, boolean grounded, float fallDistance) {}

    private IllusionWalking() { }
    public static boolean enabled() { return enabled; }
    public static boolean busy() { return !pending.isEmpty(); }

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
        cancelTransfers();
        LocalCutaway.setEnabled(ClientConfig.CUTAWAY_ENABLED.get());
    }

    private static void cancelTransfers() {
        generation++;
        rollback(pending.peekFirst());
        pending.clear();
        teleportPrediction = null;
        lastSource = lastTarget = null;
        reverseGuard = 0;
        cooldown = 0;
    }

    private static void message(String key) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.translatable("message.isometricprojection." + key), true);
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (enabled && !serverSupported()) disable();
        // Leaving the view suspends traversal without changing the user's switch.
        if (!CameraController.active() && (!pending.isEmpty() || teleportPrediction != null || lastSource != null))
            cancelTransfers();
        if (cooldown > 0) cooldown--;
        if (reverseGuard > 0) reverseGuard--;
        if (!pending.isEmpty() && mc.level.getGameTime() - pending.peekFirst().sentAt > 40) {
            generation++;
            rollback(pending.peekFirst());
            pending.clear();
            teleportPrediction = null;
            cooldown = 10;
            message("illusion_failed");
        }
    }

    /** Swept test immediately before LocalPlayer.move applies its actual physics displacement. */
    public static boolean beforeMove(Vec3 movement) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled || !IsometricControls.controlling() || !serverSupported()) return false;
        LocalPlayer player = mc.player;
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
        if (fullTop(mc.level, adjacent)) return false;
        for (int distance = 1; distance <= 16; distance++) {
            for (int sign : new int[]{1, -1}) {
                IllusionMath.Shift shift = IllusionMath.alignedShift(CameraController.yaw(), CameraController.PITCH, distance * sign);
                if (shift == null) continue;
                BlockPos target = adjacent.offset(shift.x(), shift.y(), shift.z());
                if (reverseGuard > 0 && support.equals(lastTarget) && target.equals(lastSource)
                        && movement.x * lastMotion.x + movement.z * lastMotion.z >= 0) continue;
                if (!mc.level.hasChunkAt(target) || !mc.level.getWorldBorder().isWithinBounds(target)
                        || !fullTop(mc.level, target)) continue;
                Vec3 to = new Vec3(crossing.x() + shift.x(), target.getY() + 1, crossing.z() + shift.z());
                AABB landing = player.getBoundingBox().move(to.subtract(player.position()));
                if (!mc.level.noCollision(player, landing) || !IllusionVisibility.seamVisible(player, support, target,
                        crossing.stepX(), crossing.stepZ(), CameraController.yaw(), player.getEyePosition())) continue;
                transfer(support, target, to);
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

    private static void transfer(BlockPos support, BlockPos target, Vec3 to) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Vec3 from = player.position();
        float drag = mc.level.getBlockState(support).getFriction(mc.level, support, player) * .91F;
        Vec3 velocity = player.getDeltaMovement().multiply(drag, 0, drag);


        long id = ++generation;
        Pending request = new Pending(id, player, mc.level, from, to, mc.level.getGameTime());
        pending.addLast(request);
        // The major position change is parallel to the camera ray, so it does not shift the scene sideways.
        player.setPos(to);
        var shift = IllusionMath.alignedShift(CameraController.yaw(), CameraController.PITCH, target.getY() - support.getY());
        player.xo += shift.x(); player.yo += shift.y(); player.zo += shift.z();
        player.xOld += shift.x(); player.yOld += shift.y(); player.zOld += shift.z();
        shiftCloak(player, new Vec3(shift.x(), shift.y(), shift.z()));
        player.resetFallDistance();
        player.setOnGround(true);
        player.setDeltaMovement(velocity);
        lastSource = support; lastTarget = target; lastMotion = velocity; reverseGuard = 4;
        mc.getConnection().send(new IllusionNetwork.Request(id, support, target, from, to, velocity, CameraController.yaw()));
    }

    public static boolean serverSupported() {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(IllusionNetwork.Request.TYPE)
                && connection.hasChannel(IllusionNetwork.Result.TYPE);
    }

    public static void onResult(IllusionNetwork.Result result) {
        Pending request = pending.peekFirst();
        if (request != null && request.id == result.id() && !result.accepted()) {
            rollback(request);
            pending.clear();
            teleportPrediction = null;
            cooldown = 10;
            message("illusion_failed");
        }
    }

    public static void beforeTeleport(double x, double y, double z, boolean absolute) {
        teleportPrediction = null;
        var mc = Minecraft.getInstance();
        Pending request = pending.peekFirst();
        if (request == null) return;
        if (!absolute || mc.player != request.player || mc.level != request.level
                || request.to.distanceToSqr(new Vec3(x, y, z)) >= .01) {
            pending.clear();
            return;
        }
        var player = mc.player;
        teleportPrediction = new Prediction(player.position(), player.getDeltaMovement(), player.xo, player.yo, player.zo,
                player.xOld, player.yOld, player.zOld, player.getYRot(), player.getXRot(), player.onGround(), player.fallDistance);
    }

    public static void onTeleportApplied() {
        Prediction prediction = teleportPrediction;
        teleportPrediction = null;
        if (prediction == null || pending.isEmpty()) return;
        Pending request = pending.removeFirst();
        var mc = Minecraft.getInstance();
        if (mc.player != request.player || mc.level != request.level) return;
        var player = mc.player;
        player.setPos(prediction.position);
        player.xo = prediction.xo; player.yo = prediction.yo; player.zo = prediction.zo;
        player.xOld = prediction.xOld; player.yOld = prediction.yOld; player.zOld = prediction.zOld;
        player.setYRot(prediction.yaw); player.setXRot(prediction.pitch);
        player.setDeltaMovement(prediction.velocity);
        player.setOnGround(prediction.grounded);
        player.fallDistance = prediction.fallDistance;
        cooldown = 0;
    }
    private static void rollback(Pending request) {
        if (request == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == request.player && mc.level == request.level) {
            shiftCloak(mc.player, request.from.subtract(mc.player.position()));
            mc.player.setPos(request.from);
            mc.player.setOldPosAndRot();
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static void shiftCloak(LocalPlayer player, Vec3 shift) {
        // Preserve the cape's lag relative to the player across the teleport offset.
        player.xCloak += shift.x;
        player.yCloak += shift.y;
        player.zCloak += shift.z;
        player.xCloakO += shift.x;
        player.yCloakO += shift.y;
        player.zCloakO += shift.z;
    }
}
