package io.github.flycats233.isometricprojection.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.flycats233.isometricprojection.IsometricControls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MultiPlayerGameMode.class)
abstract class BucketUseMixin {
    @WrapMethod(method = "useItem")
    private InteractionResult isometricprojection$aimFilledBucket(Player player, InteractionHand hand,
            Operation<InteractionResult> original) {
        var stack = player.getItemInHand(hand);
        if (!IsometricControls.controlling() || !(player instanceof LocalPlayer local) || player.isSpectator()
                || !(stack.getItem() instanceof BucketItem) || stack.is(Items.BUCKET)) {
            return original.call(player, hand);
        }
        if (!(Minecraft.getInstance().hitResult instanceof BlockHitResult target)
                || target.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;

        Vec3 eye = player.getEyePosition();
        Vec3 aim = target.getLocation().subtract(Vec3.atLowerCornerOf(target.getDirection().getNormal()).scale(.001));
        Vec3 delta = aim.subtract(eye);
        Vec3 end = eye.add(delta.normalize().scale(player.blockInteractionRange()));
        BlockHitResult actual = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));
        if (actual.getType() != HitResult.Type.BLOCK || !actual.getBlockPos().equals(target.getBlockPos())
                || actual.getDirection() != target.getDirection()) return InteractionResult.PASS;

        float yaw = player.getYRot();
        float pitch = player.getXRot();
        player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        player.setXRot((float) Math.toDegrees(Math.atan2(-delta.y, delta.horizontalDistance())));
        try {
            // Vanilla uses these angles for both local prediction and the server use-item packet.
            return original.call(player, hand);
        } finally {
            player.setYRot(yaw);
            player.setXRot(pitch);
            local.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround()));
        }
    }
}
