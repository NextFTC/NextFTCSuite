# Module NextFTC Telemetry FTC

FTC lifecycle integration for the `telemetry` module, for teams that want logging and live telemetry
without using the rest of NextFTC. It doesn't depend on `robot` or `hardware`, and it needs no
OpMode base class. Adding the dependency is enough.

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

While the server is running, it also serves your logs. `/download` is a page that lists every
log with its OpMode name and start time, newest first. On a Control Hub that is
`http://192.168.43.1:5810/download`, and on a Robot Controller phone it is
`http://192.168.49.1:5810/download` over Wi-Fi Direct. `/logs` returns the same list as JSON, and
`/logs/<file name>` downloads one file. A log that is still being written is listed but can't be
downloaded until its OpMode stops.

Two utility OpModes, "Disable NextFTC Telemetry Server" and "Enable NextFTC Telemetry Server", turn
the NetworkTables server off for competition and back on afterward. The choice is saved on the Robot
Controller, so it holds after the app restarts. `FtcTelemetry.setServerEnabled` does the same from
code. The log file and Driver Station lines are unaffected. Settings in `FtcTelemetry.config` are not
saved. They come from your code each time.

The hook is found by Sloth when the event loop is created, so there is nothing to register.

# Package dev.nextftc.telemetry.ftc

`FtcTelemetry`, the lifecycle hook, and `FtcTelemetryConfig`, its settings.
