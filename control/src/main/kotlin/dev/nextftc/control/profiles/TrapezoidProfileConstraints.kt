/*
 * Copyright (c) FIRST and other WPILib contributors.
 * Open Source Software; you can modify and/or share it under the terms of
 * the WPILib BSD license file in the root directory of this project.
 *
 * Copyright (c) 2026 NextFTC Team
 * Portions of this file are original code or adaptations by the NextFTC Team.
 * Use of this source code is governed by an BSD-3-clause
 * license that can be found in the LICENSE.md file at the root of this repository or at
 * https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.profiles

import dev.nextftc.units.Unit
import dev.nextftc.units.degreesPerSecond
import dev.nextftc.units.degreesPerSecondSquared
import dev.nextftc.units.inchesPerSecond
import dev.nextftc.units.inchesPerSecondSquared
import dev.nextftc.units.measuretypes.Per
import dev.nextftc.units.unittypes.PerUnit
import dev.nextftc.units.unittypes.TimeUnit

/**
 * Constraints for a trapezoidal motion profile.
 *
 * @property maxVelocity The maximum velocity of the profile.
 * @property maxAcceleration The maximum acceleration of the profile.
 */
data class TrapezoidProfileConstraints<U : Unit<U>>(
  val maxVelocity: Per<U, TimeUnit>,
  val maxAcceleration: Per<PerUnit<U, TimeUnit>, TimeUnit>,
) {
  init {
    require(maxVelocity.magnitude >= 0.0) { "Constraints must be non-negative" }
    require(maxAcceleration.magnitude >= 0.0) { "Constraints must be non-negative" }
  }

  companion object {
    @JvmStatic fun linear(maxVelocity: Double, maxAcceleration: Double) = TrapezoidProfileConstraints(
      maxVelocity.inchesPerSecond,
      maxAcceleration.inchesPerSecondSquared,
    )

    @JvmStatic fun angular(maxVelocity: Double, maxAcceleration: Double) = TrapezoidProfileConstraints(
      maxVelocity.degreesPerSecond,
      maxAcceleration.degreesPerSecondSquared,
    )
  }
}
