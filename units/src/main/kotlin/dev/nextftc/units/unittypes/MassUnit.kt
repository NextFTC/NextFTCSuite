/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("MassUnits")

package dev.nextftc.units.unittypes

import dev.nextftc.units.Measure
import dev.nextftc.units.Unit
import dev.nextftc.units.measuretypes.Mass

/**
 * Unit of measurement for mass.
 *
 * Supported units include kilograms (base unit), grams, milligrams, metric tons, pounds, and
 * ounces.
 */
class MassUnit(
  baseUnit: MassUnit?,
  toBaseConverter: (Double) -> Double,
  fromBaseConverter: (Double) -> Double,
  unitName: String,
  unitSymbol: String,
) : Unit<MassUnit>(
  baseUnit,
  toBaseConverter,
  fromBaseConverter,
  unitName,
  unitSymbol,
) {
  /**
   * Convenience constructor for defining a derived mass unit from a base unit.
   *
   * This constructor creates a unit whose magnitude is a fixed multiple of the provided
   * base unit. For example, `Kilograms` is defined relative to `Grams` using a
   * multiplier of 1e3.
   *
   * @param baseUnit the unit to derive from (typically the base mass unit)
   * @param baseUnitEquivalent how many base unit units are equal to one of this unit
   * @param name the human-readable name of the unit
   * @param symbol the short symbol used for the unit
   */
  constructor(
    baseUnit: MassUnit,
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

  override fun of(magnitude: Double): Mass = Mass(magnitude, this)

  override fun ofBaseUnits(baseUnitMagnitude: Double): Measure<MassUnit> =
    of(this.fromBaseUnits(baseUnitMagnitude))
}
