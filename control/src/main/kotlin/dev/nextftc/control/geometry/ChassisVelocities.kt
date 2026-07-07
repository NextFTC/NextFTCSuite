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
 * Represents the velocity of a robot chassis in its local (body) frame.
 *
 * ## ChassisVelocities vs PoseVelocity2d
 *
 * **ChassisVelocities** represents velocities in the **robot's local frame** (body frame):
 * - The x-axis points forward from the robot
 * - The y-axis points left from the robot
 * - Linear velocities are relative to the robot's orientation
 * - This is the natural frame for robot control commands
 *
 * **PoseVelocity2d** represents velocities in the **global (field) frame**:
 * - The x and y axes are fixed to the field
 * - Linear velocities are relative to the field coordinate system
 * - This is the natural frame for trajectory planning and pose tracking
 *
 * ## Conversion Between Frames
 *
 * To convert from chassis (local) to global velocities:
 * ```kotlin
 * val globalVel = currentPose.heading * chassisVel.linearVel
 * ```
 *
 * To convert from global to chassis (local) velocities:
 * ```kotlin
 * val chassisVel = currentPose.heading.inverse() * globalVel
 * ```
 *
 * ## Example Usage
 *
 * ```kotlin
 * // Robot moving forward at 10 in/s, turning at 0.5 rad/s
 * val chassisVel = ChassisVelocities(
 *     linearVel = Vector2d(10.0.inchesPerSecond, 0.0.inchesPerSecond),
 *     angVel = 0.5.radiansPerSecond
 * )
 *
 * // Convert to field frame
 * val fieldVel = PoseVelocity2d(
 *     linearVel = pose.heading * chassisVel.linearVel,
 *     angVel = chassisVel.angVel  // Angular velocity is frame-invariant
 * )
 * ```
 *
 * @property linearVel linear velocity in the robot's local frame (forward/left)
 * @property angVel angular velocity (frame-invariant)
 * @see PoseVelocity2d for velocities in the global frame
 */
data class ChassisVelocities(
  @JvmField val linearVel: Vector2d<PerUnit<DistanceUnit, TimeUnit>>,
  @JvmField val angVel: AngularVelocity,
) {
  /**
   * Adds two chassis velocities component-wise.
   *
   * @param pv the chassis velocity to add
   * @return the sum of the velocities
   */
  operator fun plus(pv: ChassisVelocities) = ChassisVelocities(
    linearVel + pv.linearVel,
    angVel + pv.angVel,
  )

  /**
   * Computes the difference between two chassis velocities.
   *
   * @param pv the chassis velocity to subtract
   * @return the velocity difference
   */
  operator fun minus(pv: ChassisVelocities) = ChassisVelocities(
    linearVel - pv.linearVel,
    angVel - pv.angVel,
  )

  /**
   * Negates the chassis velocity.
   *
   * @return the negated velocity
   */
  operator fun unaryMinus() = ChassisVelocities(-linearVel, -angVel)

  /**
   * Multiplies the chassis velocity by a scalar.
   *
   * @param scalar the scalar multiplier
   * @return the scaled velocity
   */
  operator fun times(scalar: Double) = ChassisVelocities(linearVel * scalar, angVel * scalar)

  /**
   * Divides the chassis velocity by a scalar.
   *
   * @param scalar the scalar divisor
   * @return the scaled velocity
   */
  operator fun div(scalar: Double) = ChassisVelocities(linearVel / scalar, angVel / scalar)

  /**
   * Converts this chassis velocity (local frame) to a pose velocity (global frame).
   *
   * Transforms the linear velocity from the robot's local frame to the global field frame
   * using the robot's current heading. Angular velocity remains unchanged as it's frame-invariant.
   *
   * @param heading the robot's current heading rotation
   * @return the velocity in the global (field) frame
   * @see PoseVelocity2d.toChassis for the inverse transformation
   */
  fun toPose(heading: Rotation2d): PoseVelocity2d = PoseVelocity2d(
    linearVel = heading * linearVel,
    angVel = angVel,
  )

  /**
   * Linear interpolation (lerp) toward another chassis velocity.
   *
   * Interpolates both linear and angular velocity in the chassis frame.
   *
   * @param other the target velocity to interpolate toward
   * @param t the interpolation parameter in range [0, 1]
   * @return the interpolated velocity
   */
  fun lerp(other: ChassisVelocities, t: Double): ChassisVelocities {
    require(t in 0.0..1.0) { "Interpolation parameter t must be in range [0, 1], got $t" }

    val interpolatedLinearVel = linearVel.lerp(other.linearVel, t)
    val interpolatedAngVel = angVel * (1.0 - t) + other.angVel * t

    return ChassisVelocities(interpolatedLinearVel, interpolatedAngVel)
  }

  companion object {
    /**
     * Zero chassis velocity (stationary robot).
     */
    @JvmField
    val zero = ChassisVelocities(Vector2d.zero(InchesPerSecond), RadiansPerSecond.of(0.0))
  }
}
