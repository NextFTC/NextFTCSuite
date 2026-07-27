/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.drive

data class DriveInput(val x: Double, val y: Double, val rx: Double)

/** Computed power for each wheel of a 4-wheel drivetrain. */
data class WheelPowers(
    val frontLeft: Double,
    val frontRight: Double,
    val backLeft: Double,
    val backRight: Double,
)

/** Converts raw drive input into wheel powers for a specific drivetrain type. */
interface DriveKinematics {
    fun calculate(input: DriveInput): WheelPowers
}
