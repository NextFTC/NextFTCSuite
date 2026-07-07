/*
 * Copyright (c) FIRST and other WPILib contributors.
 * Open Source Software; you can modify and/or share it under the terms of
 * the WPILib BSD license file in the root directory of this project.
 *
 * Copyright (c) 2026 NextFTC Team
 * Portions of this file are original code or adaptations by the NextFTC Team.
 * Use of this source code is governed by an BSD-3-clause
 * license that can be found in the LICENSE.md file at the root of this repository or at
 * https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.robot.triggers

/**
 * A lightweight execution loop for polling boolean triggers and executing bound actions.
 *
 * Functions similarly to WPILib's EventLoop, keeping a registry of Runnables that are
 * executed sequentially on every [poll] call.
 */
class EventLoop {
  private val bindings = mutableListOf<Runnable>()

  /**
   * Binds a new action to be executed every time the loop is polled.
   *
   * @param action The action to run.
   */
  fun bind(action: Runnable) {
    bindings.add(action)
  }

  /**
   * Executes all bound actions sequentially.
   * This is automatically called by [dev.nextftc.robot.SchedulerHook] in [dev.nextftc.robot.NextOpMode].
   */
  fun poll() {
    bindings.forEach { it.run() }
  }

  /**
   * Clears all bound actions from the loop.
   */
  fun clear() {
    bindings.clear()
  }
}
