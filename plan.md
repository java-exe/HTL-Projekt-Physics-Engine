# High-Performance Physics Engine – Masterplan

## Strategie: Contract-First

Das Protokoll (`protocol.txt`) ist unantastbar. Alle drei Teams (BE1, BE2, FE) bauen gegen diesen Vertrag.

---

## Architektur-Übersicht

```
┌─────────────────────┐     SharedRingBuffer     ┌──────────────────────┐
│   BE1: Engine       │ ─────────────────────── │   BE2: Network       │
│                     │                          │                      │
│  ParticleState.java │                          │  NioServer.java      │
│  PhysicsEngine.java │                          │  ProtocolHandler.java│
│  SimulationLoop.java│                          │  BufferEncoder.java  │
└─────────────────────┘                          └──────────┬───────────┘
                                                            │ WebSocket (Binary)
                                                            │
                                                 ┌──────────▼───────────┐
                                                 │   FE: Visualizer     │
                                                 │                      │
                                                 │  MockServer.js       │
                                                 │  NetworkClient.js    │
                                                 │  FrameBuffer.js      │
                                                 │  Renderer.js         │
                                                 │  Telemetrie.js       │
                                                 └──────────────────────┘
```

---

## Binary Protocol V1 (Big Endian) – Auszug aus `protocol.txt`

### Server → Client

| Offset | Größe  | Typ    | Beschreibung            |
|--------|--------|--------|-------------------------|
| 0      | 4 B    | Uint32 | Magic `0xCAFEBABE`      |
| 4      | 2 B    | Uint16 | PacketType (1=Sim, 2=Stats) |
| 6      | 2 B    | Uint16 | PayloadLength           |

**SimData Payload** (17 Bytes pro Partikel):

| Offset | Größe | Typ    | Feld           |
|--------|-------|--------|----------------|
| 0      | 4 B   | Uint32 | ID             |
| 4      | 4 B   | Float32| X              |
| 8      | 4 B   | Float32| Y              |
| 12     | 1 B   | Uint8  | R              |
| 13     | 1 B   | Uint8  | G              |
| 14     | 1 B   | Uint8  | B              |
| 15     | 1 B   | Uint8  | A              |
| 16     | 1 B   | Uint8  | CollisionFreq  |

**Stats Payload** (8 Bytes):

| Offset | Größe | Typ     | Feld    |
|--------|-------|---------|---------|
| 0      | 4 B   | Float32 | TPS     |
| 4      | 4 B   | Float32 | CPULoad |

### Client → Server (Commands)

| Offset | Größe | Typ     | Feld       |
|--------|-------|---------|------------|
| 0      | 1 B   | Uint8   | CMD_CODE   |
| 1      | 4 B   | Float32 | VALUE      |

---

## Module & Verantwortlichkeiten

### BE1 – Core Engine (`backend/engine/`)
- **`ParticleState.java`** – Off-Heap `MemorySegment` Layout, VarHandles für x/y/vx/vy/r/g/b/a
- **`PhysicsEngine.java`** – SIMD `update()` via `VectorAPI`, Gravitation, Kollision
- **`SimulationLoop.java`** – Fixed Timestep (60 Hz), schreibt in `SharedRingBuffer`
- **`SharedRingBuffer.java`** – Thread-sicherer Lock-free Ring Buffer zwischen BE1 und BE2

**Interface zu BE2:**
```java
public interface PhysicsProvider {
    MemorySegment getCurrentFrameData();
    int getParticleCount();
    void update();
}
```

### BE2 – Network Layer (`backend/network/`)
- **`NioServer.java`** – Java NIO `Selector`-Loop, non-blocking I/O
- **`ProtocolHandler.java`** – WebSocket Handshake (RFC 6455: SHA-1, Base64)
- **`BufferEncoder.java`** – Liest `MemorySegment` aus BE1, baut Binary WebSocket Frames
- **`CommandHandler.java`** – Parst eingehende Befehle vom Client

