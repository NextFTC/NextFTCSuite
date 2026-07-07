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
 * A class that limits the rate of change of an input value. Useful for implementing voltage,
 * setpoint, and/or output ramps. A slew-rate limit is most appropriate when the quantity being
 * controlled is a velocity or a voltage.
 *
 * @param positiveRateLimit The rate-of-change limit in the positive direction, in units per
 *     second. This is expected to be positive.
 * @param negativeRateLimit The rate-of-change limit in the negative direction, in units per
 *     second. This is expected to be negative.
 * @param initialValue The initial value of the input.
 */
class SlewRateLimiter @JvmOverloads constructor(
  var positiveRateLimit: Double,
  var negativeRateLimit: Double,
  initialValue: Double = 0.0,
) {
  var lastValue: Double = initialValue
    private set
  private var lastTimestamp: ComparableTimeMark? = null

  /**
   * Creates a new SlewRateLimiter with the given positive rate limit and negative rate limit of
   * -rateLimit.
   *
   * @param rateLimit The rate-of-change limit, in units per second.
   */
  constructor(rateLimit: Double) : this(rateLimit, -rateLimit, 0.0)

  /**
   * Filters the input to limit its slew rate.
   *
   * @param input The input value whose slew rate is to be limited.
   * @param timestamp the current time (default: TimeSource.Monotonic.markNow())
   * @return The filtered value, which will not change faster than the slew rate.
   */
  @JvmOverloads
  fun calculate(input: Double, timestamp: ComparableTimeMark = TimeSource.Monotonic.markNow()): Double {
    if (lastTimestamp == null) {
      lastTimestamp = timestamp
      return lastValue
    }

    val elapsedTime = (timestamp - lastTimestamp!!).toDouble(DurationUnit.SECONDS)

    lastValue +=
      (input - lastValue).coerceIn(
        negativeRateLimit * elapsedTime,
        positiveRateLimit * elapsedTime,
      )

    lastTimestamp = timestamp
    return lastValue
  }

  /**
   * Resets the slew rate limiter to the specified value; ignores the rate limit when doing so.
   *
   * @param value The value to reset to.
   * @param timestamp the current time (default: TimeSource.Monotonic.markNow())
   */
  @JvmOverloads
  fun reset(value: Double, timestamp: ComparableTimeMark = TimeSource.Monotonic.markNow()) {
    lastValue = value
    lastTimestamp = timestamp
  }
}
