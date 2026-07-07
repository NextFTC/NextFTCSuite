/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units

import dev.nextftc.units.measuretypes.Mul
import dev.nextftc.units.measuretypes.Per
import dev.nextftc.units.unittypes.MulUnit
import dev.nextftc.units.unittypes.PerUnit
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.math.withSign

/**
 * A measure holds the magnitude and unit of some dimension, such as distance, time, or speed. Two
 * measures with the same *unit* and *magnitude* are effectively equivalent objects.
 *
 * @param U the unit type of the measure
 */
interface Measure<U : Unit<U>> : Comparable<Measure<U>> {
  companion object {
    /**
     * The threshold for two measures to be considered equivalent if converted to the same unit.
     * This is only needed due to floating-point error.
     */
    const val EQUIVALENCE_THRESHOLD = 1e-12

    /**
     * Returns the measure with the absolute value closest to positive infinity.
     *
     * @param U the type of the units of the measures
     * @param measures the set of measures to compare
     * @return the measure with the greatest positive magnitude, or null if no measures were
     *   provided
     */
    fun <U : Unit<U>> max(vararg measures: Measure<U>): Measure<U>? {
      if (measures.isEmpty()) {
        return null // nothing to compare
      }

      var max: Measure<U>? = null
      for (measure in measures) {
        if (max == null || measure > max) {
          max = measure
        }
      }

      return max
    }

    /**
     * Returns the measure with the absolute value closest to negative infinity.
     *
     * @param U the type of the units of the measures
     * @param measures the set of measures to compare
     * @return the measure with the greatest negative magnitude
     */
    fun <U : Unit<U>> min(vararg measures: Measure<U>): Measure<U>? {
      if (measures.isEmpty()) {
        return null // nothing to compare
      }

      var min: Measure<U>? = null
      for (measure in measures) {
        if (min == null || measure < min) {
          min = measure
        }
      }

      return min
    }
  }

  /**
   * Gets the unitless magnitude of this measure.
   *
   * @return the magnitude in terms of [unit].
   */
  val magnitude: Double

  /**
   * Gets the magnitude of this measure in terms of the base unit. If the unit is the base unit for
   * its system of measure, then the value will be equivalent to [magnitude].
   *
   * @return the magnitude in terms of the base unit
   */
  val baseUnitMagnitude: Double

  /**
   * Gets the units of this measure.
   *
   * @return the unit
   */
  val unit: U

  /**
   * Converts this measure to a measure with a different unit of the same type, eg minutes to
   * seconds. Converting to the same unit is equivalent to calling [magnitude].
   *
   * ```
   * Meters.of(12).in(Feet) // 39.3701
   * Seconds.of(15).in(Minutes) // 0.25
   * ```
   *
   * @param unit the unit to convert this measure to
   * @return the value of this measure in the given unit
   */
  fun into(unit: U): Double = if (this.unit == unit) {
    magnitude
  } else {
    unit.fromBaseUnits(baseUnitMagnitude)
  }

  /**
   * A convenience method to get the base unit of the measurement. Equivalent to `unit.baseUnit`.
   *
   * @return the base unit of measure.
   */
  val baseUnit
    get() = unit.baseUnit

  /**
   * Absolute value of measure.
   *
   * @return the absolute value of this measure in the same unit as this measure.
   */
  val absoluteValue: Measure<U>
    get() = unit.of(magnitude.absoluteValue)

  /**
   * Absolute value of measure.
   *
   * @param unit unit to use
   * @return the absolute value of this measure in the given unit
   */
  fun abs(unit: U): Double = abs(this.into(unit))

  /**
   * Take the sign of another measure.
   *
   * @param other measure from which to take sign
   * @param unit unit to use
   * @return the value of the measure in the given unit with the sign of the provided measure
   */
  fun withSign(other: Measure<U>, unit: U): Double = this.into(unit).withSign(other.into(unit))

