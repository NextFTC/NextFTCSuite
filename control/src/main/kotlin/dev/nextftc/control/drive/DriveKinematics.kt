/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.drive

/** Raw driver input for a 3-DOF planar drivetrain. */
data class DriveInput(val x: Double, val y: Double, val rx: Double)

/**
 * Converts raw drive input into a drivetrain-specific output.
 *
 * Each drivetrain type (mecanum, tank, etc.) implements this with its own
 * [Output] type
 *
 * @param Output The type produced by this drivetrain's kinematics — e.g.
 *   `MecanumWheelPowers`, `TankWheelPowers`, or a list of swerve module states.
 */
interface DriveKinematics<Output> {
  /** Converts [input] into this drivetrain's output type. */
  fun calculate(input: DriveInput): Output
}
