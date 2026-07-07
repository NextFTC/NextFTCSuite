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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * @usesMathJax
 *
 * Represents a 2D pose (position and orientation) in the Special Euclidean group SE(2).
 *
 * A pose combines:
 * - A position vector \(\mathbf{p} = (x, y)\) in 2D space
 * - A heading (orientation) \(R \in SO(2)\)
 *
 * ## SE(2) - The Special Euclidean Group
 *
 * SE(2) is the group of rigid body transformations in 2D, consisting of rotations and translations.
 * A pose can be represented as a transformation matrix:
 *
 * \(g = \begin{bmatrix} R & \mathbf{p} \\ 0 & 1 \end{bmatrix} \in SE(2)\)
 *
 * where \(R\) is a 2×2 rotation matrix and \(\mathbf{p}\) is a 2D translation vector.
 *
 * ## Operations
 *
 * **Composition**: Combining two poses gives a new pose:
 * \(g_1 \circ g_2 = (R_1 R_2, R_1 \mathbf{p}_2 + \mathbf{p}_1)\)
 *
 * **Inverse**: The inverse pose:
 * \(g^{-1} = (R^T, -R^T \mathbf{p})\)
 *
 * **Exponential map**: Converts a twist (velocity) to a pose:
 * \(\exp: \mathfrak{se}(2) \to SE(2)\)
 *
 * **Logarithm map**: Converts a pose to a twist:
 * \(\log: SE(2) \to \mathfrak{se}(2)\)
 *
 * ## Example Usage
 *
 * ```kotlin
 * // Create a pose at (10, 5) with 45-degree heading
 * val pose = Pose2d(Vector2d(10.0.inches, 5.0.inches), Math.PI / 4)
 *
 * // Compose poses
 * val pose2 = Pose2d(Vector2d(1.0.inches, 0.0.inches), 0.0)
 * val composed = pose * pose2
 *
 * // Transform a vector
 * val localPoint = Vector2d(1.0.inches, 0.0.inches)
 * val globalPoint = pose * localPoint
 *
 * // Get relative pose
 * val relativePose = pose2 - pose
 * ```
 *
 * @property position the position vector in 2D space
 * @property heading the orientation as a Rotation2d
 */
