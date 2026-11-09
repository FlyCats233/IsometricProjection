package io.github.flycats233.isometricprojection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import org.lwjgl.glfw.GLFW;

public final class StatusHud {
    private static final int ROW_HEIGHT = 15;
    private static final int CUTAWAY = 0, ILLUSION = 1, RESET = 2, CONFIG = 3;

    private StatusHud() {}

    public static void render(RenderGuiEvent.Post event) {
        if (!visible()) return;
        Minecraft mc = Minecraft.getInstance();
        Panel panel = layout(mc);
        int hovered = panel.hovered(mc);
        GuiGraphics gui = event.getGuiGraphics();
        boolean showStates = ClientConfig.SHOW_HUD.get();
        for (int row = 0; row < panel.labels().length; row++) {
            int top = panel.top() + row * ROW_HEIGHT;
            boolean locked = row == CUTAWAY && IllusionWalking.enabled()
                    || row == ILLUSION && !IllusionWalking.serverSupported();
            int color = locked ? 0xFFB8BBC2 : row == hovered ? 0xFFD1EAFF : 0xFFF4F6F9;
            gui.drawString(mc.font, panel.labels()[row], panel.left() + 4, top + 3, color, true);
            int right = panel.left() + panel.width() - 3;
            if (row == CONFIG) {
                gui.drawString(mc.font, "›", right - mc.font.width("›"), top + 3, color, true);
            } else if (row == RESET) {
                gui.drawString(mc.font, "↺", right - mc.font.width("↺"), top + 3, color, true);
            } else if (showStates) {
                boolean enabled = row == CUTAWAY ? LocalCutaway.isEnabled() : IllusionWalking.enabled();
                drawSwitch(gui, right - 21, top + 3, enabled, locked, row == hovered);
            }
            if (locked && row == hovered && showStates) {
                gui.renderTooltip(mc.font, Component.translatable(row == CUTAWAY
                        ? "hud.isometricprojection.locked_off" : "hud.isometricprojection.singleplayer"),
                        (int) panel.mouseX(mc), (int) panel.mouseY(mc));
            }
        }
    }

    private static void drawSwitch(GuiGraphics gui, int x, int y, boolean enabled, boolean locked, boolean hovered) {
        int track = locked ? 0xB0505661 : enabled ? 0xDF32956E : 0xD0505967;
        gui.fill(x + 2, y, x + 19, y + 9, track);
        gui.fill(x, y + 2, x + 21, y + 7, track);
        gui.fill(x + 1, y + 1, x + 20, y + 8, track);
        int thumbX = x + (enabled ? 13 : 2);
        int thumb = locked ? 0xFFADB3BE : hovered ? 0xFFFFFFFF : 0xFFF1F4FA;
        gui.fill(thumbX + 1, y + 1, thumbX + 5, y + 8, thumb);
        gui.fill(thumbX, y + 2, thumbX + 6, y + 7, thumb);
    }

    public static void mouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS || !visible()
                || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                && event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;
        Minecraft mc = Minecraft.getInstance();
        int row = layout(mc).hovered(mc);
        if (row < 0) return;
        event.setCanceled(true);
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        switch (row) {
            case CUTAWAY -> CameraController.toggleCutaway();
            case ILLUSION -> IllusionWalking.toggle();
            case RESET -> CameraController.resetView();
            case CONFIG -> ModList.get().getModContainerById("isometricprojection").ifPresent(container ->
                    mc.setScreen(new ConfigurationScreen(container, null)));
        }
    }

    private static boolean visible() {
        return IsometricControls.controlling() && !Minecraft.getInstance().options.hideGui
                && !CameraController.fineRotationHeld() && !IsometricControls.fineCursorCaptured();
    }

    private static Panel layout(Minecraft mc) {
        Component[] labels = {
                Component.translatable("hud.isometricprojection.cutaway"),
                Component.translatable("hud.isometricprojection.illusion"),
                Component.translatable("hud.isometricprojection.reset_view"),
                Component.translatable("hud.isometricprojection.config")
        };
        int width = 116;
        for (Component label : labels) width = Math.max(width, mc.font.width(label) + 34);
        int left = Math.max(4, mc.getWindow().getGuiScaledWidth() - width - 8);
        int top = Math.max(4, mc.getWindow().getGuiScaledHeight() - 38 - labels.length * ROW_HEIGHT);
        return new Panel(left, top, width, labels);
    }

    private record Panel(int left, int top, int width, Component[] labels) {
        double mouseX(Minecraft mc) {
            var window = mc.getWindow();
            return mc.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
        }

        double mouseY(Minecraft mc) {
            var window = mc.getWindow();
            return mc.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
        }

        int hovered(Minecraft mc) {
            double x = mouseX(mc);
            double y = mouseY(mc);
            if (x < left || x >= left + width || y < top || y >= top + labels.length * ROW_HEIGHT) return -1;
            return (int) (y - top) / ROW_HEIGHT;
        }
    }
}
