/**
 * Renderer.js
 * Three.js WebGL Renderer mit InstancedBufferGeometry.
 * Alle Partikel = 1 Draw-Call.
 */

const PARTICLE_SIZE = 17; // Bytes pro Partikel im Protokoll

export class Renderer {

    constructor(canvas, maxParticles = 50000) {
        this.canvas       = canvas;
        this.maxParticles = maxParticles;

        // Three.js wird via CDN geladen (package.json importmap)
        this._three  = null;
        this._scene  = null;
        this._camera = null;
        this._renderer = null;
        this._points   = null;

        // Pre-allokierte Typed Arrays (kein new im RAF-Loop!)
        this._positions = new Float32Array(maxParticles * 2); // x, y
        this._colors    = new Float32Array(maxParticles * 4); // r, g, b, a
        this._sizes     = new Float32Array(maxParticles);     // CollisionFreq → size

        this._posAttr    = null;
        this._colorAttr  = null;
        this._sizeAttr   = null;

        this._particleCount = 0;
        this._rafId         = null;
    }

    async init(THREE) {
        this._three = THREE;

        this._scene    = new THREE.Scene();
        this._camera   = new THREE.OrthographicCamera(-1, 1, 1, -1, 0.1, 1000);
        this._camera.position.z = 1;

        this._renderer = new THREE.WebGLRenderer({
            canvas: this.canvas,
            antialias: false, // Speed > Quality für 250k Partikel
            alpha: true,
        });
        this._renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
        this._updateSize();

        this._buildGeometry(THREE);
        this._updateCamera();

        window.addEventListener('resize', () => {
            this._updateSize();
            this._updateCamera();
        });
    }

    _buildGeometry(THREE) {
        const geo = new THREE.BufferGeometry();

        // 1 Vertex pro Partikel (Points-Rendering)
        geo.setAttribute('position',
            new THREE.BufferAttribute(new Float32Array(this.maxParticles * 3), 3));

        this._posAttr = new THREE.BufferAttribute(this._positions, 2);
        this._posAttr.usage = THREE.DynamicDrawUsage;
        geo.setAttribute('instancePosition', this._posAttr);

        this._colorAttr = new THREE.BufferAttribute(this._colors, 4);
        this._colorAttr.usage = THREE.DynamicDrawUsage;
        geo.setAttribute('instanceColor', this._colorAttr);

        this._sizeAttr = new THREE.BufferAttribute(this._sizes, 1);
        this._sizeAttr.usage = THREE.DynamicDrawUsage;
        geo.setAttribute('instanceSize', this._sizeAttr);

        const mat = new THREE.ShaderMaterial({
            vertexShader: VERT_SHADER,
            fragmentShader: FRAG_SHADER,
            transparent: true,
            depthWrite: false,
            blending: THREE.AdditiveBlending,
        });

        this._points = new THREE.Points(geo, mat);
        this._scene.add(this._points);
    }

    /**
     * Aktualisiert die GPU-Buffer mit interpolierten Partikel-Daten.
     * Wird im RAF-Loop aufgerufen – KEIN new, KEIN GC-Druck.
     *
     * @param {ArrayBuffer} currentBuf   - Aktueller Frame
     * @param {number}      currentOff   - Byte-Offset
     * @param {ArrayBuffer} previousBuf  - Vorheriger Frame (für LERP), kann null sein
     * @param {number}      previousOff  - Byte-Offset vorheriger Frame
     * @param {number}      alpha        - Interpolations-Faktor (0-1)
     * @param {number}      count        - Partikelanzahl
     */
    updateParticles(currentBuf, currentOff, previousBuf, previousOff, alpha, count) {
        this._particleCount = Math.min(count, this.maxParticles);

        const cur  = new DataView(currentBuf);
        const prev = previousBuf ? new DataView(previousBuf) : null;

        for (let i = 0; i < this._particleCount; i++) {
            const cOff = currentOff  + i * PARTICLE_SIZE;
            const pOff = previousOff + i * PARTICLE_SIZE;

            const cx = cur.getFloat32(cOff + 4, false);
            const cy = cur.getFloat32(cOff + 8, false);

            let x = cx, y = cy;
            if (prev) {
                const px = prev.getFloat32(pOff + 4, false);
                const py = prev.getFloat32(pOff + 8, false);
                x = px + (cx - px) * alpha;
                y = py + (cy - py) * alpha;
            }

            const pi2 = i * 2;
            const pi4 = i * 4;
            this._positions[pi2]     = x;
            this._positions[pi2 + 1] = y;

            this._colors[pi4]     = cur.getUint8(cOff + 12) / 255;
            this._colors[pi4 + 1] = cur.getUint8(cOff + 13) / 255;
            this._colors[pi4 + 2] = cur.getUint8(cOff + 14) / 255;
            this._colors[pi4 + 3] = cur.getUint8(cOff + 15) / 255;

            const cf = cur.getUint8(cOff + 16);
            this._sizes[i] = 3 + cf * 0.1; // Kollision → größer
        }

        this._points.geometry.setDrawRange(0, this._particleCount);
        this._posAttr.needsUpdate   = true;
        this._colorAttr.needsUpdate = true;
        this._sizeAttr.needsUpdate  = true;
    }

    render() {
        this._renderer.render(this._scene, this._camera);
    }

    _updateSize() {
        const w = this.canvas.clientWidth;
        const h = this.canvas.clientHeight;
        this._renderer.setSize(w, h, false);
    }

    _updateCamera() {
        const w = this.canvas.clientWidth / 2;
        const h = this.canvas.clientHeight / 2;
        const cam = this._camera;
        cam.left   = -w;
        cam.right  =  w;
        cam.top    =  h;
        cam.bottom = -h;
        cam.updateProjectionMatrix();
    }
}

// Inline Shaders (vermeidet fetch()-Calls für Sprint 1)
const VERT_SHADER = `
attribute vec2  instancePosition;
attribute vec4  instanceColor;
attribute float instanceSize;
varying vec4 vColor;

void main() {
    vColor = instanceColor;
    gl_Position  = projectionMatrix * modelViewMatrix * vec4(instancePosition, 0.0, 1.0);
    gl_PointSize = max(2.0, instanceSize);
}`;

const FRAG_SHADER = `
precision mediump float;
varying vec4 vColor;

void main() {
    vec2  uv   = gl_PointCoord - 0.5;
    float dist = length(uv);
    if (dist > 0.5) discard;
    float glow = 1.0 - smoothstep(0.2, 0.5, dist);
    gl_FragColor = vec4(vColor.rgb * (0.6 + 0.4 * glow), vColor.a * glow);
}`;
