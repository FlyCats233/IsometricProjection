package io.github.flycats233.isometricprojection.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.flycats233.isometricprojection.IsometricControls;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.glue.SuperGlueSelectionHandler", remap = false)
abstract class CreateGlueSelectionMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/utility/RaycastHelper;getTraceTarget(Lnet/minecraft/world/entity/player/Player;DLnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"),
            require = 0)
    private Vec3 isometricprojection$aimGlueRemoval(Vec3 original) {
        if (!IsometricControls.controlling()) return original;
        Minecraft mc = Minecraft.getInstance();
        Vec3 eye = mc.player.getEyePosition();
        if (!(mc.hitResult instanceof BlockHitResult target) || target.getType() != HitResult.Type.BLOCK) {
            return eye;
        }
        Vec3 hit = target.getLocation();
        return hit.add(hit.subtract(eye).normalize().scale(.01));
    }
}
