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
import dev.nextftc.units.unittypes.MulUnit

/**
 * Immutable measurement representing the product of two units.
 *
 * This class represents values like torque (Newton-meters), energy (kilowatt-hours), or any other
 * quantity that is the product of two units.
 */
open class Mul<N : Unit<N>, D : Unit<D>>(
  override val magnitude: Double,
  override val unit: MulUnit<N, D>,
) : Measure<MulUnit<N, D>> {
  override val baseUnitMagnitude: Double = unit.toBaseUnits(magnitude)

  /**
   * Returns a measure equivalent to this one equal to zero minus its current value.
   *
   * @return a measure equal to zero minus this measure
   */
  override fun unaryMinus(): Mul<N, D> = Mul(-magnitude, unit)

  /**
   * Adds another measure of the same unit type to this one.
   *
   * @param other the measurement to add
   * @return a measure of the sum of both measures
   */
  override fun plus(other: Measure<out MulUnit<N, D>>): Mul<N, D> {
    val sum = baseUnitMagnitude + other.baseUnitMagnitude
    return Mul(unit.fromBaseUnits(sum), unit)
  }

  /**
   * Subtracts another measure of the same unit type from this one.
   *
   * @param other the measurement to subtract
   * @return a measure of the difference between the measures
   */
  override fun minus(other: Measure<out MulUnit<N, D>>): Mul<N, D> = this + -other

  /**
   * Multiplies this measure by a scalar unitless multiplier.
   *
   * @param multiplier the scalar multiplication factor
   * @return the scaled result
   */
  override fun times(multiplier: Double): Mul<N, D> = Mul(magnitude * multiplier, unit)

  /**
   * Divides this measure by a scalar and returns the result.
   *
   * @param divisor the value to divide by
   * @return the division result
   */
  override fun div(divisor: Double): Mul<N, D> = Mul(magnitude / divisor, unit)

  override fun toString() = toShortString()
}
