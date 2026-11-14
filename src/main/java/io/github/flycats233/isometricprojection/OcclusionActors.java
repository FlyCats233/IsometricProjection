package io.github.flycats233.isometricprojection;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.*;
import org.joml.Matrix4f;

/** Players inherit only the rules of the block supporting their feet. */
public final class OcclusionActors {
    private static final Map<UUID, Set<BlockPos>> supports = new HashMap<>();
    private static ClientLevel level;
    private static ShaderInstance shader;
    private static RenderTarget actor;
    private OcclusionActors() {}

    public static boolean inherits(Player player, BlockPos support) {
        return supports.getOrDefault(player.getUUID(), Set.of()).contains(support);
    }

    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (level != mc.level) { supports.clear(); level = mc.level; }
        if (level == null) return;
        var rules = ClientOcclusion.rules();
        supports.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
        for (var player : level.players()) {
            if (player.isPassenger() || player.getAbilities().flying || player.isSpectator()
                    || player.isSwimming() || !player.isAlive()) {
                supports.remove(player.getUUID());
                continue;
            }
            if (!player.onGround()) continue; // Retain the last support throughout a jump/fall.
            Set<BlockPos> standing = new HashSet<>();
            var bounds = player.getBoundingBox();
            for (var rule : rules) {
                var b = rule.covering();
                if (rule.valid(level) && Math.abs(player.getY() - b.getY() - 1) < .08
                        && bounds.maxX > b.getX() + .001 && bounds.minX < b.getX() + .999
                        && bounds.maxZ > b.getZ() + .001 && bounds.minZ < b.getZ() + .999) standing.add(b);
            }
            if (standing.isEmpty()) supports.remove(player.getUUID());
            else supports.put(player.getUUID(), standing);
        }
    }

    public static void shaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath("isometricprojection", "occlusion_actor"),
                DefaultVertexFormat.POSITION_TEX), value -> shader = value);
        } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }

    public static void render(RenderLevelStageEvent event, RenderTarget scene, Matrix4f inverse) {
        var mc = Minecraft.getInstance();
        if (shader == null || supports.isEmpty()) return;
        var main = mc.getMainRenderTarget();
        if (actor == null) {
            actor = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            actor.setClearColor(0, 0, 0, 0);
        } else if (actor.width != main.width || actor.height != main.height)
            actor.resize(main.width, main.height, Minecraft.ON_OSX);
        var camera = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var dispatcher = mc.getEntityRenderDispatcher();
        for (Player player : mc.level.players()) {
            var standing = supports.get(player.getUUID());
            if (standing == null || player.isInvisible()) continue;
            var rules = ClientOcclusion.rules().stream().filter(r -> standing.contains(r.covering())
                    && r.valid(mc.level) && !LocalCutaway.hides(r.hidden()) && !LocalCutaway.hides(r.covering())).toList();
            if (rules.isEmpty()) continue;
            actor.clear(Minecraft.ON_OSX);
            boolean nativeTerrain=SodiumTerrainCompat.active();
            if(nativeTerrain) {
                org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER,main.frameBufferId);
                org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER,actor.frameBufferId);
                org.lwjgl.opengl.GL30.glBlitFramebuffer(0,0,main.width,main.height,0,0,actor.width,actor.height,
                        org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT,org.lwjgl.opengl.GL11.GL_NEAREST);
            }
            actor.bindWrite(false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            PoseStack pose = new PoseStack();
            pose.mulPose(event.getModelViewMatrix());
            var position = new net.minecraft.world.phys.Vec3(
                    net.minecraft.util.Mth.lerp(partial, player.xOld, player.getX()),
                    net.minecraft.util.Mth.lerp(partial, player.yOld, player.getY()),
                    net.minecraft.util.Mth.lerp(partial, player.zOld, player.getZ())).subtract(camera);
            // Seed B's surface depth so vanilla shadows land on it before compositing.
            var vp = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
            var pairs = OcclusionRule.visibleFacePairs(new net.minecraft.world.phys.Vec3(event.getCamera().getLookVector()).scale(-1))
                    .stream().filter(f -> f.covering() == net.minecraft.core.Direction.UP).toList();
            mc.gameRenderer.lightTexture().turnOnLightLayer();
            com.mojang.blaze3d.platform.Lighting.setupLevel();
            if (mc.level.effects().constantAmbientLight()) com.mojang.blaze3d.platform.Lighting.setupNetherLevel();
            RenderSystem.setShaderTexture(0, net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
            if(nativeTerrain) RenderSystem.colorMask(false,false,false,false);
            for (var rule : rules) for (var faces : pairs)
                OcclusionRenderer.drawFace(rule, faces, vp, inverse, camera, scene, main.width, main.height,
                        RenderSystem.getShaderTexture(2));
            RenderSystem.colorMask(true,true,true,true);
            try (var vertices = new ByteBufferBuilder(4096)) {
                var buffers = MultiBufferSource.immediate(vertices);
                dispatcher.setRenderShadow(true);
                try {
                    dispatcher.render(player, position.x, position.y, position.z, net.minecraft.util.Mth.lerp(partial, player.yRotO, player.getYRot()), partial,
                            pose, buffers, dispatcher.getPackedLightCoords(player, partial));
                    buffers.endBatch();
                } finally { dispatcher.setRenderShadow(true); main.bindWrite(false); }
            }
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            try {
                for (var rule : rules) {
                    var a = net.minecraft.world.phys.Vec3.atLowerCornerOf(rule.hidden()).subtract(camera);
                    shader.safeGetUniform("OpaqueBackdrop").set(nativeTerrain ? 1 : 0);
                    shader.setSampler("ActorColor", actor.getColorTextureId());
                    shader.setSampler("ActorDepth", actor.getDepthTextureId());
                    shader.setSampler("SceneDepth", scene.getDepthTextureId());
                    shader.safeGetUniform("InverseViewProjection").set(inverse);
                    shader.safeGetUniform("HiddenMin").set((float)a.x, (float)a.y, (float)a.z);
                    var b = net.minecraft.world.phys.Vec3.atLowerCornerOf(rule.covering()).subtract(camera);
                    shader.safeGetUniform("CoveringMin").set((float)b.x, (float)b.y, (float)b.z);
                    RenderSystem.setShader(() -> shader);
                    var quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                    quad.addVertex(-1,-1,0).setUv(0,0);
                    quad.addVertex(1,-1,0).setUv(1,0);
                    quad.addVertex(1,1,0).setUv(1,1);
                    quad.addVertex(-1,1,0).setUv(0,1);
                    BufferUploader.drawWithShader(quad.buildOrThrow());
                }
            } finally {
                RenderSystem.enableDepthTest(); RenderSystem.depthMask(true); RenderSystem.disableBlend();
            }
        }
    }

    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        supports.clear(); level = null;
        if (actor != null) { actor.destroyBuffers(); actor = null; }
    }
}

