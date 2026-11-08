package io.github.flycats233.isometricprojection.mixin;

import io.github.flycats233.isometricprojection.CameraController;
import io.github.flycats233.isometricprojection.IsometricControls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Unique private boolean isometricprojection$worldPass;

    @Inject(method = "pick(F)V", at = @At("HEAD"), cancellable = true)
    private void isometricprojection$mousePick(float partialTick, CallbackInfo ci) {
        if (IsometricControls.controlling()) {
            IsometricControls.pick(partialTick);
            ci.cancel();
        }
    }

    @Inject(method = {"bobHurt", "bobView"}, at = @At("HEAD"), cancellable = true)
    private void isometricprojection$stableCamera(CallbackInfo ci) {
        if (CameraController.active()) ci.cancel();
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void isometricprojection$begin(CallbackInfo ci) {
        // Use one sampled angle for camera setup, mouse picking and this rendered frame.
        CameraController.advanceCamera();
        isometricprojection$worldPass = true;
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void isometricprojection$end(CallbackInfo ci) {
        isometricprojection$worldPass = false;
    }

    @Inject(method = "getProjectionMatrix", at = @At("HEAD"), cancellable = true)
    private void isometricprojection$projection(double fov, CallbackInfoReturnable<Matrix4f> cir) {
        if (!isometricprojection$worldPass || !CameraController.active()) return;
        Minecraft mc = Minecraft.getInstance();
        float halfHeight = CameraController.span() / 2;
        float halfWidth = halfHeight * mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
        float far = Math.max(256, mc.options.getEffectiveRenderDistance() * 32F);
        cir.setReturnValue(new Matrix4f().setOrtho(-halfWidth, halfWidth, -halfHeight, halfHeight, 0.05F, far)
                .translate(0, 0, -CameraController.DISTANCE));
    }
}
