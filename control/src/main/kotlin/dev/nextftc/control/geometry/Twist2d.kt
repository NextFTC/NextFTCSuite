/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.geometry

import dev.nextftc.units.Inches
import dev.nextftc.units.measuretypes.Angle
import dev.nextftc.units.measuretypes.Distance
import dev.nextftc.units.unittypes.DistanceUnit

/**
 * @usesMathJax
 *
 * Represents a differential twist in 2D: a combination of linear and angular displacement.
 *
 * A twist is an element of the Lie algebra \(\mathfrak{se}(2)\), the tangent space of SE(2).
 * It represents an infinitesimal transformation consisting of:
 * - **Linear displacement**: \(\mathbf{v}\) - translation component
 * - **Angular displacement**: \(\theta\) - rotation component (as a Rotation2d)
 *
 * ## Relationship to Velocity
 *
 * A twist can be thought of as a "velocity × time" - it represents the displacement
 * that would result from moving with a constant velocity for a unit time.
 *
 * ## Exponential Map
 *
 * The exponential map converts a twist to a pose:
 * \(\exp: \mathfrak{se}(2) \to SE(2)\)
 *
 * This allows us to compute the pose resulting from a given displacement.
 *
 * ## Example Usage
 *
 * ```kotlin
 * // Create a twist: move forward 1 inch and rotate 45 degrees
 * val twist = Twist2d(
 *     Vector2d(1.0.inches, 0.0.inches),
 *     Math.PI / 4
 * )
 *
 * // Convert twist to pose
 * val pose = Pose2d.exp(twist)
 *
 * // Add twist to existing pose
 * val newPose = currentPose + twist
 * ```
 *
 * @property line linear displacement vector
 * @property angle angular displacement as a rotation
 */
data class Twist2d(@JvmField val line: Vector2d<DistanceUnit>, @JvmField val angle: Double) {
  /**
   * Constructs a Twist2d from x and y distance measurements and a rotation.
   *
   * @param x the x-component of linear displacement
   * @param y the y-component of linear displacement
   * @param angle the angular displacement as a rotation
   */
  constructor(x: Distance, y: Distance, angle: Rotation2d) : this(Vector2d(x, y), angle.log())

  /**
   * Constructs a Twist2d from x and y distance measurements and an angle measurement.
   *
   * @param x the x-component of linear displacement
   * @param y the y-component of linear displacement
   * @param angle the angular displacement as an angle measurement
   */
  constructor(
    x: Distance,
    y: Distance,
    angle: Angle,
  ) : this(Vector2d(x, y), angle.into(dev.nextftc.units.Radians))

  /**
   * Constructs a Twist2d from x and y coordinates (in inches) and a rotation.
   *
   * Convenience constructor that assumes inch units for linear displacement.
   *
   * @param x the x-component of displacement in inches
   * @param y the y-component of displacement in inches
   * @param angle the angular displacement as a rotation
   */
  constructor(x: Double, y: Double, angle: Rotation2d) : this(Vector2d.displacement(x, y), angle.log())

  /**
   * Constructs a Twist2d from x and y coordinates (in inches) and an angle (in radians).
   *
   * Convenience constructor that assumes inch units for linear displacement and radians for angle.
   *
   * @param x the x-component of displacement in inches
   * @param y the y-component of displacement in inches
   * @param angle the angular displacement in radians
   */
  constructor(x: Double, y: Double, angle: Double) : this(Vector2d.displacement(x, y), angle)

  /**
   * Adds two twists component-wise (Lie algebra addition).
   *
   * @param other the twist to add
   * @return the sum of the twists
   */
  operator fun plus(other: Twist2d): Twist2d = Twist2d(
    line + other.line,
    angle + other.angle,
  )

  /**
   * Subtracts two twists component-wise (Lie algebra subtraction).
   *
   * @param other the twist to subtract
   * @return the difference of the twists
   */
  operator fun minus(other: Twist2d): Twist2d = Twist2d(
    line - other.line,
    angle - other.angle,
  )

  /**
   * Negates the twist.
   *
   * @return the negated twist
   */
  operator fun unaryMinus(): Twist2d = Twist2d(-line, -angle)

  /**
   * Multiplies the twist by a scalar (useful for scaling displacements).
   *
   * @param scalar the scalar multiplier
   * @return the scaled twist
   */
  operator fun times(scalar: Double): Twist2d = Twist2d(
    line * scalar,
    angle * scalar,
  )

  /**
   * Divides the twist by a scalar.
   *
   * @param scalar the scalar divisor
   * @return the scaled twist
   */
  operator fun div(scalar: Double): Twist2d = Twist2d(
    line / scalar,
    angle / scalar,
  )

  /**
   * Linear interpolation (lerp) toward another twist.
   *
   * Interpolates both linear and angular displacement.
   *
   * @param other the target twist to interpolate toward
   * @param t the interpolation parameter in range [0, 1]
   * @return the interpolated twist
   */
  fun lerp(other: Twist2d, t: Double): Twist2d {
    require(t in 0.0..1.0) { "Interpolation parameter t must be in range [0, 1], got $t" }

    val interpolatedLine = line.lerp(other.line, t)
    val interpolatedAngle = angle + (other.angle - angle) * t

    return Twist2d(interpolatedLine, interpolatedAngle)
  }

  companion object {
    @JvmField
    val zero = Twist2d(Vector2d.zero(Inches), 0.0)
  }
}
