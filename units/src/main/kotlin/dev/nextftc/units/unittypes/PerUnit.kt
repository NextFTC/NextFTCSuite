/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("PerUnits")

package dev.nextftc.units.unittypes

import dev.nextftc.units.Unit
import dev.nextftc.units.measuretypes.Per

/**
 * Represents a unit that is the ratio of two other units (e.g., meters per second, degrees per
 * second).
 *
 * The PerUnit automatically normalizes to use base units for internal calculations. For example, if
 * you create a PerUnit with Miles and Hours, it internally works with Meters and Seconds (the base
 * units), ensuring that conversions work correctly.
 *
 * @param N the type of the numerator unit
 * @param D the type of the denominator unit
 * @param numerator the numerator unit (e.g., Meters, Miles)
 * @param denominator the denominator unit (e.g., Seconds, Hours)
 */
open class PerUnit<N : Unit<N>, D : Unit<D>> internal constructor(val numerator: N, val denominator: D) :
  Unit<PerUnit<N, D>>(
    null,
    { value ->
      // Convert to base units: (numerator/denominator) -> (baseNumerator/baseDenominator)
      // Example: 60 miles/hour -> ? meters/second
      // 1. Convert numerator: 60 miles -> 96560.64 meters
      // 2. Convert denominator: 1 hour -> 3600 seconds
      // 3. Result: 96560.64 meters / 3600 seconds = 26.822 meters/second
      val numeratorInBase = numerator.toBaseUnits(value)
      // How many base units of denominator equal 1 unit of this denominator?
      // E.g., 1 hour = 3600 seconds, so we divide by 3600
      val denominatorToBaseRatio = denominator.toBaseUnits(1.0)
      numeratorInBase / denominatorToBaseRatio
    },
    { baseValue ->
      // Convert from base units: (baseNumerator/baseDenominator) -> (numerator/denominator)
      // Example: 26.822 meters/second -> ? miles/hour
      // 1. Convert numerator back: meters -> miles
      // 2. Convert denominator back: seconds -> hours
      val numeratorFromBase = numerator.fromBaseUnits(baseValue)
      // How many base units of denominator equal 1 unit of this denominator?
      val denominatorToBaseRatio = denominator.toBaseUnits(1.0)
      numeratorFromBase * denominatorToBaseRatio
    },
    "${numerator.name()} per ${denominator.name()}",
    "${numerator.symbol()}/${denominator.symbol()}",
  ) {
  /**
   * The base PerUnit using the base units of both numerator and denominator. For example, Miles per
   * Hour would have a basePerUnit of Meters per Second.
   */
  val basePerUnit: PerUnit<N, D> by lazy {
    if (numerator == numerator.baseUnit && denominator == denominator.baseUnit) {
      this
    } else {
      PerUnit(numerator.baseUnit, denominator.baseUnit)
    }
  }

  override fun of(magnitude: Double): Per<N, D> = Per(magnitude, this)

  override fun ofBaseUnits(baseUnitMagnitude: Double): Per<N, D> = of(this.fromBaseUnits(baseUnitMagnitude))

  override fun per(time: TimeUnit): PerUnit<PerUnit<N, D>, TimeUnit> = of(this, time)

  companion object {
    private val cache:
      java.util.concurrent.ConcurrentHashMap<Pair<Unit<*>, Unit<*>>, PerUnit<*, *>> =
      java.util.concurrent.ConcurrentHashMap()

    /**
     * Generic factory returns a cached PerUnit for the given numerator and denominator.
     */
    @Suppress("UNCHECKED_CAST")
    fun <N : Unit<N>, D : Unit<D>> of(numerator: N, denominator: D): PerUnit<N, D> {
      val key = Pair(numerator as Unit<*>, denominator as Unit<*>)
      val created = cache.computeIfAbsent(key) {
        // Prefer specialized classes when possible
        when (numerator) {
          is DistanceUnit if denominator is TimeUnit -> LinearVelocityUnit(
            numerator,
            denominator,
          )
          is AngleUnit if denominator is TimeUnit -> AngularVelocityUnit(
            numerator,
            denominator,
          )
          is LinearVelocityUnit if denominator is TimeUnit -> LinearAccelerationUnit(
            numerator,
            denominator,
          )
          is AngularVelocityUnit if denominator is TimeUnit -> AngularAccelerationUnit(
            numerator,
            denominator,
          )
          else -> PerUnit(numerator, denominator)
        }
      }
      return created as PerUnit<N, D>
    }
  }
}
