/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Measure
import dev.nextftc.units.Nanoseconds
import dev.nextftc.units.durationUnit
import dev.nextftc.units.unittypes.TimeUnit
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class Time
internal constructor(private val duration: Duration, override val unit: TimeUnit) :
  Measure<TimeUnit> {
  override val magnitude = duration.toDouble(unit.durationUnit ?: DurationUnit.MILLISECONDS)

  override val baseUnitMagnitude: Double =
    duration.toDouble(unit.baseUnit.durationUnit ?: DurationUnit.MILLISECONDS)

  /**
   * Returns a measure equivalent to this one equal to zero minus its current value. For non-linear
   * unit types like temperature, the zero point is treated as the zero value of the base unit (eg
   * Kelvin). In effect, this means code like `Celsius.of(10).unaryMinus()` returns a value
   * equivalent to -10 Kelvin, and *not* -10° Celsius.
   *
   * @return a measure equal to zero minus this measure
   */
  override fun unaryMinus(): Measure<TimeUnit> = this.times(-1.0)

  /**
   * Adds another measure of the same unit type to this one.
   *
   * @param other the measurement to add
   * @return a measure of the sum of both measures
   */
  override fun plus(other: Measure<out TimeUnit>): Time {
    val baseUnitDuration = baseUnit.durationUnit ?: DurationUnit.SECONDS
    val otherDuration = other.baseUnitMagnitude.toDuration(baseUnitDuration)

    return Time(this.duration + otherDuration, unit)
  }

  /**
   * Subtracts another measure of the same unit type from this one.
   *
   * @param other the measurement to subtract
   * @return a measure of the difference between the measures
   */
  override fun minus(other: Measure<out TimeUnit>): Time {
    val baseUnitDuration = baseUnit.durationUnit ?: DurationUnit.SECONDS
    val otherDuration = other.baseUnitMagnitude.toDuration(baseUnitDuration)

    return Time(this.duration - otherDuration, unit)
  }

  /**
   * Multiplies this measure by a scalar unitless multiplier.
   *
   * @param multiplier the scalar multiplication factor
   * @return the scaled result
   */
  override fun times(multiplier: Double): Time = Time(this.duration * multiplier, unit)

  /**
   * Divides this measure by a scalar and returns the result.
   *
   * @param divisor the value to divide by
   * @return the division result
   */
  override fun div(divisor: Double): Time = Time(this.duration / divisor, unit)

  /**
   * Multiplies this time by a linear velocity to get distance.
   *
   * @param velocity the linear velocity to multiply by
   * @return the distance traveled
   */
  operator fun times(velocity: LinearVelocity): Distance = velocity * this

  /**
   * Multiplies this time by an angular velocity to get angle.
   *
   * @param velocity the angular velocity to multiply by
   * @return the angle traveled
   */
  operator fun times(velocity: AngularVelocity): Angle = velocity * this

  /**
   * Multiplies this time by a linear acceleration to get velocity.
   *
   * @param acceleration the linear acceleration to multiply by
   * @return the velocity achieved
   */
  operator fun times(acceleration: LinearAcceleration): LinearVelocity = acceleration * this

  /**
   * Multiplies this time by an angular acceleration to get angular velocity.
   *
   * @param acceleration the angular acceleration to multiply by
   * @return the angular velocity achieved
   */
  operator fun times(acceleration: AngularAcceleration): AngularVelocity = acceleration * this

  override fun toString() = toShortString()
}

fun Duration.toTime(): Time = Time(this, Nanoseconds)

fun Time.toDuration(): Duration = magnitude.toDuration(unit.durationUnit ?: DurationUnit.MILLISECONDS)
