package io.github.flycats233.isometricprojection;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only preferences, editable from the NeoForge Mods configuration screen. */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CUTAWAY_ENABLED, AUTO_ZOOM_UNDER_COVER, SHOW_HUD, SHOW_GUIDE;
    public static final ModConfigSpec.DoubleValue CUTAWAY_RADIUS, CUTAWAY_DEPTH, DEFAULT_SPAN, ZOOM_STEP;
    public static final ModConfigSpec.IntValue ROTATION_MILLIS, ZOOM_MILLIS;
    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        CUTAWAY_ENABLED = b.translation("config.isometricprojection.cutawayEnabled").comment("Default/persisted cutaway preference. Illusion connections temporarily disable it.").define("cutawayEnabled", true);
        CUTAWAY_RADIUS = b.translation("config.isometricprojection.cutawayRadius").comment("Opening radius in projected world units.").defineInRange("cutawayRadius", 4D, 1D, 12D);
        CUTAWAY_DEPTH = b.translation("config.isometricprojection.cutawayDepth").comment("Maximum opening depth towards the camera, in blocks.").defineInRange("cutawayDepth", 32D, 4D, 32D);
        DEFAULT_SPAN = b.translation("config.isometricprojection.defaultSpan").comment("Default vertical visible world size. Applied when configuration is loaded/saved.").defineInRange("defaultSpan", 24D, 8D, 64D);
        ZOOM_STEP = b.translation("config.isometricprojection.zoomStep").defineInRange("zoomStep", 2D, .5D, 8D);
        AUTO_ZOOM_UNDER_COVER = b.translation("config.isometricprojection.autoZoomUnderCover").define("autoZoomUnderCover", false);
        ROTATION_MILLIS = b.translation("config.isometricprojection.rotationMillis").defineInRange("rotationMillis", 250, 1, 1000);
        ZOOM_MILLIS = b.translation("config.isometricprojection.zoomMillis").defineInRange("zoomMillis", 200, 1, 1000);
        SHOW_HUD = b.translation("config.isometricprojection.showHud").define("showHud", true);
        SHOW_GUIDE = b.translation("config.isometricprojection.showGuide").define("showGuide", true);
        SPEC = b.build();
    }

    private ClientConfig() { }
    public static void changed(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC || event instanceof ModConfigEvent.Unloading) return;
        net.minecraft.client.Minecraft.getInstance().execute(() -> {
            LocalCutaway.setEnabled(CUTAWAY_ENABLED.get() && !IllusionWalking.enabled());
            CameraController.configure();
        });
    }
}
