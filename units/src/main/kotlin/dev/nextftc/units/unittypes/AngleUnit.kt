/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("AngleUnits")

package dev.nextftc.units.unittypes

import dev.nextftc.units.Measure
import dev.nextftc.units.Unit
import dev.nextftc.units.measuretypes.Angle

/**
 * Unit of measurement for angles.
 *
 * Supported units include radians (base unit), degrees, rotations (full circles), and gradians.
 */
class AngleUnit(
  baseUnit: AngleUnit?,
  toBaseConverter: (Double) -> Double,
  fromBaseConverter: (Double) -> Double,
  unitName: String,
  unitSymbol: String,
) : Unit<AngleUnit>(baseUnit, toBaseConverter, fromBaseConverter, unitName, unitSymbol) {
  /**
   * Convenience constructor for defining a derived angle unit from a base unit.
   *
   * This constructor creates a unit whose magnitude is a fixed multiple of the provided
   * base unit. For example, `Degrees` is defined relative to `Radians` using a
   * multiplier of PI/180.
   *
   * @param baseUnit the unit to derive from (typically the base angle unit)
   * @param baseUnitEquivalent how many base unit units are equal to one of this unit
   * @param name the human-readable name of the unit
   * @param symbol the short symbol used for the unit
   */
  constructor(
    baseUnit: AngleUnit,
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
  override fun of(magnitude: Double): Angle = Angle(magnitude, this)

  /**
   * Creates a new immutable measurement of the given magnitude in terms of this unit's base unit.
   *
   * @param baseUnitMagnitude the magnitude in terms of the base unit
   * @return the measurement object
   */
  override fun ofBaseUnits(baseUnitMagnitude: Double): Measure<AngleUnit> =
    of(this.fromBaseUnits(baseUnitMagnitude))

  /**
   * Combines this unit with a unit of time to create an angular velocity unit.
   *
   * @param time the unit of time
   * @return the combined angular velocity unit
   */
  override fun per(time: TimeUnit): AngularVelocityUnit = PerUnit.of(this, time) as AngularVelocityUnit
}
