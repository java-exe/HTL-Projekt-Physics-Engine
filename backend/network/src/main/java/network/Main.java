package network;

import engine.PhysicsEngine;
import engine.SharedRingBuffer;
import engine.SimulationLoop;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Einstiegspunkt: Startet BE1 + BE2 zusammen.
 * BE1 schreibt in SharedRingBuffer, BE2 liest und sendet per WebSocket.
 */
public class Main {

    private static final int PORT           = 8080;
    private static final int PARTICLE_COUNT = 10_000;
    private static final int BROADCAST_HZ   = 60;

    public static void main(String[] args) throws IOException, InterruptedException {
        // BE1: Engine aufsetzen
        SharedRingBuffer ringBuffer = new SharedRingBuffer();
        PhysicsEngine    engine     = new PhysicsEngine(PARTICLE_COUNT);
        SimulationLoop   simLoop    = new SimulationLoop(engine, ringBuffer);

        // BE2: Netzwerk aufsetzen
        CommandHandler  commandHandler  = new CommandHandler(engine);
        ProtocolHandler protocolHandler = new ProtocolHandler(commandHandler);
        NioServer       server          = new NioServer(PORT, protocolHandler);
        BufferEncoder   encoder         = new BufferEncoder(ringBuffer);

        // Broadcast-Loop: sendet aktuellen Frame an alle Clients
        ScheduledExecutorService broadcaster = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "broadcaster");
            t.setDaemon(true);
            return t;
        });
        broadcaster.scheduleAtFixedRate(
            () -> encoder.broadcastFrame(server.getConnectedClients()),
            0, 1_000_000_000L / BROADCAST_HZ, TimeUnit.NANOSECONDS
        );

        // Simulation starten
        simLoop.start();

        // NIO-Server blockiert hier (Selector-Loop)
        System.out.println("[BE2] Server starting on port " + PORT);
        server.start(); // blockierend
    }
}
