package network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BE2: Implementiert den WebSocket-Handshake (RFC 6455).
 * Leitet danach eingehende Frames an CommandHandler weiter.
 */
public class ProtocolHandler {

    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final Pattern KEY_PATTERN = Pattern.compile("Sec-WebSocket-Key: (.+)");
    private static final byte[] PONG = new byte[]{(byte)0x8A, 0x00}; // Pong frame

    private final CommandHandler commandHandler;

    public ProtocolHandler(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    public void handle(SocketChannel client, ClientState state, ByteBuffer data) throws IOException {
        if (state.getState() == ClientState.State.HANDSHAKE_PENDING) {
            performHandshake(client, state, data);
        } else {
            parseWebSocketFrame(client, data);
        }
    }

    private void performHandshake(SocketChannel client, ClientState state, ByteBuffer data) throws IOException {
        String request = StandardCharsets.UTF_8.decode(data).toString();
        Matcher m = KEY_PATTERN.matcher(request);
        if (!m.find()) return;

        String key = m.group(1).trim();
        String acceptKey = computeAcceptKey(key);

        String response = "HTTP/1.1 101 Switching Protocols\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";

        client.write(ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8)));
        state.setState(ClientState.State.CONNECTED);
        System.out.println("[ProtocolHandler] Handshake OK for " + client.getRemoteAddress());
    }

    private String computeAcceptKey(String key) {
        try {
            String concat = key + WEBSOCKET_GUID;
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(concat.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 not available", e);
        }
    }

    /**
     * Minimal WebSocket Frame Parser (RFC 6455).
     * Unterstützt: Text/Binary/Ping/Close Frames (un-masked vom Client wird erwartet).
     */
    private void parseWebSocketFrame(SocketChannel client, ByteBuffer data) throws IOException {
        if (data.remaining() < 2) return;

        byte b0 = data.get();
        byte b1 = data.get();

        int opcode = b0 & 0x0F;
        boolean masked = (b1 & 0x80) != 0;
        int payloadLen = b1 & 0x7F;

        // Nur Einzel-Frame-Messages (kein Fragmentation-Support in Sprint 1)
        if (payloadLen > 125) return; // TODO: 16/64-bit extended length

        byte[] mask = new byte[4];
        if (masked) {
            data.get(mask);
        }

        byte[] payload = new byte[payloadLen];
        data.get(payload);

        if (masked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= mask[i % 4];
            }
        }

        switch (opcode) {
            case 0x2 -> commandHandler.onBinaryCommand(client, ByteBuffer.wrap(payload));
            case 0x8 -> client.close(); // Close frame
            case 0x9 -> client.write(ByteBuffer.wrap(PONG)); // Ping → Pong
        }
    }
}
