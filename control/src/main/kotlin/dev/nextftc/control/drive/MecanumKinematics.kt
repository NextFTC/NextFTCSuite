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

/**
 * Converts raw drive inputs into mecanum wheel powers.
 *
 * Applies a strafe compensation factor to account for mecanum wheels being
 * physically less efficient strafing than driving forward/backward.
 */
class MecanumKinematics : DriveKinematics {

    private val strafeCompensation = 1.1

    override fun calculate(input: DriveInput): WheelPowers {
        val compensatedX = input.x * strafeCompensation

        val denominator = max(
            compensatedX.absoluteValue + input.y.absoluteValue + input.rx.absoluteValue,
            1.0
        )

        return WheelPowers(
            frontLeft = (input.y + compensatedX + input.rx) / denominator,
            frontRight = (input.y - compensatedX - input.rx) / denominator,
            backLeft = (input.y - compensatedX + input.rx) / denominator,
            backRight = (input.y + compensatedX - input.rx) / denominator,
        )
    }
}