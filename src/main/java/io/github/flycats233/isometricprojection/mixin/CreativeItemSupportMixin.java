package io.github.flycats233.isometricprojection.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.flycats233.isometricprojection.ClientOcclusion;
import io.github.flycats233.isometricprojection.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/** Saved creative hotbars may still contain the editor from a supported world. */
@Mixin(MultiPlayerGameMode.class)
abstract class CreativeItemSupportMixin {
    @WrapMethod(method = "handleCreativeModeItemAdd")
    private void isometricprojection$guardCreativeItem(ItemStack stack, int slot, Operation<Void> original) {
        if (!stack.is(ModItems.OCCLUSION_TOOL.get()) || ClientOcclusion.serverSupported()) {
            original.call(stack, slot);
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player != null && slot >= 0 && slot < player.inventoryMenu.slots.size())
            player.inventoryMenu.getSlot(slot).set(ItemStack.EMPTY);
        original.call(ItemStack.EMPTY, slot);
    }

    @WrapMethod(method = "handleCreativeModeItemDrop")
    private void isometricprojection$guardCreativeDrop(ItemStack stack, Operation<Void> original) {
        if (!stack.is(ModItems.OCCLUSION_TOOL.get()) || ClientOcclusion.serverSupported()) original.call(stack);
    }
}
