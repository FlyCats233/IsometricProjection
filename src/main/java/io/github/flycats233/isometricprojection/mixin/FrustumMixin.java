package io.github.flycats233.isometricprojection.mixin;

import io.github.flycats233.isometricprojection.CameraController;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public abstract class FrustumMixin {
    @Inject(method = "offsetToFullyIncludeCameraCube", at = @At("HEAD"), cancellable = true)
    private void isometricprojection$skipPerspectiveExpansion(int cubeSize, CallbackInfoReturnable<Frustum> cir) {
        // Moving parallel projection backwards cannot widen its side planes.
        // Vanilla's unbounded loop may never contain the camera cube.
        if (CameraController.active()) cir.setReturnValue((Frustum) (Object) this);
    }
}
