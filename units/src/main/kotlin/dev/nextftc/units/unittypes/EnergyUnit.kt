/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("EnergyUnits")

package dev.nextftc.units.unittypes

import dev.nextftc.units.Measure
import dev.nextftc.units.Unit
import dev.nextftc.units.measuretypes.Energy

/**
 * Unit of measurement for energy (force × distance).
 *
 * Supported units include joules (base unit), kilojoules, watt-hours, kilowatt-hours. Energy is
 * dimensionally equivalent to force × distance = mass × distance² / time².
 *
 * Note: While energy and torque have the same dimensions, they represent different physical
 * quantities - energy is a scalar, torque is a vector (moment).
 */
class EnergyUnit(
  baseUnit: EnergyUnit?,
  toBaseConverter: (Double) -> Double,
  fromBaseConverter: (Double) -> Double,
  unitName: String,
  unitSymbol: String,
) : Unit<EnergyUnit>(
  baseUnit,
  toBaseConverter,
  fromBaseConverter,
  unitName,
  unitSymbol,
) {
  /**
   * Convenience constructor for defining a derived energy unit from a base unit.
   *
   * This constructor creates a unit whose magnitude is a fixed multiple of the provided
   * base unit. For example, `Joules` derivatives such as `Kilojoules` are defined using
   * an appropriate multiplier.
   *
   * @param baseUnit the unit to derive from (typically the base energy unit)
   * @param baseUnitEquivalent how many base unit units are equal to one of this unit
   * @param name the human-readable name of the unit
   * @param symbol the short symbol used for the unit
   */
  constructor(
    baseUnit: EnergyUnit,
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

  override fun of(magnitude: Double): Energy = Energy(magnitude, this)

  override fun ofBaseUnits(baseUnitMagnitude: Double): Measure<EnergyUnit> =
    of(this.fromBaseUnits(baseUnitMagnitude))
}
