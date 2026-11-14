package io.github.flycats233.isometricprojection;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "isometricprojection", dist = Dist.CLIENT)
public final class IsometricProjection {
    public IsometricProjection(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, IsometricProjection::settingsScreen);
        modBus.addListener(ClientConfig::changed);
        modBus.addListener(OcclusionToolAppearance::setup);
        modBus.addListener(OcclusionRenderer::shaders);
        modBus.addListener(OcclusionActors::shaders);
        NeoForge.EVENT_BUS.addListener(OcclusionActors::tick);
        NeoForge.EVENT_BUS.addListener(OcclusionActors::logout);
        OcclusionNetwork.receiver = ClientOcclusion::receive;
        OcclusionNetwork.clientRules = ClientOcclusion::rules;
        ModItems.serverItemsAvailable = ClientOcclusion::serverSupported;
        NeoForge.EVENT_BUS.addListener(ClientOcclusion::logout);
        NeoForge.EVENT_BUS.addListener(OcclusionRenderer::logout);
        NeoForge.EVENT_BUS.addListener(OcclusionRenderer::render);
        NeoForge.EVENT_BUS.addListener(ToolRelationOverlay::hud);
        IllusionNetwork.clientReceiver = IllusionWalking::onResult;
        modBus.addListener(CameraController::registerKeys);
        NeoForge.EVENT_BUS.addListener(CameraController::tick);
        NeoForge.EVENT_BUS.addListener(IsometricControls::tick);
        NeoForge.EVENT_BUS.addListener(IsometricControls::movement);
        NeoForge.EVENT_BUS.addListener(IsometricControls::interaction);
        NeoForge.EVENT_BUS.addListener(IllusionWalking::tick);
        NeoForge.EVENT_BUS.addListener(LocalCutaway::tick);
        NeoForge.EVENT_BUS.addListener(JoinGuide::login);
        NeoForge.EVENT_BUS.addListener(JoinGuide::logout);
        NeoForge.EVENT_BUS.addListener(JoinGuide::tick);
        NeoForge.EVENT_BUS.addListener(StatusHud::render);
        NeoForge.EVENT_BUS.addListener(ProjectileAim::render);
        NeoForge.EVENT_BUS.addListener(StatusHud::mouseButton);
        NeoForge.EVENT_BUS.addListener(StatusHud::mouseScroll);
        PonderCommand.register();
    }

    static Screen settingsScreen(ModContainer container, Screen parent) {
        return new ConfigurationScreen(container, parent,
                (context, key, original) -> key.equals("autoZoomUnderCover") ? null : original);
    }
}
