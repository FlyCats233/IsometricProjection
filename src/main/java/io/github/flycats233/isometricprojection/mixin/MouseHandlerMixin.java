package io.github.flycats233.isometricprojection.mixin;

import io.github.flycats233.isometricprojection.CameraController;
import io.github.flycats233.isometricprojection.IsometricControls;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void isometricprojection$freeAim(double delta, CallbackInfo ci) {
        if (CameraController.active()) {
            if (IsometricControls.fineCursorCaptured()) {
                CameraController.fineRotate(((MouseHandler) (Object) this).getXVelocity());
            }
            ci.cancel();
        }
    }

    @Inject(method = "grabMouse", at = @At("RETURN"))
    private void isometricprojection$showPointer(CallbackInfo ci) {
        if (IsometricControls.controlling() && !CameraController.fineRotationHeld()) IsometricControls.freeCursor();
    }
}
