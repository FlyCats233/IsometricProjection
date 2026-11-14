#version 150
uniform sampler2D ActorColor;
uniform sampler2D ActorDepth;
uniform sampler2D SceneDepth;
uniform mat4 InverseViewProjection;
uniform int OpaqueBackdrop;
uniform vec3 HiddenMin;
uniform vec3 CoveringMin;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    float depth = texture(SceneDepth, texCoord).r;
    float actorDepth = texture(ActorDepth, texCoord).r;
    if (depth >= 1.0 || actorDepth >= 1.0 || actorDepth <= depth) discard;
    vec4 scene = InverseViewProjection * vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec3 local = scene.xyz / scene.w - HiddenMin;
    if (any(lessThan(local, vec3(-0.002))) || any(greaterThan(local, vec3(1.002)))) discard;
    if (min(min(min(abs(local.x), abs(local.x-1.0)), min(abs(local.y), abs(local.y-1.0))),
            min(abs(local.z), abs(local.z-1.0))) > 0.002) discard;
    fragColor = texture(ActorColor, texCoord);
    if (fragColor.a < 0.01) discard;
    // Shadow blending can replace alpha even though B supplies an opaque backdrop.
    vec4 actorPoint = InverseViewProjection * vec4(texCoord * 2.0 - 1.0, actorDepth * 2.0 - 1.0, 1.0);
    vec3 onB = actorPoint.xyz / actorPoint.w - CoveringMin;
    if (OpaqueBackdrop == 1) fragColor.a = 1.0;
    else if (all(greaterThanEqual(onB, vec3(-0.003))) && all(lessThanEqual(onB, vec3(1.003))))
        fragColor.a = 1.0;
    else
        fragColor.rgb /= fragColor.a;
}