### FE – Visualizer (`frontend/src/`)
- **`MockServer.js`** – Web Worker, erzeugt protokollkonforme Binärdaten (kein Backend nötig)
- **`NetworkClient.js`** – WebSocket-Client mit `USE_MOCK`-Schalter
- **`FrameBuffer.js`** – Ring-Queue der letzten 3 Frames für LERP
- **`Renderer.js`** – Three.js `InstancedBufferGeometry`, 1 Draw-Call für alle Partikel
- **`Telemetrie.js`** – FPS/TPS/Latenz-Graphen via uPlot
- **`shaders/particle.vert.glsl`** – Custom Vertex Shader
- **`shaders/particle.frag.glsl`** – Custom Fragment Shader (Glow, runde Partikel)

---

## Sprint-Plan (9 Wochen)

| Woche | BE1 (Engine)                  | BE2 (Netzwerk)                | FE (Visualizer)                    |
|-------|-------------------------------|-------------------------------|------------------------------------|
| **1** | `ParticleState` Struct-Layout | NIO-Server Gerüst             | MockServer Aufbau + ArrayBuffer-Test |
| **2** | `PhysicsLoop` (Dummy-Daten)   | WebSocket Handshake (RFC 6455)| Renderer: 5k Partikel sichtbar     |
| **3** | SIMD Vector-Math Integration  | Binary Frame Encoding         | LERP Interpolation (60 FPS stabil) |
| **4** | `SharedRingBuffer` Logic      | Zero-Copy Pipeline            | Custom Shaders + Instancing        |
| **5** | Spatial Partitioning          | Buffer-Pooling                | Telemetrie-Graphen                 |
| **6** | API-Endpoints (Commands)      | Command-Handling              | UI Dashboard + Slider (Gravity)    |
| **7** | Performance-Tuning            | Thundering-Herd Fix           | **Merge-Day** (USE_MOCK = false)   |
| **8** | 250k Partikel Support         | Last-Tests                    | Heatmap-Modus (Shader)             |
| **9** | Doku & Reports                | Code-Cleanup                  | Präsentations-Demo-Mode            |

---

## Definition of Done

### Gesamt
- [ ] Java-Backend + JS-Frontend kommunizieren via `protocol.txt`
- [ ] 250.000+ Partikel mit stabilen 60 FPS im Browser
- [ ] Kein Memory-Leak nach 1h Dauerlauf

### BE1
- [ ] SIMD-Loop via Java Vector API aktiv
- [ ] Off-Heap Speicher (kein GC-Druck)
- [ ] Fixed-Timestep Loop bei exakt 60 Hz

### BE2
- [ ] RFC 6455 Handshake korrekt implementiert
- [ ] Binary Framing ohne Server-side Masking
- [ ] Zero-Copy: kein unnötiges Array-Kopieren

### FE
- [ ] Zero Allocation im `requestAnimationFrame`-Loop
- [ ] Max. 2 Draw-Calls für alle Partikel
- [ ] LERP: keine Ruckler bei 15 TPS Server-Rate

---

## Verzeichnisstruktur

```
HTL-Projekt-Physics-Engine/
├── plan.md                      ← Dieser Plan
├── protocol.txt                 ← Der unantastbare Vertrag
├── backend/
│   ├── engine/                  ← BE1
│   │   └── src/main/java/engine/
│   │       ├── ParticleState.java
│   │       ├── PhysicsEngine.java
│   │       ├── SimulationLoop.java
│   │       └── SharedRingBuffer.java
│   └── network/                 ← BE2
│       └── src/main/java/network/
│           ├── NioServer.java
│           ├── ProtocolHandler.java
│           ├── BufferEncoder.java
│           └── CommandHandler.java
└── frontend/
    ├── index.html
    ├── package.json
    └── src/
        ├── main.js
        ├── MockServer.js
        ├── NetworkClient.js
        ├── FrameBuffer.js
        ├── Renderer.js
        ├── Telemetrie.js
        └── shaders/
            ├── particle.vert.glsl
            └── particle.frag.glsl
```
