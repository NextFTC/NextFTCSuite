/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("TimeUnits")

package dev.nextftc.units.unittypes

import dev.nextftc.units.Measure
import dev.nextftc.units.Unit
import dev.nextftc.units.durationUnit
import dev.nextftc.units.measuretypes.Time
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class TimeUnit(
  baseUnit: TimeUnit?,
  toBaseConverter: (Double) -> Double,
  fromBaseConverter: (Double) -> Double,
  unitName: String,
  unitSymbol: String,
) : Unit<TimeUnit>(baseUnit, toBaseConverter, fromBaseConverter, unitName, unitSymbol) {
  /**
   * Convenience constructor for defining a derived time unit from a base time unit.
   *
   * This constructor creates a unit whose magnitude is a fixed multiple of the provided
   * base unit. For example, `Minutes` is defined relative to `Seconds` using a
   * multiplier of 60.0.
   *
   * @param baseUnit the unit to derive from (typically the base time unit)
   * @param baseUnitEquivalent how many base unit units are equal to one of this unit
   * @param name the human-readable name of the unit
   * @param symbol the short symbol used for the unit
   */
  constructor(
    baseUnit: TimeUnit,
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
   * Implementations are **strongly** recommended to sharpen the return type to a unit-specific
   * measurement implementation.
   *
   * @param magnitude the magnitude of the measurement.
   * @return the measurement object
   */
  override fun of(magnitude: Double): Time {
    require(this.durationUnit != null) {
      "Unit $this does not have a corresponding internal duration unit."
    }

    return Time(magnitude.toDuration(this.durationUnit!!), this)
  }

  /**
   * Creates a new immutable measurement of the given magnitude in terms of this unit's base unit.
   * Implementations are **strongly** recommended to sharpen the return type to a unit-specific
   * measurement implementation.
   *
   * @param baseUnitMagnitude the magnitude in terms of the base unit
   * @return the measurement object
   */
  override fun ofBaseUnits(baseUnitMagnitude: Double): Measure<TimeUnit> =
    of(this.fromBaseUnits(baseUnitMagnitude))
}
