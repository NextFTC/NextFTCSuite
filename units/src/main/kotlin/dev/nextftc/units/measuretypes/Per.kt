/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Measure
import dev.nextftc.units.Unit
import dev.nextftc.units.unittypes.PerUnit

open class Per<N : Unit<N>, D : Unit<D>>(
  override val magnitude: Double,
  override val unit: PerUnit<N, D>,
) : Measure<PerUnit<N, D>> {
  override val baseUnitMagnitude: Double = unit.toBaseUnits(magnitude)

  /**
   * Returns a measure equivalent to this one equal to zero minus its current value. For non-linear
   * unit types like temperature, the zero point is treated as the zero value of the base unit (eg
   * Kelvin). In effect, this means code like `Celsius.of(10).unaryMinus()` returns a value
   * equivalent to -10 Kelvin, and *not* -10° Celsius.
   *
   * @return a measure equal to zero minus this measure
   */
  override fun unaryMinus(): Per<N, D> = Per(-magnitude, unit)

  /**
   * Adds another measure of the same unit type to this one.
   *
   * @param other the measurement to add
   * @return a measure of the sum of both measures
   */
  override fun plus(other: Measure<out PerUnit<N, D>>): Per<N, D> {
    val sum = baseUnitMagnitude + other.baseUnitMagnitude
    return Per(unit.fromBaseUnits(sum), unit)
  }

  /**
   * Subtracts another measure of the same unit type from this one.
   *
   * @param other the measurement to subtract
   * @return a measure of the difference between the measures
   */
  override fun minus(other: Measure<out PerUnit<N, D>>): Per<N, D> = this + -other

  /**
   * Multiplies this measure by a scalar unitless multiplier.
   *
   * @param multiplier the scalar multiplication factor
   * @return the scaled result
   */
  override fun times(multiplier: Double): Per<N, D> = Per(magnitude * multiplier, unit)

  /**
   * Divides this measure by a scalar and returns the result.
   *
   * @param divisor the value to divide by
   * @return the division result
   */
  override fun div(divisor: Double): Per<N, D> = Per(magnitude / divisor, unit)

  override fun toString() = toShortString()
}
