/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.actuators

import dev.nextftc.hardware.Caching
import kotlin.math.round

/**
 * Wrapper for the goBILDA PWM RGB Headlight Module.
 *
 * The module behaves like a servo,
 * different PWM positions correspond to different colors/patterns.
 *
 * Example:
 * ```
 * val headlight = RGBHeadlight("headlights")
 *
 * headlight.setColor(RGBHeadlight.Color.RED)
 *
 * headlight.setBrightness(0.8)
 * ```
 *
 * @param name The name of the servo in the hardware map.
 * @param cacheTolerance Tolerance used by the [Caching] delegate for
 * position updates; defaults to 0.01.
 */
class RGBHeadlight(name: String, cacheTolerance: Double = 0.01) : NextServo(name, cacheTolerance) {

  /**
   * Available colors/patterns for the headlights
   *
   * Values are servo positions.
   */
  enum class Color(val position: Double) {
    OFF(0.0),

    RED(0.279),
    ORANGE(0.333),
    YELLOW(0.388),
    SAGE(0.444),
    GREEN(0.500),
    AZURE(0.555),
    BLUE(0.611),
    INDIGO(0.666),
    VIOLET(0.722),
    WHITE(1.0),
  }

  /**
   * Sets the headlight color/pattern.
   */
  fun setColor(color: Color) {
    position = round(color.position * 100) / 100.0
  }

  /**
   * Sets the headlight color/pattern using a raw PWM position.
   */
  fun setColor(pwm: Double) {
    position = round(pwm * 100) / 100.0
  }

  /**
   * Turns headlights off.
   */
  fun off() {
    setColor(Color.OFF)
  }

  /**
   * Sets brightness by scaling the PWM range.
   *
   * brightness:
   * 0.0 = dimmest
   * 1.0 = brightest
   */
  fun setBrightness(brightness: Double) {
    val clipped = brightness.coerceIn(0.0, 1.0)

    val lower = 500.0
    val upper = 2500.0 * clipped

    setPwmRange(lower, upper)
  }
}
