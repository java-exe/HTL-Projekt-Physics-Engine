package network;

/**
 * Hält den Verbindungszustand eines einzelnen Clients.
 * Wird als Attachment am SelectionKey gespeichert.
 */
public class ClientState {

    public enum State {
        HANDSHAKE_PENDING,  // TCP verbunden, WebSocket Handshake noch ausstehend
        CONNECTED           // WebSocket Handshake abgeschlossen, bereit für Frames
    }

    private State state;

    public ClientState(State initialState) {
        this.state = initialState;
    }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public boolean isConnected() {
        return state == State.CONNECTED;
    }
}
