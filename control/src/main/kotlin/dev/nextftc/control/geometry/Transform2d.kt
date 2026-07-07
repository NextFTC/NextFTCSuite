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
 * Represents a finite transformation (relative pose) in 2D space.
 *
 * A Transform2d represents the **change in pose** from one pose to another. Unlike [Twist2d]
 * which represents infinitesimal displacements in the Lie algebra, Transform2d represents
 * finite relative transformations in SE(2).
 *
 * ## Transform2d vs Twist2d vs Pose2d
 *
 * - **Pose2d**: An absolute pose in the global frame (position + heading)
 * - **Transform2d**: A relative pose change - the transformation from one pose to another
 * - **Twist2d**: An infinitesimal displacement in the Lie algebra (used for velocities)
 *
 * ## Mathematical Representation
 *
 * A transform can be thought of as a pose relative to another pose:
 * \(T_{A \to B} = P_A^{-1} \circ P_B\)
 *
 * Where \(P_A\) and \(P_B\) are absolute poses.
 *
 * ## Common Use Cases
 *
 * **Relative Localization**: Expressing one pose relative to another:
 * ```kotlin
 * val robotPose = Pose2d(Vector2d(10.0.inches, 5.0.inches), Math.PI / 4)
 * val targetPose = Pose2d(Vector2d(15.0.inches, 8.0.inches), Math.PI / 2)
 * val transform = robotPose.relativeTo(targetPose)  // Transform from robot to target
 * ```
 *
 * **Odometry Updates**: Representing the change in pose between measurements:
 * ```kotlin
 * val prevPose = odometry.getPose()
 * // ... robot moves ...
 * val currPose = odometry.getPose()
 * val delta = Transform2d(prevPose, currPose)  // How much the robot moved
 * ```
 *
 * **Coordinate Frame Transformations**: Converting between coordinate frames:
 * ```kotlin
 * val cameraToRobot = Transform2d(
 *     Vector2d(6.0.inches, 0.0.inches),  // Camera is 6" forward
 *     Rotation2d.exp(0.0)
 * )
 * val targetInCameraFrame = Vector2d(24.0.inches, 12.0.inches)
 * val targetInRobotFrame = cameraToRobot * targetInCameraFrame
 * ```
 *
 * ## Example Usage
 *
 * ```kotlin
 * // Create a transform: 5 inches forward, rotated 30 degrees
 * val transform = Transform2d(
 *     Vector2d(5.0.inches, 0.0.inches),
 *     Math.PI / 6
 * )
 *
 * // Apply transform to a pose
 * val initialPose = Pose2d(Vector2d(0.0.inches, 0.0.inches), 0.0)
 * val finalPose = initialPose + transform
 *
 * // Compose transforms
 * val transform1 = Transform2d(Vector2d(5.0.inches, 0.0.inches), 0.0)
 * val transform2 = Transform2d(Vector2d(0.0.inches, 3.0.inches), Math.PI / 4)
 * val combined = transform1 + transform2
 *
 * // Inverse transform
 * val inverse = transform.inverse()
 * ```
 *
 * @property translation the translational component
 * @property rotation the rotational component
 * @see Pose2d for absolute poses
 * @see Twist2d for infinitesimal displacements
 */
