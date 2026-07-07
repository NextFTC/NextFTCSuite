/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Measure
import dev.nextftc.units.unittypes.AngleUnit

/**
 * Immutable measurement of angle.
 *
 * This class represents an angle value with a specific unit (e.g., radians, degrees). It supports
 * arithmetic operations and conversions between different angle units.
 */
class Angle
internal constructor(override val magnitude: Double, override val unit: AngleUnit) :
  Measure<AngleUnit> {
  override val baseUnitMagnitude: Double = unit.toBaseUnits(magnitude)

  /**
   * Returns a measure equivalent to this one equal to zero minus its current value.
   *
   * @return a measure equal to zero minus this measure
   */
  override fun unaryMinus(): Angle = Angle(-magnitude, unit)

  /**
   * Adds another angle measurement to this one.
   *
   * @param other the measurement to add
   * @return a measure of the sum of both measures
   */
  override fun plus(other: Measure<out AngleUnit>): Angle {
    val otherInBaseUnits = other.baseUnitMagnitude
    val sumInBaseUnits = this.baseUnitMagnitude + otherInBaseUnits
    return Angle(unit.fromBaseUnits(sumInBaseUnits), unit)
  }

  /**
   * Subtracts another angle measurement from this one.
   *
   * @param other the measurement to subtract
   * @return a measure of the difference between the measures
   */
  override fun minus(other: Measure<out AngleUnit>): Angle {
    val otherInBaseUnits = other.baseUnitMagnitude
    val diffInBaseUnits = this.baseUnitMagnitude - otherInBaseUnits
    return Angle(unit.fromBaseUnits(diffInBaseUnits), unit)
  }

  /**
   * Multiplies this angle by a scalar unitless multiplier.
   *
   * @param multiplier the scalar multiplication factor
   * @return the scaled result
   */
  override fun times(multiplier: Double): Angle = Angle(magnitude * multiplier, unit)

  /**
   * Divides this angle by a scalar and returns the result.
   *
   * @param divisor the value to divide by
   * @return the division result
   */
  override fun div(divisor: Double): Angle = Angle(magnitude / divisor, unit)

  /**
   * Divides this angle by a time to get angular velocity (angle per time).
   *
   * @param time the time to divide by
   * @return the angular velocity (angle/time)
   */
  operator fun div(time: Time): AngularVelocity {
    val velocityUnit =
      dev.nextftc.units.unittypes
        .AngularVelocityUnit(unit, time.unit)
    return AngularVelocity(magnitude / time.magnitude, velocityUnit)
  }

  override fun toString() = toShortString()
}
