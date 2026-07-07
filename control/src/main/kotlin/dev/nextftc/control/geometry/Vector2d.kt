/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("Vectors")

package dev.nextftc.control.geometry

import dev.nextftc.units.Measure
import dev.nextftc.units.Unit
import dev.nextftc.units.inches
import dev.nextftc.units.inchesPerSecond
import dev.nextftc.units.inchesPerSecondSquared
import dev.nextftc.units.measuretypes.Time
import dev.nextftc.units.unittypes.DistanceUnit
import dev.nextftc.units.unittypes.PerUnit
import dev.nextftc.units.unittypes.TimeUnit
import kotlin.math.sqrt

/**
 * @usesMathJax
 *
 * A 2D vector with components in a specified unit system.
 *
 * This class represents a 2D vector \(\mathbf{v} = (x, y)\) where both components
 * have the same unit type \(U\). Common use cases include position vectors (with distance units),
 * velocity vectors (with velocity units), and acceleration vectors (with acceleration units).
 *
 * ## Operations
 *
 * **Vector arithmetic**:
 * - Addition: \(\mathbf{v}_1 + \mathbf{v}_2 = (x_1 + x_2, y_1 + y_2)\)
 * - Subtraction: \(\mathbf{v}_1 - \mathbf{v}_2 = (x_1 - x_2, y_1 - y_2)\)
 * - Negation: \(-\mathbf{v} = (-x, -y)\)
 *
 * **Scalar operations**:
 * - Multiplication: \(c \cdot \mathbf{v} = (c \cdot x, c \cdot y)\)
 * - Division: \(\mathbf{v} / c = (x / c, y / c)\)
 *
 * **Vector products**:
 * - Dot product: \(\mathbf{v}_1 \cdot \mathbf{v}_2 = x_1 x_2 + y_1 y_2\)
 * - Squared norm: \(\|\mathbf{v}\|^2 = x^2 + y^2\)
 * - Norm (magnitude): \(\|\mathbf{v}\| = \sqrt{x^2 + y^2}\)
 *
 * ## Example Usage
 *
 * ```kotlin
 * // Position vector
 * val position = Vector2d(10.0.inches, 5.0.inches)
 *
 * // Velocity vector
 * val velocity = Vector2d(2.0.inchesPerSecond, 3.0.inchesPerSecond)
 *
 * // Vector operations
 * val sum = position + Vector2d(1.0.inches, 2.0.inches)
 * val scaled = position * 2.0
 * val magnitude = position.norm()
 * ```
 *
 * @param U the unit type for both components (e.g., DistanceUnit, VelocityUnit)
 * @property x the x-component of the vector
 * @property y the y-component of the vector
 */
data class Vector2d<U : Unit<U>>(@JvmField val x: Measure<U>, @JvmField val y: Measure<U>) {
  /**
   * Adds two vectors component-wise.
   *
   * @param v the vector to add
   * @return the sum \(\mathbf{v}_1 + \mathbf{v}_2\)
   */
  operator fun plus(v: Vector2d<U>) = Vector2d(x + v.x, y + v.y)

  /**
   * Subtracts two vectors component-wise.
   *
   * @param v the vector to subtract
   * @return the difference \(\mathbf{v}_1 - \mathbf{v}_2\)
   */
  operator fun minus(v: Vector2d<U>) = Vector2d(x - v.x, y - v.y)

  /**
   * Negates the vector.
   *
   * @return the negated vector \(-\mathbf{v}\)
   */
  operator fun unaryMinus() = Vector2d(-x, -y)

  /**
   * Multiplies the vector by a scalar.
   *
   * @param z the scalar multiplier
   * @return the scaled vector \(z \cdot \mathbf{v}\)
   */
  operator fun times(z: Double) = Vector2d(x * z, y * z)

  /**
   * Divides the vector by a scalar.
   *
   * @param z the scalar divisor
   * @return the scaled vector \(\mathbf{v} / z\)
   */
  operator fun div(z: Double) = Vector2d(x / z, y / z)

  /**
   * Computes the dot product with another vector.
   *
   * @param v the other vector
   * @return the dot product \(\mathbf{v}_1 \cdot \mathbf{v}_2 = x_1 x_2 + y_1 y_2\)
   */
  infix fun dot(v: Vector2d<U>) = x.into(x.unit) * v.x.into(x.unit) + y.into(x.unit) * v.y.into(x.unit)

  /**
   * Computes the squared norm (magnitude squared) of the vector.
   *
   * This is more efficient than [norm] when only relative magnitudes are needed,
   * as it avoids the square root computation.
   *
   * @return the squared norm \(\|\mathbf{v}\|^2 = x^2 + y^2\)
   */
  fun sqrNorm() = this dot this

