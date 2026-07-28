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

data class MecanumWheelPowers(
  val frontLeft: Double,
  val frontRight: Double,
  val backLeft: Double,
  val backRight: Double,
)

/**
 * Converts raw drive inputs into mecanum wheel powers.
 *
 * Applies a strafe compensation factor to account for mecanum wheels being
 * physically less efficient strafing than driving forward/backward.
 */
class MecanumKinematics @JvmOverloads constructor(private val strafeCompensation: Double = 1.1) :
  DriveKinematics<MecanumWheelPowers> {
  override fun calculate(input: DriveInput): MecanumWheelPowers {
    val compensatedX = input.x * strafeCompensation

    val denominator = max(
      compensatedX.absoluteValue + input.y.absoluteValue + input.rx.absoluteValue,
      1.0,
    )

    return MecanumWheelPowers(
      frontLeft = (input.y + compensatedX + input.rx) / denominator,
      frontRight = (input.y - compensatedX - input.rx) / denominator,
      backLeft = (input.y - compensatedX + input.rx) / denominator,
      backRight = (input.y + compensatedX - input.rx) / denominator,
    )
  }
}
