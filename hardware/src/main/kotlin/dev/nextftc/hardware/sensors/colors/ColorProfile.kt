/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.sensors.colors

import dev.nextftc.hardware.sensors.NextColorDistanceSensor
import kotlin.math.abs

enum class ColorSpace { RGB, HSV }

/**
 * Describes a target color and the per-channel tolerances used to decide whether a
 * sensor reading is a match.
 *
 * Use [NextColorDistanceSensor.debug] in telemetry to read live HSV values and
 * calibrate [color] and [tolerance]. [ColorSpace.HSV] is recommended for most cases
 * as it is more stable under changing lighting conditions.
 *
 * Example:
 * ```
 * val green = ColorProfile(
 *     space = ColorSpace.HSV,
 *     color = NextColor.HSV(130f, 0.7f, 0.6f),
 *     tolerance = NextColor.HSV(20f, 0.3f, 1f),
 * )
 *
 * override fun periodic() {
 *     sensor.update()
 *     if (sensor.isColor(green)) { ... }
 * }
 * ```
 *
 * @property space     The color space to compare in.
 * @property color     The target color to match against.
 * @property tolerance How far each channel can deviate from [color] and still count as a match.
 *
 * @author 28shettr
 */
data class ColorProfile(val space: ColorSpace, val color: NextColor, val tolerance: NextColor) {

  private val colorHsv = color.hsv
  private val toleranceHsv = tolerance.hsv
  private val colorRgb = color.rgb
  private val toleranceRgb = tolerance.rgb

  /** Returns `true` if [reading] falls within [tolerance] of [color] in [space]. */
  fun matches(reading: NextColor): Boolean = when (space) {
    ColorSpace.RGB -> matchesRgb(reading)
    ColorSpace.HSV -> matchesHsv(reading)
  }
  private fun matchesRgb(input: NextColor): Boolean {
    val c = colorRgb
    val t = toleranceRgb
    val i = input.rgb

    return abs(i[0] - c[0]) <= t[0] &&
      abs(i[1] - c[1]) <= t[1] &&
      abs(i[2] - c[2]) <= t[2]
  }

  private fun matchesHsv(input: NextColor): Boolean {
    val c = colorHsv
    val t = toleranceHsv
    val i = input.hsv
    return wraparoundCheck(i[0], c[0]) <= t[0] &&
      abs(i[1] - c[1]) <= t[1] &&
      abs(i[2] - c[2]) <= t[2]
  }

  private fun wraparoundCheck(a: Float, b: Float): Float {
    val diff = abs(a - b) % 360f
    return if (diff > 180f) 360f - diff else diff
  }
}
