package io.github.flycats233.isometricprojection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class OcclusionNetwork {
    public static Consumer<Snapshot> receiver = snapshot -> {};
    public static Supplier<List<OcclusionRule>> clientRules = List::of;
    private OcclusionNetwork() {}
    public record Snapshot(ResourceLocation dimension, List<OcclusionRule> rules) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("isometricprojection", "occlusion_rules"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = StreamCodec.of((buffer, value) -> {
            buffer.writeResourceLocation(value.dimension);
            buffer.writeVarInt(value.rules.size());
            for (var rule : value.rules) {
                buffer.writeLong(rule.id());
                buffer.writeBlockPos(rule.hidden()); buffer.writeEnum(rule.hiddenFace());
                buffer.writeBlockPos(rule.covering()); buffer.writeEnum(rule.coveringFace());
            }
        }, buffer -> {
            var dimension = buffer.readResourceLocation();
            int count = buffer.readVarInt();
            if (count < 0 || count > 128) throw new IllegalArgumentException("Invalid occlusion rule count");
            List<OcclusionRule> rules = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                rules.add(new OcclusionRule(buffer.readLong(), buffer.readBlockPos(), buffer.readEnum(Direction.class),
                        buffer.readBlockPos(), buffer.readEnum(Direction.class)));
            }
            return new Snapshot(dimension, List.copyOf(rules));
        });
        @Override public Type<Snapshot> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Snapshot.TYPE, Snapshot.CODEC,
                (payload, context) -> context.enqueueWork(() -> receiver.accept(payload)));
    }
    public static void send(ServerPlayer player) {
        if (player.connection.hasChannel(Snapshot.TYPE)) {
            player.connection.send(new Snapshot(player.serverLevel().dimension().location(), OcclusionData.get(player.serverLevel()).rules()));
        }
    }
    public static void broadcast(ServerLevel level) {
        for (var player : level.players()) send(player);
    }
}
