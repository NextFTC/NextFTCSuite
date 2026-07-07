# Module NextFTC Robot

The robot library for NextFTC, designed to provide a robust, command-based framework for FTC robots. It features a reflection-based architecture powered by Sinister that automates OpMode registration and instantiation, reducing boilerplate and creating a seamless developer experience.

By implementing `NextRobot` and `Mechanism`, you can structure your robot codebase logically. The command-based architecture is integrated tightly with PedroPathing's Ivy `Scheduler`, enabling advanced command compositions and event-driven architectures.

# Package dev.nextftc.robot

The core robot architecture and command-based wrappers.
* **Robot Structure**: `NextRobot` represents the root of your robot, containing multiple `Mechanism` instances. `NextOpMode` is the base class for your OpModes that seamlessly injects your `NextRobot` instance.
* **Scanning & Registration**: `RobotScanner` and `RobotOpModeScanner` work alongside Sinister to automatically discover your robot implementations and register your custom OpModes to the FTC dashboard without manual `@TeleOp` or `@Autonomous` configuration clutter.
* **Event System**: `Trigger` and `EventLoop` provide a WPILib-style API for binding commands to conditional states, allowing commands to run based on arbitrary boolean conditions.

# Package dev.nextftc.robot.triggers

Predefined triggers and controller wrappers to simplify input mapping.
* **CommandGamepad**: A gamepad wrapper that exposes every physical button and joystick as a bindable `Trigger`, allowing you to seamlessly bind Ivy commands to controller inputs (e.g., `gamepad.a.onTrue(scoreCommand)`).
