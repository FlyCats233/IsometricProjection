package io.github.flycats233.isometricprojection.mixin;

import io.github.flycats233.isometricprojection.SodiumBatchState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion", remap = false)
abstract class SodiumRegionBatchMixin {
    @Inject(method = "getCachedBatch", at = @At("RETURN"), require = 0)
    private void isometricprojection$refreshBatch(CallbackInfoReturnable<Object> cir) {
        if (cir.getReturnValue() instanceof SodiumBatchState batch) batch.isometricprojection$refreshProjection();
    }
}
