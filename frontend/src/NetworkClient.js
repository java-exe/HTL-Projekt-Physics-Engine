/**
 * NetworkClient.js
 * Verwaltet die Datenquelle: MockServer (DEV) oder echtes WebSocket (PROD).
 *
 * USE_MOCK = true  → MockServer (kein Java-Backend nötig)
 * USE_MOCK = false → ws://localhost:8080 (Java NIO Server)
 */
import { MockServer } from './MockServer.js';

const USE_MOCK         = true;
const WS_URL           = 'ws://localhost:8080';
const MAGIC            = 0xCAFEBABE;
const TYPE_SIM         = 1;
const TYPE_STATS       = 2;
const PARTICLE_SIZE    = 17;

// CMD_CODES (protocol.txt)
export const CMD = {
    SET_GRAVITY:         0x01,
    SET_FRICTION:        0x02,
    RESET_SIM:           0x03,
    SET_PARTICLE_COUNT:  0x04,
};

export class NetworkClient {

    constructor({ onSimData, onStats, onConnect, onDisconnect, particleCount = 5000 }) {
        this.onSimData     = onSimData;
        this.onStats       = onStats;
        this.onConnect     = onConnect    ?? (() => {});
        this.onDisconnect  = onDisconnect ?? (() => {});
        this.particleCount = particleCount;

        this._mock   = null;
        this._socket = null;
    }

    connect() {
        if (USE_MOCK) {
            this._mock = new MockServer((data) => this._processData(data), this.particleCount);
            this._mock.start();
            this.onConnect();
        } else {
            this._socket = new WebSocket(WS_URL);
            this._socket.binaryType = 'arraybuffer';
            this._socket.onopen    = () => this.onConnect();
            this._socket.onclose   = () => this.onDisconnect();
            this._socket.onmessage = (e) => this._processData(e.data);
        }
    }

    /**
     * Sendet einen Binär-Befehl an den Server (oder Mock).
     * @param {number} cmdCode - CMD_CODE (Uint8)
     * @param {number} value   - Float32 Wert
     */
    sendCommand(cmdCode, value) {
        const buf  = new ArrayBuffer(5);
        const view = new DataView(buf);
        view.setUint8(0, cmdCode);
        view.setFloat32(1, value, false); // Big Endian

        if (USE_MOCK && this._mock) {
            this._dispatchToMock(cmdCode, value);
        } else if (this._socket?.readyState === WebSocket.OPEN) {
            this._socket.send(buf);
        }
    }

    _dispatchToMock(cmdCode, value) {
        switch (cmdCode) {
            case CMD.SET_GRAVITY:  this._mock.setGravity(value);  break;
            case CMD.SET_FRICTION: this._mock.setFriction(value); break;
        }
    }

    _processData(buffer) {
        const view = new DataView(buffer);
        if (buffer.byteLength < 8) return;

        const magic = view.getUint32(0, false);
        if (magic !== MAGIC) {
            console.warn('[NetworkClient] Invalid magic number:', magic.toString(16));
            return;
        }

        const type       = view.getUint16(4, false);
        const payloadLen = view.getUint16(6, false);

        if (type === TYPE_SIM) {
            const count = Math.floor(payloadLen / PARTICLE_SIZE);
            this.onSimData(buffer, 8, count);
        } else if (type === TYPE_STATS) {
            const tps     = view.getFloat32(8,  false);
            const cpuLoad = view.getFloat32(12, false);
            this.onStats({ tps, cpuLoad });
        }
    }

    disconnect() {
        this._mock?.stop();
        this._socket?.close();
    }
}
