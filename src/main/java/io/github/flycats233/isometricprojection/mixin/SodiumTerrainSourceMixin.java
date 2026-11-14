package io.github.flycats233.isometricprojection.mixin;

import io.github.flycats233.isometricprojection.SodiumTerrainCompat;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader",remap=false)
abstract class SodiumTerrainSourceMixin {
    @Inject(method="getShaderSource",at=@At("RETURN"),cancellable=true,require=0)
    private static void isometricprojection$source(ResourceLocation id, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(SodiumTerrainCompat.patch(id,cir.getReturnValue()));
    }
}
