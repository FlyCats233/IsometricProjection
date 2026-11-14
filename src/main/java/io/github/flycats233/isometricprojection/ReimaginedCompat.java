package io.github.flycats233.isometricprojection;

import java.util.*;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/** Reimagined's forward-lit terrain keeps its own material and lighting path. */
public final class ReimaginedCompat {
    private static Method packName, currentPack, pipelineManager;
    private static boolean checked;
    private static boolean terrainPatched, entitiesPatched, entitiesVertexPatched;
    private ReimaginedCompat() {}
    public static void reset() { terrainPatched=false; entitiesPatched=false; entitiesVertexPatched=false; }

    public static boolean active() {
        return terrainPatched && entitiesPatched && entitiesVertexPatched && shaderPackActive() && currentName().toLowerCase(Locale.ROOT).contains("complementaryreimagined");
    }
    private static String currentName() {
        try {return packName == null ? "" : (String)packName.invoke(null);}
        catch(ReflectiveOperationException e) {return "";}
    }
    public static boolean shaderPackActive() {
        try {
            if (!checked) {
                checked = true;
                Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
                packName = iris.getMethod("getCurrentPackName");
                currentPack = iris.getMethod("getCurrentPack");
                pipelineManager = iris.getMethod("getPipelineManager");
            }
            var manager = pipelineManager == null ? null : pipelineManager.invoke(null);
            var pipeline = manager == null ? Optional.empty() : (Optional<?>)manager.getClass().getMethod("getPipeline").invoke(manager);
            return pipeline.isPresent() && pipeline.get().getClass().getSimpleName().equals("IrisRenderingPipeline")
                    && currentPack != null && ((Optional<?>)currentPack.invoke(null)).isPresent();
        } catch (ReflectiveOperationException e) { return false; }
    }

    public static String vertex(String source, String name) {
        if (!name.startsWith("gbuffers_entities") || !source.contains("GetLightMapCoordinates")
                || !source.contains("void main()") || !source.contains("ftransform()")) return source;
        entitiesVertexPatched = true;
        return source.replace("void main()", "void isoOriginalVertexMain()") + """

uniform int isoActorCount;
uniform vec3 isoActorMin[128];
uniform vec3 isoActorMax[128];
uniform vec3 isoActorHidden[128];
out vec3 isoActorPosition;
flat out int isoActorShifted;
void main() {
    isoOriginalVertexMain();
    isoActorPosition=(gbufferModelViewInverse * gl_ModelViewMatrix * gl_Vertex).xyz;
    isoActorShifted=0;
    if(gbufferProjection[3][3] < 0.5 || (entityId!=50016 && entityId!=50017)) return;
    vec3 toward=normalize(mat3(gbufferModelViewInverse)*vec3(0.0,0.0,1.0));
    float shift=0.0;
    for(int i=0;i<isoActorCount;i++) {
        if(all(greaterThanEqual(isoActorPosition,isoActorMin[i])) && all(lessThanEqual(isoActorPosition,isoActorMax[i]))) {
            vec3 center=(isoActorMin[i]+isoActorMax[i])*0.5;
            vec3 halfSize=(isoActorMax[i]-isoActorMin[i])*0.5;
            float back=dot(center,toward)-dot(halfSize,abs(toward));
            float front=dot(isoActorHidden[i]+vec3(0.5),toward)+dot(vec3(0.5),abs(toward));
            shift=max(shift,front-back+0.002);
        }
    }
    if(shift>0.0) {
        // Translate only clip-space depth: keep native model ordering and alpha/depth handling.
        vec4 delta=gbufferProjection * gbufferModelView * vec4(toward*shift,0.0);
        gl_Position.z+=delta.z;
        isoActorShifted=1;
    }
}
""";
    }

    public static String fragment(String source, String name) {
        boolean terrain = name.startsWith("gbuffers_terrain") && source.contains("DoBrightBlockTweaks");
        boolean entities = name.startsWith("gbuffers_entities") && source.contains("ProbablyMainPlayer");
        if (!terrain && !entities || source.contains("isoOriginalMain")) return source;
        if (!source.contains("void main()")) return source;
        if(terrain) terrainPatched = true;
        if(entities) entitiesPatched = true;
        // Orthographic rays have a constant view direction, including for specular highlights.
        source = source.replace("vec3 nViewPos = normalize(viewPos);",
                "vec3 nViewPos = gbufferProjection[3][3] > 0.5 ? vec3(0.0,0.0,-1.0) : normalize(viewPos);");
        if (entities) {
            // Keep the native fragment shader (including its depth/skin layer behavior).
            source = source.replace("void main()", "in vec3 isoActorPosition;\nflat in int isoActorShifted;\nvoid main()");
            return source.replace("vec3 viewPos = ScreenToView(screenPos);",
                    "vec3 viewPos = isoActorShifted != 0 ? (gbufferModelView * vec4(isoActorPosition,1.0)).xyz : ScreenToView(screenPos);");
        }
        source = source.replace("void main()", "void isoOriginalMain()");
        return source + HEADER + TERRAIN + FOOTER;
    }

