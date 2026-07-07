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

import com.pedropathing.ivy.Command
import com.pedropathing.ivy.Scheduler
import dev.nextftc.control.filters.Debouncer
import dev.nextftc.control.filters.EdgeCounterFilter

/**
 * This class provides an easy way to link commands to conditions.
 *
 * <p>It is very easy to link a button to a command. For instance, you could link the trigger button
 * of a joystick to a "score" command.
 *
 * <p>Triggers can easily be composed for advanced functionality using the [and], [or], [negate] operators.
 */
class Trigger(private val loop: EventLoop, private val condition: () -> Boolean) : () -> Boolean {

  /**
   * Creates a new trigger based on the given condition.
   *
   * <p>Polled by the default scheduler button loop.
   *
   * @param condition the condition represented by this trigger
   */
  constructor(condition: () -> Boolean) : this(defaultEventLoop, condition)

  companion object {
    val defaultEventLoop = EventLoop()
  }

  /**
   * Adds a binding to the EventLoop.
   *
   * @param body The body of the binding to add.
   */
  private fun addBinding(body: (Boolean, Boolean) -> Unit) {
    loop.bind(object : Runnable {
      private var previous = condition()

      override fun run() {
        val current = condition()
        body(previous, current)
        previous = current
      }
    })
  }

  /**
   * Starts the command when the condition changes.
   *
   * @param command the command to start
   * @return this trigger, so calls can be chained
   */
  fun onChange(command: Command): Trigger {
    addBinding { previous, current ->
      if (previous != current) {
        Scheduler.schedule(command)
      }
    }
    return this
  }

  /**
   * Starts the given command whenever the condition changes from `false` to `true`.
   *
   * @param command the command to start
   * @return this trigger, so calls can be chained
   */
  fun onTrue(command: Command): Trigger {
    addBinding { previous, current ->
      if (!previous && current) {
        Scheduler.schedule(command)
      }
    }
    return this
  }

  /**
   * Starts the given command whenever the condition changes from `true` to `false`.
   *
   * @param command the command to start
   * @return this trigger, so calls can be chained
   */
  fun onFalse(command: Command): Trigger {
    addBinding { previous, current ->
      if (previous && !current) {
        Scheduler.schedule(command)
      }
    }
    return this
  }

  /**
   * Starts the given command when the condition changes to `true` and cancels it when the condition
   * changes to `false`.
   *
   * @param command the command to start
   * @return this trigger, so calls can be chained
   */
  fun whileTrue(command: Command): Trigger {
    addBinding { previous, current ->
      if (!previous && current) {
        Scheduler.schedule(command)
      } else if (previous && !current) {
        command.cancel()
      }
    }
    return this
  }

  /**
   * Starts the given command when the condition changes to `false` and cancels it when the
   * condition changes to `true`.
   *
   * @param command the command to start
   * @return this trigger, so calls can be chained
   */
  fun whileFalse(command: Command): Trigger {
    addBinding { previous, current ->
      if (previous && !current) {
        Scheduler.schedule(command)
      } else if (!previous && current) {
        command.cancel()
      }
    }
    return this
  }

  /**
   * Toggles a command when the condition changes from `false` to `true`.
   *
   * @param command the command to toggle
   * @return this trigger, so calls can be chained
   */
  fun toggleOnTrue(command: Command): Trigger {
    addBinding { previous, current ->
      if (!previous && current) {
        if (command.isScheduled) {
          command.cancel()
        } else {
          Scheduler.schedule(command)
        }
      }
    }
    return this
  }

  /**
   * Toggles a command when the condition changes from `true` to `false`.
   *
   * @param command the command to toggle
   * @return this trigger, so calls can be chained
   */
  fun toggleOnFalse(command: Command): Trigger {
    addBinding { previous, current ->
      if (previous && !current) {
        if (command.isScheduled) {
          command.cancel()
        } else {
          Scheduler.schedule(command)
        }
      }
    }
    return this
  }

  override fun invoke(): Boolean = condition()

  fun getAsBoolean(): Boolean = condition()

  /**
   * Composes two triggers with logical AND.
   *
   * @param trigger the condition to compose with
   * @return A trigger which is active when both component triggers are active.
   */
  fun and(trigger: () -> Boolean): Trigger = Trigger(loop) { condition() && trigger() }

  /**
   * Composes two triggers with logical OR.
   *
   * @param trigger the condition to compose with
   * @return A trigger which is active when either component trigger is active.
   */
  fun or(trigger: () -> Boolean): Trigger = Trigger(loop) { condition() || trigger() }

  /**
   * Creates a new trigger that is active when this trigger is inactive, i.e. that acts as the
   * negation of this trigger.
   *
   * @return the negated trigger
   */
  fun negate(): Trigger = Trigger(loop) { !condition() }

  /**
   * Creates a new debounced trigger from this trigger - it will become active when this trigger has
   * been active for longer than the specified period.
   *
   * @param seconds The debounce period.
   * @param type The debounce type.
   * @return The debounced trigger.
   */
  @JvmOverloads
  fun debounce(seconds: Double, type: Debouncer.DebounceType = Debouncer.DebounceType.RISING): Trigger =
    Trigger(
      loop,
      object : () -> Boolean {
        val debouncer = Debouncer(seconds, type)
        override fun invoke(): Boolean = debouncer.calculate(condition())
      },
    )

  /**
   * Creates a new multi-press trigger from this trigger - it will become active when this trigger
   * has been activated the required number of times within the specified time window.
   *
   * <p>This is useful for implementing "double-click" style functionality.
   *
   * <p>Input for this must be stable, consider using a Debouncer before this to avoid counting
   * noise as multiple presses.
   *
   * @param requiredPresses The number of presses required.
   * @param windowTime The number of seconds in which the presses must occur.
   * @return The multi-press trigger.
   */
  fun multiPress(requiredPresses: Int, windowTime: Double): Trigger = Trigger(
    loop,
    object : () -> Boolean {
      val edgeCounterFilter = EdgeCounterFilter(requiredPresses, windowTime)
      override fun invoke(): Boolean = edgeCounterFilter.calculate(condition())
    },
  )
}
