package io.github.flycats233.isometricprojection.mixin;

import io.github.flycats233.isometricprojection.IllusionWalking;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void isometricprojection$confirmCrossing(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        IllusionWalking.onTeleportApplied();
    }
}
