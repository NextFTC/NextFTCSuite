/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.geometry

import dev.nextftc.units.InchesPerSecondSquared
import dev.nextftc.units.RadiansPerSecondSquared
import dev.nextftc.units.measuretypes.AngularAcceleration
import dev.nextftc.units.measuretypes.Time
import dev.nextftc.units.unittypes.DistanceUnit
import dev.nextftc.units.unittypes.PerUnit
import dev.nextftc.units.unittypes.TimeUnit

/**
 * @usesMathJax
 *
 * Represents the acceleration of a 2D pose in the global (field) frame.
 *
 * A pose acceleration describes how a pose velocity changes over time and consists of:
 * - **Linear acceleration**: \(\mathbf{a}\) - the acceleration of the position in field coordinates
 * - **Angular acceleration**: \(\alpha\) - the rate of change of angular velocity (frame-invariant)
 *
 * ## PoseAcceleration2d vs ChassisAccelerations
 *
 * **PoseAcceleration2d** represents accelerations in the **global (field) frame**:
 * - The x-axis and y-axis are fixed to the field
 * - Linear accelerations are expressed in field coordinates
 * - Natural for trajectory generation and feedforward control
 * - Includes effects from changing the robot's orientation
 *
 * **ChassisAccelerations** represents accelerations in the **robot's local frame** (body frame):
 * - The x-axis points forward, y-axis points left (from robot's perspective)
 * - Linear accelerations are relative to the robot's orientation
 * - Natural for motor control and dynamic modeling
 * - Does not directly include centripetal acceleration effects
 *
 * ## Important Note on Rotating Frames
 *
 * When a robot is rotating, the relationship between chassis and field accelerations is
 * more complex than for velocities. The field acceleration includes:
 * 1. The rotated chassis acceleration
 * 2. Centripetal acceleration: \(\mathbf{a}_{centripetal} = \omega \times \mathbf{v}\)
 *
 * Where \(\omega\) is angular velocity and \(\mathbf{v}\) is the velocity vector.
 *
 * ## Kinematic Integration
 *
 * Given an initial velocity \(\mathbf{v}_0\) and a time step \(\Delta t\), the new velocity is:
 * \(\mathbf{v}(t + \Delta t) = \mathbf{v}_0 + \mathbf{a} \cdot \Delta t\)
 *
 * Similarly for angular components:
 * \(\omega(t + \Delta t) = \omega_0 + \alpha \cdot \Delta t\)
 *
 * ## Example Usage
 *
 * ```kotlin
 * // Robot accelerating at 5 in/s² in the field's x-direction
 * val poseAcc = PoseAcceleration2d(
 *     linearAcc = Vector2d(5.0.inchesPerSecondSquared, 0.0.inchesPerSecondSquared),
 *     angAcc = 0.1.radiansPerSecondSquared
 * )
 *
 * // Integrate to get velocity after 0.1 seconds
 * val dt = 0.1.seconds
 * val initialVel = PoseVelocity2d.zero
 * val newVel = poseAcc.integrateToVel(dt, initialVel)
 *
 * // For trajectory control, often converted to chassis frame:
 * val chassisAcc = ChassisAccelerations(
 *     linearAcc = pose.heading.inverse() * poseAcc.linearAcc,
 *     angAcc = poseAcc.angAcc
 * )
 * ```
 *
 * @property linearAcc linear acceleration in the global (field) frame
 * @property angAcc angular acceleration (frame-invariant)
 * @see ChassisAccelerations for accelerations in the robot's local frame
 */
data class PoseAcceleration2d(
  @JvmField val linearAcc: Vector2d<PerUnit<PerUnit<DistanceUnit, TimeUnit>, TimeUnit>>,
  @JvmField val angAcc: AngularAcceleration,
) {
  /**
   * Adds two pose accelerations component-wise.
   *
   * @param pa the pose acceleration to add
   * @return the sum of the accelerations
   */
  operator fun plus(pa: PoseAcceleration2d) = PoseAcceleration2d(
    linearAcc + pa.linearAcc,
    angAcc + pa.angAcc,
  )

  /**
   * Subtracts two pose accelerations component-wise.
   *
   * @param pa the pose acceleration to subtract
   * @return the difference of the accelerations
   */
  operator fun minus(pa: PoseAcceleration2d) = PoseAcceleration2d(
    linearAcc - pa.linearAcc,
    angAcc - pa.angAcc,
  )

  /**
   * Negates the pose acceleration.
   *
   * @return the negated acceleration
   */
  operator fun unaryMinus() = PoseAcceleration2d(-linearAcc, -angAcc)

  /**
   * Multiplies the pose acceleration by a scalar.
   *
   * @param scalar the scalar multiplier
   * @return the scaled acceleration
   */
  operator fun times(scalar: Double) = PoseAcceleration2d(linearAcc * scalar, angAcc * scalar)

  /**
   * Divides the pose acceleration by a scalar.
   *
   * @param scalar the scalar divisor
   * @return the scaled acceleration
   */
  operator fun div(scalar: Double) = PoseAcceleration2d(linearAcc / scalar, angAcc / scalar)

  /**
   * Linear interpolation (lerp) toward another pose acceleration.
   *
   * Interpolates both linear and angular acceleration.
   *
   * @param other the target acceleration to interpolate toward
   * @param t the interpolation parameter in range [0, 1]
   * @return the interpolated acceleration
   */
  fun lerp(other: PoseAcceleration2d, t: Double): PoseAcceleration2d {
    require(t in 0.0..1.0) { "Interpolation parameter t must be in range [0, 1], got $t" }

    val interpolatedLinearAcc = linearAcc.lerp(other.linearAcc, t)
    val interpolatedAngAcc = angAcc * (1.0 - t) + other.angAcc * t

    return PoseAcceleration2d(interpolatedLinearAcc, interpolatedAngAcc)
  }

  /**
   * Uses kinematic integration to compute a new velocity given a time step and initial velocity.
   * @param dt time step
   * @param initial initial velocity; default is 0.
   */
  @JvmOverloads
  fun integrateToVel(dt: Time, initial: PoseVelocity2d = PoseVelocity2d.zero) = PoseVelocity2d(
    initial.linearVel + Vector2d(
      linearAcc.x.unit.numerator.numerator.per(
        dt.unit,
      ).of(linearAcc.x.into(linearAcc.x.unit) * dt.into(dt.unit)),
      linearAcc.x.unit.numerator.numerator.per(
        dt.unit,
      ).of(linearAcc.y.into(linearAcc.x.unit) * dt.into(dt.unit)),
    ),
    initial.angVel + angAcc * dt,
  )

  /**
   * Converts this pose acceleration (global frame) to a chassis acceleration (local frame).
   *
   * Transforms the linear acceleration from the global field frame to the robot's local frame
   * using the robot's current heading. Angular acceleration remains unchanged as it's frame-invariant.
   *
   * **Note**: This is a simplified transformation that doesn't account for centripetal effects
   * from the robot's rotation. For accurate transformations when the robot is rotating,
   * you need to include centripetal acceleration terms.
   *
   * @param heading the robot's current heading rotation
   * @return the acceleration in the robot's local (chassis) frame
   * @see ChassisAccelerations.toPose for the inverse transformation
   */
  fun toChassis(heading: Rotation2d): ChassisAccelerations = ChassisAccelerations(
    linearAcc = heading.inverse() * linearAcc,
    angAcc = angAcc,
  )

  companion object {
    @JvmField
    val zero = PoseAcceleration2d(
      Vector2d.zero(InchesPerSecondSquared),
      RadiansPerSecondSquared.of(0.0),
    )
  }
}
