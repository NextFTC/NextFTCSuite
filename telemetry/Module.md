# Module NextFTC Telemetry

Logging and live telemetry for FTC robot code, in formats AdvantageScope understands. It has no FTC
SDK or Android dependency, so it runs in unit tests or a desktop tool as easily as on a Robot
Controller, and it doesn't depend on the other NextFTC modules.

Robot code logs to `Telemetry`, a table, or a typed handle, and the values go to whatever backends
are registered: a WPILOG file, a NetworkTables 4 server, or a mock for tests. A model of WPILib's
struct serialization lets typed values like poses be logged in a layout AdvantageScope recognizes.

The NetworkTables server runs on [Ktor](https://ktor.io)'s CIO engine, and the module also uses
`msgpack-core`, `kotlinx-serialization-json`, and `kotlin-reflect`.

To see it working without a robot, run `./gradlew :telemetry:simulateLive`. It plays a simulated Auto
and TeleOp, serves NetworkTables 4 on port 5810 for AdvantageScope to connect to at `localhost`, and
writes a log for each OpMode to `telemetry/build/sim-logs`. Pass options such as
`-PsimArgs="--auto 5 --teleop 5 --topics 100"`. The source, in `src/simulator`, shows how the pieces
are wired together.

Timestamps are microseconds since an origin, measured on a `TimeSource.WithComparableMarks`. Give the
registry, the log writer, and the server the same source and origin and they agree on every
timestamp, so a value means the same moment in the log and on the wire.

# Package dev.nextftc.telemetry

`Telemetry`, the entry point, which wraps a shared `TelemetryRegistry`. Log by name with
`Telemetry.log("Drive/Pose", pose)`, or hold a typed handle for the fast path:
`DoubleTopic`, `IntTopic`, `LongTopic`, `FloatTopic` and `BooleanTopic` for numbers and booleans, and
`Topic<T>` for strings, arrays, structs, and lists of structs. `TelemetryTable` groups related
topics under a path, and `TelemetryLoggable` lets an object publish its own fields.

The registry stamps each value, sends it to every registered backend (or only those registered for
a path prefix), and reports problems through a warning handler instead of throwing. A path keeps one
type while a backend is registered, repeated numbers and strings are skipped unless a handle's
`keepDuplicates` is set, and a handle made before any backend exists starts working when one is
registered.

A handle can carry properties, the first being a unit that dashboards such as AdvantageScope show and
convert: `table.doubleTopic("wheelVelocity").withUnit("m/s")`. They go out when the path is first
published, as the `properties` of the NetworkTables announcement and as the metadata of the log
entry, laid out the way WPILib logs NetworkTables topics. Values are strings, booleans, or finite
numbers.

`TelemetryBackend` and `TelemetryEntry` are what a destination implements.

# Package dev.nextftc.telemetry.backend

The backends: `WPILogBackend` writes a WPILOG, `NT4Backend` publishes to an `NT4Server`, and
`MockTelemetryBackend` records what was logged so a test can check it.

# Package dev.nextftc.telemetry.wpilog

`WPILogWriter`, which writes the [WPILOG](https://github.com/wpilibsuite/allwpilib/blob/main/wpiutil/doc/datalog.adoc)
format to any `OutputStream`: entries, primitive and array values, and struct values with their
schema entries. `WPILogType` names the types of the non-struct values.

# Package dev.nextftc.telemetry.nt

`NT4Server`, a [NetworkTables 4](https://github.com/wpilibsuite/allwpilib/blob/main/ntcore/doc/networktables4.adoc)
server that dashboards such as AdvantageScope and Glass connect to. Publish a topic, set values on
it, and subscribed clients are sent the topics they asked for, their current values, and every
change after that. `NT4Type` names the types of the values it can publish.

It is publish-only: clients can read but not write. It supports the 4.1 and 4.0 protocols and the
RTT subprotocol, answers the clock-synchronization messages clients use, and publishes struct schemas
as the `/.schema/` topics AdvantageScope reads. A client that falls too far behind is disconnected so
it can reconnect and catch up, instead of the robot waiting on it. It sends every change whatever a
subscription's `periodic` option says, and it has no `$` meta topics.

A topic name keeps its type for as long as the server object exists, even after the topic is
unpublished. Dashboards such as AdvantageScope key a field by name and keep its first type, and show
a "Conflicting types" error for values of another, so the server refuses the new type and telemetry
warns. A name can be published again with the type it had, which is what each OpMode does when its
backend publishes the same topics again.

# Package dev.nextftc.telemetry.struct

`StructSchema`, the description of a WPILib struct (type name, fixed size, schema text, nested
types, and a packer), and `structSchema`, a builder for defining one by hand.
`StructSchemaRegistry` builds schemas by reflecting over a class's properties, or a Java class's
public fields, getters, or record components, with `StructName`, `StructIgnore`, `StructSize` and
`StructOrder` to adjust the result. The same schema works for both the log writer and the server.
