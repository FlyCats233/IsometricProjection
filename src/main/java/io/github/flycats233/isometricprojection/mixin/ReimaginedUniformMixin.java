package io.github.flycats233.isometricprojection.mixin;

import io.github.flycats233.isometricprojection.ReimaginedCompat;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="net.irisshaders.iris.gl.program.ProgramUniforms",remap=false)
abstract class ReimaginedUniformMixin {
    @Inject(method="update",at=@At("RETURN"),require=0)
    private void isometricprojection$uniforms(CallbackInfo ci) { ReimaginedCompat.uniforms(); }
}
