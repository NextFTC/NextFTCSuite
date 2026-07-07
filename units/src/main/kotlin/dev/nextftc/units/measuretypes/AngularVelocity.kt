/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Measure
import dev.nextftc.units.Watts
import dev.nextftc.units.unittypes.AngleUnit
import dev.nextftc.units.unittypes.AngularVelocityUnit
import dev.nextftc.units.unittypes.PerUnit
import dev.nextftc.units.unittypes.TimeUnit

/**
 * Immutable measurement of angular velocity (angle per time).
 *
 * This class represents angular velocity values like radians per second, RPM, etc. All arithmetic
 * operations return AngularVelocity for type safety.
 */
class AngularVelocity(magnitude: Double, unit: AngularVelocityUnit) :
  Per<AngleUnit, TimeUnit>(magnitude, unit) {
  override fun unaryMinus(): AngularVelocity = AngularVelocity(-magnitude, unit as AngularVelocityUnit)

  override fun plus(other: Measure<out PerUnit<AngleUnit, TimeUnit>>): AngularVelocity {
    val sum = baseUnitMagnitude + other.baseUnitMagnitude
    return AngularVelocity(unit.fromBaseUnits(sum), unit as AngularVelocityUnit)
  }

  override fun minus(other: Measure<out PerUnit<AngleUnit, TimeUnit>>): AngularVelocity = this + -other

  override fun times(multiplier: Double): AngularVelocity =
    AngularVelocity(magnitude * multiplier, unit as AngularVelocityUnit)

  override fun div(divisor: Double): AngularVelocity =
    AngularVelocity(magnitude / divisor, unit as AngularVelocityUnit)

  /**
   * Multiplies this angular velocity by a time to get angle.
   *
   * @param time the time to multiply by
   * @return the angle traveled
   */
  operator fun times(time: Time): Angle {
    val angleUnit = (unit as AngularVelocityUnit).numerator
    val timeInCorrectUnit = time.into(unit.denominator)
    return Angle(magnitude * timeInCorrectUnit, angleUnit)
  }

  /**
   * Divides this angular velocity by a time to get angular acceleration.
   *
   * @param time the time to divide by
   * @return the angular acceleration (angular velocity/time)
   */
  operator fun div(time: Time): AngularAcceleration {
    val accelerationUnit =
      dev.nextftc.units.unittypes.AngularAccelerationUnit(
        unit as AngularVelocityUnit,
        time.unit,
      )
    return AngularAcceleration(magnitude / time.magnitude, accelerationUnit)
  }

  /**
   * Multiplies this angular velocity by a torque to get power.
   *
   * Power = Angular Velocity × Torque (P = ω × τ)
   *
   * @param torque the torque being applied
   * @return the power in watts
   */
  operator fun times(torque: Torque): Power {
    val angularVelocityInRadPerSec = this.baseUnitMagnitude
    val torqueInNm = torque.baseUnitMagnitude
    return Power(angularVelocityInRadPerSec * torqueInNm, Watts)
  }

  override fun toString() = toShortString()
}
