/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.drive

data class DriveInput(val x: Double, val y: Double, val rx: Double)

interface DriveKinematics<Output> {
  fun calculate(input: DriveInput): Output
}
