/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.sensors.colors

import android.graphics.Color
import dev.nextftc.hardware.sensors.NextColorDistanceSensor

/**
 * Stores a color. Use [rgb] if you have red, green, and blue values,
 * or [hsv] if you have hue, saturation, and brightness values.
 *
 * You can use the [NextColorDistanceSensor.debug] to find these values.
 * Reccomened to use [hsv] for most cases.
 * Example:
 * ```
 * val lime = NextColor.HSV(100f, 0.9f, 0.85f)
 * val sameColor = NextColor.RGB(lime.red, lime.green, lime.blue)
 * ```
 *
 *
 * @author 28shettr
 */

data class NextColor(val red: Float, val green: Float, val blue: Float) {
  /** The color as a `[red, green, blue]` float array (0–255). */
  val rgb: FloatArray
    get() = floatArrayOf(red, green, blue)

  /** The color as a `[hue, saturation, value]` float array (hue: 0–360, saturation/value: 0–1). */
  val hsv: FloatArray
    get() {
      val out = FloatArray(3)
      Color.RGBToHSV(red.toInt(), green.toInt(), blue.toInt(), out)
      return out
    }

  companion object {

    /**
     * Creates a color from red, green, and blue values.
     * You can use the [NextColorDistanceSensor.debug] to find these values.
     *
     * @param red   How much red (0–255).
     * @param green How much green (0–255).
     * @param blue  How much blue (0–255).
     */
    fun rgb(red: Float, green: Float, blue: Float): NextColor {
      require(red in 0f..255f) { "value must be 0-255 got $red" }
      require(green in 0f..255f) { "value must be 0-255 got $green" }
      require(blue in 0f..255f) { "value must be 0-255 got $blue" }

      return NextColor(red, green, blue)
    }

    /**
     * Creates a color from hue, saturation, and brightness values.
     * You can use the [NextColorDistanceSensor.debug] to find these values.
     *
     * @param hue        The color's position on the color wheel, in degrees (0–360).
     * @param saturation How vivid the color is (0 = grey, 1 = fully vivid).
     * @param value      How bright the color is (0 = black, 1 = full brightness).
     */
    fun hsv(hue: Float, saturation: Float, value: Float): NextColor {
      require(hue in 0f..360f) { "value must be 0-360 got $hue" }
      require(saturation in 0f..1f) { "value must be 0-1 got $saturation" }
      require(value in 0f..1f) { "value must be 0-1 got $value" }

      val rgbInt = Color.HSVToColor(floatArrayOf(hue, saturation, value))
      return NextColor(
        Color.red(rgbInt).toFloat(),
        Color.green(rgbInt).toFloat(),
        Color.blue(rgbInt).toFloat(),
      )
    }
  }
}
