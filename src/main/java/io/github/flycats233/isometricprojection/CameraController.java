package io.github.flycats233.isometricprojection;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class CameraController {
    private static final KeyMapping TOGGLE = key("toggle", GLFW.GLFW_KEY_I);
    private static final KeyMapping LEFT = key("left", GLFW.GLFW_KEY_LEFT_BRACKET);
    private static final KeyMapping RIGHT = key("right", GLFW.GLFW_KEY_RIGHT_BRACKET);
    private static final KeyMapping IN = key("zoom_in", GLFW.GLFW_KEY_EQUAL);
    private static final KeyMapping OUT = key("zoom_out", GLFW.GLFW_KEY_MINUS);
    private static final KeyMapping CUTAWAY = key("cutaway", GLFW.GLFW_KEY_V);
    private static final KeyMapping ILLUSION = key("illusion", GLFW.GLFW_KEY_G);
    private static boolean enabled;
    private static CameraType previous = CameraType.FIRST_PERSON;
    private static float yaw = 45;
    private static final SmoothRotation ROTATION = new SmoothRotation(45, 250_000_000L);
    private static float span = 24;
    private static final SmoothZoom ZOOM = new SmoothZoom(24, 8, 64, 200_000_000L);
    public static final float PITCH = 35.26439F;
    public static final float DISTANCE = 32;

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.isometricprojection." + name, InputConstants.Type.KEYSYM,
                code, "key.categories.isometricprojection");
    }
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : new KeyMapping[]{TOGGLE, LEFT, RIGHT, IN, OUT, CUTAWAY, ILLUSION}) event.register(key);
    }
    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return enabled && mc.level != null && mc.player != null;
    }
    public static float yaw() { return yaw; }
    public static boolean isRotating() { return ROTATION.isMoving(); }
    public static void advanceCamera() {
        long now = System.nanoTime();
        yaw = (float) ROTATION.sample(now);
        span = (float) ZOOM.sample(now);
    }
    public static float span() { return span; }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (enabled) mc.options.setCameraType(previous);
            enabled = false;
            yaw = (float) ROTATION.finish();
            span = (float) ZOOM.finish();
            drain();
            return;
        }
        if (mc.screen != null) { drain(); return; }
        while (TOGGLE.consumeClick()) {
            if (!enabled) previous = mc.options.getCameraType();
            enabled = !enabled;
            if (!enabled) { yaw = (float) ROTATION.finish(); span = (float) ZOOM.finish(); }
            mc.options.setCameraType(enabled ? CameraType.THIRD_PERSON_BACK : previous);
            mc.player.displayClientMessage(Component.translatable(
                    enabled ? "message.isometricprojection.on" : "message.isometricprojection.off"), true);
        }
        while (LEFT.consumeClick()) if (enabled && !IllusionWalking.busy()) ROTATION.rotateBy(-90, System.nanoTime());
        while (RIGHT.consumeClick()) if (enabled && !IllusionWalking.busy()) ROTATION.rotateBy(90, System.nanoTime());
        while (IN.consumeClick()) if (enabled) ZOOM.adjust(-2, System.nanoTime());
        while (OUT.consumeClick()) if (enabled) ZOOM.adjust(2, System.nanoTime());
        while (CUTAWAY.consumeClick()) if (enabled) {
            if (IllusionWalking.enabled()) {
                mc.player.displayClientMessage(Component.translatable("message.isometricprojection.illusion_no_cutaway"), true);
                continue;
            }
            boolean cutaway = LocalCutaway.toggle();
            mc.player.displayClientMessage(Component.translatable(cutaway
                    ? "message.isometricprojection.cutaway_on" : "message.isometricprojection.cutaway_off"), true);
        }
        while (ILLUSION.consumeClick()) IllusionWalking.toggle();
        if (enabled) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }
    private static void drain() {
        for (KeyMapping key : new KeyMapping[]{TOGGLE, LEFT, RIGHT, IN, OUT, CUTAWAY, ILLUSION})
            while (key.consumeClick()) { }
    }
}