data class Transform2d(
  @JvmField val translation: Vector2d<DistanceUnit>,
  @JvmField val rotation: Rotation2d,
) {
  /**
   * Constructs a Transform2d from a translation vector and rotation angle.
   *
   * @param translation the translational component
   * @param rotation the rotation angle in radians
   */
  constructor(
    translation: Vector2d<DistanceUnit>,
    rotation: Double,
  ) : this(translation, Rotation2d.exp(rotation))

  /**
   * Constructs a Transform2d from x and y distance measurements and a rotation.
   *
   * @param x the x-component of translation
   * @param y the y-component of translation
   * @param rotation the rotational component
   */
  constructor(x: Distance, y: Distance, rotation: Rotation2d) : this(Vector2d(x, y), rotation)

  /**
   * Constructs a Transform2d from x and y distance measurements and a rotation angle.
   *
   * @param x the x-component of translation
   * @param y the y-component of translation
   * @param rotation the rotation angle measurement
   */
  constructor(
    x: Distance,
    y: Distance,
    rotation: Angle,
  ) : this(Vector2d(x, y), Rotation2d.fromAngle(rotation))

  /**
   * Constructs a Transform2d from x and y coordinates (in inches) and a rotation.
   *
   * Convenience constructor that assumes inch units for translation.
   *
   * @param x the x-component of translation in inches
   * @param y the y-component of translation in inches
   * @param rotation the rotational component
   */
  constructor(
    x: Double,
    y: Double,
    rotation: Rotation2d,
  ) : this(Vector2d.displacement(x, y), rotation)

  /**
   * Constructs a Transform2d from x and y coordinates (in inches) and a rotation angle (in radians).
   *
   * Convenience constructor that assumes inch units for translation and radians for rotation.
   *
   * @param x the x-component of translation in inches
   * @param y the y-component of translation in inches
   * @param rotation the rotation angle in radians
   */
  constructor(x: Double, y: Double, rotation: Double) : this(x, y, Rotation2d.exp(rotation))

  /**
   * Constructs a Transform2d representing the transformation from one pose to another.
   *
   * This computes the relative transformation: \(T = P_{initial}^{-1} \circ P_{final}\)
   *
   * @param initial the starting pose
   * @param final the ending pose
   */
  constructor(initial: Pose2d, final: Pose2d) : this(
    translation = initial.heading.inverse() * (final.position - initial.position),
    rotation = initial.heading.inverse() * final.heading,
  )

  /**
   * Adds (composes) two transforms.
   *
   * Composes this transform with another: \(T_1 \circ T_2\)
   *
   * @param other the transform to compose with
   * @return the composed transform
   */
  operator fun plus(other: Transform2d): Transform2d = Transform2d(
    translation = translation + (rotation * other.translation),
    rotation = rotation * other.rotation,
  )

  /**
   * Subtracts (de-composes) two transforms.
   *
   * Computes \(T_1 \circ T_2^{-1}\)
   *
   * @param other the transform to subtract
   * @return the relative transform
   */
  operator fun minus(other: Transform2d): Transform2d = this + other.inverse()

  /**
   * Negates the transform (same as inverse).
   *
   * @return the inverse transform
   */
  operator fun unaryMinus(): Transform2d = inverse()

  /**
   * Scales the transform by a scalar.
   *
   * This scales both the translation and rotation components.
   * Useful for interpolating between transforms.
   *
   * @param scalar the scaling factor
   * @return the scaled transform
   */
  operator fun times(scalar: Double): Transform2d = Transform2d(
    translation = translation * scalar,
    rotation = Rotation2d.exp(rotation.log() * scalar),
  )

  /**
   * Divides the transform by a scalar.
   *
   * @param scalar the divisor
   * @return the scaled transform
   */
  operator fun div(scalar: Double): Transform2d = Transform2d(
    translation = translation / scalar,
    rotation = Rotation2d.exp(rotation.log() / scalar),
  )

  /**
   * Transforms a vector by this transformation.
   *
   * Applies the rotation and translation to the vector.
   *
   * @param vec the vector to transform
   * @return the transformed vector
   */
  operator fun times(vec: Vector2d<DistanceUnit>): Vector2d<DistanceUnit> = rotation * vec + translation

  /**
   * Computes the inverse transformation.
   *
   * If this transform represents A→B, the inverse represents B→A.
   *
   * @return the inverse transform
   */
  fun inverse(): Transform2d = Transform2d(
    translation = rotation.inverse() * -translation,
    rotation = rotation.inverse(),
  )

  /**
   * Linear interpolation (lerp) toward another transform.
   *
   * Interpolates both translation and rotation:
   * - Translation: Linear interpolation of the translation vectors
   * - Rotation: Spherical linear interpolation of the rotations
   *
   * @param other the target transform to interpolate toward
   * @param t the interpolation parameter in range [0, 1]
   * @return the interpolated transform
   */
  fun lerp(other: Transform2d, t: Double): Transform2d {
    require(t in 0.0..1.0) { "Interpolation parameter t must be in range [0, 1], got $t" }

    val interpolatedTranslation = translation.lerp(other.translation, t)
    val interpolatedRotation = rotation.lerp(other.rotation, t)

    return Transform2d(interpolatedTranslation, interpolatedRotation)
  }

  companion object {
    /**
     * The identity transform (no transformation).
     */
    @JvmField
    val identity = Transform2d(Vector2d.zero(Inches), Rotation2d.zero)
  }
}
