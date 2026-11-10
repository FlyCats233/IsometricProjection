package io.github.flycats233.isometricprojection;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
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
        if (!ClientConfig.SHOW_GUIDE.get()) return;
        mc.gui.getChat().addMessage(Component.translatable("message.isometricprojection.guide_controls",
                key("toggle"), key("fine_rotation"))
                .withStyle(ChatFormatting.GOLD));
        mc.gui.getChat().addMessage(Component.translatable("message.isometricprojection.guide_movement",
                Component.keybind(mc.options.keyUp.getName()), Component.keybind(mc.options.keyLeft.getName()),
                Component.keybind(mc.options.keyDown.getName()), Component.keybind(mc.options.keyRight.getName())));
        mc.gui.getChat().addMessage(Component.translatable("message.isometricprojection.guide_illusion")
                .withStyle(ChatFormatting.AQUA));
        if (PonderCommand.available()) {
            mc.gui.getChat().addMessage(Component.translatable("message.isometricprojection.guide_ponder")
                    .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/isometricprojection ponder"))));
        }
    }

    private static Component key(String action) { return Component.keybind("key.isometricprojection." + action); }
}
