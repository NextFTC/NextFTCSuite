/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.robot

import com.pedropathing.ivy.Command

/**
 * Represents a subsystem or mechanism on the robot (e.g., an arm, drivetrain, or intake).
 *
 * Mechanisms encapsulate the hardware interactions and provide a centralized place
 * to manage periodic updates and default commands within the command-based architecture.
 */
interface Mechanism {
  /**
   * Called periodically during the OpMode loop.
   * This is where you can update hardware reads, calculate state, or push telemetry.
   */
  fun periodic() {
  }

  /**
   * The default command to run when no other commands are actively requiring this mechanism.
   * Returns null if there is no default command.
   */
  val defaultCommand: Command?
    get() = null
}
