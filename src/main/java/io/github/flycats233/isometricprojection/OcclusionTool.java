package io.github.flycats233.isometricprojection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class OcclusionTool extends Item {
    public OcclusionTool() { super(new Properties().stacksTo(1)); }

    public static boolean hasSelection(ItemStack stack, Level level) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains("occlusion_first") && (level == null
                || level.dimension().location().toString().equals(tag.getString("occlusion_dimension")));
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("message.isometricprojection.tool.help"));
    }

    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOn(context);
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        ServerLevel level = player.serverLevel();
        var pos = context.getClickedPos();
        var face = context.getClickedFace();
        var stack = context.getItemInHand();
        if (!player.isCreative()) return reply(player, "creative");
        if (player.isShiftKeyDown()) {
            clear(stack);
            boolean removed = OcclusionData.get(level).remove(pos, face);
            if (removed) OcclusionNetwork.broadcast(level);
            return reply(player, removed ? "removed" : "cleared");
        }
        if (!OcclusionRule.validBlock(level, pos)) return reply(player, "invalid");
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains("occlusion_first") || !level.dimension().location().toString().equals(tag.getString("occlusion_dimension"))) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
                data.putLong("occlusion_first", pos.asLong());
                data.putInt("occlusion_face", face.get3DDataValue());
                data.putString("occlusion_dimension", level.dimension().location().toString());
            });
            return reply(player, "first");
        }
        BlockPos first = BlockPos.of(tag.getLong("occlusion_first"));
        Direction firstFace = Direction.from3DDataValue(tag.getInt("occlusion_face"));
        if (first.equals(pos) || first.distSqr(pos) > 64 * 64 || !OcclusionRule.validBlock(level, first))
            return reply(player, "invalid");
        if (!OcclusionRule.hasTopSeam(first, pos)) return reply(player, "no_seam");
        if (OcclusionData.get(level).hasReverse(first, pos)) return reply(player, "reverse");
        if (!OcclusionData.get(level).add(first, firstFace, pos, face)) return reply(player, "limit");
        clear(stack);
        OcclusionNetwork.broadcast(level);
        return reply(player, "saved");
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !level.isClientSide) {
            clear(stack);
            player.displayClientMessage(Component.translatable("message.isometricprojection.tool.cleared"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static InteractionResult reply(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("message.isometricprojection.tool." + key), true);
        return InteractionResult.SUCCESS;
    }

    private static void clear(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove("occlusion_first"); tag.remove("occlusion_face"); tag.remove("occlusion_dimension");
        });
    }
}
