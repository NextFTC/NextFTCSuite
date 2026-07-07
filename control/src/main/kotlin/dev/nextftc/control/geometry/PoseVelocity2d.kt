/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.geometry

import dev.nextftc.units.InchesPerSecond
import dev.nextftc.units.RadiansPerSecond
import dev.nextftc.units.measuretypes.AngularVelocity
import dev.nextftc.units.unittypes.DistanceUnit
import dev.nextftc.units.unittypes.PerUnit
import dev.nextftc.units.unittypes.TimeUnit

/**
 * @usesMathJax
 *
 * Represents the velocity of a 2D pose in the global (field) frame.
 *
 * A pose velocity describes how a pose changes over time and consists of:
 * - **Linear velocity**: \(\mathbf{v}\) - the velocity of the position in field coordinates
 * - **Angular velocity**: \(\omega\) - the rate of change of heading (frame-invariant)
 *
 * ## PoseVelocity2d vs ChassisVelocities
 *
 * **PoseVelocity2d** represents velocities in the **global (field) frame**:
 * - The x-axis and y-axis are fixed to the field
 * - Linear velocities are expressed in field coordinates
 * - Natural for trajectory planning and global navigation
 * - Used in Lie algebra operations and pose derivatives
 *
 * **ChassisVelocities** represents velocities in the **robot's local frame** (body frame):
 * - The x-axis points forward, y-axis points left (from robot's perspective)
 * - Linear velocities are relative to the robot's orientation
 * - Natural for robot control and motor commands
 * - Used in odometry and state estimation
 *
 * ## Relationship to Twists
 *
 * A pose velocity is the time derivative of a pose and lives in the tangent space
 * of SE(2), also known as the Lie algebra \(\mathfrak{se}(2)\).
 *
 * For a pose \(g(t)\), the pose velocity is \(\dot{g}(t) = (\mathbf{v}, \omega)\).
 *
 * ## Frame Conversion
 *
 * To convert from chassis (local) to pose (global) velocities:
 * ```kotlin
 * val poseVel = PoseVelocity2d(
 *     linearVel = currentPose.heading * chassisVel.linearVel,
 *     angVel = chassisVel.angVel  // Angular velocity is frame-invariant
 * )
 * ```
 *
 * To convert from pose (global) to chassis (local) velocities:
 * ```kotlin
 * val chassisVel = ChassisVelocities(
 *     linearVel = currentPose.heading.inverse() * poseVel.linearVel,
 *     angVel = poseVel.angVel
 * )
 * ```
 *
 * ## Example Usage
 *
 * ```kotlin
 * // Robot moving at 10 in/s in the field's x-direction, 5 in/s in y-direction
 * val poseVel = PoseVelocity2d(
 *     linearVel = Vector2d(10.0.inchesPerSecond, 5.0.inchesPerSecond),
 *     angVel = 0.5.radiansPerSecond
 * )
 *
 * // Transform to robot's local frame
 * val chassisVel = ChassisVelocities(
 *     linearVel = pose.heading.inverse() * poseVel.linearVel,
 *     angVel = poseVel.angVel
 * )
 * ```
 *
 * @property linearVel linear velocity in the global (field) frame
 * @property angVel angular velocity (frame-invariant)
 * @see ChassisVelocities for velocities in the robot's local frame
 */
data class PoseVelocity2d(
  @JvmField val linearVel: Vector2d<PerUnit<DistanceUnit, TimeUnit>>,
  @JvmField val angVel: AngularVelocity,
) {
  /**
   * Adds two pose velocities component-wise.
   *
   * @param pv the pose velocity to add
   * @return the sum of the velocities
   */
  operator fun plus(pv: PoseVelocity2d) = PoseVelocity2d(
    linearVel + pv.linearVel,
    angVel + pv.angVel,
  )

  /**
   * Subtracts two pose velocities component-wise.
   *
   * @param pv the pose velocity to subtract
   * @return the difference of the velocities
   */
  operator fun minus(pv: PoseVelocity2d) = PoseVelocity2d(
    linearVel - pv.linearVel,
    angVel - pv.angVel,
  )

  /**
   * Negates the pose velocity.
   *
   * @return the negated velocity
   */
  operator fun unaryMinus() = PoseVelocity2d(-linearVel, -angVel)

  /**
   * Multiplies the pose velocity by a scalar.
   *
   * @param scalar the scalar multiplier
   * @return the scaled velocity
   */
  operator fun times(scalar: Double) = PoseVelocity2d(linearVel * scalar, angVel * scalar)

  /**
   * Divides the pose velocity by a scalar.
   *
   * @param scalar the scalar divisor
   * @return the scaled velocity
   */
  operator fun div(scalar: Double) = PoseVelocity2d(linearVel / scalar, angVel / scalar)

  /**
   * Converts this pose velocity (global frame) to a chassis velocity (local frame).
   *
   * Transforms the linear velocity from the global field frame to the robot's local frame
   * using the robot's current heading. Angular velocity remains unchanged as it's frame-invariant.
   *
   * @param heading the robot's current heading rotation
   * @return the velocity in the robot's local (chassis) frame
   * @see ChassisVelocities.toPose for the inverse transformation
   */
  fun toChassis(heading: Rotation2d): ChassisVelocities = ChassisVelocities(
    linearVel = heading.inverse() * linearVel,
    angVel = angVel,
  )

  /**
   * Linear interpolation (lerp) toward another pose velocity.
   *
   * Interpolates both linear and angular velocity.
   *
   * @param other the target velocity to interpolate toward
   * @param t the interpolation parameter in range [0, 1]
   * @return the interpolated velocity
   */
  fun lerp(other: PoseVelocity2d, t: Double): PoseVelocity2d {
    require(t in 0.0..1.0) { "Interpolation parameter t must be in range [0, 1], got $t" }

    val interpolatedLinearVel = linearVel.lerp(other.linearVel, t)
    val interpolatedAngVel = angVel * (1.0 - t) + other.angVel * t

    return PoseVelocity2d(interpolatedLinearVel, interpolatedAngVel)
  }

  companion object {
    @JvmField
    val zero = PoseVelocity2d(Vector2d.zero(InchesPerSecond), RadiansPerSecond.of(0.0))
  }
}
