/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("VoltageUnits")

package dev.nextftc.units.unittypes

import dev.nextftc.units.Measure
import dev.nextftc.units.Unit
import dev.nextftc.units.measuretypes.Voltage

/**
 * Unit of measurement for electrical potential difference (voltage).
 *
 * Supported units include volts (base unit), millivolts, kilovolts, and microvolts.
 */
class VoltageUnit(
  baseUnit: VoltageUnit?,
  toBaseConverter: (Double) -> Double,
  fromBaseConverter: (Double) -> Double,
  unitName: String,
  unitSymbol: String,
) : Unit<VoltageUnit>(baseUnit, toBaseConverter, fromBaseConverter, unitName, unitSymbol) {
  /**
   * Convenience constructor for defining a derived voltage unit from a base unit.
   *
   * This constructor creates a unit whose magnitude is a fixed multiple of the provided
   * base unit. For example, `Millivolts` is defined relative to `Volts` using a
   * multiplier of 1e-3.
   *
   * @param baseUnit the unit to derive from (typically the base voltage unit)
   * @param baseUnitEquivalent how many base unit units are equal to one of this unit
   * @param name the human-readable name of the unit
   * @param symbol the short symbol used for the unit
   */
  constructor(
    baseUnit: VoltageUnit,
    baseUnitEquivalent: Double,
    name: String,
    symbol: String,
  ) : this(
    baseUnit,
    { x -> x * baseUnitEquivalent },
    { x -> x / baseUnitEquivalent },
    name,
    symbol,
  )

  /**
   * Creates a new immutable measurement of the given magnitude in terms of this unit.
   *
   * @param magnitude the magnitude of the measurement.
   * @return the measurement object
   */
  override fun of(magnitude: Double): Voltage = Voltage(magnitude, this)

  /**
   * Creates a new immutable measurement of the given magnitude in terms of this unit's base unit.
   *
   * @param baseUnitMagnitude the magnitude in terms of the base unit
   * @return the measurement object
   */
  override fun ofBaseUnits(baseUnitMagnitude: Double): Measure<VoltageUnit> =
    of(this.fromBaseUnits(baseUnitMagnitude))
}
