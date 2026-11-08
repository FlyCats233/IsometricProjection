package io.github.flycats233.isometricprojection;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Common entry point: no client classes are loaded on a dedicated server. */
@Mod("isometricprojection")
public final class NetworkMod {
    public NetworkMod(IEventBus bus) { bus.addListener(IllusionNetwork::register); }
}
