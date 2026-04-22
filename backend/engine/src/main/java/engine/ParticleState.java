package engine;

import java.lang.foreign.*;

/**
 * Definiert das Off-Heap Memory Layout eines einzelnen Partikels.
 * Layout (40 Bytes): x(4) y(4) vx(4) vy(4) r(1) g(1) b(1) a(1) id(4) collisionFreq(1) + padding(15)
 *
 * Kein GC-Druck: Alle Daten leben im nativen Speicher (MemorySegment).
 * Zugriff via MemorySegment.get/set (Java 21 FFM API, JEP 454).
 */
public class ParticleState {

    // Gesamtgröße eines Partikel-Eintrags im Off-Heap-Speicher
    public static final long BYTE_SIZE = 40L;

    // Offsets innerhalb eines Partikels
    public static final long OFFSET_X              = 0L;
    public static final long OFFSET_Y              = 4L;
    public static final long OFFSET_VX             = 8L;
    public static final long OFFSET_VY             = 12L;
    public static final long OFFSET_R              = 16L;
    public static final long OFFSET_G              = 17L;
    public static final long OFFSET_B              = 18L;
    public static final long OFFSET_A              = 19L;
    public static final long OFFSET_ID             = 20L;
    public static final long OFFSET_COLLISION_FREQ = 24L;
    // Bytes 25-39: Padding / reserviert für zukünftige Felder

    private final MemorySegment segment;
    private final long baseOffset;

    public ParticleState(MemorySegment segment, long index) {
        this.segment = segment;
        this.baseOffset = index * BYTE_SIZE;
    }

    // --- Getter ---

    public float getX()  { return segment.get(ValueLayout.JAVA_FLOAT, baseOffset + OFFSET_X); }
    public float getY()  { return segment.get(ValueLayout.JAVA_FLOAT, baseOffset + OFFSET_Y); }
    public float getVx() { return segment.get(ValueLayout.JAVA_FLOAT, baseOffset + OFFSET_VX); }
    public float getVy() { return segment.get(ValueLayout.JAVA_FLOAT, baseOffset + OFFSET_VY); }
    public byte  getR()  { return segment.get(ValueLayout.JAVA_BYTE,  baseOffset + OFFSET_R); }
    public byte  getG()  { return segment.get(ValueLayout.JAVA_BYTE,  baseOffset + OFFSET_G); }
    public byte  getB()  { return segment.get(ValueLayout.JAVA_BYTE,  baseOffset + OFFSET_B); }
    public byte  getA()  { return segment.get(ValueLayout.JAVA_BYTE,  baseOffset + OFFSET_A); }
    public int   getId() { return segment.get(ValueLayout.JAVA_INT,   baseOffset + OFFSET_ID); }
    public byte  getCollisionFreq() { return segment.get(ValueLayout.JAVA_BYTE, baseOffset + OFFSET_COLLISION_FREQ); }

    // --- Setter ---

    public void setX(float v)  { segment.set(ValueLayout.JAVA_FLOAT, baseOffset + OFFSET_X,  v); }
    public void setY(float v)  { segment.set(ValueLayout.JAVA_FLOAT, baseOffset + OFFSET_Y,  v); }
    public void setVx(float v) { segment.set(ValueLayout.JAVA_FLOAT, baseOffset + OFFSET_VX, v); }
    public void setVy(float v) { segment.set(ValueLayout.JAVA_FLOAT, baseOffset + OFFSET_VY, v); }
    public void setR(byte v)   { segment.set(ValueLayout.JAVA_BYTE,  baseOffset + OFFSET_R,  v); }
    public void setG(byte v)   { segment.set(ValueLayout.JAVA_BYTE,  baseOffset + OFFSET_G,  v); }
    public void setB(byte v)   { segment.set(ValueLayout.JAVA_BYTE,  baseOffset + OFFSET_B,  v); }
    public void setA(byte v)   { segment.set(ValueLayout.JAVA_BYTE,  baseOffset + OFFSET_A,  v); }
    public void setId(int v)   { segment.set(ValueLayout.JAVA_INT,   baseOffset + OFFSET_ID, v); }
    public void setCollisionFreq(byte v) { segment.set(ValueLayout.JAVA_BYTE, baseOffset + OFFSET_COLLISION_FREQ, v); }
}
