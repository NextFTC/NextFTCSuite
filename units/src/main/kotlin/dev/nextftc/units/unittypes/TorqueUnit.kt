/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("TorqueUnits")

package dev.nextftc.units.unittypes

import dev.nextftc.units.Measure
import dev.nextftc.units.Unit
import dev.nextftc.units.measuretypes.Torque

/**
 * Unit of measurement for torque (force × distance).
 *
 * Supported units include newton-meters (base unit), pound-feet, newton-centimeters. Torque is
 * dimensionally equivalent to force × distance = mass × distance² / time².
 */
class TorqueUnit(
  baseUnit: TorqueUnit?,
  toBaseConverter: (Double) -> Double,
  fromBaseConverter: (Double) -> Double,
  unitName: String,
  unitSymbol: String,
) : Unit<TorqueUnit>(
  baseUnit,
  toBaseConverter,
  fromBaseConverter,
  unitName,
  unitSymbol,
) {
  /**
   * Convenience constructor for defining a derived torque unit from a base unit.
   *
   * This constructor creates a unit whose magnitude is a fixed multiple of the provided
   * base unit. For example, `NewtonMeters` derivatives can be defined using appropriate multipliers.
   *
   * @param baseUnit the unit to derive from (typically the base torque unit)
   * @param baseUnitEquivalent how many base unit units are equal to one of this unit
   * @param name the human-readable name of the unit
   * @param symbol the short symbol used for the unit
   */
  constructor(
    baseUnit: TorqueUnit,
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

  override fun of(magnitude: Double): Torque = Torque(magnitude, this)

  override fun ofBaseUnits(baseUnitMagnitude: Double): Measure<TorqueUnit> =
    of(this.fromBaseUnits(baseUnitMagnitude))
}
