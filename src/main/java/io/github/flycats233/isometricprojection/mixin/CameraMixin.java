package io.github.flycats233.isometricprojection.mixin;

import io.github.flycats233.isometricprojection.CameraController;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch, float roll);
    @Shadow protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setup", at = @At("TAIL"))
    private void isometricprojection$setup(BlockGetter level, Entity entity, boolean detached,
                                           boolean mirrored, float partialTick, CallbackInfo ci) {
        if (!CameraController.active()) return;
        setRotation(CameraController.yaw(), CameraController.PITCH, 0);
        setPosition(Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getEyeHeight(),
                Mth.lerp(partialTick, entity.zo, entity.getZ()));
        // Keep audio, fog and entity distance calculations anchored to the player.
        // The visual standoff is applied in the world projection matrix instead.
    }
}
