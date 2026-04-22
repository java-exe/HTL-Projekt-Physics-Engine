package engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import java.lang.foreign.*;
import static org.junit.jupiter.api.Assertions.*;

class ParticleStateTest {

    private Arena arena;

    @AfterEach
    void tearDown() {
        if (arena != null) arena.close();
    }

    @Test
    void readWriteSingleParticle() {
        arena = Arena.ofConfined();
        MemorySegment seg = arena.allocate(ParticleState.BYTE_SIZE);
        ParticleState p = new ParticleState(seg, 0);

        p.setId(42);
        p.setX(1.5f);
        p.setY(-3.0f);
        p.setVx(10.0f);
        p.setVy(-5.0f);
        p.setR((byte) 255);
        p.setG((byte) 128);
        p.setB((byte) 0);
        p.setA((byte) 200);
        p.setCollisionFreq((byte) 7);

        assertEquals(42,      p.getId());
        assertEquals(1.5f,    p.getX(),  1e-6f);
        assertEquals(-3.0f,   p.getY(),  1e-6f);
        assertEquals(10.0f,   p.getVx(), 1e-6f);
        assertEquals(-5.0f,   p.getVy(), 1e-6f);
        assertEquals((byte) 255, p.getR());
        assertEquals((byte) 128, p.getG());
        assertEquals((byte) 0,   p.getB());
        assertEquals((byte) 200, p.getA());
        assertEquals((byte) 7,   p.getCollisionFreq());
    }

    @Test
    void multipleParticlesNoOverlap() {
        arena = Arena.ofConfined();
        int count = 100;
        MemorySegment seg = arena.allocate(count * ParticleState.BYTE_SIZE);

        // Schreibe unterschiedliche Werte in jeden Slot
        for (int i = 0; i < count; i++) {
            ParticleState p = new ParticleState(seg, i);
            p.setId(i);
            p.setX(i * 1.0f);
            p.setY(i * -1.0f);
        }

        // Verifiziere, dass kein Slot den anderen überschrieben hat
        for (int i = 0; i < count; i++) {
            ParticleState p = new ParticleState(seg, i);
            assertEquals(i,         p.getId());
            assertEquals(i * 1.0f,  p.getX(),  1e-6f);
            assertEquals(i * -1.0f, p.getY(),  1e-6f);
        }
    }

    @Test
    void byteSizeIsCorrect() {
        assertEquals(40L, ParticleState.BYTE_SIZE);
    }
}
