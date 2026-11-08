package io.github.flycats233.isometricprojection.mixin;

import io.github.flycats233.isometricprojection.LocalCutaway;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderChunkRegion.class)
public abstract class RenderChunkRegionMixin {
    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    private void isometricprojection$meshAir(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        // The real ClientLevel is untouched. Neighbour face culling also sees this virtual air.
        if (LocalCutaway.hidesDuringCompile(pos)) cir.setReturnValue(Blocks.AIR.defaultBlockState());
    }

    @Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true)
    private void isometricprojection$meshEmptyFluid(BlockPos pos, CallbackInfoReturnable<FluidState> cir) {
        if (LocalCutaway.hidesDuringCompile(pos)) cir.setReturnValue(Fluids.EMPTY.defaultFluidState());
    }
}
