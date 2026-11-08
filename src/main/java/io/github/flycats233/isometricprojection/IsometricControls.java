package io.github.flycats233.isometricprojection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import org.lwjgl.glfw.GLFW;

public final class IsometricControls {
    private static boolean cursorWasFree;

    private IsometricControls() { }

    public static boolean controlling() {
        Minecraft mc = Minecraft.getInstance();
        return CameraController.active() && mc.screen == null && mc.getOverlay() == null
                && mc.isWindowActive() && mc.getCameraEntity() == mc.player && mc.player.isAlive();
    }

    public static void freeCursor() {
        Minecraft mc = Minecraft.getInstance();
        GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean free = controlling();
        if (free) {
            // Keep vanilla's logical capture for button handling, but expose the OS pointer.
            freeCursor();
        } else {
            if (cursorWasFree && mc.mouseHandler.isMouseGrabbed() && mc.isWindowActive()) {
                GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                mc.mouseHandler.setIgnoreFirstMove();
            }
        }
        cursorWasFree = free;
    }

    /** Called instead of vanilla eye-direction picking, before vanilla click handling. */
    public static void pick(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (!controlling()) return;
        double width = Math.max(1, mc.getWindow().getScreenWidth());
        double height = Math.max(1, mc.getWindow().getScreenHeight());
        double aspect = (double) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
        double right = (2 * mc.mouseHandler.xpos() / width - 1) * CameraController.span() * aspect / 2;
        double up = (1 - 2 * mc.mouseHandler.ypos() / height) * CameraController.span() / 2;
        ControlMath.Offset offset = ControlMath.screenOffset(CameraController.yaw(), CameraController.PITCH, right, up);
        double yaw = Math.toRadians(CameraController.yaw());
        double pitch = Math.toRadians(CameraController.PITCH);
        Vec3 direction = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3 eye = mc.player.getEyePosition(partialTick);
        Vec3 origin = eye.add(offset.x(), offset.y(), offset.z()).subtract(direction.scale(CameraController.DISTANCE - .05));
        double depth = Math.max(256, mc.options.getEffectiveRenderDistance() * 32D) - .05;
        Vec3 end = origin.add(direction.scale(depth));
        BlockHitResult block = mc.level.clip(LocalCutaway.selectionContext(origin, end, mc.player));
        double blockDistance = block.getType() == HitResult.Type.MISS ? origin.distanceToSqr(end) : origin.distanceToSqr(block.getLocation());
        Vec3 entityEnd = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(mc.player, origin, entityEnd,
                new AABB(origin, entityEnd).inflate(1), e -> !e.isSpectator() && e.isPickable(), blockDistance);
        HitResult pointed = entity == null ? block : entity;

        Vec3 aim = pointed.getType() == HitResult.Type.MISS ? eye.add(offset.x(), offset.y(), offset.z()) : pointed.getLocation();
        double reach = entity == null ? mc.player.blockInteractionRange() : mc.player.entityInteractionRange();
        boolean reachable = pointed.getType() != HitResult.Type.MISS && eye.distanceToSqr(pointed.getLocation()) <= reach * reach;
        if (reachable) {
            // The screen ray must not grant interaction through a wall next to the player.
            BlockHitResult obstruction = mc.level.clip(new ClipContext(eye, pointed.getLocation(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            reachable = obstruction.getType() == HitResult.Type.MISS
                    || (entity == null && obstruction.getBlockPos().equals(block.getBlockPos()));
        }
        mc.hitResult = reachable ? pointed : BlockHitResult.miss(aim, Direction.getNearest(direction.x, direction.y, direction.z), net.minecraft.core.BlockPos.containing(aim));
        mc.crosshairPickEntity = reachable && entity != null ? entity.getEntity() : null;
    }

    public static void movement(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player || !CameraController.active()) return;
        Input input = event.getInput();
        if (!controlling()) {
            input.forwardImpulse = input.leftImpulse = 0;
            return;
        }
        ControlMath.Horizontal world = ControlMath.worldMovement(CameraController.yaw(), input.forwardImpulse, input.leftImpulse);
        double speed = Math.hypot(world.x(), world.z());
        if (speed > 1.0E-5) {
            float desiredYaw = (float) ControlMath.facingYaw(world);
            float yaw = mc.player.getYRot() + net.minecraft.util.Mth.wrapDegrees(desiredYaw - mc.player.getYRot());
            mc.player.setYRot(yaw);
            mc.player.setYHeadRot(yaw);
            mc.player.setYBodyRot(yaw);
            mc.player.setXRot(0);
        }
        // Every direction is forward locomotion after turning, including S and diagonals.
        input.leftImpulse = 0;
        input.forwardImpulse = (float) Math.min(1, speed);
        input.up = input.forwardImpulse > 0;
        input.down = input.forwardImpulse < 0;
        input.left = input.leftImpulse > 0;
        input.right = input.leftImpulse < 0;
    }

}
