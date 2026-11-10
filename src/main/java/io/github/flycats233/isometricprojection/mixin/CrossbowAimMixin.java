package io.github.flycats233.isometricprojection.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CrossbowItem.class)
abstract class CrossbowAimMixin {
    @WrapMethod(method = "shootProjectile")
    private void isometricprojection$useCurrentPlayerAim(LivingEntity shooter, Projectile projectile,
            int index, float velocity, float inaccuracy, float spread, LivingEntity target, Operation<Void> original) {
        if (!(shooter instanceof ServerPlayer player) || target != null) {
            original.call(shooter, projectile, index, velocity, inaccuracy, spread, target);
            return;
        }

        // The use-item packet updates yaw immediately, but player head yaw updates on the next tick.
        float previousHeadYaw = player.getYHeadRot();
        player.setYHeadRot(player.getYRot());
        try {
            original.call(shooter, projectile, index, velocity, inaccuracy, spread, target);
        } finally {
            player.setYHeadRot(previousHeadYaw);
        }
    }
}
