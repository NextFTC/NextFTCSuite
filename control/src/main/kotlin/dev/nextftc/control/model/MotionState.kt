/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.model

import dev.nextftc.linalg.Vector
import dev.nextftc.linalg.vectorOf
import dev.nextftc.units.Measure
import dev.nextftc.units.Seconds
import dev.nextftc.units.Unit
import dev.nextftc.units.measuretypes.Per
import dev.nextftc.units.unittypes.PerUnit
import dev.nextftc.units.unittypes.TimeUnit

/**
 * The kinematic state of a system, parameterized by position unit type.
 *
 * Represents the complete motion state of a system at a point in time, including
 * position, velocity, and acceleration. Velocity is derived as position per time,
 * and acceleration as velocity per time.
 *
 * This class is generic over the position unit type, allowing it to be used
 * for both linear motion (with [dev.nextftc.units.unittypes.DistanceUnit]) and angular motion
 * (with [dev.nextftc.units.unittypes.AngleUnit]).
 *
 * @param U The position unit type (e.g., [dev.nextftc.units.unittypes.DistanceUnit] for linear
 * motion, [dev.nextftc.units.unittypes.AngleUnit] for angular motion)
 */
data class MotionState<U : Unit<U>> @JvmOverloads constructor(
  val position: Measure<U>,
  val velocity: Per<U, TimeUnit> = position.unit.per(Seconds).of(0.0),
  val acceleration: Per<PerUnit<U, TimeUnit>, TimeUnit> = position.unit.per(
    Seconds,
  ).per(Seconds).of(0.0),
) {
  /**
   * Convenience constructor that accepts raw numeric magnitudes and a unit, converting them
   * into strongly-typed `Measure` and `Per` values used by this class.
   *
   * All three numeric parameters default to `0.0`.
   *
   * Example:
   * ```kotlin
   * // Create a linear MotionState with position in inches, velocity in inches/sec,
   * // and acceleration in inches/sec^2
   * val s = MotionState(Inches, 12.0, 3.0, 0.5)
   * ```
   *
   * @param unit the position unit (used to construct typed measures)
   * @param position position magnitude in `unit`
   * @param velocity velocity magnitude in `unit / second` (interpreted as `unit.per(Seconds)`)
   * @param acceleration acceleration magnitude in `unit / second^2` (interpreted as `unit.per(Seconds).per(Seconds)`)
   */
  @JvmOverloads constructor(
    unit: U,
    position: Double = 0.0,
    velocity: Double = 0.0,
    acceleration: Double = 0.0,
  ) : this(
    unit.of(position),
    unit.per(Seconds).of(velocity),
    unit.per(Seconds).per(Seconds).of(acceleration),
  )

  /**
   * Creates a copy of this motion state with optionally modified values using raw doubles.
   *
   * The double values are interpreted in the same units as the current state's
   * position, velocity, and acceleration.
   *
   * @param position The new position magnitude (defaults to current position magnitude)
   * @param velocity The new velocity magnitude (defaults to current velocity magnitude)
   * @param acceleration The new acceleration magnitude (defaults to current acceleration magnitude)
   * @return A new [MotionState] with the specified values
   */
  fun copy(
    position: Double = this.position.magnitude,
    velocity: Double = this.velocity.magnitude,
    acceleration: Double = this.acceleration.magnitude,
  ) = copy(
    position = this.position.unit.of(position),
    velocity = this.velocity.unit.of(velocity),
    acceleration = this.acceleration.unit.of(acceleration),
  )

  /**
   * Converts this motion state to a 3-element vector.
   *
   * The vector contains [position, velocity, acceleration] in that order,
   * using the magnitude values in the current units.
   *
   * @return A [Vector] containing the position, velocity, and acceleration magnitudes
   */
  fun toVector() = vectorOf(position.magnitude, velocity.magnitude, acceleration.magnitude)

  /**
   * Returns the negation of this motion state.
   *
   * All components (position, velocity, acceleration) are negated.
   *
   * @return A new [MotionState] with all components negated
   */
  operator fun unaryMinus() = copy(position = -position, velocity = -velocity, acceleration = -acceleration)

  /**
   * Adds another motion state to this one, component-wise.
   *
   * @param other The motion state to add
   * @return A new [MotionState] with each component being the sum of the corresponding components
   */
  operator fun plus(other: MotionState<U>) = copy(
    position = position + other.position,
    velocity = velocity + other.velocity,
    acceleration = acceleration + other.acceleration,
  )

  /**
   * Subtracts another motion state from this one, component-wise.
   *
   * This is useful for computing error states (reference - measured).
   *
   * @param other The motion state to subtract
   * @return A new [MotionState] with each component being the difference of the corresponding components
   */
  operator fun minus(other: MotionState<U>) = copy(
    position = position - other.position,
    velocity = velocity - other.velocity,
    acceleration = acceleration - other.acceleration,
  )

  /**
   * Multiplies this motion state by a scalar, component-wise.
   *
   * @param scalar The scalar to multiply by
   * @return A new [MotionState] with each component multiplied by the scalar
   */
  operator fun times(scalar: Double) = copy(
    position = position * scalar,
    velocity = velocity * scalar,
    acceleration = acceleration * scalar,
  )

  /**
   * Multiplies this motion state by a scalar, component-wise.
   *
   * @param scalar The scalar to multiply by (converted to Double)
   * @return A new [MotionState] with each component multiplied by the scalar
   */
  operator fun times(scalar: Number) = times(scalar.toDouble())

  /**
   * Divides this motion state by a scalar, component-wise.
   *
   * @param divisor The scalar to divide by
   * @return A new [MotionState] with each component divided by the divisor
   */
  operator fun div(divisor: Double) = copy(
    position = position / divisor,
    velocity = velocity / divisor,
    acceleration = acceleration / divisor,
  )

  /**
   * Divides this motion state by a scalar, component-wise.
   *
   * @param divisor The scalar to divide by (converted to Double)
   * @return A new [MotionState] with each component divided by the divisor
   */
  operator fun div(divisor: Number) = div(divisor.toDouble())
}
