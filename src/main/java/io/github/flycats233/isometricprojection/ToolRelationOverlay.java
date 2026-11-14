package io.github.flycats233.isometricprojection;

import com.mojang.blaze3d.vertex.*;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Editing guides are visible through terrain and never write world depth. */
final class ToolRelationOverlay extends RenderType {
    private static final RenderType GUIDE = create("isometric_relation_guides", DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES, 1536, false, false, CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER).setLineState(new LineStateShard(OptionalDouble.of(2)))
                    .setDepthTestState(NO_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL)
                    .createCompositeState(false));
    private record Arrow(float ax,float ay,float bx,float by) {}
    private static final java.util.List<Arrow> arrows=new java.util.ArrayList<>();
    private ToolRelationOverlay() { super("unused",DefaultVertexFormat.POSITION_COLOR_NORMAL,VertexFormat.Mode.LINES,0,false,false,()->{},()->{}); }

    static void render(RenderLevelStageEvent event) {
        arrows.clear();
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.level==null) return;
        boolean holding=false;
        for(var hand:net.minecraft.world.InteractionHand.values()) holding |= mc.player.getItemInHand(hand).is(ModItems.OCCLUSION_TOOL.get());
        if(!holding) return;
        var camera=event.getCamera().getPosition();
        PoseStack pose=new PoseStack();pose.mulPose(event.getModelViewMatrix());
        var buffers=mc.renderBuffers().bufferSource();var lines=buffers.getBuffer(GUIDE);
        for(var rule:ClientOcclusion.rules()) {
            if(!rule.valid(mc.level) || Math.min(rule.hidden().distToCenterSqr(camera),rule.covering().distToCenterSqr(camera))>96*96)continue;
            box(pose,lines,new AABB(rule.hidden()).move(camera.scale(-1)),1,.55f,.15f);
            box(pose,lines,new AABB(rule.covering()).move(camera.scale(-1)),.15f,.85f,1);
            Vec3 a=Vec3.atCenterOf(rule.hidden()).subtract(camera),b=Vec3.atCenterOf(rule.covering()).subtract(camera);
            Vec3 direction=b.subtract(a).normalize();
            double inset=.51/Math.max(Math.abs(direction.x),Math.max(Math.abs(direction.y),Math.abs(direction.z)));
            Vec3 from=a.add(direction.scale(inset)),tip=b.subtract(direction.scale(inset));
            var matrix=new org.joml.Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
            var pa=matrix.transform(new org.joml.Vector4f((float)from.x,(float)from.y,(float)from.z,1));
            var pb=matrix.transform(new org.joml.Vector4f((float)tip.x,(float)tip.y,(float)tip.z,1));
            if(pa.w>0 && pb.w>0 && Math.abs(pa.z/pa.w)<=1 && Math.abs(pb.z/pb.w)<=1) {
                float w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
                arrows.add(new Arrow((pa.x/pa.w*.5f+.5f)*w,(.5f-pa.y/pa.w*.5f)*h,
                        (pb.x/pb.w*.5f+.5f)*w,(.5f-pb.y/pb.w*.5f)*h));
            }
        }
        for(var hand:net.minecraft.world.InteractionHand.values()) {
            var stack=mc.player.getItemInHand(hand);if(!stack.is(ModItems.OCCLUSION_TOOL.get()))continue;
            var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            if(tag.contains("occlusion_first") && mc.level.dimension().location().toString().equals(tag.getString("occlusion_dimension")))
                box(pose,lines,new AABB(net.minecraft.core.BlockPos.of(tag.getLong("occlusion_first"))).move(camera.scale(-1)),1,1,.2f);
        }
        buffers.endBatch(GUIDE);
    }
    private static void box(PoseStack pose,VertexConsumer buffer,AABB bounds,float r,float g,float b) {
        LevelRenderer.renderLineBox(pose,buffer,bounds.inflate(.004),r,g,b,1);
    }
    static void hud(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
        if(Minecraft.getInstance().screen!=null)return;
        var gui=event.getGuiGraphics();
        for(var a:arrows) {
            float dx=a.bx-a.ax,dy=a.by-a.ay;
            double angle=Math.atan2(dy,dx),length=Math.hypot(dx,dy);
            if(length<2)continue;
            line(gui,a.ax,a.ay,a.bx,a.by);
            double head=Math.min(6,length*.55);
            line(gui,a.bx,a.by,(float)(a.bx-head*Math.cos(angle-.55)),(float)(a.by-head*Math.sin(angle-.55)));
            line(gui,a.bx,a.by,(float)(a.bx-head*Math.cos(angle+.55)),(float)(a.by-head*Math.sin(angle+.55)));
        }
    }
    private static void line(net.minecraft.client.gui.GuiGraphics gui,float ax,float ay,float bx,float by) {
        var pose=gui.pose();pose.pushPose();pose.translate(ax,ay,0);
        pose.mulPose(new org.joml.Quaternionf().rotationZ((float)Math.atan2(by-ay,bx-ax)));
        gui.fill(0,-1,(int)Math.ceil(Math.hypot(bx-ax,by-ay)),1,0xFFFFFF33);
        pose.popPose();
    }
}
