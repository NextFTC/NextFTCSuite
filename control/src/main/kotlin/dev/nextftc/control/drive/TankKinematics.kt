/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.drive

import kotlin.math.absoluteValue
import kotlin.math.max

data class TankWheelPowers(val left: Double, val right: Double)

/**
 * Converts raw drive inputs into tank/differential wheel powers.
 */

class TankKinematics : DriveKinematics<TankWheelPowers> {
  override fun calculate(input: DriveInput): TankWheelPowers {
    val denominator = max(input.x.absoluteValue + input.rx.absoluteValue, 1.0)
    return TankWheelPowers(
      left = (input.x + input.rx) / denominator,
      right = (input.x - input.rx) / denominator,
    )
  }
}
