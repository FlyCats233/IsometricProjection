package io.github.flycats233.isometricprojection;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Select the tool's texture from its server-synchronized first-block selection. */
final class OcclusionToolAppearance {
    private OcclusionToolAppearance() {}

    static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.OCCLUSION_TOOL.get(),
                ResourceLocation.fromNamespaceAndPath("isometricprojection", "selected"),
                (stack, level, entity, seed) -> OcclusionTool.hasSelection(stack, level) ? 1 : 0));
    }
}
