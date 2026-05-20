/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.sensors

/**
 * Defines a color by its HSV bounds. Used by [NextColorDistanceSensor.isColor]
 * to check if a sensor reading matches.
 *
 * Hue is in degrees (0..360). Saturation and value are in (0..1) and default
 * to the full range, so you can specify just hue if you want.
 *
 * Example:
 * ```
 * val green  = ColorProfile(hueMin = 150f, hueMax = 185f, saturationMin = 0.2f)
 * val purple = ColorProfile(hueMin = 200f, hueMax = 250f, saturationMin = 0.2f)
 * ```
 *
 *
 * @property hueMin Lower hue bound in degrees (0..360).
 * @property hueMax Upper hue bound in degrees (0..360).
 * @property saturationMin Lower saturation bound (0..1). Defaults to 0.
 * @property saturationMax Upper saturation bound (0..1). Defaults to 1.
 * @property valueMin Lower value/brightness bound (0..1). Defaults to 0.
 * @property valueMax Upper value/brightness bound (0..1). Defaults to 1.
 *
 * @author 28shettr
 */

data class ColorProfile @JvmOverloads constructor(
    val hueMin: Float,
    val hueMax: Float,
    val saturationMin: Float = 0f,
    val saturationMax: Float = 1f,
    val valueMin: Float = 0f,
    val valueMax: Float = 1f,
) {
    /** True if [hsv] falls inside all three bounds. Hue wraps if [hueMin] > [hueMax]. */
    fun contains(hsv: FloatArray): Boolean {
        val h = hsv[0]
        val s = hsv[1]
        val v = hsv[2]
        val hueInRange = if (hueMin > hueMax) {
            h >= hueMin || h <= hueMax   // wraparound: e.g. 350..10 means 350..360 OR 0..10
        } else {
            h in hueMin..hueMax           // normal range
        }

        return hueInRange
                && s in saturationMin..saturationMax
                && v in valueMin..valueMax
    }
}
