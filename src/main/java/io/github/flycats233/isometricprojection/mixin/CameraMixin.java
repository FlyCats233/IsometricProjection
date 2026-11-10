package io.github.flycats233.isometricprojection.mixin;

import io.github.flycats233.isometricprojection.CameraController;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch, float roll);
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow private boolean detached;
    @Shadow public abstract float getYRot();
    @Shadow public abstract float getXRot();
    @Shadow public abstract Vec3 getPosition();

    @Inject(method = "setup", at = @At("TAIL"))
    private void isometricprojection$setup(BlockGetter level, Entity entity, boolean vanillaDetached,
                                           boolean mirrored, float partialTick, CallbackInfo ci) {
        float progress = CameraController.viewProgress();
        if (progress <= 0) {
            CameraController.renderedYaw(getYRot());
            return;
        }
        float yaw = progress >= 1 ? CameraController.yaw()
                : Mth.rotLerp(progress, getYRot(), CameraController.yaw());
        setRotation(yaw, Mth.lerp(progress, getXRot(), CameraController.PITCH), 0);
        CameraController.renderedYaw(yaw);
        Vec3 position = getPosition().lerp(new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getEyeHeight(),
                Mth.lerp(partialTick, entity.zo, entity.getZ())), progress);
        setPosition(position.x, position.y, position.z);
        if (progress > .08f) detached = true;
        // Keep audio, fog and entity distance calculations anchored to the player.
        // The visual standoff is applied in the world projection matrix instead.
    }
}
