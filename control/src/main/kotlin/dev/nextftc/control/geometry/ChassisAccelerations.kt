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
 * Represents the acceleration of a robot chassis in its local (body) frame.
 *
 * ## ChassisAccelerations vs PoseAcceleration2d
 *
 * **ChassisAccelerations** represents accelerations in the **robot's local frame** (body frame):
 * - The x-axis points forward from the robot
 * - The y-axis points left from the robot
 * - Linear accelerations are relative to the robot's orientation
 * - Useful for motor control and dynamic modeling
 *
 * **PoseAcceleration2d** represents accelerations in the **global (field) frame**:
 * - The x and y axes are fixed to the field
 * - Linear accelerations are relative to the field coordinate system
 * - Useful for trajectory generation and feedforward control
 *
 * ## Frame Considerations
 *
 * When a robot rotates, the relationship between chassis and field accelerations is more
 * complex than for velocities due to centripetal effects. The transformation includes:
 * - Rotation of the acceleration vector
 * - Centripetal acceleration terms from the rotating frame
 *
 * ## Kinematic Integration
 *
 * Given an initial velocity \(\mathbf{v}_0\) and a time step \(\Delta t\), the new velocity is:
 * \(\mathbf{v}(t + \Delta t) = \mathbf{v}_0 + \mathbf{a} \cdot \Delta t\)
 *
 * ## Example Usage
 *
 * ```kotlin
 * // Robot accelerating forward at 5 in/s², with angular acceleration 0.1 rad/s²
 * val chassisAcc = ChassisAccelerations(
 *     linearAcc = Vector2d(5.0.inchesPerSecondSquared, 0.0.inchesPerSecondSquared),
 *     angAcc = 0.1.radiansPerSecondSquared
 * )
 *
 * // Integrate to get velocity after 0.1 seconds
 * val newVel = chassisAcc.integrateToVel(0.1.seconds, currentVel)
 * ```
 *
 * @property linearAcc linear acceleration in the robot's local frame (forward/left)
 * @property angAcc angular acceleration (frame-invariant)
 * @see PoseAcceleration2d for accelerations in the global frame
 */
data class ChassisAccelerations(
  @JvmField val linearAcc: Vector2d<PerUnit<PerUnit<DistanceUnit, TimeUnit>, TimeUnit>>,
  @JvmField val angAcc: AngularAcceleration,
) {
  /**
   * Adds two chassis accelerations component-wise.
   *
   * @param pa the chassis acceleration to add
   * @return the sum of the accelerations
   */
  operator fun plus(pa: ChassisAccelerations) = ChassisAccelerations(
    linearAcc + pa.linearAcc,
    angAcc + pa.angAcc,
  )

  /**
   * Subtracts two chassis accelerations component-wise.
   *
   * @param pa the chassis acceleration to subtract
   * @return the difference of the accelerations
   */
  operator fun minus(pa: ChassisAccelerations) = ChassisAccelerations(
    linearAcc - pa.linearAcc,
    angAcc - pa.angAcc,
  )

  /**
   * Negates the chassis acceleration.
   *
   * @return the negated acceleration
   */
  operator fun unaryMinus() = ChassisAccelerations(-linearAcc, -angAcc)

  /**
   * Multiplies the chassis acceleration by a scalar.
   *
   * @param scalar the scalar multiplier
   * @return the scaled acceleration
   */
  operator fun times(scalar: Double) = ChassisAccelerations(linearAcc * scalar, angAcc * scalar)

  /**
   * Divides the chassis acceleration by a scalar.
   *
   * @param scalar the scalar divisor
   * @return the scaled acceleration
   */
  operator fun div(scalar: Double) = ChassisAccelerations(linearAcc / scalar, angAcc / scalar)

  /**
   * Uses kinematic integration to compute a new velocity given a time step and initial velocity.
   *
   * Performs Euler integration: \(\mathbf{v}_{new} = \mathbf{v}_0 + \mathbf{a} \cdot \Delta t\)
   *
   * @param dt time step
   * @param initial initial velocity; defaults to zero
   * @return the integrated velocity after time dt
   */
  @JvmOverloads
  fun integrateToVel(dt: Time, initial: ChassisVelocities = ChassisVelocities.zero) = ChassisVelocities(
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
   * Converts this chassis acceleration (local frame) to a pose acceleration (global frame).
   *
   * Transforms the linear acceleration from the robot's local frame to the global field frame
   * using the robot's current heading. Angular acceleration remains unchanged as it's frame-invariant.
   *
   * **Note**: This is a simplified transformation that doesn't account for centripetal effects
   * from the robot's rotation. For accurate transformations when the robot is rotating,
   * you need to include centripetal acceleration terms.
   *
   * @param heading the robot's current heading rotation
   * @return the acceleration in the global (field) frame
   * @see PoseAcceleration2d for the inverse type
   */
  fun toPose(heading: Rotation2d): PoseAcceleration2d = PoseAcceleration2d(
    linearAcc = heading * linearAcc,
    angAcc = angAcc,
  )

  /**
   * Linear interpolation (lerp) toward another chassis acceleration.
   *
   * Interpolates both linear and angular acceleration in the chassis frame.
   *
   * @param other the target acceleration to interpolate toward
   * @param t the interpolation parameter in range [0, 1]
   * @return the interpolated acceleration
   */
  fun lerp(other: ChassisAccelerations, t: Double): ChassisAccelerations {
    require(t in 0.0..1.0) { "Interpolation parameter t must be in range [0, 1], got $t" }

    val interpolatedLinearAcc = linearAcc.lerp(other.linearAcc, t)
    val interpolatedAngAcc = angAcc * (1.0 - t) + other.angAcc * t

    return ChassisAccelerations(interpolatedLinearAcc, interpolatedAngAcc)
  }

  companion object {
    /**
     * Zero chassis acceleration (constant velocity robot).
     */
    @JvmField
    val zero = ChassisAccelerations(
      Vector2d.zero(InchesPerSecondSquared),
      RadiansPerSecondSquared.of(0.0),
    )
  }
}
