package io.github.flycats233.isometricprojection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

public final class IsometricControls {
    private static boolean cursorWasFree;
    private static boolean fineCursorCaptured;

    private IsometricControls() { }

    public static boolean controlling() {
        Minecraft mc = Minecraft.getInstance();
        return CameraController.active() && !CameraController.transitioning() && mc.screen == null && mc.getOverlay() == null
                && mc.isWindowActive() && mc.getCameraEntity() == mc.player && mc.player.isAlive();
    }

    public static void freeCursor() {
        Minecraft mc = Minecraft.getInstance();
        GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR,
                ProjectileAim.active() && !mc.options.hideGui ? GLFW.GLFW_CURSOR_HIDDEN : GLFW.GLFW_CURSOR_NORMAL);
    }

    static void restoreVanillaCursor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.getOverlay() != null || !mc.isWindowActive()) return;
        // Closing a screen can leave either Minecraft's grab or the GLFW cursor mode released.
        mc.mouseHandler.grabMouse();
        GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        mc.mouseHandler.setIgnoreFirstMove();
        cursorWasFree = false;
        fineCursorCaptured = false;
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean free = controlling();
        if (free) {
            boolean capture = CameraController.fineRotationHeld();
            if (capture != fineCursorCaptured) {
                mc.mouseHandler.setIgnoreFirstMove();
                fineCursorCaptured = capture;
            }
            if (capture) {
                GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
            } else {
                freeCursor();
            }
        } else {
            fineCursorCaptured = false;
            if (cursorWasFree && mc.mouseHandler.isMouseGrabbed() && mc.isWindowActive()) {
                GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
                mc.mouseHandler.setIgnoreFirstMove();
            }
        }
        cursorWasFree = free;
    }

    public static boolean fineCursorCaptured() { return fineCursorCaptured; }

    public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!CameraController.transitioning()) return;
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    /** Called instead of vanilla eye-direction picking, before vanilla click handling. */
    public static void pick(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (!controlling()) return;
        if (fineCursorCaptured || CameraController.fineRotationHeld()) {
            Vec3 eye = mc.player.getEyePosition(partialTick);
            mc.hitResult = BlockHitResult.miss(eye, Direction.UP, net.minecraft.core.BlockPos.containing(eye));
            mc.crosshairPickEntity = null;
            return;
        }
        CursorRay ray = cursorRay(partialTick);
        Vec3 origin = ray.origin(), end = ray.end(), eye = mc.player.getEyePosition(partialTick);
        Vec3 direction = end.subtract(origin).normalize();
        BlockHitResult block = mc.level.clip(LocalCutaway.selectionContext(origin, end, mc.player));
        pickTarget(mc, origin, end, eye, direction, block);
    }

    public record CursorRay(Vec3 origin, Vec3 end) {}

    public static BlockHitResult cursorBlockHit(ClipContext.Fluid fluids) {
        Minecraft mc = Minecraft.getInstance();
        CursorRay ray = cursorRay(mc.getTimer().getGameTimeDeltaPartialTick(true));
        return mc.level.clip(LocalCutaway.selectionContext(ray.origin(), ray.end(), mc.player, fluids));
    }

    static CursorRay cursorRay(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
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
        return new CursorRay(origin, end);
    }

    public static CursorRay reachableCursorRay(double reach) {
        Minecraft mc = Minecraft.getInstance();
        CursorRay ray = cursorRay(1);
        Vec3 eye = mc.player.getEyePosition();
        Vec3 direction = ray.end().subtract(ray.origin()).normalize();
        Vec3 eyeOffset = eye.subtract(ray.origin());
        double along = eyeOffset.dot(direction);
        double remaining = reach * reach - eyeOffset.subtract(direction.scale(along)).lengthSqr();
        if (remaining <= 0) return new CursorRay(eye, eye);
        double half = Math.sqrt(remaining);
        double start = Math.max(0, along - half);
        double end = along + half;
        BlockHitResult block = mc.level.clip(LocalCutaway.selectionContext(ray.origin(), ray.end(), mc.player));
        if (block.getType() == HitResult.Type.BLOCK) {
            end = Math.min(end, block.getLocation().subtract(ray.origin()).dot(direction) + .001);
        }
        if (end <= start) return new CursorRay(eye, eye);
        return new CursorRay(ray.origin().add(direction.scale(start)), ray.origin().add(direction.scale(end)));
    }

    private static void pickTarget(Minecraft mc, Vec3 origin, Vec3 end, Vec3 eye, Vec3 direction, BlockHitResult block) {
        double blockDistance = block.getType() == HitResult.Type.MISS ? origin.distanceToSqr(end) : origin.distanceToSqr(block.getLocation());
        Vec3 entityEnd = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(mc.player, origin, entityEnd,
                new AABB(origin, entityEnd).inflate(1), e -> !e.isSpectator() && e.isPickable(), blockDistance);
        BlockHitResult visualBlock = OcclusionRule.resolve(mc.level, ClientOcclusion.rules(), origin, end, block);
        HitResult pointed = entity == null ? visualBlock : entity;

        Vec3 aim = pointed.getType() == HitResult.Type.MISS
                ? origin.add(direction.scale(CameraController.DISTANCE - .05)) : pointed.getLocation();
        double reach = entity == null ? mc.player.blockInteractionRange() : mc.player.entityInteractionRange();
        boolean reachable = pointed.getType() != HitResult.Type.MISS && eye.distanceToSqr(pointed.getLocation()) <= reach * reach;
        if (reachable) {
            // The screen ray must not grant interaction through a wall next to the player.
            ClipContext context = new ClipContext(eye, pointed.getLocation(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player) {
                @Override public net.minecraft.world.phys.shapes.VoxelShape getBlockShape(
                        net.minecraft.world.level.block.state.BlockState state,
                        net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
                    return entity == null && visualBlock != block && pos.equals(block.getBlockPos())
                            ? net.minecraft.world.phys.shapes.Shapes.empty() : super.getBlockShape(state, level, pos);
                }
            };
            BlockHitResult obstruction = mc.level.clip(context);
            reachable = obstruction.getType() == HitResult.Type.MISS
                    || (entity == null && obstruction.getBlockPos().equals(visualBlock.getBlockPos()));
        }
        mc.hitResult = reachable ? pointed : BlockHitResult.miss(aim, Direction.getNearest(direction.x, direction.y, direction.z), net.minecraft.core.BlockPos.containing(aim));
        mc.crosshairPickEntity = reachable && entity != null ? entity.getEntity() : null;
    }

    public static void movement(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player || !CameraController.renderingIsometric()) return;
        Input input = event.getInput();
        if (CameraController.transitioning() && mc.screen == null && mc.isWindowActive()) {
            var world = ControlMath.worldMovement(CameraController.renderedYaw(), input.forwardImpulse, input.leftImpulse);
            var forward = ControlMath.worldMovement(mc.player.getYRot(), 1, 0);
            var left = ControlMath.worldMovement(mc.player.getYRot(), 0, 1);
            input.forwardImpulse = (float) (world.x() * forward.x() + world.z() * forward.z());
            input.leftImpulse = (float) (world.x() * left.x() + world.z() * left.z());
            return;
        }
        if (!controlling()) {
            input.forwardImpulse = input.leftImpulse = 0;
            return;
        }
        ControlMath.Horizontal world = ControlMath.worldMovement(CameraController.yaw(), input.forwardImpulse, input.leftImpulse);
        double speed = Math.hypot(world.x(), world.z());
        if (speed > 1.0E-5) {
            float desiredYaw = (float) ControlMath.facingYaw(world);
            if (mc.player.getVehicle() instanceof Boat boat && boat.getControllingPassenger() == mc.player) {
                boat.setYRot(boat.getYRot() + net.minecraft.util.Mth.wrapDegrees(desiredYaw - boat.getYRot()));
            }
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