  /**
   * Take the sign of another measure. This measure's and the provided measure's signs are
   * considered in this measure's unit.
   *
   * @param other measure from which to take sign
   * @return the value of the measure with the sign of the provided measure
   */
  fun withSign(other: Measure<U>) = unit.of(this.into(unit).withSign(other.into(unit)))

  /**
   * Returns the sign of this measure.
   */
  val sign get() = magnitude.sign

  /**
   * Returns a measure equivalent to this one equal to zero minus its current value. For non-linear
   * unit types like temperature, the zero point is treated as the zero value of the base unit (eg
   * Kelvin). In effect, this means code like `Celsius.of(10).unaryMinus()` returns a value
   * equivalent to -10 Kelvin, and *not* -10° Celsius.
   *
   * @return a measure equal to zero minus this measure
   */
  operator fun unaryMinus(): Measure<U>

  /**
   * Returns a measure equivalent to this one equal to zero minus its current value. For non-linear
   * unit types like temperature, the zero point is treated as the zero value of the base unit (eg
   * Kelvin). In effect, this means code like `Celsius.of(10).negate()` returns a value equivalent
   * to -10 Kelvin, and *not* -10° Celsius.
   *
   * @return a measure equal to zero minus this measure
   * @deprecated use unaryMinus() instead. This was renamed for consistency with other WPILib
   *   classes like Rotation2d
   */
  @Deprecated("use unaryMinus() instead", ReplaceWith("unaryMinus()"))
  fun negate(): Measure<U> = unaryMinus()

  /**
   * Adds another measure of the same unit type to this one.
   *
   * @param other the measurement to add
   * @return a measure of the sum of both measures
   */
  operator fun plus(other: Measure<out U>): Measure<U>

  /**
   * Subtracts another measure of the same unit type from this one.
   *
   * @param other the measurement to subtract
   * @return a measure of the difference between the measures
   */
  operator fun minus(other: Measure<out U>): Measure<U>

  /**
   * Multiplies this measure by a scalar unitless multiplier.
   *
   * @param multiplier the scalar multiplication factor
   * @return the scaled result
   */
  operator fun times(multiplier: Double): Measure<U>

  /**
   * Multiplies this measure by a scalar unitless multiplier.
   *
   * @param multiplier the scalar multiplication factor
   * @return the scaled result
   */
  operator fun times(multiplier: Number): Measure<U> = times(multiplier.toDouble())

  /**
   * Divides this measure by a scalar and returns the result.
   *
   * @param divisor the value to divide by
   * @return the division result
   */
  operator fun div(divisor: Double): Measure<U>

  /**
   * Divides this measure by a scalar and returns the result.
   *
   * @param divisor the value to divide by
   * @return the division result
   */
  operator fun div(divisor: Number): Measure<U> = div(divisor.toDouble())

  /**
   * Divides this measure by another measure and returns the ratio as a dimensionless value.
   *
   * @param other the other measure to divide by
   * @return the ratio of the two measures
   */
  fun div(other: Measure<out U>): Double = baseUnitMagnitude / other.baseUnitMagnitude

  /**
   * Multiplies this measure by another measure of a different type to create a compound unit. For
   * example, Distance * Distance creates an area measurement, or Voltage * Time creates charge.
   *
   * Subclasses can override this to return more specific types (e.g., LinearVelocity * Time →
   * Distance).
   *
   * @param V the type of the other unit
   * @param other the other measure to multiply by
   * @return a Mul measurement representing the product of the two measures
   */
  operator fun <V : Unit<V>> times(other: Measure<V>): Mul<U, V> {
    val mulUnit =
      MulUnit(this.unit, other.unit)
    return Mul(this.magnitude * other.magnitude, mulUnit)
  }

