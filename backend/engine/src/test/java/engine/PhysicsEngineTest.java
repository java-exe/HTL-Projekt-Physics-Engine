package engine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PhysicsEngineTest {

    private PhysicsEngine engine;

    @BeforeEach
    void setUp() {
        engine = new PhysicsEngine(100);
    }

    @AfterEach
    void tearDown() {
        engine.close();
    }

    @Test
    void particleCountIsCorrect() {
        assertEquals(100, engine.getParticleCount());
    }

    @Test
    void frameDataIsNotNullAfterInit() {
        assertNotNull(engine.getCurrentFrameData());
    }

    @Test
    void frameDataSizeMatchesParticleCount() {
        long expectedBytes = 100L * ParticleState.BYTE_SIZE;
        assertEquals(expectedBytes, engine.getCurrentFrameData().byteSize());
    }

    @Test
    void updateChangesParticlePositions() {
        var frame = engine.getCurrentFrameData();
        ParticleState p = new ParticleState(frame, 0);

        float xBefore = p.getX();
        float yBefore = p.getY();

        engine.update();

        // Nach einem Tick müssen sich x oder y verändert haben
        // (Gravitation + Geschwindigkeit wirken)
        assertFalse(xBefore == p.getX() && yBefore == p.getY(),
            "Partikel hat sich nach update() nicht bewegt");
    }

    @Test
    void gravityReducesVerticalVelocity() {
        var frame = engine.getCurrentFrameData();
        ParticleState p = new ParticleState(frame, 0);

        float vyBefore = p.getVy();

        // Nach einem Tick muss vy kleiner sein (Gravitation zieht nach unten)
        engine.update();

        assertTrue(p.getVy() < vyBefore,
            "Gravitation muss vy nach jedem Tick reduzieren");
    }

    @Test
    void setGravityZeroStopsVerticalAcceleration() {
        engine.setGravity(0.0f);
        engine.setFriction(1.0f); // Kein Reibungsverlust

        var frame = engine.getCurrentFrameData();
        ParticleState p = new ParticleState(frame, 0);

        float vyBefore = p.getVy();
        engine.update();
        float vyAfter = p.getVy();

        assertEquals(vyBefore, vyAfter, 1e-4f,
            "Bei gravity=0 und friction=1 darf sich vy nicht ändern");
    }

    // ---------------------------------------------------------------
    // TODO für das Team: folgende Tests implementieren
    // ---------------------------------------------------------------

    @Test
    void wallCollisionReverseVelocity() {
        // TODO: Partikel an die Wand setzen (x > WORLD_WIDTH),
        // update() aufrufen, prüfen dass vx umgekehrt wird.
        // Wartet auf Implementierung der Wand-Kollision in PhysicsEngine.
    }

    @Test
    void particleParticleCollisionConservesMomentum() {
        // TODO: Spatial Partitioning (Sprint 5) vorausgesetzt.
        // Zwei Partikel mit entgegengesetzten Velocities aufeinander setzen,
        // nach Kollision Impuls p = m*v prüfen (Masse = 1 für alle Partikel).
    }
}
