/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Measure
import dev.nextftc.units.Newtons
import dev.nextftc.units.unittypes.DistanceUnit
import dev.nextftc.units.unittypes.LinearAccelerationUnit
import dev.nextftc.units.unittypes.LinearVelocityUnit
import dev.nextftc.units.unittypes.PerUnit
import dev.nextftc.units.unittypes.TimeUnit

/**
 * Immutable measurement of linear acceleration (velocity per time, or distance per time squared).
 *
 * This class represents acceleration values like meters per second squared. All arithmetic
 * operations return LinearAcceleration for type safety.
 */
class LinearAcceleration(magnitude: Double, unit: LinearAccelerationUnit) :
  Per<PerUnit<DistanceUnit, TimeUnit>, TimeUnit>(magnitude, unit) {
  override fun unaryMinus(): LinearAcceleration =
    LinearAcceleration(-magnitude, unit as LinearAccelerationUnit)

  override fun plus(
    other: Measure<out PerUnit<PerUnit<DistanceUnit, TimeUnit>, TimeUnit>>,
  ): LinearAcceleration {
    val sum = baseUnitMagnitude + other.baseUnitMagnitude
    return LinearAcceleration(unit.fromBaseUnits(sum), unit as LinearAccelerationUnit)
  }

  override fun minus(
    other: Measure<out PerUnit<PerUnit<DistanceUnit, TimeUnit>, TimeUnit>>,
  ): LinearAcceleration = this + -other

  override fun times(multiplier: Double): LinearAcceleration =
    LinearAcceleration(magnitude * multiplier, unit as LinearAccelerationUnit)

  override fun div(divisor: Double): LinearAcceleration =
    LinearAcceleration(magnitude / divisor, unit as LinearAccelerationUnit)

  /**
   * Multiplies this acceleration by a time to get velocity.
   *
   * @param time the time to multiply by
   * @return the velocity achieved
   */
  operator fun times(time: Time): LinearVelocity {
    val velocityUnit: LinearVelocityUnit =
      when (unit.numerator) {
        is LinearVelocityUnit -> unit.numerator
        else -> LinearVelocityUnit(unit.numerator.numerator, unit.numerator.denominator)
      }
    val timeInCorrectUnit = time.into((unit as LinearAccelerationUnit).denominator)
    return LinearVelocity(magnitude * timeInCorrectUnit, velocityUnit)
  }

  /**
   * Multiplies this acceleration by a mass to get force.
   *
   * Force = Acceleration × Mass (F = a × m)
   *
   * @param mass the mass being accelerated
   * @return the force in newtons
   */
  operator fun times(mass: Mass): Force {
    val accelerationInMps2 = this.baseUnitMagnitude
    val massInKg = mass.baseUnitMagnitude
    return Force(accelerationInMps2 * massInKg, Newtons)
  }

  override fun toString() = toShortString()
}