    static final String HEADER = """

uniform int isoRuleCount;
uniform vec3 isoHidden[128];
uniform vec3 isoCovering[128];
uniform int isoActorCount;
uniform vec3 isoActorMin[128];
uniform vec3 isoActorMax[128];
uniform vec3 isoActorHidden[128];
float isoFrontDepth(vec3 p, vec3 a, float crossingTolerance) {
    vec3 toward = normalize(mat3(gbufferModelViewInverse) * vec3(0.0,0.0,1.0));
    float lo = -1e20, hi = 1e20;
    for (int axis=0; axis<3; axis++) {
        if (abs(toward[axis]) < 1e-7) {
            if (p[axis] < a[axis]-crossingTolerance || p[axis] > a[axis]+1.0+crossingTolerance) return gl_FragDepth;
        } else {
            float t0=(a[axis]-p[axis])/toward[axis];
            float t1=(a[axis]+1.0-p[axis])/toward[axis];
            lo=max(lo,min(t0,t1)); hi=min(hi,max(t0,t1));
        }
    }
    if (hi <= 0.0001 || hi+crossingTolerance < lo) return gl_FragDepth;
    vec4 clip=gbufferProjection * gbufferModelView * vec4(p+toward*hi,1.0);
    // Keep several depth-buffer steps of separation, scaled for orthographic depth range.
    float separation=max(0.000001,abs(gbufferProjection[2][2])*0.5*0.001);
    return min(gl_FragDepth, clip.z/clip.w*0.5+0.5-separation);
}
void main() {
    gl_FragDepth=gl_FragCoord.z;
    isoOriginalMain();
    if (gbufferProjection[3][3] < 0.5) return;
""";
    static final String TERRAIN = """
    vec3 isoPoint=vertexPos;
    float crossingTolerance=max(0.00002,min(0.001,max(length(dFdx(isoPoint)),length(dFdy(isoPoint)))*0.2));
    for(int i=0;i<isoRuleCount;i++) {
        vec3 local=isoPoint-isoCovering[i];
        if(all(greaterThanEqual(local,vec3(-0.001))) && all(lessThanEqual(local,vec3(1.001))))
            gl_FragDepth=isoFrontDepth(isoPoint,isoHidden[i],crossingTolerance);
    }
""";
    static final String FOOTER = "}\n";

    public static void uniforms() {
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        if(program == 0) return;
        int countLocation = GL20.glGetUniformLocation(program, "isoRuleCount");
        int actorsLocation = GL20.glGetUniformLocation(program, "isoActorCount");
        if (countLocation < 0 && actorsLocation < 0) return;
        var mc = Minecraft.getInstance();
        boolean enabled = (active() || SodiumTerrainCompat.active()) && CameraController.active() && !CameraController.transitioning() && mc.level != null;
        var camera = mc.gameRenderer.getMainCamera().getPosition();
        var rules = enabled ? ClientOcclusion.rules().stream().filter(r -> r.valid(mc.level)
                && !LocalCutaway.hides(r.hidden()) && !LocalCutaway.hides(r.covering())).limit(128).toList() : List.<OcclusionRule>of();
        if (countLocation >= 0) {
            float[] a=new float[rules.size()*3], b=new float[a.length];
            for(int i=0;i<rules.size();i++) {
                var rule=rules.get(i);
                put(a,i,rule.hidden().getX()-camera.x,rule.hidden().getY()-camera.y,rule.hidden().getZ()-camera.z);
                put(b,i,rule.covering().getX()-camera.x,rule.covering().getY()-camera.y,rule.covering().getZ()-camera.z);
            }
            GL20.glUniform1i(countLocation,rules.size());
            if(a.length>0) { upload(program,"isoHidden",a); upload(program,"isoCovering",b); }
        }
        if (actorsLocation >= 0) {
            var mins=new ArrayList<Float>();var maxs=new ArrayList<Float>();var hidden=new ArrayList<Float>();
            if(enabled) for(var player:mc.level.players()) for(var rule:rules) {
                if(mins.size()>=384 || !OcclusionActors.inherits(player,rule.covering())) continue;
                float partial=mc.getTimer().getGameTimeDeltaPartialTick(false);
                // A collision box excludes arms, skin layers, capes and held equipment.
                var box=player.getBoundingBox().move(
                        net.minecraft.util.Mth.lerp(partial,player.xOld,player.getX())-player.getX(),
                        net.minecraft.util.Mth.lerp(partial,player.yOld,player.getY())-player.getY(),
                        net.minecraft.util.Mth.lerp(partial,player.zOld,player.getZ())-player.getZ()).inflate(.9,.3,.9);
                for(double v:new double[]{box.minX-camera.x,box.minY-camera.y,box.minZ-camera.z})mins.add((float)v);
                for(double v:new double[]{box.maxX-camera.x,box.maxY-camera.y,box.maxZ-camera.z})maxs.add((float)v);
                for(double v:new double[]{rule.hidden().getX()-camera.x,rule.hidden().getY()-camera.y,rule.hidden().getZ()-camera.z})hidden.add((float)v);
            }
            GL20.glUniform1i(actorsLocation,mins.size()/3);
            if(!mins.isEmpty()) {upload(program,"isoActorMin",array(mins));upload(program,"isoActorMax",array(maxs));upload(program,"isoActorHidden",array(hidden));}
        }
    }
    private static float[] array(List<Float> list) {float[] a=new float[list.size()];for(int i=0;i<a.length;i++)a[i]=list.get(i);return a;}
    private static void put(float[] a,int i,double x,double y,double z) {a[3*i]=(float)x;a[3*i+1]=(float)y;a[3*i+2]=(float)z;}
    private static void upload(int p,String name,float[] data) {int l=GL20.glGetUniformLocation(p,name+"[0]");if(l>=0)GL20.glUniform3fv(l,data);}
}
