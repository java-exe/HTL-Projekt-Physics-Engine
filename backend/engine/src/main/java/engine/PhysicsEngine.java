package engine;

import java.lang.foreign.*;

/**
 * BE1: Kernlogik der Physik-Simulation.
 *
 * Sprint 1: Dummy-Positionen (Partikel kreisen).
 * Sprint 3: SIMD / Java Vector API Integration.
 */
public class PhysicsEngine implements PhysicsProvider {

    private final int particleCount;
    private final Arena arena;
    private final MemorySegment segment;

    // Physik-Parameter (via Commands aus BE2 setzbar)
    private volatile float gravity   = 9.81f;
    private volatile float friction  = 0.99f;

    private static final float FIXED_DT = 1.0f / 60.0f;

    public PhysicsEngine(int particleCount) {
        this.particleCount = particleCount;
        this.arena = Arena.ofShared(); // Thread-sicher, manuell schließbar
        this.segment = arena.allocate(particleCount * ParticleState.BYTE_SIZE);
        initParticles();
    }

    private void initParticles() {
        for (int i = 0; i < particleCount; i++) {
            ParticleState p = new ParticleState(segment, i);
            double angle = 2.0 * Math.PI * i / particleCount;
            float radius = 200.0f + (i % 50) * 3.0f;

            p.setId(i);
            p.setX((float)(Math.cos(angle) * radius));
            p.setY((float)(Math.sin(angle) * radius));
            p.setVx((float)(-Math.sin(angle) * 50.0));
            p.setVy((float)( Math.cos(angle) * 50.0));
            p.setR((byte)(128 + (int)(127 * Math.sin(angle))));
            p.setG((byte)(128 + (int)(127 * Math.cos(angle))));
            p.setB((byte)(200));
            p.setA((byte)(255));
            p.setCollisionFreq((byte) 0);
        }
    }

    @Override
    public void update() {
        // TODO Sprint 3: Java Vector API (SIMD) einsetzen
        for (int i = 0; i < particleCount; i++) {
            ParticleState p = new ParticleState(segment, i);

            float vy = p.getVy() - gravity * FIXED_DT;
            float vx = p.getVx() * friction;
            vy = vy * friction;

            p.setVx(vx);
            p.setVy(vy);
            p.setX(p.getX() + vx * FIXED_DT);
            p.setY(p.getY() + vy * FIXED_DT);
        }
    }

    @Override
    public MemorySegment getCurrentFrameData() {
        return segment.asReadOnly();
    }

    @Override
    public int getParticleCount() {
        return particleCount;
    }

    public void setGravity(float g)  { this.gravity = g; }
    public void setFriction(float f) { this.friction = f; }

    public void close() {
        arena.close();
    }
}
