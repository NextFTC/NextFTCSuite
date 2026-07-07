/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Measure
import dev.nextftc.units.unittypes.TemperatureUnit

/**
 * Immutable measurement of temperature.
 *
 * This class represents temperature values in Celsius, Fahrenheit, or Kelvin. It supports
 * arithmetic operations and conversions between different temperature units.
 */
class Temperature
internal constructor(
  override val magnitude: Double,
  override val unit: TemperatureUnit,
) : Measure<TemperatureUnit> {
  override val baseUnitMagnitude: Double = unit.toBaseUnits(magnitude)

  override fun unaryMinus(): Temperature = Temperature(-magnitude, unit)

  override fun plus(other: Measure<out TemperatureUnit>): Temperature {
    val otherInBaseUnits = other.baseUnitMagnitude
    val sumInBaseUnits = this.baseUnitMagnitude + otherInBaseUnits
    return Temperature(unit.fromBaseUnits(sumInBaseUnits), unit)
  }

  override fun minus(other: Measure<out TemperatureUnit>): Temperature {
    val otherInBaseUnits = other.baseUnitMagnitude
    val diffInBaseUnits = this.baseUnitMagnitude - otherInBaseUnits
    return Temperature(unit.fromBaseUnits(diffInBaseUnits), unit)
  }

  override fun times(multiplier: Double): Temperature = Temperature(magnitude * multiplier, unit)

  override fun div(divisor: Double): Temperature = Temperature(magnitude / divisor, unit)

  override fun toString() = toShortString()
}
