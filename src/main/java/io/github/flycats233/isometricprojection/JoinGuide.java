package io.github.flycats233.isometricprojection;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** One local chat guide per login, after the world loading screen has closed. */
public final class JoinGuide {
    private static Connection connection;
    private static boolean pending;

    private JoinGuide() { }

    public static void login(ClientPlayerNetworkEvent.LoggingIn event) {
        if (connection == event.getConnection()) return;
        connection = event.getConnection();
        pending = true;
    }

    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        connection = null;
        pending = false;
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!pending || mc.player == null || mc.level == null || mc.screen != null || mc.getOverlay() != null) return;
        pending = false;
        mc.gui.getChat().addMessage(Component.translatable("message.isometricprojection.guide_controls",
                key("toggle"), key("left"), key("right"), key("zoom_in"), key("zoom_out"), key("cutaway"))
                .withStyle(ChatFormatting.GOLD));
        mc.gui.getChat().addMessage(Component.translatable("message.isometricprojection.guide_movement",
                Component.keybind(mc.options.keyUp.getName()), Component.keybind(mc.options.keyLeft.getName()),
                Component.keybind(mc.options.keyDown.getName()), Component.keybind(mc.options.keyRight.getName()),
                Component.keybind(mc.options.keyJump.getName())));
        mc.gui.getChat().addMessage(Component.translatable("message.isometricprojection.guide_illusion", key("illusion"))
                .withStyle(ChatFormatting.AQUA));
    }

    private static Component key(String action) { return Component.keybind("key.isometricprojection." + action); }
}