data class Pose2d(
  @JvmField
  val position: Vector2d<DistanceUnit>,
  @JvmField
  val heading: Rotation2d,
) {
  /**
   * Constructs a Pose2d from a position vector and a heading angle.
   *
   * @param position the position vector
   * @param heading the heading angle in radians
   */
  constructor(
    position: Vector2d<DistanceUnit>,
    heading: Double,
  ) : this(position, Rotation2d.exp(heading))

  /**
   * Constructs a Pose2d from x and y distance measurements and a heading rotation.
   *
   * @param x the x-coordinate
   * @param y the y-coordinate
   * @param heading the heading rotation
   */
  constructor(x: Distance, y: Distance, heading: Rotation2d) : this(Vector2d(x, y), heading)

  /**
   * Constructs a Pose2d from x and y distance measurements and a heading angle.
   *
   * @param x the x-coordinate
   * @param y the y-coordinate
   * @param heading the heading angle measurement
   */
  constructor(
    x: Distance,
    y: Distance,
    heading: Angle,
  ) : this(Vector2d(x, y), Rotation2d.fromAngle(heading))

  /**
   * Constructs a Pose2d from x and y coordinates (in inches) and a heading rotation.
   *
   * Convenience constructor that assumes inch units for position coordinates.
   *
   * @param x the x-coordinate in inches
   * @param y the y-coordinate in inches
   * @param heading the heading rotation
   */
  constructor(
    x: Double,
    y: Double,
    heading: Rotation2d,
  ) : this(Vector2d.displacement(x, y), heading)

  /**
   * Constructs a Pose2d from x and y coordinates (in inches) and a heading angle (in radians).
   *
   * Convenience constructor that assumes inch units for position and radians for heading.
   *
   * @param x the x-coordinate in inches
   * @param y the y-coordinate in inches
   * @param heading the heading angle in radians
   */
  constructor(x: Double, y: Double, heading: Double) : this(x, y, Rotation2d.exp(heading))

  /**
   * Adds a twist to this pose using the exponential map.
   *
   * @param t the twist to add
   * @return the resulting pose
   */
  operator fun plus(t: Twist2d) = this * exp(t)

  /**
   * Applies a transform to this pose.
   *
   * This computes the pose resulting from applying the transform to this pose.
   *
   * @param transform the transform to apply
   * @return the transformed pose
   */
  operator fun plus(transform: Transform2d): Pose2d = Pose2d(
    position = heading * transform.translation + position,
    heading = heading * transform.rotation,
  )

  /**
   * Computes the transform from this pose to another pose.
   *
   * Returns the transformation that, when applied to this pose, yields the target pose.
   *
   * @param target the target pose
   * @return the transform from this pose to the target
   */
  fun relativeTo(target: Pose2d): Transform2d = Transform2d(this, target)

  /**
   * Computes the relative pose from another pose to this pose.
   *
   * @param t the reference pose
   * @return the pose difference
   */
  fun minusExp(t: Pose2d) = t.inverse() * this

  /**
   * Computes the twist (logarithm map) from another pose to this pose.
   *
   * @param t the reference pose
   * @return the twist difference
   */
  operator fun minus(t: Pose2d) = minusExp(t).log()

  /**
   * Composes this pose with another pose.
   *
   * @param p the pose to compose with
   * @return the composed pose
   */
  operator fun times(p: Pose2d) = Pose2d(heading * p.position + position, heading * p.heading)

  /**
   * Transforms a vector by this pose.
   *
   * @param v the vector to transform
   * @return the transformed vector
   */
  operator fun times(v: Vector2d<DistanceUnit>) = heading * v + position

  /**
   * Transforms a pose velocity by this pose's rotation.
   *
   * @param pv the pose velocity to transform
   * @return the transformed pose velocity
   */
  operator fun times(pv: PoseVelocity2d) = PoseVelocity2d(heading * pv.linearVel, pv.angVel)

  /**
   * Computes the inverse of this pose.
   *
   * @return the inverse pose
   */
  fun inverse() = Pose2d(heading.inverse() * -position, heading.inverse())

  /**
   * Linear interpolation (lerp) toward another pose.
   *
   * Interpolates both position and heading:
   * - Position: Linear interpolation of the position vectors
   * - Heading: Spherical linear interpolation of the rotations
   *
   * @param other the target pose to interpolate toward
   * @param t the interpolation parameter in range [0, 1]
   * @return the interpolated pose
   */
  fun lerp(other: Pose2d, t: Double): Pose2d {
    require(t in 0.0..1.0) { "Interpolation parameter t must be in range [0, 1], got $t" }

    val interpolatedPosition = position.lerp(other.position, t)
    val interpolatedHeading = heading.lerp(other.heading, t)

    return Pose2d(interpolatedPosition, interpolatedHeading)
  }

  /**
   * Computes the linear distance to another pose.
   *
   * Only accounts for linear distance, not angular change.
   *
   * @param other the target pose
   * @return the linear distance between poses
   */
  fun distanceTo(other: Pose2d): Double {
    val delta = other.position - position
    return sqrt(
      delta.x.into(delta.x.unit) * delta.x.into(delta.x.unit) +
        delta.y.into(delta.y.unit) * delta.y.into(delta.y.unit),
    )
  }

  /**
   * Finds the parameter t where the distance between two poses is minimized.
   *
   * This is useful for finding the closest point on a line segment between two poses.
   *
   * Formula: \(t = \frac{(\mathbf{p} - \mathbf{a}) \cdot (\mathbf{b} - \mathbf{a})}{|\mathbf{b} - \mathbf{a}|^2}\)
   *
   * @param start the starting pose
   * @param end the ending pose
   * @return the parameter t clamped to [0, 1]
   */
  fun closestParameterOnSegment(start: Pose2d, end: Pose2d): Double {
    val delta = end.position - start.position
    val toPoint = this.position - start.position

    val dotProduct = toPoint.x.into(toPoint.x.unit) * delta.x.into(delta.x.unit) +
      toPoint.y.into(toPoint.y.unit) * delta.y.into(delta.y.unit)
    val lengthSquared = delta.x.into(delta.x.unit) * delta.x.into(delta.x.unit) +
      delta.y.into(delta.y.unit) * delta.y.into(delta.y.unit)

    if (lengthSquared == 0.0) return 0.0

    return (dotProduct / lengthSquared).coerceIn(0.0, 1.0)
  }

  /**
   * Finds the closest pose on a line segment between two poses.
   *
   * @param start the starting pose
   * @param end the ending pose
   * @return the closest pose on the segment
   */
  fun closestOnSegment(start: Pose2d, end: Pose2d): Pose2d {
    val t = closestParameterOnSegment(start, end)
    return start.lerp(end, t)
  }

  /**
   * Computes the twist (logarithm map) of this pose.
   *
   * @return the twist representation
   */
  fun log(): Twist2d {
    val theta = heading.log()

    val halfu = 0.5 * theta + snz(theta)
    val v = halfu / tan(halfu)
    val xVal = v * position.x.into(position.x.unit) + halfu * position.y.into(position.x.unit)
    val yVal = -halfu * position.x.into(position.x.unit) + v * position.y.into(position.x.unit)
    return Twist2d(
      Vector2d(
        position.x.unit.of(xVal),
        position.x.unit.of(yVal),
      ),
      theta,
    )
  }

  companion object {
    /**
     * Computes the pose exponential map from a twist.
     *
     * @param t the twist to exponentiate
     * @return the resulting pose
     */
    @JvmStatic
    fun exp(t: Twist2d): Pose2d {
      val heading = Rotation2d.exp(t.angle)

      val theta = t.angle
      val u = theta + snz(theta)
      val c = 1 - cos(u)
      val s = sin(u)
      val xVal = (s * t.line.x.into(t.line.x.unit) - c * t.line.y.into(t.line.x.unit)) / u
      val yVal = (c * t.line.x.into(t.line.x.unit) + s * t.line.y.into(t.line.x.unit)) / u
      val translation = Vector2d(
        t.line.x.unit.of(xVal),
        t.line.x.unit.of(yVal),
      )

      return Pose2d(translation, heading)
    }

    @JvmField
    val zero = Pose2d(Vector2d.zero(Inches), Rotation2d.zero)
  }
}

/**
 * Returns a small non-zero value with the same sign as the input.
 * Used for numerical stability in calculations involving division.
 *
 * @param x the input value
 * @return a small non-zero value (1e-10) with the same sign as x, or 1e-10 if x is zero
 */
internal fun snz(x: Double): Double = if (abs(x) < 1e-10) 1e-10 else 0.0
