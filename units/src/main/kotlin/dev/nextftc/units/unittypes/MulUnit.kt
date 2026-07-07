/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("MulUnits")

package dev.nextftc.units.unittypes

import dev.nextftc.units.Unit
import dev.nextftc.units.measuretypes.Mul

/**
 * Represents a unit that is the product of two other units (e.g., Newton-meters for torque,
 * kilowatt-hours for energy).
 *
 * The MulUnit automatically normalizes to use base units for internal calculations. For example, if
 * you create a MulUnit with Kilometers and Hours, it internally works with Meters and Seconds (the
 * base units), ensuring that conversions work correctly.
 *
 * @param N the type of the first unit
 * @param D the type of the second unit
 * @param first the first unit (e.g., Newtons, Meters)
 * @param second the second unit (e.g., Meters, Seconds)
 */
open class MulUnit<N : Unit<N>, D : Unit<D>>(val first: N, val second: D) :
  Unit<MulUnit<N, D>>(
    null,
    { value ->
      // Convert to base units: (first * second) -> (baseFirst * baseSecond)
      // Example: newton-meters -> (kg⋅m/s²)⋅m = kg⋅m²/s²
      val firstInBase = first.toBaseUnits(value)
      val secondRatio = second.toBaseUnits(1.0)
      firstInBase * secondRatio
    },
    { baseValue ->
      // Convert from base units: (baseFirst * baseSecond) -> (first * second)
      val firstFromBase = first.fromBaseUnits(baseValue)
      val secondRatio = second.toBaseUnits(1.0)
      firstFromBase / secondRatio
    },
    "$first⋅$second",
    "$first⋅$second",
  ) {
  /**
   * The base MulUnit using the base units of both first and second. For example, Kilometers⋅Hours
   * would have a baseMulUnit of Meters⋅Seconds.
   */
  val baseMulUnit: MulUnit<N, D> by lazy {
    if (first == first.baseUnit && second == second.baseUnit) {
      this
    } else {
      MulUnit(first.baseUnit, second.baseUnit)
    }
  }

  override fun of(magnitude: Double): Mul<N, D> = Mul(magnitude, this)

  override fun ofBaseUnits(baseUnitMagnitude: Double): Mul<N, D> = of(this.fromBaseUnits(baseUnitMagnitude))
}
