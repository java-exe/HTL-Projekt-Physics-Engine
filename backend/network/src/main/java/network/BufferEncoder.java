package network;

import engine.ParticleState;
import engine.SharedRingBuffer;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
 * BE2: Liest Frames aus dem SharedRingBuffer und baut WebSocket Binary Frames.
 * Protokoll: protocol.txt – SimData (PacketType = 1).
 *
 * Kein Server-Side Masking (RFC 6455: nur Client maskiert).
 */
public class BufferEncoder {

    private static final int    MAGIC       = 0xCAFEBABE;
    private static final short  TYPE_SIM    = 1;
    private static final short  TYPE_STATS  = 2;
    private static final int    HEADER_SIZE = 8;          // Magic(4) + Type(2) + PayloadLen(2)
    private static final int    PARTICLE_WIRE_SIZE = 17;  // Bytes pro Partikel im Protokoll

    private final SharedRingBuffer ringBuffer;

    public BufferEncoder(SharedRingBuffer ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    /**
     * Sendet den aktuellen SimData-Frame an alle verbundenen Clients.
     */
    public void broadcastFrame(Set<SocketChannel> clients) {
        if (clients.isEmpty()) return;

        MemorySegment frame = ringBuffer.readLatest();
        if (frame == null) return;

        int count = ringBuffer.getLatestParticleCount();
        ByteBuffer ws = buildSimDataFrame(frame, count);

        for (SocketChannel client : clients) {
            try {
                ws.rewind();
                client.write(ws);
            } catch (IOException e) {
                // Wird vom NioServer beim nächsten read() erkannt
            }
        }
    }

    private ByteBuffer buildSimDataFrame(MemorySegment frame, int particleCount) {
        int payloadLen = particleCount * PARTICLE_WIRE_SIZE;
        // WebSocket Binary Frame: FIN=1, opcode=2, no mask, 16-bit length
        // Frame: [0x82][len_b1][len_b2][header][payload]
        int wsHeaderLen = (payloadLen + HEADER_SIZE > 125) ? 4 : 2;
        ByteBuffer buf = ByteBuffer.allocate(wsHeaderLen + HEADER_SIZE + payloadLen)
                .order(ByteOrder.BIG_ENDIAN);

        // WebSocket framing
        buf.put((byte) 0x82); // FIN + binary
        if (wsHeaderLen == 4) {
            buf.put((byte) 126);
            buf.putShort((short)(HEADER_SIZE + payloadLen));
        } else {
            buf.put((byte)(HEADER_SIZE + payloadLen));
        }

        // Protokoll Header
        buf.putInt(MAGIC);
        buf.putShort(TYPE_SIM);
        buf.putShort((short) payloadLen);

        // Partikel-Payload: ID(4) X(4) Y(4) R(1) G(1) B(1) A(1) CF(1) = 17 Bytes
        for (int i = 0; i < particleCount; i++) {
            long base = i * ParticleState.BYTE_SIZE;
            buf.putInt(   frame.get(ValueLayout.JAVA_INT,   base + 20)); // ID
            buf.putFloat( frame.get(ValueLayout.JAVA_FLOAT, base + 0));  // X
            buf.putFloat( frame.get(ValueLayout.JAVA_FLOAT, base + 4));  // Y
            buf.put(      frame.get(ValueLayout.JAVA_BYTE,  base + 16)); // R
            buf.put(      frame.get(ValueLayout.JAVA_BYTE,  base + 17)); // G
            buf.put(      frame.get(ValueLayout.JAVA_BYTE,  base + 18)); // B
            buf.put(      frame.get(ValueLayout.JAVA_BYTE,  base + 19)); // A
            buf.put(      frame.get(ValueLayout.JAVA_BYTE,  base + 24)); // CollisionFreq
        }

        buf.flip();
        return buf;
    }
}
