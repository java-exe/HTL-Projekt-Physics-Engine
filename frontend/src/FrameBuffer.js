/**
 * FrameBuffer.js
 * Ring-Queue der letzten N empfangenen Frames.
 * Wird von Renderer.js für LERP-Interpolation verwendet.
 */
export class FrameBuffer {

    constructor(capacity = 3) {
        this.capacity   = capacity;
        this.frames     = new Array(capacity).fill(null);
        this.timestamps = new Float64Array(capacity);
        this.writeIdx   = 0;
        this.count      = 0;
    }

    /**
     * Schreibt einen neuen Frame in den Buffer.
     * @param {ArrayBuffer} buffer   - Rohe Protokoll-Daten
     * @param {number}      offset   - Byte-Offset zum Start der Partikel-Daten
     * @param {number}      count    - Anzahl Partikel
     */
    push(buffer, offset, count) {
        const idx = this.writeIdx % this.capacity;
        this.frames[idx]     = { buffer, offset, count };
        this.timestamps[idx] = performance.now();
        this.writeIdx++;
        if (this.count < this.capacity) this.count++;
    }

    /**
     * Gibt den neuesten und den vorletzten Frame zurück (für LERP).
     * @returns {{ current, previous, alpha }} oder null wenn < 2 Frames
     */
    getInterpolated() {
        if (this.count < 2) {
            const idx = (this.writeIdx - 1 + this.capacity) % this.capacity;
            return this.frames[idx] ? { current: this.frames[idx], previous: null, alpha: 1 } : null;
        }

        const idxCurrent  = (this.writeIdx - 1 + this.capacity) % this.capacity;
        const idxPrevious = (this.writeIdx - 2 + this.capacity) % this.capacity;

        const tCurrent  = this.timestamps[idxCurrent];
        const tPrevious = this.timestamps[idxPrevious];
        const tNow      = performance.now();

        // alpha = wie weit wir zwischen prev und current sind
        const frameInterval = tCurrent - tPrevious;
        const elapsed       = tNow - tPrevious;
        const alpha         = frameInterval > 0 ? Math.min(elapsed / frameInterval, 1) : 1;

        return {
            current:  this.frames[idxCurrent],
            previous: this.frames[idxPrevious],
            alpha,
        };
    }

    get latestCount() {
        if (this.count === 0) return 0;
        const idx = (this.writeIdx - 1 + this.capacity) % this.capacity;
        return this.frames[idx]?.count ?? 0;
    }
}
