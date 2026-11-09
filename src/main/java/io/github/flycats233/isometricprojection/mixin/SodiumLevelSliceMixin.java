package io.github.flycats233.isometricprojection.mixin;

import io.github.flycats233.isometricprojection.CutawayVolume;
import io.github.flycats233.isometricprojection.LocalCutaway;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
abstract class SodiumLevelSliceMixin {
    @Unique private CutawayVolume isometricprojection$cutaway = CutawayVolume.DISABLED;

    @Inject(method = "copyData", at = @At("HEAD"), require = 0)
    private void isometricprojection$captureCutaway(CallbackInfo ci) {
        // One snapshot per mesh build, including its neighbour and fluid lookups.
        isometricprojection$cutaway = LocalCutaway.snapshot();
    }

    @Inject(method = "getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void isometricprojection$meshAir(int x, int y, int z, CallbackInfoReturnable<BlockState> cir) {
        if (isometricprojection$cutaway.hides(x, y, z)) cir.setReturnValue(Blocks.AIR.defaultBlockState());
    }
}
