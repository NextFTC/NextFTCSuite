/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("AngularAccelerationUnits")

package dev.nextftc.units.unittypes

import dev.nextftc.units.measuretypes.AngularAcceleration

/**
 * Unit representing angular acceleration (angular velocity per time, or angle per time squared).
 *
 * Common examples:
 * - Radians per second squared (rad/s²)
 * - Degrees per second squared (deg/s²)
 * - Rotations per second squared (rot/s²)
 *
 * @param angularVelocity the angular velocity unit (numerator)
 * @param time the time unit (denominator)
 */
class AngularAccelerationUnit(angularVelocity: AngularVelocityUnit, time: TimeUnit) :
  PerUnit<PerUnit<AngleUnit, TimeUnit>, TimeUnit>(angularVelocity, time) {
  override fun of(magnitude: Double): AngularAcceleration = AngularAcceleration(magnitude, this)

  override fun ofBaseUnits(baseUnitMagnitude: Double): AngularAcceleration =
    of(this.fromBaseUnits(baseUnitMagnitude))
}
