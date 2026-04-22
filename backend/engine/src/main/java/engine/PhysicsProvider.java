package engine;

import java.lang.foreign.MemorySegment;

/**
 * Schnittstelle zwischen BE1 (Engine) und BE2 (Network).
 * BE2 ruft nur diese Methoden auf – kein Wissen über SIMD oder Layout nötig.
 */
public interface PhysicsProvider {

    /**
     * Gibt einen Pointer auf den aktuellen (fertig berechneten) Frame zurück.
     * Inhalt: N * ParticleState.BYTE_SIZE Bytes im Off-Heap-Speicher.
     */
    MemorySegment getCurrentFrameData();

    /**
     * Anzahl der aktiven Partikel im aktuellen Frame.
     */
    int getParticleCount();

    /**
     * Führt einen Simulations-Schritt aus (wird von SimulationLoop aufgerufen).
     */
    void update();
}
