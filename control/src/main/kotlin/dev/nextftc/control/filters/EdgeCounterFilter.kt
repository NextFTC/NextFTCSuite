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
 * A rising edge counter for boolean streams. Requires that the boolean change value to true for a
 * specified number of times within a specified time window after the first rising edge before the
 * filtered value changes.
 *
 * <p>The filter activates when the input has risen (transitioned from false to true) the required
 * number of times within the time window. Once activated, the output remains true as long as the
 * input is true. The edge count resets when the time window expires or when the input goes false
 * after activation.
 *
 * <p>Input must be stable; consider using a Debouncer before this filter to avoid counting noise as
 * multiple edges.
 *
 * @param requiredEdges The number of rising edges required before the output goes true.
 * @param windowTimeSeconds The maximum number of seconds in which all required edges must occur after
 *     the first rising edge.
 */
class EdgeCounterFilter(var requiredEdges: Int, var windowTimeSeconds: Double) {
  private var firstEdgeTime: ComparableTimeMark? = null
  private var currentCount: Int = 0
  private var lastInput: Boolean = false

  init {
    resetTimer()
  }

  private fun resetTimer(timestamp: ComparableTimeMark = TimeSource.Monotonic.markNow()) {
    firstEdgeTime = timestamp
  }

  private fun hasElapsed(timestamp: ComparableTimeMark): Boolean {
    if (firstEdgeTime == null) return false
    return (timestamp - firstEdgeTime!!).toDouble(DurationUnit.SECONDS) >= windowTimeSeconds
  }

  /**
   * Applies the edge counter filter to the input stream.
   *
   * @param input The current value of the input stream.
   * @param timestamp the current time (default: TimeSource.Monotonic.markNow())
   * @return True if the required number of edges have occurred within the time window and the input
   *     is currently true; false otherwise.
   */
  @JvmOverloads
  fun calculate(input: Boolean, timestamp: ComparableTimeMark = TimeSource.Monotonic.markNow()): Boolean {
    val enoughEdges = currentCount >= requiredEdges

    val expired = hasElapsed(timestamp) && !enoughEdges
    val activationEnded = !input && enoughEdges

    if (expired || activationEnded) {
      currentCount = 0
    }

    if (input && !lastInput) {
      if (currentCount == 0) {
        resetTimer(timestamp) // Start timer on first rising edge
      }

      currentCount++
    }

    lastInput = input

    return input && currentCount >= requiredEdges
  }
}
