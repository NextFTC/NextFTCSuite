/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("PowerUnits")

package dev.nextftc.units.unittypes

import dev.nextftc.units.Measure
import dev.nextftc.units.Unit
import dev.nextftc.units.measuretypes.Power

/**
 * Unit of measurement for power (energy / time).
 *
 * Supported units include watts (base unit), milliwatts, kilowatts, megawatts, and horsepower.
 * Power is dimensionally equivalent to energy / time = mass × distance² / time³.
 */
class PowerUnit(
  baseUnit: PowerUnit?,
  toBaseConverter: (Double) -> Double,
  fromBaseConverter: (Double) -> Double,
  unitName: String,
  unitSymbol: String,
) : Unit<PowerUnit>(
  baseUnit,
  toBaseConverter,
  fromBaseConverter,
  unitName,
  unitSymbol,
) {
  /**
   * Convenience constructor for defining a derived power unit from a base unit.
   *
   * This constructor creates a unit whose magnitude is a fixed multiple of the provided
   * base unit. For example, `Kilowatts` is defined relative to `Watts` using a
   * multiplier of 1e3.
   *
   * @param baseUnit the unit to derive from (typically the base power unit)
   * @param baseUnitEquivalent how many base unit units are equal to one of this unit
   * @param name the human-readable name of the unit
   * @param symbol the short symbol used for the unit
   */
  constructor(
    baseUnit: PowerUnit,
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

  override fun of(magnitude: Double): Power = Power(magnitude, this)

  override fun ofBaseUnits(baseUnitMagnitude: Double): Measure<PowerUnit> =
    of(this.fromBaseUnits(baseUnitMagnitude))
}
