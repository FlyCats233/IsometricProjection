package io.github.flycats233.isometricprojection.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "dev.engine_room.flywheel.backend.glsl.ShaderSources$SourceFinder", remap = false)
abstract class FlywheelShaderSourcesMixin {
    @WrapOperation(method = "readResource", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/packs/resources/Resource;open()Ljava/io/InputStream;"), require = 0)
    private InputStream isometricprojection$skipPerspectiveOcclusion(Resource resource,
            Operation<InputStream> original, ResourceLocation shader) throws IOException {
        InputStream input = original.call(resource);
        if (!shader.equals(ResourceLocation.fromNamespaceAndPath("flywheel", "internal/indirect/cull.glsl"))) {
            return input;
        }
        try (input) {
            String source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            // Orthographic projection has a constant clip-space W. Keep frustum culling
            // and let the depth buffer handle occlusion instead of using perspective depth.
            source = source.replace("if (projectSphere(center, radius,",
                    "if (flw_projection[3][3] == 0.0 && projectSphere(center, radius,");
            return new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
        }
    }
}
