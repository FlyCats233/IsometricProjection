package io.github.flycats233.isometricprojection;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public final class ClientOcclusion {
    private static ResourceLocation dimension;
    private static List<OcclusionRule> rules = List.of();
    private ClientOcclusion() {}
    public static boolean serverSupported() {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(OcclusionNetwork.Snapshot.TYPE);
    }
    public static void receive(OcclusionNetwork.Snapshot snapshot) {
        dimension = snapshot.dimension();
        rules = snapshot.rules();
    }
    public static List<OcclusionRule> rules() {
        var level = Minecraft.getInstance().level;
        return level != null && level.dimension().location().equals(dimension) ? rules : List.of();
    }
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        dimension = null;
        rules = List.of();
    }
}
