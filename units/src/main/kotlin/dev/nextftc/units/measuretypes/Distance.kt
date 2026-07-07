/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Joules
import dev.nextftc.units.Measure
import dev.nextftc.units.unittypes.DistanceUnit

/**
 * Immutable measurement of distance/length.
 *
 * This class represents a distance value with a specific unit (e.g., meters, feet). It supports
 * arithmetic operations and conversions between different distance units.
 */
class Distance
internal constructor(override val magnitude: Double, override val unit: DistanceUnit) :
  Measure<DistanceUnit> {
  override val baseUnitMagnitude: Double = unit.toBaseUnits(magnitude)

  /**
   * Returns a measure equivalent to this one equal to zero minus its current value.
   *
   * @return a measure equal to zero minus this measure
   */
  override fun unaryMinus(): Distance = Distance(-magnitude, unit)

  /**
   * Adds another distance measurement to this one.
   *
   * @param other the measurement to add
   * @return a measure of the sum of both measures
   */
  override fun plus(other: Measure<out DistanceUnit>): Distance {
    val otherInBaseUnits = other.baseUnitMagnitude
    val sumInBaseUnits = this.baseUnitMagnitude + otherInBaseUnits
    return Distance(unit.fromBaseUnits(sumInBaseUnits), unit)
  }

  /**
   * Subtracts another distance measurement from this one.
   *
   * @param other the measurement to subtract
   * @return a measure of the difference between the measures
   */
  override fun minus(other: Measure<out DistanceUnit>): Distance {
    val otherInBaseUnits = other.baseUnitMagnitude
    val diffInBaseUnits = this.baseUnitMagnitude - otherInBaseUnits
    return Distance(unit.fromBaseUnits(diffInBaseUnits), unit)
  }

  /**
   * Multiplies this distance by a scalar unitless multiplier.
   *
   * @param multiplier the scalar multiplication factor
   * @return the scaled result
   */
  override fun times(multiplier: Double): Distance = Distance(magnitude * multiplier, unit)

  /**
   * Divides this distance by a scalar and returns the result.
   *
   * @param divisor the value to divide by
   * @return the division result
   */
  override fun div(divisor: Double): Distance = Distance(magnitude / divisor, unit)

  /**
   * Divides this distance by a time to get velocity (distance per time).
   *
   * @param time the time to divide by
   * @return the velocity (distance/time)
   */
  operator fun div(time: Time): LinearVelocity {
    val velocityUnit =
      dev.nextftc.units.unittypes
        .LinearVelocityUnit(unit, time.unit)
    return LinearVelocity(magnitude / time.magnitude, velocityUnit)
  }

  /**
   * Multiplies this distance by a force to get energy (work done).
   *
   * Work = Distance × Force
   *
   * @param force the force applied over this distance
   * @return the energy (work) in joules
   */
  operator fun times(force: Force): Energy {
    val distanceInMeters = this.baseUnitMagnitude
    val forceInNewtons = force.baseUnitMagnitude
    return Energy(distanceInMeters * forceInNewtons, Joules)
  }

  override fun toString() = toShortString()
}
