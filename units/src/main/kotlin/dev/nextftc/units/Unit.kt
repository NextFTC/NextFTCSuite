/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units

import dev.nextftc.units.unittypes.PerUnit
import dev.nextftc.units.unittypes.TimeUnit
import kotlin.math.abs

/**
 * Unit of measurement that defines a quantity, such as grams, meters, or seconds.
 *
 * This is the base class for units. Actual units (such as Grams and Meters) can be found in the
 * Units class.
 *
 * Units can be organized in a hierarchy where each unit has an optional parent unit. The parent
 * chain is automatically traversed to find the base unit - the unit at the root of the hierarchy
 * that has no parent. For example, in a time unit hierarchy:
 * - Days has parent Hours
 * - Hours has parent Minutes
 * - Minutes has parent Seconds
 * - Seconds has no parent (it is the base unit)
 *
 * Conversions between units are automatically composed through the parent chain, so converting from
 * Days to the base unit (Seconds) properly chains through Hours and Minutes.
 *
 * @param U the self-referencing type parameter for type-safe unit operations
 */
abstract class Unit<U : Unit<U>>
/**
 * Creates a new unit with custom converters to its parent unit.
 *
 * @param parent the parent unit in the hierarchy, or null if this is the base unit. The base unit
 *   is automatically determined by traversing the parent chain until a unit with no parent is
 *   found.
 * @param toParentConverter function to convert a value from this unit to the parent unit. This
 *   converter is automatically composed with the parent's converters to create a full chain to the
 *   base unit.
 * @param fromParentConverter function to convert a value from the parent unit to this unit. This
 *   converter is automatically composed with the parent's converters to create a full chain from
 *   the base unit.
 * @param unitName the name of the unit. This should be a singular noun (so "Meter", not "Meters")
 * @param unitSymbol the short symbol for the unit, such as "m" for meters or "lb." for pounds
 */
