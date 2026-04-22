// particle.frag.glsl
// Fragment Shader: Runde Partikel mit weichem Glow-Effekt.

precision mediump float;

varying vec4 vColor;

void main() {
    // gl_PointCoord = (0,0) bis (1,1) innerhalb des Point-Sprites
    vec2  uv   = gl_PointCoord - 0.5;        // Zentriert: (-0.5 bis 0.5)
    float dist = length(uv);                  // Abstand vom Zentrum

    // Harter Kreis-Clip (alles außerhalb Radius 0.5 verwerfen)
    if (dist > 0.5) discard;

    // Soft glow: Helligkeit nimmt vom Zentrum nach außen ab
    float glow  = 1.0 - smoothstep(0.2, 0.5, dist);
    float alpha = vColor.a * glow;

    gl_FragColor = vec4(vColor.rgb * (0.6 + 0.4 * glow), alpha);
}
