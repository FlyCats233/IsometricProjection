package io.github.flycats233.isometricprojection.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.flycats233.isometricprojection.CameraController;
import io.github.flycats233.isometricprojection.IsometricControls;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow private double xpos;
    @Shadow private double ypos;

    @WrapOperation(method = {"grabMouse", "releaseMouse"}, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/InputConstants;grabOrReleaseMouse(JIDD)V"))
    private void isometricprojection$preserveCursor(long window, int mode, double x, double y,
            Operation<Void> original) {
        if (!CameraController.active()) {
            original.call(window, mode, x, y);
            return;
        }
        // Keep Minecraft's logical grab transition, but leave the visible cursor in place.
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var currentX = stack.mallocDouble(1);
            var currentY = stack.mallocDouble(1);
            GLFW.glfwGetCursorPos(window, currentX, currentY);
            xpos = currentX.get(0);
            ypos = currentY.get(0);
        }
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void isometricprojection$freeAim(double delta, CallbackInfo ci) {
        if (CameraController.renderingIsometric()) {
            if (IsometricControls.fineCursorCaptured()) {
                CameraController.fineRotate(((MouseHandler) (Object) this).getXVelocity());
            }
            ci.cancel();
        }
    }

    @Inject(method = "grabMouse", at = @At("RETURN"))
    private void isometricprojection$showPointer(CallbackInfo ci) {
        if (IsometricControls.controlling() && !CameraController.fineRotationHeld()) IsometricControls.freeCursor();
    }
}