protected constructor(
  parent: Unit<U>?,
  toParentConverter: (Double) -> Double,
  fromParentConverter: (Double) -> Double,
  private val unitName: String,
  private val unitSymbol: String,
) {
  /**
   * The base unit for this unit's measurement system. This is found by traversing the parent chain
   * until reaching a unit with no parent. For example:
   * - Seconds.baseUnit returns Seconds (it has no parent)
   * - Milliseconds.baseUnit returns Seconds (direct parent is base)
   * - Hours.baseUnit returns Seconds (traverses: Hours -> Minutes -> Seconds)
   * - Days.baseUnit returns Seconds (traverses: Days -> Hours -> Minutes -> Seconds)
   *
   * All units in the same measurement system share the same base unit.
   */
  @Suppress("UNCHECKED_CAST")
  val baseUnit: U =
    if (parent == null) {
      this as U
    } else {
      // Traverse the chain until we reach the true base unit
      var current = parent
      while (current != current.baseUnit) {
        current = current.baseUnit
      }
      current as U
    }

  /**
   * Converter function to transform values from this unit to the base unit. This is automatically
   * composed through the parent chain. For example, for Days with parent Hours:
   * - toParentConverter converts days to hours (1 day -> 24 hours)
   * - parent.toBaseUnits converts hours through the chain to seconds
   * - Final composition: 1 day -> 24 hours -> 1440 minutes -> 86400 seconds
   */
  private val toBaseConverter: (Double) -> Double =
    if (parent == null || parent == this.baseUnit) {
      toParentConverter
    } else {
      { x -> parent.toBaseUnits(toParentConverter(x)) }
    }

  /**
   * Converter function to transform values from the base unit to this unit. This is automatically
   * composed through the parent chain. For example, for Days with parent Hours:
   * - parent.fromBaseUnits converts from seconds through the chain to hours
   * - fromParentConverter converts hours to days (24 hours -> 1 day)
   * - Final composition: 86400 seconds -> 1440 minutes -> 24 hours -> 1 day
   */
  private val fromBaseConverter: (Double) -> Double =
    if (parent == null || parent == this.baseUnit) {
      fromParentConverter
    } else {
      { x -> fromParentConverter(parent.fromBaseUnits(x)) }
    }

  private val zeroMeasure: Measure<U> by lazy { of(0.0) }
  private val oneMeasure: Measure<U> by lazy { of(1.0) }

  /**
   * Creates a new unit with the given name and multiplier to the base unit.
   *
   * @param parent the base unit, e.g. Meters for distances
   * @param parentEquivalent the multiplier to convert this unit to the base unit of this type. For
   *   example, meters has a multiplier of 1, mm has a multiplier of 1e-3, and km has multiplier of
   *   1e3.
   * @param name the name of the unit. This should be a singular noun (so "Meter", not "Meters")
   * @param symbol the short symbol for the unit, such as "m" for meters or "lb." for pounds
   */
  protected constructor(
    parent: Unit<U>,
    parentEquivalent: Double,
    name: String,
    symbol: String,
  ) : this(parent, { x -> x * parentEquivalent }, { x -> x / parentEquivalent }, name, symbol)

  /**
   * Creates a new immutable measurement of the given magnitude in terms of this unit.
   * Implementations are **strongly** recommended to sharpen the return type to a unit-specific
   * measurement implementation.
   *
   * @param magnitude the magnitude of the measurement.
   * @return the measurement object
   */
  abstract fun of(magnitude: Double): Measure<U>

  /**
   * Creates a new immutable measurement of the given magnitude in terms of this unit's base unit.
   * Implementations are **strongly** recommended to sharpen the return type to a unit-specific
   * measurement implementation.
   *
   * @param baseUnitMagnitude the magnitude in terms of the base unit
   * @return the measurement object
   */
  abstract fun ofBaseUnits(baseUnitMagnitude: Double): Measure<U>

  /**
   * Gets a measure of zero magnitude in terms of this unit. The returned object is guaranteed to be
   * of the same type returned by [of]. Subclasses are encouraged to override this method to sharpen
   * the return type.
   *
   * @return a zero-magnitude measure of this unit
   */
  open fun zero(): Measure<U> = zeroMeasure

  /**
   * Gets a measure with a magnitude of 1.0 in terms of this unit. The returned object is guaranteed
   * to be of the same type returned by [of]. Subclasses are encouraged to override this method to
   * sharpen the return type.
   *
   * @return a measure of magnitude 1.0 in terms of this unit
   */
  open fun one(): Measure<U> = oneMeasure

  /**
   * Combines this unit with a unit of time. This often - but not always - results in a velocity.
   * Subclasses should sharpen the return type to be unit-specific.
   *
   * @param time the unit of time
   * @return the combined unit
   */
  @Suppress("UNCHECKED_CAST")
  open fun per(time: TimeUnit): PerUnit<U, TimeUnit> = PerUnit(this as U, time)

  /**
   * Checks if this unit is the base unit for its own system of measurement.
   *
   * @return true if this is the base unit, false if not
   */
  fun isBaseUnit(): Boolean = this == baseUnit

  /**
   * Converts a value in terms of base units to a value in terms of this unit.
   *
   * @param valueInBaseUnits the value in base units to convert
   * @return the equivalent value in terms of this unit
   */
  fun fromBaseUnits(valueInBaseUnits: Double): Double = fromBaseConverter(valueInBaseUnits)

  /**
   * Converts a value in terms of this unit to a value in terms of the base unit.
   *
   * @param valueInNativeUnits the value in terms of this unit to convert
   * @return the equivalent value in terms of the base unit
   */
  fun toBaseUnits(valueInNativeUnits: Double): Double = toBaseConverter(valueInNativeUnits)

  /**
   * Checks if this unit is equivalent to another one. Equivalence is determined by both units
   * having the same base type and treat the same base unit magnitude as the same magnitude in their
   * own units, to within [Measure.EQUIVALENCE_THRESHOLD].
   *
   * @param other the unit to compare to.
   * @return true if both units are equivalent, false if not
   */
  fun equivalent(other: Unit<*>): Boolean {
    if (this::class != other::class) {
      // different unit types, not compatible
      return false
    }

    val arbitrary = 16_777.214 // 2^24 / 1e3

    return abs(this.fromBaseConverter(arbitrary) - other.fromBaseConverter(arbitrary)) <=
      Measure.EQUIVALENCE_THRESHOLD &&
      abs(this.toBaseConverter(arbitrary) - other.toBaseConverter(arbitrary)) <=
      Measure.EQUIVALENCE_THRESHOLD
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Unit<*>) return false

    return unitName == other.unitName && unitSymbol == other.unitSymbol &&
      this.equivalent(other)
  }

  override fun hashCode(): Int {
    var result = toBaseConverter.hashCode()
    result = 31 * result + fromBaseConverter.hashCode()
    result = 31 * result + unitName.hashCode()
    result = 31 * result + unitSymbol.hashCode()
    return result
  }

  /**
   * Gets the name of this unit.
   *
   * @return the unit's name
   */
  fun name(): String = unitName

  /**
   * Gets the symbol of this unit.
   *
   * @return the unit's symbol
   */
  fun symbol(): String = unitSymbol

  override fun toString(): String = name()
}
