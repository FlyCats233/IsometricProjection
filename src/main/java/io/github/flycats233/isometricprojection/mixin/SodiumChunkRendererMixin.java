package io.github.flycats233.isometricprojection.mixin;

import io.github.flycats233.isometricprojection.CameraController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer", remap = false)
abstract class SodiumChunkRendererMixin {
    // Work on the actual command-building argument, after Iris redirects the option read.
    @ModifyVariable(method = "fillCommandBuffer", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private static boolean isometricprojection$parallelFaces(boolean enabled) {
        return enabled && !CameraController.renderingIsometric();
    }
}
