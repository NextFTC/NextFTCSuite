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
import dev.nextftc.units.unittypes.CurrentUnit

/**
 * Immutable measurement of electrical current.
 *
 * This class represents current values like amperes, milliamperes, etc. It supports arithmetic
 * operations and conversions between different current units.
 */
class Current
internal constructor(override val magnitude: Double, override val unit: CurrentUnit) :
  Measure<CurrentUnit> {
  override val baseUnitMagnitude: Double = unit.toBaseUnits(magnitude)

  override fun unaryMinus(): Current = Current(-magnitude, unit)

  override fun plus(other: Measure<out CurrentUnit>): Current {
    val otherInBaseUnits = other.baseUnitMagnitude
    val sumInBaseUnits = this.baseUnitMagnitude + otherInBaseUnits
    return Current(unit.fromBaseUnits(sumInBaseUnits), unit)
  }

  override fun minus(other: Measure<out CurrentUnit>): Current {
    val otherInBaseUnits = other.baseUnitMagnitude
    val diffInBaseUnits = this.baseUnitMagnitude - otherInBaseUnits
    return Current(unit.fromBaseUnits(diffInBaseUnits), unit)
  }

  override fun times(multiplier: Double): Current = Current(magnitude * multiplier, unit)

  override fun div(divisor: Double): Current = Current(magnitude / divisor, unit)

  /**
   * Multiplies this current by a voltage to get power.
   *
   * Power = Current × Voltage (P = I × V)
   *
   * @param voltage the voltage across the circuit
   * @return the power in watts
   */
  operator fun times(voltage: Voltage): Power {
    val currentInAmperes = this.baseUnitMagnitude
    val voltageInVolts = voltage.baseUnitMagnitude
    return Power(currentInAmperes * voltageInVolts, Watts)
  }

  override fun toString() = toShortString()
}
