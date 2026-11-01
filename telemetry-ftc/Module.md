# Module NextFTC Telemetry FTC

FTC lifecycle integration for the `telemetry` module, for teams that want logging and live telemetry
without using the rest of NextFTC. It doesn't depend on `robot` or `hardware`, and it needs no
OpMode base class: depending on it is enough.

For each OpMode, `FtcTelemetry` opens a WPILOG in `/sdcard/FIRST/nextftc/logs` and, if enabled,
publishes the same values over a NetworkTables 4 server on port 5810 for AdvantageScope. Both are
closed when the OpMode stops. Log from anywhere with `dev.nextftc.telemetry.Telemetry`.

Values also show on the Driver Station, one line per path. Call `FtcTelemetry.update()` every loop of
a `LinearOpMode` to send them, because the SDK only updates telemetry by itself after each loop of
an iterative `OpMode`. NextFTC's `NextOpMode` does it for you.

```kotlin
// Optional, e.g. in a static initializer or the first OpMode's init
FtcTelemetry.config.serverPort = 5810

// In any OpMode
Telemetry.log("Drive/Pose", pose)
```

The hook is found by Sloth when the event loop is created, so there is nothing to register.

# Package dev.nextftc.telemetry.ftc

`FtcTelemetry`, the lifecycle hook, and `FtcTelemetryConfig`, its settings.
