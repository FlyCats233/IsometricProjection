package io.github.flycats233.isometricprojection;

import java.util.function.BooleanSupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    // A dedicated server owns these items; the client replaces this with channel detection.
    public static BooleanSupplier serverItemsAvailable = () -> true;
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("isometricprojection");
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "isometricprojection");
    public static final DeferredItem<OcclusionTool> OCCLUSION_TOOL = ITEMS.register("occlusion_tool", OcclusionTool::new);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.isometricprojection"))
                    .icon(() -> OCCLUSION_TOOL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        if (serverItemsAvailable.getAsBoolean())
                            ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                    })
                    .build());
    private ModItems() {}
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }
}
