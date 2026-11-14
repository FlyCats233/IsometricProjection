package io.github.flycats233.isometricprojection.mixin;

import io.github.flycats233.isometricprojection.CameraController;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager", remap = false)
abstract class SodiumSectionVisibilityMixin {
    @Inject(method = "shouldUseOcclusionCulling", at = @At("HEAD"), cancellable = true, require = 0)
    private void isometricprojection$parallelVisibility(Camera camera, boolean spectator,
            CallbackInfoReturnable<Boolean> cir) {
        // Traversal from the player's eye can discard sections seen by parallel camera rays.
        if (CameraController.renderingIsometric()) cir.setReturnValue(false);
    }
}