  /**
   * Computes the Euclidean norm (magnitude) of the vector.
   *
   * @return the norm \(\|\mathbf{v}\| = \sqrt{x^2 + y^2}\)
   */
  fun norm() = sqrt(sqrNorm())

  /**
   * Returns the angle of this vector as a [Rotation2d],
   * assuming this vector is already normalized (unit length).
   *
   * This is more efficient than [angle] but requires the vector to be normalized.
   *
   * @return the angle as a rotation
   */
  fun angleCast() = Rotation2d(x.into(x.unit), y.into(x.unit))

  /**
   * Returns the angle of this vector as a [Rotation2d].
   *
   * This method first normalizes the vector, then computes its angle.
   *
   * @return the angle as a rotation
   */
  fun angle() = (this / norm()).angleCast()

  /**
   * Converts this vector to a pair of measurements.
   *
   * @return a pair (x, y)
   */
  fun asPair() = x to y

  /**
   * Linear interpolation (lerp) toward another vector.
   *
   * Computes: \(\mathbf{v}(t) = (1-t)\mathbf{v}_1 + t\mathbf{v}_2\)
   *
   * Where \(t \in [0, 1]\) is the interpolation parameter:
   * - \(t = 0\) returns this vector
   * - \(t = 1\) returns the other vector
   * - \(t = 0.5\) returns the midpoint
   *
   * @param other the target vector to interpolate toward
   * @param t the interpolation parameter in range [0, 1]
   * @return the interpolated vector
   */
  fun lerp(other: Vector2d<U>, t: Double): Vector2d<U> {
    require(t in 0.0..1.0) { "Interpolation parameter t must be in range [0, 1], got $t" }
    return this * (1.0 - t) + other * t
  }

  companion object {
    /**
     * A zero vector in inches (for legacy compatibility).
     */
    @JvmField
    val zero = Vector2d(0.0.inches, 0.0.inches)

    /**
     * Creates a zero vector with the specified unit.
     *
     * @param unit the unit type for the vector components
     * @return a zero vector \((0, 0)\) in the specified unit
     */
    @JvmStatic
    fun <U : Unit<U>> zero(unit: U) = Vector2d(unit.of(0.0), unit.of(0.0))

    @JvmStatic
    fun displacement(x: Double, y: Double) = Vector2d(x.inches, y.inches)

    @JvmStatic
    fun velocity(vx: Double, vy: Double) = Vector2d(vx.inchesPerSecond, vy.inchesPerSecond)

    @JvmStatic
    fun acceleration(ax: Double, ay: Double) =
      Vector2d(ax.inchesPerSecondSquared, ay.inchesPerSecondSquared)
  }
}

/**
 * @usesMathJax
 *
 * Multiplies a velocity vector by a time duration to compute displacement.
 *
 * This operator allows you to integrate velocity over time to get a displacement vector:
 * \(\mathbf{d} = \mathbf{v} \cdot \Delta t\)
 *
 * ## Unit Conversion
 *
 * Input: `Vector2d<PerUnit<DistanceUnit, TimeUnit>>` (velocity, e.g., inches/second)
 * Output: `Vector2d<DistanceUnit>` (displacement, e.g., inches)
 *
 * @param other the time duration to multiply by
 * @return the displacement vector (velocity × time)
 * @see Vector2d for vector operations
 */
@JvmName("velTimesTime")
operator fun Vector2d<PerUnit<DistanceUnit, TimeUnit>>.times(other: Time) = Vector2d(
  x.unit.numerator.of(x.into(x.unit) * other.into(other.unit)),
  x.unit.numerator.of(y.into(x.unit) * other.into(other.unit)),
)

/**
 * @usesMathJax
 *
 * Multiplies an acceleration vector by a time duration to compute velocity change.
 *
 * This operator allows you to integrate acceleration over time to get a velocity vector:
 * \(\mathbf{v} = \mathbf{a} \cdot \Delta t\)
 *
 * ## Unit Conversion
 *
 * Input: `Vector2d<PerUnit<PerUnit<DistanceUnit, TimeUnit>, TimeUnit>>` (acceleration, e.g., inches/second²)
 * Output: `Vector2d<PerUnit<DistanceUnit, TimeUnit>>` (velocity, e.g., inches/second)
 *
 * @param other the time duration to multiply by
 * @return the velocity change vector (acceleration × time)
 * @see Vector2d for vector operations
 */
@JvmName("accelTimesTime")
operator fun Vector2d<PerUnit<PerUnit<DistanceUnit, TimeUnit>, TimeUnit>>.times(other: Time) = Vector2d(
  x.unit.numerator.of(x.into(x.unit) * other.into(other.unit)),
  x.unit.numerator.of(y.into(x.unit) * other.into(other.unit)),
)
