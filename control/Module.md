# Module NextControl

A WPIMath-style control theory library for FTC robots, built on `units` for dimensioned
measurements and `linalg` for matrix/vector math. It has the usual closed-loop building blocks:
feedback controllers, feedforward models, signal filters, motion profiling, and typed 2D geometry
for tracking robot pose and motion.

The `model` and `util` packages hold the state-space plumbing underneath — linear plant models,
continuous-to-discrete conversion, Riccati equation solving, and interpolation — that the feedback
and filter classes depend on. In practice you model a system, estimate or filter its state, and
drive it toward a target with feedback plus feedforward, all in typed units and geometry.

# Package dev.nextftc.control.feedback

Feedback controllers: `PIDController` for classic PID, and `LQRController`, which solves the
discrete algebraic Riccati equation to find an optimal gain matrix.

# Package dev.nextftc.control.feedforward

Feedforward models: `SimpleFeedforward` for basic velocity/acceleration feedforward, and
`ElevatorFeedforward`/`ArmFeedforward` for gravity-compensated linear and rotational mechanisms.

# Package dev.nextftc.control.filters

Signal filters: `EMAFilter` (exponential moving average), `MedianFilter` (moving-window median),
`SlewRateLimiter` (rate-of-change limiting), and `KalmanFilter` (fuses a `LinearModel` with noisy
measurements for optimal state estimation).

# Package dev.nextftc.control.geometry

Typed 2D geometry for robot pose and motion: `Vector2d`, `Rotation2d`, `Pose2d`, `Twist2d`, and
`Transform2d`, along with local/global velocity and acceleration pairs
(`ChassisVelocities`/`PoseVelocity2d`, `ChassisAccelerations`/`PoseAcceleration2d`) and the
`RobotState` snapshot.

# Package dev.nextftc.control.model

State-space plant types: the generic `Model` interface, the discretized `LinearModel` used by
`LQRController` and `KalmanFilter`, and `MotionState`, the typed kinematic state shared by PID,
feedforward, and profiling code.

# Package dev.nextftc.control.profiles

Motion profiling: `TrapezoidProfile` builds a trapezoidal accelerate/cruise/decelerate trajectory
between two `MotionState`s, constrained by `TrapezoidProfileConstraints`.

# Package dev.nextftc.control.util

Math utilities shared across the module: state-space discretization, a DARE solver plus
cost/covariance matrix helpers for `LQRController`/`KalmanFilter`, and interpolation utilities
including `InterpolatingMap`/`InterpolatingMap2D` and linear/spline/bilinear lerp functions.
