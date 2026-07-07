/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("ForceUnits")

package dev.nextftc.units.unittypes

import dev.nextftc.units.Measure
import dev.nextftc.units.Unit
import dev.nextftc.units.measuretypes.Force

/**
 * Unit of measurement for force.
 *
 * Supported units include newtons (base unit), kilonewtons, pounds-force, and kilograms-force.
 * Force is dimensionally equivalent to mass × acceleration = mass × distance / time².
 */
class ForceUnit(
  baseUnit: ForceUnit?,
  toBaseConverter: (Double) -> Double,
  fromBaseConverter: (Double) -> Double,
  unitName: String,
  unitSymbol: String,
) : Unit<ForceUnit>(
  baseUnit,
  toBaseConverter,
  fromBaseConverter,
  unitName,
  unitSymbol,
) {
  /**
   * Convenience constructor for defining a derived force unit from a base unit.
   *
   * This constructor creates a unit whose magnitude is a fixed multiple of the provided
   * base unit. For example, `Kilonewtons` is defined relative to `Newtons` using a
   * multiplier of 1e3.
   *
   * @param baseUnit the unit to derive from (typically the base force unit)
   * @param baseUnitEquivalent how many base unit units are equal to one of this unit
   * @param name the human-readable name of the unit
   * @param symbol the short symbol used for the unit
   */
  constructor(
    baseUnit: ForceUnit,
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

  override fun of(magnitude: Double): Force = Force(magnitude, this)

  override fun ofBaseUnits(baseUnitMagnitude: Double): Measure<ForceUnit> =
    of(this.fromBaseUnits(baseUnitMagnitude))
}
