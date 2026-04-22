package network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BE2: Java NIO Selector-Loop.
 * Nimmt eingehende TCP-Verbindungen entgegen und delegiert an ProtocolHandler.
 */
public class NioServer {

    private final int port;
    private final Set<SocketChannel> connectedClients = ConcurrentHashMap.newKeySet();

    private Selector selector;
    private ServerSocketChannel serverChannel;
    private volatile boolean running = false;

    // Callbacks
    private final ProtocolHandler protocolHandler;

    public NioServer(int port, ProtocolHandler protocolHandler) {
        this.port = port;
        this.protocolHandler = protocolHandler;
    }

    public void start() throws IOException {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        running = true;

        System.out.println("[NioServer] Listening on ws://localhost:" + port);
        selectorLoop();
    }

    private void selectorLoop() throws IOException {
        ByteBuffer readBuffer = ByteBuffer.allocateDirect(4096);

        while (running) {
            selector.select(100); // 100ms Timeout
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> it = keys.iterator();

            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();

                if (!key.isValid()) continue;

                if (key.isAcceptable()) {
                    accept(key);
                } else if (key.isReadable()) {
                    read(key, readBuffer);
                }
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        if (client == null) return;

        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ,
                new ClientState(ClientState.State.HANDSHAKE_PENDING));
        connectedClients.add(client);
        System.out.println("[NioServer] New connection: " + client.getRemoteAddress());
    }

    private void read(SelectionKey key, ByteBuffer buffer) {
        SocketChannel client = (SocketChannel) key.channel();
        ClientState state = (ClientState) key.attachment();
        buffer.clear();

        try {
            int bytesRead = client.read(buffer);
            if (bytesRead == -1) {
                disconnect(key, client);
                return;
            }
            buffer.flip();
            protocolHandler.handle(client, state, buffer);
        } catch (IOException e) {
            disconnect(key, client);
        }
    }

    private void disconnect(SelectionKey key, SocketChannel client) {
        key.cancel();
        connectedClients.remove(client);
        try { client.close(); } catch (IOException ignored) {}
        System.out.println("[NioServer] Client disconnected");
    }

    public Set<SocketChannel> getConnectedClients() {
        return connectedClients;
    }

    public void stop() {
        running = false;
        try { selector.close(); } catch (IOException ignored) {}
        try { serverChannel.close(); } catch (IOException ignored) {}
    }
}
