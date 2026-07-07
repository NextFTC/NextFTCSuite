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

/**
 * @usesMathJax
 *
 * Exponential Moving Average (EMA) filter.
 *
 * Applies the recurrence: \(y_{n} = \alpha \cdot x_n + (1 - \alpha) \cdot y_n\)
 *
 * The first output uses \(y_{-1} = 0\) unless [previous] is preset.
 *
 * @param alpha smoothing factor \(\alpha \in [0.0, 1.0]\); higher values weight newer samples more.
 */
class EMAFilter(val alpha: Double) {
  /**
   * Last output sample, or `null` before the first call to [calculate].
   */
  var previous: Double? = null
    private set

  init {
    require(alpha in 0.0..1.0) { "EMA alpha must be in [0.0, 1.0] but was $alpha" }
  }

  /**
   * Updates the filter with a new sample and returns the filtered value.
   */
  fun calculate(newValue: Double): Double {
    previous = alpha * newValue + (1 - alpha) * (previous ?: 0.0)
    return previous!!
  }
}
