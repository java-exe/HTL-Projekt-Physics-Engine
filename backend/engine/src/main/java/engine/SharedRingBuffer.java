package engine;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lock-freier Ring-Buffer zwischen BE1 (Producer) und BE2 (Consumer).
 *
 * Speichert Snapshots von MemorySegment-Referenzen.
 * BE1 schreibt nach jedem update()-Aufruf.
 * BE2 liest den neuesten verfügbaren Frame.
 */
public class SharedRingBuffer {

    private static final int CAPACITY = 4; // Anzahl Slots (Potenz von 2)
    private static final int MASK = CAPACITY - 1;

    private final MemorySegment[] slots = new MemorySegment[CAPACITY];
    private final int[] particleCounts = new int[CAPACITY];

    private final AtomicInteger writeIndex = new AtomicInteger(0);
    private volatile int latestIndex = -1;

    /**
     * BE1: Schreibt einen neuen Frame in den Buffer.
     */
    public void publish(MemorySegment frame, int particleCount) {
        int idx = writeIndex.getAndIncrement() & MASK;
        slots[idx] = frame;
        particleCounts[idx] = particleCount;
        latestIndex = idx;
    }

    /**
     * BE2: Liest den neuesten Frame. Gibt null zurück, wenn noch kein Frame vorhanden.
     */
    public MemorySegment readLatest() {
        int idx = latestIndex;
        return idx < 0 ? null : slots[idx];
    }

    public int getLatestParticleCount() {
        int idx = latestIndex;
        return idx < 0 ? 0 : particleCounts[idx];
    }
}
