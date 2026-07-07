/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Joules
import dev.nextftc.units.Measure
import dev.nextftc.units.NewtonMeters
import dev.nextftc.units.unittypes.ForceUnit

/**
 * Immutable measurement of force.
 *
 * This class represents force values like newtons, pounds-force, etc. It supports arithmetic
 * operations and conversions between different force units.
 */
class Force
internal constructor(override val magnitude: Double, override val unit: ForceUnit) :
  Measure<ForceUnit> {
  override val baseUnitMagnitude: Double = unit.toBaseUnits(magnitude)

  override fun unaryMinus(): Force = Force(-magnitude, unit)

  override fun plus(other: Measure<out ForceUnit>): Force {
    val otherInBaseUnits = other.baseUnitMagnitude
    val sumInBaseUnits = this.baseUnitMagnitude + otherInBaseUnits
    return Force(unit.fromBaseUnits(sumInBaseUnits), unit)
  }

  override fun minus(other: Measure<out ForceUnit>): Force {
    val otherInBaseUnits = other.baseUnitMagnitude
    val diffInBaseUnits = this.baseUnitMagnitude - otherInBaseUnits
    return Force(unit.fromBaseUnits(diffInBaseUnits), unit)
  }

  override fun times(multiplier: Double): Force = Force(magnitude * multiplier, unit)

  override fun div(divisor: Double): Force = Force(magnitude / divisor, unit)

  /**
   * Multiplies this force by a distance to get energy (work done).
   *
   * Work = Force × Distance
   *
   * @param distance the distance over which the force is applied
   * @return the energy (work) in joules
   */
  operator fun times(distance: Distance): Energy {
    val forceInNewtons = this.baseUnitMagnitude
    val distanceInMeters = distance.baseUnitMagnitude
    return Energy(forceInNewtons * distanceInMeters, Joules)
  }

  /**
   * Multiplies this force by a distance (moment arm) to get torque.
   *
   * Torque = Force × Distance (perpendicular moment arm)
   *
   * @param momentArm the perpendicular distance from the pivot point
   * @return the torque in newton-meters
   */
  fun timesTorque(momentArm: Distance): Torque {
    val forceInNewtons = this.baseUnitMagnitude
    val distanceInMeters = momentArm.baseUnitMagnitude
    return Torque(forceInNewtons * distanceInMeters, NewtonMeters)
  }

  override fun toString() = toShortString()
}
