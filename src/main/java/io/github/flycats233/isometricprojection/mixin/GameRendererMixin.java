package io.github.flycats233.isometricprojection.mixin;

import io.github.flycats233.isometricprojection.CameraController;
import io.github.flycats233.isometricprojection.IsometricControls;
import io.github.flycats233.isometricprojection.ViewTransition;
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
        if (CameraController.transitioning()) {
            Minecraft mc = Minecraft.getInstance();
            mc.hitResult = null;
            mc.crosshairPickEntity = null;
            ci.cancel();
            return;
        }
        if (IsometricControls.controlling()) {
            IsometricControls.pick(partialTick);
            ci.cancel();
        }
    }

    @Inject(method = {"bobHurt", "bobView"}, at = @At("HEAD"), cancellable = true)
    private void isometricprojection$stableCamera(CallbackInfo ci) {
        if (CameraController.renderingIsometric()) ci.cancel();
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void isometricprojection$hideFirstPersonHand(CallbackInfo ci) {
        if (CameraController.viewProgress() > .08f) ci.cancel();
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

    @Inject(method = "getProjectionMatrix", at = @At("RETURN"), cancellable = true)
    private void isometricprojection$projection(double fov, CallbackInfoReturnable<Matrix4f> cir) {
        if (!isometricprojection$worldPass || CameraController.viewProgress() <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        float halfHeight = CameraController.span() / 2;
        float halfWidth = halfHeight * mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
        float far = Math.max(256, mc.options.getEffectiveRenderDistance() * 32F);
        cir.setReturnValue(ViewTransition.projection(cir.getReturnValue(), CameraController.viewProgress(),
                halfWidth, halfHeight, far, CameraController.DISTANCE));
    }
}
