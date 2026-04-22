/**
 * main.js – Einstiegspunkt des Visualizers.
 * Verbindet NetworkClient, FrameBuffer, Renderer und Telemetrie.
 */
import { NetworkClient, CMD } from './NetworkClient.js';
import { FrameBuffer }        from './FrameBuffer.js';
import { Renderer }           from './Renderer.js';
import { Telemetrie }         from './Telemetrie.js';

// ─── Three.js via CDN importmap ──────────────────────────────────────────────
import * as THREE from 'https://unpkg.com/three@0.165.0/build/three.module.js';

// ─── Konfiguration ───────────────────────────────────────────────────────────
const PARTICLE_COUNT = 5000;

// ─── DOM-Elemente ─────────────────────────────────────────────────────────────
const canvas    = document.getElementById('main-canvas');
const statusDot = document.getElementById('status-dot');

const hud = {
    fps:       document.getElementById('hud-fps'),
    tps:       document.getElementById('hud-tps'),
    particles: document.getElementById('hud-particles'),
    latency:   document.getElementById('hud-latency'),
};

const graphCanvas = document.getElementById('telemetry-canvas');

// ─── Module initialisieren ────────────────────────────────────────────────────
const frameBuffer = new FrameBuffer(3);
const renderer    = new Renderer(canvas, 50000);
const telemetrie  = new Telemetrie(hud, graphCanvas);

await renderer.init(THREE);

const client = new NetworkClient({
    particleCount: PARTICLE_COUNT,

    onSimData(buffer, offset, count) {
        frameBuffer.push(buffer, offset, count);
        telemetrie.onParticleCount(count);
        telemetrie.markReceived();
    },

    onStats(stats) {
        telemetrie.onStats(stats);
    },

    onConnect() {
        statusDot.classList.add('connected');
        console.log('[main] Connected');
    },

    onDisconnect() {
        statusDot.classList.remove('connected');
        console.log('[main] Disconnected');
    },
});

client.connect();

// ─── RAF-Loop ─────────────────────────────────────────────────────────────────
function animate() {
    requestAnimationFrame(animate);
    telemetrie.tickFrame();

    const interp = frameBuffer.getInterpolated();
    if (!interp) return;

    const { current, previous, alpha } = interp;

    renderer.updateParticles(
        current.buffer,  current.offset,
        previous?.buffer ?? null,
        previous?.offset ?? 0,
        alpha,
        current.count,
    );

    renderer.render();
}

animate();

// ─── UI Controls ──────────────────────────────────────────────────────────────
function bindSlider(id, valId, cmdCode, transform = (v) => v) {
    const slider = document.getElementById(id);
    const label  = document.getElementById(valId);
    slider.addEventListener('input', () => {
        const val = transform(parseFloat(slider.value));
        label.textContent = val.toFixed(2);
        client.sendCommand(cmdCode, val);
    });
}

bindSlider('ctrl-gravity',  'val-gravity',  CMD.SET_GRAVITY);
bindSlider('ctrl-friction', 'val-friction', CMD.SET_FRICTION);
bindSlider('ctrl-count',    'val-count',    CMD.SET_PARTICLE_COUNT,
    (v) => { document.getElementById('val-count').textContent = Math.round(v); return v; });
