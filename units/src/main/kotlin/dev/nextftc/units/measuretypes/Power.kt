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
import dev.nextftc.units.unittypes.PowerUnit

/**
 * Immutable measurement of power.
 *
 * This class represents power values like watts, kilowatts, horsepower, etc. It supports arithmetic
 * operations and conversions between different power units.
 */
class Power
internal constructor(override val magnitude: Double, override val unit: PowerUnit) :
  Measure<PowerUnit> {
  override val baseUnitMagnitude: Double = unit.toBaseUnits(magnitude)

  override fun unaryMinus(): Power = Power(-magnitude, unit)

  override fun plus(other: Measure<out PowerUnit>): Power {
    val otherInBaseUnits = other.baseUnitMagnitude
    val sumInBaseUnits = this.baseUnitMagnitude + otherInBaseUnits
    return Power(unit.fromBaseUnits(sumInBaseUnits), unit)
  }

  override fun minus(other: Measure<out PowerUnit>): Power {
    val otherInBaseUnits = other.baseUnitMagnitude
    val diffInBaseUnits = this.baseUnitMagnitude - otherInBaseUnits
    return Power(unit.fromBaseUnits(diffInBaseUnits), unit)
  }

  override fun times(multiplier: Double): Power = Power(magnitude * multiplier, unit)

  override fun div(divisor: Double): Power = Power(magnitude / divisor, unit)

  /**
   * Multiplies this power by a time to get energy.
   *
   * Energy = Power × Time
   *
   * @param time the duration of power application
   * @return the energy in joules
   */
  operator fun times(time: Time): Energy {
    val powerInWatts = this.baseUnitMagnitude
    val timeInSeconds = time.baseUnitMagnitude
    return Energy(powerInWatts * timeInSeconds, Joules)
  }

  override fun toString() = toShortString()
}