  /**
   * Divides this measure by another measure of a different type to create a ratio unit. For
   * example, Distance / Time creates a velocity measurement, or Voltage / Distance creates electric
   * field strength.
   *
   * Subclasses can override this to return more specific types (e.g., Distance / Time →
   * LinearVelocity).
   *
   * @param V the type of the other unit
   * @param other the other measure to divide by
   * @return a Per measurement representing the ratio of the two measures
   */
  operator fun <V : Unit<V>> div(other: Measure<V>): Per<U, V> {
    val perUnit =
      PerUnit(this.unit, other.unit)
    return Per(this.magnitude / other.magnitude, perUnit)
  }

  /**
   * Checks if this measure is near another measure of the same unit. Provide a variance threshold
   * for use for a +/- scalar, such as 0.05 for +/- 5%.
   *
   * ```
   * Inches.of(11).isNear(Inches.of(10), 0.1) // true
   * Inches.of(12).isNear(Inches.of(10), 0.1) // false
   * ```
   *
   * @param other the other measurement to compare against
   * @param varianceThreshold the acceptable variance threshold, in terms of an acceptable +/- error
   *   range multiplier. Checking if a value is within 10% means a value of 0.1 should be passed;
   *   checking if a value is within 1% means a value of 0.01 should be passed, and so on.
   * @return true if this unit is near the other measure, otherwise false
   */
  fun isNear(other: Measure<*>, varianceThreshold: Double): Boolean {
    if (!this.unit.baseUnit.equivalent(other.unit.baseUnit)) {
      return false // Disjoint units, not compatible
    }

    // abs so negative inputs are calculated correctly
    val tolerance = abs(other.baseUnitMagnitude * varianceThreshold)

    return abs(this.baseUnitMagnitude - other.baseUnitMagnitude) <= tolerance
  }

  /**
   * Checks if this measure is near another measure of the same unit, with a specified tolerance of
   * the same unit.
   *
   * ```
   * Meters.of(1).isNear(Meters.of(1.2), Millimeters.of(300)) // true
   * Degrees.of(90).isNear(Rotations.of(0.5), Degrees.of(45)) // false
   * ```
   *
   * @param other the other measure to compare against.
   * @param tolerance the tolerance allowed in which the two measures are defined as near each
   *   other.
   * @return true if this unit is near the other measure, otherwise false.
   */
  fun isNear(other: Measure<U>, tolerance: Measure<U>): Boolean = (
    abs(this.baseUnitMagnitude - other.baseUnitMagnitude) <=
      abs(tolerance.baseUnitMagnitude)
    )

  /**
   * Checks if this measure is equivalent to another measure of the same unit.
   *
   * @param other the measure to compare to
   * @return true if this measure is equivalent, false otherwise
   */
  fun isEquivalent(other: Measure<*>): Boolean {
    // Return false for disjoint units that aren't compatible
    return this.unit.baseUnit == other.unit.baseUnit &&
      abs(baseUnitMagnitude - other.baseUnitMagnitude) <= EQUIVALENCE_THRESHOLD
  }

  override operator fun compareTo(other: Measure<U>): Int =
    this.baseUnitMagnitude.compareTo(other.baseUnitMagnitude)

  /**
   * Returns a string representation of this measurement in a shorthand form. The symbol of the
   * backing unit is used, rather than the full name, and the magnitude is represented in scientific
   * notation.
   *
   * @return the short form representation of this measurement
   */
  fun toShortString(): String {
    // eg 1.234e+04 V/m (1234 Volt per Meter in long form)
    return "$magnitude ${unit.symbol()}"
  }

  /**
   * Returns a string representation of this measurement in a longhand form. The name of the backing
   * unit is used, rather than its symbol, and the magnitude is represented in a full string, not
   * scientific notation. (Very large values may be represented in scientific notation, however)
   *
   * @return the long form representation of this measurement
   */
  fun toLongString(): String {
    // eg 1234 Volt per Meter (1.234e+04 V/m in short form)
    return "$magnitude ${unit.name()}"
  }
}
