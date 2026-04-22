/**
 * MockServer.js
 * Emuliert das Java-Backend im Browser.
 * Erzeugt protokollkonforme Binärdaten (protocol.txt) ohne echten Server.
 *
 * Läuft im selben Thread (kein Worker nötig für Sprint 1).
 * Sprint 4+: In Web Worker auslagern für Zero-Main-Thread-Blocking.
 */
export class MockServer {

    static MAGIC        = 0xCAFEBABE;
    static TYPE_SIM     = 1;
    static TYPE_STATS   = 2;
    static PARTICLE_SIZE = 17; // Bytes pro Partikel im Protokoll

    constructor(onMessage, particleCount = 5000) {
        this.onMessage      = onMessage;
        this.particleCount  = particleCount;
        this.isRunning      = false;
        this.tickCount      = 0;
        this.lastTickTime   = 0;

        // Partikel-Zustand (Float32 Arrays für Null-Allocation im Loop)
        this.x   = new Float32Array(particleCount);
        this.y   = new Float32Array(particleCount);
        this.vx  = new Float32Array(particleCount);
        this.vy  = new Float32Array(particleCount);
        this.r   = new Uint8Array(particleCount);
        this.g   = new Uint8Array(particleCount);
        this.b   = new Uint8Array(particleCount);
        this.a   = new Uint8Array(particleCount);
        this.cf  = new Uint8Array(particleCount); // CollisionFreq

        // Physik-Parameter
        this.gravity  = 9.81;
        this.friction = 0.99;
        this.dt       = 1.0 / 60.0;

        // Pre-allokierter Sende-Buffer (wird wiederverwendet)
        const payloadLen = particleCount * MockServer.PARTICLE_SIZE;
        this._buffer = new ArrayBuffer(8 + payloadLen); // Header(8) + Payload
        this._view   = new DataView(this._buffer);

        this._initParticles();
        this._writeStaticHeader(payloadLen);
    }

    _initParticles() {
        for (let i = 0; i < this.particleCount; i++) {
            const angle  = (2 * Math.PI * i) / this.particleCount;
            const radius = 200 + (i % 50) * 3;

            this.x[i]  =  Math.cos(angle) * radius;
            this.y[i]  =  Math.sin(angle) * radius;
            this.vx[i] = -Math.sin(angle) * 50;
            this.vy[i] =  Math.cos(angle) * 50;
            this.r[i]  = 128 + Math.floor(127 * Math.sin(angle));
            this.g[i]  = 128 + Math.floor(127 * Math.cos(angle));
            this.b[i]  = 200;
            this.a[i]  = 255;
            this.cf[i] = 0;
        }
    }

    _writeStaticHeader(payloadLen) {
        this._view.setUint32(0, MockServer.MAGIC, false);      // Magic
        this._view.setUint16(4, MockServer.TYPE_SIM, false);   // PacketType
        this._view.setUint16(6, payloadLen, false);            // PayloadLen
    }

    start() {
        this.isRunning = true;
        this.lastTickTime = performance.now();
        this._loop();
    }

    stop() {
        this.isRunning = false;
    }

    setGravity(value)  { this.gravity  = value; }
    setFriction(value) { this.friction = value; }

    _loop() {
        if (!this.isRunning) return;

        this._update();
        this._sendFrame();
        this.tickCount++;

        // Fixed 60 TPS
        setTimeout(() => this._loop(), 1000 / 60);
    }

    _update() {
        const dt = this.dt;
        const g  = this.gravity;
        const fr = this.friction;

        // Null-Allocation: nur Array-Zugriffe, keine new-Aufrufe
        for (let i = 0; i < this.particleCount; i++) {
            this.vy[i] = (this.vy[i] - g * dt) * fr;
            this.vx[i] = this.vx[i] * fr;
            this.x[i] += this.vx[i] * dt;
            this.y[i] += this.vy[i] * dt;

            // Boden-Kollision (einfache Grenze)
            if (this.y[i] < -400) {
                this.y[i]  = -400;
                this.vy[i] = Math.abs(this.vy[i]) * 0.7;
                this.cf[i] = Math.min(255, this.cf[i] + 10);
            }
            if (this.cf[i] > 0) this.cf[i]--;
        }
    }

    _sendFrame() {
        const view = this._view;
        let offset = 8; // Nach dem Header

        for (let i = 0; i < this.particleCount; i++) {
            view.setUint32(offset,     i,           false); // ID
            view.setFloat32(offset + 4,  this.x[i],  false); // X
            view.setFloat32(offset + 8,  this.y[i],  false); // Y
            view.setUint8(offset + 12, this.r[i]);           // R
            view.setUint8(offset + 13, this.g[i]);           // G
            view.setUint8(offset + 14, this.b[i]);           // B
            view.setUint8(offset + 15, this.a[i]);           // A
            view.setUint8(offset + 16, this.cf[i]);          // CollisionFreq
            offset += MockServer.PARTICLE_SIZE;
        }

        this.onMessage(this._buffer);
    }
}
