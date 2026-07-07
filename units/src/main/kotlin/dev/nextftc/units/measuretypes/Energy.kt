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
import dev.nextftc.units.unittypes.EnergyUnit

/**
 * Immutable measurement of energy.
 *
 * This class represents energy values like joules, kilojoules, watt-hours, etc. It supports
 * arithmetic operations and conversions between different energy units.
 */
class Energy
internal constructor(override val magnitude: Double, override val unit: EnergyUnit) :
  Measure<EnergyUnit> {
  override val baseUnitMagnitude: Double = unit.toBaseUnits(magnitude)

  override fun unaryMinus(): Energy = Energy(-magnitude, unit)

  override fun plus(other: Measure<out EnergyUnit>): Energy {
    val sum = baseUnitMagnitude + other.baseUnitMagnitude
    return Energy(unit.fromBaseUnits(sum), unit)
  }

  override fun minus(other: Measure<out EnergyUnit>): Energy {
    val diff = baseUnitMagnitude - other.baseUnitMagnitude
    return Energy(unit.fromBaseUnits(diff), unit)
  }

  override fun times(multiplier: Double): Energy = Energy(magnitude * multiplier, unit)

  override fun div(divisor: Double): Energy = Energy(magnitude / divisor, unit)

  /**
   * Divides this energy by a time to get power.
   *
   * Power = Energy / Time
   *
   * @param time the time over which the energy is expended
   * @return the power in watts
   */
  operator fun div(time: Time): Power {
    val energyInJoules = this.baseUnitMagnitude
    val timeInSeconds = time.baseUnitMagnitude
    return Power(energyInJoules / timeInSeconds, Watts)
  }

  override fun toString() = toShortString()
}
