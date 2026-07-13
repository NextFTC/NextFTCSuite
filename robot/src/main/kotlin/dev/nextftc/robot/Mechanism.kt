/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.robot

import com.pedropathing.ivy.Command
import com.pedropathing.ivy.commands.Commands

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
  fun periodic() {}

  /**
   * Creates a command that runs once and requires this mechanism.
   */
  fun instant(action: Runnable): Command = Commands.instant(action).requiring(this)

  /**
   * Creates a command that runs indefinitely and requires this mechanism.
   */
  fun infinite(action: Runnable): Command = Commands.infinite(action).requiring(this)
}
