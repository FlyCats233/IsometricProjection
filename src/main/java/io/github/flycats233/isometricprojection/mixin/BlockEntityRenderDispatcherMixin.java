package io.github.flycats233.isometricprojection.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.flycats233.isometricprojection.LocalCutaway;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V", at = @At("HEAD"), cancellable = true)
    private void isometricprojection$hideUpperFloor(BlockEntity entity, float partialTick, PoseStack pose,
            MultiBufferSource buffers, CallbackInfo ci) {
        if (LocalCutaway.hides(entity.getBlockPos())) ci.cancel();
    }
}
