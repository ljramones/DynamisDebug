# DynamisDebug

Canonical debug/diagnostic coordination layer for the Dynamis engine. DynamisDebug normalizes, records, queries, and visualizes diagnostic state produced by subsystems across the engine.

DynamisDebug does **not** own subsystem telemetry generation, rendering implementation, UI widgets, logging, or benchmarking. It provides the coordination contracts and runtime infrastructure that those systems plug into.

---

## Architecture

Four modules, strict dependency flow:

```
dynamisdebug-api      Pure contracts (no deps). Records, sealed draw commands, interfaces.
dynamisdebug-core     Runtime: DebugSession, DebugHistory, DebugEventBuffer, command registry.
dynamisdebug-draw     Renderer-agnostic debug draw: DebugDrawQueue + convenience methods.
dynamisdebug-bridge   Integration: TelemetryAdapter<T> registry for subsystem telemetry.
```

Dependency graph: `bridge → core → api`, `draw → api`.

Package root: `org.dynamisengine.debug.*`

---

## Build

```bash
cd Layer1-Foundation/DynamisDebug && mvn clean install
mvn test          # run all tests
```

Requires JDK 25 with preview features. The parent POM (`dynamis-parent`) must be installed first.

---

## Key Types

### API (`dynamisdebug-api`)

**`DebugSnapshot`** — point-in-time diagnostic state: "what is true right now?" A record containing frame number, timestamp, source name, category, numeric metrics, boolean flags, and optional text summary. Snapshots are immutable with defensive copying.

**`DebugEvent`** — transient diagnostic occurrence: "what just happened?" Captures underruns, fallbacks, spikes, failures, and transitions with frame number, severity, and message.

**`DebugSnapshotProvider`** — interface for subsystems that produce snapshots. Register with `DebugSession`; the session calls `captureSnapshot(frameNumber)` each frame.

**`DebugEventSink`** — interface for receiving events. `DebugSession` implements this.

**`DebugFlag`** / **`DebugCounter`** / **`DebugMarker`** — lightweight value types for toggles, counters, and profiling scopes. All immutable records with convenience factories.

**`DebugCommand`** / **`DebugCommandResult`** / **`DebugCommandRegistry`** — runtime debug command system. Register named commands with handlers; execute by name with arguments.

**`DebugDrawCommand`** (sealed) — renderer-agnostic draw intents. Four permitted implementations:
- `DebugLineCommand` — line between two 3D points
- `DebugBoxCommand` — wireframe box (center + half-extents)
- `DebugSphereCommand` — wireframe sphere
- `DebugTextCommand` — screen-space or world-space text

### Core (`dynamisdebug-core`)

**`DebugSession`** — central coordinator. Manages providers, events, counters, flags, and commands. Thread-safe via `ConcurrentHashMap`. Catches exceptions during frame capture to prevent debug failure from crashing the engine. Can be globally enabled/disabled.

**`DebugHistory`** — ring buffer of frame snapshots for time-series inspection. Configurable capacity.

**`DebugEventBuffer`** — thread-safe ring buffer of events. Supports drain (snapshot + clear) and non-destructive recent queries.

**`DefaultCommandRegistry`** — `ConcurrentHashMap`-backed command registry with exception-safe execution.

### Draw (`dynamisdebug-draw`)

**`DebugDrawQueue`** — frame-scoped draw command accumulator. Single-frame commands are cleared each frame; persistent commands decay by elapsed time. Renderers consume `activeCommands()` and call `endFrame(dt)`.

### Bridge (`dynamisdebug-bridge`)

**`DebugBridge`** — subsystem telemetry adapter registry. Subsystems register `TelemetryAdapter<T>` instances to convert their telemetry into unified `DebugSnapshot` format.

**`TelemetryAdapter<T>`** — generic interface: `adapt(T telemetry, frameNumber) → DebugSnapshot`.

---

## Usage

```java
// Create session
var session = new DebugSession();

// Register a snapshot provider
session.registerProvider(myPhysicsProvider);

// Capture frame snapshots
Map<String, DebugSnapshot> frame = session.captureFrame(frameNumber);

// Submit events
session.submit(new DebugEvent(frame, now, "audio", AUDIO, WARNING, "underrun", "256 samples lost"));

// Counters and flags
session.setCounter("drawCalls", RENDERING, 0);
session.incrementCounter("drawCalls");
session.setFlag("showWireframe", true);
session.toggleFlag("showWireframe");

// Commands
session.commands().register(new DebugCommand("gc", "Force GC", args -> {
    System.gc();
    return DebugCommandResult.ok("GC triggered");
}));
session.commands().execute("gc");

// Debug draw
var drawQueue = new DebugDrawQueue();
drawQueue.line(0, 0, 0, 1, 1, 1, 1, 0, 0);
drawQueue.sphere(5, 5, 5, 2, 0, 1, 0);
drawQueue.text("FPS: 60", 10, 10, 1, 1, 1);
List<DebugDrawCommand> cmds = drawQueue.activeCommands();
drawQueue.endFrame(0.016f);
```

---

## License

Apache 2.0
