package io.github.flycats233.isometricprojection.mixin;

import io.github.flycats233.isometricprojection.CameraController;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow @Final private SectionOcclusionGraph sectionOcclusionGraph;
    @Unique private boolean isometricprojection$previousMode;

    @Inject(method = "setupRender", at = @At("HEAD"))
    private void isometricprojection$refreshVisibility(CallbackInfo ci) {
        boolean active = CameraController.active();
        if (active != isometricprojection$previousMode) {
            sectionOcclusionGraph.invalidate();
            isometricprojection$previousMode = active;
        }
    }

    @ModifyArg(method = "setupRender", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SectionOcclusionGraph;update(ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Ljava/util/List;)V"), index = 0)
    private boolean isometricprojection$orthographicVisibility(boolean smartCull) {
        // Portal traversal from a single eye point is not valid for parallel rays.
        // Keep the existing frustum test, but visit sections through opaque faces.
        return !CameraController.active() && smartCull;
    }
}
