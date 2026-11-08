package io.github.flycats233.isometricprojection.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexSorting;
import io.github.flycats233.isometricprojection.LocalCutaway;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMixin {
    @WrapMethod(method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;")
    private SectionCompiler.Results isometricprojection$cutawayMesh(SectionPos pos, RenderChunkRegion region,
            VertexSorting sorting, SectionBufferBuilderPack buffers,
            List<AddSectionGeometryEvent.AdditionalSectionRenderer> extra,
            Operation<SectionCompiler.Results> original) {
        LocalCutaway.beginCompile(LocalCutaway.snapshot());
        try {
            return original.call(pos, region, sorting, buffers, extra);
        } finally {
            LocalCutaway.endCompile();
        }
    }
}
