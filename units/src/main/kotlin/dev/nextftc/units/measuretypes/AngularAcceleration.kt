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
import dev.nextftc.units.unittypes.AngularAccelerationUnit
import dev.nextftc.units.unittypes.AngularVelocityUnit
import dev.nextftc.units.unittypes.PerUnit
import dev.nextftc.units.unittypes.TimeUnit

/**
 * Immutable measurement of angular acceleration (angular velocity per time, or angle per time
 * squared).
 *
 * This class represents angular acceleration values like radians per second squared. All arithmetic
 * operations return AngularAcceleration for type safety.
 */
class AngularAcceleration(magnitude: Double, unit: AngularAccelerationUnit) :
  Per<PerUnit<AngleUnit, TimeUnit>, TimeUnit>(magnitude, unit) {
  override fun unaryMinus(): AngularAcceleration =
    AngularAcceleration(-magnitude, unit as AngularAccelerationUnit)

  override fun plus(
    other: Measure<out PerUnit<PerUnit<AngleUnit, TimeUnit>, TimeUnit>>,
  ): AngularAcceleration {
    val sum = baseUnitMagnitude + other.baseUnitMagnitude
    return AngularAcceleration(unit.fromBaseUnits(sum), unit as AngularAccelerationUnit)
  }

  override fun minus(
    other: Measure<out PerUnit<PerUnit<AngleUnit, TimeUnit>, TimeUnit>>,
  ): AngularAcceleration = this + -other

  override fun times(multiplier: Double): AngularAcceleration =
    AngularAcceleration(magnitude * multiplier, unit as AngularAccelerationUnit)

  override fun div(divisor: Double): AngularAcceleration =
    AngularAcceleration(magnitude / divisor, unit as AngularAccelerationUnit)

  /**
   * Multiplies this angular acceleration by a time to get angular velocity.
   *
   * @param time the time to multiply by
   * @return the angular velocity achieved
   */
  operator fun times(time: Time): AngularVelocity {
    val velocityUnit: AngularVelocityUnit =
      when (unit.numerator) {
        is AngularVelocityUnit -> unit.numerator
        else -> AngularVelocityUnit(unit.numerator.numerator, unit.numerator.denominator)
      }
    val timeInCorrectUnit = time.into(unit.denominator)
    return AngularVelocity(magnitude * timeInCorrectUnit, velocityUnit)
  }

  override fun toString() = toShortString()
}
