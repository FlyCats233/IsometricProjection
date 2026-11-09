package io.github.flycats233.isometricprojection.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.github.flycats233.isometricprojection.IsometricControls;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorInteractionHandler", remap = false)
abstract class CreateChainSelectionMixin {
    @ModifyExpressionValue(method = "clientTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getEyePosition()Lnet/minecraft/world/phys/Vec3;"), require = 0)
    private static Vec3 isometricprojection$chainOrigin(Vec3 original,
            @Share("chainRay") LocalRef<IsometricControls.CursorRay> ray) {
        if (!IsometricControls.controlling()) return original;
        ray.set(IsometricControls.reachableCursorRay(Minecraft.getInstance().player.blockInteractionRange() + 1));
        return ray.get().origin();
    }

    @ModifyExpressionValue(method = "clientTick", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/utility/RaycastHelper;getTraceTarget(Lnet/minecraft/world/entity/player/Player;DLnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"), require = 0)
    private static Vec3 isometricprojection$chainEnd(Vec3 original,
            @Share("chainRay") LocalRef<IsometricControls.CursorRay> ray) {
        return ray.get() == null ? original : ray.get().end();
    }

    @ModifyExpressionValue(method = "clientTick", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;hitResult:Lnet/minecraft/world/phys/HitResult;"), require = 0)
    private static HitResult isometricprojection$ignoreUnrelatedHitLimit(HitResult original,
            @Share("chainRay") LocalRef<IsometricControls.CursorRay> ray) {
        return ray.get() == null ? original : null;
    }

    @WrapOperation(method = "clientTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D", ordinal = 1), require = 0)
    private static double isometricprojection$checkChainReach(Vec3 hit, Vec3 origin, Operation<Double> original,
            @Share("chainRay") LocalRef<IsometricControls.CursorRay> ray) {
        if (ray.get() != null) {
            var mc = Minecraft.getInstance();
            Vec3 eye = mc.player.getEyePosition();
            var obstruction = mc.level.clip(new ClipContext(eye, hit, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, mc.player));
            if (obstruction.getType() == HitResult.Type.BLOCK
                    && obstruction.getLocation().distanceToSqr(hit) > .0001) return Double.POSITIVE_INFINITY;
        }
        return original.call(hit, origin);
    }
}
