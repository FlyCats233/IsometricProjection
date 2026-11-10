package io.github.flycats233.isometricprojection;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class ProjectileAim {
    private ProjectileAim() {}

    public static boolean active() {
        if (!IsometricControls.controlling()) return false;
        var player = Minecraft.getInstance().player;
        return (charging() || !player.isUsingItem()
                && (firesImmediately(player.getMainHandItem()) || firesImmediately(player.getOffhandItem())))
                && !CameraController.fineRotationHeld() && !IsometricControls.fineCursorCaptured();
    }

    public static boolean charging() {
        var player = Minecraft.getInstance().player;
        var item = player.getUseItem().getItem();
        return player.isUsingItem() && (item instanceof BowItem || item instanceof TridentItem);
    }

    public static boolean firesImmediately(ItemStack stack) {
        var item = stack.getItem();
        return item instanceof CrossbowItem && CrossbowItem.isCharged(stack)
                || item instanceof WindChargeItem || item instanceof EnderpearlItem
                || item instanceof SnowballItem || item instanceof EggItem
                || item instanceof ExperienceBottleItem || item instanceof ThrowablePotionItem;
    }

    public static Vec3 direction() {
        var mc = Minecraft.getInstance();
        var ray = IsometricControls.cursorRay(mc.getTimer().getGameTimeDeltaPartialTick(true));
        var player = mc.player;
        Vec3 eye = player.getEyePosition();
        Vec3 axis = ray.end().subtract(ray.origin()).normalize();
        Vec3 end = ray.origin().add(axis.scale(Math.min(96, ray.origin().distanceTo(ray.end()))));
        var block = mc.level.clip(LocalCutaway.selectionContext(ray.origin(), end, player));
        Vec3 limit = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : end;
        var entity = ProjectileUtil.getEntityHitResult(player, ray.origin(), limit,
                new AABB(ray.origin(), limit).inflate(1), candidate -> !candidate.isSpectator() && candidate.isPickable(),
                ray.origin().distanceToSqr(limit));
        Vec3 target = entity != null ? entity.getEntity().getBoundingBox().getCenter() : limit;
        if (entity == null && block.getType() == HitResult.Type.MISS) {
            // Where there is no surface, aim through the cursor at the player's eye height.
            double distance = (eye.y - ray.origin().y) / axis.y;
            target = ray.origin().add(axis.scale(Math.clamp(distance, 0, 96)));
        }
        Vec3 aim = target.subtract(eye);
        return aim.lengthSqr() > 1.0E-6 ? aim : axis;
    }

    public static void render(RenderGuiEvent.Post event) {
        if (!active()) return;
        var mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        var window = mc.getWindow();
        var gui = event.getGuiGraphics();
        float x = (float) (mc.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth());
        float y = (float) (mc.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight());
        gui.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        try {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            var matrix = gui.pose().last().pose();
            buffer.addVertex(matrix, x, y, 0).setColor(0xFFFFFFFF);
            for (int vertex = 0; vertex <= 12; vertex++) {
                double angle = -vertex * Math.PI / 6;
                buffer.addVertex(matrix, x + 2.5f * (float) Math.cos(angle),
                        y + 2.5f * (float) Math.sin(angle), 0).setColor(0xFFFFFFFF);
            }
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
        }
    }
}
