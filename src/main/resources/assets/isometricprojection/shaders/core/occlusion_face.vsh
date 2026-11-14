#version 150
#moj_import <fog.glsl>
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
uniform mat4 ViewProjection;
uniform sampler2D Sampler2;
uniform int FogShape;
out float vertexDistance;
out vec2 texCoord;
out vec4 vertexColor;
void main() {
    gl_Position = ViewProjection * vec4(Position, 1.0);
    vertexDistance = fog_distance(Position, FogShape);
    texCoord = UV0;
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
}
