/**
 * Telemetrie.js
 * Berechnet und zeigt FPS, TPS, Latenz und CPU-Last.
 */
export class Telemetrie {

    constructor(hudElements, graphCanvas) {
        this.hud   = hudElements; // { fps, tps, particles, latency }
        this.graph = graphCanvas;

        this._fpsSamples   = new Float32Array(120).fill(0);
        this._tpsSamples   = new Float32Array(120).fill(0);
        this._sampleIdx    = 0;

        this._lastFrameTime = performance.now();
        this._frameCount    = 0;
        this._fps           = 0;
        this._tps           = 0;
        this._latency       = 0;

        this._msgSentTime   = 0;

        if (this.graph) {
            this._ctx = this.graph.getContext('2d');
            this.graph.width  = this.graph.clientWidth  * devicePixelRatio;
            this.graph.height = this.graph.clientHeight * devicePixelRatio;
        }
    }

    /** Wird einmal pro RAF-Frame aufgerufen */
    tickFrame() {
        this._frameCount++;
        const now = performance.now();
        const dt  = now - this._lastFrameTime;

        if (dt >= 500) { // Update alle 500ms
            this._fps = (this._frameCount / dt) * 1000;
            this._frameCount   = 0;
            this._lastFrameTime = now;

            this._fpsSamples[this._sampleIdx % 120] = this._fps;
            this._sampleIdx++;

            this._updateHud();
            this._drawGraph();
        }
    }

    /** Wird aufgerufen wenn ein Stats-Paket vom Server kommt */
    onStats({ tps, cpuLoad }) {
        this._tps = tps;
        this._tpsSamples[this._sampleIdx % 120] = tps;
        if (this.hud.tps) this.hud.tps.textContent = `TPS: ${tps.toFixed(1)}`;
    }

    onParticleCount(count) {
        if (this.hud.particles) this.hud.particles.textContent = `Particles: ${count.toLocaleString()}`;
    }

    markSent()     { this._msgSentTime = performance.now(); }
    markReceived() {
        if (this._msgSentTime > 0) {
            this._latency = performance.now() - this._msgSentTime;
            this._msgSentTime = 0;
        }
    }

    _updateHud() {
        if (this.hud.fps)     this.hud.fps.textContent     = `FPS: ${this._fps.toFixed(0)}`;
        if (this.hud.latency) this.hud.latency.textContent = `Latency: ${this._latency.toFixed(1)} ms`;
    }

    _drawGraph() {
        if (!this._ctx) return;
        const ctx = this._ctx;
        const w   = this.graph.width;
        const h   = this.graph.height;

        ctx.clearRect(0, 0, w, h);

        // Hintergrund
        ctx.fillStyle = '#0d0d14';
        ctx.fillRect(0, 0, w, h);

        // FPS-Linie (grün)
        this._drawLine(ctx, this._fpsSamples, '#44ff88', 120, w, h);

        // TPS-Linie (blau)
        this._drawLine(ctx, this._tpsSamples, '#4488ff', 80, w, h);

        // Labels
        ctx.fillStyle = '#44ff88';
        ctx.font = `${10 * devicePixelRatio}px monospace`;
        ctx.fillText(`FPS ${this._fps.toFixed(0)}`, 4, h - 4);
        ctx.fillStyle = '#4488ff';
        ctx.fillText(`TPS ${this._tps.toFixed(0)}`, w / 2, h - 4);
    }

    _drawLine(ctx, samples, color, maxVal, w, h) {
        ctx.beginPath();
        ctx.strokeStyle = color;
        ctx.lineWidth   = devicePixelRatio;

        const count = samples.length;
        for (let i = 0; i < count; i++) {
            const x = (i / (count - 1)) * w;
            const y = h - (samples[(this._sampleIdx + i) % count] / maxVal) * h * 0.9;
            i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y);
        }
        ctx.stroke();
    }
}
