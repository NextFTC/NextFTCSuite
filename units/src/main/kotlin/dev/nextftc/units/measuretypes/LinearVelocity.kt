/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Measure
import dev.nextftc.units.unittypes.DistanceUnit
import dev.nextftc.units.unittypes.LinearVelocityUnit
import dev.nextftc.units.unittypes.PerUnit
import dev.nextftc.units.unittypes.TimeUnit

/**
 * Immutable measurement of linear velocity (distance per time).
 *
 * This class represents velocity values like meters per second, miles per hour, etc. All arithmetic
 * operations return LinearVelocity for type safety.
 */
class LinearVelocity(magnitude: Double, unit: LinearVelocityUnit) :
  Per<DistanceUnit, TimeUnit>(magnitude, unit) {
  override fun unaryMinus(): LinearVelocity = LinearVelocity(-magnitude, unit as LinearVelocityUnit)

  override fun plus(other: Measure<out PerUnit<DistanceUnit, TimeUnit>>): LinearVelocity {
    val sum = baseUnitMagnitude + other.baseUnitMagnitude
    return LinearVelocity(unit.fromBaseUnits(sum), unit as LinearVelocityUnit)
  }

  override fun minus(other: Measure<out PerUnit<DistanceUnit, TimeUnit>>): LinearVelocity = this + -other

  override fun times(multiplier: Double): LinearVelocity =
    LinearVelocity(magnitude * multiplier, unit as LinearVelocityUnit)

  override fun div(divisor: Double): LinearVelocity =
    LinearVelocity(magnitude / divisor, unit as LinearVelocityUnit)

  /**
   * Multiplies this velocity by a time to get distance.
   *
   * @param time the time to multiply by
   * @return the distance traveled
   */
  operator fun times(time: Time): Distance {
    val distanceUnit = (unit as LinearVelocityUnit).numerator
    val timeInCorrectUnit = time.into(unit.denominator)
    return Distance(magnitude * timeInCorrectUnit, distanceUnit)
  }

  /**
   * Divides this velocity by a time to get acceleration.
   *
   * @param time the time to divide by
   * @return the acceleration (velocity/time)
   */
  operator fun div(time: Time): LinearAcceleration {
    val accelerationUnit =
      dev.nextftc.units.unittypes.LinearAccelerationUnit(
        unit as LinearVelocityUnit,
        time.unit,
      )
    return LinearAcceleration(magnitude / time.magnitude, accelerationUnit)
  }

  override fun toString() = toShortString()
}
