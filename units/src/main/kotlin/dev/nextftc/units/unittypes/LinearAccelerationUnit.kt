/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("LinearAccelerationUnits")

package dev.nextftc.units.unittypes

import dev.nextftc.units.measuretypes.LinearAcceleration

/**
 * Unit representing linear acceleration (velocity per time, or distance per time squared).
 *
 * Common examples:
 * - Meters per second squared (m/s²)
 * - Feet per second squared (ft/s²)
 * - Standard gravity (g ≈ 9.80665 m/s²)
 *
 * @param velocity the velocity unit (numerator)
 * @param time the time unit (denominator)
 */
class LinearAccelerationUnit(velocity: LinearVelocityUnit, time: TimeUnit) :
  PerUnit<PerUnit<DistanceUnit, TimeUnit>, TimeUnit>(velocity, time) {
  override fun of(magnitude: Double): LinearAcceleration = LinearAcceleration(magnitude, this)

  override fun ofBaseUnits(baseUnitMagnitude: Double): LinearAcceleration =
    of(this.fromBaseUnits(baseUnitMagnitude))
}

// Standard gravity constant (approximately 9.80665 m/s²)
const val STANDARD_GRAVITY_MPS2 = 9.80665
