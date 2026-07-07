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
import dev.nextftc.units.Watts
import dev.nextftc.units.unittypes.TorqueUnit

/**
 * Immutable measurement of torque.
 *
 * This class represents torque values like newton-meters, pound-feet, etc. It supports arithmetic
 * operations and conversions between different torque units.
 */
class Torque
internal constructor(override val magnitude: Double, override val unit: TorqueUnit) :
  Measure<TorqueUnit> {
  override val baseUnitMagnitude: Double = unit.toBaseUnits(magnitude)

  override fun unaryMinus(): Torque = Torque(-magnitude, unit)

  override fun plus(other: Measure<out TorqueUnit>): Torque {
    val sum = baseUnitMagnitude + other.baseUnitMagnitude
    return Torque(unit.fromBaseUnits(sum), unit)
  }

  override fun minus(other: Measure<out TorqueUnit>): Torque {
    val diff = baseUnitMagnitude - other.baseUnitMagnitude
    return Torque(unit.fromBaseUnits(diff), unit)
  }

  override fun times(multiplier: Double): Torque = Torque(magnitude * multiplier, unit)

  override fun div(divisor: Double): Torque = Torque(magnitude / divisor, unit)

  /**
   * Divides this torque by a distance (moment arm) to get force.
   *
   * Force = Torque / Distance (F = τ / r)
   *
   * @param momentArm the perpendicular distance from the pivot point
   * @return the force in newtons
   */
  operator fun div(momentArm: Distance): Force {
    val torqueInNm = this.baseUnitMagnitude
    val distanceInMeters = momentArm.baseUnitMagnitude
    return Force(torqueInNm / distanceInMeters, Newtons)
  }

  /**
   * Multiplies this torque by an angular velocity to get power.
   *
   * Power = Torque × Angular Velocity (P = τ × ω)
   *
   * @param angularVelocity the angular velocity in radians per second
   * @return the power in watts
   */
  operator fun times(angularVelocity: AngularVelocity): Power {
    val torqueInNm = this.baseUnitMagnitude
    val angularVelocityInRadPerSec = angularVelocity.baseUnitMagnitude
    return Power(torqueInNm * angularVelocityInRadPerSec, Watts)
  }

  override fun toString() = toShortString()
}
