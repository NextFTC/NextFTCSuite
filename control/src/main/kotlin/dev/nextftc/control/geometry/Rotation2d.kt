/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.geometry

import dev.nextftc.units.Radians
import dev.nextftc.units.Unit
import dev.nextftc.units.inches
import dev.nextftc.units.measuretypes.Angle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * @usesMathJax
 *
 * Represents a 2D rotation using the unit complex number representation.
 *
 * A rotation in 2D can be represented as a unit complex number \(z = \cos\theta + i\sin\theta\),
 * where \(\theta\) is the angle of rotation. This representation is stored as the pair
 * (real, imag) = \((\cos\theta, \sin\theta)\).
 *
 * ## Why Unit Complex Numbers?
 *
 * This representation has several advantages:
 * - **Efficient composition**: Multiplying two rotations \(z_1 \cdot z_2\) composes them
 * - **No gimbal lock**: Unlike Euler angles, no singularities
 * - **Numerically stable**: No wrap-around issues at \(2\pi\)
 * - **Direct vector transformation**: Rotating a vector is a simple complex multiplication
 *
 * ## Mathematical Operations
 *
 * **Composition** (multiplication):
 * \((\cos\theta_1, \sin\theta_1) \cdot (\cos\theta_2, \sin\theta_2) = (\cos(\theta_1+\theta_2), \sin(\theta_1+\theta_2))\)
 *
 * **Inverse** (conjugate):
 * \((\cos\theta, \sin\theta)^{-1} = (\cos\theta, -\sin\theta)\)
 *
 * **Vector rotation**:
 * To rotate a vector \((x, y)\) by angle \(\theta\):
 * \((x', y') = (\cos\theta \cdot x - \sin\theta \cdot y, \sin\theta \cdot x + \cos\theta \cdot y)\)
 *
 * ## Example Usage
 *
 * ```kotlin
 * // Create a 90-degree rotation
 * val rot90 = Rotation2d.exp(Math.PI / 2)
 *
 * // Create from an Angle
 * val rot = Rotation2d.fromAngle(45.0.degrees)
 *
 * // Compose rotations
 * val composed = rot90 * rot90  // 180-degree rotation
 *
 * // Rotate a vector
 * val v = Vector2d(1.0.inches, 0.0.inches)
 * val rotated = rot90 * v  // Results in approximately (0, 1)
 *
 * // Get angle back
 * val angle = rot90.log()  // π/2 radians
 * ```
 *
 * @property real the real part \((\cos\theta)\)
 * @property imag the imaginary part \((\sin\theta)\)
 */
data class Rotation2d(@JvmField val real: Double, @JvmField val imag: Double) {
  /**
   * Adds an angle (in radians) to this rotation.
   *
   * @param x the angle to add in radians
   * @return the composed rotation
   */
  operator fun plus(x: Double) = this * exp(x)

  /**
   * Computes the relative angle from another rotation to this rotation.
   *
   * @param r the reference rotation
   * @return the angle difference in radians
   */
  operator fun minus(r: Rotation2d) = (r.inverse() * this).log()

  /**
   * Rotates a vector by this rotation.
   *
   * Applies the rotation transformation \(R(\theta) \cdot \mathbf{v}\).
   *
   * @param v the vector to rotate
   * @return the rotated vector
   */
  operator fun <U : Unit<U>> times(v: Vector2d<U>) = Vector2d(
    v.x.unit.of(real * v.x.into(v.x.unit) - imag * v.y.into(v.x.unit)),
    v.x.unit.of(imag * v.x.into(v.x.unit) + real * v.y.into(v.x.unit)),
  )

  /**
   * Rotates a pose velocity by this rotation.
   *
   * The linear velocity is rotated, while angular velocity remains unchanged.
   *
   * @param pv the pose velocity to rotate
   * @return the rotated pose velocity
   */
  operator fun times(pv: PoseVelocity2d) = PoseVelocity2d(this * pv.linearVel, pv.angVel)

  /**
   * Composes two rotations.
   *
   * Multiplies this rotation with another rotation using complex multiplication.
   * The resulting angle is the sum of the two angles.
   *
   * @param r the rotation to compose with
   * @return the composed rotation
   */
  operator fun times(r: Rotation2d) = Rotation2d(
    real * r.real - imag * r.imag,
    real * r.imag + imag * r.real,
  )

  /**
   * Converts this rotation to a unit vector.
   *
   * @return a vector \((\cos\theta, \sin\theta)\) in inches
   */
  fun vec() = Vector2d(real.inches, imag.inches)

  /**
   * Computes the inverse (opposite) rotation.
   *
   * The inverse of a rotation by \(\theta\) is a rotation by \(-\theta\),
   * which is the complex conjugate \((\cos\theta, -\sin\theta)\).
   *
   * @return the inverse rotation
   */
  fun inverse() = Rotation2d(real, -imag)

  /**
   * Computes the angle of this rotation in radians using the logarithm map.
   *
   * Returns a value in the range \([-\pi, \pi]\).
   *
   * @return the angle in radians
   */
  fun log() = atan2(imag, real)

  /**
   * Alias for [log]. Returns the angle in radians.
   *
   * @return the angle in radians
   */
  fun toDouble() = log()

  /**
   * Linear interpolation (lerp) toward another rotation using spherical linear interpolation (SLERP).
   *
   * For rotations, we use SLERP instead of linear interpolation in 2D to maintain constant
   * angular velocity throughout the interpolation.
   *
   * The interpolation follows the shortest path on the unit circle.
   *
   * @param other the target rotation to interpolate toward
   * @param t the interpolation parameter in range [0, 1]
   * @return the interpolated rotation
   */
  fun lerp(other: Rotation2d, t: Double): Rotation2d {
    require(t in 0.0..1.0) { "Interpolation parameter t must be in range [0, 1], got $t" }

    // Compute the angle between the two rotations
    val angleDiff = other.log() - this.log()

    // Interpolate the angle
    val interpolatedAngle = this.log() + angleDiff * t

    // Return as a new rotation
    return exp(interpolatedAngle)
  }

  companion object {
    /**
     * Creates a rotation from an angle in radians using the exponential map.
     *
     * @param theta the angle in radians
     * @return a Rotation2d representing a rotation by \(\theta\)
     */
    @JvmStatic
    fun exp(theta: Double) = Rotation2d(cos(theta), sin(theta))

    /**
     * Creates a rotation from an angle in radians.
     * Alias for [exp].
     *
     * @param theta the angle in radians
     * @return a Rotation2d representing a rotation by \(\theta\)
     */
    @JvmStatic
    fun fromDouble(theta: Double) = exp(theta)

    /**
     * Creates a rotation from an [Angle] measurement.
     *
     * @param angle the angle as an Angle measurement
     * @return a Rotation2d representing the given angle
     */
    @JvmStatic
    fun fromAngle(angle: Angle) = exp(angle.into(Radians))

    /**
     * The identity rotation (no rotation, 0 radians).
     */
    @JvmField
    val zero = Rotation2d(1.0, 0.0)
  }
}
