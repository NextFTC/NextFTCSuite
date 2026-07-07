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
 * A class that implements a moving-window median filter. Useful for reducing measurement noise,
 * especially with processes that generate occasional, extreme outliers (such as values from vision
 * processing, LIDAR, or ultrasonic sensors).
 *
 * @param size The number of samples in the moving window.
 */
class MedianFilter(private val size: Int) {
  init {
    require(size > 0) { "Size must be greater than 0" }
  }

  private val valueBuffer = ArrayDeque<Double>(size)
  private val orderedValues = ArrayList<Double>(size)

  /**
   * Calculates the moving-window median for the next value of the input stream.
   *
   * @param next The next input value.
   * @return The median of the moving window, updated to include the next value.
   */
  fun calculate(next: Double): Double {
    var index = orderedValues.binarySearch(next)
    if (index < 0) {
      index = -(index + 1)
    }

    // Place value at proper insertion point
    orderedValues.add(index, next)

    // If buffer is at max size, pop element off of beginning of circular buffer (oldest element)
    // and remove it from the ordered list
    if (orderedValues.size > size) {
      val removed = valueBuffer.removeFirst()
      orderedValues.remove(removed)
    }

    // Add next value to circular buffer (newest element)
    valueBuffer.addLast(next)

    val curSize = orderedValues.size
    return if (curSize % 2 != 0) {
      orderedValues[curSize / 2]
    } else {
      (orderedValues[curSize / 2 - 1] + orderedValues[curSize / 2]) / 2.0
    }
  }

  /**
   * Returns the last value calculated by the MedianFilter.
   *
   * @return The last value.
   */
  fun lastValue(): Double = valueBuffer.lastOrNull() ?: 0.0

  /** Resets the filter, clearing the window of all elements. */
  fun reset() {
    orderedValues.clear()
    valueBuffer.clear()
  }
}
