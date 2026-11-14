#version 150
#moj_import <fog.glsl>
uniform sampler2D Sampler0;
uniform sampler2D SceneDepth;
uniform mat4 InverseViewProjection;
uniform vec2 TargetSize;
uniform vec3 HiddenMin;
uniform vec3 HiddenPlane;
uniform vec3 HiddenNormal;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
in float vertexDistance;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    vec2 uv = gl_FragCoord.xy / TargetSize;
    float depth = texture(SceneDepth, uv).r;
    if (depth >= 1.0 || gl_FragCoord.z <= depth) discard;
    vec4 scene = InverseViewProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec3 point = scene.xyz / scene.w;
    // The visible world sample must belong to the authored A block face, not another foreground object.
    if (abs(dot(point - HiddenPlane, HiddenNormal)) > 0.002) discard;
    vec3 local = point - HiddenMin;
    if (any(lessThan(local, vec3(-0.002))) || any(greaterThan(local, vec3(1.002)))) discard;
    fragColor = texture(Sampler0, texCoord) * vertexColor;
    if (fragColor.a < 0.1) discard;
    fragColor = linear_fog(fragColor, vertexDistance, FogStart, FogEnd, FogColor);
}
