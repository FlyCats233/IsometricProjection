package io.github.flycats233.isometricprojection;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "isometricprojection", dist = Dist.CLIENT)
public final class IsometricProjection {
    public IsometricProjection(IEventBus modBus) {
        modBus.addListener(CameraController::registerKeys);
        NeoForge.EVENT_BUS.addListener(CameraController::tick);
        NeoForge.EVENT_BUS.addListener(IsometricControls::tick);
        NeoForge.EVENT_BUS.addListener(IsometricControls::movement);
        NeoForge.EVENT_BUS.addListener(IllusionWalking::tick);
        NeoForge.EVENT_BUS.addListener(LocalCutaway::tick);
        NeoForge.EVENT_BUS.addListener(JoinGuide::login);
        NeoForge.EVENT_BUS.addListener(JoinGuide::logout);
        NeoForge.EVENT_BUS.addListener(JoinGuide::tick);
        NeoForge.EVENT_BUS.addListener(StatusHud::render);
    }
}
