// particle.vert.glsl
// Instanced Vertex Shader für Partikel-Rendering.
// Jedes Partikel = 1 Instance (InstancedBufferGeometry).

attribute vec2  instancePosition;  // x, y (World-Space)
attribute vec4  instanceColor;     // r, g, b, a (0-1)
attribute float instanceSize;      // Partikel-Radius in Pixel

uniform mat4  projectionMatrix;
uniform mat4  modelViewMatrix;
uniform float pointSize;

varying vec4 vColor;

void main() {
    vColor = instanceColor;

    // Wandle World-Space Position in Clip-Space um
    vec4 mvPosition = modelViewMatrix * vec4(instancePosition, 0.0, 1.0);
    gl_Position     = projectionMatrix * mvPosition;

    // Größe variiert mit CollisionFreq (via instanceSize)
    gl_PointSize = max(2.0, instanceSize);
}
