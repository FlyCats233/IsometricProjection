package io.github.flycats233.isometricprojection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;

public final class StatusHud {
    private static final int ROW_HEIGHT = 15;
    private static final int ROTATION = 0, ZOOM = 1, CUTAWAY = 2, ILLUSION = 3, AUTO_ZOOM = 4, CONFIG = 5;
    private static int dragging = -1;

    private StatusHud() {}

    public static void render(RenderGuiEvent.Post event) {
        if (!visible()) {
            dragging = -1;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Panel panel = layout(mc);
        if (dragging >= 0) updateSlider(panel, mc);
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
            } else if (row <= ZOOM) {
                Component action = action(row);
                gui.drawString(mc.font, action, right - mc.font.width(action), top + 3, color, true);
                double fraction = row == ROTATION ? CameraController.rotationFraction() : CameraController.zoomFraction();
                int thumb = panel.trackStart() + (int) Math.round(fraction * (panel.trackEnd() - panel.trackStart()));
                gui.fill(panel.trackStart(), top + 6, panel.trackEnd() + 1, top + 9, 0xB0505967);
                gui.fill(thumb - 2, top + 3, thumb + 3, top + 12, color);
                if (row == hovered && panel.mouseX(mc) <= panel.left() + 4 + mc.font.width(panel.labels()[row])) {
                    gui.renderTooltip(mc.font, Component.translatable(row == ROTATION
                            ? "hud.isometricprojection.rotation_hint" : "hud.isometricprojection.zoom_hint",
                            CameraController.fineRotationKey()), (int) panel.mouseX(mc), (int) panel.mouseY(mc));
                }
            } else if (showStates) {
                boolean enabled = switch (row) {
                    case CUTAWAY -> LocalCutaway.isEnabled();
                    case ILLUSION -> IllusionWalking.enabled();
                    default -> ClientConfig.AUTO_ZOOM_UNDER_COVER.get();
                };
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
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getAction() == GLFW.GLFW_RELEASE) {
            dragging = -1;
        }
        if (event.getAction() != GLFW.GLFW_PRESS || !visible()
                || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                && event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;
        Minecraft mc = Minecraft.getInstance();
        Panel panel = layout(mc);
        int row = panel.hovered(mc);
        if (row < 0) return;
        event.setCanceled(true);
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        if (row <= ZOOM) {
            double x = panel.mouseX(mc);
            if (x >= panel.trackEnd() + 8) {
                if (row == ROTATION) CameraController.resetView();
                else CameraController.resetZoom();
            } else if (x >= panel.trackStart() - 3) {
                dragging = row;
                updateSlider(panel, mc);
            }
            return;
        }
        switch (row) {
            case CUTAWAY -> CameraController.toggleCutaway();
            case ILLUSION -> IllusionWalking.toggle();
            case AUTO_ZOOM -> CameraController.toggleAutoZoom();
            case CONFIG -> ModList.get().getModContainerById("isometricprojection").ifPresent(container ->
                    mc.setScreen(IsometricProjection.settingsScreen(container, null)));
        }
    }

    public static void mouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!IsometricControls.controlling() || !Screen.hasControlDown() || event.getScrollDeltaY() == 0) return;
        event.setCanceled(true);
        CameraController.scrollZoom(event.getScrollDeltaY());
    }

    static boolean draggingRotation() { return dragging == ROTATION && visible(); }

    private static Component action(int row) {
        return Component.translatable(row == ROTATION ? "hud.isometricprojection.align" : "hud.isometricprojection.reset_zoom");
    }

    private static void updateSlider(Panel panel, Minecraft mc) {
        double fraction = Math.clamp((panel.mouseX(mc) - panel.trackStart()) / (panel.trackEnd() - panel.trackStart()), 0, 1);
        if (dragging == ROTATION) CameraController.slideRotation(fraction, Screen.hasControlDown());
        else CameraController.slideZoom(fraction);
    }

    private static boolean visible() {
        return IsometricControls.controlling() && !Minecraft.getInstance().options.hideGui
                && !CameraController.fineRotationHeld() && !IsometricControls.fineCursorCaptured();
    }

    private static Panel layout(Minecraft mc) {
        Component[] labels = {
                Component.translatable("hud.isometricprojection.rotation"),
                Component.translatable("hud.isometricprojection.zoom"),
                Component.translatable("hud.isometricprojection.cutaway"),
                Component.translatable("hud.isometricprojection.illusion"),
                Component.translatable("hud.isometricprojection.auto_zoom"),
                Component.translatable("hud.isometricprojection.config")
        };
        int labelWidth = Math.max(mc.font.width(labels[ROTATION]), mc.font.width(labels[ZOOM]));
        int actionWidth = Math.max(mc.font.width(action(ROTATION)), mc.font.width(action(ZOOM)));
        int width = labelWidth + 80 + actionWidth;
        for (Component label : labels) width = Math.max(width, mc.font.width(label) + 34);
        int left = Math.max(4, mc.getWindow().getGuiScaledWidth() - width - 8);
        int top = Math.max(4, mc.getWindow().getGuiScaledHeight() - 38 - labels.length * ROW_HEIGHT);
        return new Panel(left, top, width, left + labelWidth + 12, left + width - actionWidth - 12, labels);
    }

    private record Panel(int left, int top, int width, int trackStart, int trackEnd, Component[] labels) {
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
