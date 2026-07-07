/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.geometry

/**
 * @usesMathJax
 *
 * Represents the complete state of a robot: pose, velocity, and acceleration.
 *
 * A robot state captures the full kinematic description of a robot at a specific instant:
 * - **Pose**: Where the robot is and what direction it's facing (position and orientation)
 * - **Velocity**: How fast the robot is moving (linear and angular velocities in chassis frame)
 * - **Acceleration**: How the robot's velocity is changing (linear and angular accelerations in chassis frame)
 *
 * ## Frame Convention
 *
 * - **Pose**: Always in the global (field) frame
 * - **Velocity**: In the chassis (robot-local) frame for easier control
 * - **Acceleration**: In the chassis (robot-local) frame for easier control
 *
 * ## Use Cases
 *
 * **State Estimation**: Combining sensor data (odometry, IMU) to track the robot's state:
 * ```kotlin
 * val estimatedState = RobotState(
 *     pose = odometry.getPose(),
 *     velocity = chassisVelocity,
 *     acceleration = ChassisAccelerations.zero
 * )
 * ```
 *
 * **Trajectory Following**: Comparing current state to desired state:
 * ```kotlin
 * val targetState = trajectory.sample(currentTime)
 * val positionError = targetState.pose - currentState.pose
 * val velocityError = targetState.velocity - currentState.velocity
 * ```
 *
 * **Feedforward Control**: Using acceleration for better tracking:
 * ```kotlin
 * val feedforward = kV * targetState.velocity + kA * targetState.acceleration
 * val feedback = controller.calculate(currentState.pose, targetState.pose)
 * val command = feedforward + feedback
 * ```
 *
 * ## Example Usage
 *
 * ```kotlin
 * // Create a robot state
 * val state = RobotState(
 *     pose = Pose2d(Vector2d(10.0.inches, 5.0.inches), Math.PI / 4),
 *     velocity = ChassisVelocities(
 *         linearVel = Vector2d(12.0.inchesPerSecond, 0.0.inchesPerSecond),
 *         angVel = 0.5.radiansPerSecond
 *     ),
 *     acceleration = ChassisAccelerations.zero
 * )
 *
 * // Access components
 * val currentPose = state.pose
 * val forwardVelocity = state.velocity.linearVel.x
 * ```
 *
 * @property pose the robot's pose in the global (field) frame
 * @property velocity the robot's velocity in the chassis (local) frame
 * @property acceleration the robot's acceleration in the chassis (local) frame
 * @see Pose2d for the pose representation
 * @see ChassisVelocities for velocity in the chassis frame
 * @see PoseVelocity2d for velocity in the global frame
 */
data class RobotState(
  @JvmField val pose: Pose2d,
  @JvmField val velocity: ChassisVelocities,
  @JvmField val acceleration: ChassisAccelerations,
)
