package io.github.flycats233.isometricprojection;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class StatusHud {
    private StatusHud() { }

    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!CameraController.active() || mc.options.hideGui || mc.screen != null || mc.getOverlay() != null) return;
        Component illusion = mc.getSingleplayerServer() == null
                ? Component.translatable("hud.isometricprojection.singleplayer").withStyle(ChatFormatting.GRAY)
                : state(IllusionWalking.enabled());
        Component cutaway = IllusionWalking.enabled()
                ? Component.translatable("hud.isometricprojection.locked_off").withStyle(ChatFormatting.GOLD)
                : state(LocalCutaway.isEnabled());
        Component[] lines = {
                row("toggle", "isometric", state(true)),
                row("illusion", "illusion", illusion),
                row("cutaway", "cutaway", cutaway)
        };
        int width = 0;
        for (Component line : lines) width = Math.max(width, mc.font.width(line));
        int lineHeight = mc.font.lineHeight + 3;
        int panelHeight = lines.length * lineHeight + 9;
        int left = Math.max(4, mc.getWindow().getGuiScaledWidth() - width - 20);
        // Bottom-right, above the hotbar so the panel does not cover inventory slots.
        int top = Math.max(4, mc.getWindow().getGuiScaledHeight() - 40 - panelHeight);
        GuiGraphics gui = event.getGuiGraphics();
        gui.fill(left, top, left + width + 12, top + panelHeight, 0x99000000);
        for (int i = 0; i < lines.length; i++)
            gui.drawString(mc.font, lines[i], left + 6, top + 6 + i * lineHeight, 0xFFFFFFFF, true);
    }

    private static Component row(String key, String label, Component state) {
        return Component.translatable("hud.isometricprojection.row", Component.keybind("key.isometricprojection." + key),
                Component.translatable("hud.isometricprojection." + label), state);
    }

    private static Component state(boolean enabled) {
        return Component.translatable(enabled ? "hud.isometricprojection.on" : "hud.isometricprojection.off")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
    }
}
