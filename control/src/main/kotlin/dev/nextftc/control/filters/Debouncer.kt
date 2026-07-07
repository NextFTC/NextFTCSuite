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

package dev.nextftc.control.filters

import kotlin.time.ComparableTimeMark
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

/**
 * A simple debounce filter for boolean streams. Requires that the boolean change value from
 * baseline for a specified period of time before the filtered value changes.
 *
 * @param debounceTimeSeconds The number of seconds the value must change from baseline for the filtered
 *     value to change.
 * @param type Which type of state change the debouncing will be performed on.
 */
class Debouncer @JvmOverloads constructor(
  var debounceTimeSeconds: Double,
  var type: DebounceType = DebounceType.RISING,
) {
  /** Type of debouncing to perform. */
  enum class DebounceType {
    /** Rising edge. */
    RISING,

    /** Falling edge. */
    FALLING,

    /** Both rising and falling edges. */
    BOTH,
  }

  private var baseline: Boolean = type == DebounceType.FALLING
  private var prevTime: ComparableTimeMark? = null

  init {
    resetTimer()
  }

  private fun resetTimer(timestamp: ComparableTimeMark = TimeSource.Monotonic.markNow()) {
    prevTime = timestamp
  }

  private fun hasElapsed(timestamp: ComparableTimeMark): Boolean {
    if (prevTime == null) return false
    return (timestamp - prevTime!!).toDouble(DurationUnit.SECONDS) >= debounceTimeSeconds
  }

  /**
   * Applies the debouncer to the input stream.
   *
   * @param input The current value of the input stream.
   * @param timestamp the current time (default: TimeSource.Monotonic.markNow())
   * @return The debounced value of the input stream.
   */
  @JvmOverloads
  fun calculate(input: Boolean, timestamp: ComparableTimeMark = TimeSource.Monotonic.markNow()): Boolean {
    if (input == baseline) {
      resetTimer(timestamp)
    }

    if (hasElapsed(timestamp)) {
      if (type == DebounceType.BOTH) {
        baseline = input
        resetTimer(timestamp)
      }
      return input
    } else {
      return baseline
    }
  }
}
