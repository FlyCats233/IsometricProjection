package io.github.flycats233.isometricprojection;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;

/** Common entry point: no client classes are loaded on a dedicated server. */
@Mod("isometricprojection")
public final class NetworkMod {
    public NetworkMod(IEventBus bus) {
        bus.addListener(IllusionNetwork::register);
        bus.addListener(OcclusionNetwork::register);
        ModItems.register(bus);
        NeoForge.EVENT_BUS.addListener(NetworkMod::login);
        NeoForge.EVENT_BUS.addListener(NetworkMod::dimensionChanged);
    }

    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) OcclusionNetwork.send(player);
    }
    private static void dimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) OcclusionNetwork.send(player);
    }
}
