package io.github.flycats233.isometricprojection.mixin;

import io.github.flycats233.isometricprojection.CameraController;
import io.github.flycats233.isometricprojection.SodiumBatchState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gl.device.MultiDrawBatch", remap = false)
abstract class SodiumBatchMixin implements SodiumBatchState {
    @Shadow public boolean isFilled;
    @Unique private boolean isometricprojection$parallel;

    @Override public void isometricprojection$refreshProjection() {
        boolean parallel = CameraController.renderingIsometric();
        if (parallel != isometricprojection$parallel) {
            isFilled = false;
            isometricprojection$parallel = parallel;
        }
    }
}
