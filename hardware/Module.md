# Module NextFTC Hardware

NextFTC's hardware layer. It wraps the raw FTC SDK hardware classes — motors, servos, IMUs,
distance/color/digital sensors, odometry computers, and vision sensors like Limelight and
HuskyLens — in Kotlin classes that lazily resolve devices from the hardware map or Lynx modules on
first use, instead of requiring manual `hardwareMap.get(...)` calls everywhere.

The root package holds shared infrastructure (`RobotController`, `LazyHardware`, `Caching`,
`AnalogFeedback`) that centralizes hardware-map/event-loop access and avoids redundant hardware
reads and writes. Several wrappers lean on the `control` module directly: `NextMotor` uses its
PID/feedforward controllers for closed-loop control, and `NextIMU`, `NextPinpoint`, and
`NextLimelight` return its `Pose2d`/`Rotation2d` geometry types.

# Package dev.nextftc.hardware

Shared infrastructure: `RobotController`, a singleton for accessing the hardware map and
control/expansion hubs; `LazyHardware`, a delegate that initializes a device on first access;
`Caching`, a delegate that skips a hardware write if the value hasn't changed; and
`AnalogFeedback`, a delegate that converts an analog voltage reading into an `Angle`.

# Package dev.nextftc.hardware.actuators

Output device wrappers: `NextMotor` (throttle, voltage, PID position, and PID+feedforward
velocity control modes), `NextServo`/`NextCRServo` and their feedback-enabled variants
(`NextFeedbackServo`/`NextFeedbackCRServo`), and `RGBHeadlight` for the goBILDA PWM RGB headlight.

# Package dev.nextftc.hardware.sensors

Input device wrappers: `NextColorDistanceSensor`, `NextDigitalSensor`, `NextDistanceSensor`,
`NextIMU`, and `NextPinpoint` (goBilda odometry computer) — the latter two return `control` module
geometry types (`Pose2d`, `Rotation2d`, `PoseVelocity2d`).

# Package dev.nextftc.hardware.sensors.colors

Color-matching types used by `NextColorDistanceSensor`: `NextColor`, an immutable RGB/HSV value,
and `ColorProfile`, which defines a target color and tolerance for matching sensor readings.

# Package dev.nextftc.hardware.webcams

Vision sensor wrappers: `NextHuskyLens` (DFRobot HuskyLens block/arrow detection) and
`NextLimelight` (Limelight3A telemetry, AprilTag distance, and pose estimation via `Pose2d`).
