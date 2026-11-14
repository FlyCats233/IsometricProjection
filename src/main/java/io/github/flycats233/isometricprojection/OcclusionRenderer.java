package io.github.flycats233.isometricprojection;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.io.UncheckedIOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

public final class OcclusionRenderer {
    private static ShaderInstance shader;
    private static RenderTarget sceneDepth;
    private OcclusionRenderer() {}

    public static void shaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath("isometricprojection", "occlusion_face"),
                    DefaultVertexFormat.BLOCK), value -> shader = value);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (shader == null || !CameraController.active() || CameraController.transitioning()) {
            ToolRelationOverlay.render(event);
            return;
        }
        var mc = Minecraft.getInstance();
        if (ClientOcclusion.rules().isEmpty() || ReimaginedCompat.active()) {
            ToolRelationOverlay.render(event);
            return;
        }
        var main = mc.getMainRenderTarget();
        if (sceneDepth == null) sceneDepth = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
        else if (sceneDepth.width != main.width || sceneDepth.height != main.height) sceneDepth.resize(main.width, main.height, Minecraft.ON_OSX);
        sceneDepth.copyDepthFrom(main);
        main.bindWrite(false);
        Matrix4f vp = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
        Matrix4f inverseVp = new Matrix4f(vp).invert();
        Vec3 camera = event.getCamera().getPosition();
        var visiblePairs = OcclusionRule.visibleFacePairs(new Vec3(event.getCamera().getLookVector()).scale(-1))
                .stream().filter(f -> f.covering() == net.minecraft.core.Direction.UP).toList();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableCull();
        mc.gameRenderer.lightTexture().turnOnLightLayer();
        int lightTexture = RenderSystem.getShaderTexture(2);
        int previousTexture = RenderSystem.getShaderTexture(0);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        try {
            if (!SodiumTerrainCompat.active())
            for (var rule : ClientOcclusion.rules()) {
                if (!rule.valid(mc.level) || rule.hidden().distToCenterSqr(camera) > 96 * 96
                        || LocalCutaway.hides(rule.hidden()) || LocalCutaway.hides(rule.covering())) continue;
                for (var faces : visiblePairs) {
                    drawFace(rule, faces, vp, inverseVp, camera, sceneDepth, main.width, main.height, lightTexture);
                }
            }
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, previousTexture);
            main.bindWrite(false);
        }
        OcclusionActors.render(event, sceneDepth, inverseVp);
        ToolRelationOverlay.render(event);
    }

    static void drawFace(OcclusionRule rule, OcclusionRule.FacePair faces, Matrix4f vp, Matrix4f inverseVp,
            Vec3 camera, RenderTarget scene, int width, int height, int lightTexture) {
        var mc = Minecraft.getInstance();
        var state = mc.level.getBlockState(rule.covering());
        var model = mc.getBlockRenderer().getBlockModel(state);
        shader.setSampler("Sampler0", mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getId());
        shader.setSampler("SceneDepth", scene.getDepthTextureId());
        shader.setSampler("Sampler2", lightTexture);
        shader.safeGetUniform("ViewProjection").set(vp);
        shader.safeGetUniform("InverseViewProjection").set(inverseVp);
        shader.safeGetUniform("TargetSize").set((float) width, (float) height);
        Vec3 a = Vec3.atLowerCornerOf(rule.hidden()).subtract(camera);
        Vec3 plane = OcclusionRule.faceCenter(rule.hidden(), faces.hidden()).subtract(camera);
        shader.safeGetUniform("HiddenMin").set((float) a.x, (float) a.y, (float) a.z);
        shader.safeGetUniform("HiddenPlane").set((float) plane.x, (float) plane.y, (float) plane.z);
        var normal = faces.hidden().getNormal();
        shader.safeGetUniform("HiddenNormal").set((float) normal.getX(), (float) normal.getY(), (float) normal.getZ());
        RenderSystem.setShader(() -> shader);
        PoseStack pose = new PoseStack();
        pose.translate(rule.covering().getX() - camera.x, rule.covering().getY() - camera.y,
                rule.covering().getZ() - camera.z);
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        // Use world-aware vanilla AO and four independent vertex light values.
        var data = mc.level.getModelDataManager().getAt(rule.covering());
        mc.getBlockRenderer().getModelRenderer().tesselateBlock(mc.level, model, state, rule.covering(),
                pose, buffer, false, RandomSource.create(), state.getSeed(rule.covering()), OverlayTexture.NO_OVERLAY,
                data == null ? ModelData.EMPTY : data, RenderType.solid());
        shader.safeGetUniform("FogStart").set(RenderSystem.getShaderFogStart());
        shader.safeGetUniform("FogEnd").set(RenderSystem.getShaderFogEnd());
        shader.safeGetUniform("FogColor").set(RenderSystem.getShaderFogColor());
        shader.safeGetUniform("FogShape").set(RenderSystem.getShaderFogShape().getIndex());

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (sceneDepth != null) {
            sceneDepth.destroyBuffers();
            sceneDepth = null;
        }
    }
}
