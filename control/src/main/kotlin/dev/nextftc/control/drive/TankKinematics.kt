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
 * Converts raw drive inputs into tank/differential wheel powers.
 */
class TankKinematics: DriveKinematics {


    override fun calculate(input: DriveInput): WheelPowers {
        val denominator = max(input.y.absoluteValue + input.rx.absoluteValue, 1.0)

        val left = (input.y + input.rx) / denominator
        val right = (input.y - input.rx) / denominator

        return WheelPowers(frontLeft = left, frontRight = right, backLeft = left, backRight = right)
    }
}