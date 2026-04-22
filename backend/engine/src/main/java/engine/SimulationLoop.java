package engine;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Regelt das Timing der Simulation (Fixed Step, 60 Hz).
 * Schreibt nach jedem Tick in den SharedRingBuffer.
 */
public class SimulationLoop {

    private static final int TARGET_TPS = 60;
    private static final long TICK_NANOS = 1_000_000_000L / TARGET_TPS;

    private final PhysicsEngine engine;
    private final SharedRingBuffer ringBuffer;
    private final ScheduledExecutorService scheduler;

    private long tickCount = 0;
    private long lastTickNano = System.nanoTime();

    public SimulationLoop(PhysicsEngine engine, SharedRingBuffer ringBuffer) {
        this.engine = engine;
        this.ringBuffer = ringBuffer;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sim-loop");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::tick, 0, TICK_NANOS, TimeUnit.NANOSECONDS);
        System.out.println("[SimulationLoop] Started at " + TARGET_TPS + " TPS");
    }

    public void stop() {
        scheduler.shutdown();
        System.out.println("[SimulationLoop] Stopped after " + tickCount + " ticks");
    }

    private void tick() {
        engine.update();
        ringBuffer.publish(engine.getCurrentFrameData(), engine.getParticleCount());
        tickCount++;
    }

    public long getTickCount() { return tickCount; }
}
