package io.github.flycats233.isometricprojection.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.flycats233.isometricprojection.CameraController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer", remap = false)
abstract class SodiumChunkRendererMixin {
    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD",
            target = "Lnet/caffeinemc/mods/sodium/client/gui/SodiumOptions$PerformanceSettings;useBlockFaceCulling:Z"),
            require = 0)
    private boolean isometricprojection$keepCameraFacingTerrain(boolean enabled) {
        // Sodium compares face groups to the player's eye position before submitting them.
        // Parallel projection has no single eye point, so let rasterization cull those faces.
        return enabled && !CameraController.renderingIsometric();
    }
}
