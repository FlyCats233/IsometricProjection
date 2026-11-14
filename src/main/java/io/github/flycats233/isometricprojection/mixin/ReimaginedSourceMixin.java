package io.github.flycats233.isometricprojection.mixin;

import java.util.Optional;
import io.github.flycats233.isometricprojection.ReimaginedCompat;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="net.irisshaders.iris.shaderpack.programs.ProgramSource",remap=false)
abstract class ReimaginedSourceMixin {
    @Shadow public abstract String getName();
    @Inject(method="getVertexSource",at=@At("RETURN"),cancellable=true,require=0)
    private void isometricprojection$vertex(CallbackInfoReturnable<Optional<String>> cir) {
        cir.setReturnValue(cir.getReturnValue().map(s -> ReimaginedCompat.vertex(s,getName())));
    }
    @Inject(method="getFragmentSource",at=@At("RETURN"),cancellable=true,require=0)
    private void isometricprojection$fragment(CallbackInfoReturnable<Optional<String>> cir) {
        cir.setReturnValue(cir.getReturnValue().map(s -> ReimaginedCompat.fragment(s,getName())));
    }
}
