package io.github.flycats233.isometricprojection;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class CameraController {
    private static final KeyMapping TOGGLE = key("toggle", GLFW.GLFW_KEY_I);
    private static final KeyMapping LEFT = key("left", GLFW.GLFW_KEY_LEFT_BRACKET);
    private static final KeyMapping RIGHT = key("right", GLFW.GLFW_KEY_RIGHT_BRACKET);
    private static final KeyMapping IN = key("zoom_in", GLFW.GLFW_KEY_EQUAL);
    private static final KeyMapping OUT = key("zoom_out", GLFW.GLFW_KEY_MINUS);
    private static final KeyMapping FINE_ROTATION = new KeyMapping("key.isometricprojection.fine_rotation",
            InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_4, "key.categories.isometricprojection");
    private static boolean enabled;
    private static CameraType previous = CameraType.FIRST_PERSON;
    private static float yaw = 45;
    private static SmoothRotation ROTATION = new SmoothRotation(45, 250_000_000L);
    private static float span = 24;
    private static SmoothZoom ZOOM = new SmoothZoom(24, 8, 64, 200_000_000L);
    private static int configuredRotationMillis = 250, configuredZoomMillis = 200;
    private static double configuredDefaultSpan = 24;
    public static final float PITCH = 35.26439F;
    public static final float DISTANCE = 32;
    private static final double OVERHEAD_CHECK_DISTANCE = 4;

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.isometricprojection." + name, InputConstants.Type.KEYSYM,
                code, "key.categories.isometricprojection");
    }
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : new KeyMapping[]{TOGGLE, LEFT, RIGHT, IN, OUT, FINE_ROTATION}) event.register(key);
    }
    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return enabled && mc.level != null && mc.player != null;
    }
    public static float yaw() { return yaw; }
    public static boolean isRotating() { return ROTATION.isMoving() || fineRotationHeld(); }
    public static boolean fineRotationHeld() {
        return FINE_ROTATION.isDown() && IsometricControls.controlling() && !IllusionWalking.busy();
    }

    public static void fineRotate(double mouseDelta) {
        if (mouseDelta != 0 && fineRotationHeld()) {
            yaw = (float) ROTATION.moveImmediately(mouseDelta * .15, System.nanoTime());
        }
    }

    static void resetView() {
        if (!IllusionWalking.busy()) ROTATION.alignToNearest(System.nanoTime());
    }
    public static void advanceCamera() {
        long now = System.nanoTime();
        yaw = (float) ROTATION.sample(now);
        span = (float) ZOOM.sample(now);
    }
    public static float span() { return span; }
    public static void configure() {
        int rotationMillis = ClientConfig.ROTATION_MILLIS.get(), zoomMillis = ClientConfig.ZOOM_MILLIS.get();
        double defaultSpan = ClientConfig.DEFAULT_SPAN.get();
        if (rotationMillis != configuredRotationMillis) {
            yaw = (float) ROTATION.finish();
            ROTATION = new SmoothRotation(yaw, rotationMillis * 1_000_000L);
        }
        if (defaultSpan != configuredDefaultSpan || zoomMillis != configuredZoomMillis) {
            span = (float)(defaultSpan != configuredDefaultSpan ? defaultSpan : ZOOM.manualTarget());
            ZOOM = new SmoothZoom(span, 8, 64, zoomMillis * 1_000_000L);
        }
        configuredRotationMillis = rotationMillis;
        configuredZoomMillis = zoomMillis;
        configuredDefaultSpan = defaultSpan;
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (enabled) mc.options.setCameraType(previous);
            enabled = false;
            yaw = (float) ROTATION.finish();
            ZOOM.setCovered(false, System.nanoTime());
            span = (float) ZOOM.finish();
            drain();
            return;
        }
        if (mc.screen != null) { drain(); return; }
        while (TOGGLE.consumeClick()) {
            if (!enabled) previous = mc.options.getCameraType();
            enabled = !enabled;
            if (!enabled) {
                yaw = (float) ROTATION.finish();
                ZOOM.setCovered(false, System.nanoTime());
                span = (float) ZOOM.finish();
            }
            mc.options.setCameraType(enabled ? CameraType.THIRD_PERSON_BACK : previous);
            mc.player.displayClientMessage(Component.translatable(
                    enabled ? "message.isometricprojection.on" : "message.isometricprojection.off"), true);
        }
        while (LEFT.consumeClick()) if (enabled && !fineRotationHeld() && !IllusionWalking.busy()) ROTATION.rotateBy(-90, System.nanoTime());
        while (RIGHT.consumeClick()) if (enabled && !fineRotationHeld() && !IllusionWalking.busy()) ROTATION.rotateBy(90, System.nanoTime());
        while (FINE_ROTATION.consumeClick()) { }
        ZOOM.setCovered(enabled && ClientConfig.AUTO_ZOOM_UNDER_COVER.get() && hasOverheadCover(mc), System.nanoTime());
        while (IN.consumeClick()) if (enabled) ZOOM.adjust(-ClientConfig.ZOOM_STEP.get(), System.nanoTime());
        while (OUT.consumeClick()) if (enabled) ZOOM.adjust(ClientConfig.ZOOM_STEP.get(), System.nanoTime());
        if (enabled) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    static void toggleCutaway() {
        var player = Minecraft.getInstance().player;
        if (player == null || !active()) return;
        if (IllusionWalking.enabled()) {
            player.displayClientMessage(Component.translatable("message.isometricprojection.illusion_no_cutaway"), true);
            return;
        }
        player.displayClientMessage(Component.translatable(LocalCutaway.toggle()
                ? "message.isometricprojection.cutaway_on" : "message.isometricprojection.cutaway_off"), true);
    }

    private static boolean hasOverheadCover(Minecraft mc) {
        var eye = mc.player.getEyePosition();
        return mc.level.clip(new ClipContext(eye, eye.add(0, OVERHEAD_CHECK_DISTANCE, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)).getType() == HitResult.Type.BLOCK;
    }
    private static void drain() {
        for (KeyMapping key : new KeyMapping[]{TOGGLE, LEFT, RIGHT, IN, OUT, FINE_ROTATION})
            while (key.consumeClick()) { }
    }
}
