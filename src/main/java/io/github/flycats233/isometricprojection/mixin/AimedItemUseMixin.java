package io.github.flycats233.isometricprojection.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.flycats233.isometricprojection.IsometricControls;
import io.github.flycats233.isometricprojection.ProjectileAim;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MultiPlayerGameMode.class)
abstract class AimedItemUseMixin {
    @WrapMethod(method = "releaseUsingItem")
    private void isometricprojection$aimProjectile(Player player, Operation<Void> original) {
        if (!(player instanceof LocalPlayer local) || !ProjectileAim.active() || !ProjectileAim.charging()) {
            original.call(player);
            return;
        }
        isometricprojection$withAim(local, ProjectileAim.direction(), true, () -> original.call(player));
    }

    @WrapMethod(method = "useItem")
    private InteractionResult isometricprojection$aimPlacement(Player player, InteractionHand hand,
            Operation<InteractionResult> original) {
        var stack = player.getItemInHand(hand);
        if (!IsometricControls.controlling() || !(player instanceof LocalPlayer local) || player.isSpectator()) {
            return original.call(player, hand);
        }
        if (ProjectileAim.firesImmediately(stack)) {
            if (!ProjectileAim.active()) return InteractionResult.PASS;
            return isometricprojection$withAim(local, ProjectileAim.direction(), false, () -> original.call(player, hand));
        }
        boolean boat = stack.getItem() instanceof BoatItem;
        boolean filledBucket = stack.getItem() instanceof BucketItem && !stack.is(Items.BUCKET);
        if (!(boat || filledBucket)) {
            return original.call(player, hand);
        }
        if (IsometricControls.fineCursorCaptured()) return InteractionResult.PASS;
        ClipContext.Fluid fluids = boat ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE;
        HitResult selected = boat ? IsometricControls.cursorBlockHit(fluids) : Minecraft.getInstance().hitResult;
        if (!(selected instanceof BlockHitResult target)
                || target.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;

        Vec3 eye = player.getEyePosition();
        double reach = player.blockInteractionRange();
        if (eye.distanceToSqr(target.getLocation()) > reach * reach) return InteractionResult.PASS;
        Vec3 aim = target.getLocation().subtract(Vec3.atLowerCornerOf(target.getDirection().getNormal()).scale(.001));
        Vec3 delta = aim.subtract(eye);
        Vec3 end = eye.add(delta.normalize().scale(reach));
        BlockHitResult actual = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                fluids, player));
        if (actual.getType() != HitResult.Type.BLOCK || !actual.getBlockPos().equals(target.getBlockPos())
                || actual.getDirection() != target.getDirection()) return InteractionResult.PASS;

        return isometricprojection$withAim(local, delta, false, () -> original.call(player, hand));
    }

    @org.spongepowered.asm.mixin.Unique
    private static <T> T isometricprojection$withAim(LocalPlayer player, Vec3 delta, boolean release,
            Supplier<T> action) {
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        player.setXRot((float) Math.toDegrees(Math.atan2(-delta.y, delta.horizontalDistance())));
        try {
            // Use-item packets carry rotation, but the release-action packet does not.
            if (release) player.connection.send(new ServerboundMovePlayerPacket.Rot(
                    player.getYRot(), player.getXRot(), player.onGround()));
            return action.get();
        } finally {
            player.setYRot(yaw);
            player.setXRot(pitch);
            player.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround()));
        }
    }
}
