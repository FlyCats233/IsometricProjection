package io.github.flycats233.isometricprojection;

import net.minecraft.resources.ResourceLocation;

/** Change depth priority in Sodium's terrain shader while preserving its color output. */
public final class SodiumTerrainCompat {
    private static boolean vertexPatched, fragmentPatched;
    private SodiumTerrainCompat() {}

    public static boolean active() {
        return vertexPatched && fragmentPatched && !ReimaginedCompat.shaderPackActive();
    }

    public static String patch(ResourceLocation id, String source) {
        if (!id.getNamespace().equals("sodium")) return source;
        if (id.getPath().equals("blocks/block_layer_opaque.vsh")) {
            String line = "gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);";
            if (!source.contains(line)) return source;
            vertexPatched = true;
            return source.replace("void main()", "out vec3 isoPosition;\nvoid main()")
                    .replace(line, line + "\n    isoPosition = position;");
        }
        if (id.getPath().equals("blocks/block_layer_opaque.fsh") && source.contains("void main()")) {
            fragmentPatched = true;
            String header = ReimaginedCompat.HEADER
                    .replace("mat3(gbufferModelViewInverse)", "transpose(mat3(u_ModelViewMatrix))")
                    .replace("gbufferProjection", "u_ProjectionMatrix")
                    .replace("gbufferModelView", "u_ModelViewMatrix");
            return source.replace("void main()", "void isoOriginalMain()")
                    + "\nuniform mat4 u_ProjectionMatrix;\nuniform mat4 u_ModelViewMatrix;\nin vec3 isoPosition;\n"
                    + header + ReimaginedCompat.TERRAIN.replace("vertexPos", "isoPosition") + ReimaginedCompat.FOOTER;
        }
        return source;
    }
}
