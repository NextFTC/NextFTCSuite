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
import dev.nextftc.units.unittypes.MassUnit

/**
 * Immutable measurement of mass.
 *
 * This class represents mass values like kilograms, pounds, etc. It supports arithmetic operations
 * and conversions between different mass units.
 */
class Mass
internal constructor(override val magnitude: Double, override val unit: MassUnit) :
  Measure<MassUnit> {
  override val baseUnitMagnitude: Double = unit.toBaseUnits(magnitude)

  override fun unaryMinus(): Mass = Mass(-magnitude, unit)

  override fun plus(other: Measure<out MassUnit>): Mass {
    val otherInBaseUnits = other.baseUnitMagnitude
    val sumInBaseUnits = this.baseUnitMagnitude + otherInBaseUnits
    return Mass(unit.fromBaseUnits(sumInBaseUnits), unit)
  }

  override fun minus(other: Measure<out MassUnit>): Mass {
    val otherInBaseUnits = other.baseUnitMagnitude
    val diffInBaseUnits = this.baseUnitMagnitude - otherInBaseUnits
    return Mass(unit.fromBaseUnits(diffInBaseUnits), unit)
  }

  override fun times(multiplier: Double): Mass = Mass(magnitude * multiplier, unit)

  override fun div(divisor: Double): Mass = Mass(magnitude / divisor, unit)

  /**
   * Multiplies this mass by a linear acceleration to get force.
   *
   * Force = Mass × Acceleration (F = ma)
   *
   * @param acceleration the acceleration
   * @return the force in newtons
   */
  operator fun times(acceleration: LinearAcceleration): Force {
    val massInKg = this.baseUnitMagnitude
    val accelerationInMps2 = acceleration.baseUnitMagnitude
    return Force(massInKg * accelerationInMps2, Newtons)
  }

  override fun toString() = toShortString()
}
