package network;

import engine.PhysicsEngine;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;

/**
 * Parst eingehende Binär-Befehle vom Client (Client → Server Protokoll).
 * Format: [1 Byte CMD_CODE] [4 Bytes VALUE (Float32, Big Endian)]
 */
public class CommandHandler {

    // CMD_CODES (aus protocol.txt)
    private static final byte CMD_SET_GRAVITY        = 0x01;
    private static final byte CMD_SET_FRICTION       = 0x02;
    private static final byte CMD_RESET_SIM          = 0x03;
    private static final byte CMD_SET_PARTICLE_COUNT = 0x04;

    private final PhysicsEngine engine;

    public CommandHandler(PhysicsEngine engine) {
        this.engine = engine;
    }

    public void onBinaryCommand(SocketChannel source, ByteBuffer payload) {
        if (payload.remaining() < 5) return;

        payload.order(ByteOrder.BIG_ENDIAN);
        byte cmdCode = payload.get();
        float value  = payload.getFloat();

        switch (cmdCode) {
            case CMD_SET_GRAVITY  -> { engine.setGravity(value);  System.out.println("[Cmd] Gravity = " + value); }
            case CMD_SET_FRICTION -> { engine.setFriction(value); System.out.println("[Cmd] Friction = " + value); }
            case CMD_RESET_SIM   -> System.out.println("[Cmd] Reset (TODO)");
            default -> System.out.println("[Cmd] Unknown command: 0x" + Integer.toHexString(cmdCode));
        }
    }
}
