package engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimulationLoopTest {

    @Test
    void loopPublishesToRingBuffer() throws InterruptedException {
        SharedRingBuffer buffer = new SharedRingBuffer();
        PhysicsEngine    engine = new PhysicsEngine(10);
        SimulationLoop   loop   = new SimulationLoop(engine, buffer);

        assertNull(buffer.readLatest(), "Buffer sollte vor Start leer sein");

        loop.start();
        Thread.sleep(100); // kurz warten
        loop.stop();
        engine.close();

        assertNotNull(buffer.readLatest(), "Nach Start muss min. 1 Frame im Buffer sein");
        assertTrue(buffer.getLatestParticleCount() == 10);
    }

    @Test
    void loopReachesTargetTickRate() throws InterruptedException {
        SharedRingBuffer buffer = new SharedRingBuffer();
        PhysicsEngine    engine = new PhysicsEngine(10);
        SimulationLoop   loop   = new SimulationLoop(engine, buffer);

        loop.start();
        Thread.sleep(1000); // 1 Sekunde
        loop.stop();
        engine.close();

        long ticks = loop.getTickCount();
        // Erwartung: 60 Ticks ± 10% Toleranz
        assertTrue(ticks >= 54 && ticks <= 66,
            "Erwartet ~60 Ticks/s, tatsächlich: " + ticks);
    }

    // ---------------------------------------------------------------
    // TODO für das Team: folgende Tests implementieren
    // ---------------------------------------------------------------

    @Test
    void loopDoesNotDriftOverTime() throws InterruptedException {
        // TODO: Loop 10 Sekunden laufen lassen.
        // Prüfen dass getTickCount() nahe an 600 liegt (±5%).
        // Testet ob der ScheduledExecutorService driftet.
    }

    @Test
    void stopAndRestartWorks() throws InterruptedException {
        // TODO: Loop starten, stoppen, neu starten.
        // Prüfen dass tickCount nach Neustart wieder hochzählt.
        // Wartet auf Implementierung von restart() in SimulationLoop.
    }
}
