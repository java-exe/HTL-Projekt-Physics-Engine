package engine;

/**
 * Einstiegspunkt für BE1 (Standalone-Test ohne Netzwerk).
 * Startet die Simulation und gibt Tick-Infos in die Konsole.
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        final int PARTICLE_COUNT = 10_000;

        SharedRingBuffer ringBuffer = new SharedRingBuffer();
        PhysicsEngine    engine     = new PhysicsEngine(PARTICLE_COUNT);
        SimulationLoop   loop       = new SimulationLoop(engine, ringBuffer);

        System.out.println("[BE1] Starting with " + PARTICLE_COUNT + " particles...");
        loop.start();

        // 5 Sekunden laufen lassen, dann stoppen
        Thread.sleep(5_000);
        loop.stop();
        engine.close();

        System.out.println("[BE1] Completed " + loop.getTickCount() + " ticks.");
    }
}
