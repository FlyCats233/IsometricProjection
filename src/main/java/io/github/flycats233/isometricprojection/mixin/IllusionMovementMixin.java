package io.github.flycats233.isometricprojection.mixin;

import io.github.flycats233.isometricprojection.IllusionWalking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class IllusionMovementMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void isometricprojection$sweepMove(MoverType type, Vec3 movement, CallbackInfo ci) {
        if (type == MoverType.SELF && IllusionWalking.beforeMove(movement)) ci.cancel();
    }
}
