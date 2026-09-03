/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.robot

import com.pedropathing.ivy.Command
import com.pedropathing.ivy.CommandBuilder
import com.pedropathing.ivy.behaviors.BlockedBehavior
import com.pedropathing.ivy.behaviors.ConflictBehavior
import com.pedropathing.ivy.behaviors.InterruptedBehavior
import com.pedropathing.ivy.commands.Commands
import dev.nextftc.robot.coroutine.CommandScope
import dev.nextftc.robot.coroutine.CoroutineCommandBuilder

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
   * The default command that runs when this mechanism is not being controlled by a command.
   */
  val defaultCommand: Command
    get() = infinite {}

  /**
   * Creates a command that runs once and requires this mechanism.
   */
  fun instant(action: Runnable): CommandBuilder = Commands.instant(action).requiring(this)

  /**
   * Creates a command that runs indefinitely and requires this mechanism.
   */
  fun infinite(action: Runnable): CommandBuilder = Commands.infinite(action).requiring(this)

  /**
   * Creates a coroutine-based command that requires this mechanism.
   */
  fun coroutine(body: suspend CommandScope.() -> Unit): CoroutineCommandBuilder = CoroutineCommandBuilder()
    .requiring(this)
    .setBody(body)
}

internal fun Mechanism.forceDefaultCommand(command: Command) = CommandBuilder()
  .requiring(command.requirements() union setOf(this))
  .setPriority(Int.MIN_VALUE)
  .setInterruptedBehavior(InterruptedBehavior.SUSPEND)
  .setConflictBehavior(ConflictBehavior.QUEUE)
  .setBlockedBehavior(BlockedBehavior.QUEUE)
  .setStart(command::start)
  .setExecute(command::execute)
  .setEnd(command::end)
  .setDone(command::done)
