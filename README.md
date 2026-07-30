# NextFTC Suite v2

NextFTC Suite is a collection of Kotlin libraries for FIRST Tech Challenge robot code: control
theory (PID/LQR/feedforward/filters/motion profiling), typed 2D geometry, a compile-time-checked
linear algebra library, type-safe physical units, and a command-based robot framework with
reflection-driven OpMode registration. Everything is written against the FTC SDK and is meant to
drop into a normal `TeamCode` project.

Project documentation including installation instructions, tutorials, and reference materials can be found at 
[our website](https://beta.nextftc.dev/).
Full API documentation is generated with Dokka and published from this repository's `main` branch
to [kdoc.nextftc.dev](https://kdoc.nextftc.dev/).

## Modules

| Module                           | Description                                                                                                                                              |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| [`units`](units/Module.md)       | Type-safe physical units (distance, time, mass, force, voltage, angle, etc.) modeled after WPILib's units library.                                       |
| [`linalg`](linalg/Module.md)     | A Kotlin wrapper around EJML with `Nat` phantom types, so matrix/vector dimension mismatches are caught at compile time.                                 |
| [`control`](control/Module.md)   | Feedback controllers (PID, LQR), feedforward models, signal filters, motion profiling, and typed 2D geometry (`Pose2d`, `Rotation2d`, etc.).             |
| [`hardware`](hardware/Module.md) | Kotlin wrappers around FTC SDK hardware (motors, servos, sensors, IMUs, odometry computers, vision) that lazily resolve from the hardware map.           |
| [`robot`](robot/Module.md)       | A command-based robot framework (`NextRobot`/`Mechanism`/`NextOpMode`) with reflection-based OpMode registration, built on PedroPathing's Ivy scheduler. |

`control`, `linalg`, and parts of `units` are heavily adapted from and inspired by
[WPILib](https://github.com/wpilibsuite/allwpilib); the geometry classes are adapted from
[RoadRunner](https://github.com/acmerobotics/road-runner). See [Acknowledgments](#acknowledgments)
below.

## Adding NextFTC Suite to your robot project

See the installation guide on the [NextFTC website](https://beta.nextftc.dev/).

## Building locally

This project uses [Gradle](https://gradle.org/) with the Gradle Wrapper (`./gradlew`), a version
catalog ([`gradle/libs.versions.toml`](gradle/libs.versions.toml)), and both a build cache and a
configuration cache (see [`gradle.properties`](gradle.properties)).

* Run `./gradlew build` to build all modules.
* Run `./gradlew check` to run all checks, including tests and Spotless formatting.
* Run `./gradlew test` to run tests only.
* Run `./gradlew dokkaGenerate` to generate API documentation locally.
* Run `./gradlew clean` to clean all build outputs.

## Acknowledgments
A significant portion of the control, math, and filter logic (including `control`, `linalg`, and portions of `units`) is heavily adapted from and inspired by [WPILib](https://github.com/wpilibsuite/allwpilib). Additionally, a lot of the geometry classes are adapted from [RoadRunner](https://github.com/acmerobotics/road-runner).

We are incredibly grateful to the WPILib and RoadRunner contributors. Their adapted work remains under their original licenses, which can be found in [EXTERNAL-LICENSES.md](EXTERNAL-LICENSES.md).
