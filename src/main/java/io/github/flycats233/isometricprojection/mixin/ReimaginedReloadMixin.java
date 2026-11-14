package io.github.flycats233.isometricprojection.mixin;

import io.github.flycats233.isometricprojection.ReimaginedCompat;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="net.irisshaders.iris.Iris",remap=false)
abstract class ReimaginedReloadMixin {
    @Inject(method="loadShaderpack",at=@At("HEAD"),require=0)
    private static void isometricprojection$reset(CallbackInfo ci) {ReimaginedCompat.reset();}
}
